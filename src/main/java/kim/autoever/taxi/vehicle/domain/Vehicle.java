package kim.autoever.taxi.vehicle.domain;

import jakarta.persistence.Column;
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

@Getter
@Entity
@Table(name = "vehicles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vehicle extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "driver_id", nullable = false, unique = true)
    private Driver driver;

    @Column(name = "plate_number", nullable = false, length = 20, unique = true)
    private String plateNumber;

    @Column(nullable = false, length = 50)
    private String model;

    private Vehicle(Driver driver, String plateNumber, String model) {
        this.driver = driver;
        this.plateNumber = plateNumber;
        this.model = model;
    }

    public static Vehicle create(Driver driver, String plateNumber, String model) {
        return new Vehicle(driver, plateNumber, model);
    }

    public void update(String plateNumber, String model) {
        this.plateNumber = plateNumber;
        this.model = model;
    }
}