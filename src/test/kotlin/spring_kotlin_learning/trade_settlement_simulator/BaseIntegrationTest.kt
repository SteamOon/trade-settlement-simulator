package spring_kotlin_learning.trade_settlement_simulator

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Import(PostgresTestcontainersConfiguration::class)
@Testcontainers
abstract class BaseIntegrationTest
