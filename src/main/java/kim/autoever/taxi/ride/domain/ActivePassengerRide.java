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
import kim.autoever.taxi.user.domain.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 승객당 진행 중인 호출 1개를 보장한다. passenger_id UNIQUE 제약이 동시 요청도 막는다.
 */
@Getter
@Entity
@Table(name = "active_passenger_rides")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivePassengerRide extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false, unique = true)
    private User passenger;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ride_id", nullable = false, unique = true)
    private Ride ride;

    private ActivePassengerRide(User passenger, Ride ride) {
        this.passenger = passenger;
        this.ride = ride;
    }

    public static ActivePassengerRide create(User passenger, Ride ride) {
        return new ActivePassengerRide(passenger, ride);
    }
}
