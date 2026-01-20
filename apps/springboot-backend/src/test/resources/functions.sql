-- Waste stream number sequences (per processor/tenant)
CREATE TABLE IF NOT EXISTS waste_stream_sequences (
    processor_party_id VARCHAR(5) PRIMARY KEY,
    current_value BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);;

-- Insert initial sequence for tenant (starting at 0 for tests)
INSERT INTO waste_stream_sequences (processor_party_id, current_value)
VALUES ('08797', 0)
ON CONFLICT (processor_party_id) DO NOTHING;;

-- Function to get next sequence value atomically
CREATE OR REPLACE FUNCTION public.next_waste_stream_sequence(p_processor_party_id VARCHAR(5))
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_next_value BIGINT;
BEGIN
    INSERT INTO waste_stream_sequences (processor_party_id, current_value, updated_at)
    VALUES (p_processor_party_id, 1, NOW())
    ON CONFLICT (processor_party_id) DO UPDATE
    SET current_value = waste_stream_sequences.current_value + 1,
        updated_at = NOW()
    RETURNING current_value INTO v_next_value;
    
    IF v_next_value > 9999999 THEN
        RAISE EXCEPTION 'Maximum waste stream sequence exceeded for processor %', p_processor_party_id;
    END IF;
    
    RETURN v_next_value;
END;
$$;;
