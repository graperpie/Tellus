/*    */ package com.toweringheights.mixin;
/*    */ 
/*    */ import net.minecraft.class_2791;
/*    */ import net.minecraft.class_2902;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.injection.At;
/*    */ import org.spongepowered.asm.mixin.injection.Redirect;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ @Mixin({class_2902.class})
/*    */ public class HeightmapOptMixin
/*    */ {
/*    */   @Redirect(method = {"method_16684"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/class_2791;method_12031()I"), require = 0)
/*    */   private static int optimizeStartHeight(class_2791 chunk) {
/* 38 */     int highestIdx = chunk.method_12040();
/* 39 */     if (highestIdx < 0)
/*    */     {
/* 41 */       return chunk.method_31607() - 16;
/*    */     }
/*    */     
/* 44 */     return chunk.method_31607() + (highestIdx + 1) * 16;
/*    */   }
/*    */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\mixin\HeightmapOptMixin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */