-- 娣诲姞浼氬憳鍜屾敮浠樺姛鑳?

use ai_passage_creator;

-- 1. 鎵╁睍 user 琛紝娣诲姞浼氬憳鐩稿叧瀛楁
ALTER TABLE user 
ADD COLUMN vipTime DATETIME NULL COMMENT '鎴愪负浼氬憳鏃堕棿';

-- 2. 鍒涘缓鏀粯璁板綍琛?
CREATE TABLE IF NOT EXISTS payment_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '涓婚敭',
    userId BIGINT NOT NULL COMMENT '鐢ㄦ埛ID',
    stripeSessionId VARCHAR(128) COMMENT 'Stripe Checkout Session ID',
    stripePaymentIntentId VARCHAR(128) COMMENT 'Stripe 鏀粯鎰忓悜ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '閲戦锛堢編鍏冿級',
    currency VARCHAR(8) DEFAULT 'usd' COMMENT '璐у竵',
    status VARCHAR(32) NOT NULL COMMENT '鐘舵€侊細PENDING/SUCCEEDED/FAILED/REFUNDED',
    productType VARCHAR(32) NOT NULL COMMENT '浜у搧绫诲瀷锛歏IP_PERMANENT',
    description VARCHAR(256) COMMENT '鎻忚堪',
    refundTime DATETIME NULL COMMENT '閫€娆炬椂闂?,
    refundReason VARCHAR(512) NULL COMMENT '閫€娆惧師鍥?,
    createTime DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '鍒涘缓鏃堕棿',
    updateTime DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '鏇存柊鏃堕棿',
    
    INDEX idx_userId (userId),
    INDEX idx_stripeSessionId (stripeSessionId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
) COMMENT '鏀粯璁板綍琛? COLLATE = utf8mb4_unicode_ci;
