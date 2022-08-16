package net.shyshkin.study.oauth.ws.clients;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.test.common.AbstractKeycloakTest;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "app.redirect.host.uri=http://host.testcontainers.internal:8050",
        "app.oauth.uri=http://host.testcontainers.internal:${OAUTH_PORT}",
        "spring.security.oauth2.client.provider.photo-app-web-client.authorizationUri=http://host.testcontainers.internal:${OAUTH_PORT}/auth/realms/katarinazart/protocol/openid-connect/auth",
        "app.gateway.uri=http://host.testcontainers.internal:${GATEWAY_PORT}"
})
@ContextConfiguration(initializers = PhotoAppWebClientApplicationTests.Initializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PhotoAppWebClientApplicationTests extends AbstractKeycloakTest {

    @Container
    static GenericContainer<?> discoveryService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-discovery-service")
            .withNetwork(network)
            .withNetworkAliases("discovery-service")
            .waitingFor(Wait.forHealthcheck());

    //    @Container
    static BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
            .withCapabilities(new FirefoxOptions())
            .withNetwork(network)
            .withNetworkAliases("browser")
            .dependsOn(keycloak);
    @Container
    static GenericContainer<?> albumsService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-albums-service")
            .withNetwork(network)
            .withNetworkAliases("albums-service")
            .withEnv("eureka.client.enabled", "true")
            .dependsOn(keycloak, discoveryService)
            .waitingFor(Wait.forHealthcheck());
    @Container
    static GenericContainer<?> gatewayService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-api-gateway")
            .withNetwork(network)
            .withNetworkAliases("gateway-service")
            .withExposedPorts(8080)
            .withEnv("eureka.client.enabled", "true")
            .withEnv("spring.profiles.active", "local")
            .withEnv("discovery.service.uri", "http://discovery-service:8080")
            .withEnv("eureka.client.registry-fetch-interval-seconds", "1")
            .dependsOn(discoveryService, albumsService)
            .waitingFor(Wait.forHealthcheck());
    @LocalServerPort
    int serverPort;

    RemoteWebDriver driver;

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
    void getAllAlbums_usingSeleniumBrowser() throws InterruptedException {
        //given
        String url = "http://host.testcontainers.internal:" + serverPort + "/albums";

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
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    logUrlAndContent();

                    assertThat(driver.getTitle()).isEqualTo("Albums Page");
                    assertThat(driver.findElement(By.tagName("h1")).getText()).isEqualTo("Albums page");

                    assertThat(driver.getPageSource())
                            .contains("AlbumTitle1")
                            .doesNotContain("WebAlbumTitle1");
                });
    }

    private void logUrlAndContent() {
        log.debug("Current URL: {}", driver.getCurrentUrl());
        log.debug("Page: \n{}", driver.getPageSource());
    }

    private void signIn(String username, String password) {
        logUrlAndContent();

        //id = "username"
        WebElement usernameField = driver.findElement(By.id("username"));
        usernameField.sendKeys(username);

        //id = "password"
        WebElement passwordField = driver.findElement(By.id("password"));
        passwordField.sendKeys(password);
        passwordField.submit();
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            System.setProperty("OAUTH_HOST", keycloak.getHost());
            Integer keycloakPort = keycloak.getMappedPort(8080);
            System.setProperty("OAUTH_PORT", String.valueOf(keycloakPort));

            Integer gatewayPort = gatewayService.getMappedPort(8080);
            System.setProperty("GATEWAY_PORT", String.valueOf(gatewayPort));

            org.testcontainers.Testcontainers.exposeHostPorts(8050, keycloakPort);

        }
    }

}