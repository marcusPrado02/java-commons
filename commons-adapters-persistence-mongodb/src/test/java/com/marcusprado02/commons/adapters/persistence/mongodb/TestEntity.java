package com.marcusprado02.commons.adapters.persistence.mongodb;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "test_entities")
record TestEntity(@Id String id, String name, int age, String email, boolean active) {}
