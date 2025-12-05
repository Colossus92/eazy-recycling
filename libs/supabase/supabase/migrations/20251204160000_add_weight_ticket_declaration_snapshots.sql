-- Migration: Add weight_ticket_declaration_snapshots table
-- Purpose: Track declared state of weight ticket lines for late and corrective declarations
-- ADR: 0014-late-and-corrective-waste-declarations.md

create sequence if not exists weight_ticket_declaration_snapshots_id_seq start with 1 increment by 1;

create table if not exists weight_ticket_declaration_snapshots (
    id bigint not null default nextval('weight_ticket_declaration_snapshots_id_seq'),
    weight_ticket_id bigint not null,
    weight_ticket_line_index int not null,
    waste_stream_number text not null,
    declared_weight_value numeric not null,
    declaration_id text not null,
    declared_at timestamp with time zone not null default now(),
    declaration_period text not null,
    primary key (id),
    constraint fk_declaration_snapshots_weight_ticket foreign key (weight_ticket_id) references weight_tickets(id),
    constraint fk_declaration_snapshots_declaration foreign key (declaration_id) references lma_declarations(id)
);

create index if not exists idx_declaration_snapshots_weight_ticket on weight_ticket_declaration_snapshots(weight_ticket_id);
create index if not exists idx_declaration_snapshots_waste_stream on weight_ticket_declaration_snapshots(waste_stream_number);
create index if not exists idx_declaration_snapshots_period on weight_ticket_declaration_snapshots(declaration_period);

comment on table weight_ticket_declaration_snapshots is 'Stores snapshots of weight ticket lines at declaration time for tracking and corrective declarations';
comment on column weight_ticket_declaration_snapshots.weight_ticket_line_index is 'Index of the line within the weight ticket (0-based)';
comment on column weight_ticket_declaration_snapshots.declaration_period is 'Period in MMYYYY format (e.g., 112025 for November 2025)';
