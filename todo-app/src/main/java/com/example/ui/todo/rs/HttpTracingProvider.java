package com.example.ui.todo.rs;

import brave.Tracing;
import brave.http.HttpTracing;
import brave.sampler.Sampler;
import zipkin.Endpoint;
import zipkin.reporter.AsyncReporter;
import zipkin.reporter.Encoding;
import zipkin.reporter.urlconnection.URLConnectionSender;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class HttpTracingProvider {

    @Produces
    public HttpTracing tracing() {
        Tracing tracing = Tracing.newBuilder()
                .localServiceName("foo-tracing")
                .build();

        return HttpTracing.create(tracing);

    }

}
