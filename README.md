# Consolidação Completa - Fluxos + Arquitetura

## Arquitetura de 4 Microserviços

### 🔐 FidcPassword
- **Primeiro acesso/redefinição de senha**
- **Integração com Active Directory**
- **Regras de concatenação de empresa credora + CPF**
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
---

### Chamada 1: Obter Secret JWT
- **Microserviço:** `FidcPassword`
- **Endpoint:** `GET /validation/jwt-secret`
- **Headers Obrigatórios:** `x-correlation-id`

#### Response (Sucesso):
```json
{
    "secret": "mySuperSecretKey2025"
}
```

#### Response (Erro - Genérico):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Ocorreu um erro interno. Entre em contato com o suporte técnico",
    "path": "/validation/jwt-secret"
}
```

## Política de Segurança e Logging:
Por questões de segurança, todos os erros retornam erro genérico 500 para não expor informações sensíveis que possam ser exploradas por agentes maliciosos. Configure logs detalhados com correlation-id (quando disponível) para facilitar o rastreamento de erros específicos quando usuários entrarem em contato com o suporte.

### Regras de Negócio:

#### 1. Extração de Dados (Opcional)
- Extrair correlation ID do header `x-correlation-id`

#### 2. Obtenção da Secret
- **Buscar secret global no AWS Secret Manager** usando chave fixa específica para JWT
- **Se erro na integração com AWS Secret Manager:** Retornar erro 500 genérico
- **Se secret não encontrada:** Retornar erro 500 genérico (não expor detalhes internos)

#### 3. Response
- **Retornar apenas a secret** em formato JSON simples
- **Não incluir informações de expiração** ou metadata

### Características da Secret:
- **Escopo:** Global - mesma para todos os partners
- **Rotação:** Diária e transparente para o frontend
- **Uso:** Frontend obtém a secret para assinar JWTs das demais chamadas do fluxo
- **Performance:** Consulta única ao AWS Secret Manager

---

### Chamada 2: Enviar Token
- **Microserviço:** `FidcPassword`
- **Endpoint:** `POST /validation/send-token`
- **Headers Obrigatórios:** `partner`, `x-correlation-id`

#### Request (Token JWT):
```json
{
  "signedData": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcGYiOiIxMjM0NTY3ODkwMSIsImJpcnRoRGF0ZSI6IjE5ODUtMDMtMTUiLCJpYXQiOjE2NDA5OTUyMDB9.signature"
}
```

### Response (Sucesso):
```json
{
    "userEmail": "j***a@gmail.com",
    "cooldownSeconds": 30
}
```

### Response (Erro - Genérico):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Ocorreu um erro interno. Entre em contato com o suporte técnico",
    "path": "/validation/send-token"
}
```

### Estrutura dos dados dentro do JWT recebido:
```json
{
  "cpf": "12345678901",
  "birthDate": "1985-03-15"
}
```

## Política de Segurança e Logging:
Por questões de segurança, a maioria dos erros de validação técnica retornam erro genérico 500 para não expor informações sensíveis que possam ser exploradas por agentes maliciosos. Configure logs detalhados com correlation-id para facilitar o rastreamento de erros específicos quando usuários entrarem em contato com o suporte.

### Regras de Negócio:

#### 1. Validações Iniciais
- **Verificar headers obrigatórios:** Se algum header obrigatório estiver ausente → Retornar erro 500 genérico
- **Verificar campos obrigatórios do JWT:** Se `cpf` ou `birthDate` ausentes → Retornar erro 500 genérico

#### 2. Extração de Dados
- Extrair empresa credora do header `partner`
- Extrair correlation ID do header `x-correlation-id`
- Obter `cpf` e `birthDate` do payload JWT decodificado

#### 3. Validação do JWT
- **Buscar secret no AWS Secret Manager** usando chave específica para validação de tokens de primeiro acesso
- **Decodificar e validar JWT** usando a secret obtida
- **Se JWT inválido (não conseguiu decodificar):** Retornar erro 500 genérico

#### 4. Validações de Usuário
- **Primeiro verificar no AD:** Buscar usuário usando `{partner}_{cpf}`
- **Se erro na integração com AD:** Retornar erro 500 genérico
- **Se usuário NÃO está no AD:**
  - Consultar User Management se usuário existe na base de leads
  - Se erro na integração com User Management → Retornar erro 500 genérico
  - Se usuário não existir no User Management → Retornar erro 500 genérico (não vazar informação)
  - Se usuário existir → prosseguir com validação de data de nascimento e obtenção do email
  - **Definir isFirstAccess = true**
- **Se usuário JÁ está no AD:**
  - Prosseguir com validação de data de nascimento e obtenção do email
  - **Definir isFirstAccess = false**

#### 5. Obtenção do Email e Mascaramento
- **Obter email e nome completo do usuário** (disponível tanto no AD quanto no User Management)
- **Validar data de nascimento** (disponível tanto no AD quanto no User Management)
- **Se data de nascimento não conferir:** Retornar erro 500 genérico
- **Aplicar mascaramento do email:** manter primeiro caractere, últimos caracteres antes do @, e domínio completo
  - Exemplo: `joao.silva@gmail.com` → `j***a@gmail.com`

