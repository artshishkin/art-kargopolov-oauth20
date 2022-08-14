package net.shyshkin.study.oauth.clients.pkce;

import com.gargoylesoftware.htmlunit.*;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.javascript.DefaultJavaScriptErrorListener;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.test.containers.KeycloakStackContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "app.oauth.uri=http://${OAUTH_HOST}:${OAUTH_PORT}"
})
@ContextConfiguration(initializers = SpaJavascriptClientApplicationWebClientDockerTest.Initializer.class)
class SpaJavascriptClientApplicationWebClientDockerTest {

    public static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    public static final String RESOURCE_OWNER_PASSWORD = "password_art_1";

    @LocalServerPort
    int serverPort;

    private WebClient webClient;

    private String baseUri;

    @Container
    static KeycloakStackContainers keycloakStackContainers = KeycloakStackContainers.getInstance();

    static GenericContainer<?> keycloak = keycloakStackContainers.getKeycloak();

    static Network network = keycloakStackContainers.getStackNetwork();

//    @Container
//    static GenericContainer<?> discoveryService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-discovery-service")
//            .withNetwork(network)
//            .withNetworkAliases("discovery-service")
//            .withExposedPorts(8080)
//            .waitingFor(Wait.forHealthcheck());

//    @Container
//    static GenericContainer<?> usersService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-resource-server")
//            .withNetwork(network)
//            .withNetworkAliases("users-service")
//            .withExposedPorts(8080)
//            .withEnv("eureka.client.enabled", "true")
//            .dependsOn(keycloak, discoveryService)
//            .waitingFor(Wait.forHealthcheck());
//
//    @Container
//    static GenericContainer<?> gatewayService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-api-gateway")
//            .withNetwork(network)
//            .withNetworkAliases("gateway-service")
//            .withExposedPorts(8080)
//            .withEnv("eureka.client.enabled", "true")
//            .withEnv("spring.profiles.active", "local")
//            .withEnv("discovery.service.uri", "http://discovery-service:8080")
//            .withEnv("eureka.client.registry-fetch-interval-seconds", "1")
//            .dependsOn(usersService, discoveryService)
//            .waitingFor(Wait.forHealthcheck());


//    @Container
//    static GenericContainer<?> spaJavascriptClient = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-spa-javascript-client")
//            .withNetwork(network)
//            .withNetworkAliases("spa-javascript-client")
//            .withEnv(Map.of(
//                    "logging.level.net.shyshkin", "debug",
//                    "app.redirect.host.uri", "http://spa-javascript-client:8080",
//                    "app.oauth.uri", "http://keycloak:8080",
//                    "app.gateway.uri", "http://gateway-service:8080",
//                    "app.users-api.uri", "http://users-service:8080"
//            ))
//            .withExposedPorts(8080)
//            .dependsOn(keycloak/*, gatewayService, usersService*/)
//            .withLogConsumer(new Slf4jLogConsumer(log))
//            .waitingFor(Wait.forHealthcheck());


    @BeforeEach
    void setUp() {

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
        assertThat(page.getHtmlElementById("redirectHostUri").getTextContent()).isEqualTo("http://localhost:8181");
        assertThat(page.getHtmlElementById("oAuthServerUri").getTextContent()).isEqualTo(String.format("http://%s:%d", keycloak.getHost(), keycloak.getMappedPort(8080)));
        assertThat(page.getHtmlElementById("usersApiUri").getTextContent()).isEqualTo("http://localhost:8666");
        assertThat(page.getHtmlElementById("gatewayUri").getTextContent()).isEqualTo("http://localhost:8090");
        assertThat(page.getHtmlElementById("resourceServerUri").getAttribute("value")).isEqualTo("http://localhost:8666");

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

//        for (String buttonsId : directResourceServerButtonsIds) {
//            log.debug("Clicking on `{}`", buttonsId);
//            page.getHtmlElementById(buttonsId).click();
//            waitForAlert("Working...");
//        }
//
//        //click on buttons `Delete user by fake id` should show alert with "Deleted user with id: some_fake_id" message
//        for (String buttonsId : List.of("deleteRegularUserBtn")) {
//            log.debug("Clicking on `{}`", buttonsId);
//            page.getHtmlElementById(buttonsId).click();
//            waitForAlert("Deleted user with id: some_fake_id");
//        }

        boolean skipTestsWithGateway = true;
        if (skipTestsWithGateway) return;

    }

//    private void waitFor(String message, List<Executable> executableList) {
//
//        AtomicReference<String> lastUrl = new AtomicReference<>("");
//        AtomicReference<String> lastPageContent = new AtomicReference<>("");
//
//        AtomicLong start = new AtomicLong(System.currentTimeMillis());
//
//        await()
//                .timeout(10, TimeUnit.SECONDS)
//                .pollInterval(500, TimeUnit.MILLISECONDS)
//                .untilAsserted(() -> {
//
//                    String currentUrl = page.getCurrentUrl();
//                    String pageSource = page.getPageSource();
//
//                    if (Objects.equals(currentUrl, lastUrl.get()) && Objects.equals(pageSource, lastPageContent.get())) {
//
//                        log.debug("Waiting for {}... {} ms", message, System.currentTimeMillis() - start.get());
//                    } else {
//                        start.set(System.currentTimeMillis());
//                        log.debug("Current URL: {}", currentUrl);
//                        log.debug("Page: \n{}", pageSource);
//                        lastUrl.set(currentUrl);
//                        lastPageContent.set(pageSource);
//                    }
//                    assertAll(executableList);
//                });
//    }
//
//    private void waitForAlert(String expectedMessage) {
//        await()
//                .timeout(10, TimeUnit.SECONDS)
//                .pollInterval(100, TimeUnit.MILLISECONDS)
//                .untilAsserted(() -> {
//                    Alert alert = page.switchTo().alert();
//                    assertThat(alert.getTextContent()).isEqualTo(expectedMessage);
//                    alert.accept();
//                });
//    }

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
        }
    }
}