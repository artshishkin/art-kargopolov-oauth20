package net.shyshkin.study.oauth.ws.api.albums.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumDto {
    private String id;
    private String title;
    private String description;
    private String url;
    private String userId;
}
