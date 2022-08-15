package net.shyshkin.study.oauth.clients.pkce;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTextInput;
import com.gargoylesoftware.htmlunit.javascript.DefaultJavaScriptErrorListener;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.test.containers.KeycloakStackContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "app.oauth.uri=http://host.testcontainers.internal:${OAUTH_PORT}",
        "app.gateway.uri=http://localhost:${GATEWAY_PORT}",
        "app.users-api.uri=http://localhost:${USERS_API_PORT}"
})
@ContextConfiguration(initializers = SpaJavascriptClientApplicationWebClientDockerTest.Initializer.class)
class SpaJavascriptClientApplicationWebClientDockerTest {

    public static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    public static final String RESOURCE_OWNER_PASSWORD = "password_art_1";

    @LocalServerPort
    int serverPort;

    @Value("${app.users-api.uri}")
    String usersServiceUri;

    @Value("${app.gateway.uri}")
    String gatewayUri;

    private WebClient webClient;

    private String baseUri;

    private AtomicReference<String> lastAlertMessage = new AtomicReference<>();

    @Container
    static KeycloakStackContainers keycloakStackContainers = KeycloakStackContainers.getInstance();

    static GenericContainer<?> keycloak = keycloakStackContainers.getKeycloak();

    static Network network = keycloakStackContainers.getStackNetwork();

    @Container
    static GenericContainer<?> discoveryService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-discovery-service")
            .withNetwork(network)
            .withNetworkAliases("discovery-service")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> usersService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-resource-server")
            .withNetwork(network)
            .withNetworkAliases("users-service")
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
            .dependsOn(usersService, discoveryService)
            .waitingFor(Wait.forHealthcheck());

