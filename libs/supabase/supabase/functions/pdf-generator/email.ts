import { createClient } from 'npm:@supabase/supabase-js';
import { SigneeInfo } from './index.ts';
const supabase = createClient('SUPABASE_URL', 'SUPABASE_ANON_KEY')

export async function triggerEmail(signeeInfo: SigneeInfo, fileName: string) {
      try {
        await supabase.functions.invoke('waybill-email', {
          headers: { 'Content-Type': 'application/json' },
          method: 'POST',
          body: {
            email: signeeInfo.email,
            fileName: fileName
          },
        });
      } catch (error) {
        console.error('Database error:', error);
        return {
          response: new Response(JSON.stringify({ error: 'Database error', details: String(error) }), {
            status: 500,
            headers: { 'Content-Type': 'application/json' }
          })
        };
      }
}
    
