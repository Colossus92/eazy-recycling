package nl.eazysoftware.eazyrecyclingservice.config.web

import jakarta.servlet.Filter
import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Component

@Component
class SoapMethodOverrideFilter : Filter {

    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val httpRequest = request as HttpServletRequest

        if (httpRequest.method == "GET" && httpRequest.requestURI == "/ws") {
            val wrappedRequest = HttpMethodOverrideRequestWrapper(httpRequest)
            chain.doFilter(wrappedRequest, response)
            return
        }

        chain.doFilter(request, response)
    }
}