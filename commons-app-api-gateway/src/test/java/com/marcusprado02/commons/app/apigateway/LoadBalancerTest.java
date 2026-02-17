package com.marcusprado02.commons.app.apigateway;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcusprado02.commons.kernel.result.Result;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoadBalancerTest {

  @Test
  void roundRobin_shouldDistributeEvenly() {
    LoadBalancer lb = LoadBalancer.roundRobin();
    List<String> instances = List.of("instance1", "instance2", "instance3");

    Result<String> r1 = lb.choose(instances);
    Result<String> r2 = lb.choose(instances);
    Result<String> r3 = lb.choose(instances);
    Result<String> r4 = lb.choose(instances);

    assertThat(r1.getOrNull()).isEqualTo("instance1");
    assertThat(r2.getOrNull()).isEqualTo("instance2");
    assertThat(r3.getOrNull()).isEqualTo("instance3");
    assertThat(r4.getOrNull()).isEqualTo("instance1");
  }

  @Test
  void roundRobin_withEmptyList_shouldReturnError() {
    LoadBalancer lb = LoadBalancer.roundRobin();

    Result<String> result = lb.choose(List.of());

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("NO_INSTANCES");
  }

  @Test
  void random_shouldSelectFromList() {
    LoadBalancer lb = LoadBalancer.random();
    List<String> instances = List.of("instance1", "instance2", "instance3");

    Result<String> result = lb.choose(instances);

    assertThat(result.isOk()).isTrue();
    assertThat(instances).contains(result.getOrNull());
  }

  @Test
  void weightedRandom_shouldRespectWeights() {
    LoadBalancer lb = LoadBalancer.weightedRandom(List.of(90, 9, 1));
    List<String> instances = List.of("instance1", "instance2", "instance3");

    int[] counts = new int[3];
    for (int i = 0; i < 1000; i++) {
      Result<String> result = lb.choose(instances);
      String selected = result.getOrNull();
      counts[instances.indexOf(selected)]++;
    }

    // instance1 should be selected most often (roughly 90% of the time)
    assertThat(counts[0]).isGreaterThan(800);
    assertThat(counts[1]).isLessThan(150);
    assertThat(counts[2]).isLessThan(50);
  }

  @Test
  void weightedRandom_withMismatchedWeights_shouldReturnError() {
    LoadBalancer lb = LoadBalancer.weightedRandom(List.of(50, 50));
    List<String> instances = List.of("instance1", "instance2", "instance3");

    Result<String> result = lb.choose(instances);

    assertThat(result.isFail()).isTrue();
    assertThat(result.problemOrNull().code().value()).isEqualTo("WEIGHT_MISMATCH");
  }

  @Test
  void leastConnections_shouldSelectInstanceWithFewestConnections() {
    LoadBalancer lb = LoadBalancer.leastConnections();
    List<String> instances = List.of("instance1", "instance2", "instance3");

    // All instances start with 0 connections, should select first
    Result<String> r1 = lb.choose(instances);
    assertThat(r1.getOrNull()).isEqualTo("instance1");

    // instance1 now has 1 connection, should select instance2
    Result<String> r2 = lb.choose(instances);
    assertThat(r2.getOrNull()).isEqualTo("instance2");

    // Release connection from instance1
    if (lb instanceof LeastConnectionsLoadBalancer lclb) {
      lclb.releaseConnection("instance1");
    }

    // instance1 now has 0 connections again, should be selected
    Result<String> r3 = lb.choose(instances);
    assertThat(r3.getOrNull()).isEqualTo("instance1");
  }
}
