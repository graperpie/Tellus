/*    */ package com.toweringheights.mixin;
/*    */ 
/*    */ import net.minecraft.class_2791;
/*    */ import net.minecraft.class_2826;
/*    */ import net.minecraft.class_6544;
/*    */ import net.minecraft.class_6780;
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
/*    */ @Mixin({class_2791.class})
/*    */ public abstract class BiomeFillOptMixin
/*    */ {
/*    */   @Shadow
/*    */   public abstract class_2826[] method_12006();
/*    */   
/*    */   @Shadow
/*    */   public abstract int method_12040();
/*    */   
/*    */   @Shadow
/*    */   public abstract class_2826 method_38259(int paramInt);
/*    */   
/*    */   @Inject(method = {"method_38257"}, at = {@At("HEAD")}, cancellable = true, require = 0)
/*    */   private void optimizeBiomeFill(class_6780 biomeResolver, class_6544.class_6552 sampler, CallbackInfo ci) {
/* 47 */     class_2791 self = (class_2791)this;
/* 48 */     int sectionCount = self.method_32890();
/*    */ 
/*    */     
/* 51 */     if (sectionCount <= 40) {
/*    */       return;
/*    */     }
/*    */ 
/*    */     
/* 56 */     int highestIdx = method_12040();
/* 57 */     if (highestIdx < 0) {
/* 58 */       highestIdx = 0;
/*    */     }
/*    */ 
/*    */     
/* 62 */     int maxIdx = Math.min(highestIdx + 2, sectionCount);
/* 63 */     int chunkX = (self.method_12004()).field_9181;
/* 64 */     int chunkZ = (self.method_12004()).field_9180;
/*    */     
/* 66 */     for (int i = 0; i < maxIdx; i++) {
/* 67 */       class_2826 section = method_38259(i);
/* 68 */       int sectionY = self.method_32891() + i;
/* 69 */       section.method_38291(biomeResolver, sampler, chunkX, sectionY, chunkZ);
/*    */     } 
/*    */     
/* 72 */     ci.cancel();
/*    */   }
/*    */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\mixin\BiomeFillOptMixin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */