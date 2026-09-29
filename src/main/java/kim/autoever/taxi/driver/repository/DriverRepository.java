package kim.autoever.taxi.driver.repository;

import kim.autoever.taxi.driver.domain.Driver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DriverRepository extends JpaRepository<Driver, Long> {

    Optional<Driver> findByUserId(Long userId);
}