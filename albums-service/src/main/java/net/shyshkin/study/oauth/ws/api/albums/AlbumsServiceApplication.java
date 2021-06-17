package net.shyshkin.study.oauth.ws.api.albums;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class AlbumsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlbumsServiceApplication.class, args);
    }

}
