# 鏁版嵁搴撳垵濮嬪寲锛堝熀纭€琛ㄧ粨鏋勶級
# 娉ㄦ剰锛氭鏂囦欢鍙寘鍚熀纭€琛ㄧ粨鏋勶紝鍏朵粬瀛楁鐢卞閲?SQL 鏂囦欢娣诲姞

-- 璁剧疆瀛楃闆嗭紙瑙ｅ喅涓枃涔辩爜闂锛?
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 鍒涘缓搴?
create database if not exists ai_passage_creator CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 鍒囨崲搴?
use ai_passage_creator;

-- 鐢ㄦ埛琛紙鍩虹瀛楁锛宷uota 鍜?vipTime 鐢卞閲忚剼鏈坊鍔狅級
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '璐﹀彿',
    userPassword varchar(512)                           not null comment '瀵嗙爜',
    userName     varchar(256)                           null comment '鐢ㄦ埛鏄电О',
    userAvatar   varchar(1024)                          null comment '鐢ㄦ埛澶村儚',
    userProfile  varchar(512)                           null comment '鐢ㄦ埛绠€浠?,
    userEmail    varchar(128)                           null comment 'QQ 閭',
    userPhone    varchar(32)                            null comment '鑱旂郴鐢佃瘽',
    userBlog     varchar(255)                           null comment '涓汉鍗氬',
    userGithub   varchar(255)                           null comment 'GitHub 鍦板潃',
    userRole     varchar(256) default 'user'            not null comment '鐢ㄦ埛瑙掕壊锛歶ser/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '缂栬緫鏃堕棿',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '鍒涘缓鏃堕棿',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '鏇存柊鏃堕棿',
    isDelete     tinyint      default 0                 not null comment '鏄惁鍒犻櫎',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '鐢ㄦ埛' collate = utf8mb4_unicode_ci;

-- 鍒濆鍖栨暟鎹?
-- 瀵嗙爜鏄?12345678锛圡D5 鍔犲瘑 + 鐩愬€?qc锛?
INSERT INTO user (id, userAccount, userPassword, userName, userAvatar, userProfile, userRole) VALUES

-- 鏂囩珷琛紙鍩虹瀛楁锛宻tyle/phase/titleOptions/userDescription/enabledImageMethods 鐢卞閲忚剼鏈坊鍔狅級
create table if not exists article
(
    id              bigint auto_increment comment 'id' primary key,
    taskId          varchar(64)                        not null comment '浠诲姟ID锛圲UID锛?,
    userId          bigint                             not null comment '鐢ㄦ埛ID',
    topic           varchar(500)                       not null comment '閫夐',
    mainTitle       varchar(200)                       null comment '涓绘爣棰?,
    subTitle        varchar(300)                       null comment '鍓爣棰?,
    outline         json                               null comment '澶х翰锛圝SON鏍煎紡锛?,
    content         text                               null comment '姝ｆ枃锛圡arkdown鏍煎紡锛?,
    fullContent     text                               null comment '瀹屾暣鍥炬枃锛圡arkdown鏍煎紡锛屽惈閰嶅浘锛?,
    coverImage      varchar(512)                       null comment '灏侀潰鍥?URL',
    images          json                               null comment '閰嶅浘鍒楄〃锛圝SON鏁扮粍锛屽寘鍚皝闈㈠浘 position=1锛?,
    status          varchar(20) default 'PENDING'      not null comment '鐘舵€侊細PENDING/PROCESSING/COMPLETED/FAILED',
    errorMessage    text                               null comment '閿欒淇℃伅',
    createTime      datetime    default CURRENT_TIMESTAMP not null comment '鍒涘缓鏃堕棿',
    completedTime   datetime                           null comment '瀹屾垚鏃堕棿',
    updateTime      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '鏇存柊鏃堕棿',
    isDelete        tinyint     default 0              not null comment '鏄惁鍒犻櫎',
    UNIQUE KEY uk_taskId (taskId),
    INDEX idx_userId (userId),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime),
    INDEX idx_userId_status (userId, status)
) comment '鏂囩珷琛? collate = utf8mb4_unicode_ci;

-- 鏅鸿兘浣撴墽琛屾棩蹇楄〃
create table if not exists agent_log
(
    id              bigint auto_increment comment 'id' primary key,
    taskId          varchar(64)                        not null comment '浠诲姟ID',
    agentName       varchar(50)                        not null comment '鏅鸿兘浣撳悕绉?,
    startTime       datetime                           not null comment '寮€濮嬫椂闂?,
    endTime         datetime                           null comment '缁撴潫鏃堕棿',
    durationMs      int                                null comment '鑰楁椂锛堟绉掞級',
    status          varchar(20)                        not null comment '鐘舵€侊細SUCCESS/FAILED',
    errorMessage    text                               null comment '閿欒淇℃伅',
    prompt          text                               null comment '浣跨敤鐨凱rompt',
    inputData       json                               null comment '杈撳叆鏁版嵁锛圝SON鏍煎紡锛?,
    outputData      json                               null comment '杈撳嚭鏁版嵁锛圝SON鏍煎紡锛?,
    createTime      datetime    default CURRENT_TIMESTAMP not null comment '鍒涘缓鏃堕棿',
    updateTime      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '鏇存柊鏃堕棿',
    isDelete        tinyint     default 0              not null comment '鏄惁鍒犻櫎',
    INDEX idx_taskId (taskId),
    INDEX idx_agentName (agentName),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
) comment '鏅鸿兘浣撴墽琛屾棩蹇楄〃' collate = utf8mb4_unicode_ci;
