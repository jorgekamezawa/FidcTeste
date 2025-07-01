# Consolidação Final Completa - Fluxos + Arquitetura (Atualizada)

## Arquitetura de 4 Microserviços

### 🔐 FidcPassword
- **Primeiro acesso/redefinição de senha**
- **Integração com Active Directory**
- **Regras de concatenação origin + CPF**
- **Gestão de credenciais e validações**

### 🎫 FidcAuth
- **Criação de sessões**
- **Validação de sessões**
- **Troca de contexto**
- **Logout**
- **Geração de AccessToken (JWT)**

### 🚪 FidcGateway
- **Interceptação e roteamento de requisições**
- **Validação automática de sessões via AccessToken**
- **Adição de headers de contexto**
- **Validações de segurança (UserAgent, SQL Injection)**

### 🔑 FidcPermission
- **Gerenciamento completo de permissões**
- **Carregamento de permissões por contexto**
- **Validação de acesso a funcionalidades**

---

## FLUXO 1: PRIMEIRO ACESSO / REDEFINIÇÃO DE SENHA

### Chamada 1: Enviar Token
**Microserviço:** `FidcPassword`  
**Endpoint:** `POST /auth/send-token`  
**Request:** `{ cpf, birthDate }`  
**Header:** `origin` (empresa credora)

**Regras de Negócio:**
- Extrair empresa credora do header `origin`
- **Primeiro verificar no AD:** Buscar usuário usando `{origin}_{cpf}`
- **Se usuário NÃO está no AD:**
    - Consultar User Management se usuário existe na base de leads
    - Se usuário não existir no User Management → retornar erro genérico (não vazar informação)
    - Se usuário existir no User Management → validar data de nascimento
- **Se usuário JÁ está no AD:**
    - Consultar User Management para validar data de nascimento
- Integrar com BankingTicket para envio de token
- Salvar estado no Redis: `first_access:{creditorName}:{cpf}`
- TTL Redis: 10 minutos
- Configurar 3 tentativas no BankingTicket
- Retornar tempo de expiração do token

**Estado Redis Criado:**
```json
{
    "id": "9f542e02-aa50-4393-8dab-95895a78eda3",
    "creditorName": "prevcom",
    "cpf": "12345678901",
    "step": "TOKEN_SENT",
    "createdAt": "2025-07-01T12:46:56"
}
```

### Chamada 2: Validar Token
**Microserviço:** `FidcPassword`  
**Endpoint:** `POST /auth/validate-token`  
**Request:** `{ cpf, token }`  
**Header:** `origin`

**Regras de Negócio:**
- Buscar estado no Redis usando `first_access:{creditorName}:{cpf}`
- Validar se step atual é `TOKEN_SENT`
- Integrar com BankingTicket para validar token
- Se token inválido → Retornar erro
- Se tentativas esgotadas → invalidar processo (excluir a sessão)
- Se token válido → atualizar step para `TOKEN_VALIDATED`
- Manter TTL

### Chamada 3: Criar Senha
**Microserviço:** `FidcPassword`  
**Endpoint:** `POST /auth/create-password`  
**Request:** `{ cpf, password }`  
**Header:** `origin`

**Regras de Negócio:**
- Buscar estado no Redis
- Validar se step atual é `TOKEN_VALIDATED`
- **Lógica de concatenação:** `{origin}_{cpf}` para username no AD
- **Chamar integração AD interna:**
    - Verificar se usuário existe no AD (`{origin}_{cpf}`)
    - Se não existe → criar novo usuário
    - Se existe → alterar senha
- Limpar estado do Redis

**Integração Interna:**
```
FidcPassword (Auth Module) → FidcPassword (AD Module)
```

---

## FLUXO 2: LOGIN E CRIAÇÃO DE SESSÃO

