package net.shyshkin.study.oauth.clients.sociallogin.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ClientRegistrations;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;

@Configuration
@Profile("keycloak")
public class ClientRegistrationConfig {

    private ClientRegistration clientRegistration() {
        ClientRegistration clientRegistration = ClientRegistrations
                .fromOidcIssuerLocation("http://localhost:8080/auth/realms/katarinazart")
                .registrationId("photo-app-webclient")
                .clientId("photo-app-webclient")
                .clientSecret("74ef0a61-f3b5-427c-a450-09a5d1b6f192")
                .scope("openid", "profile", "roles")
                .build();
        return clientRegistration;
    }

    @Bean
    ClientRegistrationRepository clientRegistrationRepository() {
        return new InMemoryClientRegistrationRepository(clientRegistration());
    }


//    private ClientRegistration.Builder clientRegistration() {
//        Map<String, Object> metadata = new HashMap<>();
//        metadata.put("end_session_endpoint", "https://jhipster.org/logout");
//
//        return ClientRegistration.withRegistrationId("oidc")
//                .redirectUriTemplate("{baseUrl}/{action}/oauth2/code/{registrationId}")
//                .clientAuthenticationMethod(ClientAuthenticationMethod.BASIC)
//                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
//                .scope("read:user")
//                .authorizationUri("https://jhipster.org/login/oauth/authorize")
//                .tokenUri("https://jhipster.org/login/oauth/access_token")
//                .jwkSetUri("https://jhipster.org/oauth/jwk")
//                .userInfoUri("https://api.jhipster.org/user")
//                .providerConfigurationMetadata(metadata)
//                .userNameAttributeName("id")
//                .clientName("Client Name")
//                .clientId("client-id")
//                .clientSecret("client-secret");
//    }
}
