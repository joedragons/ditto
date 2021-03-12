/*
 * Copyright (c) 2021 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.services.connectivity.messaging.amqp;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.qpid.jms.message.JmsMessage;
import org.apache.qpid.proton.amqp.Symbol;
import org.eclipse.ditto.services.utils.akka.logging.ThreadSafeDittoLoggingAdapter;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.propagation.TextMap;

/**
 * An adapter for extracting properties from an AMQP 1.0 message's message annotations.
 */
public class JmsMessageExtractAdapter implements TextMap {

    private static final String TRACE_KEY = "uber-trace-id";
    private final JmsMessage jmsMessage;
    private final ThreadSafeDittoLoggingAdapter log;

    /**
     * Creates an adapter for a dittoHeaders.
     *
     * @param jmsMessage The jmsMessage.
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    public JmsMessageExtractAdapter(final JmsMessage jmsMessage,
            final ThreadSafeDittoLoggingAdapter log) {
        this.jmsMessage = Objects.requireNonNull(jmsMessage);
        this.log = log;
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {


        jmsMessage.getFacade().filterTracingAnnotations((k, v) -> {
            log.info("message annotation {} -> {} [{}]", k, v.getClass().getName(), v);
        });

        String traceId = null;
        try {
            final Object traceContext = jmsMessage.getFacade().getTracingAnnotation("x-opt-trace-context");

            log.info("x-opt-trace-context: {}", traceContext);
            if (traceContext != null) {
                log.info("x-opt-trace-context type: {}", traceContext.getClass());
                if (traceContext instanceof Map) {
                    final Map map = ((Map) traceContext);
                    map.forEach((k, v) -> {
                        log.info("trace context entry {} [{}] -> {} [{}]", k, k.getClass().getName(), v,
                                v.getClass().getName());
                    });
                    traceId = (String) map.get(Symbol.getSymbol(TRACE_KEY));
                }
            }
        } catch (Exception e) {
            traceId = null;
        }

        log.info("extracted traceId {}", traceId);

        if (traceId == null) {
            return Collections.emptyIterator();
        }
        return Collections.singletonMap(TRACE_KEY, traceId).entrySet().iterator();
    }

    @Override
    public void put(final String key, final String value) {
        throw new UnsupportedOperationException();
    }

    public static SpanContext extractSpanContext(final Tracer tracer, final JmsMessage jmsMessage,
            final ThreadSafeDittoLoggingAdapter log) {

        Objects.requireNonNull(tracer);
        Objects.requireNonNull(jmsMessage);

        return tracer.extract(Format.Builtin.TEXT_MAP, new JmsMessageExtractAdapter(jmsMessage, log));
    }
}
