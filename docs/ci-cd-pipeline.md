# CI/CD Pipeline

Este documento descreve o pipeline de Integração Contínua e Entrega Contínua configurado para o projeto java-commons.

## Visão Geral

O projeto possui dois workflows principais:
- **CI (Continuous Integration)**: Executa em todos os commits e PRs
- **Release**: Publicação de versões com versionamento semântico

## Workflow CI

Arquivo: `.github/workflows/ci.yml`

### Triggers

- Push em branches `main` e `develop`
- Pull requests para `main` e `develop`
- Execução manual via workflow_dispatch

### Jobs

#### 1. Build and Test

**Estratégia de Matriz:**
- Java 21 (LTS)
- Java 23 (Latest)

**Etapas:**
1. Checkout do código
2. Setup do Java (Temurin)
3. Cache de dependências Maven (~/.m2/repository)
4. Build completo: `mvn clean install -B`
5. Upload de artefatos:
   - Relatórios de teste (Surefire/Failsafe)
   - Relatórios JaCoCo de cobertura
   - JARs compilados

**Artefatos:**
- `test-results-java-${{ matrix.java }}`
- `coverage-report-java-${{ matrix.java }}`
- `build-artifacts-java-${{ matrix.java }}`

#### 2. Static Analysis

**Execução:** Após build bem-sucedido (Java 21 apenas)

**Ferramentas:**
- SpotBugs (bugs e security issues)
- Checkstyle (code style)
- PMD (code smells e duplicação)

**Comando:**
```bash
mvn verify -B -DskipTests \
  -Dspotbugs.skip=false \
  -Dcheckstyle.skip=false \
  -Dpmd.skip=false
```

**Artefatos:**
- `static-analysis-reports`
  - `target/spotbugs.xml`
  - `target/checkstyle-result.xml`
  - `target/pmd.xml`
  - `target/cpd.xml`

#### 3. Security Scan

**Execução:** Após build bem-sucedido (Java 21 apenas)

**Ferramenta:** OWASP Dependency-Check

**Configuração:**
- Threshold: CVSS >= 7.0
- Formatos: HTML, JSON, SARIF
- Supressões: `dependency-check-suppressions.xml`

**Comando:**
```bash
mvn org.owasp:dependency-check-maven:check -B \
  -DfailBuildOnCVSS=7 \
  -DsuppressionFile=dependency-check-suppressions.xml
```

**Artefatos:**
- `security-scan-report`
  - `target/dependency-check-report.html`
  - `target/dependency-check-report.json`

**GitHub Integration:**
- Upload de SARIF para Security tab

#### 4. Build Summary

**Execução:** Sempre (mesmo se jobs anteriores falharem)

**Conteúdo:**
- Status de todos os jobs
- Links para artefatos
- Cobertura de código (se disponível)
- Issues de segurança (se encontrados)

### Cache Strategy

```yaml
- uses: actions/cache@v4
  with:
    path: ~/.m2/repository
    key: ${{ runner.os }}-maven-${{ hashFiles('**/pom.xml') }}
    restore-keys: ${{ runner.os }}-maven-
```

**Benefícios:**
- Reduz tempo de build em ~40-60%
- Economiza transferência de rede
- Melhora experiência do desenvolvedor

## Workflow Release

Arquivo: `.github/workflows/release.yml`

### Trigger

Manual via workflow_dispatch com seleção de tipo:
- `major`: 1.0.0 → 2.0.0 (breaking changes)
- `minor`: 1.0.0 → 1.1.0 (new features)
- `patch`: 1.0.0 → 1.0.1 (bug fixes)

### Etapas

#### 1. Calculate Version

```bash
# Extrai versão atual
CURRENT_VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)

# Calcula nova versão baseado no tipo
case "${{ inputs.release-type }}" in
  major) NEW_VERSION=$(echo $CURRENT_VERSION | awk -F. '{print $1+1".0.0"}') ;;
  minor) NEW_VERSION=$(echo $CURRENT_VERSION | awk -F. '{print $1"."$2+1".0"}') ;;
  patch) NEW_VERSION=$(echo $CURRENT_VERSION | awk -F. '{print $1"."$2"."$3+1}') ;;
esac
```

#### 2. Update Version

```bash
mvn versions:set -DnewVersion=$NEW_VERSION -DgenerateBackupPoms=false
```

Atualiza automaticamente:
- Root POM
- Todos os módulos
- Dependências inter-módulos

#### 3. Build and Deploy

```bash
mvn clean deploy -P release -DskipTests
```

**Profile Release (`-P release`):**
- Gera source JARs
- Gera javadoc JARs
- Publica no GitHub Packages

