-- 添加用户表新字段（如果不存在）
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dict_user' AND column_name = 'avatar_url') THEN
        ALTER TABLE dict_user ADD COLUMN avatar_url VARCHAR(500);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dict_user' AND column_name = 'real_name') THEN
        ALTER TABLE dict_user ADD COLUMN real_name VARCHAR(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dict_user' AND column_name = 'real_name_status') THEN
        ALTER TABLE dict_user ADD COLUMN real_name_status VARCHAR(20) DEFAULT 'pending';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dict_user' AND column_name = 'cancellation_status') THEN
        ALTER TABLE dict_user ADD COLUMN cancellation_status VARCHAR(20) DEFAULT 'active';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'dict_user' AND column_name = 'mooket_id') THEN
        ALTER TABLE dict_user ADD COLUMN mooket_id VARCHAR(50);
    END IF;
END $$;