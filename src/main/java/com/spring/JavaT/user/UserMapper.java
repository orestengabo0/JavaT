package com.spring.JavaT.user;

import com.spring.JavaT.user.dto.UserDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

/**
 * MapStruct mapper for {@link User} ↔ {@link UserDto} conversions.
 *
 * <p>{@code componentModel = "spring"} makes MapStruct generate a Spring
 * {@code @Component} so the mapper can be injected anywhere with {@code @Autowired}
 * or {@code @RequiredArgsConstructor}.
 *
 * <p>The only non-trivial mapping is {@code username}: the entity exposes
 * {@link User#getDisplayUsername()} (the human-readable username field) while
 * {@link User#getUsername()} returns the email for Spring Security. We map
 * explicitly to avoid picking up the wrong getter.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserMapper {

    /**
     * Maps a {@link User} entity to a {@link UserDto}.
     *
     * <p>{@code username} is sourced from {@code displayUsername} (the actual
     * username field) rather than {@code username} (which returns the email
     * via the {@link org.springframework.security.core.userdetails.UserDetails} contract).
     */
    @Mapping(source = "displayUsername", target = "username")
    UserDto toDto(User user);
}
