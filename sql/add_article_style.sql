# 娣诲姞鏂囩珷椋庢牸瀛楁

use ai_passage_creator;

-- 为 article 表添加 style 字段（文章风格）
ALTER TABLE article
    ADD COLUMN style VARCHAR(20) NULL COMMENT '文章风格：tech/emotional/educational/humorous' AFTER topic;
