package parser;

import java_cup.runtime.Symbol;
import java_cup.runtime.ComplexSymbolFactory;
import java_cup.runtime.ComplexSymbolFactory.Location;

import interpreter.Interpreter;

%%

%public
%class Lexer
%cup
%implements sym
%char
%line
%column

%{
    StringBuffer string = new StringBuffer();
    public Lexer(java.io.Reader in, ComplexSymbolFactory sf){
	this(in);
	symbolFactory = sf;
    }
    ComplexSymbolFactory symbolFactory;

  private Symbol symbol(String name, int sym) {
      return symbolFactory.newSymbol(name, sym, new Location(yyline+1,yycolumn+1,yyline+1), new Location(yyline+1,yycolumn+yylength(),yycolumn+1));
  }
  
  private Symbol symbol(String name, int sym, Object val) {
      Location left = new Location(yyline + 1, yycolumn + 1, yyline + 1);
      Location right = new Location(yyline + 1, yycolumn + yylength(), yycolumn + 1);
      return symbolFactory.newSymbol(name, sym, left, right, val);
  } 
  /*private Symbol symbol(String name, int sym, Object val, int buflength) {
      Location left = new Location(yyline + 1, yycolumn + yylength() - buflength, yychar + yylength() - buflength);
      Location right = new Location(yyline + 1, yycolumn + yylength(), yychar + yylength());
      return symbolFactory.newSymbol(name, sym, left, right, val);
  }*/      
  private void error(String message) {
    System.out.println("Error at line "+ (yyline + 1) + ", column " + (yycolumn + 1) + " : " + message);
  }
%} 

%eofval{
     return symbolFactory.newSymbol("EOF", EOF, new Location(yyline + 1, yycolumn + 1, yychar), new Location(yyline + 1, yycolumn + 1, yychar + 1));
%eofval}


IntLiteral = 0 | [1-9][0-9]*


new_line = \r|\n|\r\n;

white_space = {new_line} | [ \t\f]


Identifier = [_a-zA-Z] [_a-zA-Z0-9]*



%%

<YYINITIAL>{
/* int literals */

{IntLiteral} { return symbol("Intconst", INTCONST, Long.parseLong(yytext())); }


/* separators */
"+"               { return symbol("+",  PLUS); }
"-"               { return symbol("-",  MINUS); }
"*"               { return symbol("*",  TIMES); }
"("               { return symbol("(",  LPAREN); }
")"               { return symbol(")",  RPAREN); }
";"                 {return symbol(";", SEMICOLON);}
"||"                {return symbol("||", OR);}
"&&"                {return symbol("&&", AND);}
"<="                {return symbol("<=", LESSTHANOREQUAL);}
">="                {return symbol(">=", GREATERTHANOREQUAL);}
"="                 {return symbol("=", EQUAL);}
"<"                 {return symbol("<", LESSTHAN);}
">"                 {return symbol(">", GREATERTHAN);}
"=="                {return symbol("==",EQUALTO);}
"!="                {return symbol("!=", NOTEQUALTO);}
"!"                 {return symbol("!", NOT);}
"{"                 {return symbol("{", LEFTCURLYBRACE);}
"}"                 {return symbol("}", RIGHTCURLYBRACE);}
","                 {return symbol(",", COMMA);}
"return"            {return symbol("return", RETURN); }
"if"                {return symbol("if", IF);}
"int"               {return symbol("int",INT);}
"Q"                 {return symbol("Q",Q);}
"print"             {return symbol("print",PRINT);}
"Ref"               {return symbol("Ref",REF);}
"while"             {return symbol("while",WHILE);}
"else"              {return symbol("else", ELSE);}
"."                 {return symbol(".",DOT);}

/* identifiers */
{Identifier} {return symbol("Identifier", IDENT, yytext());}


/* comments */
"/*" [^*] ~"*/" | "/*" "*"+ "/"
                  { /* ignore comments */ }

{white_space}     { /* ignore */ }

}

/* error fallback */
[^]               { /*error("Illegal character <" + yytext() + ">");*/ Interpreter.fatalError("Illegal character <" + yytext() + ">", Interpreter.EXIT_PARSING_ERROR); }
