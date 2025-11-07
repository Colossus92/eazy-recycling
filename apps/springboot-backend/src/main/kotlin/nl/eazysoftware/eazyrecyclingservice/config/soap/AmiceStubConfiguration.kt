package nl.eazysoftware.eazyrecyclingservice.config.soap

import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding.*
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.toetsen.ToetsenAfvalstroomNummerServiceSoap
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Stub configuration for Amice SOAP services when amice.enabled=false.
 * Provides stub beans that throw exceptions when invoked, allowing the application to start
 * without requiring actual Amice configuration.
 */
@Configuration
@ConditionalOnProperty(name = ["amice.enabled"], havingValue = "false", matchIfMissing = true)
class AmiceStubConfiguration {

  private val unsupportedOperationException = UnsupportedOperationException(
  "Amice integration is disabled. Set amice.enabled=true to enable waste stream reporting."
  )

  @Bean
  fun toetsenAfvalstroomNummerServiceSoap(): ToetsenAfvalstroomNummerServiceSoap {
    return ToetsenAfvalstroomNummerServiceSoap {
      throw unsupportedOperationException
    }
  }

  @Bean
  fun meldingServiceSoap(): MeldingServiceSoap {
    return object : MeldingServiceSoap {
      override fun eersteOntvangstMelding(parameters: ArrayOfMessageEersteOntvangstMelding?): Boolean {
        throw unsupportedOperationException
      }

      override fun nulMelding(meldingsNummerMelder: String, periodeMelding: String, verwerkersnummer: String): Boolean {
        throw unsupportedOperationException
      }

      override fun afgifteMelding(meldingen: ArrayOfMessageAfgifteMelding?): Boolean {
        throw unsupportedOperationException
      }

      override fun maandelijkseOntvangstMelding(meldingen: ArrayOfMessageMaandelijkseOntvangstMelding?): Boolean {
        throw unsupportedOperationException
      }

      override fun meldingSessie(
        retourberichtViaEmail: Boolean,
        eersteOntvangstMeldingen: EersteOntvangstMeldingenDetails,
        maandelijkseOntvangstMeldingen: MaandelijkseOntvangstMeldingenDetails,
        afgifteMeldingen: AfgifteMeldingenDetails,
        nulMelding: NulMeldingDetails
      ): MeldingSessieResponseDetails {
        throw unsupportedOperationException
      }

      override fun opvragenResultaatVerwerkingMeldingSessie(meldingSessieUUID: String): OpvragenResultaatVerwerkingMeldingSessieResponseDetails {
        throw unsupportedOperationException
      }
    }
  }
}
