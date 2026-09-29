package kim.autoever.taxi.driver.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "drivers")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Driver extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private DriverAvailability availability;

    private Driver(User user, DriverAvailability availability) {
        this.user = user;
        this.availability = availability;
    }

    public static Driver create(User user) {
        return new Driver(user, DriverAvailability.OFFLINE);
    }

    public void changeAvailability(DriverAvailability availability) {
        this.availability = availability;
    }
}