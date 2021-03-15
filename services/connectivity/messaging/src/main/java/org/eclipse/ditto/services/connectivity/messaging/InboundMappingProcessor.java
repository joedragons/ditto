/*
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.services.connectivity.messaging;

import static org.eclipse.ditto.model.base.common.ConditionChecker.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.eclipse.ditto.model.base.exceptions.DittoRuntimeException;
import org.eclipse.ditto.model.base.headers.DittoHeaderDefinition;
import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.headers.DittoHeadersBuilder;
import org.eclipse.ditto.model.base.headers.DittoHeadersSizeChecker;
import org.eclipse.ditto.model.base.headers.WithDittoHeaders;
import org.eclipse.ditto.model.base.tracing.TracingHelper;
import org.eclipse.ditto.model.connectivity.ConnectionId;
import org.eclipse.ditto.model.connectivity.ConnectionType;
import org.eclipse.ditto.model.connectivity.PayloadMappingDefinition;
import org.eclipse.ditto.protocoladapter.Adaptable;
import org.eclipse.ditto.protocoladapter.ProtocolAdapter;
import org.eclipse.ditto.services.base.config.limits.LimitsConfig;
import org.eclipse.ditto.services.connectivity.config.ConnectivityConfig;
import org.eclipse.ditto.services.connectivity.mapping.DefaultMessageMapperFactory;
import org.eclipse.ditto.services.connectivity.mapping.DittoMessageMapper;
import org.eclipse.ditto.services.connectivity.mapping.MessageMapper;
import org.eclipse.ditto.services.connectivity.mapping.MessageMapperFactory;
import org.eclipse.ditto.services.connectivity.mapping.MessageMapperRegistry;
import org.eclipse.ditto.services.connectivity.messaging.mappingoutcome.MappingOutcome;
import org.eclipse.ditto.services.connectivity.util.ConnectivityMdcEntryKey;
import org.eclipse.ditto.services.models.connectivity.ExternalMessage;
import org.eclipse.ditto.services.models.connectivity.ExternalMessageBuilder;
import org.eclipse.ditto.services.models.connectivity.ExternalMessageFactory;
import org.eclipse.ditto.services.models.connectivity.MappedInboundExternalMessage;
import org.eclipse.ditto.services.utils.akka.logging.ThreadSafeDittoLoggingAdapter;
import org.eclipse.ditto.services.utils.tracing.TracingTags;
import org.eclipse.ditto.signals.base.Signal;
import org.eclipse.ditto.signals.commands.base.Command;

import akka.actor.ActorSystem;
import akka.japi.Pair;
import io.opentracing.Span;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMapAdapter;
import io.opentracing.tag.Tags;
import io.opentracing.util.GlobalTracer;

/**
 * Processes incoming {@link ExternalMessage}s to {@link Signal}s.
 * Encapsulates the message processing logic from the inbound message mapping processor actor.
 */
