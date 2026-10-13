package kim.autoever.taxi.user.controller;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserSearchControllerTest {

    @Test
    void searchPassesInputAsSqlParameter() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        UserSearchController controller =
                new UserSearchController(jdbcTemplate);

        // SQL 구문처럼 보이는 입력도 파라미터로 전달되는지 확인
        String name = "' OR '1'='1";
        String sql = "SELECT id, name FROM users WHERE name = ?";
        List<Map<String, Object>> expected = List.of(
                Map.<String, Object>of("id", 1L, "name", name)
        );

        when(jdbcTemplate.queryForList(sql, name))
                .thenReturn(expected);

        List<Map<String, Object>> actual = controller.search(name);

        assertEquals(expected, actual);
        verify(jdbcTemplate).queryForList(sql, name);
    }
}
