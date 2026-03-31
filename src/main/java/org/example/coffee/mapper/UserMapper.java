package org.example.coffee.mapper;

import org.example.coffee.dto.user.ChangeInfoUserRequest;
import org.example.coffee.dto.user.UserRequest;
import org.example.coffee.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper
public interface UserMapper {
    UserEntity getEntityFromInput(UserRequest userRequest);

    void updateEntityFromInput(@MappingTarget UserEntity userEntity, ChangeInfoUserRequest changeInfoUserRequest);
}
