# Remove EBA Schema-API Support for Legacy Software Integration

## Context and Problem Statement

The EBA (Elektronische Begeleidingsbrief Afval) standard is a Dutch electronic waste accompanying document system that standardizes the digital exchange of waste transport documentation. The EBA standard defines XML schemas and APIs for creating, validating, and exchanging begeleidingsbrieven (waste accompanying documents) between waste management companies, transporters, and regulatory authorities in the Netherlands.

Initially, our eazy-recycling application was planned to integrate with legacy waste management software through the EBA schema-API. This integration would have enabled seamless migration of existing waste transport data and provided interoperability with established systems in the Dutch waste management ecosystem.

However, we encountered significant challenges in implementing this integration due to insufficient support from the company maintaining the API connection in the legacy software. Without proper cooperation and technical support from the legacy system maintainers, the planned API integration became unfeasible within our project timeline and budget constraints.

## Considered Options

* **Continue with EBA schema-API integration** - Persist with the original plan despite limited support
* **Remove EBA schema-API support entirely** - Completely eliminate all EBA-related code and dependencies
* **Remove API integration but retain EBA schema library** - Disable API functionality while preserving the schema for future use

## Decision Outcome

Chosen option: "Remove API integration but retain EBA schema library", because it provides the best balance between addressing current constraints while maintaining future flexibility.

This decision allows us to:
- Eliminate the immediate technical debt and complexity of unsupported API integration
- Preserve the investment made in EBA schema understanding and implementation
- Keep the door open for future integrations with other systems that may require EBA standard compliance
- Maintain compliance readiness for potential regulatory requirements

## Pros and Cons of the Options

### Continue with EBA schema-API integration

**Pros:**
- Maintains original migration strategy
- Provides immediate interoperability with legacy systems
- Leverages existing EBA standard adoption in the industry

**Cons:**
- Requires significant additional development time without proper support
- High risk of implementation failures due to insufficient documentation
- Potential for ongoing maintenance issues with unsupported integration points
- May delay overall project delivery

### Remove EBA schema-API support entirely

**Pros:**
- Simplifies codebase and reduces complexity
- Eliminates dependency on external unsupported systems
- Faster development cycle without integration constraints

**Cons:**
- Loses all investment in EBA standard implementation
- Eliminates future integration possibilities
- May require complete reimplementation if EBA compliance becomes necessary
- Reduces interoperability with Dutch waste management ecosystem

### Remove API integration but retain EBA schema library

**Pros:**
- Eliminates current technical risks while preserving future options
- Maintains EBA standard knowledge and schema definitions
- Allows for future integrations with better-supported systems
- Keeps compliance capabilities for regulatory requirements
- Reduces immediate complexity while maintaining strategic flexibility

**Cons:**
- Maintains some unused code in the current implementation
- Requires ongoing maintenance of unused schema library
- May create confusion about current system capabilities

## More Information

The EBA schema library remains in the `libs/eba-schema/` directory as a shared library within our NX workspace. This preserves our implementation of the EBA standard XML schemas and related utilities, which may be valuable for:

- Future integrations with other waste management systems
- Compliance with Dutch waste transport regulations
- Export/import functionality with EBA-compliant systems
- Potential API integrations when better support becomes available

The decision to retain the library follows our NX workspace pattern of maintaining shared libraries for cross-cutting concerns, even when not immediately utilized by all applications.
