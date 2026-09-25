-- Dev-only: 100 tickets + history + comments. Idempotent if "Seed ticket #" exists.

WITH guard AS (
    SELECT NOT EXISTS (SELECT 1 FROM ticket WHERE title LIKE 'Seed ticket #%') AS run_seed
),
series AS (
    SELECT g AS n
    FROM generate_series(1, 100) AS g
    CROSS JOIN guard
    WHERE guard.run_seed
),
inserted AS (
    INSERT INTO ticket (
        title,
        description,
        priority,
        status,
        assignee,
        created_at,
        updated_at,
        version
    )
    SELECT
        'Seed ticket #' || s.n || ': ' || topics.topic,
        'Demo description for ticket ' || s.n
            || '. Keywords: login portal password staging. '
            || 'Reported by support seed script.',
        priorities.priority,
        statuses.status,
        CASE
            WHEN s.n % 4 = 0 THEN NULL
            ELSE assignees.name
        END,
        CURRENT_TIMESTAMP - (s.n || ' hours')::INTERVAL,
        CURRENT_TIMESTAMP - ((GREATEST(s.n - 1, 0)) || ' hours')::INTERVAL,
        0
    FROM series s
    CROSS JOIN LATERAL (
        SELECT (ARRAY[
            'Cannot log in',
            'Password reset missing',
            'SSO redirect loop',
            'Invoice PDF blank',
            'Mobile sync delay'
        ])[1 + (s.n % 5)] AS topic
    ) topics
    CROSS JOIN LATERAL (
        SELECT (ARRAY['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'])[1 + (s.n % 4)] AS priority
    ) priorities
    CROSS JOIN LATERAL (
        SELECT (ARRAY[
            'OPEN',
            'IN_PROGRESS',
            'RESOLVED',
            'CLOSED',
            'CANCELLED'
        ])[1 + (s.n % 5)] AS status
    ) statuses
    CROSS JOIN LATERAL (
        SELECT (ARRAY['Priya', 'Rahul', 'Alex', 'Sam'])[1 + (s.n % 4)] AS name
    ) assignees
    RETURNING id, status, created_at
)
INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at, note)
SELECT id, NULL, 'OPEN', created_at, NULL
FROM inserted;

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at, note)
SELECT id, 'OPEN', 'IN_PROGRESS', created_at + INTERVAL '15 minutes', NULL
FROM ticket
WHERE title LIKE 'Seed ticket #%'
  AND status IN ('IN_PROGRESS', 'RESOLVED', 'CLOSED');

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at, note)
SELECT id, 'IN_PROGRESS', 'RESOLVED', created_at + INTERVAL '45 minutes',
       'Seed resolution note for ticket ' || id
FROM ticket
WHERE title LIKE 'Seed ticket #%'
  AND status IN ('RESOLVED', 'CLOSED');

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at, note)
SELECT id, 'RESOLVED', 'CLOSED', created_at + INTERVAL '2 hours', NULL
FROM ticket
WHERE title LIKE 'Seed ticket #%'
  AND status = 'CLOSED';

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at, note)
SELECT id, 'OPEN', 'CANCELLED', created_at + INTERVAL '20 minutes',
       'Seed cancellation note for ticket ' || id
FROM ticket
WHERE title LIKE 'Seed ticket #%'
  AND status = 'CANCELLED'
  AND id % 2 = 0;

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at, note)
SELECT id, 'IN_PROGRESS', 'CANCELLED', created_at + INTERVAL '40 minutes',
       'Seed cancellation note for ticket ' || id
FROM ticket
WHERE title LIKE 'Seed ticket #%'
  AND status = 'CANCELLED'
  AND id % 2 = 1;

INSERT INTO ticket_status_history (ticket_id, from_status, to_status, changed_at, note)
SELECT id, 'OPEN', 'IN_PROGRESS', created_at + INTERVAL '25 minutes', NULL
FROM ticket
WHERE title LIKE 'Seed ticket #%'
  AND status = 'CANCELLED'
  AND id % 2 = 1;

INSERT INTO comment (ticket_id, author, body, created_at)
SELECT
    t.id,
    (ARRAY['Rahul', 'Priya', 'Alex'])[1 + (t.id % 3)],
    'Seed comment on ticket ' || t.id || '. Reproduced in staging.',
    t.created_at + INTERVAL '20 minutes'
FROM ticket t
WHERE t.title LIKE 'Seed ticket #%'
  AND t.id % 2 = 0;
