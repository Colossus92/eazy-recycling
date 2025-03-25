package nl.eazysoftware.eazyrecyclingservice.controller

import io.restassured.RestAssured
import io.restassured.RestAssured.given
import io.restassured.http.ContentType
import io.restassured.response.ValidatableResponse
import nl.eazysoftware.eazyrecyclingservice.TestContainerBaseTest
import nl.eazysoftware.eazyrecyclingservice.repository.CompanyRepository
import nl.eazysoftware.eazyrecyclingservice.repository.LocationRepository
import nl.eazysoftware.eazyrecyclingservice.repository.TransportRepository
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.web.server.LocalServerPort
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.test.AfterTest
import kotlin.test.Test


class WaybillControllerTest(
    @Autowired
    private val transportRepository: TransportRepository,

    @Autowired
    private val companyRepository: CompanyRepository,

    @Autowired
    private val locationRepository: LocationRepository,
): TestContainerBaseTest() {

    @LocalServerPort
    private val port: Int? = null

    @BeforeEach
    fun setUp() {
        RestAssured.baseURI = "http://localhost:$port"
    }

    @AfterTest
    fun cleanUp() {
        transportRepository.deleteAll()
        companyRepository.deleteAll()
        locationRepository.deleteAll()
    }

    @Test
    fun `When no waybills are present an empty response is returned`() {
        getWaybill()
            .body(".", hasSize<Any>(0))
    }

    @Test
    fun `When a waybill is created it can be retrieved`() {
        given()
            .contentType("text/xml")
            .body(readFileAsString())
            .`when`()
            .post("/ws")
            .then()
            .statusCode(200)

        getWaybill()
            .body(".", hasSize<Any>(1))
    }

    private fun getWaybill(): ValidatableResponse = given()
        .contentType(ContentType.JSON)
        .header("Charset", "utf-8")
        .`when`()
        .get("/waybill")
        .then()
        .statusCode(200)

    private fun readFileAsString(): String {
        val path = Paths.get(ClassLoader.getSystemResource("waybill.xml").toURI())
        return String(Files.readAllBytes(path))
    }
}