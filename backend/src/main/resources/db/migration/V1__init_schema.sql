-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- 账户表
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL,
    note TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_accounts_user_id ON accounts(user_id);
CREATE INDEX IF NOT EXISTS idx_accounts_user_status ON accounts(user_id, status);

-- 存款记录表
CREATE TABLE IF NOT EXISTS deposits (
    id BIGINT PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    deposit_type VARCHAR(50) NOT NULL,
    deposit_time DATE NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    interest_rate DECIMAL(5,2),
    term INTEGER,
    note TEXT,
    reconciliation_date DATE NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

CREATE INDEX IF NOT EXISTS idx_deposits_user_reconciliation ON deposits(user_id, reconciliation_date);
CREATE INDEX IF NOT EXISTS idx_deposits_account_reconciliation ON deposits(account_id, reconciliation_date);
CREATE INDEX IF NOT EXISTS idx_deposits_user_account_reconciliation ON deposits(user_id, account_id, reconciliation_date);
CREATE INDEX IF NOT EXISTS idx_deposits_reconciliation_date ON deposits(reconciliation_date);

-- 对账快照表
CREATE TABLE IF NOT EXISTS reconciliation_snapshots (
    id BIGINT PRIMARY KEY AUTOINCREMENT,
    user_id BIGINT NOT NULL,
    reconciliation_date DATE NOT NULL,
    total_amount DECIMAL(15,2) NOT NULL,
    note TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE(user_id, reconciliation_date)
);

CREATE INDEX IF NOT EXISTS idx_snapshots_user_date ON reconciliation_snapshots(user_id, reconciliation_date);
CREATE INDEX IF NOT EXISTS idx_snapshots_date ON reconciliation_snapshots(reconciliation_date);
