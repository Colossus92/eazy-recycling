# Accept Denormalization Between TransportDto and WasteStreamDto for Historical Accuracy

## Context and Problem Statement

We are implementing DDD/hexagonal architecture with separate aggregates for `WasteTransport` and `WasteStream`. The `WasteTransport` aggregate references a `WasteStreamNumber` to link to the `WasteStream` aggregate, which contains waste regulatory information.

At the persistence layer, we have:
- `TransportDto` - Stores transport execution details (driver, truck, dates, locations, hours)
- `WasteStreamDto` - Stores waste regulatory information (waste type, parties, locations, processing methods)

**The Problem**: Several fields overlap between these two entities:
- `consignorParty` (the party ordering the transport / waste producer)
- `pickupLocation` (origin of the waste)
- `deliveryLocation` / `processorParty` (destination for waste processing)

We need to decide whether to:
1. Store these fields only in `WasteStreamDto` and always join (no duplication)
2. Store them in both entities (intentional denormalization)
3. Create separate transport tables for container vs. waste transports

The choice impacts:
- Data consistency vs. historical accuracy
- Query complexity (especially for the `PlanningController` which needs transport details across both types)
- Domain model alignment
- Flexibility for real-world logistics scenarios

## Considered Options

* **Pure Reference: Store only in WasteStreamDto** - Minimal duplication, single source of truth
* **Intentional Denormalization: Store in both** - Accept duplication for historical accuracy and flexibility
* **Separate Tables: WasteTransportDto and ContainerTransportDto** - Complete separation at persistence layer

## Decision Outcome

Chosen option: **Intentional Denormalization - Store in both `TransportDto` and `WasteStreamDto`**.

**Rationale:**
1. **Different Lifecycles**: `WasteStream` is mutable master data that can be edited or soft-deleted, while `TransportDto` is an immutable historical record of what actually occurred
2. **Regulatory Audit Trail**: Transport records must maintain accurate historical snapshots for compliance and invoicing, even if the waste stream configuration changes later
3. **Real-World Flexibility**: Logistics operations need to handle exceptions (e.g., alternate pickup locations, emergency processor changes) without breaking the waste stream configuration
4. **Query Performance**: The `PlanningController` needs fast access to location data for both container and waste transports without complex polymorphic joins
5. **Domain Alignment**: Matches DDD pattern where `WasteTransport` captures a point-in-time snapshot when the transport was created

## Pros and Cons of the Options

### Pure Reference: Store only in WasteStreamDto

**Pros:**
- Absolute single source of truth
- No data duplication
- Changes to waste stream automatically reflect in all transports

**Cons:**
- Historical inaccuracy: if waste stream changes, old transport records show wrong data
- No flexibility for transport-specific deviations from the standard flow
- Complex queries: planning views must always join to `WasteStreamDto`
- Tight coupling: cannot delete/archive waste stream without affecting transport history
- Fails audit requirements: transport records must reflect actual event, not current configuration

### Intentional Denormalization: Store in both (chosen)

**Pros:**
- **Historical Accuracy**: Transport records are immutable snapshots of what happened
- **Audit Compliance**: Regulatory requirements met with accurate point-in-time data
- **Operational Flexibility**: Allows exceptions (alternate pickup, emergency delivery) without corrupting master data
- **Query Simplicity**: Planning queries remain fast and simple for both transport types
- **Lifecycle Independence**: Can archive/soft-delete waste streams without affecting historical transports
- **DDD Alignment**: Transport aggregate is self-contained with its own state

**Cons:**
- Data duplication for these three fields
- Requires validation logic in use cases to ensure consistency at creation time
- Slightly more storage (minimal impact for ~3 fields)

### Separate Tables: WasteTransportDto and ContainerTransportDto

**Pros:**
- Clean separation of concerns
- No nullable fields for type-specific data

**Cons:**
- Complex union queries for planning view
- Duplicates all common transport fields (driver, truck, dates, status, hours)
- Harder to maintain shared transport behavior
- More complex repository layer with polymorphic handling
- Still need to solve the denormalization question for waste transports

## Implementation Pattern

### Database Schema

