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
   | expression (LT | LE | GT | GE) expression         # ComparativeEx
   | expression (EQ | NEQ) expression                  # EqualityEx
   | literal                                           # LiteralEx
   | symbol                                            # SymbolEx
   | IF expression THEN expression ELSE expression     # IfEx
   | LET binding (COMMA binding)* IN expression        # LetEx
   | LPAREN expression RPAREN                          # ParenthesizedEx
   | lambdaParameters ARROW expression                 # LambdaEx
   ;

arguments: LPAREN (expression (COMMA expression)*)? RPAREN ;

binding
  : symbol ASSIGN expression
  | symbol formalParameters ASSIGN expression // Sugar for function bindings
  ;

lambdaParameters
  : parameter // Sugar for single-parameter lambdas with no return type annotation
  | formalParameters
  ;

// The return type annotation is optional (but semantically necessary for recursive functions).
formalParameters: LPAREN (parameter (COMMA parameter)*)? RPAREN typeAnnotation? ;

parameter: symbol typeAnnotation ;

typeAnnotation: COLON typeExpression ;

typeExpression
  // TODO: function types
  : typePrimitive;

typePrimitive
  : DOUBLE
  | BOOL
  ;

literal
  : number
  | bool
  ;

symbol: IDENTIFIER ;
number: INTEGER | FLOAT ;
bool: TRUE | FALSE ;

/* Lex rules */

// N.B. Order of lex rules affects precedence so, keywords and literals must come before
// identifiers.

LET: 'let';
IN: 'in';
IF: 'if';
THEN: 'then';
ELSE: 'else';

ASSIGN: '=';
ARROW: '->';

LPAREN: '(' ;
RPAREN: ')' ;
COMMA: ',' ;
COLON: ':' ;

POW: '^' ;
TIMES: '*' ;
DIV: '/' ;
PLUS: '+' ;
MINUS: '-' ;
LT: '<';
LE: '<=';
GT: '>';
GE: '>=';
EQ: '==';
NEQ: '<>';

TRUE: 'true';
FALSE: 'false';

DOUBLE: 'double';
BOOL: 'bool';

INTEGER: DIGITS ;
FLOAT: DIGITS ('.' [0-9]*)? (E SIGN? DIGITS)? ;

fragment E: ('e' | 'E') ;
fragment SIGN: ('+' | '-') ;
fragment DIGITS: [0-9]+ ;

IDENTIFIER: IDENT_START IDENT_CHAR* ;

fragment IDENT_START: ('a' .. 'z') | ('A' .. 'Z') | '_' ;
fragment IDENT_CHAR: IDENT_START | ('0' .. '9') ;

WHITESPACE: [ \r\n\t]+ -> skip ;
