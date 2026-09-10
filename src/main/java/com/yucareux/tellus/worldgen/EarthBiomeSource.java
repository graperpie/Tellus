package com.yucareux.tellus.worldgen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.yucareux.tellus.world.data.biome.BiomeClassification;
import com.yucareux.tellus.world.data.cover.TellusLandCoverSource;
import com.yucareux.tellus.world.data.koppen.TellusKoppenSource;
import com.yucareux.tellus.world.data.satellite.SatelliteTileSampler;
import com.yucareux.tellus.integration.meridian.MeridianLodPolicy;
import com.yucareux.tellus.integration.meridian.TellusLodSurfaceClassifier;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate.Sampler;

public final class EarthBiomeSource extends BiomeSource {
   public static final MapCodec<EarthBiomeSource> CODEC = RecordCodecBuilder.mapCodec(
      instance -> instance.group(
            RegistryOps.retrieveGetter(Registries.BIOME), EarthGeneratorSettings.CODEC.fieldOf("settings").forGetter(EarthBiomeSource::settings)
         )
         .apply(instance, EarthBiomeSource::new)
   );
   private static final int ESA_SNOW_ICE = 70;
   private static final int ESA_WATER = 80;
   private static final int ESA_MANGROVES = 95;
   private static final int ESA_NO_DATA = 0;
   private static final int CAVE_MIN_DEPTH = 8;
   private static final int LUSH_MIN_DEPTH = 12;
   private static final int DRIPSTONE_MIN_DEPTH = 16;
   private static final int DEEP_DARK_MIN_DEPTH = 24;
   private static final int CAVE_BIOME_GRID = 48;
   private static final int CAVE_BIOME_Y_GRID = 32;
   private static final int DEEP_DARK_GRID = 96;
   private static final int DEEP_DARK_Y_GRID = 48;
   private static final int DEEP_DARK_Y_OFFSET = 32;
    private static final double MAX_CAVE_BIOME_CHANCE = 0.55;
    private static final TellusLandCoverSource LAND_COVER_SOURCE = TellusWorldgenSources.landCover();
    private static final TellusKoppenSource KOPPEN_SOURCE = TellusWorldgenSources.koppen();
   
   private final HolderGetter<Biome> biomeLookup;
   
   private final EarthGeneratorSettings settings;
   
   private final SatelliteTileSampler satelliteSampler;
   
   private final Set<Holder<Biome>> possibleBiomes;
   
   private final Holder<Biome> plains;
   
   private final Holder<Biome> ocean;
   
   private final Holder<Biome> river;
   
   private final Holder<Biome> frozenPeaks;
   
   private final Holder<Biome> mangrove;
   
   private final Holder<Biome> lushCaves;
   
   private final Holder<Biome> dripstoneCaves;
   
   private final Holder<Biome> deepDark;
   
   private final WaterSurfaceResolver waterResolver;
   private final boolean remaSnowEnabled;
   private final double remaSnowBoundaryZ;
   private final int deepDarkCeiling;
   private volatile boolean fastSpawnMode = true;

   public EarthBiomeSource(HolderGetter<Biome> biomeLookup, EarthGeneratorSettings settings) {
      this.biomeLookup = Objects.requireNonNull(biomeLookup, "biomeLookup");
      this.settings = Objects.requireNonNull(settings, "settings");
      this.deepDarkCeiling = settings.resolveSeaLevel() - DEEP_DARK_Y_OFFSET;
      this.plains = this.biomeLookup.getOrThrow(Biomes.PLAINS);
      this.ocean = this.resolveBiome(Biomes.OCEAN, this.plains);
      this.river = this.resolveBiome(Biomes.RIVER, this.plains);
      this.frozenPeaks = this.resolveBiome(Biomes.FROZEN_PEAKS, this.plains);
      this.mangrove = this.resolveBiome(Biomes.MANGROVE_SWAMP, this.plains);
      this.lushCaves = this.resolveOptionalBiome(Biomes.LUSH_CAVES);
      this.dripstoneCaves = this.resolveOptionalBiome(Biomes.DRIPSTONE_CAVES);
      this.deepDark = this.resolveOptionalBiome(Biomes.DEEP_DARK);
      this.waterResolver = TellusWorldgenSources.waterResolver(this.settings);
      final java.net.http.HttpClient satelliteHttp = java.net.http.HttpClient.newBuilder()
              .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
              .connectTimeout(java.time.Duration.ofSeconds(5))
              .build();
      this.satelliteSampler = new SatelliteTileSampler(satelliteHttp,
              net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("tellus/cache/satellite"));
      this.remaSnowEnabled = false;
      this.remaSnowBoundaryZ = Double.POSITIVE_INFINITY;
      this.possibleBiomes = this.buildPossibleBiomes();
   }

