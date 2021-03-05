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
package org.eclipse.ditto.services.things.starter;

import org.eclipse.ditto.services.base.actors.AbstractDittoRootActorTest;
import org.eclipse.ditto.services.things.common.config.DittoThingsConfig;
import org.eclipse.ditto.services.things.common.config.ThingsConfig;
import org.eclipse.ditto.services.utils.config.DefaultScopedConfig;

import akka.actor.ActorSystem;
import akka.actor.Props;

/**
 * Tests {@link org.eclipse.ditto.services.things.starter.ThingsRootActor}.
 */
public final class ThingsRootActorTest extends AbstractDittoRootActorTest {

    @Override
    protected String serviceName() {
        return "things";
    }

    @Override
    protected Props getRootActorProps(final ActorSystem system) {
        final ThingsConfig config =
                DittoThingsConfig.of(DefaultScopedConfig.dittoScoped(system.settings().config()));
        return ThingsRootActor.props(config, system.deadLetters(),
                DefaultThingPersistenceActorPropsFactory.of(system.deadLetters()));
    }
}