### Chamada 1: Criar Sessão
**Microserviço:** `FidcAuth`  
**Endpoint:** `POST /session/create`  
**Request:** `{ cpf }`  
**Header:** `authorization` (JWT do portal), `origin`, `user-agent`, `channel`, `fingerprint`

**Regras de Negócio:**
- Validar JWT usando secret compartilhada do AWS Secret Manager
- Extrair empresa credora do header `origin`
- Consultar User Management para obter dados do usuário
- Verificar se usuário já possui sessão ativa
- **Se possui sessão ativa:** invalidar sessão anterior no Redis
- Gerar novo `sessionId` único
- **Lógica de Permissões:**
    - Se usuário tem apenas 1 relacionamento → chamar `FidcPermission` e carregar permissões
    - Se usuário tem >1 relacionamentos → aguardar seleção de contexto
- Criar objeto de sessão completo
- Salvar no Redis: `session:{sessionId}`
- TTL inicial: 30 minutos
- **Gerar AccessToken JWT** contendo: `sessionId` + `origin`
- **Publicar evento SNS:** Login bem-sucedido
- Registrar criação da sessão no PostgreSQL (auditoria)

**Objeto Sessão Criado (COMPLETO):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "eventOrigin": "prevcom",
  "userAgent": "Mozilla/5.0...",
  "channel": "WEB",
  "fingerprint": "abc123def456",
  "userInfo": {
    "cpf": "12345678901",
    "name": "João Silva Santos",
    "email": "joao.silva@email.com",
    "birthDate": "1985-03-15",
    "phone": "+5511999887766",
    "isFirstAccessCompleted": true
  },
  "creditor": {
    "id": "CRED001",
    "name": "Prevcom RS",
    "type": "PREVIDENCIA"
  },
  "relationshipList": [
    {
      "id": "REL001",
      "type": "PLANO_PREVIDENCIA",
      "name": "Plano Previdência Básico",
      "status": "ACTIVE",
      "contractNumber": "PREV-2023-001234"
    },
    {
      "id": "REL002", 
      "type": "PLANO_PREVIDENCIA",
      "name": "Plano Previdência Premium",
      "status": "ACTIVE",
      "contractNumber": "PREV-2024-005678"
    }
  ],
  "relationshipsSelected": null,
  "permissions": null
}
```

**Response com AccessToken:**
```json
{
  "sessionData": { ...objeto sessão acima... },
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expiresIn": 1800
}
```

### Chamada 2: Selecionar Contexto
**Microserviço:** `FidcAuth`  
**Endpoint:** `POST /session/select-context`  
**Request:** `{ sessionId, relationshipId }`  
**Header:** `authorization` (AccessToken JWT)

**Regras de Negócio:**
- **Extrair sessionId do AccessToken JWT**
- Buscar sessão atual no Redis usando `sessionId`
- **Validar se `relationshipId` pertence ao usuário:** verificar se existe no `relationshipList` da sessão
- Se relacionamento inválido → retornar erro de segurança
- **Manter o mesmo sessionId** (não gerar novo)
- **Chamar FidcPermission:** carregar as novas permissões para o contexto selecionado
- Atualizar sessão adicionando `relationshipsSelected` + `permissions`
- Manter TTL atual
- Registrar atualização no PostgreSQL

**Sessão Atualizada com Contexto (COMPLETO):**
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "eventOrigin": "prevcom",
  "userAgent": "Mozilla/5.0...",
  "channel": "WEB",
  "fingerprint": "abc123def456",
  "userInfo": {
    "cpf": "12345678901",
    "name": "João Silva Santos",
    "email": "joao.silva@email.com",
    "birthDate": "1985-03-15",
    "phone": "+5511999887766",
    "isFirstAccessCompleted": true
  },
  "creditor": {
    "id": "CRED001",
    "name": "Prevcom RS",
    "type": "PREVIDENCIA"
  },
  "relationshipList": [
    {
      "id": "REL001",
      "type": "PLANO_PREVIDENCIA",
      "name": "Plano Previdência Básico",
      "status": "ACTIVE",
      "contractNumber": "PREV-2023-001234"
    },
    {
      "id": "REL002", 
      "type": "PLANO_PREVIDENCIA",
      "name": "Plano Previdência Premium", 
      "status": "ACTIVE",
      "contractNumber": "PREV-2024-005678"
    }
  ],
  "relationshipsSelected": {
    "id": "REL001",
    "type": "PLANO_PREVIDENCIA", 
    "name": "Plano Previdência Básico",
    "status": "ACTIVE",
    "contractNumber": "PREV-2023-001234"
  },
  "permissions": [
    "VIEW_PLAN_DETAILS",
    "VIEW_CONTRIBUTIONS", 
    "VIEW_STATEMENTS",
    "DOWNLOAD_DOCUMENTS",
    "UPDATE_PERSONAL_DATA",
    "REQUEST_PORTABILITY"
  ]
}
```

