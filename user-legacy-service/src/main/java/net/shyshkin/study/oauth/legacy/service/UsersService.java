package net.shyshkin.study.oauth.legacy.service;

import net.shyshkin.study.oauth.legacy.response.UserRest;

public interface UsersService {

    UserRest getUserDetails(String userName, String password);

    UserRest getUserDetails(String userName);
}
