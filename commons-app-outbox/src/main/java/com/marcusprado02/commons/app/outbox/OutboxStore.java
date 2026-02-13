package com.marcusprado02.commons.app.outbox;

import com.marcusprado02.commons.app.outbox.model.OutboxMessage;
import com.marcusprado02.commons.app.outbox.model.OutboxMessageId;
import com.marcusprado02.commons.app.outbox.model.OutboxStatus;
import java.util.List;

/**
 * @deprecated Use {@link com.marcusprado02.commons.app.outbox.port.OutboxRepositoryPort} instead.
 *     This interface will be removed in a future version.
 */
@Deprecated(since = "0.1.0", forRemoval = true)
public interface OutboxStore {

  /** Recupera todas as mensagens que estão no status fornecido. */
  List<OutboxMessage> fetchByStatus(OutboxStatus status);

  /** Atualiza o status da mensagem. */
  void updateStatus(OutboxMessageId id, OutboxStatus newStatus);

  /** Atualiza status e attempts (tentativas) da mensagem. */
  void updateStatus(OutboxMessageId id, OutboxStatus newStatus, int attempts);

  /** Persiste mensagem de outbox. */
  void append(OutboxMessage message);
}
