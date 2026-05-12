-- dict_user 表新增 mooket_no 字段
ALTER TABLE dict_user ADD COLUMN IF NOT EXISTS mooket_no VARCHAR(100);
