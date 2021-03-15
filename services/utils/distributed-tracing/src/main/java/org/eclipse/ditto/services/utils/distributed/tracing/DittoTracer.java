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
 package org.eclipse.ditto.services.utils.distributed.tracing;

 import org.eclipse.ditto.services.utils.distributed.tracing.config.DistributedTracingConfig;

 import io.jaegertracing.Configuration;
 import io.opentracing.util.GlobalTracer;

 public final class DittoTracer {

     private DittoTracer(){

     }

     public static void registerGlobalTracer(DistributedTracingConfig distributedTracingConfig, String serviceName) {
         GlobalTracer.registerIfAbsent(() -> {
             final Configuration.SenderConfiguration sender = new Configuration.SenderConfiguration()
                     .withEndpoint(distributedTracingConfig.getJaegerCollectorEndpoint());

             final Configuration.ReporterConfiguration reporterConfiguration = new Configuration.ReporterConfiguration()
                     .withSender(sender);

             final Configuration.SamplerConfiguration constSampler = new Configuration.SamplerConfiguration()
                     .withType(distributedTracingConfig.getSampler())
                     .withParam(distributedTracingConfig.getSampleParam());
             Configuration configuration = new Configuration("ditto-" + serviceName)
                     .withReporter(reporterConfiguration)
                     .withSampler(constSampler);
             return configuration.getTracer();
         });
     }

 }
