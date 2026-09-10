/*    */ package com.toweringheights.mixin;
/*    */ 
/*    */ import java.util.List;
/*    */ import java.util.Set;
/*    */ import net.fabricmc.loader.api.FabricLoader;
/*    */ import org.objectweb.asm.tree.ClassNode;
/*    */ import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
/*    */ import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ 
/*    */ public class TellusMixinPlugin
/*    */   implements IMixinConfigPlugin
/*    */ {
/*    */   private boolean tellusPresent = false;
/*    */   
/*    */   public void onLoad(String mixinPackage) {
/* 20 */     this.tellusPresent = FabricLoader.getInstance().isModLoaded("tellus");
/*    */   }
/*    */ 
/*    */   
/*    */   public String getRefMapperConfig() {
/* 25 */     return null;
/*    */   }
/*    */ 
/*    */   
/*    */   public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
/* 30 */     return this.tellusPresent;
/*    */   }
/*    */ 
/*    */ 
/*    */   
/*    */   public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
/*    */ 
/*    */   
/*    */   public List<String> getMixins() {
/* 39 */     return null;
/*    */   }
/*    */   
/*    */   public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
/*    */   
/*    */   public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
/*    */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\mixin\TellusMixinPlugin.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */