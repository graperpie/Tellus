/*
 * Decompiled with CFR 0.152.
 */
package net.irisshaders.iris.helpers;

import java.util.Objects;

public record Tri<X, Y, Z>(X first, Y second, Z third) {
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Tri)) {
            return false;
        }
        Tri tri = (Tri)obj;
        return Objects.equals(tri.first, this.first) && Objects.equals(tri.second, this.second) && Objects.equals(tri.third, this.third);
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (this.first == null ? 0 : this.first.hashCode());
        result = 31 * result + (this.second == null ? 0 : this.second.hashCode());
        result = 31 * result + (this.third == null ? 0 : this.third.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "First: " + this.first.toString() + " Second: " + this.second.toString() + " Third: " + this.third.toString();
    }
}

