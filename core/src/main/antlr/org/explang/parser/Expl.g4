grammar Expl;
@header {
package org.explang.parser;
}

/* Parse rules */

// Thank goodness ANTLR 4 supports direct left recursion.
expression
   : expression arguments                              # CallEx
   | expression index                                  # IndexEx
   | (PLUS | MINUS | NOT) expression                   # UnaryEx
   | <assoc=right> expression POW expression           # ExponentiationEx
   | expression (TIMES | DIV) expression               # MultiplicativeEx
   | expression (PLUS | MINUS) expression              # AdditiveEx
   // The * symbol means "all". I would really like to remove it but couldn't figure out parsing
   // of : as a binary *or* ternary operator.
   | expression COLON (expression|TIMES) COLON expression   # StepRangeEx
   | TIMES COLON (expression|TIMES) COLON expression        # RightStepRangeEx
   | expression COLON (expression|TIMES)                    # RangeEx
   | TIMES COLON (expression|TIMES)                         # RightRangeEx
   | expression (LT | LE | GT | GE) expression         # ComparativeEx
   | expression (EQ | NEQ) expression                  # EqualityEx
   | expression (AND | OR | XOR) expression            # ConjunctionEx
   | literal                                           # LiteralEx
   | symbol                                            # SymbolEx
   | IF expression THEN expression ELSE expression     # IfEx
   | LET binding (COMMA binding)* COMMA? IN expression # LetEx
   | LPAREN expression RPAREN                          # ParenthesizedEx
   | lambdaParameters ARROW expression                 # LambdaEx
   ;

arguments: LPAREN (expression (COMMA expression)*)? RPAREN ;

index
  : LBRACKET expression RBRACKET;

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
  // (->double), (double->double), (double,bool->double)
  : LPAREN (typeExpression (COMMA typeExpression)*)? ARROW typeExpression RPAREN
  // double[], (double->double)[][]
  | typeExpression LBRACKET RBRACKET
  | typePrimitive
  ;

typePrimitive
  : BOOL
  | LONG
  | DOUBLE
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
LBRACKET: '[';
RBRACKET: ']';
COMMA: ',' ;
COLON: ':' ;

POW: '^' ;
TIMES: '*' ;
DIV: '/' ;
PLUS: '+' ;
MINUS: '-' ;
LT: '<' ;
LE: '<=' ;
GT: '>' ;
GE: '>=' ;
EQ: '==' ;
NEQ: '<>' ;
AND: 'and' ;
OR: 'or' ;
XOR: 'xor' ;
NOT: 'not' ;


TRUE: 'true';
FALSE: 'false';

LONG: 'long';
BOOL: 'bool';
DOUBLE: 'double';

INTEGER: DIGITS ;
FLOAT: DIGITS ('.' [0-9]*)? (E SIGN? DIGITS)? ;

fragment E: ('e' | 'E') ;
fragment SIGN: ('+' | '-') ;
fragment DIGITS: [0-9]+ ;

IDENTIFIER: IDENT_START IDENT_CHAR* ;

fragment IDENT_START: ('a' .. 'z') | ('A' .. 'Z') | '_' ;
fragment IDENT_CHAR: IDENT_START | ('0' .. '9') ;

WHITESPACE: [ \r\n\t]+ -> skip ;
