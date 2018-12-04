package com.example.apigateway.gateway;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@SpringBootApplication
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
    RestTemplate rest() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setReadTimeout(15000);
        factory.setConnectTimeout(1500);

        return new RestTemplate(factory);
    }

    @Bean
    Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(40);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("ApiGatewayLookup-");
        executor.initialize();
        return executor;
    }

    @Bean
    byte[] defaultBanner() throws IOException {
        InputStream stream = GatewayApp.class.getResource("/default-banner.png").openStream();
        return StreamUtils.copyToByteArray(stream);
    }

}

@RestController
class Controllers {

    @Autowired
    RestTemplate rest;

    @Autowired
    Executor exec;

    @Autowired
    byte[] defaultBanner;

    @RequestMapping(value = "/banners", produces = MediaType.IMAGE_PNG_VALUE)
    public CompletableFuture<byte[]> getBanners() {
        final String bannersUrl = "http://localhost:8081/";

        //TODO: on failure return the default banner: "default-banner.png"
        // from defaultBanner field

        return CompletableFuture.supplyAsync(() -> rest.getForObject(bannersUrl, byte[].class), exec);
    }

    @RequestMapping(value = "/api/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public CompletableFuture<ResponseEntity<String>> legacyTodos(@RequestBody(required = false) String payload,
                                                                final HttpServletRequest request) {
        final String legacyApiUrl = "http://localhost:8080/";

        final String method = request.getMethod();
        final String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        return CompletableFuture.supplyAsync(() -> {
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(payload, headers);

            return rest.exchange(legacyApiUrl + path, HttpMethod.resolve(method), entity, String.class);
        }, exec);
    }

}
