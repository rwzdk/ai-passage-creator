# 数据库初始化（基础表结构）
# @author <a href="https://codefather.cn">编程导航学习圈</a>

-- 璁剧疆瀛楃闆嗭紙瑙ｅ喅涓枃涔辩爜闂锛?
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 鍒涘缓搴?
create database if not exists ai_passage_creator CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 鍒囨崲搴?
use ai_passage_creator;

-- 用户表（基础字段，quota 和 vipTime 由增量脚本添加）
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '璐﹀彿',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           null comment '鐢ㄦ埛鏄电О',
    userAvatar   varchar(1024)                          null comment '鐢ㄦ埛澶村儚',
    userProfile  varchar(512)                           null comment '鐢ㄦ埛绠€浠?,
    userEmail    varchar(128)                           null comment 'QQ 閭',
    userPhone    varchar(32)                            null comment '鑱旂郴鐢佃瘽',
    userBlog     varchar(255)                           null comment '涓汉鍗氬',
    userGithub   varchar(255)                           null comment 'GitHub 鍦板潃',
    userRole     varchar(256) default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '缂栬緫鏃堕棿',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '鍒涘缓鏃堕棿',
    updateTime   datetime     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '鏇存柊鏃堕棿',
    isDelete     tinyint      default 0                 not null comment '鏄惁鍒犻櫎',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '鐢ㄦ埛' collate = utf8mb4_unicode_ci;

-- 鍒濆鍖栨暟鎹?
-- 初始化数据
INSERT INTO user (id, userAccount, userPassword, userName, userAvatar, userProfile, userRole) VALUES

-- 文章表（基础字段，style/phase/titleOptions/userDescription/enabledImageMethods 由增量脚本添加）
create table if not exists article
(
    id              bigint auto_increment comment 'id' primary key,
-- 文章表（基础字段，style/phase/titleOptions/userDescription/enabledImageMethods 由增量脚本添加）
    userId          bigint                             not null comment '鐢ㄦ埛ID',
    topic           varchar(500)                       not null comment '閫夐',
    mainTitle       varchar(200)                       null comment '涓绘爣棰?,
    subTitle        varchar(300)                       null comment '鍓爣棰?,
    userId          bigint                             not null comment '用户ID',
    topic           varchar(500)                       not null comment '选题',
    mainTitle       varchar(200)                       null comment '主标题',
    coverImage      varchar(512)                       null comment '灏侀潰鍥?URL',
    outline         json                               null comment '大纲（JSON格式）',
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
    startTime       datetime                           not null comment '开始时间',
    endTime         datetime                           null comment '缁撴潫鏃堕棿',
    durationMs      int                                null comment '耗时（毫秒）',
    status          varchar(20)                        not null comment '鐘舵€侊細SUCCESS/FAILED',
    errorMessage    text                               null comment '閿欒淇℃伅',
    prompt          text                               null comment '浣跨敤鐨凱rompt',
    durationMs      int                                null comment '耗时（毫秒）',
    status          varchar(20)                        not null comment '状态：SUCCESS/FAILED',
    createTime      datetime    default CURRENT_TIMESTAMP not null comment '鍒涘缓鏃堕棿',
    updateTime      datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '鏇存柊鏃堕棿',
    isDelete        tinyint     default 0              not null comment '鏄惁鍒犻櫎',
    INDEX idx_taskId (taskId),
    INDEX idx_agentName (agentName),
    INDEX idx_status (status),
    INDEX idx_createTime (createTime)
) comment '鏅鸿兘浣撴墽琛屾棩蹇楄〃' collate = utf8mb4_unicode_ci;