   public EarthGeneratorSettings settings() {
      return this.settings;
   }

   void setFastSpawnMode(boolean enabled) {
      this.fastSpawnMode = enabled;
   }
   
   protected Stream<Holder<Biome>> collectPossibleBiomes() {
      return Objects.requireNonNull(this.possibleBiomes.stream(), "possibleBiomes.stream()");
   }
   
   protected MapCodec<? extends BiomeSource> codec() {
      return Objects.requireNonNull(CODEC, "CODEC");
   }
   
   public Holder<Biome> getNoiseBiome(int x, int y, int z,  Sampler sampler) {
      int blockX = QuartPos.toBlock(x);
      int blockY = QuartPos.toBlock(y);
      int blockZ = QuartPos.toBlock(z);
      return this.resolveBiomeAtBlock(blockX, blockY, blockZ);
   }
   
   public Holder<Biome> getBiomeAtBlock(int blockX, int blockZ) {
      return this.resolveSurfaceBiomeAtBlock(blockX, blockZ);
   }

   public Holder<Biome> getBiomeAtBlock(
      int blockX, int blockZ, int rawCoverClass, int visualCoverClass, WaterSurfaceResolver.WaterColumnData column
   ) {
      return this.getBiomeAtBlock(blockX, blockZ, rawCoverClass, visualCoverClass, column, null);
   }

   public Holder<Biome> getBiomeAtBlock(
      int blockX, int blockZ, int satelliteRgb, WaterSurfaceResolver.WaterColumnData column, String koppenCode
   ) {
      return this.fastSpawnMode
         ? this.resolveFastSpawnSurfaceBiome(blockX, blockZ)
         : this.resolveSurfaceBiomeFromSatellite(blockX, blockZ, satelliteRgb, column, koppenCode);
   }

   Holder<Biome> getBiomeAtBlock(
      int blockX, int blockZ, int rawCoverClass, int visualCoverClass, WaterSurfaceResolver.WaterColumnData column, String koppenCode
   ) {
      return this.fastSpawnMode
         ? this.resolveFastSpawnSurfaceBiome(blockX, blockZ)
         : this.resolveSurfaceBiomeAtBlock(blockX, blockZ, rawCoverClass, visualCoverClass, column, koppenCode);
   }
   
    private Holder<Biome> resolveSurfaceBiomeAtBlock(int blockX, int blockZ) {
       if (this.fastSpawnMode) {
          return this.resolveFastSpawnSurfaceBiome(blockX, blockZ);
       } else {
          int satelliteRgb = this.sampleSatelliteRgb(blockX, blockZ);
          return this.resolveSurfaceBiomeFromSatellite(blockX, blockZ, satelliteRgb, null, null);
       }
    }
   
   private int sampleSatelliteRgb(int blockX, int blockZ) {
       double lat = this.latitudeFromBlock(blockZ);
       double lon = this.longitudeFromBlock(blockX);
       int zoom = MeridianLodPolicy.satelliteZoom(0, lat, this.settings.worldScale());
       return this.satelliteSampler.sampleRgb(lat, lon, zoom);
   }

   private double latitudeFromBlock(int blockZ) {
       return EarthProjection.blockZToLat(blockZ, this.settings.worldScale());
   }

   private double longitudeFromBlock(int blockX) {
       return EarthProjection.blockXToLon(blockX, this.settings.worldScale());
   }

