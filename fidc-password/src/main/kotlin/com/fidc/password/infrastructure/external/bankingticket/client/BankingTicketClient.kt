package com.fidc.password.infrastructure.external.bankingticket.client

import com.fidc.password.infrastructure.external.bankingticket.dto.BankingTicketEmailRequest
import com.fidc.password.infrastructure.external.bankingticket.dto.BankingTicketEmailResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

@FeignClient(name = "banking-ticket", url = "\${external-apis.banking-ticket.base-url}")
interface BankingTicketClient {

    @PostMapping("/ticket/v4/email")
    fun sendTokenByEmail(@RequestBody request: BankingTicketEmailRequest): BankingTicketEmailResponse
}