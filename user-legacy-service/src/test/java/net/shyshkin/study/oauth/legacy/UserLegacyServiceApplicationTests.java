package net.shyshkin.study.oauth.legacy;

import lombok.extern.slf4j.Slf4j;
import net.shyshkin.study.oauth.legacy.response.UserRest;
import net.shyshkin.study.oauth.legacy.response.VerifyPasswordResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.OK;

@Slf4j
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserLegacyServiceApplicationTests {

    @Autowired
    TestRestTemplate testRestTemplate;

    @Test
    void getUserByEmail_present() {
        //given
        String email = "test2@test.com";

        //when
        ResponseEntity<UserRest> responseEntity = testRestTemplate.getForEntity("/users/{userName}", UserRest.class, email);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(OK);
        UserRest body = responseEntity.getBody();
        assertThat(body)
                .hasNoNullFieldsOrProperties()
                .hasFieldOrPropertyWithValue("userId", "qswe3mg84mfjtu")
                .hasFieldOrPropertyWithValue("firstName", "Art")
                .hasFieldOrPropertyWithValue("lastName", "Shyshkin")
                .hasFieldOrPropertyWithValue("userName", "Art Shyshkin")
                .hasFieldOrPropertyWithValue("email", "test2@test.com");
        assertThat(body.getRoles())
                .hasSize(1)
                .allSatisfy(role -> assertThat(role).isEqualTo("user"));
    }

    @Test
    void getUserByEmail_with2Roles() {
        //given
        String email = "nazar_admin_developer@test.com";

        //when
        ResponseEntity<UserRest> responseEntity = testRestTemplate.getForEntity("/users/{userName}", UserRest.class, email);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(OK);
        UserRest body = responseEntity.getBody();
        assertThat(body)
                .hasNoNullFieldsOrProperties()
                .hasFieldOrPropertyWithValue("userId", "blablabla4")
                .hasFieldOrPropertyWithValue("firstName", "Nazar")
                .hasFieldOrPropertyWithValue("lastName", "Shyshkin")
                .hasFieldOrPropertyWithValue("userName", "Nazar Shyshkin")
                .hasFieldOrPropertyWithValue("email", "nazar_admin_developer@test.com");
        assertThat(body.getRoles())
                .hasSize(2)
                .contains("admin","developer");
    }

    @Test
    void getUserByEmail_absent() {
        //given
        String email = "absent@test.com";

        //when
        ResponseEntity<UserRest> responseEntity = testRestTemplate.getForEntity("/users/{userName}", UserRest.class, email);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(OK);
        UserRest body = responseEntity.getBody();
        assertThat(body)
                .hasAllNullFieldsOrProperties();
    }

    @Test
    void verifyUserPassword_correct() {
        //given
        String email = "test2@test.com";
        String password = "art";

        //when
        ResponseEntity<VerifyPasswordResponse> responseEntity = testRestTemplate
                .postForEntity("/users/{userName}/verify-password",
                        password,
                        VerifyPasswordResponse.class,
                        email);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(OK);
        VerifyPasswordResponse body = responseEntity.getBody();
        assertThat(body)
                .hasNoNullFieldsOrProperties()
                .hasFieldOrPropertyWithValue("result", true);
    }

    @Test
    void verifyUserPassword_accountAbsent() {
        //given
        String email = "absent2@test.com";
        String password = "art";

        //when
        ResponseEntity<VerifyPasswordResponse> responseEntity = testRestTemplate
                .postForEntity("/users/{userName}/verify-password",
                        password,
                        VerifyPasswordResponse.class,
                        email);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(OK);
        VerifyPasswordResponse body = responseEntity.getBody();
        assertThat(body)
                .hasNoNullFieldsOrProperties()
                .hasFieldOrPropertyWithValue("result", false);
    }

    @Test
    void verifyUserPassword_wrongPassword() {
        //given
        String email = "test2@test.com";
        String password = "wrong_password";

        //when
        ResponseEntity<VerifyPasswordResponse> responseEntity = testRestTemplate
                .postForEntity("/users/{userName}/verify-password",
                        password,
                        VerifyPasswordResponse.class,
                        email);

        //then
        log.debug("Response entity: {}", responseEntity);
        assertThat(responseEntity.getStatusCode()).isEqualTo(OK);
        VerifyPasswordResponse body = responseEntity.getBody();
        assertThat(body)
                .hasNoNullFieldsOrProperties()
                .hasFieldOrPropertyWithValue("result", false);
    }

}
