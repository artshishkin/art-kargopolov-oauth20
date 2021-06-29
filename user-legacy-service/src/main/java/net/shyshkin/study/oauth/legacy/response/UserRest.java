package net.shyshkin.study.oauth.legacy.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRest {
    private String firstName;
    private String lastName;
    private String email;
    private String userName;
    private String userId;
    private List<String> roles;
}
