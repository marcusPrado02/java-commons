package com.marcusprado02.commons.app.workflow;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.Map;

/**
 * Executes workflow actions.
 *
 * <p>Implement this interface to define custom actions for workflow states.
 */
@FunctionalInterface
public interface ActionExecutor {

  /**
   * Executes an action with the given context.
   *
   * @param actionName action name
   * @param context workflow context
   * @return the result of the action execution
   */
  Result<Map<String, Object>> execute(String actionName, Map<String, Object> context);
}
