package net.shyshkin.study.oauth.ws.clients.orders;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.*;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestPropertySource(properties = {
        "server.port=8180",
        "app.services.orders.uri=http://${ORDERS_SERVICE_HOST}:${ORDERS_SERVICE_PORT}/orders",
        "app.oauth.uri=http://host.testcontainers.internal:8000"
})
// for local tests add host.testcontainers.internal to /etc/hosts as 127.0.0.1
@ContextConfiguration(initializers = OrdersWebOAuthClientApplicationTests.Initializer.class)
class OrdersWebOAuthClientApplicationTests {

    private static final String DEFAULT_USERNAME = "art";
    private static final String CORRECT_PASSWORD = "art_pass";

    private static final String USER_USERNAME = DEFAULT_USERNAME;
    private static final String USER_PASSWORD = CORRECT_PASSWORD;

    private static final String ADMIN_USERNAME = "kate";
    private static final String ADMIN_PASSWORD = "kate_pass";

    private String webAppUri;

    @LocalServerPort
    int serverPort;

    private WebClient webClient;

    private static final int hostPort = 8000;
    private static final int containerExposedPort = 8080;
    private static final Consumer<CreateContainerCmd> cmd = e -> e.withPortBindings(new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(containerExposedPort)));

    @Container
    static GenericContainer<?> newSpringAuthServer = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-new-spring-authorization-server")
            .withNetworkAliases("auth-server")
            .withEnv("app.auth-server.provider.issuer", "http://host.testcontainers.internal:8000")
            .withEnv("app.client.baseUri", "http://127.0.0.1:8180")
            .withExposedPorts(8080)
            .withCreateContainerCmdModifier(cmd)
