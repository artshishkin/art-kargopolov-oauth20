package net.shyshkin.study.oauth.clients.sociallogin;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.function.Executable;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "app.oauth.uri=http://${OAUTH_HOST}:${OAUTH_PORT}"
})
@ActiveProfiles("keycloak")
@ContextConfiguration(initializers = SocialLoginApplicationIT.Initializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SocialLoginApplicationIT {

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

    @Container
    static BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
            .withCapabilities(new FirefoxOptions())
            .withNetwork(network)
            .withNetworkAliases("browser")
            .dependsOn(keycloak);

    @Container
    static GenericContainer<?> socialLoginExample = new GenericContainer<>("artarkatesoft/oauth20-social-login")
            .withNetwork(network)
            .withNetworkAliases("social-login-example")
            .withEnv(Map.of(
                    "spring.profiles.active", "keycloak",
                    "logging.level.net.shyshkin", "debug",
                    "app.redirect.host.uri", "http://social-login-example:8080",
                    "app.oauth.uri", "http://keycloak:8080"
            ))
            .withExposedPorts(8080)
            .dependsOn(keycloak)
            .withLogConsumer(new Slf4jLogConsumer(log))
            .waitingFor(Wait.forLogMessage(".*Completed initialization in.*\\n", 1));

    RemoteWebDriver driver;

    @Autowired
    TestRestTemplate testRestTemplate;

    @BeforeEach
    void setUp() {
        driver = browser.getWebDriver();
    }

    @Test
    void loginLogout_usingSeleniumBrowser() {
        //given
        String homePageUrl = "http://social-login-example:8080/home";

        //visit secured home page should redirect to keycloak Sign In Page
        driver.get(homePageUrl);

        assertThat(driver.getTitle()).isEqualTo("Sign in to katarinazart");

        //correct signing in should redirect to home page
        signIn(RESOURCE_OWNER_USERNAME, RESOURCE_OWNER_PASSWORD);

        waitFor("log IN completion",
                List.of(
                        () -> assertThat(driver.getTitle()).isEqualTo("Home Page"),

                        () -> assertThat(driver.getPageSource())
                                .contains("User Name Attribute:")
                                .contains("Artem Shyshkin")
                ));

        //clicking on logout link should ask for logout confirmation
        WebElement aLink = driver.findElementByTagName("a");
        aLink.click();

        assertThat(driver.getTitle()).isEqualTo("Confirm Log Out?");

        //clicking on logout confirmation button should redirect to index page
        driver.findElementByTagName("button").submit();

        waitFor("log OUT completion",
                List.of(
                        () -> assertThat(driver.getTitle()).isEqualTo("Index Page")
                ));

        //after logout next visit home page should redirect to keycloak sign in page AGAIN
        driver.get(homePageUrl);

        assertThat(driver.getTitle()).isEqualTo("Sign in to katarinazart");

        log.debug("Current URL: {}", driver.getCurrentUrl());

    }

    private void waitFor(String message, List<Executable> executableList) {

        AtomicReference<String> lastUrl = new AtomicReference<>("");
        AtomicReference<String> lastPageContent = new AtomicReference<>("");

        AtomicLong start = new AtomicLong(System.currentTimeMillis());

        await()
                .timeout(5, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {

                    String currentUrl = driver.getCurrentUrl();
                    String pageSource = driver.getPageSource();

                    if (Objects.equals(currentUrl, lastUrl.get()) && Objects.equals(pageSource, lastPageContent.get())) {

                        log.debug("Waiting for {}... {} ms", message, System.currentTimeMillis() - start.get());
                    } else {
                        start.set(System.currentTimeMillis());
                        log.debug("Current URL: {}", currentUrl);
                        log.debug("Page: \n{}", pageSource);
                        lastUrl.set(currentUrl);
                        lastPageContent.set(pageSource);
                    }
                    assertAll(executableList);
                });
    }

    private void signIn(String username, String password) {
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
        }
    }

}