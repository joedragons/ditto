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

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Objects;

import org.eclipse.ditto.model.base.headers.DittoHeadersBuilder;

import io.opentracing.propagation.TextMap;

/**
 * An adapter for injecting properties into DittoHeaders.
 */
public class DittoHeadersInjectAdapter implements TextMap {

    private final DittoHeadersBuilder dittoHeadersBuilder;

    /**
     * Creates an adapter for DittoHeaders.
     *
     * @param dittoHeadersBuilder The dittoHeaders.
     * @throws NullPointerException if any of the parameters are {@code null}.
     */
    public DittoHeadersInjectAdapter(final DittoHeadersBuilder dittoHeadersBuilder) {
        this.dittoHeadersBuilder = Objects.requireNonNull(dittoHeadersBuilder);
    }

    @Override
    public Iterator<Entry<String, String>> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void put(final String key, final String value) {
        dittoHeadersBuilder.putHeader(key, value);
    }

}
