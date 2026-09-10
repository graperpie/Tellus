/*
 * Decompiled with CFR 0.152.
 */
package kroppeb.stareval.element.tree;

import kroppeb.stareval.element.AccessibleExpressionElement;

public record AccessExpressionElement(AccessibleExpressionElement base, String index) implements AccessibleExpressionElement
{
    @Override
    public String toString() {
        return "Access{" + String.valueOf(this.base) + "[" + this.index + "]}";
    }
}

