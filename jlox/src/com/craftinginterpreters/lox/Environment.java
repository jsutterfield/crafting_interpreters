package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Environment {

    final Environment enclosing;
    private final Map<String, Object> initializedValues = new HashMap<>();
    private final Set<String> unInitializedValues = new HashSet<>();

    Environment() {
        enclosing = null;
    }

    Environment(Environment enclosing) {
        this.enclosing = enclosing;
    }

    Object get(Token name) {
        if (initializedValues.containsKey(name.lexeme)) {
            return initializedValues.get(name.lexeme);
        }

        if (unInitializedValues.contains(name.lexeme)) {
            throw new RuntimeError(name, "Accessing uninitialized variable '" + name.lexeme + "'.");
        }

        if (enclosing != null) {
            return enclosing.get(name);
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void defineWithValue(String name, Object value) {
        initializedValues.put(name, value);
    }

    public void defineWithoutValue(String lexeme) {
        unInitializedValues.add(lexeme);
    }

    void assign(Token name, Object value) {
        if (unInitializedValues.contains(name.lexeme)) {
            unInitializedValues.remove(name.lexeme);
            initializedValues.put(name.lexeme, value);

            return;
        }


        if (initializedValues.containsKey(name.lexeme)) {
            initializedValues.put(name.lexeme, value);
            return;
        }

        if (enclosing != null) {
            enclosing.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }
}
