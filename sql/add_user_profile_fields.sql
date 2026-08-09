SET NAMES utf8mb4;
USE ai_passage_creator;

SET @profile_email_sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `user` ADD COLUMN `userEmail` varchar(128) NULL COMMENT ''QQ 邮箱'' AFTER `userProfile`',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'userEmail'
);
PREPARE profile_email_stmt FROM @profile_email_sql;
EXECUTE profile_email_stmt;
DEALLOCATE PREPARE profile_email_stmt;

SET @profile_phone_sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `user` ADD COLUMN `userPhone` varchar(32) NULL COMMENT ''联系电话'' AFTER `userEmail`',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'userPhone'
);
PREPARE profile_phone_stmt FROM @profile_phone_sql;
EXECUTE profile_phone_stmt;
DEALLOCATE PREPARE profile_phone_stmt;

SET @profile_blog_sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `user` ADD COLUMN `userBlog` varchar(255) NULL COMMENT ''个人博客'' AFTER `userPhone`',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'userBlog'
);
PREPARE profile_blog_stmt FROM @profile_blog_sql;
EXECUTE profile_blog_stmt;
DEALLOCATE PREPARE profile_blog_stmt;

SET @profile_github_sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `user` ADD COLUMN `userGithub` varchar(255) NULL COMMENT ''GitHub 地址'' AFTER `userBlog`',
              'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'user' AND column_name = 'userGithub'
);
PREPARE profile_github_stmt FROM @profile_github_sql;
EXECUTE profile_github_stmt;
DEALLOCATE PREPARE profile_github_stmt;
