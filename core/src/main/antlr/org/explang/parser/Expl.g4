grammar Expl;
@header {
package org.explang.parser;
}

/* Parse rules */

expression: sum ;

sum: product ((PLUS | MINUS) product)* ;
product: factor ((TIMES | DIV) factor)* ;
factor: signed (POW signed)* ;

signed
   : PLUS signed
   | MINUS signed
   | atom
   ;

atom
   : number
   | LPAREN expression RPAREN
   ;

number: INTEGER | FLOAT ;

/* Lex rules */

PLUS: '+' ;
MINUS: '-' ;
TIMES: '*' ;
DIV: '/' ;
POW: '^' ;

LPAREN: '(' ;
RPAREN: ')' ;

INTEGER: DIGITS ;
FLOAT: DIGITS ('.' [0-9]*)? (E SIGN? DIGITS)? ;

fragment E: ('e' | 'E') ;
fragment SIGN: ('+' | '-') ;
fragment DIGITS: [0-9]+ ;

WHITESPACE: [ \r\n\t]+ -> skip ;
