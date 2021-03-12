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

import java.util.Collections;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.ditto.model.base.headers.DittoHeaders;

import io.opentracing.propagation.TextMap;

/**
 * An adapter for extracting properties from an AMQP 1.0 message's message annotations.
 */
public class DittoHeadersExtractAdapter implements TextMap {

    private final DittoHeaders dittoHeaders;

    /**
     * Creates an adapter for a dittoHeaders.
     *
     * @param dittoHeaders The dittoHeaders.
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    public DittoHeadersExtractAdapter(final DittoHeaders dittoHeaders) {
        this.dittoHeaders = Objects.requireNonNull(dittoHeaders);
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        if (dittoHeaders.isEmpty()) {
            return Collections.emptyIterator();
        }
        return dittoHeaders.entrySet().iterator();
    }

    @Override
    public void put(final String key, final String value) {
        throw new UnsupportedOperationException();
    }

}
