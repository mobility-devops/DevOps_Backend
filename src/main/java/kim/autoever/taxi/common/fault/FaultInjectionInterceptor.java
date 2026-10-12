package kim.autoever.taxi.common.fault;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kim.autoever.taxi.common.exception.BusinessException;
import kim.autoever.taxi.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.DoubleSupplier;

/**
 * 자동 롤백 시험용 고장 주입. /api/** 요청 중 FAULT_RATE 비율만 500 을 반환하거나 응답을 늦춘다.
 * WebConfig 에서 /api/** 에만 등록하므로 /actuator/** (probe, 메트릭)는 고장 대상이 아니다.
 */
@Component
public class FaultInjectionInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(FaultInjectionInterceptor.class);

    private final FaultProperties properties;
    private final DoubleSupplier random;

    @Autowired
    public FaultInjectionInterceptor(FaultProperties properties) {
        this(properties, () -> ThreadLocalRandom.current().nextDouble());
    }

    FaultInjectionInterceptor(FaultProperties properties, DoubleSupplier random) {
        this.properties = properties;
        this.random = random;
    }

    @PostConstruct
    void logMode() {
        if (properties.mode() != FaultProperties.Mode.NONE) {
            log.warn("고장 주입 활성화: mode={}, rate={}, delayMs={}",
                    properties.mode(), properties.rate(), properties.delayMs());
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws InterruptedException {
        if (properties.mode() == FaultProperties.Mode.NONE || random.getAsDouble() >= properties.rate()) {
            return true;
        }
        if (properties.mode() == FaultProperties.Mode.SLOW) {
            Thread.sleep(properties.delayMs());
            return true;
        }
        // ERROR: GlobalExceptionHandler 가 {code, message, requestId} 형식의 500 으로 바꿔 준다.
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "fault injection");
    }
}