#### 6. Envio do Token e Persistência
- **Integrar com BankingTicket** para envio de token por email
- **Se erro na integração com BankingTicket:** Retornar erro 500 genérico
- **Configurações do BankingTicket:**
  - Gerar token numérico de 6 dígitos aleatórios
  - Configurar 3 tentativas de validação
  - Validade de 10 minutos
  - Enviar token para o email do usuário
- **Salvar novo estado no Redis:** `first_access:{creditorName}:{cpf}`
- **Se erro ao salvar no Redis:** Retornar erro 500 genérico
- **TTL Redis:** 10 minutos (tempo total para completar todo o fluxo de primeiro acesso/alteração de senha)
- **Retornar response de sucesso** com email mascarado e cooldownSeconds fixo (30)

### Estado Redis Criado (Primeiro Acesso):
```json
{
    "creditorName": "prevcom",
    "cpf": "12345678901",
    "step": "TOKEN_SENT",
    "createdAt": "2025-07-01T12:46:56",
    "isFirstAccess": true,
    "userEmail": "joao.silva@gmail.com",
    "userFullName": "João Silva Santos",
    "userBirthDate": "1985-03-15",
    "userPhoneNumber": "11957753776"
}
```

### Estado Redis Criado (Usuário Existente):
```json
{
    "creditorName": "prevcom",
    "cpf": "12345678901",
    "step": "TOKEN_SENT",
    "createdAt": "2025-07-01T12:46:56",
    "isFirstAccess": false
}
```

---

### Chamada 3: Validar Token
- **Microserviço:** `FidcPassword`
- **Endpoint:** `POST /validation/validate-token`
- **Headers Obrigatórios:** `partner`, `x-correlation-id`

#### Request (Token JWT):
```json
{
  "signedData": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcGYiOiIxMjM0NTY3ODkwMSIsInRva2VuIjoiMTIzNDU2IiwiaWF0IjoxNjQwOTk1MjAwfQ.signature"
}
```

### Response (Sucesso):
```
HTTP 204 No Content
```

### Response (Erro - Token Inválido):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 400,
    "error": "Bad Request",
    "message": "Código de verificação informado é inválido",
    "path": "/validation/validate-token"
}
```

### Response (Erro - Tentativas Esgotadas):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 429,
    "error": "Too Many Requests",
    "message": "Número máximo de tentativas de validação excedido",
    "path": "/validation/validate-token"
}
```

### Response (Erro - Processo Não Encontrado):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 404,
    "error": "Not Found",
    "message": "Processo de validação expirado. Inicie o fluxo novamente",
    "path": "/validation/validate-token"
}
```

### Response (Erro - Genérico):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Ocorreu um erro interno. Entre em contato com o suporte técnico",
    "path": "/validation/validate-token"
}
```

### Estrutura dos dados dentro do JWT recebido:
```json
{
  "cpf": "12345678901",
  "token": "123456"
}
```

## Política de Segurança e Logging:
Por questões de segurança, a maioria dos erros de validação técnica retornam erro genérico 500 para não expor informações sensíveis que possam ser exploradas por agentes maliciosos. Configure logs detalhados com correlation-id para facilitar o rastreamento de erros específicos quando usuários entrarem em contato com o suporte.

### Regras de Negócio:

#### 1. Validações Iniciais
- **Verificar headers obrigatórios:** Se algum header obrigatório estiver ausente → Retornar erro 500 genérico
- **Verificar campos obrigatórios do JWT:** Se `cpf` ou `token` ausentes → Retornar erro 500 genérico

#### 2. Extração de Dados
- Extrair empresa credora do header `partner`
- Extrair correlation ID do header `x-correlation-id`
- Obter `cpf` e `token` do payload JWT decodificado

#### 3. Validação do JWT
- **Buscar secret no AWS Secret Manager** usando chave específica para validação de tokens de primeiro acesso
- **Decodificar e validar JWT** usando a secret obtida
- **Se JWT inválido (não conseguiu decodificar):** Retornar erro 500 genérico

#### 4. Validação de Estado
- **Buscar estado existente no Redis:** `first_access:{creditorName}:{cpf}`
- **Se estado NÃO encontrado:** Retornar erro 404 específico (processo não encontrado - TTL expirou)
- **Se estado encontrado:** Validar se step atual é `TOKEN_SENT`
- **Se step diferente de TOKEN_SENT:** Retornar erro 500 genérico (processo em estado inválido)

#### 5. Validação do Token via BankingTicket
- **Integrar com BankingTicket** para validar o código de verificação informado
- **Se BankingTicket retornar token inválido:** Retornar erro 400 com mensagem específica
- **Se BankingTicket retornar tentativas esgotadas:**
  - Remover estado do Redis (invalidar processo)
  - Se erro ao remover do Redis → Logar erro mas prosseguir com response 429
  - Retornar erro 429 com mensagem específica
