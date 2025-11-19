import * as postgres from 'https://deno.land/x/postgres@v0.17.0/mod.ts';

// Initialize database connection pool
const databaseUrl = Deno.env.get('SUPABASE_DB_URL') || '';
export const pool = new postgres.Pool(databaseUrl, 3, true)

/**
 * Type definition for the weight ticket data object
 */
export interface WeightTicketLine {
  waste_type_name: string;
  weight_value: number;
  weight_unit: string;
}

export interface WeightTicketData {
  weightTicket: {
    id: number;
    truck_license_plate: string;
    reclamation?: string;
    note?: string;
    status: string;
    created_at: string;
    updated_at: string;
    weighted_at?: string;
    cancellation_reason?: string;
    tarra_weight_value?: number;
    tarra_weight_unit?: string;
    second_weighing_value?: number;
    second_weighing_unit?: string;
    direction?: string;
    pdf_url?: string;
  };
  lines: WeightTicketLine[];
  consignorParty?: {
    id: string;
    name: string;
    street_name?: string;
    building_number?: string;
    postal_code?: string;
    city?: string;
    country?: string;
  };
  carrierParty?: {
    id: string;
    name: string;
    street_name?: string;
    building_number?: string;
    postal_code?: string;
    city?: string;
    country?: string;
  };
  pickupLocation?: {
    id: string;
    name?: string;
    street_name?: string;
    building_number?: string;
    postal_code?: string;
    city?: string;
  };
  deliveryLocation?: {
    id: string;
    name?: string;
    street_name?: string;
    building_number?: string;
    postal_code?: string;
    city?: string;
  };
}

/**
 * Formats a date string to DD-MM-YYYY HH:mm format
 * @param dateString The date string to format
 * @returns Formatted date string
 */
export function formatDateTime(dateString?: string): string {
  if (!dateString) return '';

  const date = new Date(dateString);
  const day = date.getDate().toString().padStart(2, '0');
  const month = (date.getMonth() + 1).toString().padStart(2, '0');
  const year = date.getFullYear();
  const hours = date.getHours().toString().padStart(2, '0');
  const minutes = date.getMinutes().toString().padStart(2, '0');

  return `${day}-${month}-${year} ${hours}:${minutes}`;
}

/**
 * Fetches comprehensive weight ticket data for PDF generation
 * @param ticketId The ID of the weight ticket
 * @returns The weight ticket data or an error response
 */
