package org.explang.truffle.nodes;

import org.explang.truffle.Type;

/**
 * An abstract node with two children of the same type.
 */
abstract class BinaryNode extends ExpressionNode {
  @Child ExpressionNode left;
  @Child ExpressionNode right;

  BinaryNode(Type t, ExpressionNode left, ExpressionNode right) {
    super(t);
    left.checkType(t);
    right.checkType(t);
    this.left = left;
    this.right = right;
  }
}
