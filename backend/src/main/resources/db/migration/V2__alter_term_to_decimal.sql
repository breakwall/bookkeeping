-- V2__alter_term_to_decimal.sql
-- 将 deposits 表的 term 字段从 INTEGER 改为 DECIMAL(3,1) 以支持 0.5 年等小数值

-- 1. 创建新表（term 字段类型改为 DECIMAL）
CREATE TABLE deposits_new (
    id BIGINT PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    deposit_type VARCHAR(50) NOT NULL,
    deposit_time DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,2),
    term DECIMAL(3,1),
    note TEXT,
    reconciliation_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- 2. 复制数据（INTEGER 自动转换为 DECIMAL）
INSERT INTO deposits_new (id, user_id, account_id, deposit_type, deposit_time, amount, interest_rate, term, note, reconciliation_date, created_at, updated_at)
SELECT id, user_id, account_id, deposit_type, deposit_time, amount, interest_rate, CAST(term AS DECIMAL(3,1)), note, reconciliation_date, created_at, updated_at
FROM deposits;

-- 3. 保留旧表作为备份（以防万一）
ALTER TABLE deposits RENAME TO deposits_backup;

-- 4. 重命名新表
ALTER TABLE deposits_new RENAME TO deposits;

-- 5. 重建索引
CREATE INDEX IF NOT EXISTS idx_deposits_user_reconciliation ON deposits(user_id, reconciliation_date);
CREATE INDEX IF NOT EXISTS idx_deposits_account_reconciliation ON deposits(account_id, reconciliation_date);
CREATE INDEX IF NOT EXISTS idx_deposits_user_account_reconciliation ON deposits(user_id, account_id, reconciliation_date);
CREATE INDEX IF NOT EXISTS idx_deposits_reconciliation_date ON deposits(reconciliation_date);

-- 注意：迁移成功后，可以手动删除备份表：DROP TABLE deposits_backup;
