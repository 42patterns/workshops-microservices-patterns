package de.tdlabs.examples.keycloak;

import org.jboss.resteasy.plugins.server.servlet.HttpServlet30Dispatcher;
import org.jboss.resteasy.plugins.server.servlet.ResteasyContextParameters;
import org.keycloak.services.filters.KeycloakSessionServletFilter;
import org.keycloak.services.listeners.KeycloakSessionDestroyListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.autoconfigure.web.EmbeddedServletContainerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

@SpringBootApplication(exclude = LiquibaseAutoConfiguration.class)
@EnableConfigurationProperties(KeycloakServerProperties.class)
public class KeycloakStarter {

    private final static Logger logger = LoggerFactory.getLogger(KeycloakStarter.class);
    private final static String KEYCLOAK_APPLICATION_NAME = "embeddedKeycloakApplication";
    public static void main(String[] args) {
        SpringApplication.run(KeycloakStarter.class, args);
    }

    @Bean
    ServletRegistrationBean keycloakJaxRsApplication() {
        ServletRegistrationBean servlet = new ServletRegistrationBean(new HttpServlet30Dispatcher());
        servlet.setName(KEYCLOAK_APPLICATION_NAME);
        servlet.addInitParameter("keycloak.embedded", "true");
        servlet.addInitParameter("javax.ws.rs.Application", EmbeddedKeycloakApplication.class.getName());
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_SERVLET_MAPPING_PREFIX, "/");
        servlet.addInitParameter(ResteasyContextParameters.RESTEASY_USE_CONTAINER_FORM_PARAMS, "true");
        servlet.addUrlMappings("/*");
        servlet.setLoadOnStartup(1);
        servlet.setAsyncSupported(true);

        return servlet;
    }

    @Bean
    ServletListenerRegistrationBean<KeycloakSessionDestroyListener> keycloakSessionDestroyListener() {
        return new ServletListenerRegistrationBean<>(new KeycloakSessionDestroyListener());
    }

    @Bean
    FilterRegistrationBean keycloakSessionManagement() {
        FilterRegistrationBean filter = new FilterRegistrationBean();
        filter.setName("Keycloak Session Management");
        filter.setFilter(new KeycloakSessionServletFilter());
        filter.addUrlPatterns("/*");

        return filter;
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> onApplicationReadyEventListener(ServerProperties serverProperties) {
        return evt -> {
            ServletContext context = evt.getApplicationContext().getBean(ServletContext.class);
            Object initializeFlag = context.getAttribute(EmbeddedKeycloakApplication.KEYCLOAK_INITIALIZED_FLAG);
            if (initializeFlag == null) {
                logger.error("Could not start Embedded Keycloak server");
                evt.getApplicationContext().close();
            }

            Integer port = serverProperties.getPort();
            String rootContextPath = serverProperties.getContextPath();

            logger.info("Embedded Keycloak started: http://localhost:{}{} to use keycloak", port, rootContextPath);
        };
    }

}
