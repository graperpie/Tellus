/*
 * Decompiled with CFR 0.152.
 */
package kroppeb.stareval.element.tree;

import kroppeb.stareval.element.ExpressionElement;
import kroppeb.stareval.parser.UnaryOp;

public record UnaryExpressionElement(UnaryOp op, ExpressionElement inner) implements ExpressionElement
{
    @Override
    public String toString() {
        return "UnaryExpr{" + String.valueOf(this.op) + " {" + String.valueOf(this.inner) + "} }";
    }
}

