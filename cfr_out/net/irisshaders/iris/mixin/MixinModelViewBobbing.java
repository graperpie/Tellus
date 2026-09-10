/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.class_1294
 *  net.minecraft.class_310
 *  net.minecraft.class_3532
 *  net.minecraft.class_4184
 *  net.minecraft.class_4587
 *  net.minecraft.class_757
 *  net.minecraft.class_9779
 *  org.joml.Matrix4f
 *  org.joml.Matrix4fc
 *  org.joml.Quaternionfc
 *  org.joml.Vector3f
 *  org.joml.Vector3fc
 *  org.spongepowered.asm.mixin.Final
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.Shadow
 *  org.spongepowered.asm.mixin.Unique
 *  org.spongepowered.asm.mixin.injection.At
 *  org.spongepowered.asm.mixin.injection.Inject
 *  org.spongepowered.asm.mixin.injection.ModifyArg
 *  org.spongepowered.asm.mixin.injection.Redirect
 *  org.spongepowered.asm.mixin.injection.callback.CallbackInfo
 */
package net.irisshaders.iris.mixin;

import net.irisshaders.iris.Iris;
import net.minecraft.class_1294;
import net.minecraft.class_310;
import net.minecraft.class_3532;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_757;
import net.minecraft.class_9779;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={class_757.class})
public abstract class MixinModelViewBobbing {
    @Shadow
    @Final
    class_310 field_4015;
    @Shadow
    @Final
    private class_4184 field_18765;
    @Shadow
    private int field_47130;
    @Unique
    private Matrix4fc bobbingEffectsModel;
    @Unique
    private boolean areShadersOn;

    @Shadow
    protected abstract void method_3186(class_4587 var1, float var2);

    @Shadow
    protected abstract void method_3198(class_4587 var1, float var2);

    @Inject(method={"renderLevel"}, at={@At(value="HEAD")})
    private void iris$saveShadersOn(class_9779 deltaTracker, CallbackInfo ci) {
        this.areShadersOn = Iris.isPackInUseQuick();
    }

    @ModifyArg(method={"renderLevel"}, index=0, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
    private class_4587 iris$separateViewBobbing(class_4587 stack) {
        if (!this.areShadersOn) {
            return stack;
        }
        stack.method_22903();
        stack.method_23760().method_23761().identity();
        return stack;
    }

    @Redirect(method={"renderLevel"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
    private void iris$stopBobbing(class_757 instance, class_4587 pGameRenderer0, float pFloat1) {
        if (!this.areShadersOn) {
            this.method_3186(pGameRenderer0, pFloat1);
        }
    }

    @Redirect(method={"renderLevel"}, at=@At(value="INVOKE", target="Lnet/minecraft/client/renderer/GameRenderer;bobHurt(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
    private void iris$saveBobbing(class_757 instance, class_4587 pGameRenderer0, float pFloat1) {
        if (!this.areShadersOn) {
            this.method_3198(pGameRenderer0, pFloat1);
        }
    }

    @Redirect(method={"renderLevel"}, at=@At(value="INVOKE", target="Ljava/lang/Double;floatValue()F"))
    private float iris$disableConfusionWithShaders(Double instance) {
        return this.areShadersOn ? 0.0f : instance.floatValue();
    }

    @Redirect(method={"renderLevel"}, at=@At(value="INVOKE", target="Lorg/joml/Matrix4f;rotation(Lorg/joml/Quaternionfc;)Lorg/joml/Matrix4f;", remap=false))
    private Matrix4f iris$applyBobbingToModelView(Matrix4f instance, Quaternionfc quat, class_9779 deltaTracker) {
        if (!this.areShadersOn) {
            instance.rotation(quat);
            return instance;
        }
        class_4587 stack = new class_4587();
        stack.method_23760().method_23761().set((Matrix4fc)instance);
        float tickDelta = this.field_18765.method_55437();
        this.method_3198(stack, tickDelta);
        if (((Boolean)this.field_4015.field_1690.method_42448().method_41753()).booleanValue()) {
            this.method_3186(stack, tickDelta);
        }
        instance.set((Matrix4fc)stack.method_23760().method_23761());
        float f = deltaTracker.method_60637(false);
        float h = ((Double)this.field_4015.field_1690.method_42453().method_41753()).floatValue();
        float i = class_3532.method_16439((float)f, (float)this.field_4015.field_1724.field_44912, (float)this.field_4015.field_1724.field_44911) * h * h;
        if (i > 0.0f) {
            int j = this.field_4015.field_1724.method_6059(class_1294.field_5916) ? 7 : 20;
            float k = 5.0f / (i * i + 5.0f) - i * 0.04f;
            k *= k;
            Vector3f vector3f = new Vector3f(0.0f, class_3532.field_15724 / 2.0f, class_3532.field_15724 / 2.0f);
            float l = ((float)this.field_47130 + f) * (float)j * ((float)Math.PI / 180);
            instance.rotate(l, (Vector3fc)vector3f);
            instance.scale(1.0f / k, 1.0f, 1.0f);
            instance.rotate(-l, (Vector3fc)vector3f);
        }
        instance.rotate(quat);
        return instance;
    }
}

