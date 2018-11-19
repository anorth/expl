package org.explang.truffle.nodes;

/**
 * An abstract node with two children of the same type.
 */
abstract class BinaryNode<T> extends ExpressionNode<T> {
  @Child ExpressionNode<T> left;
  @Child ExpressionNode<T> right;

  BinaryNode(ExpressionNode<T> left, ExpressionNode<T> right) {
    this.left = left;
    this.right = right;
  }
}
