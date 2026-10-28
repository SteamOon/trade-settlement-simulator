package spring_kotlin_learning.trade_settlement_simulator

import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@Import(PostgresTestcontainersConfiguration::class)
class HttpContractIntegrationTest @Autowired constructor(
    private val context: WebApplicationContext,
    private val objectMapper: ObjectMapper,
) {

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build()
    }

    @Test
    fun `POST api trades validation rejects invalid body and returns field errors`() {
        val invalidTrade = validTradeRequest(
            "currency" to "usd",
            "quantity" to 0,
            "price" to 0,
            "ourPartyId" to "",
        )

        mockMvc.perform(
            post("/api/trades")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidTrade))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.error").value("Bad Request"))
            .andExpect(jsonPath("$.fieldErrors.currency").value("Currency must be a valid ISO-4217 code"))
            .andExpect(jsonPath("$.fieldErrors.quantity").value("Quantity must be greater than zero"))
    }

    @Test
    fun `POST api trades returns 201 Created and Location header for valid body`() {
        val validTrade = validTradeRequest()

        mockMvc.perform(
            post("/api/trades")
                .header("Idempotency-Key", "create-valid-001")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validTrade))
        )
            .andExpect(status().isCreated)
            .andExpect(header().string("Location", containsString("/api/trades/")))
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.history[0].to").value("PENDING"))
    }

    @Test
    fun `GET api trades missing id returns 404 ApiError`() {
        val missingId = UUID.randomUUID()

        mockMvc.perform(get("/api/trades/{id}", missingId))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"))
            .andExpect(jsonPath("$.message").value("Trade $missingId not found"))
            .andExpect(jsonPath("$.timestamp").exists())
    }

    @Test
    fun `POST api trades confirmation handles mismatch and illegal transition as conflicts`() {
        val unmatchedTradeId = createTradeId("confirm-unmatched")
        val matchingRequest = validConfirmationRequest()
        val mismatchRequest = validConfirmationRequest(
            "quantity" to 99.99,
            "price" to 49.99,
        )

        mockMvc.perform(
            post("/api/trades/{id}/confirmation", unmatchedTradeId)
                .header("Idempotency-Key", "confirm-mismatch-001")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mismatchRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UNMATCHED"))

        val illegalTradeId = createTradeId("confirm-illegal")
        mockMvc.perform(
            post("/api/trades/{id}/confirmation", illegalTradeId)
                .header("Idempotency-Key", "confirm-match-001")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(matchingRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("MATCHED"))

        mockMvc.perform(
            post("/api/trades/{id}/confirmation", illegalTradeId)
                .header("Idempotency-Key", "confirm-illegal-001")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(mismatchRequest))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Trade is already matched; confirmation does not match the captured trade details"))
    }

    @Test
    fun `duplicate idempotency key returns same resource and rejects changed payload`() {
        val tradeId = createTradeId("idem-confirm")
        val confirmation = validConfirmationRequest()

        mockMvc.perform(
            post("/api/trades/{id}/confirmation", tradeId)
                .header("Idempotency-Key", "duplicate-confirm")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmation))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(tradeId.toString()))
            .andExpect(jsonPath("$.status").value("MATCHED"))

        mockMvc.perform(
            post("/api/trades/{id}/confirmation", tradeId)
                .header("Idempotency-Key", "duplicate-confirm")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(confirmation))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(tradeId.toString()))
            .andExpect(jsonPath("$.status").value("MATCHED"))

        mockMvc.perform(
            post("/api/trades/{id}/confirmation", tradeId)
                .header("Idempotency-Key", "duplicate-confirm")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validConfirmationRequest("price" to 55.25)))
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.message").value("Trade is already matched; confirmation does not match the captured trade details"))
    }

    @Test
    fun `POST api trades settlement result settles a matched trade`() {
        val tradeId = createTradeId("settlement-result-001")

        mockMvc.perform(
            post("/api/trades/{id}/confirmation", tradeId)
                .header("Idempotency-Key", "settlement-result-confirm")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validConfirmationRequest()))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("MATCHED"))

        mockMvc.perform(
            post("/api/trades/{id}/settlement-instruction-result", tradeId)
                .header("Idempotency-Key", "settlement-result-001")
                .contentType(APPLICATION_JSON)
                .content("""{"result":"SETTLED","reason":"settle_success"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("SETTLED"))
    }

    @Test
    fun `POST api trades settlement result rejects unmatched trades with 409`() {
        val tradeId = createTradeId("settlement-result-mismatch")

        mockMvc.perform(
            post("/api/trades/{id}/confirmation", tradeId)
                .header("Idempotency-Key", "settlement-result-mismatch-confirm")
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validConfirmationRequest("quantity" to 99.99)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UNMATCHED"))

        mockMvc.perform(
            post("/api/trades/{id}/settlement-instruction-result", tradeId)
                .header("Idempotency-Key", "settlement-result-mismatch-001")
                .contentType(APPLICATION_JSON)
                .content("""{"result":"SETTLED","reason":"settle_success"}""")
        )
            .andExpect(status().isConflict)
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"))
            .andExpect(jsonPath("$.message").value("Cannot settle a trade in UNMATCHED state"))
    }

    @Test
    fun `GET api accounts returns balances`() {
        mockMvc.perform(get("/api/accounts"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].balance").exists())
            .andExpect(jsonPath("$[0].partyId").exists())
    }

    private fun createTradeId(idempotencyKey: String): UUID {
        val request = validTradeRequest()
        val response = mockMvc.perform(
            post("/api/trades")
                .header("Idempotency-Key", idempotencyKey)
                .contentType(APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()
            .response
            .contentAsString

        return UUID.fromString(objectMapper.readTree(response).get("id").asText())
    }

    private fun validTradeRequest(vararg overrides: Pair<String, Any?>): Map<String, Any?> {
        val request = linkedMapOf<String, Any?>(
            "side" to "BUY",
            "isin" to "US0378331005",
            "quantity" to 100.00,
            "price" to 50.25,
            "currency" to "EUR",
            "ourPartyId" to "HOUSE-01",
            "counterpartyId" to "COUNTERPARTY-01",
            "tradeDate" to "2026-10-26",
            "settlementDate" to "2026-10-27",
        )
        overrides.forEach { (key, value) -> request[key] = value }
        return request
    }

    private fun validConfirmationRequest(vararg overrides: Pair<String, Any?>): Map<String, Any?> {
        val request = linkedMapOf<String, Any?>(
            "side" to "SELL",
            "isin" to "US0378331005",
            "quantity" to 100.00,
            "price" to 50.25,
            "currency" to "EUR",
        )
        overrides.forEach { (key, value) -> request[key] = value }
        return request
    }
}
