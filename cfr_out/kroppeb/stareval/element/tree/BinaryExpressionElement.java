/*
 * Decompiled with CFR 0.152.
 */
package kroppeb.stareval.element.tree;

import kroppeb.stareval.element.ExpressionElement;
import kroppeb.stareval.parser.BinaryOp;

public record BinaryExpressionElement(BinaryOp op, ExpressionElement left, ExpressionElement right) implements ExpressionElement
{
    @Override
    public String toString() {
        return "BinaryExpr{ {" + String.valueOf(this.left) + "} " + String.valueOf(this.op) + " {" + String.valueOf(this.right) + "} }";
    }
}

