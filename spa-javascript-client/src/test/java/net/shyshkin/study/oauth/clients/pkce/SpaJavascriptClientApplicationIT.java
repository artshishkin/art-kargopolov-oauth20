package net.shyshkin.study.oauth.clients.pkce;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.test.containers.KeycloakStackContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.openqa.selenium.Alert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.boot.test.context.SpringBootTest;
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

import java.util.*;
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
@ContextConfiguration(initializers = SpaJavascriptClientApplicationIT.Initializer.class)
class SpaJavascriptClientApplicationIT {

    public static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    public static final String RESOURCE_OWNER_PASSWORD = "password_art_1";

    @Container
    static KeycloakStackContainers keycloakStackContainers = KeycloakStackContainers.getInstance();

    static GenericContainer<?> keycloak = keycloakStackContainers.getKeycloak();

    static Network network = keycloakStackContainers.getStackNetwork();

    @Container
    static GenericContainer<?> discoveryService = new GenericContainer<>("artarkatesoft/oauth20-discovery-service")
            .withNetwork(network)
            .withNetworkAliases("discovery-service")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> usersService = new GenericContainer<>("artarkatesoft/oauth20-resource-server")
            .withNetwork(network)
            .withNetworkAliases("users-service")
            .withExposedPorts(8080)
            .withEnv("eureka.client.enabled", "true")
            .dependsOn(keycloak, discoveryService)
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> gatewayService = new GenericContainer<>("artarkatesoft/oauth20-api-gateway")
            .withNetwork(network)
            .withNetworkAliases("gateway-service")
            .withExposedPorts(8080)
            .withEnv("eureka.client.enabled", "true")
            .withEnv("spring.profiles.active", "local")
            .withEnv("discovery.service.uri", "http://discovery-service:8080")
            .withEnv("eureka.client.registry-fetch-interval-seconds", "1")
            .dependsOn(usersService, discoveryService)
            .waitingFor(Wait.forHealthcheck());

    @Container
    static BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
            .withCapabilities(new FirefoxOptions())
            .withNetwork(network)
            .withNetworkAliases("browser")
            .withLogConsumer(new Slf4jLogConsumer(log))
            .dependsOn(keycloak);

    @Container
    static GenericContainer<?> spaJavascriptClient = new GenericContainer<>("artarkatesoft/oauth20-spa-javascript-client")
            .withNetwork(network)
            .withNetworkAliases("spa-javascript-client")
            .withEnv(Map.of(
                    "logging.level.net.shyshkin", "debug",
                    "app.redirect.host.uri", "http://spa-javascript-client:8080",
                    "app.oauth.uri", "http://keycloak:8080",
                    "app.gateway.uri", "http://gateway-service:8080",
                    "app.users-api.uri", "http://users-service:8080"
            ))
            .withExposedPorts(8080)
            .dependsOn(keycloak, gatewayService, usersService)
            .withLogConsumer(new Slf4jLogConsumer(log))
            .waitingFor(Wait.forLogMessage(".*Completed initialization in.*\\n", 1));

    RemoteWebDriver driver;

    @BeforeEach
    void setUp() {
        driver = browser.getWebDriver();
    }

    @Test
    void fullWorkflowTest() {
        //given
        String indexPageUrl = "http://spa-javascript-client:8080";

        //visit index page should show correct content
        driver.get(indexPageUrl);

        assertThat(driver.getTitle()).isEqualTo("Javascript Application with PKCE");
        assertThat(driver.findElement(By.id("redirectHostUri")).getText()).isEqualTo("http://spa-javascript-client:8080");
        assertThat(driver.findElement(By.id("oAuthServerUri")).getText()).isEqualTo("http://keycloak:8080");
        assertThat(driver.findElement(By.id("usersApiUri")).getText()).isEqualTo("http://users-service:8080");
        assertThat(driver.findElement(By.id("gatewayUri")).getText()).isEqualTo("http://gateway-service:8080");
        assertThat(driver.findElement(By.id("resourceServerUri")).getAttribute("value")).isEqualTo("http://users-service:8080");

        //click on button `Generate Random State Value` should change text in `stateValue` field
        driver.findElement(By.id("generateStateBtn")).click();
        assertThat(driver.findElement(By.id("stateValue")).getText()).isNotEqualTo("Some Value");

        //click on button `Generate Code Verifier Value` should change text in `codeVerifierValue` field
        driver.findElement(By.id("generateCodeVerifierBtn")).click();
        assertThat(driver.findElement(By.id("codeVerifierValue")).getText()).isNotEqualTo("Code Verifier Value");

//        logCurrentPage();

        //click on button `Generate Code Challenge Value` should change text in `codeVerifierValue` field
        WebElement generateCodeChallengeBtn = driver.findElement(By.id("generateCodeChallengeBtn"));
        generateCodeChallengeBtn.click();
        log.debug("generateCodeChallengeBtn: {}", generateCodeChallengeBtn);

        waitFor("generating code Challenge",
                List.of(
                        () -> driver.findElement(By.id("generateCodeChallengeBtn")).click(),
                        () -> assertThat(driver.findElement(By.id("codeChallengeValue")).getText()).isNotEqualTo("Code Challenge Value")
                ));

        String indexWindowHandle = driver.getWindowHandle();
        log.debug("Index Window Handler: {}", indexWindowHandle);

        //click on button `Get Auth Code` should pop up new window for signing into keycloak
        driver.findElement(By.id("getAuthCodeBtn")).click();

        Set<String> windowHandles = driver.getWindowHandles();
        assertThat(windowHandles).hasSize(2);

        Optional<String> authPageOptional = windowHandles
                .stream()
                .filter(windowHandle -> !Objects.equals(windowHandle, indexWindowHandle))
                .findAny();
        assertThat(authPageOptional.isPresent()).isTrue();

        String authPageWindowHandle = authPageOptional.get();
        driver.switchTo().window(authPageWindowHandle);

        waitFor("new Window appear",
                List.of(
                        () -> assertThat(driver.getTitle()).isEqualTo("Sign in to katarinazart")
                ));


        //correct signing in should redirect to index page with Access Token Generated
        signIn(RESOURCE_OWNER_USERNAME, RESOURCE_OWNER_PASSWORD);

        await()
                .timeout(10, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    log.debug("Window Handles:");
                    driver.getWindowHandles()
                            .forEach(handle -> log.debug("{}", handle));
                    assertThat(driver.getWindowHandles()).hasSize(1);
                });

