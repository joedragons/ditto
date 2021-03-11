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
package org.eclipse.ditto.model.base.tracing;

import java.util.Objects;

import org.eclipse.ditto.model.base.headers.DittoHeaders;
import org.eclipse.ditto.model.base.headers.DittoHeadersBuilder;

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.noop.NoopSpanContext;
import io.opentracing.propagation.Format;

public class TracingHelper {

    /**
     * Injects a {@code SpanContext} into {@code DittoHeaders}.
     *
     * @param tracer The Tracer to use for injecting the context.
     * @param spanContext The context to inject or {@code null} if no context is available.
     * @param dittoHeaders The {@code DittoHeaders} object to inject the context into.
     * @throws NullPointerException if tracer or dittoHeaders is {@code null}.
     */
    public static DittoHeaders injectSpanContext(final Tracer tracer, final SpanContext spanContext,
            final DittoHeaders dittoHeaders) {

        Objects.requireNonNull(tracer);
        Objects.requireNonNull(dittoHeaders);

        if (spanContext != null && !(spanContext instanceof NoopSpanContext)) {
            final DittoHeadersBuilder dittoHeadersBuilder = dittoHeaders.toBuilder();
            tracer.inject(spanContext, Format.Builtin.TEXT_MAP, new DittoHeadersInjectAdapter(dittoHeadersBuilder));
            return dittoHeadersBuilder.build();
        } else {
            return dittoHeaders;
        }
    }

    /**
     * Extracts a {@code SpanContext} from {@code DittoHeaders}.
     * <p>
     * The span context will be read from the message annotations of the given message.
     *
     * @param tracer The Tracer to use for extracting the context.
     * @param dittoHeaders The {@code DittoHeaders} to extract the context from.
     * @return The context or {@code null} if the given {@code DittoHeaders} does not contain a context.
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    public static SpanContext extractSpanContext(final Tracer tracer, final DittoHeaders dittoHeaders) {

        Objects.requireNonNull(tracer);
        Objects.requireNonNull(dittoHeaders);

        return tracer.extract(Format.Builtin.TEXT_MAP, new DittoHeadersExtractAdapter(dittoHeaders));
    }

}
