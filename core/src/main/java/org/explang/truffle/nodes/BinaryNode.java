package org.explang.truffle.nodes;

/**
 * An abstract node with two children of the same type.
 */
abstract class BinaryNode<T> extends ExpressionNode {
  @Child ExpressionNode left;
  @Child ExpressionNode right;

  BinaryNode(ExpressionNode left, ExpressionNode right) {
    this.left = left;
    this.right = right;
  }
}
