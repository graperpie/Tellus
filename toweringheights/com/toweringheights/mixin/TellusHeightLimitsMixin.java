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
/*    */ @Mixin(targets = {"com/yucareux/tellus/worldgen/EarthGeneratorSettings$HeightLimits"}, remap = false)
/*    */ public abstract class TellusHeightLimitsMixin
/*    */ {
/*    */   @ModifyConstant(method = {"maxRange"}, constant = {@Constant(intValue = -2032)}, require = 0)
/*    */   private static int modifyMaxRangeMinY(int original) {
/* 18 */     return ToweringHeightsConfig.get().getMinY();
/*    */   }
/*    */   
/*    */   @ModifyConstant(method = {"maxRange"}, constant = {@Constant(intValue = 4064, ordinal = 0)}, require = 0)
/*    */   private static int modifyMaxRangeHeight(int original) {
/* 23 */     return ToweringHeightsConfig.get().getHeight();
/*    */   }
/*    */   
/*    */   @ModifyConstant(method = {"maxRange"}, constant = {@Constant(intValue = 4064, ordinal = 1)}, require = 0)
/*    */   private static int modifyMaxRangeLogicalHeight(int original) {
/* 28 */     return ToweringHeightsConfig.get().getLogicalHeight();
/*    */   }
/*    */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\mixin\TellusHeightLimitsMixin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */