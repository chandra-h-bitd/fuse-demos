/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sample.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.zipkin.ZipkinTracer;

//only needed when talking to Hawkular
import com.github.kristofa.brave.http.HttpSpanCollector;
import com.github.kristofa.brave.EmptySpanCollectorMetricsHandler;


@Component
public class Service1Route extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // create zipkin
        ZipkinTracer zipkin = new ZipkinTracer();

        // for talking to Zipkin server
        // zipkin.setEndpoint("http://localhost:9411/api/v2/spans");

        // for talking to Hawkular
        zipkin.setEndpoint("http://localhost:8080/api/v1/spans");
        zipkin.setIncludeMessageBody(true);
        zipkin.setIncludeMessageBodyStreams(true);
        zipkin.addServerServiceMapping("http://localhost:7070/service2", "service2");
        zipkin.setSpanCollector(HttpSpanCollector.create("http://localhost:8080", new EmptySpanCollectorMetricsHandler()));
        // end of talking to Hawkular

        // set the service name
        zipkin.setServiceName("service2");
        // capture 100% of all the events
        zipkin.setRate(1.0f);
        // include message bodies in the traces (not recommended for production)
        zipkin.setIncludeMessageBodyStreams(true);

        // add zipkin to CamelContext
        zipkin.init(getContext());

        from("jetty:http://0.0.0.0:{{service1.port}}/service1").routeId("service1").streamCaching()
        .removeHeaders("CamelHttp*")
        .log("Service1 request: ${body}")
        .delay(simple("${random(1000,2000)}"))
        .transform(simple("Service1-${body}"))
        .to("http://0.0.0.0:{{service2.port}}/service2")
        .log("Service1 response: ${body}");
    }

}
