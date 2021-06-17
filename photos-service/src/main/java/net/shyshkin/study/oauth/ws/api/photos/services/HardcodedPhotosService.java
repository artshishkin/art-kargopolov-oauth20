package net.shyshkin.study.oauth.ws.api.photos.services;

import net.shyshkin.study.oauth.ws.api.photos.dto.PhotoDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class HardcodedPhotosService implements PhotosService {

    @Override
    public List<PhotoDto> getAllPhotos() {
        return IntStream
                .rangeClosed(1, 2)
                .mapToObj(this::fakePhoto)
                .collect(Collectors.toList());
    }

    private PhotoDto fakePhoto(int index) {
        return PhotoDto.builder()
                .id("PhotoId" + index)
                .title("PhotoTitle" + index)
                .description("PhotoDescription" + index)
                .url("PhotoUrl" + index)
                .userId("userId" + index)
                .albumId("albumId" + index)
                .build();
    }
}
