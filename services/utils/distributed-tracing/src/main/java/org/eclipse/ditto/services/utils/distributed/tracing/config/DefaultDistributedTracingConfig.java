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
 package org.eclipse.ditto.services.utils.distributed.tracing.config;

 import static org.eclipse.ditto.services.utils.distributed.tracing.config.DistributedTracingConfig.DistributedTracingConfigValue.JAEGER_ENDPOINT;
 import static org.eclipse.ditto.services.utils.distributed.tracing.config.DistributedTracingConfig.DistributedTracingConfigValue.SAMPLER_PARAM;
 import static org.eclipse.ditto.services.utils.distributed.tracing.config.DistributedTracingConfig.DistributedTracingConfigValue.SAMPLER_TYPE;

 import org.eclipse.ditto.services.utils.config.DefaultScopedConfig;

 import com.typesafe.config.Config;

 public final class DefaultDistributedTracingConfig implements DistributedTracingConfig {

     private static final String CONFIG_PATH = "distributed-tracing";

     private final String collectorEndpoint;
     private final String sampler;
     private final int samplerParam;

     private DefaultDistributedTracingConfig(final DefaultScopedConfig distributedTracingScopedConfig) {
         collectorEndpoint = distributedTracingScopedConfig.getString(JAEGER_ENDPOINT.getConfigPath());
         sampler = distributedTracingScopedConfig.getString(SAMPLER_TYPE.getConfigPath());
         samplerParam = distributedTracingScopedConfig.getInt(SAMPLER_PARAM.getConfigPath());
     }

     /**
      * Returns an instance of {@code DefaultDistributedTracingConfig} based on the settings of the specified Config.
      *
      * @param config a raw config.
      * @return the instance.
      * @throws org.eclipse.ditto.services.utils.config.DittoConfigError if {@code config} is invalid.
      */
     public static DefaultDistributedTracingConfig of(final Config config) {
         final DefaultScopedConfig dittoScoped = DefaultScopedConfig.dittoScoped(config);
         return new DefaultDistributedTracingConfig(DefaultScopedConfig.newInstance(dittoScoped, CONFIG_PATH));
     }

     @Override
     public String getJaegerCollectorEndpoint() {
         return collectorEndpoint;
     }

     @Override
     public String getSampler() {
         return sampler;
     }

     @Override
     public int getSampleParam() {
         return samplerParam;
     }
 }
