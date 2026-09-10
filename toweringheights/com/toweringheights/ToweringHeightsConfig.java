/*     */ package com.toweringheights;
/*     */ 
/*     */ import com.google.gson.Gson;
/*     */ import com.google.gson.GsonBuilder;
/*     */ import java.io.IOException;
/*     */ import java.nio.file.Files;
/*     */ import java.nio.file.Path;
/*     */ import java.nio.file.attribute.FileAttribute;
/*     */ import org.slf4j.Logger;
/*     */ import org.slf4j.LoggerFactory;
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
/*     */ public class ToweringHeightsConfig
/*     */ {
/*  24 */   private static final Logger LOGGER = LoggerFactory.getLogger("toweringheights");
/*  25 */   private static final Gson GSON = (new GsonBuilder()).setPrettyPrinting().create();
/*  26 */   private static final Path CONFIG_DIR = Path.of("config", new String[0]);
/*  27 */   private static final Path CONFIG_PATH = CONFIG_DIR.resolve("toweringheights.json");
/*     */ 
/*     */   
/*     */   private static ToweringHeightsConfig instance;
/*     */ 
/*     */   
/*  33 */   public int minY = -1024;
/*  34 */   public int height = 10240;
/*  35 */   public int maxRenderSectionsY = 128;
/*     */ 
/*     */ 
/*     */   
/*     */   private transient int maxY;
/*     */ 
/*     */ 
/*     */   
/*     */   public static ToweringHeightsConfig get() {
/*  44 */     if (instance == null) {
/*  45 */       instance = load();
/*     */     }
/*  47 */     return instance;
/*     */   }
/*     */   
/*     */   public int getMinY() {
/*  51 */     return this.minY;
/*     */   }
/*     */   
/*     */   public int getHeight() {
/*  55 */     return this.height;
/*     */   }
/*     */   
/*     */   public int getMaxY() {
/*  59 */     if (this.maxY == 0) {
/*  60 */       this.maxY = this.minY + this.height;
/*     */     }
/*  62 */     return this.maxY;
/*     */   }
/*     */   
/*     */   public int getLogicalHeight() {
/*  66 */     return this.height;
/*     */   }
/*     */   
/*     */   public int getMaxRenderSectionsY() {
/*  70 */     return this.maxRenderSectionsY;
/*     */   }
/*     */ 
/*     */   
/*     */   private static ToweringHeightsConfig load() {
/*     */     ToweringHeightsConfig config;
/*  76 */     if (Files.exists(CONFIG_PATH, new java.nio.file.LinkOption[0])) {
/*     */       try {
/*  78 */         String json = Files.readString(CONFIG_PATH);
/*  79 */         config = (ToweringHeightsConfig)GSON.fromJson(json, ToweringHeightsConfig.class);
/*  80 */         if (config == null) {
/*  81 */           config = new ToweringHeightsConfig();
/*     */         }
/*  83 */         LOGGER.info("Loaded config: minY={}, height={} (maxY={})", new Object[] {
/*  84 */               Integer.valueOf(config.minY), Integer.valueOf(config.height), Integer.valueOf(config.minY + config.height) });
/*  85 */       } catch (Exception e) {
/*  86 */         LOGGER.error("Failed to load config, using defaults", e);
/*  87 */         config = new ToweringHeightsConfig();
/*     */       } 
/*     */     } else {
/*  90 */       config = new ToweringHeightsConfig();
/*  91 */       LOGGER.info("No config found, creating default: minY={}, height={}", Integer.valueOf(config.minY), Integer.valueOf(config.height));
/*     */     } 
/*     */ 
/*     */     
/*  95 */     config.validate();
/*     */ 
/*     */     
/*  98 */     config.save();
/*     */     
/* 100 */     return config;
/*     */   }
/*     */ 
/*     */   
/*     */   private void validate() {
/* 105 */     if (this.minY % 16 != 0) {
/* 106 */       LOGGER.warn("minY ({}) must be divisible by 16, rounding down", Integer.valueOf(this.minY));
/* 107 */       this.minY = Math.floorDiv(this.minY, 16) * 16;
/*     */     } 
/* 109 */     if (this.height % 16 != 0) {
/* 110 */       LOGGER.warn("height ({}) must be divisible by 16, rounding up", Integer.valueOf(this.height));
/* 111 */       this.height = (this.height + 15) / 16 * 16;
/*     */     } 
/*     */ 
/*     */     
/* 115 */     int maxAllowedMinY = -16368;
/* 116 */     int maxAllowedHeight = 32736;
/* 117 */     if (this.minY < maxAllowedMinY) {
/* 118 */       LOGGER.warn("minY ({}) below minimum {}, clamping", Integer.valueOf(this.minY), Integer.valueOf(maxAllowedMinY));
/* 119 */       this.minY = maxAllowedMinY;
/*     */     } 
/* 121 */     if (this.height > maxAllowedHeight) {
/* 122 */       LOGGER.warn("height ({}) exceeds maximum {}, clamping", Integer.valueOf(this.height), Integer.valueOf(maxAllowedHeight));
/* 123 */       this.height = maxAllowedHeight;
/*     */     } 
/* 125 */     if (this.height < 16) {
/* 126 */       this.height = 16;
/*     */     }
/* 128 */     if (this.minY + this.height > 16368) {
/* 129 */       LOGGER.warn("minY + height ({}) exceeds 16368, reducing height", Integer.valueOf(this.minY + this.height));
/* 130 */       this.height = 16368 - this.minY;
/*     */     } 
/*     */ 
/*     */     
/* 134 */     if (this.maxRenderSectionsY < 16) {
/* 135 */       this.maxRenderSectionsY = 16;
/*     */     }
/* 137 */     if (this.maxRenderSectionsY > this.height / 16) {
/* 138 */       this.maxRenderSectionsY = this.height / 16;
/*     */     }
/*     */     
/* 141 */     this.maxY = this.minY + this.height;
/*     */   }
/*     */   
/*     */   private void save() {
/*     */     try {
/* 146 */       Files.createDirectories(CONFIG_DIR, (FileAttribute<?>[])new FileAttribute[0]);
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
/* 158 */       String json = "{\n  \"_minY\": \"Minimum Y value of the world. Cannot go below -16384.\",\n  \"minY\": %d,\n\n  \"_height\": \"Total world height (minY + height = maximum height). Has to be a multiple of 16, and minY + height cannot exceed 32768.\",\n  \"height\": %d,\n\n  \"_maxRenderSectionsY\": \"How many 16-block sections the mod renders vertically. Eg: a value of 128 would be 2048 blocks vertically. Lower values save memory and FPS.\",\n  \"maxRenderSectionsY\": %d\n}\n".formatted(new Object[] { Integer.valueOf(this.minY), Integer.valueOf(this.height), Integer.valueOf(this.maxRenderSectionsY) });
/* 159 */       Files.writeString(CONFIG_PATH, json, new java.nio.file.OpenOption[0]);
/* 160 */     } catch (IOException e) {
/* 161 */       LOGGER.error("Failed to save config", e);
/*     */     } 
/*     */   }
/*     */ }


/* Location:              C:\Users\grape\AppData\Roaming\ModrinthApp\profiles\terrarium newesr\mods\toweringheights-0.1.2.jar!\com\toweringheights\ToweringHeightsConfig.class
 * Java compiler version: 21 (65.0)
 * JD-Core Version:       1.1.3
 */