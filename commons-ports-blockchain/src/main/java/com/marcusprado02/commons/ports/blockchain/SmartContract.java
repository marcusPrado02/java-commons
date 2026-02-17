package com.marcusprado02.commons.ports.blockchain;

/**
 * Represents a deployed smart contract.
 *
 * @param address contract address
 * @param abi contract Application Binary Interface (JSON)
 * @param bytecode contract bytecode (optional)
 */
public record SmartContract(String address, String abi, String bytecode) {

  /**
   * Creates a builder for SmartContract.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for SmartContract. */
  public static class Builder {
    private String address;
    private String abi;
    private String bytecode;

    public Builder address(String address) {
      this.address = address;
      return this;
    }

    public Builder abi(String abi) {
      this.abi = abi;
      return this;
    }

    public Builder bytecode(String bytecode) {
      this.bytecode = bytecode;
      return this;
    }

    public SmartContract build() {
      return new SmartContract(address, abi, bytecode);
    }
  }
}
