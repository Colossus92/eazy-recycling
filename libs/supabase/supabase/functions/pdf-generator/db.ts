import * as postgres from 'https://deno.land/x/postgres@v0.17.0/mod.ts'

// Initialize database connection pool
const databaseUrl = Deno.env.get('SUPABASE_DB_URL')!
export const pool = new postgres.Pool(databaseUrl, 3, true)

/**
 * Type definition for the transport data object
 */
export interface TransportData {
  transport: {
    id: string;
    display_number: string;
    transport_type: string;
    pickup_date_time: string;
    delivery_date_time: string;
    truck_id?: string;
    note?: string;
  };
  consignor: {
    id: string;
    name: string;
    street_name: string;
    building_number: string;
    postal_code: string;
    city: string;
    country: string;
    chamber_of_commerce_id: string;
    vihb_id?: string;
  };
  goods: {
    name: string;
    quantity: number;
    unit: string;
    net_net_weight: number;
    waste_stream_number: string;
    eural_code: string;
    processing_method_code: string;
    consignor_classification: number;
  };
  signatures: {
    consignor_email?: string;
    consignor_signed_at?: string;
    carrier_email?: string;
    carrier_signed_at?: string;
    consignee_email?: string;
    consignee_signed_at?: string;
    pickup_email?: string;
    pickup_signed_at?: string;
  };
  delivery_company: {
    id: string;
    name: string;
    street_name: string;
    building_number: string;
    postal_code: string;
    city: string;
    country: string;
    chamber_of_commerce_id: string;
    vihb_id?: string;
  };
  pickup_party: {
    id: string;
    name: string;
    street_name: string;
    building_number: string;
    postal_code: string;
    city: string;
    country: string;
    chamber_of_commerce_id: string;
    vihb_id?: string;
  };
  carrier_party: {
    id: string;
    name: string;
    street_name: string;
    building_number: string;
    postal_code: string;
    city: string;
    country: string;
    chamber_of_commerce_id: string;
    vihb_id?: string;
  };
  consignee: {
    id: string;
    name: string;
    street_name: string;
    building_number: string;
    postal_code: string;
    city: string;
    country: string;
    chamber_of_commerce_id: string;
    vihb_id?: string;
  };
  pickup_location: {
    street_name: string;
    building_number: string;
    postal_code: string;
    city: string;
  }
  delivery_location: {
    street_name: string;
    building_number: string;
    postal_code: string;
    city: string;
  }
}

/**
 * Validates if a string is a valid UUID
 * @param id The string to validate
 * @returns Boolean indicating if the string is a valid UUID
 */
export function isValidUUID(id: string): boolean {
  const uuidRegex = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  return uuidRegex.test(id);
}

/**
 * Fetches comprehensive transport data for PDF generation
 * @param transportId The UUID of the transport
 * @returns The transport data or an error response
 */
