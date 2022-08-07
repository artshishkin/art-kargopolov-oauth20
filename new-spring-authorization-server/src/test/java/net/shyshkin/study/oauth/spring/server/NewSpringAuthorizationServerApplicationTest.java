package net.shyshkin.study.oauth.spring.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebResponse;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NewSpringAuthorizationServerApplicationTest {

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

    private static final String DEFAULT_USERNAME = "art";
    private static final String CORRECT_PASSWORD = "art_pass";

    private static final String CLIENT_ID = "client1";
    private static final String CLIENT_PASSWORD = "myClientSecretValue";

    @Autowired
    private WebClient webClient;

    @Autowired
    TestRestTemplate restTemplate;

    private static String authorizationCode;

    @BeforeEach
    void setUp() {
        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(true);
        this.webClient.getOptions().setRedirectEnabled(true);
        this.webClient.getCookieManager().clearCookies();    // log out
    }

    @Test
    void contextLoads() {
    }

    @Test
    @Order(10)
    public void whenLoginSuccessfulThenDisplayNotFoundError() throws IOException {
        HtmlPage page = this.webClient.getPage("/");

        assertLoginPage(page);

        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        WebResponse signInResponse = signIn(page, DEFAULT_USERNAME, CORRECT_PASSWORD).getWebResponse();
        assertThat(signInResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());    // there is no "default" index page
    }

    @Test
    @Order(20)
    public void whenLoginFailsThenDisplayBadCredentials() throws IOException {
        HtmlPage page = this.webClient.getPage("/");
        HtmlPage loginErrorPage = signIn(page, DEFAULT_USERNAME, "wrong-password");

        HtmlElement alert = loginErrorPage.querySelector("div[role=\"alert\"]");
        assertThat(alert).isNotNull();
        assertThat(alert.getTextContent()).isEqualTo("Bad credentials");
    }

    @Test
    @Order(30)
    public void whenNotLoggedInAndRequestingTokenThenRedirectsToLogin() throws IOException {
        HtmlPage page = this.webClient.getPage(AUTHORIZATION_REQUEST);

        assertLoginPage(page);
    }

    @Test
    @Order(40)
    public void whenLoggingInAndRequestingTokenThenRedirectsToClientApplication() throws IOException {
        // Log in
        this.webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
        this.webClient.getOptions().setRedirectEnabled(false);
        signIn(this.webClient.getPage("/login"), DEFAULT_USERNAME, CORRECT_PASSWORD);

        // Request Authorization code
        WebResponse response = this.webClient.getPage(AUTHORIZATION_REQUEST).getWebResponse();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.MOVED_PERMANENTLY.value());
        String location = response.getResponseHeaderValue("location");
        assertThat(location).startsWith(REDIRECT_URI);
        assertThat(location).contains("code=");
        log.debug("Location: {}", location);

        authorizationCode = extractCode(location);

        log.debug("Code: {}", authorizationCode);

    }

    @Test
    @Order(50)
    public void whenAskingForATokenWithAuthCodeThenReturnsToken() throws IOException {

        //given
        if (authorizationCode == null) whenLoggingInAndRequestingTokenThenRedirectsToClientApplication();

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

        authorizationCode = null;
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