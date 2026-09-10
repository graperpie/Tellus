/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.llamalad7.mixinextras.injector.wrapoperation.Operation
 *  com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation
 *  net.minecraft.class_1297
 *  net.minecraft.class_638
 *  net.minecraft.class_761
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.injection.At
 */
package net.irisshaders.batchedentityrendering.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import net.minecraft.class_1297;
import net.minecraft.class_638;
import net.minecraft.class_761;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value={class_761.class}, priority=999)
public class MixinLevelRenderer_EntityListSorting {
    @Shadow
    private class_638 field_4085;

    @WrapOperation(method={"renderLevel"}, at={@At(value="INVOKE", target="Ljava/lang/Iterable;iterator()Ljava/util/Iterator;")})
    private Iterator<class_1297> batchedentityrendering$sortEntityList(Iterable<class_1297> instance, Operation<Iterator<class_1297>> original) {
        this.field_4085.method_16107().method_15396("sortEntityList");
        HashMap sortedEntities = new HashMap();
        ArrayList entities = new ArrayList();
        ((Iterator)original.call(new Object[]{instance})).forEachRemaining(entity -> sortedEntities.computeIfAbsent(entity.method_5864(), entityType -> new ArrayList(32)).add(entity));
        sortedEntities.values().forEach(entities::addAll);
        this.field_4085.method_16107().method_15407();
        return entities.iterator();
    }
}

