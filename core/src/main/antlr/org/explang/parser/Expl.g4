grammar Expl;
@header {
package org.explang.parser;
}

/* Parse rules */

expression  : NUMBER '+' NUMBER ;

/* Lex rules */

NUMBER     : [0-9]+ ;
WHITESPACE : (' ' | '\t') -> skip ;
