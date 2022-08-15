package net.shyshkin.study.oauth.ws.clients;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.test.common.AbstractKeycloakTest;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "app.redirect.host.uri=http://host.testcontainers.internal:8050",
        "app.oauth.uri=http://${OAUTH_HOST}:${OAUTH_PORT}",
        "spring.security.oauth2.client.provider.photo-app-web-client.authorizationUri=http://host.testcontainers.internal:${OAUTH_PORT}/auth/realms/katarinazart/protocol/openid-connect/auth"
})
@ContextConfiguration(initializers = PhotoAppWebClientApplicationTests.Initializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled("Can not make it work")
class PhotoAppWebClientApplicationTests extends AbstractKeycloakTest {

    //    @Container
    static BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
            .withCapabilities(new FirefoxOptions())
            .withNetwork(network)
            .withNetworkAliases("browser")
            .withReuse(true)
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
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    logUrlAndContent();

                    assertThat(driver.getTitle()).isEqualTo("Albums Page");
                    assertThat(driver.findElement(By.tagName("h1")).getText()).isEqualTo("Albums page");

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

            org.testcontainers.Testcontainers.exposeHostPorts(8050, keycloakPort);

        }
    }

}