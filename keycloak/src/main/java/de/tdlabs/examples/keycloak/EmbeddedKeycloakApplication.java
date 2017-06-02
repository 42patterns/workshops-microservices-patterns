package de.tdlabs.examples.keycloak;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StreamUtils;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.ws.rs.core.Context;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class EmbeddedKeycloakApplication extends KeycloakApplication {

    public final static String KEYCLOAK_INITIALIZED_FLAG = "keycloak-initialized-flag";
    private final static Logger logger = LoggerFactory.getLogger(EmbeddedKeycloakApplication.class);
    private final KeycloakServerProperties keycloakServerProperties;

    public EmbeddedKeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) throws IOException {
        super(context, dispatcher);

        WebApplicationContext springCtx = WebApplicationContextUtils.getWebApplicationContext(context);
        keycloakServerProperties = springCtx.getBean(KeycloakServerProperties.class);

        tryCreateMasterRealmAdminUser();
        tryImportExistingKeycloakFile();

        context.setAttribute(KEYCLOAK_INITIALIZED_FLAG, true);
    }

    private void tryImportExistingKeycloakFile() throws IOException {
        KeycloakSession session = getSessionFactory().create();

        Optional<InputStream> configuration = Optional.empty();
        Path maybeFile = Paths.get(keycloakServerProperties.getConfigurationFile());
        if (Files.exists(maybeFile)) {
            logger.info("Config file {} exists. Performing import", maybeFile);
            configuration = Optional.of(Files.newInputStream(maybeFile));
        } else {
            ResourceLoader loader = new DefaultResourceLoader();
            Resource resource = loader.getResource(keycloakServerProperties.getConfigurationFile());
            if (resource.exists()) {
                logger.info("Config file {} exists. Performing import", resource.getURL());
                configuration = Optional.of(resource.getInputStream());
            } else {
                logger.warn("Couldn't extract configuration file to import.");
            }
        }
        if (configuration.isPresent()) {
            Path keycloakTempDir = Files.createTempDirectory("keycloak");
            Path tempFile = Files.createTempFile(keycloakTempDir, "keycloak-users", ".json");
            int bytesCopied = StreamUtils.copy(configuration.get(), Files.newOutputStream(tempFile));

            if (bytesCopied > 0) {
                ExportImportConfig.setAction("import");
                ExportImportConfig.setProvider("singleFile");
                ExportImportConfig.setFile(tempFile.toString());

                ExportImportManager manager = new ExportImportManager(session);
                manager.runImport();
            } else {
                logger.warn("Couldn't extract configuration file to import.");
            }
        } else {
            logger.warn("Configuration file {} doesn't exist.", maybeFile);
        }
    }

    private void tryCreateMasterRealmAdminUser() {

        KeycloakSession session = getSessionFactory().create();

        ApplianceBootstrap applianceBootstrap = new ApplianceBootstrap(session);
        String adminUsername = keycloakServerProperties.getAdminUsername();
        String adminPassword = keycloakServerProperties.getAdminPassword();

        try {

            session.getTransactionManager().begin();
            applianceBootstrap.createMasterRealmUser(adminUsername, adminPassword);
            session.getTransactionManager().commit();
        } catch (Exception ex) {
            System.out.println("Couldn't create keycloak master admin user: " + ex.getMessage());
            session.getTransactionManager().rollback();
        }

        session.close();
    }

}
