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

 import org.eclipse.ditto.services.utils.config.WithConfigPath;

 public interface DistributedTracingConfig {

     String getJaegerCollectorEndpoint();

     String getSampler();

     int getSampleParam();

     enum DistributedTracingConfigValue implements WithConfigPath {

         /**
          * Hostname of the jaeger agent.
          */
         JAEGER_ENDPOINT("jaeger.collector.endpoint"),

         /**
          * The type of the sampler, e.g. 'const'
          */
         SAMPLER_TYPE("sampler.type"),


         /**
          * The parameter for the sampler, e.g. sampler=const and param=1 for capturing all traces.
          */
         SAMPLER_PARAM("sampler.param");

         private final String path;

         private DistributedTracingConfigValue(final String thePath) {
             path = thePath;
         }

         @Override
         public String getConfigPath() {
             return path;
         }

     }
 }
