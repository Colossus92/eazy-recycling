package nl.eazysoftware.eazyrecyclingservice.config.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MeldingSessie
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.MeldingSessieResponse
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.OpvragenResultaatVerwerkingMeldingSessie
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.OpvragenResultaatVerwerkingMeldingSessieResponse
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.toetsen.ToetsenAfvalstroomNummer
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.toetsen.ToetsenAfvalstroomNummerResponse
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Stub configuration for Amice SOAP client interfaces when amice.enabled=false.
 * Provides stub beans that throw exceptions when invoked, allowing the application to start
 * without requiring actual Amice configuration or SSL certificates.
 */
@Configuration
@ConditionalOnProperty(name = ["amice.enabled"], havingValue = "false", matchIfMissing = true)
class AmiceStubConfiguration {

  private val unsupportedOperationException = UnsupportedOperationException(
    "Amice integration is disabled. Set amice.enabled=true to enable waste stream reporting."
  )

  @Bean
  fun toetsenAfvalstroomNummerClient(): ToetsenAfvalstroomNummerClient {
    return object : ToetsenAfvalstroomNummerClient {
      override fun validate(body: ToetsenAfvalstroomNummer): ToetsenAfvalstroomNummerResponse {
        throw unsupportedOperationException
      }
    }
  }

  @Bean
  fun meldingServiceClient(): MeldingServiceClient {
    return object : MeldingServiceClient {
      override fun apply(body: MeldingSessie): MeldingSessieResponse {
        throw unsupportedOperationException
      }

      override fun requestStatus(body: OpvragenResultaatVerwerkingMeldingSessie): OpvragenResultaatVerwerkingMeldingSessieResponse {
        throw unsupportedOperationException
      }
    }
  }
}
