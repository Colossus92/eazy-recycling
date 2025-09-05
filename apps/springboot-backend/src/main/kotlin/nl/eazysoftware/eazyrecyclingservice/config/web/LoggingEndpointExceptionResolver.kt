package nl.eazysoftware.eazyrecyclingservice.config.web

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.ws.context.MessageContext
import org.springframework.ws.server.endpoint.AbstractEndpointExceptionResolver

@Component
class LoggingEndpointExceptionResolver: AbstractEndpointExceptionResolver() {

    private val log = LoggerFactory.getLogger(LoggingEndpointExceptionResolver::class.java)

    override fun logException(ex: Exception?, messageContext: MessageContext?) {
        log.error("An error occurred", ex)
    }

    override fun resolveExceptionInternal(
        var1: MessageContext?,
        var2: Any?,
        var3: java.lang.Exception?
    ): Boolean {
        return true
    }
}