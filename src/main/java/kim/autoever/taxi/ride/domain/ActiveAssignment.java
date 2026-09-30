package kim.autoever.taxi.ride.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kim.autoever.taxi.common.domain.BaseEntity;
import kim.autoever.taxi.driver.domain.Driver;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기사당 활성 운행 1개, 호출당 배정 기사 1명을 보장한다. driver_id, ride_id UNIQUE 제약이 동시 수락도 막는다.
 */
@Getter
@Entity
@Table(name = "active_assignments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActiveAssignment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private Driver driver;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    private Ride ride;

    private ActiveAssignment(Driver driver, Ride ride) {
        this.driver = driver;
        this.ride = ride;
    }

    public static ActiveAssignment create(Driver driver, Ride ride) {
        return new ActiveAssignment(driver, ride);
    }
}
