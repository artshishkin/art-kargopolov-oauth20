package net.shyshkin.study.oauth.legacy.service;

import lombok.RequiredArgsConstructor;
import net.shyshkin.study.oauth.legacy.data.UsersRepository;
import net.shyshkin.study.oauth.legacy.mappers.UserMapper;
import net.shyshkin.study.oauth.legacy.response.UserRest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public UserRest getUserDetails(String userName) {
        return usersRepository
                .findByEmail(userName)
                .map(userMapper::toUserRest)
                .orElse(new UserRest());
    }

    @Override
    public UserRest getUserDetails(String userName, String password) {
        return usersRepository
                .findByEmail(userName)
                .filter(entity -> passwordEncoder.matches(password, entity.getEncryptedPassword()))
                .map(userMapper::toUserRest)
                .orElse(null);
    }
}
