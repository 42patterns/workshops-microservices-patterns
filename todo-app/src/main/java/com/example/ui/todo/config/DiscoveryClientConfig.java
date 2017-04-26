package com.example.ui.todo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.concurrent.ThreadLocalRandom;

@Singleton
@Startup
public class DiscoveryClientConfig {

    private static Logger log = LoggerFactory.getLogger(DiscoveryClientConfig.class);

    private static final String APPLICATION_NAME = "legacy";
    private static final String REGISTRATION_XML = "<instance>\n" +
            "  <instanceId>{0}</instanceId>\n" +
            "  <hostName>localhost</hostName>\n" +
            "  <app>{2}</app>\n" +
            "  <ipAddr>{1}</ipAddr>\n" +
            "  <vipAddress>{2}</vipAddress>\n" +
            "  <secureVipAddress>{2}</secureVipAddress>\n" +
            "  <status>UP</status>\n" +
            "  <port>8080</port>\n" +
            "  <securePort>745</securePort>\n" +
            "  <homePageUrl>http://{3}</homePageUrl>\n" +
            "  <statusPageUrl>http://{3}/status</statusPageUrl>\n" +
            "  <healthCheckUrl>http://{3}/info</healthCheckUrl>\n" +
            "  <dataCenterInfo>\n" +
            "    <name>MyOwn</name>\n" +
            "  </dataCenterInfo>\n" +
            "  <leaseInfo>\n" +
            "    <evictionDurationInSecs>3600</evictionDurationInSecs>\n" +
            "  </leaseInfo>\n" +
            "</instance>";
    private static final String EUREKA_HOST = "http://localhost:8761/";
    private String instanceId;
    private String ipAddress;
    private boolean registrationStatus = false;

    @PostConstruct
    public void setupDiscovery() {
        this.ipAddress = localIpAddress();
        this.instanceId = generateInstanceId(ipAddress);
        discoveryRegister();
    }

    @Schedule(second = "*/30", minute = "*", hour = "*")
    public void renewalAttempt() {
        if (registrationStatus) {
            discoveryRenewal();
        } else {
            discoveryRegister();
        }
    }

    private void discoveryRegister() {
        final URI uri = UriBuilder.fromUri(EUREKA_HOST)
                .path("eureka").path("apps").path(APPLICATION_NAME).build();
        Client client = ClientBuilder.newClient();

        log.info("Discovery client. {}/{}, registering...",
                APPLICATION_NAME, instanceId);

        try {
            Response response = client.target(uri)
                    .request()
                    .post(Entity.entity(eurekaXmlRequest(), MediaType.APPLICATION_XML_TYPE));

            if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
                this.registrationStatus = true;
            }

            log.info("Discovery client. {}/{}, response.status={}",
                    APPLICATION_NAME, instanceId, response.getStatus());

        } catch (Exception e) {
            log.info("Discovery client. {}/{}, failed!",
                    APPLICATION_NAME, instanceId);
        }
    }

    @PreDestroy
    public void discoveryUnregister() {

        final URI uri = UriBuilder.fromUri(EUREKA_HOST)
                .path("eureka").path("apps")
                .path(APPLICATION_NAME).path(instanceId).build();
        Client client = ClientBuilder.newClient();

        Response response = client.target(uri)
                .request()
                .delete();

        log.info("Discovery client de-registration. {}/{}, response.status={}",
                APPLICATION_NAME, instanceId, response.getStatus());

    }

    public void discoveryRenewal() {
        final URI uri = UriBuilder.fromUri(EUREKA_HOST)
                .path("eureka").path("apps")
                .path(APPLICATION_NAME).path(instanceId).build();
        Client client = ClientBuilder.newClient();

        Response response = client.target(uri)
                .request()
                .put(Entity.json(null));

        log.info("Discovery client renewal. {}/{}, response.status={}",
                APPLICATION_NAME, instanceId, response.getStatus());
    }

    private String eurekaXmlRequest() {
        Object[] params = {
                this.instanceId, //instanceId
                this.ipAddress, //ipAddress
                APPLICATION_NAME, //appName
                appUrl("localhost", "8080"), //appUrl
        };

        return MessageFormat.format(REGISTRATION_XML, params);
    }

    private String appUrl(String hostName, String port) {
        return hostName + ":" + port;
    }

    private String generateInstanceId(String ipAddress) {
        return String.format("%s:%s:%s",
                APPLICATION_NAME,
                ipAddress,
                ThreadLocalRandom.current().nextInt(0, 10+ 1));
    }

    private String localIpAddress() {
        try{
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while(interfaces.hasMoreElements()){
                NetworkInterface i = interfaces.nextElement();
                if(i != null && i.isUp() && !i.isLoopback()) {
                    Enumeration<InetAddress> addresses = i.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress inetAddress = addresses.nextElement();
                        if (inetAddress.isSiteLocalAddress()) {
                            return inetAddress.getHostName();
                        }
                    }
                }
            }
        } catch(SocketException e){
            throw new RuntimeException(e);
        }

        throw new IllegalStateException("No ip address found");
    }

}