```kotlin
@Entity
@Table(name = "transports")
data class TransportDto(
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  val id: UUID? = null,
  
  // Stored for ALL transports (container and waste)
  @ManyToOne
  @JoinColumn(name = "consignor_party_id")
  val consignorParty: CompanyDto,
  
  @ManyToOne
  @JoinColumn(name = "carrier_party_id")
  val carrierParty: CompanyDto,
  
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pickup_location_id")
  val pickupLocation: PickupLocationDto,
  
  @OneToOne
  @JoinColumn(name = "delivery_location_id")
  val deliveryLocation: PickupLocationDto,
  
  // Reference to waste stream (nullable - only for waste transports)
  @ManyToOne
  @JoinColumn(name = "waste_stream_number", referencedColumnName = "number")
  val wasteStream: WasteStreamDto? = null,
  
  // Common execution fields
  val pickupDateTime: LocalDateTime,
  val deliveryDateTime: LocalDateTime?,
  val truck: Truck?,
  val driver: ProfileDto?,
  val transportHours: Double?,
  val sequenceNumber: Int,
  // ... other fields
)
```

### Use Case Validation

Enforce consistency at transport creation time:

```kotlin
@Service
class CreateWasteTransportService(
  private val wasteTransports: WasteTransports,
  private val wasteStreams: WasteStreams
) : CreateWasteTransport {
  
  @Transactional
  override fun handle(cmd: CreateWasteTransportCommand): CreateWasteTransportResult {
    // Fetch waste stream for reference and validation
    val wasteStream = wasteStreams.findByNumber(cmd.wasteStreamNumber)
      ?: throw IllegalArgumentException("Afvalstroom niet gevonden")
    
    // Validate or warn if transport deviates from waste stream configuration
    // (Optional: log deviations for audit purposes)
    
    // Create transport with snapshot of current waste stream data
    val wasteTransport = WasteTransport(
      carrierParty = cmd.carrierParty,
      pickupDateTime = cmd.pickupDateTime,
      wasteStreamNumber = cmd.wasteStreamNumber,
      // ... other fields
    )
    
    return wasteTransports.save(wasteTransport)
  }
}
```

### Mapper Pattern

The mapper captures the point-in-time snapshot from the domain model:

```kotlin
@Component
class WasteTransportMapper(
  private val wasteStreamRepository: WasteStreamRepository,
  private val entityManager: EntityManager
) {
  fun toDto(domain: WasteTransport): TransportDto {
    // Fetch current waste stream to get location/party references
    val wasteStreamDto = wasteStreamRepository.findById(domain.wasteStreamNumber.number)
      ?: throw IllegalStateException("Waste stream not found")
    
    return TransportDto(
      id = domain.transportId?.uuid,
      consignorParty = wasteStreamDto.consignorParty,
      pickupLocation = wasteStreamDto.pickupLocation,
      deliveryLocation = wasteStreamDto.processorParty.deliveryLocation,
      wasteStream = wasteStreamDto, // Keep reference for regulatory queries
      carrierParty = entityManager.getReference(CompanyDto::class.java, domain.carrierParty.uuid),
      pickupDateTime = domain.pickupDateTime.toLocalDateTime(),
      // ... map other fields
    )
  }
}
```

## Alignment with Existing Patterns

This decision aligns with **ADR-0008 (Soft Deletes with Snapshots)**:
- Similar rationale: preserve historical truth when master data changes
- Waste streams are master data that can be soft-deleted
- Transport records are documents that must maintain immutable snapshots
- Both use the pattern: "master data + document with snapshot"

## Consequences

### Positive
- Transport records remain accurate historical documents
- Planning queries stay performant and simple
- Support for logistics flexibility (exceptions, deviations)
- Clean DDD aggregate boundaries with proper lifecycle management
- Audit compliance maintained

### Negative
- Must maintain consistency validation in use cases
- Requires developer discipline to understand which data is snapshot vs. reference
- Minimal storage overhead (~3 fields duplicated per waste transport)

### Neutral
- Adds `wasteStream` foreign key reference to `TransportDto` for regulatory queries
- Need to document the snapshot pattern for new developers

## Migration Guidance

1. **Add `waste_stream_number` column** to `transports` table as nullable foreign key
2. **Keep existing fields** (`consignor_party_id`, `pickup_location_id`, `delivery_location_id`) for all transports
3. **Update `WasteTransportMapper`** to populate both the snapshot fields and the waste stream reference
4. **Add validation** in `CreateWasteTransportService` to ensure consistency at creation time
5. **Document pattern** in developer onboarding materials: "TransportDto stores execution snapshot, WasteStreamDto stores current configuration"

## More Information

- **Related ADR**: [ADR-0008: Soft Deletion Strategy with Snapshots](./0008-soft-deletes.md)
- **Domain Model**: `WasteTransport` aggregate in `/domain/model/transport/WasteTransport.kt`
- **Use Case**: `CreateWasteTransportService` in `/application/usecase/transport/CreateWasteTransport.kt`
- **Regulatory Context**: Dutch waste transport regulations require immutable transport records for VIHB (Vervoersbewijzen Afvalstoffen) compliance
