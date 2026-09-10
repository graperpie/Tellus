/*     */ package com.toweringheights.mixin;
/*     */ 
/*     */ import java.util.List;
/*     */ import java.util.Optional;
/*     */ import net.minecraft.class_2487;
/*     */ import net.minecraft.class_2804;
/*     */ import net.minecraft.class_2826;
/*     */ import net.minecraft.class_2852;
/*     */ import net.minecraft.class_5539;
/*     */ import org.spongepowered.asm.mixin.Mixin;
/*     */ import org.spongepowered.asm.mixin.Shadow;
/*     */ import org.spongepowered.asm.mixin.Unique;
/*     */ import org.spongepowered.asm.mixin.injection.At;
/*     */ import org.spongepowered.asm.mixin.injection.Inject;
/*     */ import org.spongepowered.asm.mixin.injection.Redirect;
/*     */ import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ @Mixin({class_2852.class})
/*     */ public class ChunkSerializerMixin
/*     */ {
/*     */   @Shadow
/*     */   private List<class_2852.class_9898> comp_2959;
/*     */   @Unique
/*  38 */   private int toweringheights$writeIndex = 0;
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Inject(method = {"method_12410"}, at = {@At("HEAD")})
/*     */   private void resetWriteIndex(CallbackInfoReturnable<class_2487> cir) {
/*  46 */     this.toweringheights$writeIndex = 0;
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Redirect(method = {"method_12410"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/class_2487;method_10567(Ljava/lang/String;B)V"), require = 0)
/*     */   private void writeSectionYFull(class_2487 tag, String key, byte value) {
/*  62 */     tag.method_10567(key, value);
/*  63 */     if ("Y".equals(key) && 
/*  64 */       this.toweringheights$writeIndex < this.comp_2959.size()) {
/*  65 */       int realY = ((class_2852.class_9898)this.comp_2959.get(this.toweringheights$writeIndex)).comp_2963();
/*  66 */       tag.method_10569("YInt", realY);
/*  67 */       this.toweringheights$writeIndex++;
/*     */     } 
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Unique
/*  75 */   private static final ThreadLocal<Integer> toweringheights$readY = new ThreadLocal<>();
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Redirect(method = {"method_61794"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/class_2487;method_68562(Ljava/lang/String;B)B"), require = 0)
/*     */   private static byte readSectionY(class_2487 tag, String key, byte defaultValue) {
/*  89 */     if ("Y".equals(key)) {
/*  90 */       Optional<Integer> yInt = tag.method_10550("YInt");
/*  91 */       if (yInt.isPresent()) {
/*  92 */         int realY = ((Integer)yInt.get()).intValue();
/*  93 */         toweringheights$readY.set(Integer.valueOf(realY));
/*  94 */         return ((Byte)tag.method_10571(key).orElse(Byte.valueOf(defaultValue))).byteValue();
/*     */       } 
/*  96 */       byte b = ((Byte)tag.method_10571(key).orElse(Byte.valueOf(defaultValue))).byteValue();
/*  97 */       toweringheights$readY.set(Integer.valueOf(b));
/*  98 */       return b;
/*     */     } 
/* 100 */     return ((Byte)tag.method_10571(key).orElse(Byte.valueOf(defaultValue))).byteValue();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Redirect(method = {"method_61794"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/class_5539;method_32891()I"), require = 0)
/*     */   private static int fixBoundsCheckMin(class_5539 accessor) {
/* 117 */     Integer realY = toweringheights$readY.get();
/* 118 */     if (realY != null) {
/* 119 */       return Integer.MIN_VALUE;
/*     */     }
/* 121 */     return accessor.method_32891();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Redirect(method = {"method_61794"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/class_5539;method_31597()I"), require = 0)
/*     */   private static int fixBoundsCheckMax(class_5539 accessor) {
/* 133 */     Integer realY = toweringheights$readY.get();
/* 134 */     if (realY != null) {
/* 135 */       return Integer.MAX_VALUE;
/*     */     }
/* 137 */     return accessor.method_31597();
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Redirect(method = {"method_61794"}, at = @At(value = "NEW", target = "(ILnet/minecraft/class_2826;Lnet/minecraft/class_2804;Lnet/minecraft/class_2804;)Lnet/minecraft/class_2852$class_9898;"), require = 0)
/*     */   private static class_2852.class_9898 fixSectionDataY(int y, class_2826 chunkSection, class_2804 blockLight, class_2804 skyLight) {
/* 158 */     Integer realY = toweringheights$readY.get();
/* 159 */     if (realY != null) {
/* 160 */       y = realY.intValue();
/* 161 */       toweringheights$readY.remove();
/*     */     } 
/* 163 */     return new class_2852.class_9898(y, chunkSection, blockLight, skyLight);
/*     */   }
/*     */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\mixin\ChunkSerializerMixin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */