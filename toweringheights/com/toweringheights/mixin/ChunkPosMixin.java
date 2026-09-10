/*    */ package com.toweringheights.mixin;
/*    */ 
/*    */ import net.minecraft.class_1923;
/*    */ import net.minecraft.class_4076;
/*    */ import org.spongepowered.asm.mixin.Mixin;
/*    */ import org.spongepowered.asm.mixin.Overwrite;
/*    */ import org.spongepowered.asm.mixin.Unique;
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
/*    */ @Mixin({class_1923.class})
/*    */ public class ChunkPosMixin
/*    */ {
/*    */   @Unique
/*    */   private static final int MAX_Z_BLOCK = 8388607;
/*    */   
/*    */   @Overwrite
/*    */   public static boolean method_76803(int chunkX, int chunkZ) {
/* 26 */     int blockX = class_4076.method_18688(chunkX);
/* 27 */     int blockZ = class_4076.method_18688(chunkZ);
/* 28 */     int maxX = 16777215;
/* 29 */     return (Math.abs(blockX) <= maxX && Math.abs(blockZ) <= 8388607);
/*    */   }
/*    */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\mixin\ChunkPosMixin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */