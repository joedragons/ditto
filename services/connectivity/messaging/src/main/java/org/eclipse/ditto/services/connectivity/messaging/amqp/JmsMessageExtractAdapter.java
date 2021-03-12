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
import java.util.Map.Entry;
import java.util.Objects;

import org.apache.qpid.jms.message.JmsMessage;

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

    /**
     * Creates an adapter for a dittoHeaders.
     *
     * @param jmsMessage The jmsMessage.
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    public JmsMessageExtractAdapter(final JmsMessage jmsMessage) {
        this.jmsMessage = Objects.requireNonNull(jmsMessage);
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {

        final String traceId = (String) jmsMessage.getFacade().getTracingAnnotation(TRACE_KEY);

        if (traceId == null) {
            return Collections.emptyIterator();
        }
        return Collections.singletonMap(TRACE_KEY, traceId).entrySet().iterator();
    }

    @Override
    public void put(final String key, final String value) {
        throw new UnsupportedOperationException();
    }

    public static SpanContext extractSpanContext(final Tracer tracer, final JmsMessage jmsMessage) {

        Objects.requireNonNull(tracer);
        Objects.requireNonNull(jmsMessage);

        return tracer.extract(Format.Builtin.TEXT_MAP, new JmsMessageExtractAdapter(jmsMessage));
    }
}
