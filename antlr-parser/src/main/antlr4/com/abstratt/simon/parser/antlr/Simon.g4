grammar Simon;

program: declarations rootObjects EOF;

languageName: IDENT;

declarations: declaration*;

declaration: (languageDeclaration | importDeclaration);

languageDeclaration: '@language' IDENT;

importDeclaration: '@import' CHAR_LITERAL;

rootObjects: rootObject*;

rootObject: object;

object: objectHeader 
properties?
components?;

objectHeader: modifiers? objectClass objectName?;

modifiers: '[' modifier* ']';

modifier: simpleIdentifier;

objectClass: qualifiedIdentifier;

qualifiedIdentifier: simpleIdentifier identifierTail ? ;

identifierTail: '.' qualifiedIdentifier ;

simpleIdentifier: IDENT ;

objectName: IDENT;

objectNameRef: qualifiedIdentifier;

properties:
'('
    slot*
')';


components:
OPEN_BRACKET
    componentOrLink*
CLOSE_BRACKET;

componentOrLink: component | link;

component: 
    featureName? OPEN_BRACKET childObjects CLOSE_BRACKET;
    
link: 
    featureName? keyValueSep objectNameRef;
    
childObjects:
	childObject*;    
    
childObject:
    object;

slot: 
    featureName? keyValueSep slotValue;
    
keyValueSep: ':'|'=';    
    
featureName: IDENT;    

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
LINE_COMMENT: '//' ~[\r\n]* -> skip;

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