**Integração:**
```
FidcAuth → FidcPermission (carregamento de permissões)
```

---

## FLUXO 3: GATEWAY E VALIDAÇÃO DE SESSÃO

### Interceptação de Requisições
**Microserviço:** `FidcGateway`  
**Endpoint:** `/* (todas as rotas para o back-end core)`  
**Header:** `authorization` (AccessToken JWT)

**Regras de Negócio:**
- Interceptar todas as chamadas destinadas ao back-end core
- **Extrair e validar AccessToken JWT**
- **Decodificar AccessToken:** obter `sessionId` + `origin`
- **Chamar FidcAuth:** buscar sessão no Redis: `session:{sessionId}`
- **Se sessão não existe:** retornar 401 Unauthorized
- **Validações de Segurança:**
    - Verificar se UserAgent é o mesmo do login
    - Verificar se Origin é o mesmo do AccessToken
    - Filtros de SQL Injection (futuro - não MVP)
- **Se sessão existe:** verificar TTL e aplicar lógica de renovação
- **Lógica de Renovação (via FidcAuth):**
    - Se faltam < 5 minutos para expirar E < 2h desde criação → renovar +10min
    - Se > 2h desde criação → NÃO renovar (deixar expirar)
- Atualizar `lastActivity` no Redis
- **Adicionar headers para o back-end core:**
    - `X-User-CPF`: CPF do usuário
    - `X-User-Name`: Nome do usuário
    - `X-Creditor-Name`: Nome da empresa credora
    - `X-Relationship-Id`: ID do relacionamento selecionado
    - `X-Relationship-Type`: Tipo do relacionamento
    - `X-User-Permissions`: Lista de permissões (JSON array)
- Redirecionar requisição para back-end core
- Registrar atividade no PostgreSQL (assíncrono)

**Integração:**
```
FidcGateway → FidcAuth (validação de sessão)
FidcGateway → Back-end Core (redirecionamento)
```

---

## FLUXO 4: LOGOUT

### Chamada Única: Invalidar Sessão
**Microserviço:** `FidcAuth`  
**Endpoint:** `POST /session/logout`  
**Request:** `{ sessionId }`  
**Header:** `authorization` (AccessToken JWT)

**Regras de Negócio:**
- **Extrair sessionId do AccessToken JWT**
- Buscar sessão no Redis
- Remover sessão do Redis
- Atualizar status no PostgreSQL para 'REVOKED'
- Registrar timestamp do logout

---

## FLUXO 5: GERENCIAMENTO DE PERMISSÕES

### Carregamento de Permissões
**Microserviço:** `FidcPermission`  
**Endpoints Internos:**
- `POST /permissions/load-by-context`
- `GET /permissions/validate-access`

**Momentos de execução:**
1. **Login com 1 relacionamento:** Carrega permissões automaticamente
2. **Seleção de contexto:** Carrega permissões para relacionamento específico
3. **Troca de contexto:** Recarrega permissões para novo relacionamento

