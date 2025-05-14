package nl.eazysoftware.eazyrecyclingservice.controller.user

import io.github.jan.supabase.auth.user.UserInfo
import nl.eazysoftware.eazyrecyclingservice.domain.service.UserService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class DriverController(
    val userService: UserService
) {

    @GetMapping(path = ["/drivers"])
    fun getAllDrivers(): List<UserResponse> {
        return userService.findAllDrivers()
            .map { UserResponse.from(it) }
    }
}