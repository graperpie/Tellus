/*    */ package com.toweringheights.mixin;
/*    */ 
/*    */ import net.minecraft.class_2784;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.injection.Constant;
/*    */ import org.spongepowered.asm.mixin.injection.ModifyConstant;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ @Mixin({class_2784.class})
/*    */ public class WorldBorderMixin
/*    */ {
/*    */   @ModifyConstant(method = {"*"}, constant = {@Constant(doubleValue = 2.9999984E7D)}, require = 0)
/*    */   private double modifyMaxBorderSizeDouble(double original) {
/* 18 */     return 8000000.0D;
/*    */   }
/*    */   
/*    */   @ModifyConstant(method = {"*"}, constant = {@Constant(intValue = 29999984)}, require = 0)
/*    */   private int modifyMaxBorderSizeInt(int original) {
/* 23 */     return 8000000;
/*    */   }
/*    */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\mixin\WorldBorderMixin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */