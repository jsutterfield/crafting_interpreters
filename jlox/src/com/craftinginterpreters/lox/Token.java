package com.craftinginterpreters.lox;

public class Token {
    final private TokenType type;
    final private String lexeme;
    final private Object literal;
    final private int line;

    Token(TokenType type, String lexeme, Object literal, int line) {
        this.type = type;
        this.lexeme = lexeme;
        this.literal = literal;
        this.line = line;
    }

    @Override
    public String toString() {
        return type + " "  + lexeme + " " + literal;
    }
}
