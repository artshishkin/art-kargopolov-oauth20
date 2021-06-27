package net.shyshkin.study.oauth.test.containers;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Map;

public class KeycloakStackContainers extends GenericContainer<KeycloakStackContainers> {

    private static KeycloakStackContainers instance;

    private static boolean containerStarted = false;

    private final Network network = Network.newNetwork();

    private final PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("keycloak")
            .withUsername("keycloak")
            .withPassword("password")
            .withNetwork(network)
            .withNetworkAliases("postgres");

    private final GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:latest")
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
            .withFileSystemBind("C:\\Users\\Admin\\IdeaProjects\\Study\\SergeyKargopolov\\OAuth20\\art-kargopolov-oauth20\\docker-compose\\keycloak-postgres\\export\\realm-export.json", "/tmp/export/realm-export.json")
            .withExposedPorts(8080)
            .dependsOn(postgreSQL)
            .waitingFor(Wait.forLogMessage(".*Admin console listening on.*\\n", 1));


    public Network getStackNetwork() {
        return network;
    }

    public static KeycloakStackContainers getInstance() {
        if (instance == null)
            instance = new KeycloakStackContainers();
        return instance;
    }

    public PostgreSQLContainer<?> getPostgreSQL() {
        return postgreSQL;
    }

    public  GenericContainer<?> getKeycloak() {
        return keycloak;
    }

    @Override
    public void start() {
        if (!containerStarted) {
            postgreSQL.start();
            keycloak.start();
        }
        containerStarted = true;
    }

    @Override
    public void stop() {
        //do nothing, JVM handles shut down
    }
}