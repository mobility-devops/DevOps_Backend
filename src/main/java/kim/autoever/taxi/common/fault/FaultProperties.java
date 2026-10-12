package kim.autoever.taxi.common.fault;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 자동 롤백 시험용 고장 주입 설정. 환경변수 FAULT_MODE, FAULT_RATE, FAULT_DELAY_MS 로 받는다.
 * 아무것도 설정하지 않으면 고장 없음(NONE)이다.
 */
@ConfigurationProperties("fault")
public record FaultProperties(
        @DefaultValue("NONE") Mode mode,
        @DefaultValue("1.0") double rate,
        @DefaultValue("700") long delayMs) {

    public enum Mode { NONE, ERROR, SLOW }

    public FaultProperties {
        // 오타가 조용히 "정상"으로 넘어가면 시험 결과를 오해하므로 기동 시점에 실패시킨다.
        if (rate < 0.0 || rate > 1.0) {
            throw new IllegalArgumentException("FAULT_RATE 는 0.0 ~ 1.0 이어야 합니다: " + rate);
        }
        if (delayMs < 0) {
            throw new IllegalArgumentException("FAULT_DELAY_MS 는 0 이상이어야 합니다: " + delayMs);
        }
    }
}