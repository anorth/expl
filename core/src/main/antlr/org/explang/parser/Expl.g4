grammar Expl;
@header {
package org.explang.parser;
}

/* Parse rules */

expression: sum ;

sum: signed ((PLUS | MINUS) signed)* ;

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

LPAREN: '(' ;
RPAREN: ')' ;

INTEGER: [0-9]+ ;
FLOAT: [0-9]+ ('.' [0-9]*)? (E SIGN? [0-9]+)? ;

fragment E: ('e' | 'E') ;
fragment SIGN: ('+' | '-') ;

WHITESPACE: [ \r\n\t]+ -> skip ;