    @BeforeEach
    void setUp() {

        System.setProperty("app.redirect.host.uri", "http://localhost:" + serverPort);

        this.webClient = new WebClient();
        this.baseUri = "http://localhost:" + serverPort;
        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);
        this.webClient.getOptions().setThrowExceptionOnScriptError(false);
        this.webClient.getOptions().setRedirectEnabled(true);
        this.webClient.setAjaxController(new NicelyResynchronizingAjaxController());
        this.webClient.setJavaScriptErrorListener(new DefaultJavaScriptErrorListener() {
            @Override
            public void scriptException(HtmlPage page, ScriptException scriptException) {
                log.debug("JS error in line {} column {} Failing line\n`{}`\nscript source code {}",
                        scriptException.getFailingLineNumber(),
                        scriptException.getFailingColumnNumber(),
                        scriptException.getFailingLine(),
                        scriptException.getScriptSourceCode()
                );
                scriptException.printStackTrace();
            }
        });
        this.webClient.getCookieManager().clearCookies();    // log out

    }

    @Test
    void fullWorkflowTest() throws IOException {
        //given
        String indexPageUrl = baseUri;

        //visit index page should show correct content
        HtmlPage page = webClient.getPage(indexPageUrl);

        webClient.waitForBackgroundJavaScript(1000L);

        assertThat(page.getTitleText()).isEqualTo("Javascript Application with PKCE");
        assertThat(page.getHtmlElementById("redirectHostUri").getTextContent()).isEqualTo("http://localhost:" + serverPort);
        assertThat(page.getHtmlElementById("oAuthServerUri").getTextContent()).isEqualTo(String.format("http://host.testcontainers.internal:%d", keycloak.getMappedPort(8080)));
        assertThat(page.getHtmlElementById("usersApiUri").getTextContent()).isEqualTo(usersServiceUri);
        assertThat(page.getHtmlElementById("gatewayUri").getTextContent()).isEqualTo(gatewayUri);
        assertThat(page.getHtmlElementById("resourceServerUri").getAttribute("value")).isEqualTo(usersServiceUri);

        //click on button `Generate Random State Value` should change text in `stateValue` field
        page.getHtmlElementById("generateStateBtn").click();
        assertThat(page.getHtmlElementById("stateValue").getTextContent()).isNotEqualTo("Some Value");

        //click on button `Generate Code Verifier Value` should change text in `codeVerifierValue` field
        page.getHtmlElementById("generateCodeVerifierBtn").click();
        assertThat(page.getHtmlElementById("codeVerifierValue").getTextContent()).isNotEqualTo("Code Verifier Value");

        //click on button `Generate Code Challenge Value` should change text in `codeVerifierValue` field
        HtmlButton generateCodeChallengeBtn = page.getHtmlElementById("generateCodeChallengeBtn");
        generateCodeChallengeBtn.click();
        log.debug("generateCodeChallengeBtn: {}", generateCodeChallengeBtn);

        assertThat(page.getHtmlElementById("codeChallengeValue").getTextContent()).isNotEqualTo("Code Challenge Value");

        WebWindow currentWindow = webClient.getCurrentWindow();

        //click on button `Get Auth Code` should pop up new window for signing into keycloak
        page.getHtmlElementById("getAuthCodeBtn").click();

        List<WebWindow> webWindows = webClient.getWebWindows();
        WebWindow loginWindow = webWindows.get(1);

        webClient.waitForBackgroundJavaScript(3000L);

        HtmlPage loginPage = (HtmlPage) loginWindow.getEnclosedPage();

        assertLoginPage(loginPage);

        //correct signing in should redirect to index page with Access Token Generated
        page = signIn(loginPage, RESOURCE_OWNER_USERNAME, RESOURCE_OWNER_PASSWORD);

        assertSpaPage(page);
        String accessToken = page.getHtmlElementById("accessToken").getTextContent();
        assertThat(accessToken)
                .isNotBlank()
                .hasSizeGreaterThan(20);

        log.debug("Access Token: {}", accessToken);

        //click on buttons `Get Info From Resource Server directly` should show alert with "Working..." message
        List<String> directResourceServerButtonsIds = List.of(
                "getInfoFromResourceServerDirectBtn",
                "getInfoFromResourceServerScopeBtn",
                "getInfoFromResourceServerRoleBtn"
        );

        webClient.setAlertHandler((AlertHandler) (page1, message) -> lastAlertMessage.set(message));

        for (String buttonsId : directResourceServerButtonsIds) {
            log.debug("Clicking on `{}`", buttonsId);
            page.getHtmlElementById(buttonsId).click();
            waitForAlert("Working...");
        }

        //click on buttons `Delete user by fake id` should show alert with "Deleted user with id: some_fake_id" message
        for (String buttonsId : List.of("deleteRegularUserBtn")) {
            log.debug("Clicking on `{}`", buttonsId);
            page.getHtmlElementById(buttonsId).click();
            waitForAlert("Deleted user with id: some_fake_id");
        }

        boolean skipTestsWithGateway = false;
        if (skipTestsWithGateway) return;

        //click on buttons `Get Info From Resource Server `users` through Gateway` should show alert with "Working..." message
        for (String buttonsId : List.of("getInfoFromResourceServerThroughGatewayBtn")) {
            log.debug("Clicking on `{}`", buttonsId);
            page.getHtmlElementById(buttonsId).click();
            waitForAlert("Working...");
        }

        //get Info From Resource Server through API Gateway should show alert with "Working..." message
        HtmlTextInput serverUriInputField = page.getHtmlElementById("resourceServerUri");
        serverUriInputField.setText(gatewayUri);
//        serverUriInputField.type(gatewayUri);
        assertThat(serverUriInputField.getAttribute("value")).isEqualTo(gatewayUri);

        for (String buttonsId : directResourceServerButtonsIds) {
            log.debug("GATEWAY: Clicking on `{}`", buttonsId);
            page.getHtmlElementById(buttonsId).click();
            waitForAlert("Working...");
        }

        //delete user through API Gateway should show alert with "Deleted user with id: some_fake_id" message
        for (String buttonsId : List.of("deleteRegularUserBtn")) {
            log.debug("Clicking on `{}`", buttonsId);
            page.getHtmlElementById(buttonsId).click();
            waitForAlert("Deleted user with id: some_fake_id");
        }

    }

    private void waitForAlert(String expectedMessage) {
        await()
                .timeout(5, TimeUnit.SECONDS)
                .pollInterval(100, TimeUnit.MILLISECONDS)
                .untilAsserted(() -> {
                    assertThat(lastAlertMessage.get()).isEqualTo(expectedMessage);
                    lastAlertMessage.set("");
                });
    }

    private static <P extends Page> P signIn(HtmlPage page, String username, String password) throws IOException {
        HtmlInput usernameInput = page.querySelector("input[name=\"username\"]");
        HtmlInput passwordInput = page.querySelector("input[name=\"password\"]");
        HtmlInput signInButton = page.querySelector("input[name=\"login\"]");

        usernameInput.type(username);
        passwordInput.type(password);
        return signInButton.click();
    }

    private static void assertLoginPage(HtmlPage page) {
        assertThat(page.getUrl().toString()).contains("/auth/realms/katarinazart/protocol/openid-connect/auth");

        HtmlInput usernameInput = page.querySelector("input[name=\"username\"]");
        HtmlInput passwordInput = page.querySelector("input[name=\"password\"]");
        HtmlInput signInButton = page.querySelector("input[name=\"login\"]");
        String title = page.getTitleText();

        assertThat(title).isEqualTo("Sign in to katarinazart");
        assertThat(usernameInput).isNotNull();
        assertThat(passwordInput).isNotNull();
        assertThat(signInButton.getValueAttribute()).isEqualToIgnoringCase("Sign In");
    }

    private static void assertSpaPage(HtmlPage page) {
        String title = page.getTitleText();
        assertThat(title).isEqualTo("Javascript Application with PKCE");
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            System.setProperty("OAUTH_HOST", keycloak.getHost());
            Integer keycloakPort = keycloak.getMappedPort(8080);
            System.setProperty("OAUTH_PORT", String.valueOf(keycloakPort));

            Integer usersServicePort = usersService.getMappedPort(8080);
            System.setProperty("USERS_API_PORT", String.valueOf(usersServicePort));

            Integer gatewayPort = gatewayService.getMappedPort(8080);
            System.setProperty("GATEWAY_PORT", String.valueOf(gatewayPort));
        }
    }
}