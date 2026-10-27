package spring_kotlin_learning.trade_settlement_simulator.ledger.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import spring_kotlin_learning.trade_settlement_simulator.infrastructure.ledger.AccountRepository
import spring_kotlin_learning.trade_settlement_simulator.ledger.api.dto.AccountResponse

@RestController
@RequestMapping("/api")
class AccountController(
    private val accountRepository: AccountRepository,
) {

    @GetMapping("/accounts")
    fun getAccounts(): List<AccountResponse> {
        return accountRepository.findAll()
            .map(AccountResponse::fromEntity)
    }
}
