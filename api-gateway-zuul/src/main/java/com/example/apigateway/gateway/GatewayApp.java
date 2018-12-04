package com.example.apigateway.gateway;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.route.ZuulFallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

@EnableZuulProxy
@SpringBootApplication
public class GatewayApp {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApp.class, args);
    }

    @Bean
    ZuulFilter writeFilter() {
        return new ZuulFilter() {
            @Override
            public String filterType() {
                return "pre";
            }

            @Override
            public int filterOrder() {
                return 0;
            }

            @Override
            public boolean shouldFilter() {
                RequestContext context = RequestContext.getCurrentContext();
                if (Arrays.asList("PUT", "POST").contains(context.getRequest().getMethod())) {
                    return true;
                }

                return false;
            }

            @Override
            public Object run() {
                RequestContext context = RequestContext.getCurrentContext();
                context.addZuulResponseHeader("X-Profanity", "true");
                context.set("serviceId", "profanity");
                context.setRouteHost(null);

                return null;
            }
        };
    }

    @Bean
    ZuulFilter zuulFilter(){
        //uncomment zuul.debug.request = true in configuration to unable rouingDebug dump
        return new ZuulFilter() {
            @Override
            public String filterType() {
                return "post";
            }

            @Override
            public int filterOrder() {
                return 999999;
            }

            @Override
            public boolean shouldFilter() {
                return true;
            }

            @Override
            public Object run() {
                final List<String> routingDebug = (List<String>) RequestContext.getCurrentContext().get("routingDebug");
                routingDebug.forEach(System.out::println);
                return null;
            }
        };
    }

    @Bean
    ZuulFallbackProvider fallbackProvider() throws URISyntaxException, IOException {
        final InputStream stream = GatewayApp.class.getResource("/default-banner.png").openStream();

        return new ZuulFallbackProvider() {
            @Override
            public String getRoute() {
                return "banners";
            }

            @Override
            public ClientHttpResponse fallbackResponse() {

                return new ClientHttpResponse() {
                    @Override
                    public HttpStatus getStatusCode() throws IOException {
                        return HttpStatus.OK;
                    }

                    @Override
                    public int getRawStatusCode() throws IOException {
                        return 200;
                    }

                    @Override
                    public String getStatusText() throws IOException {
                        return "OK";
                    }

                    @Override
                    public void close() {

                    }

                    @Override
                    public InputStream getBody() throws IOException {
                        return stream;
                    }

                    @Override
                    public HttpHeaders getHeaders() {
                        HttpHeaders headers = new HttpHeaders();
                        headers.setContentType(MediaType.IMAGE_PNG);
                        return headers;
                    }
                };
            }
        };
    }

}
