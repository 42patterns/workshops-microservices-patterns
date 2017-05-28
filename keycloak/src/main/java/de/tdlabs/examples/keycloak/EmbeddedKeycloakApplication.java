package de.tdlabs.examples.keycloak;

import org.jboss.resteasy.core.Dispatcher;
import org.keycloak.exportimport.ExportImportConfig;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.managers.ApplianceBootstrap;
import org.keycloak.services.resources.KeycloakApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.ws.rs.core.Context;
import java.io.File;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.Map;
import java.util.Set;

/**
 * Created by tom on 12.06.16.
 */
public class EmbeddedKeycloakApplication extends KeycloakApplication {

    private final static Logger logger = LoggerFactory.getLogger(EmbeddedKeycloakApplication.class);
    private final KeycloakServerProperties keycloakServerProperties;

    public EmbeddedKeycloakApplication(@Context ServletContext context, @Context Dispatcher dispatcher) {
        super(context, dispatcher);

        WebApplicationContext springCtx = WebApplicationContextUtils.getWebApplicationContext(context);
        keycloakServerProperties = springCtx.getBean(KeycloakServerProperties.class);

        tryCreateMasterRealmAdminUser();
        tryImportExistingKeycloakFile();
    }

    private void tryImportExistingKeycloakFile() {
        KeycloakSession session = getSessionFactory().create();

        String path = getClass().getResource(keycloakServerProperties.getUsersConfigurationFile()).getPath();
        if (new File(path).exists()) {
            logger.info("Configuration file {} exists. Importing users.", path);

            ExportImportConfig.setAction("import");
            ExportImportConfig.setProvider("singleFile");
            ExportImportConfig.setFile(path);

            ExportImportManager manager = new ExportImportManager(session);
            manager.runImport();
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
