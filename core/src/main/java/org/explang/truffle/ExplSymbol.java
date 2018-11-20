package org.explang.truffle;

/** Type object for a symbol. */
public class ExplSymbol {
  public final String name;

  public ExplSymbol(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return ":" + this.name;
  }
}
