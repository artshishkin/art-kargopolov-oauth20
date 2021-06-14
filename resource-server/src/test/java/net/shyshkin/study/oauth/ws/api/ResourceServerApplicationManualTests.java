package net.shyshkin.study.oauth.ws.api;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@Testcontainers
@ContextConfiguration(initializers = ResourceServerApplicationManualTests.Initializer.class)
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug"
})
class ResourceServerApplicationManualTests {

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
        String url = "http://host.testcontainers.internal:8080/auth/realms/katarinazart/protocol/openid-connect/auth?response_type=code&client_id=photo-app-code-flow-client&scope=openid profile&state=jskd879sdkj&redirect_uri=http://localhost:8083/callback";
        log.debug("Browser container: {}", browser);
        driver.get(url);

        String expectedTitle = "Sign in to katarinazart";
        assertThat(driver.getTitle()).isEqualTo(expectedTitle);
        //id = "username"
        //id = "password"
        //id = "kc-login"
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
