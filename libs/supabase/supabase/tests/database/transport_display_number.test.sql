BEGIN;

-- run 7 tests
SELECT plan(7);

-- 1) ensure_year_sequence()
SELECT is(
               ensure_year_sequence('99'),
               'transport_seq_99',
               'ensure_year_sequence returns transport_seq_99'
       );

-- 2) sequence exists
SELECT ok(
               EXISTS(
                   SELECT 1
                   FROM pg_class
                   WHERE relname = 'transport_seq_99'
                     AND relkind = 'S'
               ),
               'transport_seq_99 sequence was created'
       );


-- 3) first INSERT → display_number = YY-000001
INSERT INTO transports (id, updated_at)
VALUES (
           '00000000-0000-0000-0000-000000000001'::uuid,
           NOW()
       );

SELECT is(
               (SELECT display_number
                FROM transports
                WHERE id = '00000000-0000-0000-0000-000000000001'::uuid),
               TO_CHAR(NOW(), 'YY') || '-000001',
               'first display_number matches YY-000001'
       );

-- 4) second INSERT → display_number = YY-000002
INSERT INTO transports (id, updated_at)
VALUES (
           '00000000-0000-0000-0000-000000000002'::uuid,
           NOW()
       );

SELECT is(
               (SELECT display_number
                FROM transports
                WHERE id = '00000000-0000-0000-0000-000000000002'::uuid),
               TO_CHAR(NOW(), 'YY') || '-000002',
               'second display_number matches YY-000002'
       );

-- 5) insert with custom display_number (should be overridden to YY-000003)
INSERT INTO transports (id, updated_at, display_number)
VALUES (
           '00000000-0000-0000-0000-000000000003'::uuid,
           NOW(),
           '99-999999'
       );

SELECT is(
               (SELECT display_number
                FROM transports
                WHERE id = '00000000-0000-0000-0000-000000000003'::uuid),
               TO_CHAR(NOW(), 'YY') || '-000003',
               'insert with provided display_number is overridden to YY-000003'
       );


-- 6) updating display_number must fail
SELECT throws_ok(
               $$UPDATE transports
      SET display_number = '99-123456'
    WHERE id = '00000000-0000-0000-0000-000000000002'::uuid$$,
               'display_number cannot be changed',
               'prevent_display_number_update raises an exception'
       );

-- 7) new year reset test
SELECT is(
               nextval(ensure_year_sequence('26')),
               '1',
               'sequence for a new year (26) starts at 1'
       );

-- finish and rollback
SELECT * FROM finish();
ROLLBACK;