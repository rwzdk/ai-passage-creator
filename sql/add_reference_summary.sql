-- 上传文档仅保存摘要，不保存原始文件
ALTER TABLE article
    ADD COLUMN referenceSummary TEXT NULL COMMENT '上传文档生成的参考摘要' AFTER userDescription;
