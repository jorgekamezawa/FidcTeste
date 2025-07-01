package com.fidc.password

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.openfeign.EnableFeignClients

@SpringBootApplication
@EnableFeignClients
class FidcPasswordApplication

fun main(args: Array<String>) {
    runApplication<FidcPasswordApplication>(*args)
}