package net.shyshkin.study.oauth.test.containers;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.time.Duration;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Slf4j
public class KeycloakStackContainers extends GenericContainer<KeycloakStackContainers> {

    private static final String ENV_FILE_PATH = "../docker-compose/.env";

    private static KeycloakStackContainers instance;

    private static boolean containerStarted = false;

    private static Map<String, String> versions;

    private final Network network = Network.newNetwork();

    private final PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>("postgres:" + getVersion("POSTGRES_VERSION"))
            .withDatabaseName("keycloak")
            .withUsername("keycloak")
            .withPassword("password")
            .withNetwork(network)
            .withNetworkAliases("postgres");

    private final GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:" + getVersion("KEYCLOAK_VERSION"))
            .withNetwork(network)
            .withNetworkAliases("keycloak")
            .withEnv(Map.of(
                    "DB_VENDOR", "POSTGRES",
                    "DB_ADDR", "postgres",
                    "DB_DATABASE", "keycloak",
                    "DB_USER", "keycloak",
                    "DB_SCHEMA", "public",
                    "DB_PASSWORD", "password",
                    "KEYCLOAK_USER", "admin",
                    "KEYCLOAK_PASSWORD", "Pa55w0rd"
            ))
            .withCommand(
                    "-b 0.0.0.0",
                    "-Dkeycloak.migration.action=import",
                    "-Dkeycloak.migration.provider=singleFile",
                    "-Dkeycloak.migration.file=/tmp/export/realm-export.json",
                    "-Dkeycloak.migration.strategy=IGNORE_EXISTING"
            )
            .withFileSystemBind("../docker-compose/keycloak-postgres/export/realm-export.json", "/tmp/export/realm-export.json")
            .withExposedPorts(8080)
            .withCopyFileToContainer(MountableFile.forHostPath("../user-storage-provider/target/my-remote-user-storage-provider.jar"),
                    "/opt/jboss/keycloak/standalone/deployments/my-remote-user-storage-provider.jar")
            .dependsOn(postgreSQL)
//            .withLogConsumer(new Slf4jLogConsumer(log))
            .waitingFor(Wait.forLogMessage(".*Admin console listening on.*\\n", 1)
                    .withStartupTimeout(Duration.ofSeconds(120)));

    private final GenericContainer<?> userLegacyService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-user-legacy-service:" + getVersion("SERVICE_VERSION"))
            .withNetwork(network)
            .withNetworkAliases("user-legacy-service")
            .waitingFor(Wait.forHealthcheck());

    public Network getStackNetwork() {
        return network;
    }

    public static KeycloakStackContainers getInstance() {
        if (instance == null)
            instance = new KeycloakStackContainers();
        return instance;
    }

    public PostgreSQLContainer<?> getPostgreSQL() {
        return getInstance().postgreSQL;
    }

    public GenericContainer<?> getKeycloak() {
        return getInstance().keycloak;
    }

    @Override
    public void start() {
        if (!containerStarted) {
            postgreSQL.start();
            keycloak.start();
            userLegacyService.start();
        }
        containerStarted = true;
    }

    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }

    private static String getVersion(String versionKey) {
        if (versions == null) {
            versions = getEnvVariables();
        }
        return versions.get(versionKey);
    }

    private static Map<String, String> getEnvVariables() {
        Properties properties = new Properties();
        try (Reader reader = new FileReader(ENV_FILE_PATH)) {
            properties.load(reader);
        } catch (IOException e) {
            log.error("", e);
        }

        Map<String, String> envVariables = properties.entrySet()
                .stream()
                .collect(Collectors.toMap(e -> e.getKey().toString(),
                        e -> e.getValue().toString()));

        log.debug("Docker-compose Environment variables: {}", envVariables);
        return envVariables;
    }

}
