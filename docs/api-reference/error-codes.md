# Error Code Reference — Java Commons Platform

Referência central de todos os error codes produzidos pelos módulos do java-commons.

> **Convenção de nomenclatura**: `DOMÍNIO.CATEGORIA[.DETALHE]` — ex.: `BACKUP.NOT_FOUND`, `VALIDATION.REQUIRED_FIELD`.
> Todos os codes são strings imutáveis encapsuladas em `ErrorCode.of(String)` e transportados em `Problem`.

---

## Índice

- [Kernel — Códigos Padrão](#kernel--códigos-padrão)
- [Backup & Restore](#backup--restore)
- [Pagamentos (Stripe)](#pagamentos-stripe)
- [E-mail (SES / SendGrid / SMTP)](#e-mail-ses--sendgrid--smtp)
- [SMS (Twilio / AWS SNS)](#sms-twilio--aws-sns)
- [Busca (OpenSearch / Elasticsearch)](#busca-opensearch--elasticsearch)
- [Excel (Apache POI)](#excel-apache-poi)
- [Workflow Engine](#workflow-engine)
- [Blockchain (Web3j)](#blockchain-web3j)
- [Machine Learning (TensorFlow)](#machine-learning-tensorflow)
- [Configuração](#configuração)
- [Rate Limiting & Web](#rate-limiting--web)
- [Como usar](#como-usar)

---

## Kernel — Códigos Padrão

Definidos em `StandardErrorCodes` e usados como baseline em toda a plataforma.

| Código | Categoria | Descrição |
|--------|-----------|-----------|
| `VALIDATION.FAILED` | VALIDATION | Falha de validação genérica |
| `VALIDATION.REQUIRED_FIELD` | VALIDATION | Campo obrigatório ausente |
| `VALIDATION.INVALID_FORMAT` | VALIDATION | Formato de campo inválido |
| `VALIDATION.OUT_OF_RANGE` | VALIDATION | Valor fora do intervalo permitido |
| `VALIDATION.CONSTRAINT_VIOLATION` | VALIDATION | Violação de constraint |
| `VALIDATION.MULTIPLE_ERRORS` | VALIDATION | Múltiplos erros de validação agregados |
| `BUSINESS.RULE_VIOLATION` | BUSINESS | Violação de regra de negócio |
| `BUSINESS.INVALID_STATE` | BUSINESS | Entidade em estado inválido para a operação |
| `BUSINESS.OPERATION_NOT_ALLOWED` | BUSINESS | Operação não permitida no contexto atual |
| `BUSINESS.DUPLICATE` | BUSINESS | Tentativa de criar registro duplicado |
| `NOT_FOUND.ENTITY` | NOT_FOUND | Entidade não encontrada |
| `NOT_FOUND.RESOURCE` | NOT_FOUND | Recurso não encontrado |
| `CONFLICT.VERSION` | CONFLICT | Conflito de versão (optimistic locking) |
| `CONFLICT.CONCURRENT_MODIFICATION` | CONFLICT | Modificação concorrente detectada |
| `CONFLICT.DUPLICATE_ENTRY` | CONFLICT | Entrada duplicada em conflito |
| `UNAUTHORIZED.MISSING_CREDENTIALS` | UNAUTHORIZED | Credenciais não fornecidas |
| `UNAUTHORIZED.INVALID_CREDENTIALS` | UNAUTHORIZED | Credenciais inválidas |
| `UNAUTHORIZED.TOKEN_EXPIRED` | UNAUTHORIZED | Token de autenticação expirado |
| `FORBIDDEN.INSUFFICIENT_PERMISSIONS` | FORBIDDEN | Usuário sem permissão suficiente |
| `FORBIDDEN.ACCESS_DENIED` | FORBIDDEN | Acesso ao recurso negado |
| `TECHNICAL.DATABASE_ERROR` | TECHNICAL | Falha em operação de banco de dados |
| `TECHNICAL.EXTERNAL_SERVICE_ERROR` | TECHNICAL | Falha em serviço externo |
| `TECHNICAL.TIMEOUT` | TECHNICAL | Timeout na operação |
| `TECHNICAL.INTERNAL_ERROR` | TECHNICAL | Erro interno do sistema |
| `TECHNICAL.CONFIGURATION_ERROR` | TECHNICAL | Erro de configuração |
| `INTEGRATION.COMMUNICATION_FAILURE` | INTEGRATION | Falha de comunicação entre serviços |
| `INTEGRATION.SERIALIZATION_ERROR` | INTEGRATION | Erro de serialização/deserialização |
| `INTEGRATION.PROTOCOL_ERROR` | INTEGRATION | Erro de protocolo de integração |

**Factory methods** disponíveis em `Problems`:

```java
Problems.validation("VALIDATION.REQUIRED_FIELD", "Campo 'email' é obrigatório");
Problems.notFound("NOT_FOUND.ENTITY", "Usuário não encontrado: " + id);
Problems.business("BUSINESS.RULE_VIOLATION", "Saldo insuficiente para a operação");
Problems.conflict("CONFLICT.VERSION", "Versão desatualizada — recarregue e tente novamente");
Problems.unauthorized("UNAUTHORIZED.TOKEN_EXPIRED", "Sessão expirada");
```

---

## Backup & Restore

Módulo: `commons-app-backup-restore`

### Erros de Backup (Filesystem)

| Código | Descrição |
|--------|-----------|
| `BACKUP.FULL_BACKUP_FAILED` | Falha ao criar backup full |
| `BACKUP.NOT_FOUND` | Backup não encontrado pelo ID fornecido |
| `BACKUP.PARENT_NOT_FOUND` | Backup pai não encontrado (incremental/differential) |
| `BACKUP.NO_PARENT_METADATA` | Metadados do backup pai não disponíveis |
| `BACKUP.FULL_BACKUP_NOT_FOUND` | Backup full de referência não encontrado |
| `BACKUP.NOT_A_FULL_BACKUP` | Backup de referência não é do tipo FULL |
| `BACKUP.NO_FULL_METADATA` | Metadados do backup full indisponíveis |
| `BACKUP.DELETE_FAILED` | Falha ao excluir backup |
| `BACKUP.VERIFICATION_FAILED` | Verificação de integridade do backup falhou |

### Erros de Backup (Database)

| Código | Descrição |
|--------|-----------|
| `DB_BACKUP.FAILED` | Operação de backup do banco falhou |
| `DB_BACKUP.NOT_FOUND` | Backup de banco não encontrado |
| `DB_BACKUP.FILE_MISSING` | Arquivo de backup ausente |
| `DB_BACKUP.INVALID_FORMAT` | Formato de arquivo de backup inválido |
| `DB_BACKUP.RESTORE_FAILED` | Restore do banco de dados falhou |
| `DB_BACKUP.VERIFY_FAILED` | Verificação do backup de banco falhou |

### Erros de Restore

| Código | Descrição |
|--------|-----------|
| `RESTORE.FAILED` | Operação de restore falhou |
| `RESTORE.INTEGRITY_CHECK_FAILED` | Verificação de integridade falhou durante restore |
| `RESTORE.ENCRYPTION_KEY_MISSING` | Chave de decriptação não fornecida para backup encriptado |
| `RESTORE.BACKUP_FILE_NOT_FOUND` | Arquivo de backup não encontrado durante restore |
| `RESTORE.INVALID_POINT_IN_TIME` | Timestamp de point-in-time inválido (formato esperado: ISO-8601) |
| `RESTORE.NO_BACKUP_FOR_POINT_IN_TIME` | Nenhum backup disponível para o timestamp solicitado |

### Erros de Backup S3

| Código | Descrição |
|--------|-----------|
| `S3_BACKUP.UPLOAD_FAILED` | Upload para S3 falhou |
| `S3_BACKUP.MISSING_BUCKET` | Configuração do bucket S3 ausente |
| `S3_BACKUP.NOT_FOUND` | Backup não encontrado no S3 |
| `S3_BACKUP.DELETE_FAILED` | Falha ao excluir backup do S3 |
| `S3_BACKUP.LIST_FAILED` | Falha ao listar backups no S3 |
| `S3_BACKUP.VERIFY_FAILED` | Verificação de backup S3 falhou |

---

## Pagamentos (Stripe)

Módulo: `commons-adapters-payment-stripe`

### Pagamentos

| Código | Descrição |
|--------|-----------|
| `PAYMENT.CREATE_FAILED` | Criação de pagamento falhou |
| `PAYMENT.NOT_FOUND` | Pagamento não encontrado |
| `PAYMENT.LIST_FAILED` | Falha ao listar pagamentos |
| `PAYMENT.CANCEL_FAILED` | Cancelamento de pagamento falhou |
| `PAYMENT.CONFIRM_FAILED` | Confirmação de pagamento falhou |

### Métodos de Pagamento

| Código | Descrição |
|--------|-----------|
| `PAYMENT_METHOD.CREATE_FAILED` | Criação de método de pagamento falhou |
| `PAYMENT_METHOD.UNSUPPORTED_TYPE` | Tipo de método de pagamento não suportado |
| `PAYMENT_METHOD.NOT_FOUND` | Método de pagamento não encontrado |
| `PAYMENT_METHOD.LIST_FAILED` | Falha ao listar métodos de pagamento |
| `PAYMENT_METHOD.DELETE_FAILED` | Exclusão de método de pagamento falhou |

### Assinaturas

| Código | Descrição |
|--------|-----------|
| `SUBSCRIPTION.CREATE_FAILED` | Criação de assinatura falhou |
| `SUBSCRIPTION.NOT_FOUND` | Assinatura não encontrada |
| `SUBSCRIPTION.LIST_FAILED` | Falha ao listar assinaturas |
| `SUBSCRIPTION.UPDATE_FAILED` | Atualização de assinatura falhou |
| `SUBSCRIPTION.CANCEL_FAILED` | Cancelamento de assinatura falhou |
| `SUBSCRIPTION.RESUME_FAILED` | Reativação de assinatura falhou |

### Reembolsos

| Código | Descrição |
|--------|-----------|
| `REFUND.CREATE_FAILED` | Criação de reembolso falhou |
| `REFUND.NOT_FOUND` | Reembolso não encontrado |
| `REFUND.LIST_FAILED` | Falha ao listar reembolsos |
| `REFUND.CANCEL_FAILED` | Cancelamento de reembolso falhou |

### Disputas

| Código | Descrição |
|--------|-----------|
| `DISPUTE.NOT_FOUND` | Disputa não encontrada |
| `DISPUTE.LIST_FAILED` | Falha ao listar disputas |
| `DISPUTE.EVIDENCE_FAILED` | Falha ao submeter evidências de disputa |
| `DISPUTE.ACCEPT_FAILED` | Falha ao aceitar disputa |

### Webhooks

| Código | Descrição |
|--------|-----------|
| `WEBHOOK.EMPTY_PAYLOAD` | Payload do webhook está vazio |
| `WEBHOOK.MISSING_SIGNATURE` | Assinatura HMAC do webhook ausente |
| `WEBHOOK.INVALID_SIGNATURE` | Validação de assinatura HMAC falhou |
| `WEBHOOK.PARSE_FAILED` | Falha ao fazer parse do webhook |

---

## E-mail (SES / SendGrid / SMTP)

Módulos: `commons-adapters-email-ses`, `commons-adapters-email-sendgrid`, `commons-adapters-email-smtp`

### AWS SES

| Código | Descrição |
|--------|-----------|
| `SES.NULL_EMAIL` | Endereço de e-mail nulo ou inválido |
| `SES.SEND_ERROR` | Falha ao enviar e-mail via SES |
| `SES.NULL_TEMPLATE_REQUEST` | Requisição de template nula |
| `SES.TEMPLATE_SEND_ERROR` | Falha ao enviar e-mail com template |
| `SES.CONNECTION_FAILED` | Falha de conexão com SES |
| `SES.BAD_REQUEST` | Requisição inválida para SES (HTTP 400) |
| `SES.UNAUTHORIZED` | Autenticação SES falhou (HTTP 401/403) |
| `SES.NOT_FOUND` | Recurso não encontrado no SES (HTTP 404) |
| `SES.RATE_LIMITED` | Limite de taxa SES excedido (HTTP 429) |
| `SES.API_ERROR` | Erro genérico na API SES |

### SendGrid

| Código | Descrição |
|--------|-----------|
| `SENDGRID_RATE_LIMITED` | Limite de taxa SendGrid excedido |

---

## SMS (Twilio / AWS SNS)

Módulos: `commons-adapters-sms-twilio`, `commons-adapters-sms-aws-sns`

| Código | Descrição |
|--------|-----------|
| `SMS_SEND_ERROR` | Falha ao enviar SMS |
| `AUTHORIZATION_ERROR` | Erro de autorização no provedor |
| `INTERNAL_SERVER_ERROR` | Erro interno no provedor de SMS |
| `INVALID_PARAMETER` | Parâmetro inválido enviado ao provedor |
| `INVALID_PARAMETER_VALUE` | Valor de parâmetro inválido |
| `MMS_NOT_SUPPORTED` | MMS não suportado pelo provedor |
| `PHONE_OPTED_OUT` | Número de telefone optou por não receber mensagens |
| `RATE_LIMIT_EXCEEDED` | Limite de taxa excedido |

---

## Busca (OpenSearch / Elasticsearch)

Módulo: `commons-adapters-search-opensearch`

| Código | Descrição |
|--------|-----------|
| `OPENSEARCH.INDEX_IO_ERROR` | Erro de I/O ao indexar documento |
| `OPENSEARCH.BULK_INDEX_IO_ERROR` | Erro de I/O em indexação em lote |
| `OPENSEARCH.SEARCH_IO_ERROR` | Erro de I/O durante busca |
| `OPENSEARCH.DOCUMENT_NOT_FOUND` | Documento não encontrado |
| `OPENSEARCH.GET_IO_ERROR` | Erro de I/O ao recuperar documento |
| `OPENSEARCH.DELETE_IO_ERROR` | Erro de I/O ao excluir documento |
| `OPENSEARCH.UPDATE_IO_ERROR` | Erro de I/O ao atualizar documento |
| `OPENSEARCH.CREATE_INDEX_IO_ERROR` | Erro de I/O ao criar índice |
| `OPENSEARCH.DELETE_INDEX_IO_ERROR` | Erro de I/O ao excluir índice |
| `OPENSEARCH.INDEX_EXISTS_IO_ERROR` | Erro de I/O ao verificar existência de índice |
| `OPENSEARCH.REFRESH_IO_ERROR` | Erro de I/O ao refresh do índice |
| `OPENSEARCH.AGGREGATE_IO_ERROR` | Erro de I/O durante agregação |
| `OPENSEARCH.ERROR` | Erro genérico OpenSearch |

---

## Excel (Apache POI)

Módulo: `commons-adapters-excel-poi`

| Código | Descrição |
|--------|-----------|
| `EXCEL_READ_ERROR` | Falha ao ler arquivo Excel |
| `EXCEL_PARSE_ERROR` | Falha ao fazer parse do Excel |
| `EXCEL_WRITE_FILE_ERROR` | Falha ao escrever arquivo Excel |
| `EXCEL_READER_CLOSED` | Reader de Excel foi fechado |
| `EXCEL_WRITER_CLOSED` | Writer de Excel foi fechado |
| `EXCEL_WORKSHEET_NOT_FOUND` | Planilha não encontrada no workbook |
| `EXCEL_NO_WORKSHEET` | Nenhuma planilha selecionada |
| `EXCEL_NO_MORE_ROWS` | Não há mais linhas para leitura |
| `EXCEL_ROW_READ_ERROR` | Falha ao ler linha do Excel |
| `EXCEL_WRITE_ROW_ERROR` | Falha ao escrever linha no Excel |
| `EXCEL_FLUSH_ERROR` | Falha ao fazer flush do writer |
| `EXCEL_TO_CSV_ERROR` | Falha ao converter Excel para CSV |
| `CSV_TO_EXCEL_ERROR` | Falha ao converter CSV para Excel |
| `EXCEL_VALIDATION_ERROR` | Erro de validação do arquivo Excel |
| `EXCEL_NO_WORKSHEETS` | Arquivo Excel não contém planilhas |

---

## Workflow Engine

Módulo: `commons-app-workflow-engine`

| Código | Descrição |
|--------|-----------|
| `WORKFLOW.DEFINITION_NOT_FOUND` | Definição de workflow não encontrada |
| `WORKFLOW.INSTANCE_NOT_FOUND` | Instância de workflow não encontrada |
| `WORKFLOW.ALREADY_TERMINAL` | Workflow já está em estado terminal |
| `WORKFLOW.NO_TRANSITION` | Nenhuma transição de estado válida disponível |
| `WORKFLOW.STATE_NOT_FOUND` | Estado não encontrado na definição |
| `ACTION.FAILED` | Execução de ação do workflow falhou |

---

## Blockchain (Web3j)

Módulo: `commons-adapters-blockchain-web3j`

| Código | Descrição |
|--------|-----------|
| `BLOCKCHAIN.NOT_IMPLEMENTED` | Funcionalidade não implementada |
| `BLOCKCHAIN.INTERNAL_ERROR` | Erro interno de blockchain |
| `BLOCKCHAIN.NETWORK_ERROR` | Erro de rede blockchain |
| `BLOCKCHAIN.VALIDATION_ERROR` | Erro de validação blockchain |

---

## Machine Learning (TensorFlow)

Módulo: `commons-adapters-ml-tensorflow`

| Código | Descrição |
|--------|-----------|
| `ML.MODEL_NOT_FOUND` | Arquivo de modelo TensorFlow não encontrado |
| `ML.MODEL_LOAD_FAILED` | Falha ao carregar modelo |
| `ML.MODEL_CLOSED` | Modelo foi fechado |
| `ML.PREDICTION_FAILED` | Operação de predição falhou |
| `ML.INVALID_INPUT` | Input inválido para predição |

---

## Configuração

Módulo: `commons-app-configuration`

| Código | Descrição |
|--------|-----------|
| `CONFIG.REFRESH_FAILED` | Refresh de configuração falhou |

---

## Rate Limiting & Web

Módulos: `commons-app-rate-limiting`, `commons-adapters-web-spring`

| Código | Descrição |
|--------|-----------|
| `RATE_LIMIT_EXCEEDED` | Limite de taxa excedido para a chave/cliente |

---

## Como usar

### Criando um Problem com código personalizado

```java
// Via factory (recomendado)
Problem p = Problems.notFound("ORDER.NOT_FOUND", "Pedido não encontrado: " + orderId);
Result<Order> result = Result.fail(p);

// Via builder (para casos complexos)
Problem p = Problem.of(
    ErrorCode.of("PAYMENT.CONFIRM_FAILED"),
    ErrorCategory.TECHNICAL,
    Severity.ERROR,
    "Falha ao confirmar pagamento no Stripe"
);
```

### Tratando erros por categoria

```java
result.ifFail(problem -> {
    switch (problem.category()) {
        case NOT_FOUND -> response.setStatus(404);
        case VALIDATION -> response.setStatus(400);
        case UNAUTHORIZED -> response.setStatus(401);
        case FORBIDDEN -> response.setStatus(403);
        case CONFLICT -> response.setStatus(409);
        default -> response.setStatus(500);
    }
});
```

### Tratando erros por código específico

```java
if ("BACKUP.NOT_FOUND".equals(problem.code().value())) {
    // lógica de recuperação específica
}
```

### Convertendo para RFC 7807 (Problem Details)

```java
RFC7807ProblemDetail detail = RFC7807ProblemDetail.from(problem)
    .withInstance(URI.create("/api/orders/" + orderId))
    .build();
// Serializa para JSON e retorna no body da resposta HTTP
```

---

## Adicionando novos error codes

1. Use sempre o padrão `DOMÍNIO.CATEGORIA[.DETALHE]`
2. Prefira os códigos padrão de `StandardErrorCodes` quando aplicável
3. Documente o novo código neste arquivo na seção do módulo correspondente
4. Crie testes que validem a criação e o conteúdo do código

---

*Última atualização: 2026-03-25*
