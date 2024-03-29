package net.shyshkin.study.oauth.ws.api.gateway;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.test.common.AbstractKeycloakTest;
import net.shyshkin.study.oauth.ws.api.gateway.dto.OAuthResponse;
import org.assertj.core.data.Percentage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
        "logging.level.net.shyshkin=debug",
        "eureka.client.service-url.defaultZone=${DISCOVERY_SERVICE_URI}/eureka"
})
@ContextConfiguration(initializers = ApiGatewayLoadBalancingTests.Initializer.class)
@ActiveProfiles("local")
class ApiGatewayLoadBalancingTests extends AbstractKeycloakTest {

    @Container
    static GenericContainer<?> discoveryService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-discovery-service")
            .withNetwork(network)
            .withNetworkAliases("discovery-service")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> usersService1 = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-resource-server")
            .withNetwork(network)
            .withNetworkAliases("users-service")
            .withExposedPorts(8080)
            .withEnv("eureka.client.enabled", "true")
            .dependsOn(keycloak, discoveryService)
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> usersService2 = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-resource-server")
            .withNetwork(network)
            .withNetworkAliases("users-service")
            .withExposedPorts(8080)
            .withEnv("eureka.client.enabled", "true")
            .dependsOn(keycloak, discoveryService)
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> albumsService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-albums-service")
            .withNetwork(network)
            .withNetworkAliases("albums-service")
            .withExposedPorts(8080)
            .withEnv("eureka.client.enabled", "true")
            .dependsOn(keycloak, discoveryService, usersService1)  //fake dependency to start users-service before this service
            .waitingFor(Wait.forHealthcheck());

    @Container
    static GenericContainer<?> photosService = new GenericContainer<>("artarkatesoft/art-kargopolov-oauth20-photos-service")
            .withNetwork(network)
            .withNetworkAliases("photos-service")
            .withExposedPorts(8080)
            .withEnv("eureka.client.enabled", "true")
            .dependsOn(keycloak, discoveryService, usersService2)  //fake dependency to start users-service before this service
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
            .dependsOn(photosService, usersService1, usersService2, discoveryService, albumsService)
            .waitingFor(Wait.forHealthcheck());

    @Autowired
    ApplicationContext applicationContext;

    WebTestClient webTestClient;

    private WebClient keycloakWebClient;

    static String jwtAccessToken;

    private static WebTestClient discoveryServiceWebTestClient;

    @BeforeEach
    void setUp() {

        Integer keycloakPort = keycloak.getMappedPort(8080);
        String keycloakHost = keycloak.getHost();
        String keycloakUri = String.format("http://%s:%d", keycloakHost, keycloakPort);

        keycloakWebClient = WebClient
                .builder()
                .baseUrl(keycloakUri)
                .build();

        Integer gatewayPort = gatewayService.getMappedPort(8080);
        String gatewayHost = gatewayService.getHost();
        String gatewayUri = String.format("http://%s:%d", gatewayHost, gatewayPort);

        webTestClient = WebTestClient
                .bindToServer()
                .baseUrl(gatewayUri)
//                .defaultHeaders(headers -> headers.setBearerAuth(jwtAccessToken))
                .build();
    }

    @Nested
    class LoadBalancingTests {

        @BeforeEach
        void setUp() {
            //given
            checkJwtExists();
            awaitForServiceToRegisterWithEureka("USERS-SERVICE", 2);
        }

