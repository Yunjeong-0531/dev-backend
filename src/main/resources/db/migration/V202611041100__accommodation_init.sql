-- [예시] 복사한 뒤 파일명의 날짜시각(V202611041100)을 "복사한 시점"으로 바꾸세요.
-- 규칙: V<yyyyMMddHHmm>__<feature>_<설명>.sql  /  이미 병합된 파일은 수정하지 말고 새 파일로 변경한다.
CREATE TABLE accommodations (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    host_id         BIGINT UNSIGNED NOT NULL COMMENT '등록한 호스트(users.id). 코드에서는 id 로만 참조',
    name            VARCHAR(100)    NOT NULL,
    region          VARCHAR(30)     NOT NULL,
    price_per_night INT             NOT NULL,
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_accommodations_host (host_id),
    KEY idx_accommodations_region (region),
    -- DB 외래키는 걸어도 된다. (코드에서 User 엔티티를 직접 엮지 않는 것과는 별개)
    CONSTRAINT fk_accommodation_host FOREIGN KEY (host_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='숙소';
