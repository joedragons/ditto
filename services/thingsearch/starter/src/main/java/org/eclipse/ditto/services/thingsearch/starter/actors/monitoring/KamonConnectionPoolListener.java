/*
 * Copyright (c) 2017 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 * Contributors:
 *    Bosch Software Innovations GmbH - initial contribution
 *
 */
package org.eclipse.ditto.services.thingsearch.starter.actors.monitoring;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import com.mongodb.connection.ServerId;
import com.mongodb.event.ConnectionAddedEvent;
import com.mongodb.event.ConnectionCheckedInEvent;
import com.mongodb.event.ConnectionCheckedOutEvent;
import com.mongodb.event.ConnectionPoolClosedEvent;
import com.mongodb.event.ConnectionPoolListener;
import com.mongodb.event.ConnectionPoolOpenedEvent;
import com.mongodb.event.ConnectionPoolWaitQueueEnteredEvent;
import com.mongodb.event.ConnectionPoolWaitQueueExitedEvent;
import com.mongodb.event.ConnectionRemovedEvent;

import kamon.Kamon;
import scala.concurrent.duration.FiniteDuration;

/**
 * Reports MongoDB connection pool statistics to Kamon.
 */
public class KamonConnectionPoolListener implements ConnectionPoolListener {

    private final ConcurrentMap<ServerId, PoolMetric> metrics = new ConcurrentHashMap<>();

    @Override
    public void connectionPoolOpened(final ConnectionPoolOpenedEvent event) {
        final PoolMetric metric = new PoolMetric(event.getServerId());
        metrics.put(event.getServerId(), metric);
    }

    @Override
    public void connectionPoolClosed(final ConnectionPoolClosedEvent event) {
        final PoolMetric metric = metrics.remove(event.getServerId());
        if (metric != null) {
            metric.remove();
        }
    }

    @Override
    public void connectionCheckedOut(final ConnectionCheckedOutEvent event) {
        final PoolMetric metric = metrics.get(event.getConnectionId().getServerId());
        if (metric != null) {
            metric.checkedOutCount.incrementAndGet();
        }
    }

    @Override
    public void connectionCheckedIn(final ConnectionCheckedInEvent event) {
        final PoolMetric metric = metrics.get(event.getConnectionId().getServerId());
        if (metric != null) {
            metric.checkedOutCount.decrementAndGet();
        }
    }

    @Override
    public void waitQueueEntered(final ConnectionPoolWaitQueueEnteredEvent event) {
        final PoolMetric metric = metrics.get(event.getServerId());
        if (metric != null) {
            metric.waitQueueSize.incrementAndGet();
        }
    }

    @Override
    public void waitQueueExited(final ConnectionPoolWaitQueueExitedEvent event) {
        final PoolMetric metric = metrics.get(event.getServerId());
        if (metric != null) {
            metric.waitQueueSize.decrementAndGet();
        }
    }

    @Override
    public void connectionAdded(final ConnectionAddedEvent event) {
        final PoolMetric metric = metrics.get(event.getConnectionId().getServerId());
        if (metric != null) {
            metric.poolSize.incrementAndGet();
        }
    }

    @Override
    public void connectionRemoved(final ConnectionRemovedEvent event) {
        final PoolMetric metric = metrics.get(event.getConnectionId().getServerId());
        if (metric != null) {
            metric.poolSize.decrementAndGet();
        }
    }

    private class PoolMetric {

        private static final String CONNECTION_POOL_PREFIX = "pool.";
        private static final String CHECKED_OUT_COUNT = ".checkedOutCount";
        private static final String POOL_SIZE = ".poolSize";
        private static final String WAIT_QUEUE_SIZE = ".waitQueueSize";

        private final AtomicLong poolSize = new AtomicLong();
        private final AtomicLong checkedOutCount = new AtomicLong();
        private final AtomicLong waitQueueSize = new AtomicLong();
        private final String name;

        private PoolMetric(final ServerId serverId) {
            this.name = serverId.getClusterId().getValue();
            Kamon.metrics()
                    .gauge(CONNECTION_POOL_PREFIX + name + POOL_SIZE, FiniteDuration.apply(30, TimeUnit.SECONDS),
                            poolSize::get);
            Kamon.metrics()
                    .gauge(CONNECTION_POOL_PREFIX + name + CHECKED_OUT_COUNT,
                            FiniteDuration.apply(30, TimeUnit.SECONDS), checkedOutCount::get);
            Kamon.metrics()
                    .gauge(CONNECTION_POOL_PREFIX + name + WAIT_QUEUE_SIZE, FiniteDuration.apply(30, TimeUnit.SECONDS),
                            waitQueueSize::get);
        }

        private void remove() {
            Kamon.metrics().removeGauge(CONNECTION_POOL_PREFIX + name + POOL_SIZE);
            Kamon.metrics().removeGauge(CONNECTION_POOL_PREFIX + name + CHECKED_OUT_COUNT);
            Kamon.metrics().removeGauge(CONNECTION_POOL_PREFIX + name + WAIT_QUEUE_SIZE);
        }
    }
}
