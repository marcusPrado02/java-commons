# Cobertura de Testes com JaCoCo

## Visão Geral

Este projeto utiliza [JaCoCo](https://www.jacoco.org/) (Java Code Coverage) versão 0.8.11 para medir e reportar a cobertura de testes.

## Configuração

### Limites de Cobertura

Os seguintes limites são configuráveis via propriedades Maven:

```xml
<jacoco.line.coverage>0.80</jacoco.line.coverage>      <!-- 80% de cobertura de linhas -->
<jacoco.branch.coverage>0.75</jacoco.branch.coverage>  <!-- 75% de cobertura de branches -->
```

### Verificação de Cobertura

Por padrão, a **verificação estrita de cobertura está desabilitada** (`jacoco.check.skip=true`), permitindo builds com baixa cobertura enquanto o projeto está em desenvolvimento.

Para **habilitar a verificação estrita**:

```bash
./mvnw verify -Djacoco.check.skip=false
```

Isso fará com que o build **falhe** se os módulos não atingirem os limites configurados.

## Como Usar

### 1. Gerar Relatórios de Cobertura

Execute os testes para gerar relatórios:

```bash
./mvnw clean test
```

Os relatórios HTML serão gerados em:
```
<módulo>/target/site/jacoco/index.html
```

### 2. Visualizar Relatórios

Abra o arquivo `index.html` no navegador para ver:
- Cobertura por pacote, classe e método
- Linhas cobertas/não cobertas (destacadas em verde/vermelho)
- Cobertura de branches (decisões if/switch)
- Métricas detalhadas

### 3. Relatório Agregado

Para gerar um relatório consolidado de todos os módulos:

```bash
./mvnw clean verify
./mvnw jacoco:report-aggregate -pl commons-platform
```

O relatório agregado estará em:
```
commons-platform/target/site/jacoco-aggregate/index.html
```

### 4. Verificar Cobertura no CI/CD

O build do GitHub Actions:
1. Executa testes e gera relatórios
2. Faz upload dos relatórios como artefatos (retenção: 30 dias)
3. Adiciona comentário em PRs com métricas de cobertura
4. Exige no mínimo 80% de cobertura geral e 75% em arquivos alterados

## Excluir Módulos da Verificação

Para módulos que não precisam de verificação de cobertura (ex: agregadores, POMs, starters), adicione no `pom.xml`:

```xml
<properties>
    <jacoco.check.skip>true</jacoco.check.skip>
</properties>
```

## Arquivos Gerados

- `target/jacoco.exec` - Dados binários de execução
- `target/site/jacoco/` - Relatórios HTML
- `target/site/jacoco/jacoco.xml` - Relatório XML (usado pelo GitHub Actions)
- `target/site/jacoco/jacoco.csv` - Relatório CSV

## Melhores Práticas

1. **Execute testes regularmente** para manter os relatórios atualizados
2. **Revise a cobertura** antes de fazer merge de PRs
3. **Foque em testar lógica de negócio** - não necessariamente 100% de cobertura
4. **Use DTOs/Records** podem ter baixa cobertura sem problemas
5. **Exclua código gerado** se necessário via configuração JaCoCo

## Comandos Úteis

```bash
# Gerar relatórios sem executar verificações
./mvnw clean test

# Build completo com verificação de cobertura
./mvnw clean verify -Djacoco.check.skip=false

# Apenas um módulo
./mvnw test -pl commons-kernel-errors

# Ver relatório agregado
./mvnw jacoco:report-aggregate -pl commons-platform
open commons-platform/target/site/jacoco-aggregate/index.html
```

## Integração com IDEs

### IntelliJ IDEA
- Run with Coverage (▶️ com ícone de escudo)
- View → Tool Windows → Coverage

### VS Code
- Instale a extensão "Coverage Gutters"
- Os relatórios JaCoCo XML são automaticamente detectados

## Troubleshooting

### Relatórios não gerados
Verifique se os testes foram executados:
```bash
./mvnw clean test
ls -la */target/site/jacoco/
```

### Build falhando por cobertura
Desabilite temporariamente:
```bash
./mvnw verify -Djacoco.check.skip=true
```

### Cobertura zerada
Certifique-se que há testes executados:
```bash
./mvnw test | grep "Tests run"
```

## Referências

- [JaCoCo Documentation](https://www.jacoco.org/jacoco/trunk/doc/)
- [Maven JaCoCo Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [GitHub Actions Workflow](.github/workflows/ci.yml)
