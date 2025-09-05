package nl.eazysoftware.eazyrecyclingservice.config.web

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.io.IOException
import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletException
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

@Component
class RequestLoggingFilter : Filter {
    private val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

    @Throws(IOException::class, ServletException::class)
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest
        val httpResponse = response as HttpServletResponse

        val method = httpRequest.method
        val uri = httpRequest.requestURI
        val queryString = httpRequest.queryString ?: ""
        val headers = httpRequest.headerNames.toList().joinToString { "$it=${httpRequest.getHeader(it)}" }

        logger.info("Incoming Request: Method=$method, URI=$uri, Query=$queryString, Body=$httpRequest Headers=[$headers]")

        chain.doFilter(request, response)

        val status = httpResponse.status
        logger.info("Response Status for URI=$uri: $status")
    }
}
