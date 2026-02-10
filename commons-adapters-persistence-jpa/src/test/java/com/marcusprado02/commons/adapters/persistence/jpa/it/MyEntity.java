package com.marcusprado02.commons.adapters.persistence.jpa.it;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class MyEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  public MyEntity() {}

  public MyEntity(Long id, String name) {
    this.id = id;
    this.name = name;
  }

  public Long id() {
    return id;
  }

  public String name() {
    return name;
  }
}
