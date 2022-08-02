package net.shyshkin.study.oauth.ws.api.photos.security;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.test.containers.KeycloakStackContainers;
import net.shyshkin.study.oauth.ws.api.photos.dto.OAuthResponse;
import net.shyshkin.study.oauth.ws.api.photos.dto.PhotoDto;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://${OAUTH_HOST}:${OAUTH_PORT}/auth/realms/katarinazart/protocol/openid-connect/certs"
})
@ContextConfiguration(initializers = WebSecurityTest.Initializer.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WebSecurityTest {

    public static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    public static final String RESOURCE_OWNER_PASSWORD = "password_art_1";

    @Container
    static KeycloakStackContainers keycloakStackContainers = KeycloakStackContainers.getInstance();

    static GenericContainer<?> keycloak = keycloakStackContainers.getKeycloak();

    static Network network = keycloakStackContainers.getStackNetwork();

    @Container
    static BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
            .withCapabilities(new FirefoxOptions())
            .withNetwork(network)
            .withNetworkAliases("browser")
            .dependsOn(keycloak);

    RemoteWebDriver driver;

    @Autowired
    RestTemplateBuilder restTemplateBuilder;
    private RestTemplate keycloakRestTemplate;

    @Autowired
    TestRestTemplate testRestTemplate;

    static String jwtAccessToken;
    public static final ParameterizedTypeReference<List<PhotoDto>> PHOTOS_DTO_LIST_TYPE = new ParameterizedTypeReference<List<PhotoDto>>() {
    };

    @BeforeEach
    void setUp() {

        driver = browser.getWebDriver();
        Integer keycloakPort = keycloak.getMappedPort(8080);
        String keycloakHost = keycloak.getHost();
        String keycloakUri = String.format("http://%s:%d", keycloakHost, keycloakPort);

        keycloakRestTemplate = restTemplateBuilder
                .rootUri(keycloakUri)
                .build();

        checkJwtExists();
    }

    @Test
    @Order(20)
    void getAlbums_correct() {

        //when
        RequestEntity<?> requestEntity = RequestEntity
                .get("/photos")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                .build();
        ResponseEntity<List<PhotoDto>> responseEntity = testRestTemplate.exchange(requestEntity, PHOTOS_DTO_LIST_TYPE);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<PhotoDto> body = responseEntity.getBody();
        assertThat(body)
                .hasSize(2)
                .allSatisfy(photoDto -> assertThat(photoDto).hasNoNullFieldsOrProperties());
    }

    @Test
    @Order(30)
    void getAlbums_absentToken_UNAUTHORIZED() {

        //when
        RequestEntity<?> requestEntity = RequestEntity
                .get("/photos")
                .build();
        var responseEntity = testRestTemplate.exchange(requestEntity, String.class);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(responseEntity.getBody()).isBlank();
    }

    @Test
    @Order(40)
    void getAlbums_absentEndpoint_404_NOT_FOUND() throws JSONException {

        //when
        RequestEntity<?> requestEntity = RequestEntity
                .get("/photozzz")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                .build();
        var responseEntity = testRestTemplate.exchange(requestEntity, String.class);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        JSONObject json = new JSONObject(responseEntity.getBody());
        assertThat(json).isNotNull();
        log.debug("Converted Json Object: {}", json);
        assertAll(
                () -> assertThat(json.has("timestamp")).isTrue(),
                () -> assertThat(json.getInt("status")).isEqualTo(404),
                () -> assertThat(json.getString("error")).isEqualTo("Not Found"),
                () -> assertThat(json.getString("path")).isEqualTo("/photozzz")
        );
    }

    @Test
    @Order(50)
    void getAlbums_tokenExpired_UNAUTHORIZED() {

        //given
        String expiredToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICI2YmJadUpFNVFwc0xCTlc5STNEbzFQRVVjci1VQmFicjdiNkR6NmVvNnhRIn0.eyJleHAiOjE2MjM4MDcyNDAsImlhdCI6MTYyMzgwNjk0MCwiYXV0aF90aW1lIjoxNjIzODA2OTA4LCJqdGkiOiI0OThlYjA4NC1mYzA2LTQxMmQtOGVmMS02Y2EyMGUxZTYyNDYiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODAvYXV0aC9yZWFsbXMva2F0YXJpbmF6YXJ0IiwiYXVkIjoiYWNjb3VudCIsInN1YiI6IjYyNGJhOGNkLWIwMmYtNDQwNS1iNmU3LTY4NTVhNGJiMzQ1MiIsInR5cCI6IkJlYXJlciIsImF6cCI6InBob3RvLWFwcC1jb2RlLWZsb3ctY2xpZW50Iiwic2Vzc2lvbl9zdGF0ZSI6IjU5ZmJjMTkwLThiZjAtNGUyZS05MjUyLWIwMDE3MWFhY2Q2YSIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiZGVmYXVsdC1yb2xlcy1rYXRhcmluYXphcnQiLCJvZmZsaW5lX2FjY2VzcyIsImRldmVsb3BlciIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcHJvZmlsZSIsIm5hbWUiOiJBcnRlbSBTaHlzaGtpbiIsInByZWZlcnJlZF91c2VybmFtZSI6InNoeXNoa2luLmFydCIsImdpdmVuX25hbWUiOiJBcnRlbSIsImZhbWlseV9uYW1lIjoiU2h5c2hraW4ifQ.EwNS0StvqBQiUxlP_jw7WXws8S3UFnguzhYP0NFovPtUMJLEjvS73ITZ4GPsknn2VFqw3nXQb1a2eJkoeyeisvi9u36r9gDiO_nwg6hjywdx_8xuxkKYrCs2hagr8UnVBnsfwoKzQ3syzNM5L2vREQlKLfEae6xMDG2JdOUPvx6q7d8BtMfaQPiEQznL2vx1EHp0COBDX762_SU2FSXunQHq2WwVgg9mz1TpHoYSS7vpHatJSjIJs1pJTdTf5w8XJfaFdedPubIoIoFYaM2cuUwpUwvzw4GaukpGWLYZXl7vVqY9w_Td2c9QjxNVdLuunmQnL9DPWi4_gISYr6o0DA";

        //when
        RequestEntity<?> requestEntity = RequestEntity
                .get("/photos")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(expiredToken))
                .build();
        var responseEntity = testRestTemplate.exchange(requestEntity, String.class);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(responseEntity.getBody()).isBlank();
    }

    @Test
    @Order(60)
    void getAlbums_fakeToken_UNAUTHORIZED() {

        //given
        String fakeToken = "some_fake_token";

        //when
        RequestEntity<?> requestEntity = RequestEntity
                .get("/photos")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(fakeToken))
                .build();
        var responseEntity = testRestTemplate.exchange(requestEntity, String.class);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(responseEntity.getBody()).isBlank();
    }

    @Test
    @Order(60)
    void getSecretAlbums_noAccess_FORBIDDEN() {

        //when
        RequestEntity<?> requestEntity = RequestEntity
                .get("/photos/super-secret")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                .build();
        var responseEntity = testRestTemplate.exchange(requestEntity, String.class);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(responseEntity.getBody()).isBlank();
    }

    private void checkJwtExists() {
        if (jwtAccessToken == null) {
            String code = getAuthorizationCode("openid profile");
            log.debug("Code from keycloak: {}", code);
            jwtAccessToken = getAccessToken(code);
        }
        log.debug("Jwt Access Token: {}", jwtAccessToken);
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
                .hasFieldOrProperty("accessToken");

        return oAuthResponse.getAccessToken();
    }

    private void askOAuthServerForAuthorizationCode(String scope) {

        String url = String.format("http://keycloak:8080/auth/realms/katarinazart/protocol/openid-connect/auth?response_type=code&client_id=photo-app-code-flow-client&scope=%s&state=jskd879sdkj&redirect_uri=http://localhost:8083/callback",
                scope);
        log.debug("Browser container: {}", browser);
        driver.get(url);

        String expectedTitle = "Sign in to katarinazart";
        assertThat(driver.getTitle()).isEqualTo(expectedTitle);

        log.debug("Driver: {}", driver);
//        log.debug("Page Source: {}", driver.getPageSource());
    }

    private String getAuthorizationCode(String scope) {

        try {
            askOAuthServerForAuthorizationCode(scope);

            signIn(RESOURCE_OWNER_USERNAME, RESOURCE_OWNER_PASSWORD);
        } catch (Exception e) {
            log.debug("Exception while getting an Authorization Code", e);
        }
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

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            log.debug("ResourceServerApplicationTests.Initializer.initialize()");
            System.setProperty("OAUTH_HOST", keycloak.getHost());
            System.setProperty("OAUTH_PORT", String.valueOf(keycloak.getMappedPort(8080)));
        }
    }
}