#### 4. Git Operations

```bash
# Commit changes
git add -A
git commit -m "chore: release version $NEW_VERSION"

# Create tag
git tag -a "v$NEW_VERSION" -m "Release version $NEW_VERSION"

# Push
git push origin main
git push origin "v$NEW_VERSION"
```

#### 5. GitHub Release

```bash
gh release create "v$NEW_VERSION" \
  --title "Release v$NEW_VERSION" \
  --generate-notes \
  --verify-tag
```

**Conteúdo:**
- Release notes geradas automaticamente
- Changelog desde último release
- Lista de commits e PRs

### Artifacts Published

**GitHub Packages:**
- `{module-name}-{version}.jar`
- `{module-name}-{version}-sources.jar`
- `{module-name}-{version}-javadoc.jar`

**URL Pattern:**
```
https://maven.pkg.github.com/marcusPrado02/java-commons
```

## Configuração Local

### GitHub Packages Authentication

**~/.m2/settings.xml:**
```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_TOKEN</password>
    </server>
  </servers>
</settings>
```

**Token Permissions:**
- `write:packages`
- `read:packages`
- `delete:packages` (opcional)

### Usar Dependências Publicadas

**pom.xml:**
```xml
<repositories>
  <repository>
    <id>github</id>
    <url>https://maven.pkg.github.com/marcusPrado02/java-commons</url>
  </repository>
</repositories>

<dependencies>
  <dependency>
    <groupId>com.example</groupId>
    <artifactId>commons-kernel-core</artifactId>
    <version>1.0.0</version>
  </dependency>
</dependencies>
```

## Monitoramento

### GitHub Actions

**Status Badges:**
```markdown
![CI](https://github.com/marcusPrado02/java-commons/workflows/CI/badge.svg)
![Release](https://github.com/marcusPrado02/java-commons/workflows/Release/badge.svg)
```

**Visualização:**
- Actions tab → Workflows
- Pull Requests → Checks
- Security tab → Dependabot alerts + SARIF results

### Métricas Importantes

**Build Health:**
- ✅ Taxa de sucesso dos builds
- ⏱️ Tempo médio de build
- 📊 Cobertura de código
- 🔒 Vulnerabilidades encontradas

**Thresholds Recomendados:**
- Build time: < 5 min para CI completo
- Taxa de sucesso: > 95%
- Cobertura: > 80% (line), > 75% (branch)
- CVEs críticos: 0

## Troubleshooting

### Build Falha no CI mas Passa Local

**Possíveis causas:**
1. Diferença de versão Java
2. Dependências não comitadas
3. Testes não determinísticos
4. Variáveis de ambiente

**Solução:**
```bash
# Simular ambiente CI localmente
mvn clean install -B

# Executar com mesmo Java do CI
sdk use java 21-tem
```

### Security Scan Falso Positivo

**Supressão em dependency-check-suppressions.xml:**
```xml
<suppress>
  <notes>Justificativa detalhada aqui</notes>
  <cve>CVE-2024-XXXXX</cve>
</suppress>
```

### Falha no Deploy

**Verificar:**
1. Token GitHub com permissões corretas
2. Branch protegida permite pushes
3. Versão não existe no registry

**Logs:**
```bash
gh run view --log
```

### Cache Corrompido

**Invalidar cache:**
1. Ir em Actions → Caches
2. Deletar cache corrompido
3. Próximo build recria

## Best Practices

### Commits

- ✅ Sempre executar `mvn verify` antes de push
- ✅ Testar com Java 21 e 23 localmente se possível
- ✅ Executar static analysis: `mvn verify -Dspotbugs.skip=false`

### Pull Requests

- ✅ Esperar CI passar antes de merge
- ✅ Revisar relatório de cobertura
- ✅ Check security scan results
- ✅ Resolver todos os comentários

### Releases

- ✅ Atualizar CHANGELOG.md antes
- ✅ Testar build de release localmente: `mvn clean install -P release`
- ✅ Verificar que não há SNAPSHOT dependencies
- ✅ Documentar breaking changes

### Manutenção

- 🔄 Atualizar actions versions mensalmente
- 🔄 Revisar suppressions trimestralmente
- 🔄 Renovar tokens antes de expirar
- 🔄 Monitorar tempo de build e otimizar

## Referências

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [Maven Release Plugin](https://maven.apache.org/maven-release/maven-release-plugin/)
- [GitHub Packages Maven](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry)
- [OWASP Dependency-Check](https://jeremylong.github.io/DependencyCheck/dependency-check-maven/)
- [Semantic Versioning](https://semver.org/)
