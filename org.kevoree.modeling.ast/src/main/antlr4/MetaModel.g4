grammar MetaModel;

fragment ESC :   '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE : 'u' HEX HEX HEX HEX ;
fragment HEX : [0-9a-fA-F] ;

STRING :  '"' (ESC | ~["\\])* '"' | '\'' (ESC | ~["\\])* '\'' ;

IDENT : [a-zA-Z_][a-zA-Z_0-9]*;

NUMBER : [\-]?[0-9]+'.'?[0-9]*;

TYPE_NAME : [0-9a-zA-Z_]+[0-9a-zA-Z_.]*;

metamodel: annotationDeclr* decl*;

decl : enumDeclr | classDeclr;

enumDeclr :
    'enum' TYPE_NAME '{' IDENT (',' IDENT)* '}';

classDeclr :
    'class' TYPE_NAME classParentDeclr? '{' annotationDeclr* (attributeDeclaration | referenceDeclaration | dependencyDeclaration | inputDeclaration | outputDeclaration)* '}';

classParentDeclr :
    'extends' TYPE_NAME (',' TYPE_NAME )*;

attributeDeclaration : 'att' IDENT ':' attributeType annotationDeclr*;

attributeType : 'String' | 'Double' | 'Long' | 'Continuous' | 'Int' | 'Bool' | TYPE_NAME;

referenceDeclaration : ('ref' | 'ref*') IDENT ':' TYPE_NAME annotationDeclr*;

dependencyDeclaration : 'dependency' IDENT ':' TYPE_NAME;

inputDeclaration : 'input' IDENT STRING;

outputDeclaration : 'output' IDENT ':' attributeType;

annotationDeclr : 'with' IDENT (NUMBER|STRING)?;

WS : [ \t\r\n]+ -> skip ; // skip spaces, tabs, newlines
