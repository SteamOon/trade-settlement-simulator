package spring_kotlin_learning.trade_settlement_simulator

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import kotlin.test.assertEquals

@SpringBootTest
@Import(PostgresTestcontainersConfiguration::class)
class TradeSettlementSimulatorApplicationTests {

	@Autowired
	lateinit var jdbcTemplate: JdbcTemplate

	@Test
	fun contextLoads() {
		val parties = jdbcTemplate.queryForObject("select count(*) from parties", Int::class.java)
		val accounts = jdbcTemplate.queryForObject("select count(*) from accounts", Int::class.java)
		assertEquals(2, parties)
		assertEquals(4, accounts)
	}
}