public final class InboundMappingProcessor
        extends AbstractMappingProcessor<ExternalMessage, MappedInboundExternalMessage> {

    private final ProtocolAdapter protocolAdapter;
    private final DittoHeadersSizeChecker dittoHeadersSizeChecker;

    private InboundMappingProcessor(final ConnectionId connectionId,
            final ConnectionType connectionType,
            final MessageMapperRegistry registry,
            final ThreadSafeDittoLoggingAdapter logger,
            final ProtocolAdapter protocolAdapter,
            final DittoHeadersSizeChecker dittoHeadersSizeChecker) {

        super(registry, logger, connectionId, connectionType);
        this.protocolAdapter = protocolAdapter;
        this.dittoHeadersSizeChecker = dittoHeadersSizeChecker;
    }

    /**
     * Initializes a new command processor with mappers defined in mapping mappingContext.
     * The dynamic access is needed to instantiate message mappers for an actor system.
     *
     * @param connectionId the connection that the processor works for.
     * @param connectionType the type of the connection that the processor works for.
     * @param mappingDefinition the configured mappings used by this processor
     * @param actorSystem the dynamic access used for message mapper instantiation.
     * @param connectivityConfig the configuration settings of the Connectivity service.
     * @param protocolAdapter the ProtocolAdapter to be used.
     * @param logger the logging adapter to be used for log statements.
     * @return the processor instance.
     * @throws org.eclipse.ditto.model.connectivity.MessageMapperConfigurationInvalidException if the configuration of
     * one of the {@code mappingContext} is invalid.
     * @throws org.eclipse.ditto.model.connectivity.MessageMapperConfigurationFailedException if the configuration of
     * one of the {@code mappingContext} failed for a mapper specific reason.
     */
    public static InboundMappingProcessor of(final ConnectionId connectionId,
            final ConnectionType connectionType,
            final PayloadMappingDefinition mappingDefinition,
            final ActorSystem actorSystem,
            final ConnectivityConfig connectivityConfig,
            final ProtocolAdapter protocolAdapter,
            final ThreadSafeDittoLoggingAdapter logger) {

        final ThreadSafeDittoLoggingAdapter loggerWithConnectionId =
                logger.withMdcEntry(ConnectivityMdcEntryKey.CONNECTION_ID, connectionId);

        final MessageMapperFactory messageMapperFactory =
                DefaultMessageMapperFactory.of(connectionId, actorSystem, connectivityConfig.getMappingConfig(),
                        loggerWithConnectionId);
        final MessageMapperRegistry registry =
                messageMapperFactory.registryOf(DittoMessageMapper.CONTEXT, mappingDefinition);

        final LimitsConfig limitsConfig = connectivityConfig.getLimitsConfig();
        final DittoHeadersSizeChecker dittoHeadersSizeChecker =
                DittoHeadersSizeChecker.of(limitsConfig.getHeadersMaxSize(), limitsConfig.getAuthSubjectsMaxCount());

        return of(connectionId, connectionType, registry, loggerWithConnectionId, protocolAdapter,
                dittoHeadersSizeChecker);
    }

    static InboundMappingProcessor of(final ConnectionId connectionId, final ConnectionType connectionType,
            final MessageMapperRegistry registry, final ThreadSafeDittoLoggingAdapter logger,
            final ProtocolAdapter adapter, final DittoHeadersSizeChecker sizeChecker) {
        return new InboundMappingProcessor(connectionId, connectionType, registry, logger, adapter, sizeChecker);
    }

    /**
     * Processes an {@link ExternalMessage} which may result in 0..n messages/errors.
     *
     * @param message the inbound {@link ExternalMessage} to be processed
     * @return combined results of all message mappers.
     */
    @Override
    List<MappingOutcome<MappedInboundExternalMessage>> process(final ExternalMessage message) {
        final Pair<ExternalMessage, Span> messageSpanPair = traceExternalMessage(message,
                "map-" + message.getClass().getSimpleName());
        final ExternalMessage tracedMessage = messageSpanPair.first();

        final List<MessageMapper> mappers = getMappers(tracedMessage.getPayloadMapping().orElse(null));
        logger.withCorrelationId(tracedMessage.getHeaders().get(DittoHeaderDefinition.CORRELATION_ID.getKey()))
                .debug("Mappers resolved for message: {}", mappers);
        final MappingTimer mappingTimer = MappingTimer.inbound(connectionId, connectionType);
        final var mappingOutcomes = mappingTimer.overall(() -> mappers.stream()
                .flatMap(mapper -> runMapper(mapper, tracedMessage, mappingTimer))
                .collect(Collectors.toList())
        );
        messageSpanPair.second().finish();
        return mappingOutcomes;
    }

    private Pair<ExternalMessage, Span> traceExternalMessage(ExternalMessage message, String spanOperation) {
        final Tracer tracer = GlobalTracer.get();
        final SpanContext spanContext = tracer.extract(Format.Builtin.TEXT_MAP, new TextMapAdapter(message.getHeaders()));
        final Span span = tracer.buildSpan(spanOperation)
                .asChildOf(spanContext)
                .withTag(TracingTags.CONNECTION_ID, connectionId.toString())
                .withTag(TracingTags.CONNECTION_TYPE, connectionType.getName())
                .start();

        final Map<String, String> traceHeaders = new HashMap<>();
        tracer.inject(span.context(), Format.Builtin.TEXT_MAP, new TextMapAdapter(traceHeaders));
        final ExternalMessage tracedMessage = ExternalMessageFactory.newExternalMessageBuilder(message)
                .withAdditionalHeaders(traceHeaders)
                .build();
        return Pair.create(tracedMessage, span);
    }

    private Stream<MappingOutcome<MappedInboundExternalMessage>> runMapper(final MessageMapper mapper,
            final ExternalMessage rawMessage,
            final MappingTimer timer) {

        checkNotNull(rawMessage, "message");
        final Pair<ExternalMessage, Span> externalMessageSpanPair = traceExternalMessage(
                rawMessage, "mapper-" + mapper.getId());
        final ExternalMessage message = externalMessageSpanPair.first();
        final Span mapperSpan = externalMessageSpanPair.second();
        try {
            if (shouldMapMessageByContentType(message, mapper) && shouldMapMessageByConditions(message, mapper)) {
                logger.withCorrelationId(message.getInternalHeaders())
                        .debug("Mapping message using mapper {}.", mapper.getId());
                final List<Pair<Adaptable, Span>> tracedAdaptables = timer.payload(mapper.getId(), () -> mapper.map(message))
                        .stream()
                        .map(adaptable -> injectNewSpanAsChildOf(adaptable, mapperSpan))
                        .collect(Collectors.toList());

                if (isNullOrEmpty(tracedAdaptables)) {
                    mapperSpan.finish();
                    return Stream.of(MappingOutcome.dropped(mapper.getId(), message));
                } else {
                    final List<MappedInboundExternalMessage> mappedMessages = new ArrayList<>(tracedAdaptables.size());
                    for (final Pair<Adaptable,Span> tracedAdaptable : tracedAdaptables) {
                        final Adaptable adaptable = tracedAdaptable.first();
                        try {
                            final Signal<?> signal = timer.protocol(() -> protocolAdapter.fromAdaptable(adaptable));
                            dittoHeadersSizeChecker.check(signal.getDittoHeaders());
                            final DittoHeaders dittoHeaders = signal.getDittoHeaders();
                            final DittoHeaders headersWithMapper =
                                    dittoHeaders.toBuilder().inboundPayloadMapper(mapper.getId()).build();
                            final Signal<?> signalWithMapperHeader = signal.setDittoHeaders(headersWithMapper);
                            final MappedInboundExternalMessage mappedMessage =
                                    MappedInboundExternalMessage.of(message, adaptable.getTopicPath(),
                                            signalWithMapperHeader);
                            mappedMessages.add(mappedMessage);
                        } catch (final Exception e) {
                            mapperSpan
                                    .setTag(Tags.ERROR, true)
                                    .log(e.getMessage())
                                    .finish();
                            tracedAdaptables.stream().map(Pair::second).forEach(Span::finish);
                            return Stream.of(MappingOutcome.error(mapper.getId(),
                                    toDittoRuntimeException(e, mapper, adaptable.getDittoHeaders(), message),
                                    adaptable.getTopicPath(),
                                    message
                            ));
                        }
                    }
                    mapperSpan.finish();
                    final var mappingOutcomeStream = mappedMessages.stream()
                                    .map(mapped -> MappingOutcome.mapped(mapper.getId(), mapped, mapped.getTopicPath(),
                                            message));

                    tracedAdaptables.stream().map(Pair::second).forEach(Span::finish);
                    return mappingOutcomeStream;
                }
            } else {
                logger.withCorrelationId(message.getInternalHeaders())
                        .debug("Not mapping message with mapper <{}> as content-type <{}> was " +
                                        "blocked or MessageMapper conditions {} were not matched.",
                                mapper.getId(), message.findContentType(), mapper.getIncomingConditions());
                return Stream.of(MappingOutcome.dropped(mapper.getId(), message));
            }
        } catch (final Exception e) {
            mapperSpan
                    .setTag(Tags.ERROR, true)
                    .log(e.getMessage())
                    .finish();
            return Stream.of(MappingOutcome.error(mapper.getId(), toDittoRuntimeException(e, mapper,
                    resolveDittoHeadersBestEffort(message), message), null, message));
        }
    }

    private static Pair<Adaptable, Span> injectNewSpanAsChildOf(Adaptable adaptable, Span span) {
        final Tracer tracer = GlobalTracer.get();
        final Span mappedMessageSpan = tracer.buildSpan("mapped-" + adaptable.getClass().getSimpleName())
                .asChildOf(span.context())
                .start();

        final Adaptable tracedAdaptable =
                adaptable.setDittoHeaders(TracingHelper.injectSpanContext(tracer, mappedMessageSpan.context(),
                        adaptable.getDittoHeaders()));
        return Pair.create(tracedAdaptable, mappedMessageSpan);
    }

    private DittoHeaders resolveDittoHeadersBestEffort(final ExternalMessage message) {
        final DittoHeadersBuilder<?, ?> headersBuilder = DittoHeaders.newBuilder();
        message.getHeaders().forEach((key, value) -> {
            try {
                headersBuilder.putHeader(key, value);
                if (key.equals("device_id")) {
                    // this is kind of a workaround to preserve "best effort" an entityId by a special header value
                    // whenever the device connectivity layer sends the "entity id" in this header
                    headersBuilder.putHeader(DittoHeaderDefinition.ENTITY_ID.getKey(), value);
                }
            } catch (final Exception e) {
                // ignore this single invalid header
                logger.info("Putting a (protocol) header resulted in an exception: {} - {}",
                        e.getClass().getSimpleName(), e.getMessage());
            }
        });
        return headersBuilder.build();
    }

    private static DittoRuntimeException toDittoRuntimeException(final Throwable error,
            final MessageMapper mapper,
            final DittoHeaders bestEffortHeaders,
            final ExternalMessage message) {

        final DittoRuntimeException dittoRuntimeException = DittoRuntimeException.asDittoRuntimeException(error, e ->
                buildMappingFailedException("inbound",
                        message.findContentType().orElse(""),
                        mapper.getId(),
                        bestEffortHeaders,
                        e)
        );

        if (error instanceof WithDittoHeaders) {
            final DittoHeaders existingHeaders = ((WithDittoHeaders<?>) error).getDittoHeaders();
            final DittoHeaders mergedHeaders = bestEffortHeaders.toBuilder().putHeaders(existingHeaders)
                    .build();
            return dittoRuntimeException.setDittoHeaders(mergedHeaders);
        } else {
            return dittoRuntimeException.setDittoHeaders(bestEffortHeaders);
        }
    }

    private static boolean shouldMapMessageByContentType(final ExternalMessage message, final MessageMapper mapper) {
        return message.findContentType()
                .map(filterByContentTypeBlocklist(mapper))
                .orElse(true);
    }

    private boolean shouldMapMessageByConditions(final ExternalMessage message, final MessageMapper mapper) {
        return resolveConditions(mapper.getIncomingConditions().values(),
                Resolvers.forExternalMessage(message, connectionId));
    }

    private static Function<String, Boolean> filterByContentTypeBlocklist(final MessageMapper mapper) {
        return contentType -> !mapper.getContentTypeBlocklist().contains(contentType);
    }

    private static boolean isNullOrEmpty(@Nullable final Collection<?> messages) {
        return messages == null || messages.isEmpty();
    }

}
