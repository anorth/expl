package org.explang.truffle.nodes;

import org.explang.syntax.Type;

/**
 * An abstract node with two children of the same type.
 */
abstract class BinaryNode extends ExpressionNode {
  private final String name;
  @Child ExpressionNode left;
  @Child ExpressionNode right;

  BinaryNode(Type t, String name, ExpressionNode left, ExpressionNode right) {
    super(t);
    assert left.type().equals(right.type()) :
        "Mismatched operand types to " + name + ": " + left.type() + ", " + right.type();
    this.name = name;
    this.left = left;
    this.right = right;
  }

  @Override
  public String toString() { return name + "(" + this.left + "," + this.right + ")"; }
}
