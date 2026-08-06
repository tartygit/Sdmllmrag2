package com.cth.sdm.application.mapper;

import com.cth.sdm.application.dto.UserDto;
import com.cth.sdm.domain.model.Role;
import com.cth.sdm.domain.model.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "mapRoles")
    UserDto toDto(User user);

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "failedAttempts", ignore = true)
    @Mapping(target = "lockTime", ignore = true)
    @Mapping(target = "passwordUpdatedAt", ignore = true)
    @Mapping(target = "mfaSecret", ignore = true)
    @Mapping(target = "mfaEnabled", ignore = true)
    User toEntity(UserDto dto);

    @Named("mapRoles")
    default Set<String> mapRoles(Set<Role> roles) {
        if (roles == null) {
            return null;
        }
        return roles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());
    }
}
