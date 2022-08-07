package net.shyshkin.study.oauth.spring.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class NewSpringAuthorizationServerApplicationTest {

    private static final String REDIRECT_URI = "http://127.0.0.1:8080/authorized";
    private static final TypeReference<Map<String, Object>> TOKEN_RESPONSE_TYPE_REFERENCE = new TypeReference<Map<String, Object>>() {
    };

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

    @Autowired
    MockMvc mockMvc;

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
    public void whenAskingForATokenWithAuthCodeThenReturnsToken_usingTestRestTemplate() throws IOException {

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

    @Test
    @Order(60)
    public void whenAskingForATokenWithAuthCodeThenReturnsToken_usingMockMvc() throws Exception {

        //given
        if (authorizationCode == null) whenLoggingInAndRequestingTokenThenRedirectsToClientApplication();

        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.set(OAuth2ParameterNames.GRANT_TYPE, AuthorizationGrantType.AUTHORIZATION_CODE.getValue());
        parameters.set(OAuth2ParameterNames.CODE, authorizationCode);
        parameters.set(OAuth2ParameterNames.REDIRECT_URI, REDIRECT_URI);

        HttpHeaders basicAuth = new HttpHeaders();
        basicAuth.setBasicAuth(CLIENT_ID, CLIENT_PASSWORD);

        MvcResult mvcResult = this.mockMvc.perform(post("/oauth2/token")
                        .params(parameters)
                        .headers(basicAuth))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, containsString(MediaType.APPLICATION_JSON_VALUE)))
                .andExpect(jsonPath("$.access_token").isNotEmpty())
                .andExpect(jsonPath("$.token_type").isNotEmpty())
                .andExpect(jsonPath("$.expires_in").value(allOf(greaterThan(0), lessThanOrEqualTo(300)), Integer.class))
                .andExpect(jsonPath("$.refresh_token").isNotEmpty())
                .andExpect(jsonPath("$.scope").isNotEmpty())
                .andExpect(jsonPath("$.id_token").isNotEmpty())
                .andReturn();

        ObjectMapper objectMapper = new ObjectMapper();
        String responseJson = mvcResult.getResponse().getContentAsString();
        Map<String, Object> responseMap = objectMapper.readValue(responseJson, TOKEN_RESPONSE_TYPE_REFERENCE);
        log.debug("Response body as Map: {}", responseMap);
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