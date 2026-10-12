package kim.autoever.taxi.config;

import kim.autoever.taxi.common.fault.FaultInjectionInterceptor;
import kim.autoever.taxi.common.fault.FaultProperties;
import kim.autoever.taxi.user.auth.CurrentUserArgumentResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@EnableConfigurationProperties(FaultProperties.class)
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserArgumentResolver currentUserArgumentResolver;
    private final FaultInjectionInterceptor faultInjectionInterceptor;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // /actuator/** 는 등록하지 않는다: probe 가 실패하면 판정이 아니라 Pod 재시작으로 끝난다.
        registry.addInterceptor(faultInjectionInterceptor).addPathPatterns("/api/**");
    }
}