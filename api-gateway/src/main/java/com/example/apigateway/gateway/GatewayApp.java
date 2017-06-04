package com.example.apigateway.gateway;

import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.sleuth.Sampler;
import org.springframework.cloud.sleuth.instrument.async.LazyTraceExecutor;
import org.springframework.cloud.sleuth.instrument.async.TraceableExecutorService;
import org.springframework.cloud.sleuth.sampler.AlwaysSampler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.*;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApp {

    /*
        * Authentication
        * Failure handling
        * Simple load-balancing
        * Simple heart-beat check
    */

    public static void main(String[] args) {
        SpringApplication.run(GatewayApp.class, args);
    }

    @Bean
    @LoadBalanced
    RestTemplate rest() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(15000);
        factory.setConnectTimeout(1500);

        return new RestTemplate(factory);
    }

    @Bean
    ExecutorService asyncExecutor() {
        return Executors.newFixedThreadPool(4, new CustomizableThreadFactory("ApiGateway-"));
    }

    @Bean
    byte[] defaultBanner() throws IOException {
        InputStream stream = GatewayApp.class.getResource("/default-banner.png").openStream();
        return StreamUtils.copyToByteArray(stream);
    }

}

@Configuration
class TracesConfig {

    @Bean
    public Sampler defaultSampler() {
        return new AlwaysSampler();
    }

    @Bean
    public TraceableExecutorService traceExecutor(BeanFactory bf, ExecutorService exec) {
        return new TraceableExecutorService(bf, exec);
    }

}

@RestController
class Controllers {

    @Autowired
    RestTemplate rest;

    @Autowired
    TraceableExecutorService exec;

    LazyTraceExecutor e;

    @Autowired
    byte[] defaultBanner;

    @RequestMapping(value = "/banners", produces = MediaType.IMAGE_PNG_VALUE)
    public CompletableFuture<ResponseEntity<byte[]>> getBanners() {
        final String bannersUrl = "http://banners/";

        final RetryPolicy rt = new RetryPolicy()
                .retryOn(Exception.class)
                .withDelay(10, TimeUnit.SECONDS)
                .withMaxRetries(2);

        return shiftbyte(exec.submit(() ->
                Failsafe.with(rt)
                        .withFallback(defaultBanner)
                        .get(() -> rest.getForEntity(bannersUrl, byte[].class))));
    }

    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST},
            value = "/api/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> profanityProxy(@RequestBody(required = false) String payload,
                                                                    final HttpServletRequest request) {

        final String profanityProxy = "http://profanity/";
        final String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        URI uri = UriComponentsBuilder.fromHttpUrl(profanityProxy)
                .path(path).build().toUri();

        return makeCall(uri, payload.toString(), request);
    }

    @RequestMapping(value = "/api/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> legacyTodos(@RequestBody(required = false) String payload,
                                                                 final HttpServletRequest request) {

        final String legacyApiUrl = "http://legacy/";

        final String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        URI uri = UriComponentsBuilder.fromHttpUrl(legacyApiUrl)
                .path(path).build().toUri();


        return makeCall(uri, payload, request);
    }

    private CompletableFuture<ResponseEntity<String>> makeCall(URI uri, String payload, final HttpServletRequest request) {
        final String method = request.getMethod();

        return shift(exec.submit(() -> {

            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);
            return rest.exchange(uri, HttpMethod.resolve(method), entity, String.class);
        }));
    }

    private static CompletableFuture<ResponseEntity<byte[]>> shiftbyte(Future<ResponseEntity<byte[]>> f) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static CompletableFuture<ResponseEntity<String>> shift(Future<ResponseEntity<String>> f) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
