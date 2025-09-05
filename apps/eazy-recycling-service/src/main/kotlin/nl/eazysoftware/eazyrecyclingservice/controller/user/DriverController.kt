package nl.eazysoftware.eazyrecyclingservice.controller.user

import nl.eazysoftware.eazyrecyclingservice.config.security.SecurityExpressions.HAS_ANY_ROLE
import nl.eazysoftware.eazyrecyclingservice.domain.service.UserService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class DriverController(
    val userService: UserService
) {

    @PreAuthorize(HAS_ANY_ROLE)
    @GetMapping(path = ["/drivers"])
    fun getAllDrivers(): List<UserResponse> {
        return userService.findAllDrivers()
            .map { UserResponse.from(it) }
    }
}