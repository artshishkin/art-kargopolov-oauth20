package net.shyshkin.study.oauth.ws.api.gateway;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.api.gateway.dto.OAuthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "app.routes.uri.users-api=${USERS_SERVICE_URI}",
        "app.routes.uri.albums-api=${ALBUMS_SERVICE_URI}",
        "app.routes.uri.photos-api=${PHOTOS_SERVICE_URI}",
})
@ContextConfiguration(initializers = ApiGatewayApplicationTests.Initializer.class)
class ApiGatewayApplicationTests {

    public static final String RESOURCE_OWNER_USERNAME = "shyshkin.art";
    public static final String RESOURCE_OWNER_PASSWORD = "password_art_1";

    static Network network = Network.newNetwork();

    @Container
    static PostgreSQLContainer<?> postgreSQL = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("keycloak")
            .withUsername("keycloak")
            .withPassword("password")
            .withNetwork(network)
            .withNetworkAliases("postgres");

    @Container
    static GenericContainer<?> keycloak = new GenericContainer<>("quay.io/keycloak/keycloak:latest")
            .withNetwork(network)
            .withNetworkAliases("keycloak")
            .withEnv(Map.of(
                    "DB_VENDOR", "POSTGRES",
                    "DB_ADDR", "postgres",
                    "DB_DATABASE", "keycloak",
                    "DB_USER", "keycloak",
                    "DB_SCHEMA", "public",
                    "DB_PASSWORD", "password",
                    "KEYCLOAK_USER", "admin",
                    "KEYCLOAK_PASSWORD", "Pa55w0rd"
            ))
            .withCommand(
                    "-b 0.0.0.0",
                    "-Dkeycloak.migration.action=import",
                    "-Dkeycloak.migration.provider=singleFile",
                    "-Dkeycloak.migration.file=/tmp/export/realm-export.json",
                    "-Dkeycloak.migration.strategy=IGNORE_EXISTING"
            )
            .withFileSystemBind("C:\\Users\\Admin\\IdeaProjects\\Study\\SergeyKargopolov\\OAuth20\\art-kargopolov-oauth20\\docker-compose\\keycloak-postgres\\export\\realm-export.json", "/tmp/export/realm-export.json")
            .withExposedPorts(8080)
            .dependsOn(postgreSQL)
            .waitingFor(Wait.forLogMessage(".*Admin console listening on.*\\n", 1));

    @Container
    static BrowserWebDriverContainer<?> browser = new BrowserWebDriverContainer<>()
            .withCapabilities(new FirefoxOptions())
            .withNetwork(network)
            .withNetworkAliases("browser")
            .dependsOn(keycloak);

    @Container
    static GenericContainer<?> usersService = new GenericContainer<>("artarkatesoft/oauth20-resource-server")
            .withNetwork(network)
            .withNetworkAliases("users-service")
            .withExposedPorts(8080)
            .dependsOn(keycloak)
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> albumsService = new GenericContainer<>("artarkatesoft/oauth20-albums-service")
            .withNetwork(network)
            .withNetworkAliases("albums-service")
            .withExposedPorts(8080)
            .dependsOn(keycloak)
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> photosService = new GenericContainer<>("artarkatesoft/oauth20-photos-service")
            .withNetwork(network)
            .withNetworkAliases("photos-service")
            .withExposedPorts(8080)
            .dependsOn(keycloak)
            .waitingFor(Wait.forHealthcheck());

    RemoteWebDriver driver;

    @Autowired
    ApplicationContext applicationContext;

    WebTestClient webTestClient;

    private WebClient keycloakWebClient;

    static String jwtAccessToken;

    @BeforeEach
    void setUp() {

        Integer keycloakPort = keycloak.getMappedPort(8080);
        String keycloakHost = keycloak.getHost();
        String keycloakUri = String.format("http://%s:%d", keycloakHost, keycloakPort);

        keycloakWebClient = WebClient
                .builder()
                .baseUrl(keycloakUri)
                .build();

        webTestClient = WebTestClient
                .bindToApplicationContext(applicationContext)
                .configureClient()
//                .defaultHeaders(headers -> headers.setBearerAuth(jwtAccessToken))
                .build();
    }

    @Nested
    class UsersServiceTests {

