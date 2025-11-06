package nl.eazysoftware.eazyrecyclingservice.config.soap

import jakarta.xml.ws.BindingProvider
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.ToetsenAfvalstroomNummerService
import nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.ToetsenAfvalstroomNummerServiceSoap
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource
import java.net.Authenticator
import java.net.PasswordAuthentication
import java.net.URI
import java.security.KeyStore
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

/**
 * Configuration for SOAP clients used to communicate with external Amice services.
 * Configures both client certificate authentication (PFX/PKCS12) and HTTP Basic Authentication.
 *
 * Only active when amice.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = ["amice.enabled"], havingValue = "true", matchIfMissing = false)
class SoapClientConfiguration {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Value("\${amice.url:}")
  private lateinit var amiceBaseUrl: String

  @Value("\${amice.username:}")
  private lateinit var username: String

  @Value("\${amice.password:}")
  private lateinit var password: String

  @Value("\${amice.certificate.path:}")
  private lateinit var certificatePath: Resource

  @Value("\${amice.certificate.password:}")
  private lateinit var certificatePassword: String

  @Value("\${amice.certificate.enabled:false}")
  private var certificateEnabled: Boolean = false


  @Bean("amiceToetsenUrl")
  fun amiceToetsenUrl(): String? {
    return if (amiceBaseUrl.isNotBlank()) {
      "$amiceBaseUrl/ToetsenAfvalstroomnummerService.asmx"
    } else {
      null
    }
  }

  @Bean
  fun toetsenAfvalstroomNummerServiceSoap(@Value("#{@amiceToetsenUrl}") serviceUrl: String?): ToetsenAfvalstroomNummerServiceSoap? {
    if (serviceUrl.isNullOrBlank()) {
      logger.warn("Amice SOAP client not initialized: amice.url is not configured")
      return null
    }

    logger.info("Initializing Amice SOAP client for URL: $serviceUrl")

    // Configure SSL context with client certificate if enabled
    if (certificateEnabled) {
      configureSslContext()
    }

    // Configure HTTP Basic Authentication
    if (username.isNotBlank() && password.isNotBlank()) {
      configureBasicAuthentication()
    }

    val service = ToetsenAfvalstroomNummerService(
      URI(serviceUrl).toURL()
    )

    val port = service.toetsenAfvalstroomNummerServiceSoap

    // Configure the binding provider
    val bindingProvider = port as BindingProvider
    val requestContext = bindingProvider.requestContext

    // Set endpoint URL
    requestContext[BindingProvider.ENDPOINT_ADDRESS_PROPERTY] = serviceUrl

    // Set HTTP Basic Authentication credentials
    if (username.isNotBlank() && password.isNotBlank()) {
      requestContext[BindingProvider.USERNAME_PROPERTY] = username
      requestContext[BindingProvider.PASSWORD_PROPERTY] = password
      logger.info("Configured HTTP Basic Authentication for user: $username")
    }

    // Configure timeouts
    requestContext["com.sun.xml.ws.connect.timeout"] = 30000 // 30 seconds
    requestContext["com.sun.xml.ws.request.timeout"] = 60000 // 60 seconds

    logger.info("SOAP client initialized successfully")
    return port
  }

  private fun configureBasicAuthentication() {
    logger.info("Configuring HTTP Basic Authentication for user: $username")

    // Set default authenticator for WSDL retrieval during service initialization
    Authenticator.setDefault(object : Authenticator() {
      override fun getPasswordAuthentication(): PasswordAuthentication {
        return PasswordAuthentication(username, password.toCharArray())
      }
    })
  }

  private fun configureSslContext() {
    try {
      logger.info("Configuring SSL context with client certificate from: ${certificatePath.filename}")
      logger.info(
        "Cert resource: desc={}, exists={}, readable={}, clazz={}",
        certificatePath.description, certificatePath.exists(), certificatePath.isReadable, certificatePath::class.java.name
      )
      logger.info(
        "Certificate password={}",
        certificatePassword
      )
      // Load the PFX/PKCS12 certificate
      val keyStore = KeyStore.getInstance("PKCS12")
      certificatePath.inputStream.use { inputStream ->
        keyStore.load(inputStream, certificatePassword.toCharArray())
      }

      // Initialize KeyManagerFactory with the certificate
      val keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
      keyManagerFactory.init(keyStore, certificatePassword.toCharArray())

      // Initialize TrustManagerFactory (for server certificate validation)
      val trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
      trustManagerFactory.init(null as KeyStore?) // Use default trust store

      // Create SSL context
      val sslContext = SSLContext.getInstance("TLS")
      sslContext.init(
        keyManagerFactory.keyManagers,
        trustManagerFactory.trustManagers,
        null
      )

      // Set as default SSL context for HTTPS connections
      HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)

      logger.info("SSL context configured successfully with client certificate")
    } catch (e: Exception) {
      logger.error("Failed to configure SSL context with client certificate", e)
      throw IllegalStateException("Failed to load client certificate for SOAP service", e)
    }
  }
}
