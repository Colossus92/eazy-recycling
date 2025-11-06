package nl.eazysoftware.eazyrecyclingservice.config.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.ToetsenAfvalstroomNummer
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.ToetsenAfvalstroomNummerResponse
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.ToetsenAfvalstroomNummerServiceSoap
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Stub configuration for Amice SOAP service when amice.enabled=false.
 * Provides a stub bean that throws exceptions when invoked, allowing the application to start
 * without requiring actual Amice configuration.
 */
@Configuration
@ConditionalOnProperty(name = ["amice.enabled"], havingValue = "false", matchIfMissing = true)
class AmiceStubConfiguration {

  @Bean
  fun toetsenAfvalstroomNummerServiceSoap(): ToetsenAfvalstroomNummerServiceSoap {
    return object : ToetsenAfvalstroomNummerServiceSoap {
      override fun toetsenAfvalstroomNummer(parameters: ToetsenAfvalstroomNummer?): ToetsenAfvalstroomNummerResponse {
        throw UnsupportedOperationException(
          "Amice integration is disabled. Set amice.enabled=true to enable waste stream validation."
        )
      }
    }
  }
}
