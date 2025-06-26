import { createClient } from "npm:@supabase/supabase-js@2";

/**
 * Initialize Supabase client with service role for privileged operations
 */
export const supabase = createClient(
    Deno.env.get('SUPABASE_URL') ?? '',
    Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') ?? '',
);

export default supabase;