        @BeforeEach
        void setUp() {
            //given
            checkJwtExists();
        }

        @Test
        void existingEndpointForUnauthorizedUser_shouldBe401_UNAUTHORIZED() {

            //when
            webTestClient.get().uri("/users/status/check")
                    .exchange()

                    //then
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .isEmpty();
        }

        @Test
        void existingEndpointForAnyAuthorizedUser() {

            //when
            webTestClient.get().uri("/users/status/check")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                    .exchange()

                    //then
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .isEqualTo("Working...");
        }

        @Test
        void nonExistingEndpoint_should404NotFound() {

            //when
            webTestClient.get().uri("/users/status/checkNotFound")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                    .exchange()

                    //then
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.timestamp").exists()
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.error").isEqualTo("Not Found")
                    .jsonPath("$.path").isEqualTo("/users/status/checkNotFound");
        }

        @Test
        void updateSuperUser_developerHasNoAccess() {

            //given
            String name = "any.name";

            //when
            webTestClient.put().uri("/users/super/{name}", name)
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                    .exchange()

                    //then
                    .expectStatus().isForbidden()
                    .expectBody()
                    .isEmpty();
        }
    }

    @Nested
    class AlbumsServiceTests {

        @BeforeEach
        void setUp() {
            //given
            checkJwtExists();
        }

        @Test
        void existingEndpointForUnauthorizedUser_shouldBe401_UNAUTHORIZED() {

            //when
            webTestClient.get().uri("/albums")
                    .exchange()

                    //then
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .isEmpty();
        }

        @Test
        void existingEndpointForAnyAuthorizedUser() {

            //when
            webTestClient.get().uri("/albums")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                    .exchange()

                    //then
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$").isArray()
                    .jsonPath("$.[0]").exists()
                    .jsonPath("$.[0].id").isNotEmpty()
                    .jsonPath("$.[0].title").isNotEmpty()
                    .jsonPath("$.[0].description").isNotEmpty()
                    .jsonPath("$.[0].url").isNotEmpty()
                    .jsonPath("$.[0].userId").isNotEmpty()
                    .jsonPath("$.[1]").exists()
                    .jsonPath("$.[2]").doesNotExist()
            ;
        }

        @Test
        void nonExistingEndpoint_should404NotFound() {

            //when
            webTestClient.get().uri("/albumzzz")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                    .exchange()

                    //then
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.timestamp").exists()
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.error").isEqualTo("Not Found")
                    .jsonPath("$.path").isEqualTo("/albumzzz");
        }

        @Test
        void getSuperSecretEndpoint_developerHasNoAccess() {

            //when
            webTestClient.get().uri("/albums/super-secret")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                    .exchange()

                    //then
                    .expectStatus().isForbidden()
                    .expectBody()
                    .isEmpty();
        }
    }

    @Nested
    class PhotosServiceTests {

        @BeforeEach
        void setUp() {
            //given
            checkJwtExists();
        }

        @Test
        void existingEndpointForUnauthorizedUser_shouldBe401_UNAUTHORIZED() {

            //when
            webTestClient.get().uri("/photos")
                    .exchange()

                    //then
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .isEmpty();
        }

        @Test
        void existingEndpointForAnyAuthorizedUser() {

            //when
            webTestClient.get().uri("/photos")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                    .exchange()

                    //then
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$").isArray()
                    .jsonPath("$.[0]").exists()
                    .jsonPath("$.[0].id").isNotEmpty()
                    .jsonPath("$.[0].title").isNotEmpty()
                    .jsonPath("$.[0].description").isNotEmpty()
                    .jsonPath("$.[0].url").isNotEmpty()
                    .jsonPath("$.[0].userId").isNotEmpty()
                    .jsonPath("$.[0].albumId").isNotEmpty()
                    .jsonPath("$.[1]").exists()
                    .jsonPath("$.[2]").doesNotExist()
            ;
        }

        @Test
        void nonExistingEndpoint_should404NotFound() {

            //when
            webTestClient.get().uri("/photozzz")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                    .exchange()

                    //then
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.timestamp").exists()
                    .jsonPath("$.status").isEqualTo(404)
                    .jsonPath("$.error").isEqualTo("Not Found")
                    .jsonPath("$.path").isEqualTo("/photozzz");
        }

