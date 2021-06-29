package net.shyshkin.study.oauth.legacy.bootstrap;

import lombok.RequiredArgsConstructor;
import net.shyshkin.study.oauth.legacy.data.UserEntity;
import net.shyshkin.study.oauth.legacy.data.UsersRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class InitialSetup {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {

        List
                .of(
                        new UserEntity(
                                1L,
                                "qswe3mg84mfjtu",
                                "Art",
                                "Shyshkin",
                                "test2@test.com",
                                passwordEncoder.encode("art"),
                                "",
                                false,
                                "user"),
                        new UserEntity(
                                2L,
                                "blablabla2",
                                "Kate",
                                "Shyshkina",
                                "kate_developer@test.com",
                                passwordEncoder.encode("kate"),
                                "",
                                false,
                                "developer"),
                        new UserEntity(
                                3L,
                                "blablabla3",
                                "Arina",
                                "Shyshkina",
                                "arina_admin@test.com",
                                passwordEncoder.encode("arina"),
                                "",
                                false,
                                "admin"),
                        new UserEntity(
                                4L,
                                "blablabla4",
                                "Nazar",
                                "Shyshkin",
                                "nazar_admin_developer@test.com",
                                passwordEncoder.encode("nazar"),
                                "",
                                false,
                                "admin,developer"))
                .forEach(usersRepository::save);
    }
}
