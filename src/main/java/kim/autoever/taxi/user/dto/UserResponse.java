package kim.autoever.taxi.user.dto;

import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.domain.UserRole;

import java.time.Instant;

public record UserResponse(
        Long id,
        String name,
        String phone,
        UserRole role,
        Instant createdAt,
        Instant updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}