        @Test
        void getSuperSecretEndpoint_developerHasNoAccess() {

            //when
            webTestClient.get().uri("/photos/super-secret")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                    .exchange()

                    //then
                    .expectStatus().isForbidden()
                    .expectBody()
                    .isEmpty();
        }
    }

    @Nested
    class AccessTokenUsingPasswordGrantTypeTests {

        @Test
        void getJwtTokenTest() {

            //when
            String accessToken = getAccessTokenUsingPasswordGrantType();

            //then
            assertThat(accessToken).isNotBlank();
        }

        @Test
        void checkTokenIsCorrect() {

            //given
            String accessToken = getAccessTokenUsingPasswordGrantType();
            assertThat(accessToken).isNotBlank();

            //when
            webTestClient.get().uri("/users/status/check")
                    .headers(httpHeaders -> httpHeaders.setBearerAuth(accessToken))
                    .exchange()

                    //then
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .isEqualTo("Working...");
        }
    }

    private void checkJwtExists() {
        if (jwtAccessToken == null) {
            jwtAccessToken = getAccessTokenUsingAuthorizationCodeGrantType();
            log.debug("Jwt Access Token: {}", jwtAccessToken);
        }
    }

    private String getAccessTokenUsingAuthorizationCodeGrantType() {

        driver = browser.getWebDriver();

        String code = getAuthorizationCode("openid profile");
        log.debug("Code from keycloak: {}", code);
        return getAccessToken(code);
    }

    private String getAccessToken(String code) {
        //when
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "authorization_code");
        map.add("client_id", "photo-app-code-flow-client");
        map.add("client_secret", "ee68a49e-5ac6-4673-9465-51e53de3fb0e");
        map.add("code", code);
        map.add("redirect_uri", "http://localhost:8083/callback");

        ResponseEntity<OAuthResponse> responseEntity = keycloakWebClient
                .post()
                .uri("/auth/realms/katarinazart/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .toEntity(OAuthResponse.class)
                .doOnNext(entity -> log.debug("Response from OAuth2.0 server: {}", entity))
                .block();

        //then
        log.debug("Response from OAuth2.0 server: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        OAuthResponse oAuthResponse = responseEntity.getBody();
        assertThat(oAuthResponse)
                .hasFieldOrProperty("accessToken");

        return oAuthResponse.getAccessToken();
    }

    private String getAccessTokenUsingPasswordGrantType() {
        //when
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "password");
        map.add("username", RESOURCE_OWNER_USERNAME);
        map.add("password", RESOURCE_OWNER_PASSWORD);
        map.add("client_id", "photo-app-code-flow-client");
        map.add("client_secret", "ee68a49e-5ac6-4673-9465-51e53de3fb0e");
        map.add("scope", "openid profile");

        ResponseEntity<OAuthResponse> responseEntity = keycloakWebClient
                .post()
                .uri("/auth/realms/katarinazart/protocol/openid-connect/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(map))
                .retrieve()
                .toEntity(OAuthResponse.class)
                .doOnNext(entity -> log.debug("Response from OAuth2.0 server: {}", entity))
                .block();

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
        WebElement usernameField = driver.findElementById("username");
        usernameField.sendKeys(username);

        //id = "password"
        WebElement passwordField = driver.findElementById("password");
        passwordField.sendKeys(password);
        passwordField.submit();
    }

    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            log.debug("ResourceServerApplicationTests.Initializer.initialize()");
            System.setProperty("OAUTH_HOST", keycloak.getHost());
            System.setProperty("OAUTH_PORT", String.valueOf(keycloak.getMappedPort(8080)));

            setUriSystemPropertyForService(usersService);
            setUriSystemPropertyForService(albumsService);
            setUriSystemPropertyForService(photosService);
        }

        private void setUriSystemPropertyForService(GenericContainer<?> service) {
            String serviceName = service.getNetworkAliases()
                    .stream()
                    .filter(alias -> alias.contains("service"))
                    .findAny().get();
            String propertyName = serviceName.replace("-", "_").toUpperCase().concat("_URI");
            String serviceUri = String.format("http://%s:%d", service.getHost(), service.getMappedPort(8080));
            log.debug("Property `{}` set to `{}`", propertyName, serviceUri);
            System.setProperty(propertyName, serviceUri);
        }
    }
}