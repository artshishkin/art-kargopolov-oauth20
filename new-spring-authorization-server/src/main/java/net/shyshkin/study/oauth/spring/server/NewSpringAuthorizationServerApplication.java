package net.shyshkin.study.oauth.spring.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class NewSpringAuthorizationServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(NewSpringAuthorizationServerApplication.class, args);
    }

}