**Regras de Negócio:**
- Consultar sistema de permissões (User Management ou base própria)
- Carregar permissões baseadas em: `usuário + creditor + relacionamento`
- Cache otimizado para consultas frequentes
- Retornar lista de permissões específicas por contexto

**Integração:**
```
FidcAuth → FidcPermission (carregamento)
FidcGateway → FidcPermission (validação - futuro)
```

---

## FLUXO 6: EVENTOS SNS

### Publicação de Eventos
**Microserviço:** `FidcAuth`  
**Momento:** A cada login bem-sucedido

**Estrutura do Evento:**
```json
{
  "eventType": "LOGIN_SUCCESS",
  "timestamp": "2025-06-26T10:00:00Z",
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "userCpf": "12345678901",
  "creditorName": "Prevcom RS",
  "channel": "WEB",
  "userAgent": "Mozilla/5.0...",
  "origin": "prevcom"
}
```

---

## Mapa de Integrações Externas

### FidcPassword:
- User Management (validação usuários)
- BankingTicket (envio e validação de tokens por email)
- Microsoft Active Directory (LDAP/REST)
- Redis (controle de estado primeiro acesso)

### FidcAuth:
- User Management (dados do usuário)
- FidcPermission (carregamento de permissões)
- AWS Secret Manager (secret compartilhada para JWT)
- Redis (sessões ativas)
- PostgreSQL (auditoria)
- SNS (eventos de login)

### FidcGateway:
- FidcAuth (validação de sessões)
- Back-end Core (redirecionamento)

### FidcPermission:
- User Management (base de permissões)
- Cache interno para otimização

---

## Estrutura do AccessToken JWT

### Payload:
```json
{
  "sessionId": "550e8400-e29b-41d4-a716-446655440000",
  "origin": "prevcom",
  "iat": 1640995200,
  "exp": 1640997000
}
```

### Características:
- **Não salvo no Redis** (apenas sessionId é a chave)
- **Validado via secret compartilhada**
- **TTL:** Mesmo tempo da sessão (30min + renovações)
- **Usado para extrair sessionId nos microserviços**

---

## Regras de TTL e Renovação de Sessão

1. **TTL Inicial:** 30 minutos
2. **TTL Primeiro Acesso:** 10 minutos
3. **Verificação de Renovação:** A cada requisição válida
4. **Condição para Renovar:** Faltam menos de 5 minutos para expirar
5. **Extensão:** +10 minutos quando renovar
6. **Limite Máximo:** 2 horas desde a criação inicial
7. **Limite de Sessões:** 1 sessão ativa por usuário (sobrescreve a anterior)

## Estados de Invalidação de Sessão
- **Logout Explícito:** Remove do Redis + atualiza status no Postgres
- **Expiração Natural:** TTL do Redis expira automaticamente
- **Nova Sessão:** Sobrescreve sessão anterior do mesmo usuário
- **Validações de Segurança:** UserAgent diferente, Origin inválido

## Validações de Segurança
- **UserAgent:** Deve ser o mesmo do login inicial
- **Origin:** Deve corresponder ao AccessToken
- **SQL Injection:** Filtros preventivos (futuro - não MVP)
- **Fingerprint:** Validação de dispositivo

## Auditoria PostgreSQL
- **Apenas para logs e compliance**
- **Nunca consultado para validar sessão**
- **Registra:** criação, renovações, logout, expiração, troca de contexto, eventos de segurança

## Benefícios da Arquitetura Final

1. **Separação de responsabilidades clara** (4 microserviços especializados)
2. **Performance otimizada** com AccessToken JWT
3. **Segurança robusta** (validação de contexto + permissões + UserAgent)
4. **Escalabilidade independente** por domínio
5. **Observabilidade completa** via eventos SNS
6. **Flexibilidade de permissões** carregadas por contexto
7. **Auditoria e compliance** bancário completo