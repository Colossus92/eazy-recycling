# Waste Stream Validation Implementation

## Overview
Implemented REST endpoint for validating waste stream numbers against the external Amice ToetsenAfvalstroomnummer SOAP service, following DDD/Hexagonal Architecture principles.

## Architecture

### Hexagonal Architecture Layers

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API (Inbound)                        │
│  WasteStreamController.validateWasteStreamNumber()          │
│  POST /waste-streams/{wasteStreamNumber}/validate           │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                   Application Layer                          │
│  ValidateWasteStream (Use Case)                             │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                    Domain Layer                              │
│  WasteStream (Aggregate)                                     │
│  WasteStreamValidator (Outbound Port Interface)             │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│                 Infrastructure Layer                         │
│  AmiceWasteStreamValidatorAdapter (SOAP Adapter)            │
│  SoapClientConfiguration (Spring Config)                    │
└─────────────────────────────────────────────────────────────┘
```

## Components Created

### 1. Domain Layer (Ports)
**File:** `domain/ports/out/WasteStreamValidator.kt`
- **Interface:** `WasteStreamValidator` - Outbound port for validation
- **Data Classes:**
  - `WasteStreamValidationResult` - Validation result with errors
  - `ValidationError` - Individual validation error
  - `ValidationRequestData` - Echo of request data
  - Supporting data classes for consignor, location, and company data

### 2. Infrastructure Layer (Adapters)
**File:** `adapters/out/soap/AmiceWasteStreamValidatorAdapter.kt`
- **Component:** `AmiceWasteStreamValidatorAdapter`
- **Responsibility:** Maps domain `WasteStream` to SOAP request and response back to domain
- **Key Methods:**
  - `validate()` - Main validation method
  - `mapToSoapRequest()` - Maps WasteStream to SOAP ToetsenAfvalstroomNummer
  - `mapPickupLocation()` - Handles all Location variants
  - `mapConsignor()` - Maps Consignor (Company/Person)
  - `mapToValidationResult()` - Parses SOAP response

**File:** `config/soap/SoapClientConfiguration.kt`
- **Bean:** `toetsenAfvalstroomNummerServiceSoap()`
- **Configuration:** SOAP client endpoint URL (configurable via properties)

### 3. Application Layer (Use Cases)
**File:** `application/usecase/wastestream/ValidateWasteStream.kt`
- **Service:** `ValidateWasteStream`
- **Command:** `ValidateWasteStreamCommand`
- **Responsibility:** Orchestrates validation by fetching WasteStream and calling validator

### 4. Presentation Layer (REST API)
**File:** `adapters/in/web/WasteStreamController.kt`
- **Endpoint:** `POST /waste-streams/{wasteStreamNumber}/validate`
- **Response DTOs:**
  - `WasteStreamValidationResponse`
  - `ValidationErrorResponse`
  - `ValidationRequestDataResponse`
  - `ConsignorDataResponse`
  - `PickupLocationDataResponse`
  - `CompanyDataResponse`

### 5. Build Configuration
**File:** `build.gradle.kts`
- Added WSDL2Java Gradle plugin
- Added JAXB/JAXWS dependencies
- Configured WSDL code generation from `ToetsenAfvalstroomnummerService.wsdl`
- Generated classes in package: `nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated`

### 6. WSDL Files
**File:** `src/main/resources/amice/ToetsenAfvalstroomnummerService.wsdl`
- Fixed invalid XML (removed unclosed `<br>` tags)
- Service endpoint: `https://test.lma.nl/Amice.WebService3/ToetsenAfvalstroomnummerService.asmx`

## API Usage

### Request
```http
POST /waste-streams/123456789012/validate
Authorization: Bearer <token>
```

### Response (Success)
```json
{
  "isValid": true,
  "errors": [],
  "requestData": {
    "wasteStreamNumber": "123456789012",
    "routeCollection": "Nee",
    "collectorsScheme": "Nee",
    "consignor": {
      "companyRegistrationNumber": "12345678",
      "name": "Example Company",
      "country": "Nederland",
      "isPrivate": false
    },
    "pickupLocation": {
      "postalCode": "1234AB",
      "buildingNumber": "10",
      "city": "Amsterdam",
      "streetName": "Teststraat",
      "country": "Nederland"
    },
    "deliveryLocation": "12345",
    "wasteCode": "170904",
    "wasteName": "Gemengd bouw- en sloopafval",
    "processingMethod": "R5"
  }
}
```

### Response (Validation Errors)
```json
{
  "isValid": false,
  "errors": [
    {
      "code": "E001",
      "description": "Afvalstroomnummer is niet geldig"
    }
  ],
  "requestData": { ... }
}
```

## Configuration

### Basic Configuration

