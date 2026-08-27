-- 鐢ㄦ埛閰嶉鍗囩骇鑴氭湰

use ai_passage_creator;

-- 娣诲姞 quota 瀛楁
ALTER TABLE user ADD COLUMN quota int default 5 not null comment '鍓╀綑閰嶉' AFTER userRole;

-- 涓哄凡鏈夌敤鎴疯缃粯璁ら厤棰?
UPDATE user SET quota = 5 WHERE quota IS NULL;
