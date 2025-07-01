package com.simulator.sistemacore.service

import com.simulator.sistemacore.model.Creditor
import com.simulator.sistemacore.model.Relationship
import com.simulator.sistemacore.model.User
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class UserService {

    private val logger = LoggerFactory.getLogger(this::class.java)

    // Banco de dados em memória - simulação
    private val users = mutableMapOf<String, User>()

    init {
        // Dados iniciais para testes
        initializeTestData()
    }

    fun findUser(documentNumber: String, creditorName: String): User? {
        logger.debug("Buscando usuário: documentNumber=***${documentNumber.takeLast(4)}, creditorName=$creditorName")

        val user = users[documentNumber]

        if (user == null) {
            logger.debug("Usuário não encontrado")
            return null
        }

        // Verificar se o usuário pertence ao credor
        if (!user.creditor.name.equals(creditorName, ignoreCase = true)) {
            logger.debug("Usuário não pertence ao credor especificado")
            return null
        }

        logger.debug("Usuário encontrado: name=${user.name}")
        return user
    }

    fun addUser(user: User) {
        users[user.documentNumber] = user
        logger.info("Usuário adicionado: documentNumber=***${user.documentNumber.takeLast(4)}, name=${user.name}")
    }

    fun getAllUsers(): List<User> {
        return users.values.toList()
    }

    fun removeUser(documentNumber: String): Boolean {
        val removed = users.remove(documentNumber) != null
        if (removed) {
            logger.info("Usuário removido: documentNumber=***${documentNumber.takeLast(4)}")
        }
        return removed
    }

    private fun initializeTestData() {
        logger.info("Inicializando dados de teste...")

        // Usuário Prevcom 1
        addUser(User(
            documentNumber = "12345678901",
            name = "João Silva Santos",
            email = "joao.silva@email.com",
            phoneNumber = "11999887766",
            birthDate = LocalDate.of(1997, 1, 1),
            creditor = Creditor(
                name = "Prevcom",
                cge = "654321",
                documentNumber = "11273637488761"
            ),
            relationshipList = listOf(
                Relationship(1, "PLANO_PREVIDENCIA", "Prevcom RS"),
                Relationship(2, "PLANO_PREVIDENCIA", "Prevcom RG")
            )
        ))

        // Usuário Prevcom 2 (do exemplo que você passou)
        addUser(User(
            documentNumber = "45531709811",
            name = "Jorge Cailo Cardoso Kamezawa",
            email = "jorge@email.com",
            phoneNumber = "11957743554",
            birthDate = LocalDate.of(1997, 1, 1),
            creditor = Creditor(
                name = "Prevcom",
                cge = "654321",
                documentNumber = "11273637488761"
            ),
            relationshipList = listOf(
                Relationship(1, "PLANO_PREVIDENCIA", "Prevcom RS"),
                Relationship(2, "PLANO_PREVIDENCIA", "Prevcom RG"),
                Relationship(3, "PLANO_PREVIDENCIA", "Prevcom RG-Unis")
            )
        ))

        // Usuário outro credor
        addUser(User(
            documentNumber = "98765432100",
            name = "Maria Oliveira Costa",
            email = "maria.oliveira@email.com",
            phoneNumber = "11888777666",
            birthDate = LocalDate.of(1990, 12, 25),
            creditor = Creditor(
                name = "OutroCredor",
                cge = "123456",
                documentNumber = "22384748599872"
            ),
            relationshipList = listOf(
                Relationship(4, "PLANO_PREVIDENCIA", "Plano Básico"),
                Relationship(5, "PLANO_PREVIDENCIA", "Plano Premium")
            )
        ))

        logger.info("Dados de teste inicializados: {} usuários", users.size)
    }
}
