package net.shyshkin.study.oauth.ws.clients.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.ws.clients.dto.AlbumDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class WebClientAlbumsService implements AlbumsService {

    private final WebClient webClient;

    private static final ParameterizedTypeReference<List<AlbumDto>> ALBUMS_DTO_LIST_TYPE = new ParameterizedTypeReference<>() {
    };

    @Value("${app.gateway.uri}")
    private String gatewayUri;

    @Override
    public List<AlbumDto> getAllAlbums() {

        log.debug("Getting All Albums method invoked");

        return webClient
                .get()
                .uri(gatewayUri + "/albums")
                .retrieve()
                .bodyToMono(ALBUMS_DTO_LIST_TYPE)
                .block();
    }
}
