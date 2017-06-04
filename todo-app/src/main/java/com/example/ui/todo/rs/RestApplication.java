package com.example.ui.todo.rs;

import brave.Tracing;
import brave.http.HttpTracing;
import brave.jaxrs2.TracingFeature;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.Encoding;
import zipkin.reporter.urlconnection.URLConnectionSender;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@ApplicationPath("/api")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        return Collections.singleton(TodoResource.class);
    }

    @Override
    public Set<Object> getSingletons() {
        URLConnectionSender sender = URLConnectionSender.builder()
                .encoding(Encoding.JSON)
                .endpoint("http://localhost:9411/api/v1/spans")
                .build();

        Tracing tracing = Tracing.newBuilder()
                .localServiceName("legacy")
                .reporter(AsyncReporter.builder(sender).build())
                .build();

        return new LinkedHashSet<>(Arrays.asList(TracingFeature.create(tracing)));
    }
}
