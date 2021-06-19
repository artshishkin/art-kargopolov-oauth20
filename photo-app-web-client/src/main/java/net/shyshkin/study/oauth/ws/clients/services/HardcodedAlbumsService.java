package net.shyshkin.study.oauth.ws.clients.services;

import net.shyshkin.study.oauth.ws.clients.dto.AlbumDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class HardcodedAlbumsService implements AlbumsService {

    @Override
    public List<AlbumDto> getAllAlbums() {
        return IntStream
                .rangeClosed(1, 2)
                .mapToObj(this::fakeAlbum)
                .collect(Collectors.toList());
    }

    private AlbumDto fakeAlbum(int index) {
        return AlbumDto.builder()
                .id("WebAlbumId" + index)
                .title("WebAlbumTitle" + index)
                .description("WebAlbumDescription" + index)
                .url("http//localhost:8082/albums/" + index)
                .userId("webUserId" + index)
                .build();
    }
}
