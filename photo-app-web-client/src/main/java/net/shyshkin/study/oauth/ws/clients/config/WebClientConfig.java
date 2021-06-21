package net.shyshkin.study.oauth.ws.clients.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(ClientRegistrationRepository clientRegistrationRepository,
                               OAuth2AuthorizedClientRepository authorizedClientRepository) {

        var oauth2FilterFunction = new ServletOAuth2AuthorizedClientExchangeFilterFunction(clientRegistrationRepository, authorizedClientRepository);
        oauth2FilterFunction.setDefaultOAuth2AuthorizedClient(true);

        return WebClient.builder()
                .apply(oauth2FilterFunction.oauth2Configuration())
                .build();
    }

}
