# Análise Estática de Código

## Visão Geral

Este projeto está configurado com três ferramentas complementares de análise estática:

| Ferramenta | Versão | Objetivo | Relatório |
|------------|--------|----------|-----------|
| **SpotBugs** | 4.8.3 | Detectar bugs e vulnerabilidades | `target/spotbugsXml.xml` |
| **Checkstyle** | 10.12.7 | Verificar style de código | `target/checkstyle-result.xml` |
| **PMD** | 3.21.2 | Detectar code smells e duplicação | `target/pmd.xml`, `target/cpd.xml` |

## Estado Atual

⚠️ **Todas as ferramentas estão DESABILITADAS por padrão** para não bloquear o build durante desenvolvimento.

Para habilitá-las, use as propriedades Maven correspondentes.

## SpotBugs - Detecção de Bugs

### Descrição
SpotBugs analisa bytecode Java para encontrar bugs potenciais, incluindo:
- Bugs de concorrência e sincronização
- Problemas de performance
- Vulnerabilidades de segurança (via FindSecBugs)
- Código morto ou não utilizado
- Possíveis NullPointerExceptions

### Como usar

```bash
# Executar análise
./mvnw verify -Dspotbugs.skip=false

# Ver relatório HTML interativo
./mvnw spotbugs:gui -pl <módulo>

# Exemplo - analisar apenas um módulo
./mvnw verify -pl commons-kernel-errors -Dspotbugs.skip=false
```

### Configuração

**Arquivo**: `spotbugs-exclude.xml` na raiz do projeto

Exclusões configuradas:
- Records e classes imutáveis (EI_EXPOSE_REP)
- Campos de serialização em exceções (SE_BAD_FIELD)
- Catch genérico em processadores (REC_CATCH_EXCEPTION)

### Personalizar exclusões

Edite `spotbugs-exclude.xml`:

```xml
<Match>
    <Bug pattern="PATTERN_NAME"/>
    <Class name="~.*MyClass$"/>
</Match>
```

Padrões comuns:
- `EI_EXPOSE_REP` - Exposição de representação interna
- `SE_BAD_FIELD` - Problemas de serialização
- `REC_CATCH_EXCEPTION` - Catch de Exception genérico

## Checkstyle - Verificação de Estilo

### Descrição
Checkstyle verifica conformidade com padrões de código, baseado no **Google Java Style Guide**:
- Indentação e formatação
- Naming conventions
- Documentação Javadoc
- Complexidade de métodos
- Tamanho de linhas e arquivos

### Como usar

```bash
# Executar verificação
./mvnw verify -Dcheckstyle.skip=false

# Ver relatório
cat target/checkstyle-result.xml
```

### Configuração

**Arquivo**: `checkstyle-suppressions.xml` na raiz do projeto

**Regra base**: `google_checks.xml` (embutido no Checkstyle)

Supressões configuradas:
- JavaDoc relaxado em testes
- Classes de configuração Spring Boot
- Records e DTOs
- Arquivos Main (demos/exemplos)

### Personalizar supressões

Edite `checkstyle-suppressions.xml`:

```xml
<suppress checks="MissingJavadocType" files=".*MyClass\.java"/>
```

Checks comuns:
- `MissingJavadocType` - Javadoc faltando em classes
- `MissingJavadocMethod` - Javadoc faltando em métodos
- `LineLength` - Linha muito longa
- `NeedBraces` - Faltam chaves em if/for

## PMD - Code Smells e Duplicação

### Descrição
PMD analisa código-fonte Java para detectar:
- Code smells (código ruim mas que funciona)
- Código complexo demais
- Métodos muito longos
- Duplicação de código (CPD)
- Padrões antipattern

### Como usar

```bash
# Executar análise PMD + CPD (duplicação)
./mvnw verify -Dpmd.skip=false

# Ver relatório
cat target/pmd.xml
cat target/cpd.xml
```

### Configuração

**Ruleset**: `/rulesets/java/quickstart.xml` (embutido no PMD)

