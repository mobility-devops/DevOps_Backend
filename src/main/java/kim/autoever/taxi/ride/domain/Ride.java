package kim.autoever.taxi.ride.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import kim.autoever.taxi.common.domain.BaseEntity;
import kim.autoever.taxi.driver.domain.Driver;
import kim.autoever.taxi.user.domain.User;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "rides")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ride extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private User passenger;

    /** 수락 전에는 NULL */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id")
    private Driver driver;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private RideStatus status;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude",
                    column = @Column(name = "pickup_latitude", nullable = false, precision = 9, scale = 6)),
            @AttributeOverride(name = "longitude",
                    column = @Column(name = "pickup_longitude", nullable = false, precision = 9, scale = 6)),
            @AttributeOverride(name = "address",
                    column = @Column(name = "pickup_address", length = 255))
    })
    private Location pickup;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "latitude",
                    column = @Column(name = "destination_latitude", nullable = false, precision = 9, scale = 6)),
            @AttributeOverride(name = "longitude",
                    column = @Column(name = "destination_longitude", nullable = false, precision = 9, scale = 6)),
            @AttributeOverride(name = "address",
                    column = @Column(name = "destination_address", length = 255))
    })
    private Location destination;

    /** 중복 수락 방지용 낙관적 락 */
    @Version
    private Long version;

    private Ride(User passenger, Location pickup, Location destination) {
        this.passenger = passenger;
        this.pickup = pickup;
        this.destination = destination;
        this.status = RideStatus.SEARCHING;
    }

    public static Ride create(User passenger, Location pickup, Location destination) {
        return new Ride(passenger, pickup, destination);
    }

    /** 호출한 승객이거나 배정된 기사인지 확인한다. */
    public boolean isParticipant(Long userId) {
        if (passenger.getId().equals(userId)) {
            return true;
        }
        return driver != null && driver.getUser().getId().equals(userId);
    }
}