Add to `application.properties` or `application.yml`:

```properties
# SOAP Service URL (defaults to test environment)
amice.toetsen-afvalstroomnummer.url=https://test.lma.nl/Amice.WebService3/ToetsenAfvalstroomnummerService.asmx
```

For production:
```properties
amice.toetsen-afvalstroomnummer.url=https://prod.lma.nl/Amice.WebService3/ToetsenAfvalstroomnummerService.asmx
```

### Client Certificate Configuration (PFX/PKCS12)

The SOAP service requires client certificate authentication. Configure as follows:

```properties
# Enable client certificate authentication
amice.certificate.enabled=true

# Path to PFX/PKCS12 certificate file
# Can be classpath: file: or absolute path
amice.certificate.path=classpath:certificates/amice-client.pfx
# Or: file:/path/to/certificate.pfx

# Certificate password
amice.certificate.password=${AMICE_CERT_PASSWORD}
```

**YAML configuration:**
```yaml
amice:
  toetsen-afvalstroomnummer:
    url: https://test.lma.nl/Amice.WebService3/ToetsenAfvalstroomnummerService.asmx
  certificate:
    enabled: true
    path: classpath:certificates/amice-client.pfx
    password: ${AMICE_CERT_PASSWORD}
```

### Certificate Setup Steps

1. **Obtain the PFX certificate** from the Amice service provider

2. **Store the certificate securely:**
   - For development: Place in `src/main/resources/certificates/` (add to `.gitignore`)
   - For production: Store outside the application (use `file:` path)

3. **Set the password as environment variable:**
   ```bash
   export AMICE_CERT_PASSWORD=your-certificate-password
   ```

4. **Verify certificate (optional):**
   ```bash
   # List certificate contents
   keytool -list -v -storetype PKCS12 -keystore amice-client.pfx
   ```

### Security Best Practices

- **Never commit certificates to version control**
- **Use environment variables for passwords**
- **Rotate certificates before expiration**
- **Use different certificates for test/production**
- **Restrict file permissions:** `chmod 600 certificate.pfx`

### Connection Timeouts

The SOAP client is configured with:
- **Connect timeout:** 30 seconds
- **Request timeout:** 60 seconds

These can be adjusted in `SoapClientConfiguration.kt` if needed.

## Domain Mapping

The adapter maps the following WasteStream properties to SOAP request:

| Domain Model | SOAP Field | Notes |
|--------------|------------|-------|
| `wasteStreamNumber.number` | `AfvalstroomNummer` | 12-digit number |
| `collectionType == ROUTE` | `RouteInzameling` | Boolean flag |
| `collectionType == COLLECTORS_SCHEME` | `InzamelaarsRegeling` | Boolean flag |
| `consignorParty` | `Ontdoener` | Company or Person |
| `pickupLocation` | `LocatieHerkomst*` | Multiple fields based on Location type |
| `deliveryLocation.processorPartyId` | `LocatieOntvangst` | 5-digit processor ID |
| `consignorParty` (if Company) | `Afzender` | Company details |
| `collectorParty` | `Inzamelaar` | Optional |
| `dealerParty` | `Handelaar` | Optional |
| `brokerParty` | `Bemiddelaar` | Optional |
| `wasteType.euralCode` | `Afvalstof` | EURAL code |
| `wasteType.name` | `GebruikelijkeNaamAfvalstof` | Waste name |
| `wasteType.processingMethod` | `VerwerkingsMethode` | Processing code |

## Location Mapping

The adapter handles all Location variants:

1. **DutchAddress** → Full postal code, street, number, city
2. **ProximityDescription** → Postal code digits, city, description
3. **Company** → Company address details
4. **ProjectLocationSnapshot** → Project location address
5. **NoLocation** → No location fields sent

## Dependencies

The implementation requires the following companies to be loaded:
- Consignor company (if not a person)
- Collector company (if specified)
- Dealer company (if specified)
- Broker company (if specified)

These are fetched via the `Companies` port to populate company details in the SOAP request.

## Error Handling

- **WasteStream not found:** Returns 400 Bad Request with error message
- **SOAP service error:** Returns validation result with SOAP_ERROR code
- **Invalid response:** Returns validation result with NO_RESULT code
- **Validation failures:** Returns isValid=false with error codes from service

## Testing

To test the endpoint:

1. Create a waste stream via `POST /waste-streams`
2. Call validation endpoint: `POST /waste-streams/{number}/validate`
3. Check response for validation status and any errors

## Future Enhancements

- Add integration tests with mocked SOAP service
- Add retry logic for transient SOAP errors
- Cache validation results to reduce external calls
- Add metrics/monitoring for SOAP service calls
- Support for the other WSDL services (MeldingService, OpvragenStatusService)