Principais categorias de regras:
- **Best Practices** - Melhores práticas gerais
- **Code Style** - Estilo e naming
- **Design** - Princípios de design
- **Documentation** - Comentários e documentação
- **Error Prone** - Padrões propensos a erro
- **Performance** - Problemas de performance

### Personalizar regras

Para customizar, crie `pmd-ruleset.xml` e atualize o POM:

```xml
<configuration>
    <rulesets>
        <ruleset>pmd-ruleset.xml</ruleset>
    </rulesets>
</configuration>
```

## Habilitando no CI/CD

Para habilitar análise estática no GitHub Actions, edite `.github/workflows/ci.yml`:

```yaml
- name: Maven verify with static analysis
  run: ./mvnw verify -Dspotbugs.skip=false -Dcheckstyle.skip=false -Dpmd.skip=false
```

⚠️ **Atenção**: Isso fará o build falhar se houver violações!

## Estratégia Recomendada

### Para Novos Módulos
✅ Habilite todas as ferramentas desde o início
```bash
# Adicionar ao POM do módulo
<properties>
    <spotbugs.skip>false</spotbugs.skip>
    <checkstyle.skip>false</checkstyle.skip>
    <pmd.skip>false</pmd.skip>
</properties>
```

### Para Módulos Existentes
📋 Adote gradualmente:

1. **SpotBugs primeiro** (bugs reais)
2. **Checkstyle depois** (estilo)  
3. **PMD por último** (code smells)

### Corrigindo Violações

```bash
# 1. Veja as violações de um módulo
./mvnw verify -pl <módulo> -Dspotbugs.skip=false

# 2. Veja detalhes no relatório
cat <módulo>/target/spotbugsXml.xml

# 3. Corrija o código ou adicione exclusões justificadas

# 4. Confirme que passou
./mvnw verify -pl <módulo> -Dspotbugs.skip=false
```

## Integração com IDEs

### IntelliJ IDEA

**SpotBugs**: Instale plugin SpotBugs
- File → Settings → Plugins → Marketplace → "SpotBugs"
- Analyze → Analyze with SpotBugs

**Checkstyle**: Instale plugin CheckStyle-IDEA  
- File → Settings → Plugins → Marketplace → "CheckStyle-IDEA"
- Configure: `google_checks.xml` + `checkstyle-suppressions.xml`

**PMD**: Instale plugin PMDPlugin
- File → Settings → Plugins → Marketplace → "PMDPlugin"

### VS Code

**Checkstyle**: Instale extensão "Checkstyle for Java"
- Configura automaticamente com projeto Maven

## Comandos Úteis

```bash
# Análise completa de um módulo
./mvnw verify -pl <módulo> -Dspotbugs.skip=false -Dcheckstyle.skip=false -Dpmd.skip=false

# Apenas análise (sem compilar)
./mvnw spotbugs:check checkstyle:check pmd:check -pl <módulo>

# Ver GUI do SpotBugs
./mvnw spotbugs:gui -pl <módulo>

# Gerar relatório HTML do PMD
./mvnw pmd:pmd -pl <módulo>
open <módulo>/target/site/pmd.html

# Verificar duplicação de código
./mvnw pmd:cpd -pl <módulo>
cat <módulo>/target/cpd.xml
```

## Troubleshooting

### Build muito lento
```bash
# Desabilite no desenvolvimento local
./mvnw verify

# Habilite apenas antes de commit
./mvnw verify -Dspotbugs.skip=false
```

### Muitas violações
```bash
# Analise módulo por módulo
./mvnw verify -pl commons-kernel-core -Dcheckstyle.skip=false

# Adicione supressões graduais aos arquivos XML
```

### Falsos positivos
Adicione exclusões justificadas nos arquivos de configuração com comentários explicando o motivo.

## Referências

- [SpotBugs Documentation](https://spotbugs.readthedocs.io/)
- [FindSecBugs Patterns](https://find-sec-bugs.github.io/bugs.htm)
- [Checkstyle Documentation](https://checkstyle.org/)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [PMD Rule Reference](https://pmd.github.io/pmd/pmd_rules_java.html)
