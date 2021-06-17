package net.shyshkin.study.oauth.ws.api.photos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhotoDto {
    private String id;
    private String title;
    private String description;
    private String url;
    private String userId;
    private String albumId;
}