        driver.switchTo().window(indexWindowHandle);

        waitFor("log IN completion",
                List.of(
                        () -> assertThat(driver.getTitle()).isEqualTo("Javascript Application with PKCE"),

                        () -> assertThat(driver.findElement(By.id("accessToken")).getText())
                                .isNotBlank()
                                .hasSizeGreaterThan(20)
                ));

        //click on buttons `Get Info From Resource Server" directly` should show alert with "Working..." message
        List<String> directResourceServerButtonsIds = List.of(
                "getInfoFromResourceServerDirectBtn",
                "getInfoFromResourceServerScopeBtn",
                "getInfoFromResourceServerRoleBtn"
        );

        for (String buttonsId : directResourceServerButtonsIds) {
            log.debug("Clicking on `{}`", buttonsId);
            driver.findElement(By.id(buttonsId)).click();
            Alert alert = driver.switchTo().alert();
            assertThat(alert.getText()).isEqualTo("Working...");
            alert.accept();
        }

        //click on buttons `Delete user by fake id` should show alert with "Deleted user with id: some_fake_id" message
        for (String buttonsId : List.of("deleteRegularUserBtn")) {
            log.debug("Clicking on `{}`", buttonsId);
            driver.findElement(By.id(buttonsId)).click();
            Alert alert = driver.switchTo().alert();
            assertThat(alert.getText()).isEqualTo("Deleted user with id: some_fake_id");
            alert.accept();
        }

        //click on buttons `Get Info From Resource Server `users` through Gateway` should show alert with "Working..." message
        for (String buttonsId : List.of("getInfoFromResourceServerThroughGatewayBtn")) {
            log.debug("Clicking on `{}`", buttonsId);
            driver.findElement(By.id(buttonsId)).click();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Alert alert = driver.switchTo().alert();
            assertThat(alert.getText()).isEqualTo("Working...");
            alert.accept();
        }

        //get Info From Resource Server through API Gateway should show alert with "Working..." message
        String gatewayUri = driver.findElement(By.id("gatewayUri")).getText();
        log.debug("Gateway Uri: {}", gatewayUri);
        driver.findElement(By.id("resourceServerUri")).clear();
        driver.findElement(By.id("resourceServerUri")).sendKeys(gatewayUri);

        assertThat(driver.findElement(By.id("resourceServerUri")).getAttribute("value")).isEqualTo(gatewayUri);
        for (String buttonsId : directResourceServerButtonsIds) {
            log.debug("GATEWAY: Clicking on `{}`", buttonsId);
            driver.findElement(By.id("getInfoFromResourceServerDirectBtn")).click();
            Alert alert = driver.switchTo().alert();
            assertThat(alert.getText()).isEqualTo("Working...");
            alert.accept();
        }

        //delete user through API Gateway should show alert with "Deleted user with id: some_fake_id" message
        for (String buttonsId : List.of("deleteRegularUserBtn")) {
            log.debug("GATEWAY: Clicking on `{}`", buttonsId);
            driver.findElement(By.id(buttonsId)).click();
            Alert alert = driver.switchTo().alert();
            assertThat(alert.getText()).isEqualTo("Deleted user with id: some_fake_id");
            alert.accept();
        }


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
        WebElement usernameField = driver.findElement(By.id("username"));
        usernameField.sendKeys(username);

        //id = "password"
        WebElement passwordField = driver.findElement(By.id("password"));
        passwordField.sendKeys(password);
        passwordField.submit();
    }

    private void logCurrentPage() {
        log.debug("Current URL: {}", driver.getCurrentUrl());
        log.debug("Page: \n{}", driver.getPageSource());
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