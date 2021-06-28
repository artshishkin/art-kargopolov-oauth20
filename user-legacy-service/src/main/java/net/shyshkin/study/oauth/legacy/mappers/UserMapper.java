package net.shyshkin.study.oauth.legacy.mappers;

import net.shyshkin.study.oauth.legacy.data.UserEntity;
import net.shyshkin.study.oauth.legacy.response.UserRest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface UserMapper {

    @Mapping(target = "userName", expression = "java( entity.getFirstName() + \" \" + entity.getLastName() )")
    UserRest toUserRest(UserEntity entity);

}