- **Se erro na integração com BankingTicket:** Retornar erro 500 genérico
- **Se BankingTicket confirmar token válido:** Prosseguir para próxima etapa

#### 6. Atualização do Estado
- **Atualizar step no Redis** para `TOKEN_VALIDATED`
- **Manter TTL atual** (não renovar o tempo)
- **Manter todos os outros campos** inalterados
- **Se erro ao atualizar Redis:** Retornar erro 500 genérico
- **Retornar HTTP 204 No Content**

### Estado Redis Atualizado:
```json
{
  "step": "TOKEN_VALIDATED"
  // Demais campos permanecem inalterados
}
```

---

### Chamada 4: Criar Senha
- **Microserviço:** `FidcPassword`
- **Endpoint:** `POST /validation/create-password`
- **Headers Obrigatórios:** `partner`, `x-correlation-id`

#### Request (Token JWT):
```json
{
  "signedData": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcGYiOiIxMjM0NTY3ODkwMSIsInBhc3N3b3JkIjoiOTg3NjU0IiwiaWF0IjoxNjQwOTk1MjAwfQ.signature"
}
```

### Response (Sucesso):
```
HTTP 204 No Content
```

### Response (Erro - Senha Inválida):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 400,
    "error": "Bad Request",
    "message": "A senha informada não atende aos critérios de segurança estabelecidos",
    "path": "/validation/create-password"
}
```

### Response (Erro - Processo Expirado):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 404,
    "error": "Not Found",
    "message": "Processo de validação expirado. Inicie o fluxo novamente",
    "path": "/validation/create-password"
}
```

