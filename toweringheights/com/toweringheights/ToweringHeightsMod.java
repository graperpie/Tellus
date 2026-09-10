/*    */ package com.toweringheights;
/*    */ 
/*    */ import net.fabricmc.api.ModInitializer;
/*    */ import org.slf4j.Logger;
/*    */ import org.slf4j.LoggerFactory;
/*    */ 
/*    */ public class ToweringHeightsMod implements ModInitializer {
/*    */   public static final String MOD_ID = "toweringheights";
/*  9 */   public static final Logger LOGGER = LoggerFactory.getLogger("toweringheights");
/*    */ 
/*    */   
/*    */   public void onInitialize() {
/* 13 */     ToweringHeightsConfig config = ToweringHeightsConfig.get();
/* 14 */     LOGGER.info("ToweringHeights loaded — height range: {} to {} ({} blocks)", new Object[] {
/* 15 */           Integer.valueOf(config.getMinY()), Integer.valueOf(config.getMaxY()), Integer.valueOf(config.getHeight())
/*    */         });
/*    */   }
/*    */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\ToweringHeightsMod.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */