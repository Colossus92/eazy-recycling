import { pool } from "./db.ts";
import { SigneeInfo } from "./index.ts";

export async function sendToQueue(signeeInfo: SigneeInfo, fileName: string) {
    const email = signeeInfo.email;
    let connection;
      try {
        connection = await pool.connect();
        
        // Create a JSON message object
        const message = JSON.stringify({
          email: email,
          fileName: fileName
        });
        
        // Use proper parameter binding with $1 placeholder
        await connection.queryObject`
          select * from pgmq.send(
            queue_name => 'waybill-email',
            msg => ${message}
          );
        `;
      } catch (error) {
        console.error('Database error:', error);
        return {
          response: new Response(JSON.stringify({ error: 'Database error', details: String(error) }), {
            status: 500,
            headers: { 'Content-Type': 'application/json' }
          })
        };
      } finally {
        if (connection) {
          connection.release();
        }
      }
}
    
