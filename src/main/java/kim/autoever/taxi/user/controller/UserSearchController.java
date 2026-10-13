package kim.autoever.taxi.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserSearchController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/api/v1/users/search")
    public List<Map<String, Object>> search(
            @RequestParam("name") String name) {

        String sql = "SELECT id, name FROM users WHERE name = ?";
	return jdbcTemplate.queryForList(sql, name);
    }
}
