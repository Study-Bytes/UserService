ALTER TABLE users
    ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS bio VARCHAR(2000),
    ADD COLUMN IF NOT EXISTS preferred_locale VARCHAR(10) NOT NULL DEFAULT 'ru';

ALTER TABLE users
    ALTER COLUMN avatar_url TYPE VARCHAR(1000),
    ALTER COLUMN bio TYPE VARCHAR(2000);

CREATE TABLE IF NOT EXISTS teacher_requests (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    status VARCHAR(30) NOT NULL,
    motivation TEXT NOT NULL,
    experience TEXT NULL,
    portfolio_url VARCHAR(1000) NULL,
    preferred_topics_json TEXT NULL,
    review_comment TEXT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NULL,
    reviewed_by_user_id BIGINT NULL REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_teacher_requests_user_id ON teacher_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_teacher_requests_status ON teacher_requests(status);
