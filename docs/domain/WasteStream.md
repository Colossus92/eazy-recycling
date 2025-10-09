# WasteStream

Purpose: have a valid waste stream that legally allows for transporting waste. 

## Identity: 
Number (afvalstroomnummer in Dutch), unique number of 12 positions where the first 5 indicate the processing party number, the next 7 are for unique identification.

### State (snapshot):
- number (id)
- isRouteCollection
- isCollectorsScheme

### Commands:
- \<CreateWasteStreamCommand\>(args) → WasteStreamCreated | WasteStreamInvalid
- \<ValidateStreamCommand\>(args) → WasteStreamValidated | WasteStreamInvalid

### Events:
- \<WasteStreamCreated\>(payload)
- \<WasteStreamValidated\>(payload)

### Invariants (must always hold):
- [Code] Cannot be a Route Collection (RouteInzameling) and CollectorsScheme (InzamelaarsRegeling) at the same time
- [Code] A wastestream can't have both a broker and collector assigned to it
- [Code] Has to be valid against LMA XSD scheme
- [Process] when created the WasteStream should be validated at Amice
- [Process] when validated the WasteStream should be expired after 5 years

### State transitions
- Draft → Valid
  Command: ValidateWasteStream
- Valid → Expired
  
### Corrective Policy
- When a waste stream is invalid, an admin should correct the waste stream and validate it again. 
