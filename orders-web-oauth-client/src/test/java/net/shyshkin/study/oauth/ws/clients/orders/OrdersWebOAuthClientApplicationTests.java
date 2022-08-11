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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
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
        "app.oauth.uri=http://host.docker.internal:8000"
})
@ContextConfiguration(initializers = OrdersWebOAuthClientApplicationTests.Initializer.class)
class OrdersWebOAuthClientApplicationTests {

    private static final String DEFAULT_USERNAME = "art";
    private static final String CORRECT_PASSWORD = "art_pass";

    private String webAppUri;

    @LocalServerPort
    int serverPort;

    private WebClient webClient;

    private static final int hostPort = 8000;
    private static final int containerExposedPort = 8080;
    private static final Consumer<CreateContainerCmd> cmd = e -> e.withPortBindings(new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(containerExposedPort)));

    static Network network = Network.SHARED;

    @Container
    static GenericContainer<?> newSpringAuthServer = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-new-spring-authorization-server")
            .withNetworkAliases("auth-server")
            .withEnv("app.auth-server.provider.issuer", "http://host.docker.internal:8000")
            .withEnv("app.client.baseUri", "http://127.0.0.1:8180")
            .withExposedPorts(8080)
            .withNetwork(network)
            .withCreateContainerCmdModifier(cmd)
//            .withLogConsumer(new Slf4jLogConsumer(log))
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> ordersService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-orders-resource-server")
            .withNetwork(network)
            .withNetworkAliases("orders-service")
            .withEnv("spring.security.oauth2.resourceserver.jwt.issuer-uri", "http://host.docker.internal:8000")
            .withExposedPorts(8080)
            .withLogConsumer(new Slf4jLogConsumer(log))
            .dependsOn(newSpringAuthServer)
            .waitingFor(Wait.forHealthcheck());

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