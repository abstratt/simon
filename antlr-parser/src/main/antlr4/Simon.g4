grammar Simon;

@header {
    package com.abstratt.simon.parser.antlr;
}

program: (rootObject | scopedRootObjects) EOF;

scopedRootObjects: 
'@' languageName OPEN_BRACKET rootObjects CLOSE_BRACKET;

languageName: IDENT; 

rootObjects: rootObject*;

rootObject: object;

object: objectHeader 
properties?
components?;

objectHeader: objectClass objectName?;

objectClass: qualifiedIdentifier;

qualifiedIdentifier: simpleIdentifier identifierTail ? ;

identifierTail: '.' qualifiedIdentifier ;

simpleIdentifier: IDENT ;

objectName: IDENT;

properties:
'('
    slot*
')';


components:
OPEN_BRACKET
    component*
CLOSE_BRACKET;

component: 
    compositionName? OPEN_BRACKET childObjects CLOSE_BRACKET;
    
childObjects:
	childObject*;    
    
childObject:
    object;

compositionName: IDENT;

slot: 
    slotName? '=' slotValue;
    
slotName: IDENT;    

slotValue: literal;
    
query:
    expression EOF;

expression:
    nestedExpression | compositeExpression | comparisonExpression ;

compositeExpression:
    resolvedExpression (booleanOperator resolvedExpression)+;

resolvedExpression:
    comparisonExpression | nestedExpression;

nestedExpression: '(' expression ')';

comparisonExpression:
    property comparisonOperator literal;

comparisonOperator:
    EQ | NE | LT | LE | GT | GE;

booleanOperator:
    AND | OR;

property: IDENT;

WHITESPACE: [ \t\r\n]-> skip;


AND: 'AND';

OR: 'OR';

EQ: 'eq';

NE: 'ne';

LT: 'lt';

LE: 'le';

GT: 'gt';

GE: 'ge';

OPEN_BRACKET: '{';
CLOSE_BRACKET: '}';


literal:
   CHAR_LITERAL | NUM_LITERAL | enumLiteral | recordLiteral;
   
enumLiteral: IDENT;   

recordLiteral: '#' properties ;

IDENT : ('a'..'z'|'A'..'Z') ('a'..'z'|'A'..'Z'|'0'..'9'|'_')*;

CHAR_LITERAL:
    '\'' ~ ['\r\n]* '\'';

NUM_LITERAL:
   ('0' .. '9') + ('.' ('0' .. '9') +)?;

