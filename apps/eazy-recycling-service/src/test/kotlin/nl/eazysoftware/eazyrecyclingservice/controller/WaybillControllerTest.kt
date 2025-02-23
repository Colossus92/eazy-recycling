package nl.eazysoftware.eazyrecyclingservice.controller

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import nl.eazysoftware.eazyrecyclingservice.TestContainerBaseTest
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.springframework.boot.test.web.server.LocalServerPort
import kotlin.test.Test


class WaybillControllerTest(): TestContainerBaseTest() {

    @LocalServerPort
    private val port: Int? = null

    @BeforeEach
    fun setUp() {
        RestAssured.baseURI = "http://localhost:$port"
    }

    @Test
    fun `When no waybills are present an empty response is returned`() {
        given()
            .contentType(ContentType.JSON)
            .`when`()
            .get("/waybill")
            .then()
            .statusCode(200)
            .body(".", hasSize<Any>(0))
    }
}