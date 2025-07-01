package com.simulator.sistemacore.controller

import com.simulator.sistemacore.dto.UserDetailResponse
import com.simulator.sistemacore.dto.toResponse
import com.simulator.sistemacore.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/sistemacore")
@Tag(name = "Sistema Core", description = "Simulador do Sistema Core para validação de usuários")
class SistemaCoreController(
    private val userService: UserService
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @GetMapping("/users/detail")
    @Operation(
        summary = "Buscar detalhes do usuário",
        description = "Busca os detalhes completos de um usuário na base de leads por CPF e credor"
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Usuário encontrado",
                content = [Content(schema = Schema(implementation = UserDetailResponse::class))]
            ),
            ApiResponse(
                responseCode = "404",
                description = "Usuário não encontrado"
            )
        ]
    )
    fun getUserDetails(
        @RequestHeader("documentNumber") documentNumber: String,
        @RequestHeader("creditorName") creditorName: String
    ): ResponseEntity<UserDetailResponse> {

        logger.info("Recebida consulta: documentNumber=***${documentNumber.takeLast(4)}, creditorName=$creditorName")

        val user = userService.findUser(documentNumber, creditorName)

        return if (user != null) {
            logger.info("Usuário encontrado e retornado: name=${user.name}")
            ResponseEntity.ok(user.toResponse())
        } else {
            logger.info("Usuário não encontrado")
            ResponseEntity.notFound().build()
        }
    }

    @GetMapping("/users")
    @Operation(
        summary = "Listar todos os usuários",
        description = "Lista todos os usuários cadastrados (para debugging)"
    )
    fun getAllUsers(): List<UserDetailResponse> {
        logger.info("Listando todos os usuários")
        return userService.getAllUsers().map { it.toResponse() }
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Verifica se o serviço está funcionando")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "service" to "Sistema Core Simulator",
            "users" to userService.getAllUsers().size.toString()
        )
    }
}