   private Holder<Biome> resolveBiomeAtBlock(int blockX, int blockY, int blockZ) {
      if (this.fastSpawnMode) {
         return this.resolveFastSpawnSurfaceBiome(blockX, blockZ);
      } else {
         int satelliteRgb = this.sampleSatelliteRgb(blockX, blockZ);
         WaterSurfaceResolver.WaterColumnData column = this.settings.enableWater()
            ? this.waterResolver.resolveFastColumnData(blockX, blockZ, 0)
            : this.waterResolver.resolveColumnData(blockX, blockZ, 0);
         Holder<Biome> surfaceBiome = this.resolveSurfaceBiomeFromSatellite(blockX, blockZ, satelliteRgb, column, null);
         if (!this.settings.caveGeneration()) {
            return surfaceBiome;
         } else {
            int depth = column.terrainSurface() - blockY;
            return depth < CAVE_MIN_DEPTH ? surfaceBiome : this.resolveCaveBiome(surfaceBiome, blockX, blockY, blockZ, depth);
         }
      }
   }
   
   private Holder<Biome> resolveSurfaceBiomeFromSatellite(
      int blockX, int blockZ, int satelliteRgb, WaterSurfaceResolver.WaterColumnData column, String precomputedKoppen
   ) {
      // Water check using water resolver
      if (this.settings.enableWater()) {
         WaterSurfaceResolver.WaterColumnData waterColumn = column != null
            ? column
            : this.waterResolver.resolveFastColumnData(blockX, blockZ, 0);
         if (waterColumn.hasWater()) {
            return waterColumn.isOcean() ? this.ocean : this.river;
         }
      }

      // Use satellite imagery for biome classification
      // Extract RGB components
      int r = (satelliteRgb >> 16) & 0xFF;
      int g = (satelliteRgb >> 8) & 0xFF;
      int b = satelliteRgb & 0xFF;
      
      double lat = this.latitudeFromBlock(blockZ);
      double absLat = Math.abs(lat);
      int surfaceY = column != null ? column.terrainSurface() : this.settings.resolveSeaLevel();
      int seaLevel = this.settings.resolveSeaLevel();
      int heightAboveSea = surfaceY - seaLevel;
      
      // Water (deep blue)
      if (b > Math.max(r, g) + 20 && (r + g + b) / 3.0 < 100) {
         return this.ocean;
      }
      
      // Snow/Ice (bright, low saturation)
      if (r > 200 && g > 200 && b > 200 && Math.abs(r - g) < 20 && Math.abs(g - b) < 20) {
         return this.frozenPeaks;
      }
      
      // Mangrove (greenish, tropical)
      if (g > r && g > b && absLat < 30 && heightAboveSea < 50) {
         return this.mangrove;
      }
      
      // Desert (yellowish/brownish, low vegetation)
      if (r > g && g > b && r > 150 && g < 150 && absLat < 35) {
         return this.resolveBiome(Biomes.DESERT, this.plains);
      }
      
      // Badlands (reddish)
      if (r > g && r > b && g > b && r > 150 && absLat < 40) {
         return this.resolveBiome(Biomes.BADLANDS, this.plains);
      }
      
      // Jungle (dark green, tropical)
      if (g > r && g > b && r < 100 && b < 100 && absLat < 20) {
         return this.resolveBiome(Biomes.JUNGLE, this.plains);
      }
      
      // Taiga (green, high latitude)
      if (g > r && g > b && absLat > 50) {
         return this.resolveBiome(Biomes.TAIGA, this.plains);
      }
      
      // Savanna (yellowish-green, warm)
      if (g > r && g > b && r > 100 && absLat < 30 && absLat > 15) {
         return this.resolveBiome(Biomes.SAVANNA, this.plains);
      }
      
      // Swamp (dark green, low elevation)
      if (g > r && g > b && r < 120 && b < 120 && heightAboveSea < 30) {
         return this.resolveBiome(Biomes.SWAMP, this.plains);
      }
      
      // Forest (green)
      if (g > r && g > b) {
         return this.resolveBiome(Biomes.FOREST, this.plains);
      }
      
      // Plains (default)
      return this.plains;
   }

    private Holder<Biome> resolveFastSpawnSurfaceBiome(int blockX, int blockZ) {
       int rawCoverClass = LAND_COVER_SOURCE.sampleCoverClass(blockX, blockZ, this.settings.worldScale());
       if (rawCoverClass == ESA_MANGROVES) {
          return this.mangrove;
       } else if (this.settings.enableWater()) {
          WaterSurfaceResolver.WaterInfo waterInfo = this.waterResolver.resolveFastWaterInfo(blockX, blockZ, rawCoverClass);
          return waterInfo.isWater()
             ? (waterInfo.isOcean() ? this.ocean : this.river)
             : this.plains;
       } else if (rawCoverClass == ESA_WATER) {
          return this.ocean;
       } else {
          return rawCoverClass == ESA_NO_DATA ? this.ocean : this.plains;
       }
    }
   
   private Holder<Biome> resolveSurfaceBiomeAtBlock(
      int blockX, int blockZ, int rawCoverClass, int visualCoverClass,  WaterSurfaceResolver.WaterColumnData column, String precomputedKoppen
   ) {
      if (rawCoverClass == ESA_MANGROVES) {
         return this.mangrove;
      } else {
         if (this.settings.enableWater()) {
            WaterSurfaceResolver.WaterColumnData waterColumn = column != null
               ? column
               : this.waterResolver.resolveFastColumnData(blockX, blockZ, rawCoverClass);
            if (waterColumn.hasWater()) {
               return waterColumn.isOcean() ? this.ocean : this.river;
            }
         } else if (rawCoverClass == ESA_NO_DATA || rawCoverClass == ESA_WATER) {
            WaterSurfaceResolver.WaterColumnData waterColumn = column != null ? column : this.waterResolver.resolveColumnData(blockX, blockZ, rawCoverClass);
            if (waterColumn.hasWater()) {
               if (waterColumn.isOcean()) {
                  return this.ocean;
               }

               return this.river;
            }
         }

         String koppen = precomputedKoppen;
         if (koppen == null) {
            koppen = KOPPEN_SOURCE.sampleDitheredCode(blockX, blockZ, this.settings.worldScale());
            if (koppen == null) {
               koppen = KOPPEN_SOURCE.findNearestCode(blockX, blockZ, this.settings.worldScale());
            }
         }

         ResourceKey<Biome> biomeKey = BiomeClassification.findBiomeKey(visualCoverClass, koppen);
         if (biomeKey == null) {
            biomeKey = BiomeClassification.findFallbackKey(visualCoverClass);
         }

         return biomeKey == null ? this.plains : this.resolveBiome(biomeKey, this.plains);
      }
   }

   private boolean isRemaSnowTerrain(int blockZ) {
      return this.remaSnowEnabled && blockZ >= this.remaSnowBoundaryZ;
   }

   private int sampleVisualCoverClass(int blockX, int blockZ, int rawCoverClass) {
      double worldScale = this.settings.worldScale();
      return worldScale > 0.0 && worldScale < 10.0 ? LAND_COVER_SOURCE.sampleVisualCoverClass(blockX, blockZ, worldScale) : rawCoverClass;
   }
   
   private Set<Holder<Biome>> buildPossibleBiomes() {
      Set<Holder<Biome>> holders = new HashSet<>();

      for (ResourceKey<Biome> key : BiomeClassification.allBiomeKeys()) {
         holders.add(this.resolveBiome(key, this.plains));
      }

      holders.add(this.plains);
      holders.add(this.ocean);
      holders.add(this.river);
      holders.add(this.frozenPeaks);
      holders.add(this.mangrove);
      if (this.settings.caveGeneration()) {
         addIfPresent(holders, this.lushCaves);
         addIfPresent(holders, this.dripstoneCaves);
         if (this.settings.deepDark()) {
            addIfPresent(holders, this.deepDark);
         }
      }

      return holders;
   }
   
