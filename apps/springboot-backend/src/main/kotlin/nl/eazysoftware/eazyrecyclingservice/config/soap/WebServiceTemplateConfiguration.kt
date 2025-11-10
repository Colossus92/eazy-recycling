package nl.eazysoftware.eazyrecyclingservice.config.soap

import org.apache.http.HttpRequestInterceptor
import org.apache.http.auth.AuthScope
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.config.RequestConfig
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.BasicCredentialsProvider
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.protocol.HTTP
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.ssl.SslBundles
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.client.core.WebServiceTemplate
import org.springframework.ws.client.support.interceptor.ClientInterceptor
import org.springframework.ws.context.MessageContext
import org.springframework.ws.soap.client.core.SoapFaultMessageResolver
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory
import org.springframework.ws.transport.http.HttpComponentsMessageSender
import java.io.ByteArrayOutputStream

/**
 * Spring Web Services configuration using WebServiceTemplate with Apache HttpClient.
 * This approach provides better control over SSL/TLS client certificate authentication.
 *
 * Key advantages over JAX-WS:
 * - Apache HttpClient handles client certificates properly
 * - Explicit SSL configuration using Spring Boot SSL Bundles
 * - Clean separation of concerns
 * - Better debugging and logging
 *
 * Based on: https://zoltanaltfatter.com/2016/04/30/soap-over-https-with-client-certificate-authentication/
 *
 * Only active when amice.enabled=true
 */
@Configuration
@ConditionalOnProperty(name = ["amice.enabled"], havingValue = "true", matchIfMissing = false)
class WebServiceTemplateConfiguration(
  private val sslBundles: SslBundles
) {

  private val logger = LoggerFactory.getLogger(javaClass)

  @Value("\${amice.url:}")
  private lateinit var amiceBaseUrl: String

  @Value("\${amice.username:}")
  private lateinit var username: String

  @Value("\${amice.password:}")
  private lateinit var password: String

  /**
   * JAXB Marshaller for converting SOAP messages to/from Java objects.
   *
   * Note: The generated WSDL classes already have proper JAXB annotations.
   * We specify the ObjectFactory classes directly to avoid needing jaxb.index files.
   */
  @Bean
  fun amiceMarshaller(): Jaxb2Marshaller {
    val marshaller = Jaxb2Marshaller()
    marshaller.contextPath = "nl.eazysoftware.eazyrecyclingservice.adapters.out.soap.generated.melding"
    return marshaller
  }

  /**
   * SOAP Message Factory for creating SOAP messages.
   * Required for proper SOAP fault handling.
   */
  @Bean
  fun messageFactory(): SaajSoapMessageFactory {
    return SaajSoapMessageFactory()
  }

  /**
   * Logging interceptor to see raw SOAP request/response.
   */
  @Bean
  fun loggingInterceptor(): ClientInterceptor {
    return object : ClientInterceptor {

      override fun handleRequest(messageContext: MessageContext?): Boolean {
        if (messageContext != null) {
          val request = messageContext.request
          ByteArrayOutputStream().use { out ->
            request.writeTo(out)
            logger.info("SOAP Request:\n{}", String(out.toByteArray()))
          }
        }
        return true
      }

      override fun handleResponse(messageContext: MessageContext?): Boolean {
        if (messageContext != null) {
          val response = messageContext.response
          if (response != null) {
            ByteArrayOutputStream().use { out ->
              response.writeTo(out)
              logger.info("SOAP Response:\n{}", String(out.toByteArray()))
            }
          }
        }
        return true
      }

      override fun handleFault(messageContext: MessageContext?): Boolean {
        if (messageContext != null) {
          val response = messageContext.response
          if (response != null) {
            ByteArrayOutputStream().use { out ->
              response.writeTo(out)
              logger.error("SOAP Fault Response:\n{}", String(out.toByteArray()))
            }
          }
        }
        return true
      }

      override fun afterCompletion(
        messageContext: MessageContext?,
        ex: java.lang.Exception?
      ) {
        if (ex != null) {
          logger.error("SOAP call completed with exception", ex)
        }
      }

    }
  }

  /**
   * WebServiceTemplate configured with Apache HttpClient for client certificate authentication.
   *
   * This template uses:
   * - SSL Bundle for client certificate (from application.yaml)
   * - Apache HttpClient 4 for HTTP transport
   * - HTTP Basic Authentication credentials
   * - SOAP Fault handling via SoapFaultMessageResolver
   */
  @Bean
  fun amiceWebServiceTemplate(): WebServiceTemplate {
    val marshaller = amiceMarshaller()
    val msgFactory = messageFactory()

    val template = WebServiceTemplate(msgFactory)
    template.marshaller = marshaller
    template.unmarshaller = marshaller
    template.setMessageSender(httpComponentsMessageSender())

    // Add logging interceptor to see raw SOAP messages
    template.interceptors = arrayOf(loggingInterceptor())

    // Configure SOAP Fault handling - throws SoapFaultClientException with fault details
    template.faultMessageResolver = SoapFaultMessageResolver()

    logger.info("Created WebServiceTemplate with client certificate authentication and SOAP fault handling")
    return template
  }

  /**
   * HttpComponentsMessageSender using Apache HttpClient 4 configured with SSL.
   *
   * This is the key component that:
   * 1. Loads the SSL bundle (contains client certificate)
   * 2. Creates SSLContext with the certificate
   * 3. Configures HttpClient to use this SSLContext
   * 4. Returns message sender for WebServiceTemplate
   */
  private fun httpComponentsMessageSender(): HttpComponentsMessageSender {
    try {
      logger.info("Configuring HttpClient with SSL bundle for client certificate authentication")

      // Load SSL bundle from Spring Boot configuration
      val sslBundle = sslBundles.getBundle("amice-client")
      val sslContext = sslBundle.createSslContext()

      logger.info("SSL bundle 'amice-client' loaded successfully")
      logger.info("SSL context: protocol={}, provider={}", sslContext.protocol, sslContext.provider.name)

      // Create SSL socket factory (HttpClient 4 API)
      val sslSocketFactory = SSLConnectionSocketFactory(
        sslContext,
        arrayOf("TLSv1.2", "TLSv1.3"),
        null,
        SSLConnectionSocketFactory.getDefaultHostnameVerifier()
      )

      // Configure connection manager with SSL (HttpClient 4 API)
      val connectionManager = PoolingHttpClientConnectionManager(
        org.apache.http.config.RegistryBuilder.create<org.apache.http.conn.socket.ConnectionSocketFactory>()
          .register("https", sslSocketFactory)
          .build()
      )
      connectionManager.maxTotal = 20
      connectionManager.defaultMaxPerRoute = 10

      // Configure request timeouts
      val requestConfig = RequestConfig.custom()
        .setConnectTimeout(30000) // 30 seconds
        .setSocketTimeout(60000) // 60 seconds
        .build()

      // Build HttpClient with SSL and authentication (HttpClient 4 API)
      // Add interceptor to remove Content-Length header if already present (Spring WS sets it)
      val removeContentLengthInterceptor = HttpRequestInterceptor { request, _ ->
        if (request.containsHeader(HTTP.CONTENT_LEN)) {
          request.removeHeaders(HTTP.CONTENT_LEN)
        }
      }

      val httpClient: CloseableHttpClient = HttpClientBuilder.create()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(requestConfig)
        .setDefaultCredentialsProvider(createCredentialsProvider())
        .addInterceptorFirst(removeContentLengthInterceptor)
        .build()

      logger.info("HttpClient configured with SSL client certificate")
      logger.info("Authentication configured for user: {}", username)

      val messageSender = HttpComponentsMessageSender(httpClient)

      return messageSender

    } catch (e: Exception) {
      logger.error("Failed to configure HttpClient with SSL", e)
      throw IllegalStateException("Failed to configure SOAP client with client certificate", e)
    }
  }

  /**
   * Create credentials provider for HTTP Basic Authentication (HttpClient 4 API).
   */
  private fun createCredentialsProvider(): BasicCredentialsProvider {
    val credentialsProvider = BasicCredentialsProvider()

    if (username.isNotBlank() && password.isNotBlank()) {
      credentialsProvider.setCredentials(
        AuthScope.ANY,
        UsernamePasswordCredentials(username, password)
      )
      logger.debug("Configured HTTP Basic Authentication credentials")
    }

    return credentialsProvider
  }

  /**
   * Bean for ToetsenAfvalstroomnummer service endpoint URL.
   */
  @Bean("amiceToetsenEndpoint")
  fun amiceToetsenEndpoint(): String {
    return if (amiceBaseUrl.isNotBlank()) {
      "$amiceBaseUrl/ToetsenAfvalstroomnummerService.asmx"
    } else {
      throw IllegalStateException("amice.url is not configured")
    }
  }

  /**
   * Bean for Melding service endpoint URL.
   */
  @Bean("amiceMeldingEndpoint")
  fun amiceMeldingEndpoint(): String {
    return if (amiceBaseUrl.isNotBlank()) {
      "$amiceBaseUrl/MeldingService.asmx"
    } else {
      throw IllegalStateException("amice.url is not configured")
    }
  }
}
