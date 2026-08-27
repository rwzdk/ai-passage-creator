# 娣诲姞鏂囩珷椋庢牸瀛楁

use ai_passage_creator;

-- 涓?article 琛ㄦ坊鍔?style 瀛楁锛堟枃绔犻鏍硷級
ALTER TABLE article
    ADD COLUMN style VARCHAR(20) NULL COMMENT '鏂囩珷椋庢牸锛歵ech/emotional/educational/humorous' AFTER topic;
