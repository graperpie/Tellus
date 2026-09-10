/*     */ package com.toweringheights.mixin;
/*     */ import org.spongepowered.asm.mixin.Shadow;
/*     */ 
/*     */ @Mixin({class_2338.class})
/*     */ public abstract class BlockPosMixin {
/*     */   @Shadow
/*     */   @Final
/*     */   @Mutable
/*     */   public static int field_10975;
/*     */   @Shadow
/*     */   @Final
/*     */   @Mutable
/*     */   public static int field_54978;
/*     */   @Shadow
/*     */   @Final
/*     */   @Mutable
/*     */   private static long field_10976;
/*     */   @Shadow
/*     */   @Final
/*     */   @Mutable
/*     */   private static long field_10974;
/*     */   @Shadow
/*     */   @Final
/*     */   @Mutable
/*     */   private static long field_10973;
/*     */   @Shadow
/*     */   @Final
/*     */   @Mutable
/*     */   private static int field_33083;
/*     */   @Shadow
/*     */   @Final
/*     */   @Mutable
/*     */   private static int field_10981;
/*     */   @Shadow
/*     */   @Final
/*     */   @Mutable
/*     */   private static int field_10983;
/*     */   
/*     */   @Inject(method = {"<clinit>"}, at = {@At("RETURN")})
/*     */   private static void modifyPackedLengths(CallbackInfo ci) {
/*  41 */     field_10975 = 15;
/*  42 */     field_54978 = 25;
/*  43 */     field_10976 = 33554431L;
/*  44 */     field_10974 = 16777215L;
/*  45 */     field_10973 = 32767L;
/*  46 */     field_33083 = 0;
/*  47 */     field_10981 = 15;
/*  48 */     field_10983 = 39;
/*  49 */     field_54979 = 16777215;
/*     */   }
/*     */   @Shadow
/*     */   @Final
/*     */   @Mutable
/*     */   public static int field_54979;
/*     */   
/*     */   @Overwrite
/*     */   public static long method_10064(int x, int y, int z) {
/*  58 */     long result = 0L;
/*  59 */     result |= (x & 0x1FFFFFFL) << 39L;
/*  60 */     result |= (z & 0xFFFFFFL) << 15L;
/*  61 */     result |= y & 0x7FFFL;
/*  62 */     return result;
/*     */   }
/*     */   @Unique
/*     */   private static final int NEW_X_BITS = 25; @Unique
/*     */   private static final int NEW_Z_BITS = 24; @Unique
/*     */   private static final int NEW_Y_BITS = 15;
/*     */   
/*     */   @Overwrite
/*     */   public static int method_10061(long packedPos) {
/*  71 */     return (int)(packedPos << 0L >> 39L);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Overwrite
/*     */   public static int method_10071(long packedPos) {
/*  80 */     return (int)(packedPos << 49L >> 49L);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Overwrite
/*     */   public static int method_10083(long packedPos) {
/*  89 */     return (int)(packedPos << 25L >> 40L);
/*     */   }
/*     */ 
/*     */ 
/*     */ 
/*     */ 
/*     */   
/*     */   @Overwrite
/*     */   public static long method_10096(long pos, int dx, int dy, int dz) {
/*  98 */     return method_10064(
/*  99 */         method_10061(pos) + dx, 
/* 100 */         method_10071(pos) + dy, 
/* 101 */         method_10083(pos) + dz);
/*     */   }
/*     */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\mixin\BlockPosMixin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */