grammar Expl;
@header {
package org.explang.parser;
}

/* Parse rules */

expression
  : let
  | sum ;

let: LET binding (COMMA binding)* IN expression;
binding: symbol EQ expression;

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

LET: 'let';
IN: 'in';

EQ: '=';

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
