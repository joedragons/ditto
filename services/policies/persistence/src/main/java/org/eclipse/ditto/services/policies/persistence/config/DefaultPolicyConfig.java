/*
 * Copyright (c) 2017-2018 Bosch Software Innovations GmbH.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/index.php
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.ditto.services.policies.persistence.config;

import java.io.Serializable;

import javax.annotation.concurrent.Immutable;

import org.eclipse.ditto.services.base.config.supervision.DefaultSupervisorConfig;
import org.eclipse.ditto.services.base.config.supervision.SupervisorConfig;
import org.eclipse.ditto.services.utils.config.DefaultScopedConfig;
import org.eclipse.ditto.services.utils.config.ScopedConfig;
import org.eclipse.ditto.services.utils.persistence.mongo.config.ActivityCheckConfig;
import org.eclipse.ditto.services.utils.persistence.mongo.config.DefaultActivityCheckConfig;
import org.eclipse.ditto.services.utils.persistence.mongo.config.DefaultSnapshotConfig;
import org.eclipse.ditto.services.utils.persistence.mongo.config.SnapshotConfig;

import com.typesafe.config.Config;

/**
 * This class is the default implementation of the policy config.
 */
@Immutable
public final class DefaultPolicyConfig implements PolicyConfig, Serializable {

    private static final String CONFIG_PATH = "policy";

    private static final long serialVersionUID = 4885226519516590527L;

    private final SupervisorConfig supervisorConfig;
    private final ActivityCheckConfig activityCheckConfig;
    private final SnapshotConfig snapshotConfig;

    private DefaultPolicyConfig(final ScopedConfig scopedConfig) {
        supervisorConfig = DefaultSupervisorConfig.of(scopedConfig);
        activityCheckConfig = DefaultActivityCheckConfig.of(scopedConfig);
        snapshotConfig = DefaultSnapshotConfig.of(scopedConfig);
    }

    /**
     * Returns an instance of the policy config based on the settings of the specified Config.
     *
     * @param config is supposed to provide the settings of the policy config at {@value #CONFIG_PATH}.
     * @return the instance.
     * @throws org.eclipse.ditto.services.utils.config.DittoConfigError if {@code config} is invalid.
     */
    public static DefaultPolicyConfig of(final Config config) {
        return new DefaultPolicyConfig(DefaultScopedConfig.newInstance(config, CONFIG_PATH));
    }

    @Override
    public SupervisorConfig getSupervisorConfig() {
        return supervisorConfig;
    }

    @Override
    public ActivityCheckConfig getActivityCheckConfig() {
        return activityCheckConfig;
    }

    @Override
    public SnapshotConfig getSnapshotConfig() {
        return snapshotConfig;
    }

}
