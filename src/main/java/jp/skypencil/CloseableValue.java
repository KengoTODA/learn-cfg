package jp.skypencil;

import org.objectweb.asm.tree.analysis.Value;

/** A stateless {@link Value} implementation that holds state of {@link AutoCloseable} instance. */
final class CloseableValue implements Value {
  private final boolean closed;

  CloseableValue(boolean closed) {
    this.closed = closed;
  }

  @Override
  public int getSize() {
    return 1;
  }

  boolean isClosed() {
    return this.closed;
  }
}
