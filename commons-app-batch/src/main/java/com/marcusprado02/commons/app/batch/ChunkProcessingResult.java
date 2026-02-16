package com.marcusprado02.commons.app.batch;

/** Statistics and results from processing a chunk of items. */
public final class ChunkProcessingResult {

  private final int itemsRead;
  private final int itemsProcessed;
  private final int itemsFiltered;
  private final int itemsWritten;
  private final int itemsSkipped;
  private final boolean hasMoreItems;

  private ChunkProcessingResult(Builder builder) {
    this.itemsRead = builder.itemsRead;
    this.itemsProcessed = builder.itemsProcessed;
    this.itemsFiltered = builder.itemsFiltered;
    this.itemsWritten = builder.itemsWritten;
    this.itemsSkipped = builder.itemsSkipped;
    this.hasMoreItems = builder.hasMoreItems;
  }

  public static Builder builder() {
    return new Builder();
  }

  public int getItemsRead() {
    return itemsRead;
  }

  public int getItemsProcessed() {
    return itemsProcessed;
  }

  public int getItemsFiltered() {
    return itemsFiltered;
  }

  public int getItemsWritten() {
    return itemsWritten;
  }

  public int getItemsSkipped() {
    return itemsSkipped;
  }

  public boolean hasMoreItems() {
    return hasMoreItems;
  }

  public static final class Builder {
    private int itemsRead = 0;
    private int itemsProcessed = 0;
    private int itemsFiltered = 0;
    private int itemsWritten = 0;
    private int itemsSkipped = 0;
    private boolean hasMoreItems = true;

    private Builder() {}

    public Builder itemsRead(int itemsRead) {
      this.itemsRead = itemsRead;
      return this;
    }

    public Builder itemsProcessed(int itemsProcessed) {
      this.itemsProcessed = itemsProcessed;
      return this;
    }

    public Builder itemsFiltered(int itemsFiltered) {
      this.itemsFiltered = itemsFiltered;
      return this;
    }

    public Builder itemsWritten(int itemsWritten) {
      this.itemsWritten = itemsWritten;
      return this;
    }

    public Builder itemsSkipped(int itemsSkipped) {
      this.itemsSkipped = itemsSkipped;
      return this;
    }

    public Builder hasMoreItems(boolean hasMoreItems) {
      this.hasMoreItems = hasMoreItems;
      return this;
    }

    public ChunkProcessingResult build() {
      return new ChunkProcessingResult(this);
    }
  }
}
