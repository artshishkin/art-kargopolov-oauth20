package net.shyshkin.study.oauth.legacy.mappers;

import net.shyshkin.study.oauth.legacy.data.UserEntity;
import net.shyshkin.study.oauth.legacy.response.UserRest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mapper
public interface UserMapper {

    @Mapping(target = "userName", expression = "java( entity.getFirstName() + \" \" + entity.getLastName() )")
    UserRest toUserRest(UserEntity entity);

    default List<String> mapRoles(String roles) {
        return Stream
                .of(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }
}
