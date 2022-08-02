package net.shyshkin.study.oauth.ws.api.users;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.api.users.dto.OAuthResponse;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Disabled("Only for manual testing - Start keycloak-postgres docker compose file")
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ResourceServerApplicationManualTests {

    public static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    public static final String RESOURCE_OWNER_PASSWORD = "password_art_1";

    @Container
    public static BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
            .withCapabilities(new FirefoxOptions());

    static {
        org.testcontainers.Testcontainers.exposeHostPorts(8080);
    }

    RemoteWebDriver driver;

    @Autowired
    RestTemplateBuilder restTemplateBuilder;
    private RestTemplate keycloakRestTemplate;

    @Autowired
    TestRestTemplate testRestTemplate;
    static String jwtAccessToken;

    @BeforeEach
    void setUp() {

        driver = browser.getWebDriver();

        keycloakRestTemplate = restTemplateBuilder.rootUri("http://localhost:8080").build();
    }

    @Test
    @Order(10)
    void totalWorkflowTest() {

        String code = getAuthorizationCode();

        log.debug("Code from keycloak: {}", code);

        jwtAccessToken = getAccessToken(code);

        log.debug("Jwt Access Token: {}", jwtAccessToken);

        RequestEntity<?> requestEntity = RequestEntity
                .get("/users/status/check")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                .build();
        ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo("Working...");
    }

    @Test
    @Order(40)
    @Disabled("org.springframework.web.client.HttpClientErrorException$Unauthorized: 401 Unauthorized: [{\"error\":\"invalid_token\",\"error_description\":\"Token verification failed\"}]")
    void getUserInfoFromOAuthServer() {

        //given
        assertThat(jwtAccessToken).isNotEmpty();
        log.debug("JWT Token: {}", jwtAccessToken);

        //when
        var requestEntity = RequestEntity
                .get("/auth/realms/katarinazart/protocol/openid-connect/userinfo")
                .headers(headers -> headers.setBearerAuth(jwtAccessToken))
                .build();

        AtomicReference<ResponseEntity<String>> responseEntityAtomic = new AtomicReference<>();
        await()
                .timeout(3, TimeUnit.SECONDS)
                .pollInterval(1, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    try {
                        var responseEntity = keycloakRestTemplate
                                .exchange(requestEntity, String.class);
                        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
                        responseEntityAtomic.set(responseEntity);
                    } catch (Exception e) {
                        log.debug("Exception:", e);
                        assertThat(false).isTrue();
                    }
                });

        //then
        ResponseEntity<String> responseEntity = responseEntityAtomic.get();
        String body = responseEntity.getBody();

//        {
//            "sub": "624ba8cd-b02f-4405-b6e7-6855a4bb3452",
//            "email_verified": false,
//            "name": "Artem Shyshkin",
//            "preferred_username": "shyshkin.art",
//            "given_name": "Artem",
//            "family_name": "Shyshkin",
//            "email": "d.art.shishkin@gmail.com"
//        }

        assertThat(body)
                .isNotEmpty()
                .contains("email_verified")
                .contains("name")
                .contains("preferred_username")
                .contains("shyshkin.art")
                .contains("Artem Shyshkin")
                .contains("given_name")
                .contains("family_name")
                .contains("email")
                .contains("d.art.shishkin@gmail.com")
        ;

        log.debug("Response body: {}", body);

    }


    private String getAccessToken(String code) {
        //when
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "authorization_code");
        map.add("client_id", "photo-app-code-flow-client");
        map.add("client_secret", "ee68a49e-5ac6-4673-9465-51e53de3fb0e");
        map.add("code", code);
        map.add("redirect_uri", "http://localhost:8083/callback");
        map.add("scope", "openid profile");

        HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(map, headers);

        var responseEntity = keycloakRestTemplate
                .postForEntity("/auth/realms/katarinazart/protocol/openid-connect/token",
                        requestEntity,
                        OAuthResponse.class);

        //then
        log.debug("Response from OAuth2.0 server: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        OAuthResponse oAuthResponse = responseEntity.getBody();
        assertThat(oAuthResponse)
                .hasNoNullFieldsOrProperties();

        return oAuthResponse.getAccessToken();
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
        WebElement usernameField = driver.findElement(By.id("username"));
        usernameField.sendKeys(username);

        //id = "password"
        WebElement passwordField = driver.findElement(By.id("password"));
        passwordField.sendKeys(password);
        passwordField.submit();
    }

}
