package kim.autoever.taxi.user.controller;

import kim.autoever.taxi.common.exception.GlobalExceptionHandler;
import kim.autoever.taxi.common.exception.RequestIdFilter;
import kim.autoever.taxi.user.auth.CurrentUserArgumentResolver;
import kim.autoever.taxi.user.domain.User;
import kim.autoever.taxi.user.domain.UserRole;
import kim.autoever.taxi.user.dto.UserResponse;
import kim.autoever.taxi.user.repository.UserRepository;
import kim.autoever.taxi.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new UserController(userService))
                .setCustomArgumentResolvers(new CurrentUserArgumentResolver(userRepository))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .build();
    }

    private UserResponse response(long id, String name, String phone, UserRole role) {
        return new UserResponse(id, name, phone, role, Instant.parse("2026-09-30T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z"));
    }

    private User userWithId(long id, UserRole role) {
        User user = User.create("홍길동", "01012345678", role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    @Test
    void 사용자를_생성하면_201과_Location을_반환한다() throws Exception {
        when(userService.create(any())).thenReturn(response(1L, "홍길동", "01012345678", UserRole.PASSENGER));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"홍길동\",\"phone\":\"01012345678\",\"role\":\"PASSENGER\"}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/users/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.role").value("PASSENGER"));
    }

    @Test
    void 잘못된_입력이면_400과_필드_오류를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"phone\":\"abc\",\"role\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors.length()").value(3));
    }

    @Test
    void 정의되지_않은_역할이면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"홍길동\",\"phone\":\"01012345678\",\"role\":\"ADMIN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 헤더_없이_내_정보를_조회하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 숫자가_아닌_헤더면_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me").header("X-User-Id", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @Test
    void 존재하지_않는_사용자_헤더면_404를_반환한다() throws Exception {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/me").header("X-User-Id", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 내_정보를_조회한다() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithId(1L, UserRole.DRIVER)));
        when(userService.get(1L)).thenReturn(response(1L, "홍길동", "01012345678", UserRole.DRIVER));

        mockMvc.perform(get("/api/v1/me").header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.role").value("DRIVER"));
    }

    @Test
    void 내_정보를_수정한다() throws Exception {
        when(userRepository.findById(1L)).thenReturn(Optional.of(userWithId(1L, UserRole.PASSENGER)));
        when(userService.update(eq(1L), any())).thenReturn(response(1L, "김철수", "01099998888", UserRole.PASSENGER));

        mockMvc.perform(put("/api/v1/me")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"김철수\",\"phone\":\"01099998888\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("김철수"));
    }
}