export async function fetchTransportData(transportId: string): Promise<{ data?: TransportData, response?: Response }> {
  // Validate if it's a valid UUID
  if (!isValidUUID(transportId)) {
    return {
      response: new Response(JSON.stringify({ error: 'Invalid format' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    };
  }
  
  let connection;
  try {
    connection = await pool.connect();
    
    // Get transport details
    const transportResult = await connection.queryObject`
      SELECT 
        t.id, 
        t.display_number, 
        t.transport_type, 
        t.pickup_date_time, 
        t.delivery_date_time, 
        t.note,
        t.goods_id,
        t.truck_id
      FROM 
        transports t 
      WHERE 
        t.id = ${transportId}
    `;
    
    if (transportResult.rows.length === 0) {
      return {
        response: new Response(JSON.stringify({ error: 'Transport not found' }), {
          status: 404,
          headers: { 'Content-Type': 'application/json' }
        })
      };
    }
    
    // Define type for transport result
    interface TransportRow {
      id: string;
      display_number: string;
      transport_type: string;
      pickup_date_time: string;
      delivery_date_time: string;
      note?: string;
      goods_id: string;
      truck_id: string;
    }
    
    const transport = transportResult.rows[0] as TransportRow;
    const goodsId = transport.goods_id;
    
    // Get consignor details
    const consignorResult = await connection.queryObject`
      SELECT 
        c.id,
        c.name,
        c.street_name,
        c.building_number,
        c.postal_code,
        c.city,
        c.country,
        c.chamber_of_commerce_id,
        c.vihb_id
      FROM 
        transports t
      JOIN 
        companies c ON t.consignor_party_id = c.id
      WHERE 
        t.id = ${transportId}
    `;
    
    const goodsResult = await connection.queryObject`
      SELECT 
        g.id,
        g.consignor_classification,
        gi.eural_code,
        gi.name,
        gi.quantity,
        gi.unit,
        gi.net_net_weight,
        gi.waste_stream_number,
        gi.processing_method_code
      FROM 
        goods g
      JOIN
        goods_items gi ON g.goods_item_id = gi.id
      WHERE 
        g.uuid = ${goodsId}
    `;
    
    const deliveryCompanyResult = await connection.queryObject`
      SELECT 
        c.id,
        c.name,
        c.street_name,
        c.building_number,
        c.postal_code,
        c.city,
        c.country,
        c.chamber_of_commerce_id,
        c.vihb_id
      FROM 
        transports t
      JOIN 
        companies c ON t.delivery_company_id = c.id
      WHERE 
        t.id = ${transportId}
    `;
    
    const carrierPartyResult = await connection.queryObject`
      SELECT 
        c.id,
        c.name,
        c.street_name,
        c.building_number,
        c.postal_code,
        c.city,
        c.country,
        c.chamber_of_commerce_id,
        c.vihb_id
      FROM 
        transports t
      JOIN 
        companies c ON t.carrier_party_id = c.id
      WHERE 
        t.id = ${transportId}
    `;
    
    const signaturesResult = await connection.queryObject`
      SELECT 
        consignor_email,
        consignor_signed_at,
        carrier_email,
        carrier_signed_at,
        consignee_email,
        consignee_signed_at,
        pickup_email,
        pickup_signed_at
      FROM 
        signatures
      WHERE 
        transport_id = ${transportId}
    `;

    const pickupPartyResult = await connection.queryObject`
      SELECT 
        c.id,
        c.name,
        c.street_name,
        c.building_number,
        c.postal_code,
        c.city,
        c.country,
        c.chamber_of_commerce_id,
        c.vihb_id
      FROM 
        transports t
      JOIN 
        goods g ON t.goods_id = g.uuid
      JOIN
        companies c ON g.pickup_party_id = c.id
      WHERE 
        t.id = ${transportId}
    `;
    const consigneeResult = await connection.queryObject`
      SELECT 
        c.id,
        c.name,
        c.street_name,
        c.building_number,
        c.postal_code,
        c.city,
        c.country,
        c.chamber_of_commerce_id,
        c.vihb_id
      FROM 
        transports t
      JOIN 
        goods g ON t.goods_id = g.uuid
      JOIN
        companies c ON g.consignee_party_id = c.id
      WHERE 
        t.id = ${transportId}
    `;
    const pickupLocationResult = await connection.queryObject`
      SELECT 
        l.street_name,
        l.building_number,
        l.postal_code,
        l.city
      FROM 
        transports t
      JOIN 
        locations l ON t.pickup_location_id = l.id
      WHERE 
        t.id = ${transportId}
    `; 
    const deliveryLocationResult = await connection.queryObject`
      SELECT 
        l.street_name,
        l.building_number,
        l.postal_code,
        l.city
      FROM 
        transports t
      JOIN 
        locations l ON t.delivery_location_id = l.id
      WHERE 
        t.id = ${transportId}
    `;    
    const transportData: TransportData = {
      transport: {
        id: String(transport.id),
        display_number: String(transport.display_number),
        transport_type: String(transport.transport_type),
        pickup_date_time: String(transport.pickup_date_time),
        delivery_date_time: String(transport.delivery_date_time),
        note: transport.note ? String(transport.note) : undefined,
        truck_id: transport.truck_id ? String(transport.truck_id) : undefined,
      },
      consignor: consignorResult.rows[0] as TransportData['consignor'],
      goods: goodsResult.rows[0] as TransportData['goods'],
      signatures: signaturesResult.rows[0] as TransportData['signatures'],
      delivery_company: deliveryCompanyResult.rows[0] as TransportData['delivery_company'],
      pickup_party: pickupPartyResult.rows[0] as TransportData['pickup_party'],
      carrier_party: carrierPartyResult.rows[0] as TransportData['carrier_party'],
      consignee: consigneeResult.rows[0] as TransportData['consignee'],
      pickup_location: pickupLocationResult.rows[0] as TransportData['pickup_location'],
      delivery_location: deliveryLocationResult.rows[0] as TransportData['delivery_location']
    };
    
    return { data: transportData };
  } catch (err) {
    console.error('Database error:', err);
    return {
      response: new Response(JSON.stringify({ error: 'Database error', details: String(err) }), {
        status: 500,
        headers: { 'Content-Type': 'application/json' }
      })
    };
  } finally {
    if (connection) connection.release();
  }
}
