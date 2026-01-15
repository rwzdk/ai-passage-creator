-- 用户配额升级脚本
-- 为已有数据库添加 quota 字段

-- 添加 quota 字段（如果不存在）
ALTER TABLE user ADD COLUMN IF NOT EXISTS quota int default 5 not null comment '剩余配额' AFTER userRole;

-- 为已有用户设置默认配额
UPDATE user SET quota = 5 WHERE quota IS NULL;
