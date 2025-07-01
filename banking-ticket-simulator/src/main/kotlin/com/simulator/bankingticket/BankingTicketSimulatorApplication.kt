package com.simulator.bankingticket

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class BankingTicketSimulatorApplication

fun main(args: Array<String>) {
    runApplication<BankingTicketSimulatorApplication>(*args)
}