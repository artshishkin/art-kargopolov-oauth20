package net.shyshkin.study.oauth.ws.clients.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.clients.dto.AlbumDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RestTemplateAlbumsService implements AlbumsService {

    private final OAuth2AuthorizedClientService oauth2ClientService;
    private final RestTemplateBuilder restTemplateBuilder;

    private RestTemplate restTemplate;
    private static final ParameterizedTypeReference<List<AlbumDto>> ALBUMS_DTO_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    @Value("${app.gateway.uri}")
    private String gatewayUri;

    @PostConstruct
    void init() {
        restTemplate = restTemplateBuilder
                .rootUri(gatewayUri + "/albums")
                .build();
    }

    @Override
    public List<AlbumDto> getAllAlbums() {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;

        OAuth2AuthorizedClient oauth2Client = oauth2ClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                oauthToken.getName()
        );

        String jwtAccessToken = oauth2Client.getAccessToken().getTokenValue();
        log.debug("JWT Access Token: {}", jwtAccessToken);

        var requestEntity = RequestEntity
                .get("/")
                .headers(headers -> headers.setBearerAuth(jwtAccessToken))
                .build();

        ResponseEntity<List<AlbumDto>> responseEntity = restTemplate.exchange(requestEntity, ALBUMS_DTO_LIST_TYPE);

        return responseEntity.getStatusCode() == HttpStatus.OK ? responseEntity.getBody() : Collections.emptyList();
    }
}
