package net.shyshkin.study.oauth.legacy.bootstrap;

import lombok.RequiredArgsConstructor;
import net.shyshkin.study.oauth.legacy.data.UserEntity;
import net.shyshkin.study.oauth.legacy.data.UsersRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
@RequiredArgsConstructor
public class InitialSetup {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;

    @EventListener
    @Transactional
    public void onApplicationEvent(ApplicationReadyEvent event) {

        UserEntity user = new UserEntity(
                1L,
                "qswe3mg84mfjtu",
                "Art",
                "Shyshkin",
                "test2@test.com",
                passwordEncoder.encode("art"),
                "",
                false);

        usersRepository.save(user);
    }
}