### Response (Erro - Genérico):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Ocorreu um erro interno. Entre em contato com o suporte técnico",
    "path": "/validation/create-password"
}
```

### Estrutura dos dados dentro do JWT recebido:
```json
{
  "cpf": "12345678901",
  "password": "987654"
}
```

## Política de Segurança e Logging:
Por questões de segurança, a maioria dos erros de validação técnica retornam erro genérico 500 para não expor informações sensíveis que possam ser exploradas por agentes maliciosos. Configure logs detalhados com correlation-id para facilitar o rastreamento de erros específicos quando usuários entrarem em contato com o suporte.

### Regras de Negócio:

#### 1. Validações Iniciais
- **Verificar headers obrigatórios:** Se algum header obrigatório estiver ausente → Retornar erro 500 genérico
- **Verificar campos obrigatórios do JWT:** Se `cpf` ou `password` ausentes → Retornar erro 500 genérico

#### 2. Extração de Dados
- Extrair empresa credora do header `partner`
- Extrair correlation ID do header `x-correlation-id`
- Obter `cpf` e `password` do payload JWT decodificado

#### 3. Validação do JWT
- **Buscar secret no AWS Secret Manager** usando chave específica para validação de tokens de primeiro acesso
- **Decodificar e validar JWT** usando a secret obtida
- **Se JWT inválido (não conseguiu decodificar):** Retornar erro 500 genérico

#### 4. Validação de Estado
- **Buscar estado existente no Redis:** `first_access:{creditorName}:{cpf}`
- **Se estado NÃO encontrado:** Retornar erro 404 específico (processo expirado - TTL expirou)
- **Se estado encontrado:** Validar se step atual é `TOKEN_VALIDATED`
- **Se step diferente de TOKEN_VALIDATED:** Retornar erro 500 genérico (processo em estado inválido)

#### 5. Validação das Regras de Senha
- **Validar se senha tem exatamente 6 dígitos:** Deve ter exatamente 6 caracteres numéricos
- **Validar se senha é somente numérica:** Deve conter apenas dígitos (0-9)
- **Validar sequência numérica:** Não pode ser sequencial (ex: 123456, 654321)
- **Validar caracteres repetidos:** Não pode ter todos os dígitos iguais (ex: 111111, 222222)
- **Validar data de nascimento:** Não pode ser igual à data de nascimento do usuário (considerando apenas formatos de 6 dígitos)
  - Obter `birthDate` do Redis (formato: "1985-03-15")
  - Verificar formatos de 6 dígitos: DDMMAA, MMDDAA, AAMMDD, AADDMM
- **Se qualquer validação falhar:** Retornar erro 400 com mensagem específica de senha

#### 6. Preparação para Integração AD
- **Gerar username:** Concatenar `{partner}_{cpf}`
- **Exemplo:** Se partner=prevcom e cpf=12345678901, então username = `prevcom_12345678901`

#### 7. Integração com Active Directory
- **Verificar campo isFirstAccess do Redis:**
- **Se isFirstAccess = true (primeiro acesso):**
  - Criar novo usuário no AD com:
    - Username: `{partner}_{cpf}`
    - Password: senha informada
    - Email: `userEmail` do Redis
    - Nome completo: `userFullName` do Redis
    - Data de nascimento: `birthDate` do Redis
    - Group Name: `partner` (nome da empresa credora)
  - Se erro na criação → Retornar erro 500 genérico
- **Se isFirstAccess = false (usuário existente):**
  - Alterar senha do usuário existente no AD
  - Se erro na alteração → Retornar erro 500 genérico

#### 8. Finalização do Processo
- **Remover estado do Redis:** Excluir chave `first_access:{creditorName}:{cpf}`
- **Se erro ao remover do Redis:** Retornar erro 500 genérico
- **Retornar HTTP 204 No Content**

### Regras de Validação de Senha (6 dígitos):

#### ✅ **Válidas:**
- `987654` (6 dígitos, não sequencial, sem repetição)
- `204816` (6 dígitos, aleatória)
- `531842` (6 dígitos, sem padrão)

#### ❌ **Inválidas:**
- `12345` (menos de 6 dígitos)
- `1234567` (mais de 6 dígitos)
- `123456` (sequência crescente)
- `654321` (sequência decrescente)
- `111111` (caracteres repetidos)
- `150385` (data de nascimento: 15/03/85)
- `031585` (data de nascimento: 03/15/85)
- `850315` (data de nascimento: 85/03/15)
- `abc123` (contém letras)
- `12345a` (contém letras)

---

## FLUXO 2: LOGIN E CRIAÇÃO DE SESSÃO

### Chamada 1: Criar Sessão
- **Microserviço:** `FidcAuth`  
- **Endpoint:** `POST /session/create`  
- **Headers Obrigatórios:** `authorization`, `partner`, `user-agent`,
`channel`, `fingerprint`, `x-correlation-id`

#### Request:
```json
{}
```
*Observação: Body vazio no momento. Validar se mantém assim ou adota padrão JWT do FidcPassword para receber CPF, eliminando necessidade de validação via API externa.*

### Response (Sucesso):
```json
{
    "userInfo": {
        "cpf": "12345678901",
        "name": "João Silva Santos",
        "email": "joao.silva@email.com",
        "birthDate": "1985-03-15",
        "phone": "+5511999887766"
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
        "VIEW_PROFILE",
        "VIEW_STATEMENTS",
        "VIEW_PLAN_DETAILS",
        "VIEW_CONTRIBUTIONS",
        "DOWNLOAD_DOCUMENTS",
        "UPDATE_PERSONAL_DATA",
        "REQUEST_PORTABILITY"
    ],
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Response (Erro - Genérico):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Ocorreu um erro interno. Entre em contato com o suporte técnico",
    "path": "/session/create"
}
```

## Política de Segurança e Logging:
Por questões de segurança, todos os erros retornam erro genérico 500 para não expor informações sensíveis que possam ser exploradas por agentes maliciosos. Configure logs detalhados com correlation-id para facilitar o rastreamento de erros específicos quando usuários entrarem em contato com o suporte.

### Regras de Negócio:

#### 1. Validações Iniciais
- **Verificar headers obrigatórios:** Se algum header obrigatório estiver ausente → Retornar erro 500 genérico

#### 2. Extração de Dados
- Extrair empresa credora do header `partner`
- Extrair correlation ID do header `x-correlation-id`
- Extrair user agent do header `user-agent`
- Extrair channel do header `channel`
- Extrair fingerprint do header `fingerprint`
- Extrair token JWT do header `authorization`

#### 3. Validação do JWT via API Externa
- **Integrar com API do Portal Fidc** para validar o token JWT
- **Se erro na integração com API do Portal:** Retornar erro 500 genérico
- **Se JWT inválido:** Retornar erro 500 genérico
- **Se JWT válido:** Extrair CPF do usuário retornado pela API

#### 4. Consulta de Dados do Usuário
- **Consultar User Management** para obter dados completos do usuário (userInfo + creditor + relationshipList)
- **Se erro na integração com User Management:** Retornar erro 500 genérico
- **Se usuário não encontrado:** Retornar erro 500 genérico

#### 5. Geração de Identificadores
- **Gerar sessionId único** (UUID)
- **Gerar secret único** (UUID) para isolamento da sessão
- **Preparar dados de localização** (se disponíveis nos headers)

#### 6. Lógica de Relacionamentos e Permissões
- **Se relationshipList tem apenas 1 relacionamento:**
  - Definir `relationshipsSelected` com o relacionamento único
  - Integrar com FidcPermission para carregar permissões completas (básicas + específicas do relacionamento)
  - Se erro na integração com FidcPermission → Retornar erro 500 genérico
- **Se relationshipList tem múltiplos relacionamentos:**
  - Definir `relationshipsSelected` como null
  - Integrar com FidcPermission para carregar apenas permissões básicas
  - Se erro na integração com FidcPermission → Retornar erro 500 genérico

#### 7. Validação de Consistência e Invalidação de Sessão Anterior
- **Buscar controle de usuário no PostgreSQL:** tabela `user_session_control` usando CPF + creditorName
- **Se encontrar registro:**
  - **Verificar consistência entre cache e histórico:**
    - Buscar última sessão do histórico em `session_access_history`
    - Se `current_session_id` ≠ `session_id` do último histórico:
      - Logar inconsistência detectada
      - Corrigir `current_session_id` automaticamente
  - **Se is_active = true (sessão anterior ativa):**
    - Buscar sessão anterior no Redis: `session:{current_session_id}`
    - Se sessão existe no Redis → Remover do Redis
    - Se erro ao remover do Redis → Retornar erro 500 genérico

#### 8. Geração do AccessToken
- **Gerar AccessToken JWT** usando a secret única da sessão
- **AccessToken contém:** sessionId + creditorName
- **TTL do AccessToken:** 30 minutos

#### 9. Persistência Atômica da Sessão
**Operação transacional no PostgreSQL + Redis:**
- **Atualizar/Inserir em `user_session_control`:**
  - Se é primeiro acesso: `first_access_at = NOW()`
  - Se não é primeiro acesso: `previous_access_at = last_access_at`
  - Sempre: `last_access_at = NOW()`, `current_session_id = sessionId`, `is_active = true`
  - Se erro no PostgreSQL → Retornar erro 500 genérico

- **Inserir em `session_access_history`:**
  - Todos os dados completos da sessão (occurred_at, ip_address, user_agent, latitude, longitude, location_accuracy, location_timestamp)
  - Se erro no PostgreSQL → Retornar erro 500 genérico

- **Salvar sessão no Redis:** `session:{sessionId}`
  - Incluir secret única da sessão nos dados
  - TTL Redis: 30 minutos
  - Se erro ao salvar no Redis → Retornar erro 500 genérico

#### 10. Eventos SNS
- **Publicar evento SNS** com dados da sessão para outros sistemas
- **Se erro no SNS:** Retornar erro 500 genérico

#### 11. Response Final
- **Retornar dados da sessão** (excluindo os 5 primeiros campos: sessionId, eventOrigin, userAgent, channel, fingerprint)
- **Incluir accessToken** no response

### Estrutura do Banco de Dados PostgreSQL:

#### Tabela Principal (Cache de Estado):
```sql
CREATE TABLE user_session_control (
    id BIGSERIAL PRIMARY KEY,
    cpf VARCHAR(11) NOT NULL,
    creditor_name VARCHAR(100) NOT NULL,
    current_session_id UUID,
    is_active BOOLEAN DEFAULT false,
    first_access_at TIMESTAMP,
    previous_access_at TIMESTAMP,
    last_access_at TIMESTAMP,
    UNIQUE(cpf, creditor_name)
);
```

#### Tabela de Histórico Completo:
```sql
CREATE TABLE session_access_history (
    id BIGSERIAL PRIMARY KEY,
    user_session_control_id BIGINT REFERENCES user_session_control(id),
    session_id UUID NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    ip_address INET,
    user_agent TEXT,
    latitude DECIMAL(10,8),
    longitude DECIMAL(11,8),
    location_accuracy INTEGER,
    location_timestamp TIMESTAMP
);
```

### Objeto Sessão Completo Salvo no Redis:
```json
{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "eventOrigin": "prevcom",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "channel": "WEB",
    "fingerprint": "abc123def456",
    "sessionSecret": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "userInfo": {
        "cpf": "12345678901",
        "name": "João Silva Santos",
        "email": "joao.silva@email.com",
        "birthDate": "1985-03-15",
        "phone": "+5511999887766"
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
        "VIEW_PROFILE",
        "VIEW_STATEMENTS",
        "VIEW_PLAN_DETAILS",
        "VIEW_CONTRIBUTIONS",
        "DOWNLOAD_DOCUMENTS",
        "UPDATE_PERSONAL_DATA",
        "REQUEST_PORTABILITY"
    ]
}
```

### Estrutura do AccessToken JWT:
```json
{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "creditorName": "prevcom"
}
```

### Controle de Sessão Única:
- **Redis**: `session:{sessionId}` → dados completos da sessão + secret única
- **PostgreSQL**: Controle de unicidade por CPF + Credor
- **Chave única**: Apenas uma sessão ativa por CPF + Credor
- **Consistência**: Validação defensiva entre cache PostgreSQL e histórico
- **Segurança**: Secret UUID por sessão para isolamento total

---

### Chamada 2: Selecionar Contexto
- **Microserviço:** `FidcAuth`  
- **Endpoint:** `POST /session/select-context`  
- **Headers Obrigatórios:** `authorization`, `x-correlation-id`

#### Request:
```json
{
    "relationshipId": "REL002"
}
```

### Response (Sucesso):
```json
{
    "userInfo": {
        "cpf": "12345678901",
        "name": "João Silva Santos",
        "email": "joao.silva@email.com",
        "birthDate": "1985-03-15",
        "phone": "+5511999887766"
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
        "id": "REL002",
        "type": "PLANO_PREVIDENCIA",
        "name": "Plano Previdência Premium",
        "status": "ACTIVE",
        "contractNumber": "PREV-2024-005678"
    },
    "permissions": [
        "VIEW_PROFILE",
        "VIEW_STATEMENTS",
        "VIEW_PLAN_DETAILS",
        "VIEW_CONTRIBUTIONS",
        "DOWNLOAD_DOCUMENTS",
        "UPDATE_PERSONAL_DATA",
        "REQUEST_PORTABILITY",
        "PREMIUM_FEATURES"
    ]
}
```

### Response (Erro - Genérico):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Ocorreu um erro interno. Entre em contato com o suporte técnico",
    "path": "/session/select-context"
}
```

