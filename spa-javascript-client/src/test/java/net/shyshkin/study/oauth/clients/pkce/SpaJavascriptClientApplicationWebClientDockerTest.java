package net.shyshkin.study.oauth.clients.pkce;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.test.containers.KeycloakStackContainers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "app.oauth.uri=http://${OAUTH_HOST}:${OAUTH_PORT}"
})
@ContextConfiguration(initializers = SpaJavascriptClientApplicationWebClientDockerTest.Initializer.class)
class SpaJavascriptClientApplicationWebClientDockerTest {

    public static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    public static final String RESOURCE_OWNER_PASSWORD = "password_art_1";

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/authorized";

    private static final String AUTHORIZATION_REQUEST = UriComponentsBuilder
            .fromPath("/oauth2/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", "client1")
            .queryParam("scope", "openid read")
            .queryParam("state", "some-state")
            .queryParam("redirect_uri", REDIRECT_URI)
            .toUriString();

    private static final String TOKEN_REQUEST_URL = "/oauth2/token";

    private static final String CLIENT_ID = "client1";
    private static final String CLIENT_PASSWORD = "myClientSecretValue";

    private WebClient webClient;

    private String baseUri;

    RestTemplate restTemplate;

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


    @Container
    static GenericContainer<?> spaJavascriptClient = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-spa-javascript-client")
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
            .waitingFor(Wait.forHealthcheck());


    @BeforeEach
    void setUp() {

        this.webClient = new WebClient();
        this.baseUri = "http://localhost:" + spaJavascriptClient.getMappedPort(8080);
        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);
        this.webClient.getOptions().setThrowExceptionOnScriptError(false);
        this.webClient.getOptions().setRedirectEnabled(true);
        this.webClient.getCookieManager().clearCookies();    // log out

        this.restTemplate = new RestTemplateBuilder()
                .rootUri(baseUri)
                .build();
    }

    @Test
    void fullWorkflowTest() throws IOException {
        //given
        String indexPageUrl = baseUri;

        //visit index page should show correct content
        HtmlPage page = webClient.getPage(indexPageUrl);

        assertThat(page.getTitleText()).isEqualTo("Javascript Application with PKCE");
        assertThat(page.getHtmlElementById("redirectHostUri").getTextContent()).isEqualTo("http://spa-javascript-client:8080");
        assertThat(page.getHtmlElementById("oAuthServerUri").getTextContent()).isEqualTo("http://keycloak:8080");
        assertThat(page.getHtmlElementById("usersApiUri").getTextContent()).isEqualTo("http://users-service:8080");
        assertThat(page.getHtmlElementById("gatewayUri").getTextContent()).isEqualTo("http://gateway-service:8080");
        assertThat(page.getHtmlElementById("resourceServerUri").getAttribute("value")).isEqualTo("http://users-service:8080");

        //click on button `Generate Random State Value` should change text in `stateValue` field
        page.getHtmlElementById("generateStateBtn").click();
        assertThat(page.getHtmlElementById("stateValue").getTextContent()).isNotEqualTo("Some Value");

        //click on button `Generate Code Verifier Value` should change text in `codeVerifierValue` field
        page.getHtmlElementById("generateCodeVerifierBtn").click();
        assertThat(page.getHtmlElementById("codeVerifierValue").getTextContent()).isNotEqualTo("Code Verifier Value");

//        logCurrentPage();

        //click on button `Generate Code Challenge Value` should change text in `codeVerifierValue` field
        WebElement generateCodeChallengeBtn = page.getHtmlElementById("generateCodeChallengeBtn");
        generateCodeChallengeBtn.click();
        log.debug("generateCodeChallengeBtn: {}", generateCodeChallengeBtn);

        page.getHtmlElementById("generateCodeChallengeBtn").click();
        assertThat(page.getHtmlElementById("codeChallengeValue").getTextContent()).isNotEqualTo("Code Challenge Value");

        //click on button `Get Auth Code` should pop up new window for signing into keycloak
        page.getHtmlElementById("getAuthCodeBtn").click();

        assertLoginPage(page);

        //correct signing in should redirect to index page with Access Token Generated
        page = signIn(page, RESOURCE_OWNER_USERNAME, RESOURCE_OWNER_PASSWORD);


        assertSpaPage(page);
        assertThat(page.getHtmlElementById("accessToken").getTextContent())
                .isNotBlank()
                .hasSizeGreaterThan(20);

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
        HtmlButton signInButton = page.querySelector("button");

        usernameInput.type(username);
        passwordInput.type(password);
        return signInButton.click();
    }

    private static void assertLoginPage(HtmlPage page) {
        assertThat(page.getUrl().toString()).endsWith("/login");

        HtmlInput usernameInput = page.querySelector("input[name=\"username\"]");
        HtmlInput passwordInput = page.querySelector("input[name=\"password\"]");
        HtmlButton signInButton = page.querySelector("button");
        String title = page.getTitleText();

        assertThat(title).isEqualTo("Sign in to katarinazart");
        assertThat(usernameInput).isNotNull();
        assertThat(passwordInput).isNotNull();
        assertThat(signInButton.getTextContent()).isEqualTo("Sign in");
    }

    private static void assertSpaPage(HtmlPage page) {
        assertThat(page.getUrl().toString()).endsWith("/login");
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