package kim.autoever.taxi.user.auth;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.user.domain.UserRole;

public record LoginUser(Long id, UserRole role) {

    public void requireRole(UserRole required) {
        if (role != required) {
            throw new BusinessException(ErrorCode.FORBIDDEN, required + " 권한이 필요합니다.");
        }
    }
}