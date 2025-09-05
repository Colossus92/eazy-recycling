package nl.eazysoftware.eazyrecyclingservice.config.web

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper

class HttpMethodOverrideRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
    override fun getMethod(): String {
        return "POST" // Override method to POST
    }
}