## Política de Segurança e Logging:
Por questões de segurança, todos os erros retornam erro genérico 500 para não expor informações sensíveis que possam ser exploradas por agentes maliciosos. Configure logs detalhados com correlation-id para facilitar o rastreamento de erros específicos quando usuários entrarem em contato com o suporte.

### Regras de Negócio:

#### 1. Validações Iniciais
- **Verificar headers obrigatórios:** Se algum header obrigatório estiver ausente → Retornar erro 500 genérico
- **Verificar campos obrigatórios do body:** Se `relationshipId` ausente → Retornar erro 500 genérico

#### 2. Extração de Dados
- Extrair correlation ID do header `x-correlation-id`
- Extrair AccessToken JWT do header `authorization`
- Obter `relationshipId` do body da requisição

#### 3. Validação do AccessToken JWT
- **Buscar sessão no Redis:** `session:{sessionId}` usando sessionId extraído do AccessToken
- **Se sessão não encontrada no Redis:** Retornar erro 500 genérico (sessão expirada/inválida)
- **Decodificar AccessToken JWT** usando a secret única da sessão (`sessionSecret`)
- **Se JWT inválido (não conseguiu decodificar):** Retornar erro 500 genérico
- **Validar creditorName:** Se creditorName do JWT ≠ creditorName da sessão → Retornar erro 500 genérico

