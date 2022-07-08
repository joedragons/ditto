/*
 * Copyright (c) 2022 Contributors to the Eclipse Foundation
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
package org.eclipse.ditto.edge.service.dispatching;

import static org.eclipse.ditto.base.model.common.ConditionChecker.checkNotNull;

import org.eclipse.ditto.base.service.DittoExtensionIds;
import org.eclipse.ditto.base.service.DittoExtensionPoint;
import org.eclipse.ditto.internal.utils.config.ScopedConfig;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.AbstractActor.Receive;
import akka.actor.ActorContext;
import akka.actor.ActorSystem;

/**
 * This extension allows to extend commands that are forwarded from the edges of Ditto to other microservices by
 * handling them in the {@link Receive} provided by {@link #getReceiveExtension(ActorContext)}.
 */
public interface EdgeCommandForwarderExtension extends DittoExtensionPoint {

    /**
     * Loads the implementation of {@code RootChildActorStarter} which is configured for the
     * {@code ActorSystem}.
     *
     * @param actorSystem the actorSystem in which the {@code RootChildActorStarter} should be loaded.
     * @return the {@code RootChildActorStarter} implementation.
     * @throws NullPointerException if {@code actorSystem} is {@code null}.
     */
    static EdgeCommandForwarderExtension get(final ActorSystem actorSystem) {
        checkNotNull(actorSystem, "actorSystem");
        final var extensionIdConfig = ExtensionId.computeConfig(actorSystem);
        return DittoExtensionIds.get(actorSystem)
                .computeIfAbsent(extensionIdConfig, ExtensionId::new)
                .get(actorSystem);
    }

    /**
     * Builds the receive extension to allow custom handling of messages.
     * This receive will be applied BEFORE the default receives of {@link EdgeCommandForwarderActor}, so it's possible
     * to overwrite the default handling.
     *
     * @param actorContext can be used for example to determine the original sender of a message.
     * @return The desired receive extension.
     */
    Receive getReceiveExtension(ActorContext actorContext);

    final class ExtensionId extends DittoExtensionPoint.ExtensionId<EdgeCommandForwarderExtension> {

        private static final String CONFIG_KEY = "edge-command-forwarder-extension";
        private static final String CONFIG_PATH = "ditto.extensions." + CONFIG_KEY;

        private ExtensionId(final ExtensionIdConfig<EdgeCommandForwarderExtension> extensionIdConfig) {
            super(extensionIdConfig);
        }

        static ExtensionIdConfig<EdgeCommandForwarderExtension> computeConfig(final ActorSystem actorSystem) {
            final Config extensions = ScopedConfig.getOrEmpty(actorSystem.settings().config(), "ditto.extensions");
            return ExtensionIdConfig.of(EdgeCommandForwarderExtension.class, extensions, CONFIG_KEY);
        }

        @Override
        protected String getConfigPath() {
            return CONFIG_PATH;
        }

    }

}
