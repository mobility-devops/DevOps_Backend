package kim.autoever.taxi.user.service;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.common.exception.NotFoundException;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.dto.UserCreateRequest;
import kim.autoever.taxi.user.dto.UserResponse;
import kim.autoever.taxi.user.dto.UserUpdateRequest;
import kim.autoever.taxi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByPhone(request.phone())) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 등록된 전화번호입니다.");
        }
        User user = userRepository.save(User.create(request.name(), request.phone(), request.role()));
        return UserResponse.from(user);
    }

    public UserResponse get(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse update(Long userId, UserUpdateRequest request) {
        User user = findUser(userId);
        if (userRepository.existsByPhoneAndIdNot(request.phone(), userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "이미 등록된 전화번호입니다.");
        }
        user.update(request.name(), request.phone());
        userRepository.flush();
        return UserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
    }
}