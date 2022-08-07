package net.shyshkin.study.oauth.spring.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Data
@Configuration
@ConfigurationProperties("app.auth-server")
public class AuthServerConfigData {

    private String providerIssuer;
    private String clientId;
    private String clientSecret;
    private Set<String> redirectUris;
    private Set<String> scopes;

}
