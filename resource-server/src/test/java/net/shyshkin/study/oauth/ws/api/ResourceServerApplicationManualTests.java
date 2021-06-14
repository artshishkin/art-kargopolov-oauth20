package net.shyshkin.study.oauth.ws.api;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Testcontainers
@ContextConfiguration(initializers = ResourceServerApplicationManualTests.Initializer.class)
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug"
})
class ResourceServerApplicationManualTests {

    public static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    public static final String RESOURCE_OWNER_PASSWORD = "password_art_1";
    //    @Container
    public BrowserWebDriverContainer<?> browser;

    RemoteWebDriver driver;

    @BeforeEach
    void setUp() {
        browser = new BrowserWebDriverContainer<>()
                .withCapabilities(new FirefoxOptions());
        browser.start();
    }

    @AfterEach
    void tearDown() {
        browser.stop();
    }

    @Test
    void seleniumTest() {
        driver = browser.getWebDriver();

        String code = getAuthorizationCode();

        log.debug("Code from keycloak: {}", code);
    }

    private void askOAuthServerForAuthorizationCode() {

        String url = "http://host.testcontainers.internal:8080/auth/realms/katarinazart/protocol/openid-connect/auth?response_type=code&client_id=photo-app-code-flow-client&scope=openid profile&state=jskd879sdkj&redirect_uri=http://localhost:8083/callback";
        log.debug("Browser container: {}", browser);
        driver.get(url);

        String expectedTitle = "Sign in to katarinazart";
        assertThat(driver.getTitle()).isEqualTo(expectedTitle);
    }

    private String getAuthorizationCode() {

        askOAuthServerForAuthorizationCode();

        signIn(RESOURCE_OWNER_USERNAME, RESOURCE_OWNER_PASSWORD);

        waitRedirection();

        String redirectedUrl = driver.getCurrentUrl();
        log.debug("Redirected URL: {}", redirectedUrl);

        return redirectedUrl.split("code=")[1];
    }

    private void waitRedirection() {
        await()
                .timeout(3, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    String currentUrl = driver.getCurrentUrl();
                    //"http://localhost:8083/callback?state=jskd879sdkj&session_state=32b5908c-a448-4dea-bda5-1efd2836d950&code=7f7eb4e7-174a-498c-b21d-99afa4b738aa.32b5908c-a448-4dea-bda5-1efd2836d950.5ca38048-af08-42e3-8bef-fa42c2956a9c"
                    assertThat(currentUrl)
                            .contains("state=jskd879sdkj")
                            .contains("code=")
                    ;
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

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            applicationContext.addApplicationListener((ApplicationListener<WebServerInitializedEvent>) event -> {
//                org.testcontainers.Testcontainers.exposeHostPorts(event.getWebServer().getPort());
                org.testcontainers.Testcontainers.exposeHostPorts(8080);
            });
        }
    }

}
