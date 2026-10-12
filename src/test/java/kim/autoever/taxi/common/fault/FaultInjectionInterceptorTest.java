package kim.autoever.taxi.common.fault;

import kim.autoever.taxi.common.exception.GlobalExceptionHandler;
import kim.autoever.taxi.common.exception.RequestIdFilter;
import kim.autoever.taxi.common.fault.FaultProperties.Mode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.DoubleSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FaultInjectionInterceptorTest {

    private MockMvc mockMvc(FaultProperties properties, DoubleSupplier random) {
        return MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilters(new RequestIdFilter())
                .addMappedInterceptors(new String[]{"/api/**"}, new FaultInjectionInterceptor(properties, random))
                .build();
    }

    @Test
    void none_이면_고장_없이_통과한다() throws Exception {
        mockMvc(new FaultProperties(Mode.NONE, 1.0, 700), () -> 0.0)
                .perform(get("/api/v1/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void error_100퍼센트면_api는_표준_형식의_500을_반환한다() throws Exception {
        mockMvc(new FaultProperties(Mode.ERROR, 1.0, 700), () -> 0.99)
                .perform(get("/api/v1/ping"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void actuator는_고장_대상이_아니다() throws Exception {
        mockMvc(new FaultProperties(Mode.ERROR, 1.0, 700), () -> 0.0)
                .perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void error_비율에_따라_일부_요청만_실패한다() throws Exception {
        FaultProperties properties = new FaultProperties(Mode.ERROR, 0.05, 700);
        mockMvc(properties, () -> 0.01).perform(get("/api/v1/ping")).andExpect(status().isInternalServerError());
        mockMvc(properties, () -> 0.50).perform(get("/api/v1/ping")).andExpect(status().isOk());
    }

    @Test
    void rate_0이면_절대_고장나지_않는다() throws Exception {
        mockMvc(new FaultProperties(Mode.ERROR, 0.0, 700), () -> 0.0)
                .perform(get("/api/v1/ping"))
                .andExpect(status().isOk());
    }

    @Test
    void slow면_지정한_시간_이상_지연된_뒤_정상_응답한다() throws Exception {
        MockMvc mvc = mockMvc(new FaultProperties(Mode.SLOW, 1.0, 200), () -> 0.0);
        long start = System.nanoTime();
        mvc.perform(get("/api/v1/ping")).andExpect(status().isOk());
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        assertThat(elapsedMs).isGreaterThanOrEqualTo(200);
    }

    @Test
    void 설정을_안_하면_기본값은_고장_없음이다() {
        FaultProperties properties = bind(Map.of());
        assertThat(properties.mode()).isEqualTo(Mode.NONE);
        assertThat(properties.rate()).isEqualTo(1.0);
        assertThat(properties.delayMs()).isEqualTo(700);
    }

    @Test
    void 환경변수처럼_소문자_값도_읽는다() {
        FaultProperties properties = bind(Map.of("fault.mode", "error", "fault.rate", "0.05", "fault.delay-ms", "300"));
        assertThat(properties.mode()).isEqualTo(Mode.ERROR);
        assertThat(properties.rate()).isEqualTo(0.05);
        assertThat(properties.delayMs()).isEqualTo(300);
    }

    @Test
    void 잘못된_설정은_기동_시점에_실패한다() {
        assertThatThrownBy(() -> bind(Map.of("fault.mode", "eror"))).isInstanceOf(BindException.class);
        assertThatThrownBy(() -> bind(Map.of("fault.rate", "1.5"))).isInstanceOf(BindException.class);
        assertThatThrownBy(() -> bind(Map.of("fault.delay-ms", "-1"))).isInstanceOf(BindException.class);
    }

    private FaultProperties bind(Map<String, String> source) {
        return new Binder(new MapConfigurationPropertySource(source)).bindOrCreate("fault", FaultProperties.class);
    }

    @RestController
    static class TestController {
        @GetMapping("/api/v1/ping")
        String ping() {
            return "ok";
        }

        @GetMapping("/actuator/health")
        String health() {
            return "UP";
        }
    }
}