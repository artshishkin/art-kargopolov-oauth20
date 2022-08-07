package net.shyshkin.study.oauth.spring.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Set;

@Data
@Configuration
@ConfigurationProperties("app.auth-server")
public class AuthServerConfigData {

    private Provider provider;
    private List<Client> clients;

    @Data
    public static class Client {
        private String clientId;
        private String clientSecret;
        private Set<String> redirectUris;
        private Set<String> scopes;
    }

    @Data
    public static class Provider {
        private String issuer;
    }

}
