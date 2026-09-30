package kim.autoever.taxi.ride.dto;

/**
 * 진행 중인 호출이 없으면 ride 는 null 이다.
 */
public record CurrentRideResponse(RideResponse ride) {
}
