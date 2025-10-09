workspace "Eazy Recycling" "Eazy Recycling Softwreae System"

!identifiers hierarchical

model {
  planner = person "Planner"
  admin = person "Admin"
  driver = person "Driver"
  ss = softwareSystem "Software System" {
    desktop = container "Desktop App"
    mobile = container "Mobile App"
    db = container "Supabase DB" {
      tags "Database"
    }
    supabase = container "Supabase" {
      auth = component "Supabase Auth"
      functions = component "Edge Functions"
      storage = component "Supabase Storage"
      db2 = component "Database" {
        tags "Database"
      }
    }

    api = container "Spring Boot Backend" {
      order = component Order "Order aggregate" {
        tags "Aggregate"
        properties {
          identity "OrderId (UUID)"
          invariants "one invoice per order"
        }
      }
      transport = component Transport "Transport aggregate" {
        tags "Aggregate"
        properties {
          identity "TransportId (UUID)"
          lifecycle "Planned→Dispatched→Completed"
          invariants "one waybill; one order; valid container move"
        }
      }
      weight_ticket = component WeightTicket "WeightTicket aggregate" {
        tags "Aggregate"
      }
      waybill = component Waybill "Waybill aggregate" {
        tags "Aggregate"
      }
      waste_stream = component WasteStream "Wastestream aggregate" {
        tags "Aggregate"
      }
      lma_declaration = component LMA Declaration "LMA Declaration aggregate" {
        tags "Aggregate"
      }
      invoice = component Invoice "Invoice aggregate" {
        tags "Aggregate"
      }
    }

    admin -> ss.desktop "Uses"
    planner -> ss.desktop "Uses"
    driver -> ss.mobile "Uses"

    ss.desktop -> ss.api "Queries and Commands to API"
    ss.mobile -> ss.api "Queries and Commands to API"
    ss.desktop -> ss.supabase.auth "Authenticates user using"
    ss.mobile -> ss.supabase.auth "Authenticates user using"
    ss.api -> ss.supabase.auth "Authenticates requests using"
    ss.api -> ss.supabase.db2 "Reads from and writes to"
    ss.api -> ss.supabase.functions "Offloads heavy tasks to"
    ss.supabase.functions -> ss.supabase.storage "Stores documents to"

    ss.api.invoice -> ss.api.order "depends on"

    ss.api.order -> ss.api.transport "references by Id"
    ss.api.order -> ss.api.weight_ticket "references by Id"

    ss.api.transport -> ss.api.waybill "needs WaybillId"

    ss.api.waybill -> ss.api.waste_stream "depends on"
    ss.api.weight_ticket -> ss.api.waste_stream "depends on"
    ss.api.lma_declaration -> ss.api.waste_stream "periodically reports on"
  }
}
views {
  styles {
    element "Element" {
      color #0773af
      stroke #0773af
      strokeWidth 7
      shape roundedbox
    }
    element "Person" {
      shape person
    }
    element "Boundary" {
      strokeWidth 5
    }
    relationship "Relationship" {
      thickness 4
    }
    element "Database" {
      shape cylinder
    }
    element "Aggregate" {
      background #eeffff
      shape RoundedBox
    }
  }
}
}
