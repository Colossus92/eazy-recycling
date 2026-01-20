-- Migration: Add waste stream number sequence per tenant
-- This sequence is used to generate unique waste stream numbers.
-- The sequence is tenant-aware to support multi-tenancy in the future.

-- Create a table to store tenant-specific sequences
-- This allows each tenant to have their own sequence with a custom starting value
CREATE TABLE IF NOT EXISTS public.waste_stream_sequences (
    processor_party_id VARCHAR(5) PRIMARY KEY,
    current_value BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Add comment for documentation
COMMENT ON TABLE public.waste_stream_sequences IS 'Stores the current sequence value for waste stream numbers per processor (tenant). Supports multi-tenancy by having separate sequences per processor_party_id.';
COMMENT ON COLUMN public.waste_stream_sequences.processor_party_id IS 'The 5-digit processor party ID (tenant identifier)';
COMMENT ON COLUMN public.waste_stream_sequences.current_value IS 'The last used sequence value. Next value will be current_value + 1';

-- Initialize sequence for current tenant (WHD Metaalrecycling - processor_party_id: 08797)
-- Starting at 999999 so the first generated number will be 1000000
INSERT INTO public.waste_stream_sequences (processor_party_id, current_value)
VALUES ('08797', 999999)
ON CONFLICT (processor_party_id) DO NOTHING;

-- Create a function to get the next sequence value atomically
-- This ensures thread-safety when multiple requests try to generate numbers simultaneously
CREATE OR REPLACE FUNCTION public.next_waste_stream_sequence(p_processor_party_id VARCHAR(5))
RETURNS BIGINT
LANGUAGE plpgsql
AS $$
DECLARE
    v_next_value BIGINT;
BEGIN
    -- Insert a new row if it doesn't exist (for new tenants), starting at 0
    -- Then increment and return the new value atomically
    INSERT INTO public.waste_stream_sequences (processor_party_id, current_value, updated_at)
    VALUES (p_processor_party_id, 1, NOW())
    ON CONFLICT (processor_party_id) DO UPDATE
    SET current_value = public.waste_stream_sequences.current_value + 1,
        updated_at = NOW()
    RETURNING current_value INTO v_next_value;
    
    -- Validate the sequence doesn't exceed 7 digits (max 9999999)
    IF v_next_value > 9999999 THEN
        RAISE EXCEPTION 'Maximum waste stream sequence exceeded for processor %', p_processor_party_id;
    END IF;
    
    RETURN v_next_value;
END;
$$;

COMMENT ON FUNCTION public.next_waste_stream_sequence IS 'Atomically increments and returns the next waste stream sequence number for a given processor. Thread-safe for concurrent access.';

-- Grant necessary permissions
ALTER TABLE public.waste_stream_sequences OWNER TO postgres;
GRANT ALL ON TABLE public.waste_stream_sequences TO postgres;
GRANT ALL ON TABLE public.waste_stream_sequences TO service_role;
