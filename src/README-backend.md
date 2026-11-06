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

## 권한(역할) 규칙

역할은 `GUEST / HOST / ADMIN` 이고 모두 **같은 로그인 API** 로 들어옵니다. 접근 제한은 4단계로 겁니다.

| 수준 | 방법 |
|---|---|
| ① 비로그인 허용 | feature 의 `SecurityRuleContributor` 구현체 |
| ② 관리자 전용 경로 | `/api/admin/**` 는 SecurityConfig 가 ADMIN 만 허용 |
| ③ 역할 제한 | 컨트롤러에 `@AdminOnly` / `@HostOrAdmin` (`common/security`) |
| ④ 소유권 | 서비스에서 `actor.isAdmin() \|\| entity.isOwnedBy(actor.id())` |

선언하지 않은 API 는 기본적으로 "로그인 필요"입니다. 자세한 코드는 `examples/` 를 보세요.

- 관리자 기능: `PATCH /api/admin/users/{userId}/role` (역할 변경, 본인 변경 불가, 대상 세션 폐기)
- 최초 관리자: `app.auth.initial-admin-email` (가입 + 이메일 인증 후 서버 재시작 시 ADMIN 승격)

## 소셜 로그인 (인가 코드 방식)

1. 프론트가 제공자 로그인 화면으로 이동 → 로그인 후 `{frontend-base-url}/auth/callback/{provider}?code=…&state=…` 로 돌아온다
2. 프론트가 `POST /api/auth/social/{provider}` 에 `{ code, redirectUri, state }` 전송
3. 서버가 client-id/secret 으로 code → 제공자 토큰 교환 → 프로필 조회 → 기존 회원이면 로그인, 신규면 `signupToken` 반환
4. 신규는 약관 동의 후 `POST /api/auth/social/complete`

- `redirectUri` 는 `frontend-base-url + /auth/callback/{provider}` 와 **정확히 같아야** 합니다 (아니면 400).
- 제공자 콘솔에 같은 Redirect URI 를 등록해야 합니다. 설정은 `app.auth.social.*` 입니다.
- `allow-access-token: true` 는 개발용(토큰 직접 입력 화면)이며 **운영에서는 false** 입니다.

## 설정 (application.yml)

- 공용: `app.security.jwt.*`, `app.security.allowed-origins`  (이전의 `app.auth.jwt.*`, `app.auth.allowed-origins` 에서 이동)
- auth 전용: `app.auth.*`
- 예시는 `--notuse`
