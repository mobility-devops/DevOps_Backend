package kim.autoever.taxi.user.auth;

import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import kim.autoever.taxi.common.exception.NotFoundException;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    public static final String USER_ID_HEADER = "X-User-Id";

    private final UserRepository userRepository;

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && LoginUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        String header = webRequest.getHeader(USER_ID_HEADER);
        if (header == null || header.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, USER_ID_HEADER + " 헤더가 필요합니다.");
        }

        long userId;
        try {
            userId = Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, USER_ID_HEADER + " 헤더는 숫자여야 합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다."));
        return new LoginUser(user.getId(), user.getRole());
    }
}