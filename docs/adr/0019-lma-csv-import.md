# LMA CSV Import for Waste Stream Data

## Context and Problem Statement

Users who are already registered with the LMA (Landelijk Meldpunt Afvalstoffen) portal have existing waste stream numbers that they want to continue using in our system. Currently, users must manually re-enter all waste stream data, which is time-consuming and error-prone.

The LMA portal allows exporting waste stream data in Excel format. We want to provide a way for users to import this data into our system, allowing them to utilize their existing waste stream numbers.

## Considered Options

* **Option 1**: Direct Excel (.xlsx) file upload
* **Option 2**: CSV file upload (converted from Excel by user)
* **Option 3**: API integration with LMA portal

## Decision Outcome

Chosen option: **Option 2 - CSV file upload**, because:

* Simpler implementation without Excel parsing dependencies
* CSV is a universal format that's easy to validate and process
* The manual conversion step (xlsx → csv) is minimal effort for users
* Apache Commons CSV library provides robust parsing capabilities
* Reduces complexity compared to handling Excel formats directly

## Implementation Details

### Frontend Changes

A new tab "LMA Import" will be added to the Settings page (`SettingsPage.tsx`) with:
1. **File upload component** - Accepts CSV files
2. **Import status display** - Shows progress and results
3. **Error list** - Displays import errors that require attention

### Backend Changes

#### New Controller: `LmaImportController`

* `POST /admin/lma/import` - Upload and process CSV file
* `GET /admin/lma/import/errors` - Retrieve import errors

#### New Domain/Application Layer

* `LmaImportService` - Orchestrates the import process
* `LmaCsvParser` - Parses CSV using Apache Commons CSV
* `LmaImportError` entity - Stores import errors for review

#### New Database Table: `lma_import_error`

```sql
CREATE TABLE lma_import_error (
    id UUID PRIMARY KEY,
    import_batch_id UUID NOT NULL,
    row_number INT NOT NULL,
    waste_stream_number VARCHAR(15),
    error_code VARCHAR(50) NOT NULL,
    error_message TEXT NOT NULL,
    raw_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    resolved_at TIMESTAMP WITH TIME ZONE,
    resolved_by VARCHAR(255)
);
```

### CSV Field Mapping

The following mapping will be applied when processing the LMA export CSV:

| CSV Column | Target Field | Notes |
|------------|--------------|-------|
| Afvalstroomnummer | wasteStreamNumber.number | Direct mapping |
| Verwerkersnummer Locatie van Bestemming (LvB) | WasteDeliveryLocation.processorPartyId | Direct mapping |
| LvB KvK nummer | - | Skip, resolved by processorPartyId |
| LvB Bedrijfsnaam | - | Skip, resolved by processorPartyId |
| LvB Adres | - | Skip, resolved by processorPartyId |
| LvB Postcode/Plaats | - | Skip, resolved by processorPartyId |
| Handelsregisternummer Ontdoener | pickupParty.uuid | Find Party by KvK number |
| Naam Ontdoener | - | Skip, resolved by KvK number |
| Land Ontdoener | - | Skip, resolved by KvK number |
| LocatieHerkomst Straatnaam | pickupLocation.address.streetName | If no Nabijheidsbeschrijving |
| LocatieHerkomst Huisnummer | pickupLocation.address.buildingNumber | If no Nabijheidsbeschrijving |
| LocatieHerkomst HuisnummerToevoeging | pickupLocation.address.buildingNumberAddition | If no Nabijheidsbeschrijving |
| LocatieHerkomst Postcode | pickupLocation.address.postalCode / postCodeDigits | Depends on location type |
| LocatieHerkomst Plaats | pickupLocation.address.city / city | Depends on location type |
| LocatieHerkomst Nabijheidsbeschrijving | pickupLocation.description | If present, use ProximityDescription |
| LocatieHerkomst Land | pickupLocation.country | Country field |
| Euralcode | wasteType.euralCode.code | Format: add spaces after every 2 digits (170405 → 17 04 05), asterisk not preceded by space |
| Euralcode Omschrijving | - | Skip, resolved by Euralcode |
| Gebruikelijke Naam Afvalstof | wasteType.name | Direct mapping |
| VerwerkingsMethode Code | wasteType.processingMethod.code | Format: add dot after letter (A01 → A.01) |
| VerwerkingsMethode Omschrijving | - | Skip, resolved by code |
| Routeinzameling | collectionType | If "J" → WasteCollectionType.ROUTE |
| Inzamelaarsregeling | collectionType | If "J" → WasteCollectionType.COLLECTORS_SCHEME |
| Particuliere Ontdoener | - | Skip rows with "J" (not supported yet) |

### Location Type Resolution Logic

```sql
IF Nabijheidsbeschrijving is not empty:
    Use ProximityDescription:
        - description = Nabijheidsbeschrijving
        - postCodeDigits = Postcode
        - city = Plaats
        - country = Land
ELSE:
    Use DutchAddress:
        - streetName = Straatnaam
        - buildingNumber = Huisnummer
        - buildingNumberAddition = HuisnummerToevoeging
        - postalCode = Postcode
        - city = Plaats
```

### Collection Type Resolution Logic

```sql
IF Routeinzameling == "J":
    collectionType = ROUTE
ELSE IF Inzamelaarsregeling == "J":
    collectionType = COLLECTORS_SCHEME
ELSE:
    collectionType = DEFAULT
```

### Error Handling

Import errors will be stored and displayed to users. Common error scenarios:

* **COMPANY_NOT_FOUND**: KvK number doesn't match any company in the system
* **INVALID_EURAL_CODE**: Eural code format is invalid
* **INVALID_PROCESSING_METHOD**: Processing method code is invalid
* **DUPLICATE_WASTE_STREAM**: Waste stream number already exists
* **MISSING_REQUIRED_FIELD**: Required field is empty
* **INVALID_CSV_FORMAT**: CSV structure doesn't match expected format

### Private consignor

Private consignors are not supported yet, because our first priority is to support companies. Private customers will be supported at a later stage. Because of this, we will just skip the private consignor entries in the CSV file.

### Technology

* **CSV Parsing**: Apache Commons CSV library
* **Batch Processing**: Process rows in batches to handle large files
* **Transaction Management**: Each waste stream import is atomic; failures don't roll back successful imports

## Pros and Cons of the Options

### Option 1: Direct Excel Upload

* **Pros**: No manual conversion step for users
* **Cons**: Requires Apache POI or similar library, more complex parsing, larger dependency

### Option 2: CSV Upload (Chosen)

* **Pros**: Simple parsing, universal format, Apache Commons CSV is lightweight
* **Cons**: Requires manual xlsx → csv conversion by user

### Option 3: API Integration

* **Pros**: Fully automated, real-time sync possible
* **Cons**: LMA may not provide public API, complex authentication, maintenance burden

## More Information

* LMA Portal: <https://www.lma.nl/>
* Apache Commons CSV: <https://commons.apache.org/proper/commons-csv/>
* Similar pattern exists for Exact Online sync conflicts in `ExactOnlineController.kt`
