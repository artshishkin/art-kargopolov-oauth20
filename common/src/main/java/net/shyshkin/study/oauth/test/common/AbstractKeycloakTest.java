package net.shyshkin.study.oauth.test.common;

import net.shyshkin.study.oauth.test.containers.KeycloakStackContainers;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class AbstractKeycloakTest {

    protected static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    protected static final String RESOURCE_OWNER_PASSWORD = "password_art_1";

    @Container
    protected static KeycloakStackContainers keycloakStackContainers = KeycloakStackContainers.getInstance();

    protected static GenericContainer<?> keycloak = keycloakStackContainers.getKeycloak();

    protected static Network network = keycloakStackContainers.getStackNetwork();

}