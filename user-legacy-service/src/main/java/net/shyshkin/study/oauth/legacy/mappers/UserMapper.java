package net.shyshkin.study.oauth.legacy.mappers;

import net.shyshkin.study.oauth.legacy.data.UserEntity;
import net.shyshkin.study.oauth.legacy.response.UserRest;
import org.mapstruct.Mapper;

@Mapper
public interface UserMapper {

    UserRest toUserRest(UserEntity entity);

}
