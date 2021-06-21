package net.shyshkin.study.oauth.ws.clients;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "app.redirect.host.uri=http://host.testcontainers.internal:8050",
        "app.oauth.uri=http://${OAUTH_HOST}:${OAUTH_PORT}",
        "spring.security.oauth2.client.provider.photo-app-web-client.authorizationUri=http://host.testcontainers.internal:${OAUTH_PORT}/auth/realms/katarinazart/protocol/openid-connect/auth"
})
@ContextConfiguration(initializers = PhotoAppWebClientApplicationTests.Initializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Can not make it work")
class PhotoAppWebClientApplicationTests {

    public static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    public static final String RESOURCE_OWNER_PASSWORD = "password_art_1";

    static Network network = Network.newNetwork();

    @Container
    static PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("keycloak")
            .withUsername("keycloak")
            .withPassword("password")
            .withNetwork(network)
            .withNetworkAliases("postgres");

    @Container
    static GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:latest")
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

    //    @Container
    static BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
            .withCapabilities(new FirefoxOptions())
            .withNetwork(network)
            .withNetworkAliases("browser")
            .dependsOn(keycloak);

    static {
//        org.testcontainers.Testcontainers.exposeHostPorts(8050);
    }

    RemoteWebDriver driver;

    @Autowired
    TestRestTemplate testRestTemplate;

    @BeforeEach
    void setUp() {
        browser.start();
        driver = browser.getWebDriver();

    }

    @AfterEach
    void tearDown() {
        browser.stop();
    }

    @Test
    @Disabled
    void getAllAlbums() {
        //given

        //when
        ResponseEntity<String> responseEntity = testRestTemplate
                .getForEntity("/albums", String.class);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.MULTIPLE_CHOICES);

    }

    @Test
    void getAllAlbums_usingSeleniumBrowser() throws InterruptedException {
        //given
        String url = "http://host.testcontainers.internal:8050/albums";

        //when
        driver.get(url);

        String expectedTitle = "Sign in to katarinazart";
        assertThat(driver.getTitle()).isEqualTo(expectedTitle);

        //then
        signIn(RESOURCE_OWNER_USERNAME, RESOURCE_OWNER_PASSWORD);

        Thread.sleep(5000);
        logUrlAndContent();

        await()
                .timeout(3, TimeUnit.SECONDS)
                .pollInterval(1,TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    logUrlAndContent();

                    assertThat(driver.getTitle()).isEqualTo("Albums Page");
                    assertThat(driver.findElementByTagName("h1").getText()).isEqualTo("Albums page");

                    assertThat(driver.getPageSource())
                            .contains("WebAlbumTitle1");
                });

    }

    private void logUrlAndContent() {
        log.debug("Current URL: {}", driver.getCurrentUrl());
        log.debug("Page: \n{}", driver.getPageSource());
    }

    private void signIn(String username, String password) {
        logUrlAndContent();

        //id = "username"
        WebElement usernameField = driver.findElementById("username");
        usernameField.sendKeys(username);

        //id = "password"
        WebElement passwordField = driver.findElementById("password");
        passwordField.sendKeys(password);
        passwordField.submit();
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            System.setProperty("OAUTH_HOST", keycloak.getHost());
            Integer keycloakPort = keycloak.getMappedPort(8080);
            System.setProperty("OAUTH_PORT", String.valueOf(keycloakPort));

            org.testcontainers.Testcontainers.exposeHostPorts(8050, keycloakPort);

        }
    }

}