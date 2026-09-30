package kim.autoever.taxi.ride.repository;

import kim.autoever.taxi.ride.domain.ActiveAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActiveAssignmentRepository extends JpaRepository<ActiveAssignment, Long> {

    boolean existsByDriverId(Long driverId);
}
