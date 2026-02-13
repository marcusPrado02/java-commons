package com.marcusprado02.commons.ports.secrets;

import java.util.Map;
import java.util.Optional;

/**
 * Port para gerenciamento de secrets em um secret store (Vault, Azure KeyVault, AWS Secrets
 * Manager, etc).
 *
 * <p>Responsabilidades:
 *
 * <ul>
 *   <li>Recuperar secrets de forma segura
 *   <li>Armazenar novos secrets
 *   <li>Deletar secrets
 *   <li>Suporte para versionamento
 *   <li>Listar secrets disponíveis
 * </ul>
 */
public interface SecretStorePort {

  /**
   * Recupera um secret pelo caminho/nome.
   *
   * @param key Chave do secret
   * @return Secret value se existe, empty caso contrário
   */
  Optional<SecretValue> get(SecretKey key);

  /**
   * Recupera uma versão específica de um secret.
   *
   * @param key Chave do secret
   * @param version Versão específica
   * @return Secret value se existe, empty caso contrário
   */
  Optional<SecretValue> get(SecretKey key, String version);

  /**
   * Armazena um novo secret ou atualiza existente.
   *
   * @param key Chave do secret
   * @param value Valor do secret
   * @return Versão do secret criado/atualizado
   */
  String put(SecretKey key, SecretValue value);

  /**
   * Armazena múltiplos key-value pairs como um único secret.
   *
   * <p>Útil para configurações estruturadas (ex: database credenciais).
   *
   * @param key Chave do secret
   * @param data Mapa de dados
   * @return Versão do secret criado
   */
  String put(SecretKey key, Map<String, String> data);

  /**
   * Deleta um secret permanentemente.
   *
   * @param key Chave do secret
   * @return true se deletado, false se não existia
   */
  boolean delete(SecretKey key);

  /**
   * Verifica se um secret existe.
   *
   * @param key Chave do secret
   * @return true se existe, false caso contrário
   */
  boolean exists(SecretKey key);

  /**
   * Lista todos os secrets disponíveis.
   *
   * @param prefix Prefixo para filtrar (opcional)
   * @return Lista de chaves de secrets
   */
  java.util.List<SecretKey> list(String prefix);
}
