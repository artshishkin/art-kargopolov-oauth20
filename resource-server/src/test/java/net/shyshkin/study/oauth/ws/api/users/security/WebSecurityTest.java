package net.shyshkin.study.oauth.ws.api.users.security;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.test.containers.KeycloakStackContainers;
import net.shyshkin.study.oauth.ws.api.users.dto.OAuthResponse;
import net.shyshkin.study.oauth.ws.api.users.dto.UserDto;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
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

import java.util.UUID;
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

    static OAuthResponse oAuthResponse;
    static String currentUsername = RESOURCE_OWNER_USERNAME;
    static String currentPassword = RESOURCE_OWNER_PASSWORD;

    @BeforeEach
    void setUp() {

        driver = browser.getWebDriver();
        Integer keycloakPort = keycloak.getMappedPort(8080);
        String keycloakHost = keycloak.getHost();
        String keycloakUri = String.format("http://%s:%d", keycloakHost, keycloakPort);

        keycloakRestTemplate = restTemplateBuilder
                .rootUri(keycloakUri)
                .build();
    }

    @AfterEach
    void tearDown() {
        logout(true);
    }

    @Test
    @Order(20)
    void withoutUsingProperScope() {

        String code = getAuthorizationCode("openid");

        log.debug("Code from keycloak: {}", code);

        String jwtAccessToken = requestNewAccessToken(code);

        log.debug("Jwt Access Token: {}", jwtAccessToken);

        RequestEntity<?> requestEntity = RequestEntity
                .get("/users/scope/status/check")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                .build();
        ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    @Order(30)
    void withProperScope() {

        String code = getAuthorizationCode("openid profile");

        log.debug("Code from keycloak: {}", code);

        String jwtAccessToken = requestNewAccessToken(code);

        log.debug("Jwt Access Token: {}", jwtAccessToken);

        RequestEntity<?> requestEntity = RequestEntity
                .get("/users/scope/status/check")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                .build();
        ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo("Working...");
    }

    @Test
    @Order(40)
    void developerHasAccess() {

        checkJwtExists();

        RequestEntity<?> requestEntity = RequestEntity
                .get("/users/role/developer/status/check")
                .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                .build();
        ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(responseEntity.getBody()).isEqualTo("Working...");
    }

    @ParameterizedTest
    @Order(50)
    @ValueSource(strings = {
            "/users/role/admin/status/check",
            "/users/role/no_developer/status/check",
    })
    void developerHasNoAccess(String uri) {

        checkJwtExists();

        RequestEntity<?> requestEntity = RequestEntity
                .get(uri)
                .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                .build();
        ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(responseEntity.getBody()).isBlank();
    }

    @Nested
    class SecuredMethodsTests {

        @BeforeEach
        void setUp() {
            checkJwtExists();
        }

        @Test
        void deleteUser_developerHasAccess() {

            //given
            UUID userId = UUID.randomUUID();

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .delete("/users/regular/{id}", userId)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                    .build();
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isEqualTo("Deleted user with id: " + userId);
        }

        @Test
        void deleteUser_unauthorized() {

            //given
            UUID userId = UUID.randomUUID();

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .delete("/users/regular/{id}", userId)
                    .build();
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(responseEntity.getBody()).isBlank();
        }

        @Test
        void deleteUser_developerHasNoAccess() {

            //given
            UUID userId = UUID.randomUUID();

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .delete("/users/super/{id}", userId)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                    .build();
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(responseEntity.getBody()).isBlank();
        }
    }

    @Nested
    class PreAuthorizeTests {

        @BeforeEach
        void setUp() {
            checkJwtExists();
        }

        @Test
        void updateUser_developerHasAccess() {

            //given
            String name = "any.name";

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .put("/users/regular/{name}", name)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                    .build();
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isEqualTo("Updated user with name: " + name);
        }

        @Test
        void updateSuperUser_developerHasNoAccess() {

            //given
            String name = "any.name";

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .put("/users/super/{name}", name)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                    .build();
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(responseEntity.getBody()).isBlank();
        }

        @Test
        void updateSuperUser_ownerHasAccess() {

            //given
            String name = "shyshkin.art";

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .put("/users/super/{name}", name)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                    .build();
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isEqualTo("Updated super user with name: " + name);
        }

        @Test
        void updateSuperUserById_ownerHasAccess() {

            //given
            String userId = "624ba8cd-b02f-4405-b6e7-6855a4bb3452"; //from `realm-export.json`
            String expectedResponseBody = String.format("Updated super user with id: `%s` and JWT subject: `%s`", userId, userId);

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .put("/users/byId/super/{id}", userId)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                    .build();
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isEqualTo(expectedResponseBody);
        }

        @Test
        void updateSuperUserById_usingPrincipal_ownerHasAccess() {

            //given
            String userId = "624ba8cd-b02f-4405-b6e7-6855a4bb3452"; //from `realm-export.json`
            String expectedResponseBody = String.format("Updated super user with id: `%s` and same JWT subject", userId);

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .put("/users/byId_principal/super/{id}", userId)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                    .build();
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody()).isEqualTo(expectedResponseBody);
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "/users/byId/super/{id}",
                "/users/byId_principal/super/{id}"
        })
        void updateSuperUserById_noAdmin_and_noOwner_have_NO_Access(String uri) {

            //given
            String userId = UUID.randomUUID().toString();

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .put(uri, userId)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                    .build();
            ResponseEntity<String> responseEntity = testRestTemplate.exchange(requestEntity, String.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(responseEntity.getBody()).isBlank();
        }
    }

    @Nested
    class PostAuthorizeTests {

        @BeforeEach
        void setUp() {
            checkJwtExists();
        }

        @Test
        void getSuperUserById_ownerHasAccess() {

            //given
            String userId = "624ba8cd-b02f-4405-b6e7-6855a4bb3452"; //from `realm-export.json`

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .get("/users/byId/super/{id}", userId)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                    .build();
            var responseEntity = testRestTemplate.exchange(requestEntity, UserDto.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(responseEntity.getBody())
                    .isNotNull()
                    .hasNoNullFieldsOrProperties()
                    .hasFieldOrPropertyWithValue("id", userId)
                    .hasFieldOrPropertyWithValue("firstName", "Mike")
                    .hasFieldOrPropertyWithValue("lastName", "Wazowski")
            ;
        }

        @Test
        void getSuperUserById_notOwner_and_notAdmin_HasNoAccess() {

            //given
            String userId = UUID.randomUUID().toString();

            //when
            RequestEntity<?> requestEntity = RequestEntity
                    .get("/users/byId/super/{id}", userId)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(getAccessToken()))
                    .build();
            var responseEntity = testRestTemplate.exchange(requestEntity, UserDto.class);

            //then
            log.debug("Response entity: {}", responseEntity);
            assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(responseEntity.getBody()).isNull();
        }
    }

    private void checkJwtExists() {
        if (oAuthResponse == null) {
            String code = getAuthorizationCode("openid profile");
            log.debug("Code from keycloak: {}", code);
            requestNewAccessToken(code);
        }
        log.debug("Jwt Access Token: {}", oAuthResponse.getAccessToken());
    }

    private String getAccessToken() {
        checkJwtExists();
        return oAuthResponse.getAccessToken();
    }

    private String requestNewAccessToken(String code) {
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
        oAuthResponse = responseEntity.getBody();
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

            signIn(currentUsername, currentPassword);
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
        WebElement usernameField = driver.findElementById("username");
        usernameField.sendKeys(username);

        //id = "password"
        WebElement passwordField = driver.findElementById("password");
        passwordField.sendKeys(password);
        passwordField.submit();
    }

    private void logout() {
        logout(false);
    }

    private void logout(boolean eraseJwtToken) {

        String postLogoutRedirectUri = "http://keycloak:8080/auth";
        String token = oAuthResponse.getIdToken();

        String url = String.format("http://keycloak:8080/auth/realms/katarinazart/protocol/openid-connect/logout?id_token_hint=%s&post_logout_redirect_uri=%s",
                token, postLogoutRedirectUri);

        if (driver == null)
            driver = browser.getWebDriver();
        driver.get(url);
        await()
                .timeout(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertAll(
                        () -> assertThat(driver.getCurrentUrl())
//                                .satisfies(currentUrl -> log.debug("CurrentUrl: {}", currentUrl))
                                .startsWith(postLogoutRedirectUri),
                        () -> assertThat(driver.getPageSource())
//                                .satisfies(pageContent -> log.debug("Page Content: \n{}", pageContent))
                                .contains("<html>")
                ));

        if (eraseJwtToken) {
            oAuthResponse = null;
        }
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
