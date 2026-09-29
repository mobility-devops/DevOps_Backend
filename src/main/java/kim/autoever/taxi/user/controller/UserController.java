package kim.autoever.taxi.user.controller;

import jakarta.validation.Valid;
import kim.autoever.taxi.user.auth.CurrentUser;
import kim.autoever.taxi.user.auth.LoginUser;
import kim.autoever.taxi.user.dto.UserCreateRequest;
import kim.autoever.taxi.user.dto.UserResponse;
import kim.autoever.taxi.user.dto.UserUpdateRequest;
import kim.autoever.taxi.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/users")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/users/" + response.id())).body(response);
    }

    @GetMapping("/me")
    public UserResponse getMe(@CurrentUser LoginUser loginUser) {
        return userService.get(loginUser.id());
    }

    @PutMapping("/me")
    public UserResponse updateMe(@CurrentUser LoginUser loginUser,
                                 @Valid @RequestBody UserUpdateRequest request) {
        return userService.update(loginUser.id(), request);
    }
}
