/*
 * Decompiled with CFR 0.152.
 */
package kroppeb.stareval.parser;

public record BinaryOp(String name, int priority) {
    @Override
    public String toString() {
        return this.name + "{" + this.priority + "}";
    }
}

