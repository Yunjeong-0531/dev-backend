-- auth 초기 데이터: 약관 (본문은 실제 약관으로 교체할 것)
-- ---------------------------------------------------------------------
INSERT INTO terms (type, version, title, content, is_required, display_order, effective_at) VALUES
('SERVICE',   '1.0', '서비스 이용약관',               '(약관 본문)', 1, 1, NOW()),
('PRIVACY',   '1.0', '개인정보 수집 및 이용 동의',     '(약관 본문)', 1, 2, NOW()),
('AGE_14',    '1.0', '만 14세 이상 확인',             '(약관 본문)', 1, 3, NOW()),
('MARKETING', '1.0', '마케팅 정보 수신 동의 (선택)',   '(약관 본문)', 0, 4, NOW());
