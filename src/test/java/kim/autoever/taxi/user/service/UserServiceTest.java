package kim.autoever.taxi.user.service;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.common.exception.NotFoundException;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.domain.UserRole;
import kim.autoever.taxi.user.dto.UserCreateRequest;
import kim.autoever.taxi.user.dto.UserResponse;
import kim.autoever.taxi.user.dto.UserUpdateRequest;
import kim.autoever.taxi.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void 사용자를_생성한다() {
        UserCreateRequest request = new UserCreateRequest("홍길동", "01012345678", UserRole.PASSENGER);
        when(userRepository.existsByPhone("01012345678")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse response = userService.create(request);

        assertThat(response.name()).isEqualTo("홍길동");
        assertThat(response.phone()).isEqualTo("01012345678");
        assertThat(response.role()).isEqualTo(UserRole.PASSENGER);
    }

    @Test
    void 이미_등록된_전화번호로_생성하면_409_예외가_발생한다() {
        UserCreateRequest request = new UserCreateRequest("홍길동", "01012345678", UserRole.DRIVER);
        when(userRepository.existsByPhone("01012345678")).thenReturn(true);

        assertThatThrownBy(() -> userService.create(request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
        verify(userRepository, never()).save(any());
    }

    @Test
    void 사용자를_조회한다() {
        User user = User.create("홍길동", "01012345678", UserRole.PASSENGER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UserResponse response = userService.get(1L);

        assertThat(response.name()).isEqualTo("홍길동");
    }

    @Test
    void 존재하지_않는_사용자를_조회하면_404_예외가_발생한다() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.get(99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void 사용자_정보를_수정한다() {
        User user = User.create("홍길동", "01012345678", UserRole.PASSENGER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("01099998888", 1L)).thenReturn(false);

        UserResponse response = userService.update(1L, new UserUpdateRequest("김철수", "01099998888"));

        assertThat(response.name()).isEqualTo("김철수");
        assertThat(response.phone()).isEqualTo("01099998888");
        assertThat(response.role()).isEqualTo(UserRole.PASSENGER);
    }

    @Test
    void 다른_사용자의_전화번호로_수정하면_409_예외가_발생한다() {
        User user = User.create("홍길동", "01012345678", UserRole.PASSENGER);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByPhoneAndIdNot("01099998888", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.update(1L, new UserUpdateRequest("홍길동", "01099998888")))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONFLICT);
    }

    @Test
    void 존재하지_않는_사용자를_수정하면_404_예외가_발생한다() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.update(99L, new UserUpdateRequest("홍길동", "01012345678")))
                .isInstanceOf(NotFoundException.class);
    }
}