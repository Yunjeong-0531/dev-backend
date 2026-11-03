-- =====================================================================
-- 숙박예약 서비스 - 인증/회원 영역 스키마 (MySQL 8.x, InnoDB, utf8mb4)
-- =====================================================================
-- 설계 원칙
--  * 토큰류(리프레시/이메일인증/비번재설정/가입대기)는 원문이 아닌 SHA-256 해시만 저장
--  * 이메일은 저장 전 소문자 정규화 (애플리케이션에서 처리)
--  * 회원 탈퇴는 soft delete + 개인정보 익명화 (email UNIQUE 충돌 방지)
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- 1. users : 회원
-- ---------------------------------------------------------------------
CREATE TABLE users (
    id                  BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT,
    email               VARCHAR(255)     NOT NULL COMMENT '소문자 정규화. 탈퇴 시 deleted_{id}@withdrawn.local 로 익명화',
    password_hash       VARCHAR(255)     NULL     COMMENT 'bcrypt/argon2. 소셜 전용 가입자는 NULL',
    name                VARCHAR(50)      NOT NULL,
    phone               VARCHAR(20)      NULL,
    phone_verified_at   DATETIME         NULL     COMMENT '휴대폰 본인인증 완료 시각',
    role                ENUM('GUEST','HOST','ADMIN') NOT NULL DEFAULT 'GUEST',
    status              ENUM('PENDING','ACTIVE','SUSPENDED','WITHDRAWN') NOT NULL DEFAULT 'PENDING'
                        COMMENT 'PENDING: 이메일 미인증 / WITHDRAWN: 탈퇴',
    email_verified_at   DATETIME         NULL,
    failed_login_count  SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    locked_until        DATETIME         NULL     COMMENT '로그인 실패 누적 시 잠금 해제 시각',
    last_login_at       DATETIME         NULL,
    password_changed_at DATETIME         NULL,
    created_at          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at          DATETIME         NULL     COMMENT '탈퇴 시각',
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_email (email),
    KEY idx_users_phone (phone),
    KEY idx_users_status (status),
    KEY idx_users_deleted_at (deleted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='회원';

-- ---------------------------------------------------------------------
-- 2. social_accounts : 소셜 계정 연동 (1 회원 : N 소셜)
-- ---------------------------------------------------------------------
CREATE TABLE social_accounts (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id           BIGINT UNSIGNED NOT NULL,
    provider          ENUM('KAKAO','NAVER','GOOGLE','APPLE') NOT NULL,
    provider_user_id  VARCHAR(255)    NOT NULL COMMENT '제공자가 내려주는 고유 ID (sub 등)',
    provider_email    VARCHAR(255)    NULL,
    linked_at         DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_social_provider_uid (provider, provider_user_id),
    UNIQUE KEY uk_social_user_provider (user_id, provider),
    CONSTRAINT fk_social_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='소셜 로그인 연동';

-- ---------------------------------------------------------------------
-- 3. pending_social_signups : 소셜 신규 가입 대기 (/social/complete 용)
--    소셜 인증은 끝났지만 약관 동의/추가정보 입력 전인 상태를 임시 보관
-- ---------------------------------------------------------------------
CREATE TABLE pending_social_signups (
    id                BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    signup_token_hash CHAR(64)        NOT NULL COMMENT '프론트에 내려준 임시 토큰의 SHA-256',
    provider          ENUM('KAKAO','NAVER','GOOGLE','APPLE') NOT NULL,
    provider_user_id  VARCHAR(255)    NOT NULL,
    provider_email    VARCHAR(255)    NULL,
    provider_name     VARCHAR(100)    NULL,
    expires_at        DATETIME        NOT NULL COMMENT '발급 후 약 30분',
    created_at        DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pending_token (signup_token_hash),
    KEY idx_pending_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='소셜 가입 대기(임시)';

-- ---------------------------------------------------------------------
-- 4. refresh_tokens : 리프레시 토큰 (rotation + 재사용 감지)
--    family_id: 최초 로그인 시 생성, rotation 되어도 유지.
--    이미 revoked 된 토큰이 다시 들어오면 같은 family 전체를 폐기.
-- ---------------------------------------------------------------------
CREATE TABLE refresh_tokens (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id          BIGINT UNSIGNED NOT NULL,
    token_hash       CHAR(64)        NOT NULL COMMENT 'SHA-256',
    family_id        CHAR(36)        NOT NULL COMMENT 'UUID. 로그인 1회 = 1 family',
    replaced_by_id   BIGINT UNSIGNED NULL     COMMENT 'rotation 으로 대체된 새 토큰 id',
    device_name      VARCHAR(100)    NULL,
    user_agent       VARCHAR(500)    NULL,
    ip_address       VARCHAR(45)     NULL     COMMENT 'IPv6 대응',
    expires_at       DATETIME        NOT NULL,
    revoked_at       DATETIME        NULL,
    revoked_reason   ENUM('ROTATED','LOGOUT','LOGOUT_ALL','REUSE_DETECTED','PASSWORD_CHANGED','WITHDRAWN') NULL,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_token_hash (token_hash),
    KEY idx_refresh_user (user_id, revoked_at),
    KEY idx_refresh_family (family_id),
    KEY idx_refresh_expires (expires_at),
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='리프레시 토큰';

-- ---------------------------------------------------------------------
-- 5. email_verifications : 이메일 인증 토큰
-- ---------------------------------------------------------------------
CREATE TABLE email_verifications (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    email       VARCHAR(255)    NOT NULL COMMENT '발송 시점의 이메일 (추후 이메일 변경 대비)',
    token_hash  CHAR(64)        NOT NULL,
    expires_at  DATETIME        NOT NULL COMMENT '발송 후 24시간 권장',
    used_at     DATETIME        NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_verif_token (token_hash),
    KEY idx_email_verif_user (user_id, created_at),
    CONSTRAINT fk_email_verif_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='이메일 인증 토큰';

-- ---------------------------------------------------------------------
-- 6. password_reset_tokens : 비밀번호 재설정 토큰
-- ---------------------------------------------------------------------
CREATE TABLE password_reset_tokens (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    token_hash  CHAR(64)        NOT NULL,
    expires_at  DATETIME        NOT NULL COMMENT '30분~1시간',
    used_at     DATETIME        NULL,
    requested_ip VARCHAR(45)    NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_pw_reset_token (token_hash),
    KEY idx_pw_reset_user (user_id, created_at),
    CONSTRAINT fk_pw_reset_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='비밀번호 재설정 토큰';

-- ---------------------------------------------------------------------
-- 7. terms : 약관 (버전 관리)
-- ---------------------------------------------------------------------
CREATE TABLE terms (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    type          VARCHAR(30)     NOT NULL COMMENT 'SERVICE, PRIVACY, MARKETING, AGE_14 등',
    version       VARCHAR(20)     NOT NULL COMMENT '예: 1.0, 2024-06-01',
    title         VARCHAR(100)    NOT NULL,
    content       MEDIUMTEXT      NOT NULL,
    is_required   TINYINT(1)      NOT NULL DEFAULT 1,
    display_order SMALLINT        NOT NULL DEFAULT 0,
    is_active     TINYINT(1)      NOT NULL DEFAULT 1 COMMENT '현재 가입 화면에 노출되는 버전',
    effective_at  DATETIME        NOT NULL COMMENT '시행일',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_terms_type_version (type, version),
    KEY idx_terms_active (is_active, display_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='약관';

-- ---------------------------------------------------------------------
-- 8. term_agreements : 약관 동의 이력
--    철회/재동의도 이력으로 남기기 위해 행을 추가하는 방식 (UPDATE 최소화)
-- ---------------------------------------------------------------------
CREATE TABLE term_agreements (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NOT NULL,
    term_id     BIGINT UNSIGNED NOT NULL,
    agreed      TINYINT(1)      NOT NULL COMMENT '1: 동의, 0: 철회(마케팅 등)',
    ip_address  VARCHAR(45)     NULL,
    agreed_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_agree_user_term (user_id, term_id, agreed_at),
    KEY idx_agree_term (term_id),
    CONSTRAINT fk_agree_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_agree_term FOREIGN KEY (term_id) REFERENCES terms (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='약관 동의 이력';

-- ---------------------------------------------------------------------
-- 9. login_histories : 로그인 시도 이력 (감사/이상 탐지/rate limit 보조)
--    rate limit 자체는 Redis 권장. 이 테이블은 보안 감사용.
-- ---------------------------------------------------------------------
CREATE TABLE login_histories (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id     BIGINT UNSIGNED NULL     COMMENT '존재하지 않는 이메일 시도면 NULL',
    email       VARCHAR(255)    NOT NULL COMMENT '시도된 이메일',
    method      ENUM('PASSWORD','KAKAO','NAVER','GOOGLE','APPLE') NOT NULL DEFAULT 'PASSWORD',
    success     TINYINT(1)      NOT NULL,
    fail_reason VARCHAR(50)     NULL     COMMENT 'BAD_CREDENTIALS, LOCKED, UNVERIFIED, SUSPENDED 등',
    ip_address  VARCHAR(45)     NULL,
    user_agent  VARCHAR(500)    NULL,
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_login_user (user_id, created_at),
    KEY idx_login_email (email, created_at),
    KEY idx_login_ip (ip_address, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='로그인 시도 이력';
