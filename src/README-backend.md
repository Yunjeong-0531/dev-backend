# 백엔드 구조 (feature 별 패키지 + common 공유)

```
com.example.stay
├─ common/                       모든 feature 가 공유 (feature 를 import 하지 않는다)
│  ├─ ErrorCode(인터페이스), CommonErrorCode, BusinessException, ErrorResponse, GlobalExceptionHandler
│  ├─ RateLimiter, TokenUtil, EmailUtil, ClientInfo, BaseCreatedEntity/BaseTimeEntity, JpaConfig, AppConfig
│  └─ security/                  SecurityConfig, JwtProvider, JwtAuthenticationFilter,
│                                AuthUser, UserRole, SecurityProperties, SecurityRuleContributor
├─ auth/                         인증 feature
│  ├─ config/ (AuthProperties, AuthConfig, AuthSecurityRules)
│  ├─ controller/ service/ repository/ entity/ dto/ enums/ event/
│  └─ exception/ (AuthErrorCode)
└─ accommodation/                숙소 feature (담당자가 추가)
```

## 규칙

| 규칙 | 방법 |
|---|---|
| common → feature import 금지, feature ↔ feature import 금지 | `ArchitectureTest` (ArchUnit) 가 PR 에서 검사 |
| 에러 코드는 feature 마다 자기 enum | `XxxErrorCode implements ErrorCode` (공통 코드는 `CommonErrorCode`). 코드 이름은 구체적으로 (예: `ACCOMMODATION_NOT_FOUND`) |
| 로그인 사용자는 `@AuthenticationPrincipal AuthUser` | `com.example.stay.common.security.AuthUser` — auth 의 `User` 엔티티를 가져오지 말고 `user.id()` 만 사용 |
| 다른 feature 의 데이터는 id 로만 참조 | 예: 예약 테이블은 `user_id BIGINT` 로 참조 (JPA 연관관계로 `User` 엔티티를 직접 엮지 않기) |
| 공개/인증 경로는 feature 가 직접 선언 | `SecurityRuleContributor` 를 구현한 `@Component` (SecurityConfig 를 수정하지 않는다) |
| DB 변경은 Flyway 마이그레이션 파일 추가 | `--notuse`. **이미 병합된 파일은 수정 금지**, 변경은 새 파일로 |

## 새 feature 추가 절차 (예: accommodation)

1. `com.example.stay.accommodation` 아래에 `controller / service / repository / entity / dto / exception` 생성
2. `AccommodationErrorCode implements ErrorCode` 작성
3. 비로그인 조회가 필요하면 `SecurityRuleContributor` 구현체 추가
   ```java
   @Component
   class AccommodationSecurityRules implements SecurityRuleContributor {
       public List<PublicEndpoint> publicEndpoints() {
           return List.of(PublicEndpoint.get("/api/accommodations/**"));   // GET 만 공개
       }
   }
   ```
4. 테이블은 `V<날짜시각>__accommodation_init.sql` 로 추가
5. `ArchitectureTest` 에 규칙 한 쌍 추가 (이미 accommodation 규칙은 들어 있음)

## 설정 (application.yml)

- 공용: `app.security.jwt.*`, `app.security.allowed-origins`  (이전의 `app.auth.jwt.*`, `app.auth.allowed-origins` 에서 이동)
- auth 전용: `app.auth.*`
- 예시는 `--notuse`
