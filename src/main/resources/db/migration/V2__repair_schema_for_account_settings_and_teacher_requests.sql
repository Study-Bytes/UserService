-- Repair migration for environments where Flyway was enabled after legacy schema already existed.
-- Typical case: baseline-on-migrate skipped V1, but entities now require account settings and teacher request fields.

DO
$$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN
        ALTER TABLE users
            ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(1000),
            ADD COLUMN IF NOT EXISTS bio VARCHAR(2000),
            ADD COLUMN IF NOT EXISTS preferred_locale VARCHAR(10);

        ALTER TABLE users
            ALTER COLUMN avatar_url TYPE VARCHAR(1000),
            ALTER COLUMN bio TYPE VARCHAR(2000);

        UPDATE users
        SET preferred_locale = 'ru'
        WHERE preferred_locale IS NULL
           OR btrim(preferred_locale) = '';

        ALTER TABLE users
            ALTER COLUMN preferred_locale SET DEFAULT 'ru',
            ALTER COLUMN preferred_locale SET NOT NULL;
    END IF;
END
$$;

DO
$$
BEGIN
    IF to_regclass('public.refresh_tokens') IS NOT NULL THEN
        ALTER TABLE refresh_tokens
            ADD COLUMN IF NOT EXISTS revoked BOOLEAN;

        UPDATE refresh_tokens
        SET revoked = false
        WHERE revoked IS NULL;

        ALTER TABLE refresh_tokens
            ALTER COLUMN revoked SET DEFAULT false,
            ALTER COLUMN revoked SET NOT NULL;
    END IF;
END
$$;

DO
$$
BEGIN
    IF to_regclass('public.users') IS NOT NULL THEN
        CREATE TABLE IF NOT EXISTS teacher_requests
        (
            id                  BIGSERIAL PRIMARY KEY,
            user_id             BIGINT                   NOT NULL REFERENCES users (id),
            status              VARCHAR(30)              NOT NULL,
            motivation          TEXT                     NOT NULL,
            experience          TEXT                     NULL,
            portfolio_url       VARCHAR(1000)            NULL,
            preferred_topics_json TEXT                   NULL,
            review_comment      TEXT                     NULL,
            created_at          TIMESTAMP WITH TIME ZONE NOT NULL,
            reviewed_at         TIMESTAMP WITH TIME ZONE NULL,
            reviewed_by_user_id BIGINT                   NULL REFERENCES users (id)
        );

        ALTER TABLE teacher_requests
            ADD COLUMN IF NOT EXISTS user_id BIGINT,
            ADD COLUMN IF NOT EXISTS status VARCHAR(30),
            ADD COLUMN IF NOT EXISTS motivation TEXT,
            ADD COLUMN IF NOT EXISTS experience TEXT,
            ADD COLUMN IF NOT EXISTS portfolio_url VARCHAR(1000),
            ADD COLUMN IF NOT EXISTS preferred_topics_json TEXT,
            ADD COLUMN IF NOT EXISTS review_comment TEXT,
            ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE,
            ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP WITH TIME ZONE,
            ADD COLUMN IF NOT EXISTS reviewed_by_user_id BIGINT;

        ALTER TABLE teacher_requests
            ALTER COLUMN status TYPE VARCHAR(30),
            ALTER COLUMN portfolio_url TYPE VARCHAR(1000);

        CREATE INDEX IF NOT EXISTS idx_teacher_requests_user_id ON teacher_requests (user_id);
        CREATE INDEX IF NOT EXISTS idx_teacher_requests_status ON teacher_requests (status);
    END IF;
END
$$;
