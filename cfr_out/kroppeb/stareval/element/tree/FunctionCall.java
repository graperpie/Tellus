/*
 * Decompiled with CFR 0.152.
 */
package kroppeb.stareval.element.tree;

import java.util.List;
import kroppeb.stareval.element.ExpressionElement;

public record FunctionCall(String id, List<? extends ExpressionElement> args) implements ExpressionElement
{
    @Override
    public String toString() {
        return "FunctionCall{" + this.id + " {" + String.valueOf(this.args) + "} }";
    }
}

