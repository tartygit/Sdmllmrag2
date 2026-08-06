package com.cth.sdm.application.mapper;

import com.cth.sdm.application.dto.UserDto;
import com.cth.sdm.domain.model.Role;
import com.cth.sdm.domain.model.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void shouldMapUserToUserDto() {
        Role roleAdmin = Role.builder().id(1L).name("ROLE_ADMIN").build();
        User user = User.builder()
                .id(100L)
                .username("testuser")
                .email("test@cth.com")
                .fullName("Test User")
                .enabled(true)
                .roles(Set.of(roleAdmin))
                .build();

        UserDto dto = userMapper.toDto(user);

        assertNotNull(dto);
        assertEquals(100L, dto.getId());
        assertEquals("testuser", dto.getUsername());
        assertEquals("test@cth.com", dto.getEmail());
        assertEquals("Test User", dto.getFullName());
        assertTrue(dto.isEnabled());
        assertTrue(dto.getRoles().contains("ROLE_ADMIN"));
    }
}
