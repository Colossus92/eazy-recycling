-- Create wrapper functions in public schema for pgcrypto and uuid-ossp functions
-- This allows seed.sql to use these functions without schema qualification

CREATE OR REPLACE FUNCTION public.gen_salt(type text)
RETURNS text AS $$
  SELECT extensions.gen_salt($1);
$$ LANGUAGE sql IMMUTABLE STRICT;

CREATE OR REPLACE FUNCTION public.crypt(password text, salt text)
RETURNS text AS $$
  SELECT extensions.crypt($1, $2);
$$ LANGUAGE sql IMMUTABLE STRICT;

CREATE OR REPLACE FUNCTION public.gen_random_uuid()
RETURNS uuid AS $$
  SELECT extensions.gen_random_uuid();
$$ LANGUAGE sql IMMUTABLE STRICT;
