package kim.autoever.taxi;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;

/**
 * 테스트용 MySQL 을 Docker 컨테이너로 띄운다. 운영(db-01)과 같은 8.4 LTS 를 사용한다.
 * {@code @ServiceConnection} 이 컨테이너의 접속 정보를 DataSource 에 자동으로 넣는다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer("mysql:8.4")
                .withUrlParam("serverTimezone", "UTC")
                .withUrlParam("characterEncoding", "UTF-8");
    }
}