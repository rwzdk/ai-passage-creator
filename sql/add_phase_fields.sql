# 娣诲姞闃舵鐩稿叧瀛楁

use ai_passage_creator;

-- 涓?article 琛ㄦ坊鍔犻樁娈电浉鍏冲瓧娈?
ALTER TABLE article
    ADD COLUMN phase VARCHAR(50) DEFAULT 'PENDING' COMMENT '褰撳墠闃舵锛歅ENDING/TITLE_GENERATING/TITLE_SELECTING/OUTLINE_GENERATING/OUTLINE_EDITING/CONTENT_GENERATING' AFTER status,
    ADD COLUMN titleOptions JSON NULL COMMENT '鏍囬鏂规鍒楄〃锛?-5涓柟妗堬級' AFTER subTitle,
    ADD COLUMN userDescription TEXT NULL COMMENT '鐢ㄦ埛琛ュ厖鎻忚堪' AFTER topic,
    ADD COLUMN enabledImageMethods JSON NULL COMMENT '鍏佽鐨勯厤鍥炬柟寮忓垪琛? AFTER userDescription;
