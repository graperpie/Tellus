/*    */ package com.toweringheights.client.mixin;
/*    */ 
/*    */ import com.toweringheights.ToweringHeightsConfig;
/*    */ import net.fabricmc.api.EnvType;
/*    */ import net.fabricmc.api.Environment;
/*    */ import net.minecraft.class_769;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.Shadow;
/*    */ import org.spongepowered.asm.mixin.injection.At;
/*    */ import org.spongepowered.asm.mixin.injection.Inject;
/*    */ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ @Environment(EnvType.CLIENT)
/*    */ @Mixin({class_769.class})
/*    */ public class ViewAreaOptMixin
/*    */ {
/*    */   @Shadow
/*    */   protected int field_4149;
/*    */   
/*    */   @Inject(method = {"method_3325"}, at = {@At("RETURN")})
/*    */   private void capVerticalSections(int viewDistance, CallbackInfo ci) {
/* 27 */     int maxY = ToweringHeightsConfig.get().getMaxRenderSectionsY();
/* 28 */     if (this.field_4149 > maxY)
/* 29 */       this.field_4149 = maxY; 
/*    */   }
/*    */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\client\mixin\ViewAreaOptMixin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */