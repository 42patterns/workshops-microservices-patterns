package de.tdlabs.examples.keycloak;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Created by tom on 12.06.16.
 */
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakServerProperties {

    private String adminUsername = "admin";

    private String adminPassword = "admin";

    private String usersConfigurationFile = "/keycloak-users-config.json";

    public String getAdminUsername() {
        return adminUsername;
    }

    public void setAdminUsername(String adminUsername) {
        this.adminUsername = adminUsername;
    }

    public String getAdminPassword() {
        return adminPassword;
    }

    public void setAdminPassword(String adminPassword) {
        this.adminPassword = adminPassword;
    }

    public String getUsersConfigurationFile() {
        return usersConfigurationFile;
    }

    public void setUsersConfigurationFile(String usersConfigurationFile) {
        this.usersConfigurationFile = usersConfigurationFile;
    }
}
