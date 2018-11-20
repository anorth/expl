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
   | fcall
   | atom
   ;

atom
   : number
   | symbol
   | LPAREN expression RPAREN
   ;

number: INTEGER | FLOAT ;
symbol: IDENTIFIER ;

fcall: atom LPAREN expression (COMMA expression)* RPAREN ;

/* Lex rules */

IDENTIFIER: IDENT_START IDENT_CHAR* ;

fragment IDENT_START: ('a' .. 'z') | ('A' .. 'Z') | '_' ;
fragment IDENT_CHAR: IDENT_START | ('0' .. '9') ;

PLUS: '+' ;
MINUS: '-' ;
TIMES: '*' ;
DIV: '/' ;
POW: '^' ;

LPAREN: '(' ;
RPAREN: ')' ;
COMMA: ',' ;

INTEGER: DIGITS ;
FLOAT: DIGITS ('.' [0-9]*)? (E SIGN? DIGITS)? ;

fragment E: ('e' | 'E') ;
fragment SIGN: ('+' | '-') ;
fragment DIGITS: [0-9]+ ;

WHITESPACE: [ \r\n\t]+ -> skip ;
