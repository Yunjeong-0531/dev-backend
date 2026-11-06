-- 관리자가 회원의 역할을 바꾸면 대상의 세션을 모두 폐기한다 → revoked_reason 에 ROLE_CHANGED 추가.
-- 이미 병합된 마이그레이션(V202610301000__auth_init.sql)은 수정하지 않고, 이렇게 "새 파일"로 변경한다.
ALTER TABLE refresh_tokens
    MODIFY COLUMN revoked_reason
    ENUM('ROTATED','LOGOUT','LOGOUT_ALL','REUSE_DETECTED','PASSWORD_CHANGED','WITHDRAWN','ROLE_CHANGED') NULL;
