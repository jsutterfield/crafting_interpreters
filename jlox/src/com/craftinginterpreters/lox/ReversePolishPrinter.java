package com.craftinginterpreters.lox;

//public class ReversePolishPrinter implements Expr.Visitor<String> {
//    String print(Expr expr) {
//        return expr.accept(this);
//    }
//
//    @Override
//    public String visitBinaryExpr(Expr.Binary expr) {
//        String left = expr.left.accept(this);
//        String right = expr.right.accept(this);
//        return left + " " + right + " " + expr.operator.lexeme;
//    }
//
//    @Override
//    public String visitGroupingExpr(Expr.Grouping expr) {
//        return expr.accept(this);
//    }
//
//    @Override
//    public String visitLiteralExpr(Expr.Literal expr) {
//        return expr.value == null ? "nil" : expr.value.toString();
//    }
//
//    @Override
//    public String visitUnaryExpr(Expr.Unary expr) {
//        return expr.right.accept(this);
//    }
//}
