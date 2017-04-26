package com.example.services;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ServerList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.netflix.ribbon.StaticServerList;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@EnableDiscoveryClient
//@RibbonClient(value = "profanity-check-service", configuration = ProfanityServiceCfg.class)
public class ProfanityProxy {

    public static void main(String[] args) {
        SpringApplication.run(ProfanityProxy.class, args);
    }

    @LoadBalanced
    @Bean
    RestTemplate rest() {
        return new RestTemplate();
    }

}

//class ProfanityServiceCfg {
//
//    @Bean
//    public ServerList<Server> ribbonServerList() {
//         return new StaticServerList<>(new Server("localhost", 19990));
//    }
//
//}

@Component
@RestController
class Controllers {

    @Autowired
    RestTemplate rest;

    private ConcurrentHashMap<ObjectNode, ObjectNode> cache = new ConcurrentHashMap<>();

    @RequestMapping(method = {RequestMethod.PUT, RequestMethod.POST},
            value = "/api/**", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> foo(@RequestBody(required = false) ObjectNode payload,
                                      final HttpServletRequest request) {

        ObjectNode newPayload = cache.computeIfAbsent(payload, k -> maskProfanityWords(k));

        final String legacyApiUrl = "http://legacy/";
        final String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);

        URI uri = UriComponentsBuilder.fromHttpUrl(legacyApiUrl)
                .path(path).build().toUri();

        String method = request.getMethod();
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(newPayload.toString(), headers);

        return rest.exchange(uri, HttpMethod.resolve(method), entity, String.class);
    }

    public ObjectNode maskProfanityWords(ObjectNode payload) {
//        Hardcoded profanity-check URL
//        https://micro-patterns-profanity.herokuapp.com/checkprofanity?text=This%20is%20shit
        URI uri = UriComponentsBuilder.fromHttpUrl("http://micro-patterns-profanity.herokuapp.com/")
                .path("checkprofanity")
                .queryParam("text", payload.at("/title").asText())
                .build().toUri();

        RestTemplate rest = new RestTemplate();
        ResponseEntity<Profanity> profanityResponse = rest.getForEntity(uri, Profanity.class);
        Profanity isSwearWord = profanityResponse.getBody();

        if (isSwearWord.containsProfanity == true) {
            payload.put("title", isSwearWord.output);
        } else {
            payload.put("title", isSwearWord.input);
        }

        return payload;
    }

}

//{"containsProfanity":true,"input":"This is shit","output":"This is ****"}
class Profanity {
    public final boolean containsProfanity;
    public final String input;
    public final String output;

    @JsonCreator
    Profanity(@JsonProperty("containsProfanity") boolean containsProfanity,
              @JsonProperty("input") String input,
              @JsonProperty("output") String output) {
        this.containsProfanity = containsProfanity;
        this.input = input;
        this.output = output;
    }
}