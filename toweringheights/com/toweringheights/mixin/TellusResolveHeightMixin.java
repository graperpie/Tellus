/*    */ package com.toweringheights.mixin;
/*    */ 
/*    */ import com.toweringheights.ToweringHeightsConfig;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.injection.Constant;
/*    */ import org.spongepowered.asm.mixin.injection.ModifyConstant;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ @Mixin(targets = {"com/yucareux/tellus/worldgen/EarthGeneratorSettings"}, remap = false)
/*    */ public class TellusResolveHeightMixin
/*    */ {
/*    */   @ModifyConstant(method = {"resolveHeightLimits"}, constant = {@Constant(intValue = -2032)}, require = 0)
/*    */   private static int widenMinY(int original) {
/* 21 */     return ToweringHeightsConfig.get().getMinY();
/*    */   }
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */   
/*    */   @ModifyConstant(method = {"resolveHeightLimits"}, constant = {@Constant(intValue = 2031)}, require = 0)
/*    */   private static int widenMaxY(int original) {
/* 30 */     return ToweringHeightsConfig.get().getMaxY() - 1;
/*    */   }
/*    */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\mixin\TellusResolveHeightMixin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */