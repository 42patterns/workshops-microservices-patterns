package com.example.apigateway.banners;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.appinfo.providers.EurekaConfigBasedInstanceInfoProvider;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;
import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static spark.Spark.*;

public class Application {

    private static Logger log = LoggerFactory.getLogger(Application.class);
    private static final String PORT_PROPERTY = "PORT";
    private static final Integer port = valueOf(ofNullable(getProperty(PORT_PROPERTY))
            .orElse("8081"));

<<<<<<< HEAD
    public static void main(String[] args) throws Exception {

        // Load zip specific filesystem provider when run from inside a fat-jar
        URI uri = Application.class.getResource("").toURI();
        if (uri.toString().contains("!")) {
            FileSystems.newFileSystem(uri, emptyMap());
        }

=======
    public static void main(String[] args) {
        //setup Eureka
        ApplicationInfoManager applicationInfoManager = setupEurekaClient();

        //setup SparkJava application
>>>>>>> 98488c4... Service discovery: registering banners-service through eureka-client
        port(port);
        staticFileLocation("/webapp");
        exception(Exception.class, (exception, request, response) -> exception.printStackTrace());

        get("/", (req, resp) -> {

            URI rootFolder = Application.class.getResource("/webapp").toURI();
            List<Path> banners = Files.list(Paths.get(rootFolder))
                    .collect(Collectors.toList());

            Random rand = new Random();
            Path path = banners.get(rand.nextInt(banners.size()));

            log.info("Random image: {}", path.getFileName());
            byte[] bytes = Files.readAllBytes(path);

            HttpServletResponse raw = resp.raw();
            raw.setContentLength(bytes.length);
            raw.setContentType("image/png");
            ServletOutputStream stream = raw.getOutputStream();
            stream.write(bytes);

            return raw;
        });

        //wait for SparkJava application to initialize
        awaitInitialization();

        //change Eureka status to UP (from STARTING)
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.UP);
    }

    private static ApplicationInfoManager setupEurekaClient() {
        DynamicPortInstanceConfig instanceConfig = new DynamicPortInstanceConfig(port);

        InstanceInfo instanceInfo = new EurekaConfigBasedInstanceInfoProvider(instanceConfig).get();
        ApplicationInfoManager applicationInfoManager = new ApplicationInfoManager(instanceConfig, instanceInfo);
        applicationInfoManager.setInstanceStatus(InstanceInfo.InstanceStatus.STARTING);
        new DiscoveryClient(applicationInfoManager, new DefaultEurekaClientConfig());

        return applicationInfoManager;
    }

    private static class DynamicPortInstanceConfig extends MyDataCenterInstanceConfig {
        private final Integer port;

        private DynamicPortInstanceConfig(Integer port) {
            this.port = port;
        }

        @Override
        public int getNonSecurePort() {
            return port;
        }
    }

}
