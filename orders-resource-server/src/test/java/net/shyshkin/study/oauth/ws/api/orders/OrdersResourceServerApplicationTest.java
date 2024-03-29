package net.shyshkin.study.oauth.ws.api.orders;

import com.fasterxml.jackson.databind.JsonNode;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.api.orders.dto.OrderRest;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("local")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrdersResourceServerApplicationTest {

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/authorized";

    private static final String AUTHORIZATION_REQUEST = UriComponentsBuilder
            .fromPath("/oauth2/authorize")
            .queryParam("response_type", "code")
            .queryParam("client_id", "client1")
            .queryParam("scope", "openid read authorities")
            .queryParam("state", "some-state")
            .queryParam("redirect_uri", REDIRECT_URI)
            .toUriString();

    private static final String TOKEN_REQUEST_URL = "/oauth2/token";

    private static final String USER_USERNAME = "art";
    private static final String USER_PASSWORD = "art_pass";

    private static final String ADMIN_USERNAME = "kate";
    private static final String ADMIN_PASSWORD = "kate_pass";

    private static final String CLIENT_ID = "client1";
    private static final String CLIENT_PASSWORD = "myClientSecretValue";
    public static final ParameterizedTypeReference<List<OrderRest>> ORDERS_TYPE = new ParameterizedTypeReference<>() {
    };

    private WebClient webClient;

    private String baseUri;

    RestTemplate restTemplate;

    private static Map<String, String> accessTokens = new HashMap<>();

    @Autowired
    TestRestTemplate testRestTemplate;

    private static final int hostPort = 8000;
    private static final int containerExposedPort = 8080;
    private static final Consumer<CreateContainerCmd> cmd = e -> e.withPortBindings(new PortBinding(Ports.Binding.bindPort(hostPort), new ExposedPort(containerExposedPort)));

    @Container
    static GenericContainer<?> newSpringAuthServer = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-new-spring-authorization-server")
            .withNetworkAliases("new-spring-auth-server")
            .withEnv("app.auth-server.provider.issuer", "http://localhost:8000")
            .withExposedPorts(8080)
            .withCreateContainerCmdModifier(cmd)
            .waitingFor(Wait.forHealthcheck());

    @BeforeEach
    void setUp() throws IOException {
        this.webClient = new WebClient();
        this.baseUri = "http://localhost:" + newSpringAuthServer.getMappedPort(8080);
        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);
        this.webClient.getOptions().setRedirectEnabled(true);
        this.webClient.getCookieManager().clearCookies();    // log out

        this.restTemplate = new RestTemplateBuilder()
                .rootUri(baseUri)
                .build();

    }

    @Test
    @Order(10)
    void whenRequestingOrdersEndpointWithoutTokenThenShouldReturn401Unauthorized() {

        //when
        var responseEntity = testRestTemplate.exchange("/orders", HttpMethod.GET, null, ORDERS_TYPE);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(responseEntity.getBody()).isNullOrEmpty();

    }

    @Test
    @Order(20)
    void whenRequestingHealthEndpointWithoutTokenThenShouldReturn200() {

        //when
        var responseEntity = testRestTemplate.getForEntity("/actuator/health", JsonNode.class);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isNotNull();
        assertThat(responseEntity.getBody().at("/status").asText()).isEqualTo("UP");

    }

    @Test
    @Order(30)
    void whenRequestingOrdersEndpointWithTokenThenShouldReturnCorrectOrders() {

        //given
        String accessToken = getAccessToken(USER_USERNAME, USER_PASSWORD);
        RequestEntity<Void> requestEntity = RequestEntity
                .get("/orders")
                .headers(h -> h.setBearerAuth(accessToken))
                .build();

        //when
        var responseEntity = testRestTemplate.exchange(requestEntity, ORDERS_TYPE);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody())
                .isNotEmpty()
                .hasSize(5)
                .allSatisfy(order -> assertAll(
                        () -> assertThat(order)
                                .isNotNull()
                                .hasNoNullFieldsOrProperties(),
                        () -> assertThat(order.getProductId())
                                .startsWith("product-id-"),
                        () -> assertThat(order.getUserId())
                                .startsWith("user-id-"),
                        () -> assertThat(order.getQuantity())
                                .isGreaterThanOrEqualTo(1)
                                .isLessThanOrEqualTo(10)
                ));
    }

    @Test
    @Order(40)
    void whenUsingAccessTokenWithUserRole_thenHasAccessToUserOrdersEndpoint() {

        //given
        String accessToken = getAccessToken(USER_USERNAME, USER_PASSWORD);
        RequestEntity<Void> requestEntity = RequestEntity
                .get("/user/orders")
                .headers(h -> h.setBearerAuth(accessToken))
                .build();

        //when
        var responseEntity = testRestTemplate.exchange(requestEntity, ORDERS_TYPE);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody())
                .isNotEmpty()
                .hasSize(5)
                .allSatisfy(order -> assertAll(
                        () -> assertThat(order)
                                .isNotNull()
                                .hasNoNullFieldsOrProperties(),
                        () -> assertThat(order.getProductId())
                                .startsWith("product-id-"),
                        () -> assertThat(order.getUserId())
                                .startsWith("user-id-"),
                        () -> assertThat(order.getQuantity())
                                .isGreaterThanOrEqualTo(1)
                                .isLessThanOrEqualTo(10)
                ));
    }

    @Test
    @Order(50)
    @DisplayName("When user with USER_ROLE authority tries to access admin orders endpoint then he must be FORBIDDEN")
    void whenUsingAccessTokenWithUserRole_thenHas_NO_AccessToAdminOrdersEndpoint() {

        //given
        String accessToken = getAccessToken(USER_USERNAME, USER_PASSWORD);
        RequestEntity<Void> requestEntity = RequestEntity
                .get("/admin/orders")
                .headers(h -> h.setBearerAuth(accessToken))
                .build();

        //when
        var responseEntity = testRestTemplate.exchange(requestEntity, String.class);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(responseEntity.getBody())
                .isNullOrEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/orders", "/user/orders", "/admin/orders"
    })
    @Order(60)
    @DisplayName("When user with ADMIN_ROLE authority tries to access user or admin orders endpoints then he must be OK")
    void whenUsingAccessTokenWithAdminRole_thenHasAccessToAdminOrdersEndpoint(String endpointUri) {

        //given
        String accessToken = getAccessToken(ADMIN_USERNAME, ADMIN_PASSWORD);
        RequestEntity<Void> requestEntity = RequestEntity
                .get(endpointUri)
                .headers(h -> h.setBearerAuth(accessToken))
                .build();

        //when
        var responseEntity = testRestTemplate.exchange(requestEntity, ORDERS_TYPE);

        //then
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody())
                .isNotEmpty()
                .hasSize(5)
                .allSatisfy(order -> assertAll(
                        () -> assertThat(order)
                                .isNotNull()
                                .hasNoNullFieldsOrProperties(),
                        () -> assertThat(order.getProductId())
                                .startsWith("product-id-"),
                        () -> assertThat(order.getUserId())
                                .startsWith("user-id-"),
                        () -> assertThat(order.getQuantity())
                                .isGreaterThanOrEqualTo(1)
                                .isLessThanOrEqualTo(10)
                ));
    }

    private String getAccessToken(String username, String password) {
        String token = accessTokens.get(username);
        if (token == null) {
            try {
                token = createAccessToken(username, password);
            } catch (IOException e) {
                log.error("Can not get access token", e);
                throw new RuntimeException(e);
            }
        }
        return token;
    }

    private String createAccessToken(String username, String password) throws IOException {

        //given
        String authorizationCode = getAuthorizationCode(username, password);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setBasicAuth(CLIENT_ID, CLIENT_PASSWORD);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();

        map.add("grant_type", "authorization_code");
        map.add("redirect_uri", REDIRECT_URI);
        map.add("code", authorizationCode);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

        //when
        ResponseEntity<JsonNode> response = restTemplate.postForEntity(TOKEN_REQUEST_URL, request, JsonNode.class);

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode responseBody = response.getBody();
        log.debug("Response: {}", responseBody);
        assertThat(responseBody).isNotNull();
        assertThat(responseBody.at("/access_token").asText()).isNotEmpty();
        assertThat(responseBody.at("/refresh_token").asText()).isNotEmpty();
        assertThat(responseBody.at("/scope").asText()).contains("read", "openid");
        assertThat(responseBody.at("/id_token").asText()).isNotEmpty();
        assertThat(responseBody.at("/token_type").asText()).isEqualTo("Bearer");
        assertThat(responseBody.at("/expires_in").asInt()).isGreaterThan(0).isLessThanOrEqualTo(300);

        return responseBody.at("/access_token").asText();
    }

    private String getAuthorizationCode(String username, String password) throws IOException {
        // Log in
        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        this.webClient.getOptions().setRedirectEnabled(false);
        signIn(this.webClient.getPage(baseUri + "/login"), username, password);

        // Request Authorization code
        WebResponse response = this.webClient.getPage(baseUri + AUTHORIZATION_REQUEST).getWebResponse();

        assertThat(response.getStatusCode()).isIn(HttpStatus.MOVED_PERMANENTLY.value(), HttpStatus.FOUND.value());
        String location = response.getResponseHeaderValue("location");
        assertThat(location).startsWith(REDIRECT_URI);
        assertThat(location).contains("code=");
        log.debug("Location: {}", location);

        String authorizationCode = extractCode(location);

        log.debug("Code: {}", authorizationCode);

        return authorizationCode;
    }

    private String extractCode(String location) {
        int indexOfCode = location.indexOf("code=");
        return location.substring(indexOfCode + 5).split("&")[0];
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

}