package net.shyshkin.study.oauth.ws.clients;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.test.containers.KeycloakStackContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "app.oauth.uri=http://${OAUTH_HOST}:${OAUTH_PORT}"
})
@ContextConfiguration(initializers = PhotoAppWebClientApplicationTest.Initializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PhotoAppWebClientApplicationTest {

    public static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    public static final String RESOURCE_OWNER_PASSWORD = "password_art_1";

    @Container
    static KeycloakStackContainers keycloakStackContainers = KeycloakStackContainers.getInstance();

    static GenericContainer<?> keycloak = keycloakStackContainers.getKeycloak();

    static Network network = keycloakStackContainers.getStackNetwork();

    @Container
    static BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
            .withCapabilities(new FirefoxOptions())
            .withNetwork(network)
            .withNetworkAliases("browser")
            .dependsOn(keycloak);

    @Container
    static GenericContainer<?> discoveryService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-discovery-service")
            .withNetwork(network)
            .withNetworkAliases("discovery-service")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> albumsService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-albums-service")
            .withNetwork(network)
            .withNetworkAliases("albums-service")
            .withExposedPorts(8080)
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

    @Container
    static GenericContainer<?> photoAppWebClient = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-photo-app-web-client")
            .withNetwork(network)
            .withNetworkAliases("photo-app-webclient")
            .withEnv(Map.of(
                    "logging.level.net.shyshkin", "debug",
                    "app.redirect.host.uri", "http://photo-app-webclient:8080"
            ))
            .withExposedPorts(8080)
            .dependsOn(keycloak, gatewayService)
            .withLogConsumer(new Slf4jLogConsumer(log))
            .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(120)));

    RemoteWebDriver driver;

    @Autowired
    TestRestTemplate testRestTemplate;

    @BeforeEach
    void setUp() {
        driver = browser.getWebDriver();
    }

    @Test
    void getAllAlbums_usingSeleniumBrowser() {
        //given
        String url = "http://photo-app-webclient:8080/albums";

        //when
        driver.get(url);

        String expectedTitle = "Sign in to katarinazart";
        assertThat(driver.getTitle()).isEqualTo(expectedTitle);

        //then
        signIn(RESOURCE_OWNER_USERNAME, RESOURCE_OWNER_PASSWORD);

        AtomicReference<String> lastUrl = new AtomicReference<>("");
        AtomicReference<String> lastPageContent = new AtomicReference<>("");

        AtomicLong start = new AtomicLong(System.currentTimeMillis());

        await()
                .timeout(10, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {

                    String currentUrl = driver.getCurrentUrl();
                    String pageSource = driver.getPageSource();

                    if (Objects.equals(currentUrl, lastUrl.get()) && Objects.equals(pageSource, lastPageContent.get())) {
                        log.debug("Waiting for log in completion... {} ms", System.currentTimeMillis() - start.get());
                    } else {
                        start.set(System.currentTimeMillis());
                        log.debug("Current URL: {}", currentUrl);
                        log.debug("Page: \n{}", pageSource);
                        lastUrl.set(currentUrl);
                        lastPageContent.set(pageSource);
                    }
                    assertThat(driver.getTitle()).isEqualTo("Albums Page");
                    assertThat(driver.findElement(By.tagName("h1")).getText()).isEqualTo("Albums page");

                    assertThat(pageSource)
                            .contains("AlbumTitle1")
                            .doesNotContain("WebAlbumTitle1");
                });

    }

    private void signIn(String username, String password) {
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

        }
    }

}