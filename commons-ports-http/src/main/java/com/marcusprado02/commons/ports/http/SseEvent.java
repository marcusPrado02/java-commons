package com.marcusprado02.commons.ports.http;

import java.util.Optional;

public record SseEvent(Optional<String> id, Optional<String> event, String data) {

  public SseEvent {
    id = (id == null) ? Optional.empty() : id;
    event = (event == null) ? Optional.empty() : event;
    data = (data == null) ? "" : data;
  }
}
