import { SigneeInfo } from './index.ts';
import { supabase } from './client.ts';

export async function triggerEmail(signeeInfo: SigneeInfo, fileName: string) {
      try {
        await supabase.functions.invoke('waybill-email', {
          body: { email: signeeInfo.email, fileName: fileName }
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
    
