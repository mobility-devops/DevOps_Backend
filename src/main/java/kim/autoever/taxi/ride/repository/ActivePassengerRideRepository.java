package kim.autoever.taxi.ride.repository;

import kim.autoever.taxi.ride.domain.ActivePassengerRide;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ActivePassengerRideRepository extends JpaRepository<ActivePassengerRide, Long> {

    boolean existsByPassengerId(Long passengerId);

    Optional<ActivePassengerRide> findByPassengerId(Long passengerId);
}