        @Test
        void loadBalancingBetween2UsersServices() throws InterruptedException {

            //given
//            Thread.sleep(120000);
            int TOTAL_INVOCATION_COUNT = 100;
            int HOSTS_COUNT = 2;
            Map<String, Integer> hostsInvocationCount = new HashMap<>();

            //when
            for (int i = 0; i < TOTAL_INVOCATION_COUNT; i++) {
                var exchangeResult = webTestClient
                        .get().uri("/users/role/developer/status/check")
                        .headers(httpHeaders -> httpHeaders.setBearerAuth(jwtAccessToken))
                        .exchange()

                        //then
                        .expectStatus().isOk()
                        .expectHeader().exists("SERVER_HOST")
                        .expectHeader().exists("SERVER_PORT")
                        .expectBody(String.class).isEqualTo("Working...").returnResult();

                List<String> serverHostList = exchangeResult.getResponseHeaders().get("SERVER_HOST");
                assertThat(serverHostList).hasSize(1);
                String serverHost = serverHostList.get(0);

                List<String> serverPortList = exchangeResult.getResponseHeaders().get("SERVER_PORT");
                assertThat(serverPortList).hasSize(1);
                String serverPort = serverPortList.get(0);

                String serverUri = String.format("%s:%s", serverHost, serverPort);

                hostsInvocationCount.merge(serverUri, 1, Integer::sum);
            }

            hostsInvocationCount.forEach(
                    (host, count) -> log.debug("host `{}` was invoked {} times", host, count)
            );

            int hostsSize = hostsInvocationCount.size();
            assertThat(hostsSize).isEqualTo(HOSTS_COUNT);

            Optional<Integer> totalCount = hostsInvocationCount.values().stream().reduce(Integer::sum);
            assertThat(totalCount).contains(TOTAL_INVOCATION_COUNT);

            hostsInvocationCount.forEach(
                    (host, count) -> assertThat(count).isCloseTo(TOTAL_INVOCATION_COUNT / HOSTS_COUNT, Percentage.withPercentage(0.9)));
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

    @Nested
    class AllServicesInDockerTests {

        @Nested
        class UsersServiceTests {

            @BeforeEach
            void setUp() {
                //given
                checkJwtExists();
                awaitForServiceToRegisterWithEureka("USERS-SERVICE", 2);
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
                awaitForServiceToRegisterWithEureka("ALBUMS-SERVICE", 1);
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
                awaitForServiceToRegisterWithEureka("PHOTOS-SERVICE", 1);
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
    }

    private void checkJwtExists() {
        if (jwtAccessToken == null) {
            jwtAccessToken = getAccessTokenUsingPasswordGrantType();
            log.debug("Jwt Access Token: {}", jwtAccessToken);
        }
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
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        OAuthResponse oAuthResponse = responseEntity.getBody();
        assertThat(oAuthResponse)
                .hasFieldOrProperty("accessToken");

        return oAuthResponse.getAccessToken();
    }

    private void awaitForServiceToRegisterWithEureka(String serviceName, int instanceCount) {

        //given
        checkDiscoveryServiceClientInitialized();
        int lastInstanceIndex = instanceCount - 1;

        //when
        await()
                .timeout(10, TimeUnit.SECONDS)
                .untilAsserted(() ->
                                discoveryServiceWebTestClient
                                        .get().uri("/eureka/apps/{app}", serviceName)
                                        .accept(MediaType.APPLICATION_JSON)
                                        .exchange()

                                        //then
                                        .expectStatus().isOk()
                                        .expectBody()
                                        .consumeWith(result -> {
                                            byte[] responseBodyBytes = result.getResponseBody();
                                            String responseString = new String(responseBodyBytes, StandardCharsets.UTF_8);
                                            log.debug("Getting {} characteristics from Eureka, response: {}", serviceName, responseString);
                                        })
                                        .jsonPath("$.application.name").isEqualTo(serviceName)
                                        .jsonPath("$.application.instance[%d].status", lastInstanceIndex).isEqualTo("UP")
//                                .jsonPath("$.application.instance.length()").isEqualTo(instanceCount) //does not work

                );
    }

    private void checkDiscoveryServiceClientInitialized() {

        if (discoveryServiceWebTestClient == null) {

            Integer discoveryPort = discoveryService.getMappedPort(8080);
            String discoveryHost = discoveryService.getHost();
            String discoveryUri = String.format("http://%s:%d", discoveryHost, discoveryPort);

            discoveryServiceWebTestClient = WebTestClient
                    .bindToServer()
                    .baseUrl(discoveryUri)
                    .build();
        }
    }


    static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext applicationContext) {
            log.debug("ResourceServerApplicationTests.Initializer.initialize()");
            System.setProperty("OAUTH_HOST", keycloak.getHost());
            System.setProperty("OAUTH_PORT", String.valueOf(keycloak.getMappedPort(8080)));

            setUriSystemPropertyForService(discoveryService);
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