   private Holder<Biome> resolveCaveBiome( Holder<Biome> surfaceBiome, int blockX, int blockY, int blockZ, int depth) {
      double depthFactor = Mth.clamp((depth - CAVE_MIN_DEPTH) / 80.0, 0.0, 1.0);
      double noise = sampleCaveNoise(blockX, blockY, blockZ, CAVE_BIOME_GRID, CAVE_BIOME_Y_GRID);
      Holder<Biome> deepDarkBiome = this.deepDark;
      Holder<Biome> lushCavesBiome = this.lushCaves;
      Holder<Biome> dripstoneCavesBiome = this.dripstoneCaves;
      if (this.settings.deepDark() && deepDarkBiome != null && blockY <= this.deepDarkCeiling && depth >= DEEP_DARK_MIN_DEPTH) {
         double deepNoise = sampleCaveNoise(blockX, blockY, blockZ, DEEP_DARK_GRID, DEEP_DARK_Y_GRID);
         double deepChance = 0.28 + depthFactor * 0.22;
         if (deepNoise < deepChance) {
            return deepDarkBiome;
         }
      }

      double lushChance = (isLushSurface(surfaceBiome) ? 0.45 : 0.25) * (1.0 - depthFactor * 0.35);
      double dripChance = (isDrySurface(surfaceBiome) ? 0.45 : 0.25) * (0.7 + depthFactor * 0.5);
      if (depth < LUSH_MIN_DEPTH) {
         lushChance = 0.0;
      }

      if (depth < DRIPSTONE_MIN_DEPTH) {
         dripChance = 0.0;
      }

      double total = lushChance + dripChance;
      if (!(total <= 0.0) && !(noise > MAX_CAVE_BIOME_CHANCE)) {
         double pick = noise * total / MAX_CAVE_BIOME_CHANCE;
         if (lushCavesBiome != null && pick < lushChance) {
            return lushCavesBiome;
         } else {
            return dripstoneCavesBiome != null && pick < lushChance + dripChance ? dripstoneCavesBiome : surfaceBiome;
         }
      } else {
         return surfaceBiome;
      }
   }

   private static boolean isLushSurface(Holder<Biome> surfaceBiome) {
      return surfaceBiome.is(Biomes.JUNGLE)
         || surfaceBiome.is(Biomes.SPARSE_JUNGLE)
         || surfaceBiome.is(Biomes.BAMBOO_JUNGLE)
         || surfaceBiome.is(Biomes.SWAMP)
         || surfaceBiome.is(Biomes.MANGROVE_SWAMP)
         || surfaceBiome.is(Biomes.DARK_FOREST)
         || surfaceBiome.is(Biomes.FOREST)
         || surfaceBiome.is(Biomes.BIRCH_FOREST)
         || surfaceBiome.is(Biomes.OLD_GROWTH_BIRCH_FOREST)
         || surfaceBiome.is(Biomes.OLD_GROWTH_PINE_TAIGA)
         || surfaceBiome.is(Biomes.OLD_GROWTH_SPRUCE_TAIGA)
         || surfaceBiome.is(Biomes.TAIGA);
   }

   private static boolean isDrySurface(Holder<Biome> surfaceBiome) {
      return surfaceBiome.is(Biomes.DESERT)
         || surfaceBiome.is(Biomes.BADLANDS)
         || surfaceBiome.is(Biomes.WOODED_BADLANDS)
         || surfaceBiome.is(Biomes.ERODED_BADLANDS)
         || surfaceBiome.is(Biomes.SAVANNA)
         || surfaceBiome.is(Biomes.SAVANNA_PLATEAU)
         || surfaceBiome.is(Biomes.WINDSWEPT_SAVANNA);
   }

   private static double sampleCaveNoise(int blockX, int blockY, int blockZ, int gridXZ, int gridY) {
      int x = Math.floorDiv(blockX, gridXZ);
      int y = Math.floorDiv(blockY, gridY);
      int z = Math.floorDiv(blockZ, gridXZ);
      long seed = x * 341873128712L + z * 132897987541L + y * 42317861L;
      return hashToUnit(seed);
   }

   private static double hashToUnit(long seed) {
      seed ^= seed >>> 33;
      seed *= -49064778989728563L;
      seed ^= seed >>> 33;
      seed *= -4265267296055464877L;
      seed ^= seed >>> 33;
      return (seed >>> 11) * 1.110223E-16F;
   }

   private static void addIfPresent(Set<Holder<Biome>> holders,  Holder<Biome> biome) {
      if (biome != null) {
         holders.add(biome);
      }
   }
   
   private Holder<Biome> resolveBiome( ResourceKey<Biome> key,  Holder<Biome> fallback) {
      if (key == null) {
         return fallback;
      } else {
         Holder<Biome> resolved = this.biomeLookup.get(key).map(holder -> (Holder<Biome>)holder).orElse(fallback);
         return Objects.requireNonNull(resolved, "resolvedBiome");
      }
   }
   
   private Holder<Biome> resolveOptionalBiome( ResourceKey<Biome> key) {
      return key == null ? null : this.biomeLookup.get(key).map(holder -> (Holder<Biome>)holder).orElse(null);
   }
}