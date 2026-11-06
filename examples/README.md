# examples/ — 팀원용 참고 예시

> 이 폴더는 **빌드에 포함되지 않습니다** (Gradle 은 `src/` 만 컴파일). 보고 복사해서 쓰는 용도입니다.
> 경로 구조를 `src/` 와 똑같이 맞춰 두었으니, 복사한 뒤 내용만 자기 기능에 맞게 고치면 됩니다.

```
examples/
└─ backend/
   ├─ accommodation/             "숙소" feature 예시 (역할 + 소유권 검사 포함)
   │  └─ src/main/
   │     ├─ java/com/example/stay/accommodation/
   │     │  ├─ config/       AccommodationSecurityRules   (비로그인 허용 경로 선언)
   │     │  ├─ controller/   AccommodationController      (공개/HOST·ADMIN/소유권)
   │     │  │                AdminAccommodationController (관리자 전용)
   │     │  ├─ service/      AccommodationService         (소유권 검사 assertCanModify)
   │     │  ├─ entity/ repository/ dto/ exception/
   │     └─ resources/db/migration/  V…__accommodation_init.sql
   └─ roles.http                  역할별 권한을 손으로 확인하는 요청 모음 (기대 응답 코드 포함)
```

## 새 feature 를 만들 때 복사하는 순서

1. `examples/backend/accommodation/src/main/...` 를 `src/main/...` 에 복사하고 패키지/클래스 이름을 바꾼다.
2. SQL 파일명의 날짜시각을 **복사한 시점**으로 바꾼다 (`V202611051430__...`).
3. `./gradlew test` → `ArchitectureTest` 가 의존 규칙(common → feature 금지, feature ↔ feature 금지)을 검사한다.
4. `roles.http` 로 역할별 응답 코드를 확인한다.

## 권한을 거는 4가지 방법 (위에서 아래로 바깥 → 안쪽)

| 수준 | 언제 쓰나 | 방법 | 위치 |
|---|---|---|---|
| ① 비로그인 허용 | 숙소 조회처럼 누구나 보는 API | `SecurityRuleContributor` 구현체 `@Component` | `…/config/XxxSecurityRules` |
| ② 관리자 전용 경로 | 관리 화면용 API | 경로를 `/api/admin/**` 아래에 둔다 | SecurityConfig 가 자동으로 ADMIN 만 허용 |
| ③ 역할 제한 | "HOST/ADMIN 만" 같은 역할 조건 | `@AdminOnly`, `@HostOrAdmin` (클래스 또는 메서드) | 컨트롤러 |
| ④ 소유권 | "본인 것만" (역할로는 판단 불가) | 서비스에서 `actor.isAdmin() \|\| entity.isOwnedBy(actor.id())` | 서비스 |

아무 것도 선언하지 않은 API 는 **기본값이 "로그인 필요"** 입니다. (실수로 열려 있는 API 가 생기지 않도록)

### 역할 × API 예시 (accommodation 예시 기준)

| API | 비로그인 | GUEST | HOST | ADMIN |
|---|:-:|:-:|:-:|:-:|
| GET /api/accommodations | O | O | O | O |
| POST /api/accommodations | 401 | 403 | O | O |
| PATCH·DELETE /api/accommodations/{id} | 401 | 403 | 본인 것만 O (남의 것 403) | O |
| /api/admin/** | 401 | 403 | 403 | O |

## 자주 쓰는 코드 조각

```java
// 로그인한 사용자 받기 (auth 의 User 엔티티가 아니라 id/role 만 쓴다)
public ... create(@AuthenticationPrincipal AuthUser user, ...) { ... user.id() ... }

// 서비스에서 역할 확인
if (actor.isAdmin()) { ... }       // AuthUser 의 헬퍼: isAdmin(), isHost()

// 다른 feature 의 사용자는 엔티티가 아니라 id 로만 저장
@Column(name = "host_id", nullable = false) private Long hostId;

// 에러는 feature 별 enum 으로
throw new BusinessException(AccommodationErrorCode.NOT_ACCOMMODATION_OWNER);

// 역할 조건이 특이하면 @PreAuthorize 를 직접 사용할 수도 있다
@PreAuthorize("hasRole('HOST') and #hostId == authentication.principal.id()")
```

## 알아둘 점

- **세 역할(GUEST/HOST/ADMIN)은 같은 로그인 API 를 씁니다.** 역할은 토큰의 `role` 로 구분되고, 접근 제한은 위 ①~④ 에서 합니다.
- **ADMIN 이 HOST 의 모든 권한을 자동으로 갖지는 않습니다.** "HOST 와 ADMIN 둘 다 허용"이면 `@HostOrAdmin` 처럼 둘 다 적습니다.
- **역할을 바꾸면 대상의 로그인 세션이 폐기됩니다** (다시 로그인 필요). 액세스 토큰은 만료(기본 15분)까지 이전 역할로 동작할 수 있습니다.
- **권한 오류는 401/403 으로 내려갑니다.** (`GlobalExceptionHandler` 의 `AccessDeniedException` 핸들러. 이게 없으면 500 이 됩니다.)
- **프론트의 역할 체크는 화면 편의용**입니다. 실제 차단은 항상 서버에서 합니다.

## 관리자 / 호스트 계정 만들기 (개발 환경)

- **최초 ADMIN**: 일반 회원으로 가입 + 이메일 인증 → 환경변수 `INITIAL_ADMIN_EMAIL=내이메일` 로 서버 재시작 → ADMIN 으로 승격됩니다.
- **그 외 역할 변경**: ADMIN 으로 로그인해서 `PATCH /api/admin/users/{userId}/role` 호출 (`roles.http` 11번).
- 회원가입은 항상 GUEST 입니다. HOST 신청/승인 흐름이 필요하면 그때 별도 API 를 만드세요.