export async function fetchWeightTicketData(ticketId: string): Promise<{ data?: WeightTicketData, response?: Response }> {
  // Validate if it's a valid number
  const id = parseInt(ticketId);
  if (isNaN(id)) {
    return {
      response: new Response(JSON.stringify({ error: 'Invalid ticket ID format' }), {
        status: 400,
        headers: { 'Content-Type': 'application/json' }
      })
    };
  }

  let connection;
  try {
    connection = await pool.connect();

    // Get weight ticket details
    const ticketResult = await connection.queryObject`
      SELECT
        id,
        consignor_party_id,
        carrier_party_id,
        truck_license_plate,
        reclamation,
        note,
        status,
        created_at,
        updated_at,
        weighted_at,
        cancellation_reason,
        tarra_weight_value,
        tarra_weight_unit,
        second_weighing_value,
        second_weighing_unit,
        pickup_location_id,
        delivery_location_id,
        direction,
        pdf_url
      FROM
        weight_tickets
      WHERE
        id = ${id}
    `;

    if (ticketResult.rows.length === 0) {
      return {
        response: new Response(JSON.stringify({ error: 'Weight ticket not found' }), {
          status: 404,
          headers: { 'Content-Type': 'application/json' }
        })
      };
    }

    interface WeightTicketRow {
      id: number;
      consignor_party_id?: string;
      carrier_party_id?: string;
      truck_license_plate: string;
      reclamation?: string;
      note?: string;
      status: string;
      created_at: string;
      updated_at: string;
      weighted_at?: string;
      cancellation_reason?: string;
      tarra_weight_value?: number;
      tarra_weight_unit?: string;
      second_weighing_value?: number;
      second_weighing_unit?: string;
      pickup_location_id?: string;
      delivery_location_id?: string;
      direction?: string;
      pdf_url?: string;
    }

    const ticket = ticketResult.rows[0] as WeightTicketRow;

    // Get consignor party details if exists
    let consignorParty: WeightTicketData['consignorParty'];
    if (ticket.consignor_party_id) {
      const consignorResult = await connection.queryObject`
        SELECT
          id,
          name,
          street_name,
          building_number,
          postal_code,
          city,
          country
        FROM
          companies
        WHERE
          id = ${ticket.consignor_party_id}
      `;
      consignorParty = consignorResult.rows[0] as WeightTicketData['consignorParty'];
    }

    // Get carrier party details if exists
    let carrierParty: WeightTicketData['carrierParty'];
    if (ticket.carrier_party_id) {
      const carrierResult = await connection.queryObject`
        SELECT
          id,
          name,
          street_name,
          building_number,
          postal_code,
          city,
          country
        FROM
          companies
        WHERE
          id = ${ticket.carrier_party_id}
      `;
      carrierParty = carrierResult.rows[0] as WeightTicketData['carrierParty'];
    }

    // Get pickup location details if exists
    let pickupLocation: WeightTicketData['pickupLocation'];
    if (ticket.pickup_location_id) {
      const pickupResult = await connection.queryObject`
        SELECT
          id,
          name,
          street_name,
          building_number,
          postal_code,
          city
        FROM
          pickup_locations
        WHERE
          id = ${ticket.pickup_location_id}
      `;
      pickupLocation = pickupResult.rows[0] as WeightTicketData['pickupLocation'];
    }

    // Get delivery location details if exists
    let deliveryLocation: WeightTicketData['deliveryLocation'];
    if (ticket.delivery_location_id) {
      const deliveryResult = await connection.queryObject`
        SELECT
          id,
          name,
          street_name,
          building_number,
          postal_code,
          city
        FROM
          pickup_locations
        WHERE
          id = ${ticket.delivery_location_id}
      `;
      deliveryLocation = deliveryResult.rows[0] as WeightTicketData['deliveryLocation'];
    }

    // Get weight ticket lines with waste stream names, merged by waste type
    const linesResult = await connection.queryObject`
      SELECT
        ws.name as waste_type_name,
        SUM(wtl.weight_value) as weight_value,
        wtl.weight_unit
      FROM
        weight_ticket_lines wtl
      LEFT JOIN
        waste_streams ws ON wtl.waste_stream_number = ws.number
      WHERE
        wtl.weight_ticket_id = ${id}
      GROUP BY
        ws.name, wtl.weight_unit
      ORDER BY
        ws.name
    `;

    const lines = linesResult.rows as WeightTicketLine[];

    return {
      data: {
        weightTicket: {
          id: ticket.id,
          truck_license_plate: ticket.truck_license_plate,
          reclamation: ticket.reclamation,
          note: ticket.note,
          status: ticket.status,
          created_at: ticket.created_at,
          updated_at: ticket.updated_at,
          weighted_at: ticket.weighted_at,
          cancellation_reason: ticket.cancellation_reason,
          tarra_weight_value: ticket.tarra_weight_value,
          tarra_weight_unit: ticket.tarra_weight_unit,
          second_weighing_value: ticket.second_weighing_value,
          second_weighing_unit: ticket.second_weighing_unit,
          direction: ticket.direction,
          pdf_url: ticket.pdf_url,
        },
        lines,
        consignorParty,
        carrierParty,
        pickupLocation,
        deliveryLocation,
      }
    };
  } catch (error) {
    console.error('Database error:', error);
    return {
      response: new Response(JSON.stringify({ error: 'Database error' }), {
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
