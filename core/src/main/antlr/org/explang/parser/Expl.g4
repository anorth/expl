grammar Expl;
@header {
package org.explang.parser;
}

/* Parse rules */

// Thank goodness ANTLR 4 supports direct left recursion.
expression
   : expression arguments                              # CallEx
   | PLUS expression                                   # UnaryPlusEx
   | MINUS expression                                  # UnaryMinusEx
   | <assoc=right> expression POW expression           # ExponentiationEx
   | expression (TIMES | DIV) expression               # MultiplicativeEx
   | expression (PLUS | MINUS) expression              # AdditiveEx
   | symbol                                            # SymbolEx
   | literal                                           # LiteralEx
   | LET binding (COMMA binding)* IN expression        # LetEx
   | LPAREN expression RPAREN                          # ParenthesizedEx
   | lambdaParameters ARROW expression                 # LambdaEx
   ;

arguments: LPAREN (expression (COMMA expression)*)? RPAREN ;

binding
  : symbol EQ expression
  | symbol formalParameters EQ expression // Sugar for function bindings
  ;

lambdaParameters
  : symbol // Sugar for single-parameter lambdas
  | formalParameters
  ;

formalParameters: LPAREN (symbol (COMMA symbol)*)? RPAREN ;

literal
  : number ;

number: INTEGER | FLOAT ;
symbol: IDENTIFIER ;

/* Lex rules */

LET: 'let';
IN: 'in';

EQ: '=';
ARROW: '->';

LPAREN: '(' ;
RPAREN: ')' ;
COMMA: ',' ;

PLUS: '+' ;
MINUS: '-' ;
TIMES: '*' ;
DIV: '/' ;
POW: '^' ;

IDENTIFIER: IDENT_START IDENT_CHAR* ;

fragment IDENT_START: ('a' .. 'z') | ('A' .. 'Z') | '_' ;
fragment IDENT_CHAR: IDENT_START | ('0' .. '9') ;

INTEGER: DIGITS ;
FLOAT: DIGITS ('.' [0-9]*)? (E SIGN? DIGITS)? ;

fragment E: ('e' | 'E') ;
fragment SIGN: ('+' | '-') ;
fragment DIGITS: [0-9]+ ;

WHITESPACE: [ \r\n\t]+ -> skip ;