#### 4. Validação do Relacionamento
- **Buscar relationshipId na lista:** Verificar se `relationshipId` existe no `relationshipList` da sessão
- **Se relacionamento não encontrado:** Retornar erro 500 genérico (não expor detalhes de segurança)
- **Se relacionamento encontrado:** Prosseguir com seleção

#### 5. Carregamento de Permissões
- **Integrar com FidcPermission** para carregar permissões específicas do relacionamento selecionado
- **Se erro na integração com FidcPermission:** Retornar erro 500 genérico

#### 6. Atualização da Sessão
- **Atualizar dados da sessão no Redis:**
  - Definir `relationshipsSelected` com o relacionamento escolhido
  - Atualizar `permissions` com as novas permissões carregadas
  - **Manter TTL atual** (não renovar tempo) (validar como po portal controla o TTL, para ver se poderiamos renovar)
  - Se erro ao atualizar Redis → Retornar erro 500 genérico

#### 7. Response Final
- **Retornar dados da sessão atualizada** (excluindo os 5 primeiros campos: sessionId, eventOrigin, userAgent, channel, fingerprint)
- **Não incluir accessToken** (mantém o mesmo)

*Observação: Avaliar se não deveria registrar a troca de contexto em `session_access_history` para auditoria completa. Atualmente não implementado, mas pode ser relevante para compliance e rastreabilidade de ações do usuário.*

### Objeto Sessão Atualizado no Redis:
```json
{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "eventOrigin": "prevcom",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "channel": "WEB",
    "fingerprint": "abc123def456",
    "sessionSecret": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "userInfo": {
        "cpf": "12345678901",
        "name": "João Silva Santos",
        "email": "joao.silva@email.com",
        "birthDate": "1985-03-15",
        "phone": "+5511999887766"
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
        "id": "REL002",
        "type": "PLANO_PREVIDENCIA",
        "name": "Plano Previdência Premium",
        "status": "ACTIVE",
        "contractNumber": "PREV-2024-005678"
    },
    "permissions": [
        "VIEW_PROFILE",
        "VIEW_STATEMENTS",
        "VIEW_PLAN_DETAILS",
        "VIEW_CONTRIBUTIONS",
        "DOWNLOAD_DOCUMENTS",
        "UPDATE_PERSONAL_DATA",
        "REQUEST_PORTABILITY",
        "PREMIUM_FEATURES"
    ]
}
```

### Estrutura do AccessToken JWT (Mantém a Mesma):
```json
{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "creditorName": "prevcom"
}
```

### Controle de Contexto:
- **Validação de Permissão**: Apenas relacionamentos do próprio usuário podem ser selecionados
- **Sessão Mantida**: Mesmo sessionId e AccessToken
- **Auditoria**: Registra troca de contexto no histórico
- **Segurança**: Secret única da sessão valida AccessToken

---

### Chamada 3: Finalizar Sessão
- **Microserviço:** `FidcAuth`  
- **Endpoint:** `POST /session/logout`  
- **Headers Obrigatórios:** `authorization`, `x-correlation-id`

#### Request:
```json
{}
```

