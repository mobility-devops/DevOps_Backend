package kim.autoever.taxi.config;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import kim.autoever.taxi.common.response.ErrorResponse;
import kim.autoever.taxi.user.auth.CurrentUser;
import kim.autoever.taxi.user.auth.CurrentUserArgumentResolver;
import kim.autoever.taxi.user.auth.LoginUser;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    public static final String USER_ID_SCHEME = "X-User-Id";

    private static final String ERROR_SCHEMA_REF = "#/components/schemas/ErrorResponse";

    static {
        // @CurrentUser 로 주입되는 LoginUser 는 요청 파라미터가 아니므로 문서에서 제외한다.
        SpringDocUtils.getConfig()
                .addAnnotationsToIgnore(CurrentUser.class)
                .addRequestWrapperToIgnore(LoginUser.class);
    }

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("택시 배차 서비스 API")
                        .description("""
                                택시 호출 → 수락 → 도착 → 시작 → 완료/취소 흐름을 제공하는 백엔드 API입니다.

                                - 인증(JWT)은 사용하지 않고, 요청 헤더 `X-User-Id`로 사용자를 식별합니다.
                                  우측 상단 **Authorize** 에 사용자 ID를 입력하면 모든 요청에 헤더가 포함됩니다.
                                - 오류 응답은 `{ "code", "message", "requestId" }` 형식으로 통일됩니다.
                                - 시각은 UTC ISO 8601 입니다.
                                """)
                        .version("v1"))
                .tags(List.of(
                        new Tag().name("사용자").description("사용자 가입·조회·수정"),
                        new Tag().name("기사").description("기사 정보 및 상태(ONLINE/OFFLINE)"),
                        new Tag().name("차량").description("기사 차량 등록·조회·수정·삭제"),
                        new Tag().name("호출").description("택시 호출 생성·조회·수락·운행·취소")))
                .components(new Components()
                        .schemas(ModelConverters.getInstance().readAll(ErrorResponse.class))
                        .addSecuritySchemes(USER_ID_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER)
                                .name(CurrentUserArgumentResolver.USER_ID_HEADER)
                                .description("사용자 ID (users.id). 가입(POST /api/v1/users) 응답의 id 를 사용합니다.")))
                .addSecurityItem(new SecurityRequirement().addList(USER_ID_SCHEME));
    }

    /**
     * 4xx/5xx 응답에 공통 오류 형식(ErrorResponse)을 자동으로 연결한다.
     */
    @Bean
    public OperationCustomizer errorResponseCustomizer() {
        return (operation, handlerMethod) -> {
            if (operation.getResponses() != null) {
                operation.getResponses().forEach((code, response) -> {
                    boolean error = code.startsWith("4") || code.startsWith("5");
                    // springdoc 은 내용이 없는 응답에 메서드 반환 타입 스키마를 채우므로, 오류 응답은 항상 덮어쓴다.
                    if (error) {
                        response.setContent(new Content().addMediaType(
                                org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
                                new MediaType().schema(new Schema<>().$ref(ERROR_SCHEMA_REF))));
                    }
                });
            }
            return operation;
        };
    }
}