//            .withLogConsumer(new Slf4jLogConsumer(log))
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> ordersService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-orders-resource-server")
            .withNetworkAliases("orders-service")
            .withEnv("spring.security.oauth2.resourceserver.jwt.issuer-uri", "http://host.testcontainers.internal:8000")
            .withAccessToHost(true)
            .withExposedPorts(8080)
            .withLogConsumer(new Slf4jLogConsumer(log))
            .dependsOn(newSpringAuthServer)
            .waitingFor(Wait.forHealthcheck());

    @BeforeAll
    static void beforeAll() {
        org.testcontainers.Testcontainers.exposeHostPorts(8000);
    }

    @BeforeEach
    void setUp() throws IOException {
        this.webClient = new WebClient();
        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);
        this.webClient.getOptions().setRedirectEnabled(true);
        this.webClient.getCookieManager().clearCookies();    // log out

        webAppUri = "http://127.0.0.1:" + serverPort;
    }

    @Test
    @Order(1)
    void contextLoads() {
    }

    @Test
    @Order(10)
    public void whenNotLoggedInThenRedirectsToLogin() throws IOException {

        HtmlPage page = this.webClient.getPage(webAppUri + "/orders");

        assertLoginPage(page);
    }

    @Test
    @Order(20)
    public void whenLoginSuccessfulThenDisplayNotFoundError() throws IOException {
        log.debug("WebAppUri: {}", webAppUri);
        HtmlPage page = this.webClient.getPage(webAppUri + "/");

        assertLoginPage(page);

        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        WebResponse signInResponse = signIn(page, DEFAULT_USERNAME, CORRECT_PASSWORD).getWebResponse();
        log.debug("SignIn Response:\n{}", signInResponse.getContentAsString());
        assertThat(signInResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());    // there is no "default" index page
    }

    @Test
    @Order(30)
    public void whenLoginFailsThenDisplayBadCredentials() throws IOException {
        HtmlPage page = this.webClient.getPage(webAppUri + "/");
        HtmlPage loginErrorPage = signIn(page, DEFAULT_USERNAME, "wrong-password");

        HtmlElement alert = loginErrorPage.querySelector("div[role=\"alert\"]");
        assertThat(alert).isNotNull();
        assertThat(alert.getTextContent()).isEqualTo("Bad credentials");
    }

    @Test
    @Order(40)
    public void whenLoginSuccessfulThenReturnOrders() throws IOException {

        HtmlPage page = this.webClient.getPage(webAppUri + "/orders");

        assertLoginPage(page);

        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        HtmlPage ordersPage = signIn(page, DEFAULT_USERNAME, CORRECT_PASSWORD);
        assertThat(ordersPage.getWebResponse().getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(ordersPage.getTitleText()).isEqualTo("Orders");
        log.debug("Page toString: {}", ordersPage.getPage().getTextContent());
        List<DomElement> ordersIdList = ordersPage.getElementsByTagName("span");
        assertThat(ordersIdList)
                .hasSize(5)
                .allSatisfy(domElement -> {
                            UUID uuid = UUID.fromString(domElement.getTextContent()); //if wrong it will throw java.lang.IllegalArgumentException
                            log.debug("Order's id: {}", uuid);
                        }
                );
    }

    @Test
    @Order(50)
    public void whenLoginSuccessful_withRoleUser_ThenHasAccessToUserOrders() throws IOException {

        HtmlPage page = this.webClient.getPage(webAppUri + "/user/orders");

        assertLoginPage(page);

        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        HtmlPage ordersPage = signIn(page, USER_USERNAME, USER_PASSWORD);
        assertThat(ordersPage.getWebResponse().getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(ordersPage.getTitleText()).isEqualTo("Orders");
        List<DomElement> ordersIdList = ordersPage.getElementsByTagName("span");
        assertThat(ordersIdList)
                .hasSize(5)
                .allSatisfy(domElement -> {
                            UUID uuid = UUID.fromString(domElement.getTextContent()); //if wrong it will throw java.lang.IllegalArgumentException
                            log.debug("Order's id: {}", uuid);
                        }
                );
    }

    @Test
    @Order(51)
    public void whenLoginSuccessful_withRoleUser_ThenHas_NO_AccessToAdminOrders() throws IOException {

        HtmlPage page = this.webClient.getPage(webAppUri + "/admin/orders");

        assertLoginPage(page);

        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        HtmlPage ordersPage = signIn(page, USER_USERNAME, USER_PASSWORD);
        assertThat(ordersPage.getWebResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(ordersPage.getWebResponse().getContentAsString()).contains("Whitelabel Error Page");
    }

    @ParameterizedTest
    @Order(60)
    @ValueSource(strings = {
            "/orders", "/user/orders", "/admin/orders"
    })
    public void whenLoginSuccessful_withRoleAdmin_ThenHasAccessToAnyOrdersEndpoint(String endpointUri) throws IOException {

        HtmlPage page = this.webClient.getPage(webAppUri + endpointUri);

        assertLoginPage(page);

        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        HtmlPage ordersPage = signIn(page, ADMIN_USERNAME, ADMIN_PASSWORD);
        assertThat(ordersPage.getWebResponse().getStatusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(ordersPage.getTitleText()).isEqualTo("Orders");

        List<DomElement> ordersIdList = ordersPage.getElementsByTagName("span");
        assertThat(ordersIdList)
                .hasSize(5)
                .allSatisfy(domElement -> {
                            UUID uuid = UUID.fromString(domElement.getTextContent()); //if wrong it will throw java.lang.IllegalArgumentException
                            log.debug("Order's id: {}", uuid);
                        }
                );
    }

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

        assertThat(usernameInput).isNotNull();
        assertThat(passwordInput).isNotNull();
        assertThat(signInButton.getTextContent()).isEqualTo("Sign in");
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            System.setProperty("ORDERS_SERVICE_HOST", ordersService.getHost());
            Integer ordersServicePort = ordersService.getMappedPort(8080);
            System.setProperty("ORDERS_SERVICE_PORT", String.valueOf(ordersServicePort));
        }
    }
}