### Response (Sucesso):
```
HTTP 204 No Content
```

### Response (Erro - Genérico):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Ocorreu um erro interno. Entre em contato com o suporte técnico",
    "path": "/session/logout"
}
```

## Política de Segurança e Logging:
Por questões de segurança, todos os erros retornam erro genérico 500 para não expor informações sensíveis que possam ser exploradas por agentes maliciosos. Configure logs detalhados com correlation-id para facilitar o rastreamento de erros específicos quando usuários entrarem em contato com o suporte.

### Regras de Negócio:

#### 1. Validações Iniciais
- **Verificar headers obrigatórios:** Se algum header obrigatório estiver ausente → Retornar erro 500 genérico

#### 2. Extração de Dados
- Extrair correlation ID do header `x-correlation-id`
- Extrair AccessToken JWT do header `authorization`

#### 3. Validação do AccessToken JWT
- **Extrair sessionId do AccessToken JWT** (decodificação básica para obter sessionId)
- **Buscar sessão no Redis:** `session:{sessionId}` usando sessionId extraído
- **Se sessão não encontrada no Redis:**
  - Buscar em `user_session_control` usando sessionId como `current_session_id`
  - Se encontrado E `is_active = true` → Sessão inconsistente, prosseguir com invalidação
  - Se encontrado E `is_active = false` → Logout já processado, retornar HTTP 204
  - Se não encontrado → Sessão inexistente, retornar HTTP 204 (idempotente)
- **Se sessão encontrada no Redis:**
  - **Decodificar AccessToken JWT** usando a secret única da sessão (`sessionSecret`)
  - **Se JWT inválido (não conseguiu decodificar):** Retornar erro 500 genérico
  - **Validar creditorName:** Se creditorName do JWT ≠ creditorName da sessão → Retornar erro 500 genérico

#### 4. Busca do Controle de Usuário
- **Extrair dados da sessão:** CPF e creditorName
- **Buscar em `user_session_control`:** Localizar registro usando CPF + creditorName
- **Se controle não encontrado:** Logar inconsistência mas prosseguir (dados podem ter sido removidos)

#### 5. Invalidação Atômica da Sessão
**Operação transacional Redis + PostgreSQL:**
- **Remover sessão do Redis:** Excluir chave `session:{sessionId}`
- **Se erro ao remover do Redis:** Retornar erro 500 genérico
- **Atualizar PostgreSQL:**
  - Definir `is_active = false` em `user_session_control`
  - Se erro no PostgreSQL → Retornar erro 500 genérico

#### 6. Auditoria de Logout
- **Inserir em `session_access_history`:**
  - Registrar evento de logout com timestamp
  - Incluir IP e user agent (se disponíveis nos headers)
  - Se erro no PostgreSQL → Retornar erro 500 genérico

#### 7. Response Final
- **Retornar HTTP 204 No Content** (logout sempre sucesso, mesmo se já estava deslogado)

### Características do Logout:

#### **Operação Idempotente:**
- **Múltiplos logouts** da mesma sessão sempre retornam 204
- **Sessão já expirada** também retorna 204
- **Comportamento consistente** independente do estado atual

#### **Limpeza Completa:**
- **Redis**: Remove `session:{sessionId}`
- **PostgreSQL**: Atualiza `user_session_control` (`is_active = false`)
- **Auditoria**: Registra timestamp do logout

#### **Segurança:**
- **Validação completa** do AccessToken antes de qualquer operação
- **Secret por sessão** garante que apenas token válido pode fazer logout
- **Logs detalhados** para troubleshooting sem expor informações sensíveis

### Estados Após Logout:

#### **Redis:**
```
session:{sessionId} → [REMOVIDO]
```

#### **PostgreSQL - user_session_control:**
```json
{
  "cpf": "12345678901",
  "creditor_name": "prevcom",
  "current_session_id": null,
  "is_active": false,
  "last_access_at": "2025-07-01T12:46:56"
}
```

#### **PostgreSQL - session_access_history:**
```json
{
  "session_id": "550e8400-e29b-41d4-a716-446655440000",
  "occurred_at": "2025-07-01T13:15:23",
  "ip_address": "192.168.1.100",
  "user_agent": "Mozilla/5.0..."
}
```

---

## FLUXO 3: GATEWAY E VALIDAÇÃO DE SESSÃO

### Interceptação de Requisições
- **Microserviço:** FidcGateway  
- **Intercepta:** `/* (todas as rotas para o back-end core)`  
- **Headers Obrigatórios:** `authorization`, `partner`, `user-agent`, `x-correlation-id`

#### Request:
```
Qualquer requisição destinada ao back-end core
```

### Response (Sucesso):
```
Proxy da resposta do back-end core com headers adicionais
```

### Response (Erro - Sessão Inválida):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 401,
    "error": "Unauthorized",
    "message": "Sessão inválida ou expirada",
    "path": "/intercepted-path"
}
```

### Response (Erro - Genérico):
```json
{
    "timestamp": "2025-07-01T12:47:21",
    "status": 500,
    "error": "Internal Server Error",
    "message": "Ocorreu um erro interno. Entre em contato com o suporte técnico",
    "path": "/intercepted-path"
}
```

## Política de Segurança e Logging:
Por questões de segurança, a maioria dos erros retornam erro genérico 500 para não expor informações sensíveis que possam ser exploradas por agentes maliciosos. Configure logs detalhados com correlation-id para facilitar o rastreamento de erros específicos quando usuários entrarem em contato com o suporte.

### Regras de Negócio:

#### 1. Validações Iniciais
- **Verificar headers obrigatórios:** Se algum header obrigatório estiver ausente → Retornar erro 500 genérico
- **Interceptar requisições:** Apenas rotas destinadas ao back-end core

#### 2. Extração de Dados
- Extrair AccessToken JWT do header `authorization`
- Extrair partner do header `partner`
- Extrair user agent do header `user-agent`
- Extrair correlation ID do header `x-correlation-id`

#### 3. Decodificação Básica do AccessToken
- **Extrair sessionId e creditorName** do AccessToken JWT (decodificação sem validação de assinatura)
- **Se JWT malformado:** Retornar erro 500 genérico

#### 4. Busca da Sessão no Redis
- **Buscar sessão diretamente no Redis:** `session:{sessionId}`
- **Se sessão não encontrada:** Retornar erro 401 (sessão expirada/inválida)
- **Se erro na conexão com Redis:** Retornar erro 500 genérico

#### 5. Validação do AccessToken com Secret da Sessão
- **Obter sessionSecret** da sessão encontrada no Redis
- **Decodificar e validar AccessToken JWT** usando a secret única da sessão
- **Se JWT inválido (não conseguiu decodificar):** Retornar erro 401
- **Validar creditorName:** Se creditorName do JWT ≠ creditorName da sessão → Retornar erro 401

#### 6. Validações de Segurança
- **Validar UserAgent:** Se user-agent do header ≠ userAgent da sessão → Retornar erro 401
- **Validar Partner:** Se partner do header ≠ eventOrigin da sessão → Retornar erro 401

#### 7. Injeção de Headers de Contexto
- **Adicionar headers para o back-end core:**
  - `X-User-CPF`: CPF do usuário da sessão
  - `X-User-Name`: Nome completo do usuário
  - `X-Creditor-Name`: Nome da empresa credora
  - `X-Relationship-Id`: ID do relacionamento selecionado (se houver)
  - `X-Relationship-Type`: Tipo do relacionamento (se houver)
  - `X-User-Permissions`: Lista de permissões em formato JSON array
  - `X-Session-Id`: SessionId para rastreamento (opcional)
  - `X-Correlation-Id`: Propagar correlation ID

#### 8. Proxy para Back-end Core
- **Redirecionar requisição** para o back-end core com headers injetados
- **Propagar response** do back-end core para o cliente
- **Se erro no back-end core:** Propagar erro original

### Estrutura da Sessão no Redis:
```json
{
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "eventOrigin": "prevcom",
    "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "channel": "WEB",
    "fingerprint": "abc123def456",
    "sessionSecret": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "userInfo": {
        "cpf": "12345678901",
        "name": "João Silva Santos"
    },
    "creditor": {
        "name": "Prevcom RS"
    },
    "relationshipsSelected": {
        "id": "REL001",
        "type": "PLANO_PREVIDENCIA"
    },
    "permissions": [
        "VIEW_PROFILE",
        "VIEW_STATEMENTS",
        "VIEW_PLAN_DETAILS"
    ]
}
```

### Headers Injetados no Back-end Core:
```http
X-User-CPF: 12345678901
X-User-Name: João Silva Santos
X-Creditor-Name: Prevcom RS
X-Relationship-Id: REL001
X-Relationship-Type: PLANO_PREVIDENCIA
X-User-Permissions: ["VIEW_PROFILE","VIEW_STATEMENTS","VIEW_PLAN_DETAILS"]
X-Session-Id: 550e8400-e29b-41d4-a716-446655440000
X-Correlation-Id: abc-123-def-456
```

### Características do Gateway:

#### **Performance Otimizada:**
- **Acesso direto ao Redis** (sem chamadas HTTP para FidcAuth)
- **Validação de sessão em O(1)**
- **Headers pré-computados** do contexto da sessão

#### **Segurança Multicamada:**
- **Validação completa** de AccessToken com secret por sessão
- **Verificação de UserAgent** contra session hijacking
- **Validação de Partner** contra cross-tenant access
- **TTL automático do Redis** previne sessões órfãs

#### **Transparência para Back-end:**
- **Headers ricos** com todo contexto necessário
- **Correlation ID** propagado para rastreabilidade
- **Proxy transparente** da response original

---

## FLUXO 4: GERENCIAMENTO DE PERMISSÕES

### Carregamento de Permissões
- **Microserviço:** `FidcPermission`  
- **Endpoints Internos:**
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
