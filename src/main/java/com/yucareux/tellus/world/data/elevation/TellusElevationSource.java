package com.yucareux.tellus.world.data.elevation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yucareux.tellus.cache.TellusCacheDomain;
import com.yucareux.tellus.cache.TellusCacheHandle;
import com.yucareux.tellus.cache.TellusCacheRegistry;
import com.yucareux.tellus.Tellus;
import com.yucareux.tellus.world.data.source.DownloadProgressReporter;
import com.yucareux.tellus.worldgen.EarthProjection;
import com.yucareux.tellus.worldgen.TellusWorldgenSources;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class TellusElevationSource implements TellusCacheHandle {
   private static final double EQUATOR_CIRCUMFERENCE = 4.0075017E7;
   private static final int TILE_SIZE = 256;
   private static final int MIN_ZOOM = 0;
   private static final int LAND_MAX_ZOOM = 15;
   private static final int OCEAN_MAX_ZOOM = 10;
   private static final double MIN_LAT = -85.05112878;
   private static final double MAX_LAT = 85.05112878;
   private static final double MIN_LON = -180.0;
   private static final double MAX_LON = 180.0;
   private static final double RESOLUTION_METERS = 30.0;
   private static final String ENDPOINT = "https://s3.amazonaws.com/elevation-tiles-prod/terrarium";
   private static final int MAX_CACHE_TILES = intProperty("tellus.elevation.cacheTiles", 512);
   private static final ShortRaster MISSING_RASTER = ShortRaster.create(1, 1);
   private final Path cacheRoot;
   private final LoadingCache<TellusElevationSource.TileKey, ShortRaster> cache;
   private final TerrainTilesResolutionIndex terrainResolutionIndex = TerrainTilesResolutionIndex.create();

   public TellusElevationSource() {
      this.cacheRoot = FabricLoader.getInstance().getGameDir().resolve("tellus/cache/elevation-tellus");
      this.cache = CacheBuilder.newBuilder().maximumSize(MAX_CACHE_TILES).build(new CacheLoader<TellusElevationSource.TileKey, ShortRaster>() {
         public ShortRaster load( TellusElevationSource.TileKey key) throws Exception {
            return TellusElevationSource.this.loadTile(key);
         }
      });
      TellusCacheRegistry.register(this);
   }

   public double sampleElevationMeters(double blockX, double blockZ, double worldScale) {
      return this.sampleElevationMeters(blockX, blockZ, worldScale, true, worldScale);
   }

   public double sampleElevationMeters(double blockX, double blockZ, double worldScale, boolean highResOcean) {
      return this.sampleElevationMeters(blockX, blockZ, worldScale, highResOcean, worldScale);
   }

   public double samplePreviewElevationMeters(
      double blockX,
      double blockZ,
      double worldScale,
      boolean highResOcean,
      double previewResolutionMeters
   ) {
      return this.sampleElevationMeters(blockX, blockZ, worldScale, highResOcean, previewResolutionMeters);
   }

   public double samplePreviewElevationMetersLocalOnly(
      double blockX,
      double blockZ,
      double worldScale,
      boolean highResOcean,
      double previewResolutionMeters
   ) {
      if (worldScale <= 0.0) {
         return 0.0;
      } else {
         return this.sampleTerrariumMetersLocalOnly(blockX, blockZ, worldScale, highResOcean, previewResolutionMeters);
      }
   }

   public double samplePreviewElevationMetersMemoryOnly(
      double blockX,
      double blockZ,
      double worldScale,
      boolean highResOcean,
      double previewResolutionMeters
   ) {
      if (worldScale <= 0.0) {
         return Double.NaN;
      } else {
         return this.sampleTerrariumMetersMemoryOnly(blockX, blockZ, worldScale, highResOcean, previewResolutionMeters);
      }
   }

   public double samplePreviewTerrainTilesMetersLocalOnly(
      double blockX, double blockZ, double worldScale, boolean highResOcean, double previewResolutionMeters
   ) {
      return this.sampleTerrariumMetersLocalOnly(blockX, blockZ, worldScale, highResOcean, previewResolutionMeters);
   }

   private double sampleElevationMeters(
      double blockX,
      double blockZ,
      double worldScale,
      boolean highResOcean,
      double previewResolutionMeters
   ) {
      if (worldScale <= 0.0) {
         return 0.0;
      } else {
         return this.sampleTerrariumMeters(blockX, blockZ, worldScale, highResOcean, previewResolutionMeters);
      }
   }

   public TellusElevationSource.ElevationDiagnostic sampleDiagnostic(
      double blockX, double blockZ, double worldScale, boolean highResOcean
   ) {
      return this.sampleDiagnostic(blockX, blockZ, worldScale, highResOcean, worldScale);
   }

   public TellusElevationSource.ElevationDiagnostic samplePreviewDiagnostic(
      double blockX,
      double blockZ,
      double worldScale,
      boolean highResOcean,
      double previewResolutionMeters
   ) {
      return this.sampleDiagnostic(blockX, blockZ, worldScale, highResOcean, previewResolutionMeters);
   }

   private TellusElevationSource.ElevationDiagnostic sampleDiagnostic(
      double blockX,
      double blockZ,
      double worldScale,
      boolean highResOcean,
      double previewResolutionMeters
   ) {
      if (worldScale <= 0.0) {
         return diagnostic(0.0, TellusElevationSource.DemUsage.TERRAIN_TILES);
      } else {
         return this.terrainTilesDiagnostic(blockX, blockZ, worldScale, highResOcean, previewResolutionMeters);
      }
   }

   public void prefetchTiles(double blockX, double blockZ, double worldScale, int radius) {
      this.prefetchTiles(blockX, blockZ, worldScale, radius, worldScale);
   }

   public void prefetchTiles(
      double blockX,
      double blockZ,
      double worldScale,
      int radius,
      double previewResolutionMeters
   ) {
      if (!(worldScale <= 0.0)) {
         this.prefetchTerrainTiles(blockX, blockZ, worldScale, radius, previewResolutionMeters);
      }
   }

   private void prefetchTerrainTiles(double blockX, double blockZ, double worldScale, int radius, double previewResolutionMeters) {
      int step = downsampleStep(worldScale, RESOLUTION_METERS, previewResolutionMeters);
      if (step > 1) {
         blockX = downsampleBlock(blockX, step);
         blockZ = downsampleBlock(blockZ, step);
      }

      int zoom = Mth.clamp(selectZoom(worldScale), MIN_ZOOM, LAND_MAX_ZOOM);
      TellusElevationSource.TileKey center = tileKeyForBlock(blockX, blockZ, worldScale, zoom);
      if (center == null) {
         return;
      }

      int tilesPerAxis = 1 << zoom;
      int clampedRadius = Math.max(0, radius);
      int minX = Math.max(0, center.x() - clampedRadius);
      int maxX = Math.min(tilesPerAxis - 1, center.x() + clampedRadius);
      int minY = Math.max(0, center.y() - clampedRadius);
      int maxY = Math.min(tilesPerAxis - 1, center.y() + clampedRadius);

      for (int tileY = minY; tileY <= maxY; tileY++) {
         for (int tileX = minX; tileX <= maxX; tileX++) {
            this.prefetchTile(new TellusElevationSource.TileKey(zoom, tileX, tileY));
         }
      }
   }

   private double sampleTerrariumMeters(double blockX, double blockZ, double worldScale, boolean highResOcean) {
      return this.sampleTerrariumMeters(blockX, blockZ, worldScale, highResOcean, worldScale);
   }

   private double sampleTerrariumMetersLocalOnly(double blockX, double blockZ, double worldScale, boolean highResOcean, double previewResolutionMeters) {
      int step = downsampleStep(worldScale, RESOLUTION_METERS, previewResolutionMeters);
      if (step > 1) {
         blockX = downsampleBlock(blockX, step);
         blockZ = downsampleBlock(blockZ, step);
      }

      int zoom = Mth.clamp(selectZoom(worldScale), MIN_ZOOM, LAND_MAX_ZOOM);
      double sample = this.sampleAtZoomLocalOnly(blockX, blockZ, worldScale, zoom);
      if (!Double.isNaN(sample)) {
         if (sample <= 0.0 && highResOcean) {
            double oceanSample = this.sampleAtZoomLocalOnly(blockX, blockZ, worldScale, OCEAN_MAX_ZOOM);
            if (!Double.isNaN(oceanSample)) {
               return oceanSample;
            }
         }

         return sample;
      } else {
         double oceanSample = this.sampleAtZoomLocalOnly(blockX, blockZ, worldScale, OCEAN_MAX_ZOOM);
         return !Double.isNaN(oceanSample) ? oceanSample : 0.0;
      }
   }

   private double sampleTerrariumMetersMemoryOnly(double blockX, double blockZ, double worldScale, boolean highResOcean, double previewResolutionMeters) {
      int step = downsampleStep(worldScale, RESOLUTION_METERS, previewResolutionMeters);
      if (step > 1) {
         blockX = downsampleBlock(blockX, step);
         blockZ = downsampleBlock(blockZ, step);
      }

      int zoom = Mth.clamp(selectZoom(worldScale), MIN_ZOOM, LAND_MAX_ZOOM);
      double sample = this.sampleAtZoomMemoryOnly(blockX, blockZ, worldScale, zoom);
      if (!Double.isNaN(sample)) {
         if (sample <= 0.0 && highResOcean) {
            double oceanSample = this.sampleAtZoomMemoryOnly(blockX, blockZ, worldScale, OCEAN_MAX_ZOOM);
            if (!Double.isNaN(oceanSample)) {
               return oceanSample;
            }
         }

         return sample;
      } else {
         double oceanSample = this.sampleAtZoomMemoryOnly(blockX, blockZ, worldScale, OCEAN_MAX_ZOOM);
         return oceanSample;
      }
   }

   private TellusElevationSource.ElevationDiagnostic terrainTilesDiagnostic(
      double blockX, double blockZ, double worldScale, boolean highResOcean
   ) {
      return this.terrainTilesDiagnostic(blockX, blockZ, worldScale, highResOcean, worldScale);
   }

   private TellusElevationSource.ElevationDiagnostic terrainTilesDiagnostic(
      double blockX, double blockZ, double worldScale, boolean highResOcean, double previewResolutionMeters
   ) {
      return diagnostic(
         this.sampleTerrariumMeters(blockX, blockZ, worldScale, highResOcean, previewResolutionMeters),
         TellusElevationSource.DemUsage.TERRAIN_TILES,
         TellusElevationSource.DemUsage.TERRAIN_TILES.bit(),
         this.terrainTilesResolutionMeters(blockX, blockZ, worldScale)
      );
   }

   private double terrainTilesResolutionMeters(double blockX, double blockZ, double worldScale) {
      TellusElevationSource.LatLon latLon = toLatLon(blockX, blockZ, worldScale);
      if (latLon == null) {
         return TellusElevationSource.DemUsage.TERRAIN_TILES.nominalResolutionMeters();
      } else {
         double resolutionMeters = this.terrainResolutionIndex.lookupResolutionMeters(latLon.lat(), latLon.lon());
         return Double.isFinite(resolutionMeters) && resolutionMeters > 0.0
            ? resolutionMeters
            : TellusElevationSource.DemUsage.TERRAIN_TILES.nominalResolutionMeters();
      }
   }

   private double sampleTerrariumMeters(double blockX, double blockZ, double worldScale, boolean highResOcean, double previewResolutionMeters) {
      int step = downsampleStep(worldScale, RESOLUTION_METERS, previewResolutionMeters);
      if (step > 1) {
         blockX = downsampleBlock(blockX, step);
         blockZ = downsampleBlock(blockZ, step);
      }

      int zoom = Mth.clamp(selectZoom(worldScale), MIN_ZOOM, LAND_MAX_ZOOM);
      double sample = this.sampleAtZoom(blockX, blockZ, worldScale, zoom);
      if (!Double.isNaN(sample)) {
         if (sample <= 0.0 && highResOcean) {
            double oceanSample = this.sampleAtZoom(blockX, blockZ, worldScale, OCEAN_MAX_ZOOM);
            if (!Double.isNaN(oceanSample)) {
               return oceanSample;
            }
         }

         return sample;
      } else {
         double oceanSample = this.sampleAtZoom(blockX, blockZ, worldScale, OCEAN_MAX_ZOOM);
         return !Double.isNaN(oceanSample) ? oceanSample : 0.0;
      }
   }

   /**
    * Picks the terrarium tile zoom whose pixel footprint matches an LOD column that covers
    * {@code metersPerColumn} metres of ground, never exceeding the zoom the world scale itself
    * justifies. Coarse LOD levels therefore download coarse tiles instead of fetching full
    * resolution data and discarding most of it.
    */
   public static int lodZoom(double worldScale, double metersPerColumn) {
      int nativeZoom = Mth.clamp(selectZoom(worldScale), MIN_ZOOM, LAND_MAX_ZOOM);
      if (!(metersPerColumn > 0.0)) {
         return nativeZoom;
      } else {
         int footprintZoom = Mth.clamp((int)Math.round(zoomForScale(metersPerColumn)), MIN_ZOOM, LAND_MAX_ZOOM);
         return Math.min(nativeZoom, footprintZoom);
      }
   }

   /**
    * Samples elevation for a long range LOD column at an explicitly chosen zoom, so callers that
    * render far terrain can trade vertical detail for a much smaller tile working set.
    */
   public double sampleLodElevationMeters(double blockX, double blockZ, double worldScale, boolean highResOcean, int zoom) {
      int clampedZoom = Mth.clamp(zoom, MIN_ZOOM, LAND_MAX_ZOOM);
      double sample = this.sampleAtZoom(blockX, blockZ, worldScale, clampedZoom);
      if (!Double.isNaN(sample)) {
         if (sample <= 0.0 && highResOcean) {
            double oceanSample = this.sampleAtZoom(blockX, blockZ, worldScale, Math.min(clampedZoom, OCEAN_MAX_ZOOM));
            if (!Double.isNaN(oceanSample)) {
               return oceanSample;
            }
         }

         return sample;
      } else {
         double oceanSample = this.sampleAtZoom(blockX, blockZ, worldScale, Math.min(clampedZoom, OCEAN_MAX_ZOOM));
         return !Double.isNaN(oceanSample) ? oceanSample : 0.0;
      }
   }

   private double sampleAtZoom(double blockX, double blockZ, double worldScale, int zoom) {
      double blocksPerDegree = EarthProjection.blocksPerDegree(worldScale);
      double lon = EarthProjection.blockXToLon(blockX, worldScale);
      double lat = EarthProjection.blockZToLat(blockZ, worldScale);
      if (!(lat < MIN_LAT) && !(lat > MAX_LAT) && !(lon < MIN_LON) && !(lon > MAX_LON)) {
         double latRad = Math.toRadians(lat);
         double n = Math.pow(2.0, zoom);
         double x = (lon + 180.0) / 360.0 * n;
         double y = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n;
         if (!(x < 0.0) && !(y < 0.0) && !(x >= n) && !(y >= n)) {
            int tileX = Mth.floor(x);
            int tileY = Mth.floor(y);
            ShortRaster raster = this.getTile(new TellusElevationSource.TileKey(zoom, tileX, tileY));
            if (raster == null) {
               return Double.NaN;
            } else {
               double globalX = x * TILE_SIZE;
               double globalY = y * TILE_SIZE;
               return this.sampleBilinearAcrossTiles(zoom, globalX, globalY, tileX, tileY, raster);
            }
         } else {
            return Double.NaN;
         }
      } else {
         return Double.NaN;
      }
   }

   private double sampleAtZoomLocalOnly(double blockX, double blockZ, double worldScale, int zoom) {
      double blocksPerDegree = EarthProjection.blocksPerDegree(worldScale);
      double lon = EarthProjection.blockXToLon(blockX, worldScale);
      double lat = EarthProjection.blockZToLat(blockZ, worldScale);
      if (!(lat < MIN_LAT) && !(lat > MAX_LAT) && !(lon < MIN_LON) && !(lon > MAX_LON)) {
         double latRad = Math.toRadians(lat);
         double n = Math.pow(2.0, zoom);
         double x = (lon + 180.0) / 360.0 * n;
         double y = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n;
         if (!(x < 0.0) && !(y < 0.0) && !(x >= n) && !(y >= n)) {
            int tileX = Mth.floor(x);
            int tileY = Mth.floor(y);
            ShortRaster raster = this.getTileLocalOnly(new TellusElevationSource.TileKey(zoom, tileX, tileY));
            if (raster == null) {
               return Double.NaN;
            } else {
               double globalX = x * TILE_SIZE;
               double globalY = y * TILE_SIZE;
               return this.sampleBilinearAcrossTilesLocalOnly(zoom, globalX, globalY, tileX, tileY, raster);
            }
         } else {
            return Double.NaN;
         }
      } else {
         return Double.NaN;
      }
   }

   private double sampleAtZoomMemoryOnly(double blockX, double blockZ, double worldScale, int zoom) {
      double blocksPerDegree = EarthProjection.blocksPerDegree(worldScale);
      double lon = EarthProjection.blockXToLon(blockX, worldScale);
      double lat = EarthProjection.blockZToLat(blockZ, worldScale);
      if (!(lat < MIN_LAT) && !(lat > MAX_LAT) && !(lon < MIN_LON) && !(lon > MAX_LON)) {
         double latRad = Math.toRadians(lat);
         double n = Math.pow(2.0, zoom);
         double x = (lon + 180.0) / 360.0 * n;
         double y = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n;
         if (!(x < 0.0) && !(y < 0.0) && !(x >= n) && !(y >= n)) {
            int tileX = Mth.floor(x);
            int tileY = Mth.floor(y);
            ShortRaster raster = this.getTileMemoryOnly(new TellusElevationSource.TileKey(zoom, tileX, tileY));
            if (raster == null) {
               return Double.NaN;
            } else {
               double globalX = x * TILE_SIZE;
               double globalY = y * TILE_SIZE;
               return this.sampleBilinearAcrossTilesMemoryOnly(zoom, globalX, globalY, tileX, tileY, raster);
            }
         } else {
            return Double.NaN;
         }
      } else {
         return Double.NaN;
      }
   }

   private static int downsampleStep(double worldScale, double resolutionMeters, double previewResolutionMeters) {
      if (!(worldScale > 0.0)) {
         return 1;
      } else if (!(effectiveSampleResolutionMeters(worldScale, previewResolutionMeters) >= resolutionMeters)) {
         return 1;
      } else {
         return Math.max(1, Mth.floor(resolutionMeters / worldScale));
      }
   }

   private static double downsampleBlock(double blockCoord, int step) {
      if (step <= 1) {
         return blockCoord;
      } else {
         int block = Mth.floor(blockCoord);
         int snapped = Math.floorDiv(block, step) * step;
         return snapped + step * 0.5;
      }
   }

   private static TellusElevationSource.TileKey tileKeyForBlock(double blockX, double blockZ, double worldScale, int zoom) {
      double blocksPerDegree = EarthProjection.blocksPerDegree(worldScale);
      double lon = EarthProjection.blockXToLon(blockX, worldScale);
      double lat = EarthProjection.blockZToLat(blockZ, worldScale);
      if (!(lat < MIN_LAT) && !(lat > MAX_LAT) && !(lon < MIN_LON) && !(lon > MAX_LON)) {
         double latRad = Math.toRadians(lat);
         double n = Math.pow(2.0, zoom);
         double x = (lon + 180.0) / 360.0 * n;
         double y = (1.0 - Math.log(Math.tan(latRad) + 1.0 / Math.cos(latRad)) / Math.PI) / 2.0 * n;
         if (!(x < 0.0) && !(y < 0.0) && !(x >= n) && !(y >= n)) {
            int tileX = Mth.floor(x);
            int tileY = Mth.floor(y);
            return new TellusElevationSource.TileKey(zoom, tileX, tileY);
         } else {
            return null;
         }
      } else {
         return null;
      }
   }

   private static double effectiveSampleResolutionMeters(double worldScale, double previewResolutionMeters) {
      return Double.isFinite(previewResolutionMeters) && previewResolutionMeters > 0.0 ? Math.max(worldScale, previewResolutionMeters) : worldScale;
   }

   private static TellusElevationSource.LatLon toLatLon(double blockX, double blockZ, double worldScale) {
      double blocksPerDegree = EarthProjection.blocksPerDegree(worldScale);
      double lon = EarthProjection.blockXToLon(blockX, worldScale);
      double lat = EarthProjection.blockZToLat(blockZ, worldScale);
      return !(lat < MIN_LAT) && !(lat > MAX_LAT) && !(lon < MIN_LON) && !(lon > MAX_LON) ? new TellusElevationSource.LatLon(lat, lon) : null;
   }

   private void prefetchTile( TellusElevationSource.TileKey key) {
      if (this.cache.getIfPresent(key) == null) {
         try {
            this.cache.get(key);
         } catch (Exception var3) {
            Tellus.LOGGER.debug("Failed to prefetch elevation tile {}", key, var3);
         }
      }
   }

   private static int intProperty(String key, int defaultValue) {
      String value = System.getProperty(key);
      if (value == null) {
         return defaultValue;
      } else {
         try {
            return Math.max(1, Integer.parseInt(value));
         } catch (NumberFormatException var4) {
            return defaultValue;
         }
      }
   }

   private static boolean booleanProperty(String key, boolean defaultValue) {
      String value = System.getProperty(key);
      return value == null ? defaultValue : Boolean.parseBoolean(value);
   }

   private ShortRaster getTile( TellusElevationSource.TileKey key) {
      try {
         ShortRaster raster = (ShortRaster)this.cache.get(key);
         return raster == MISSING_RASTER ? null : raster;
      } catch (Exception var3) {
         Tellus.LOGGER.warn("Failed to load elevation tile {}", key, var3);
         return null;
      }
   }

   private ShortRaster getTileLocalOnly(TellusElevationSource.TileKey key) {
      ShortRaster cached = (ShortRaster)this.cache.getIfPresent(key);
      if (cached != null) {
         return cached == MISSING_RASTER ? null : cached;
      } else {
         Path cachePath = this.cachePath(key);
         if (!Files.exists(cachePath)) {
            return null;
         } else {
            try (InputStream input = Files.newInputStream(cachePath)) {
               ShortRaster raster = readPngRaster(input);
               this.cache.put(key, raster);
               return raster;
            } catch (IOException error) {
               this.handleInvalidTile(cachePath, key, error);
               return null;
            }
         }
      }
   }

   private ShortRaster getTileMemoryOnly(TellusElevationSource.TileKey key) {
      ShortRaster cached = (ShortRaster)this.cache.getIfPresent(key);
      return cached == null || cached == MISSING_RASTER ? null : cached;
   }

   private ShortRaster loadTile( TellusElevationSource.TileKey key) {
      Path cachePath = this.cachePath(key);
      if (Files.exists(cachePath)) {
         try {
            ShortRaster var15;
            try (InputStream input = Files.newInputStream(cachePath)) {
               var15 = readPngRaster(input);
            }

            return var15;
         } catch (IOException var13) {
            this.handleInvalidTile(cachePath, key, var13);
         }
      }

      byte[] data;
      try {
         data = this.downloadTile(key);
      } catch (IOException var10) {
         Tellus.LOGGER.debug("Failed to download elevation tile {}", key, var10);
         return MISSING_RASTER;
      }

      if (data == null) {
         return MISSING_RASTER;
      } else {
         this.cacheTile(cachePath, data);

         try {
            ShortRaster var5;
            try (InputStream input = new ByteArrayInputStream(data)) {
               var5 = readPngRaster(input);
            }

            return var5;
         } catch (IOException var9) {
            this.handleInvalidTile(cachePath, key, var9);
            return MISSING_RASTER;
         }
      }
   }

   private byte[] downloadTile(TellusElevationSource.TileKey key) throws IOException {
      URI uri = URI.create(String.format("%s/%d/%d/%d.png", ENDPOINT, key.zoom(), key.x(), key.y()));
      HttpURLConnection connection = (HttpURLConnection)uri.toURL().openConnection();
      try {
         connection.setConnectTimeout(8000);
         connection.setReadTimeout(8000);
         connection.setRequestProperty("User-Agent", "Tellus/1.0 (Minecraft Mod)");
         if (connection.getResponseCode() == 404) {
            return null;
         } else {
            DownloadProgressReporter.requestStarted(connection.getContentLengthLong());

            byte[] var5;
            try (InputStream input = Objects.requireNonNull(connection.getInputStream(), "elevationTileResponse")) {
               var5 = DownloadProgressReporter.readAllBytesWithProgress(input);
            } finally {
               DownloadProgressReporter.requestFinished();
            }

            return var5;
         }
      } finally {
         connection.disconnect();
      }
   }

   private void cacheTile(Path cachePath, byte[] data) {
      try {
         Files.createDirectories(cachePath.getParent());
         Files.write(cachePath, data);
      } catch (IOException var4) {
         Tellus.LOGGER.warn("Failed to cache elevation tile {}", cachePath, var4);
      }
   }

   private Path cachePath(TellusElevationSource.TileKey key) {
      return this.cacheRoot.resolve(key.zoom() + "/" + key.x() + "/" + key.y() + ".png");
   }

   private void handleInvalidTile(Path cachePath, TellusElevationSource.TileKey key, IOException cause) {
      try {
         Files.deleteIfExists(cachePath);
      } catch (IOException var5) {
         Tellus.LOGGER.debug("Failed to delete invalid elevation tile cache {}", cachePath, var5);
      }

      Tellus.LOGGER.debug("Ignoring invalid elevation tile {} at {}", new Object[]{key, cachePath, cause});
   }

   private double sampleBilinearAcrossTiles(int zoom, double globalX, double globalY, int baseTileX, int baseTileY, ShortRaster baseRaster) {
      int tilesPerAxis = 1 << zoom;
      int maxPixel = tilesPerAxis * TILE_SIZE - 1;
      double clampedX = Mth.clamp(globalX, 0.0, maxPixel);
      double clampedY = Mth.clamp(globalY, 0.0, maxPixel);
      int x0 = Mth.floor(clampedX);
      int y0 = Mth.floor(clampedY);
      int x1 = Math.min(x0 + 1, maxPixel);
      int y1 = Math.min(y0 + 1, maxPixel);
      double dx = clampedX - x0;
      double dy = clampedY - y0;
      double v00 = this.samplePixel(zoom, x0, y0, baseTileX, baseTileY, baseRaster);
      double v10 = this.samplePixel(zoom, x1, y0, baseTileX, baseTileY, baseRaster);
      double v01 = this.samplePixel(zoom, x0, y1, baseTileX, baseTileY, baseRaster);
      double v11 = this.samplePixel(zoom, x1, y1, baseTileX, baseTileY, baseRaster);
      if (!Double.isNaN(v00) && !Double.isNaN(v10) && !Double.isNaN(v01) && !Double.isNaN(v11)) {
         double lerpX0 = Mth.lerp(dx, v00, v10);
         double lerpX1 = Mth.lerp(dx, v01, v11);
         return Mth.lerp(dy, lerpX0, lerpX1);
      } else {
         double localX = clampedX - baseTileX * TILE_SIZE;
         double localY = clampedY - baseTileY * TILE_SIZE;
         return sampleBilinearLocal(baseRaster, localX, localY);
      }
   }

   private double sampleBilinearAcrossTilesLocalOnly(int zoom, double globalX, double globalY, int baseTileX, int baseTileY, ShortRaster baseRaster) {
      int tilesPerAxis = 1 << zoom;
      int maxPixel = tilesPerAxis * TILE_SIZE - 1;
      double clampedX = Mth.clamp(globalX, 0.0, maxPixel);
      double clampedY = Mth.clamp(globalY, 0.0, maxPixel);
      int x0 = Mth.floor(clampedX);
      int y0 = Mth.floor(clampedY);
      int x1 = Math.min(x0 + 1, maxPixel);
      int y1 = Math.min(y0 + 1, maxPixel);
      double dx = clampedX - x0;
      double dy = clampedY - y0;
      double v00 = this.samplePixelLocalOnly(zoom, x0, y0, baseTileX, baseTileY, baseRaster);
      double v10 = this.samplePixelLocalOnly(zoom, x1, y0, baseTileX, baseTileY, baseRaster);
      double v01 = this.samplePixelLocalOnly(zoom, x0, y1, baseTileX, baseTileY, baseRaster);
      double v11 = this.samplePixelLocalOnly(zoom, x1, y1, baseTileX, baseTileY, baseRaster);
      if (!Double.isNaN(v00) && !Double.isNaN(v10) && !Double.isNaN(v01) && !Double.isNaN(v11)) {
         double lerpX0 = Mth.lerp(dx, v00, v10);
         double lerpX1 = Mth.lerp(dx, v01, v11);
         return Mth.lerp(dy, lerpX0, lerpX1);
      } else {
         double localX = clampedX - baseTileX * TILE_SIZE;
         double localY = clampedY - baseTileY * TILE_SIZE;
         return sampleBilinearLocal(baseRaster, localX, localY);
      }
   }

   private double sampleBilinearAcrossTilesMemoryOnly(int zoom, double globalX, double globalY, int baseTileX, int baseTileY, ShortRaster baseRaster) {
      int tilesPerAxis = 1 << zoom;
      int maxPixel = tilesPerAxis * TILE_SIZE - 1;
      double clampedX = Mth.clamp(globalX, 0.0, maxPixel);
      double clampedY = Mth.clamp(globalY, 0.0, maxPixel);
      int x0 = Mth.floor(clampedX);
      int y0 = Mth.floor(clampedY);
      int x1 = Math.min(x0 + 1, maxPixel);
      int y1 = Math.min(y0 + 1, maxPixel);
      double dx = clampedX - x0;
      double dy = clampedY - y0;
      double v00 = this.samplePixelMemoryOnly(zoom, x0, y0, baseTileX, baseTileY, baseRaster);
      double v10 = this.samplePixelMemoryOnly(zoom, x1, y0, baseTileX, baseTileY, baseRaster);
      double v01 = this.samplePixelMemoryOnly(zoom, x0, y1, baseTileX, baseTileY, baseRaster);
      double v11 = this.samplePixelMemoryOnly(zoom, x1, y1, baseTileX, baseTileY, baseRaster);
      if (!Double.isNaN(v00) && !Double.isNaN(v10) && !Double.isNaN(v01) && !Double.isNaN(v11)) {
         double lerpX0 = Mth.lerp(dx, v00, v10);
         double lerpX1 = Mth.lerp(dx, v01, v11);
         return Mth.lerp(dy, lerpX0, lerpX1);
      } else {
         return Double.NaN;
      }
   }

   private double samplePixel(int zoom, int pixelX, int pixelY, int baseTileX, int baseTileY, ShortRaster baseRaster) {
      int tileX = Math.floorDiv(pixelX, TILE_SIZE);
      int tileY = Math.floorDiv(pixelY, TILE_SIZE);
      ShortRaster raster = tileX == baseTileX && tileY == baseTileY ? baseRaster : this.getTile(new TellusElevationSource.TileKey(zoom, tileX, tileY));
      if (raster == null) {
         return Double.NaN;
      } else {
         int localX = pixelX - tileX * TILE_SIZE;
         int localY = pixelY - tileY * TILE_SIZE;
         return raster.get(localX, localY);
      }
   }

   private double samplePixelLocalOnly(int zoom, int pixelX, int pixelY, int baseTileX, int baseTileY, ShortRaster baseRaster) {
      int tileX = Math.floorDiv(pixelX, TILE_SIZE);
      int tileY = Math.floorDiv(pixelY, TILE_SIZE);
      ShortRaster raster = tileX == baseTileX && tileY == baseTileY
         ? baseRaster
         : this.getTileLocalOnly(new TellusElevationSource.TileKey(zoom, tileX, tileY));
      if (raster == null) {
         return Double.NaN;
      } else {
         int localX = pixelX - tileX * TILE_SIZE;
         int localY = pixelY - tileY * TILE_SIZE;
         return raster.get(localX, localY);
      }
   }

   private double samplePixelMemoryOnly(int zoom, int pixelX, int pixelY, int baseTileX, int baseTileY, ShortRaster baseRaster) {
      int tileX = Math.floorDiv(pixelX, TILE_SIZE);
      int tileY = Math.floorDiv(pixelY, TILE_SIZE);
      ShortRaster raster = tileX == baseTileX && tileY == baseTileY
         ? baseRaster
         : this.getTileMemoryOnly(new TellusElevationSource.TileKey(zoom, tileX, tileY));
      if (raster == null) {
         return Double.NaN;
      } else {
         int localX = pixelX - tileX * TILE_SIZE;
         int localY = pixelY - tileY * TILE_SIZE;
         return raster.get(localX, localY);
      }
   }

   private static double sampleBilinearLocal(ShortRaster raster, double x, double y) {
      int maxX = raster.width() - 1;
      int maxY = raster.height() - 1;
      int x0 = Mth.clamp(Mth.floor(x), 0, maxX);
      int y0 = Mth.clamp(Mth.floor(y), 0, maxY);
      int x1 = Math.min(x0 + 1, maxX);
      int y1 = Math.min(y0 + 1, maxY);
      double dx = x - x0;
      double dy = y - y0;
      double v00 = raster.get(x0, y0);
      double v10 = raster.get(x1, y0);
      double v01 = raster.get(x0, y1);
      double v11 = raster.get(x1, y1);
      double lerpX0 = Mth.lerp(dx, v00, v10);
      double lerpX1 = Mth.lerp(dx, v01, v11);
      return Mth.lerp(dy, lerpX0, lerpX1);
   }

   private static int selectZoom(double worldScale) {
      double zoom = zoomForScale(worldScale);
      return Math.max((int)Math.round(zoom), 0);
   }

   private static TellusElevationSource.ElevationDiagnostic diagnostic(double elevation, TellusElevationSource.DemUsage provider) {
      return new TellusElevationSource.ElevationDiagnostic(elevation, provider, provider.bit(), provider.nominalResolutionMeters());
   }

   private static TellusElevationSource.ElevationDiagnostic diagnostic(
      double elevation, TellusElevationSource.DemUsage primaryProvider, int providerMask, double sourceResolutionMeters
   ) {
      double resolvedResolution = Double.isFinite(sourceResolutionMeters) && sourceResolutionMeters > 0.0
         ? sourceResolutionMeters
         : primaryProvider.nominalResolutionMeters();
      return new TellusElevationSource.ElevationDiagnostic(elevation, primaryProvider, providerMask, resolvedResolution);
   }

   private static double zoomForScale(double meters) {
      return Math.log(EQUATOR_CIRCUMFERENCE / (TILE_SIZE * meters)) / Math.log(2.0);
   }

   private static ShortRaster readPngRaster(InputStream input) throws IOException {
      BufferedImage image = ImageIO.read(input);
      if (image == null) {
         throw new IOException("Invalid tellus PNG tile");
      } else {
         int width = image.getWidth();
         int height = image.getHeight();
         ShortRaster raster = ShortRaster.create(width, height);

         for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
               int argb = image.getRGB(x, y);
               int red = argb >> 16 & 0xFF;
               int green = argb >> 8 & 0xFF;
               int blue = argb & 0xFF;
               double elevation = red * TILE_SIZE + green + blue / (double)TILE_SIZE - 32768.0;
               raster.set(x, y, (short)Math.round(elevation));
            }
         }

         return raster;
      }
   }

   @Override
   public TellusCacheDomain cacheDomain() {
      return TellusCacheDomain.TERRAIN;
   }

   @Override
   public void clearCache() {
      this.cache.invalidateAll();
      this.cache.cleanUp();
   }

   private record TileKey(int zoom, int x, int y) {
   }

   private record LatLon(double lat, double lon) {
   }


   public static enum DemUsage {
      TERRAIN_TILES("terrarium", 1),
      SWISSALTI3D_05M("swissalti3d_05m", 1 << 1),
      SWISSALTI3D_2M("swissalti3d_2m", 1 << 2),
      AHN("ahn", 1 << 3),
      CANELEVATION_2M("canelevation_2m", 1 << 4),
      CANELEVATION_30M("canelevation_30m", 1 << 5),
      NORWAYDTM1("norwaydtm1", 1 << 6),
      USGS("usgs", 1 << 7),
      COPERNICUS("copernicus", 1 << 8),
      HMA("hma", 1 << 9),
      ARCTICDEM("arcticdem", 1 << 10),
      REMA("rema", 1 << 11),
      JAPANGSI("japangsi", 1 << 12);

      private final String providerId;
      private final int bit;

      private DemUsage(String providerId, int bit) {
         this.providerId = Objects.requireNonNull(providerId, "providerId");
         this.bit = bit;
      }

      public int bit() {
         return this.bit;
      }

      public String providerId() {
         return this.providerId;
      }

      public double nominalResolutionMeters() {
         return switch (this) {
            case TERRAIN_TILES -> 30.0;
            case SWISSALTI3D_05M -> 0.5;
            case SWISSALTI3D_2M -> 2.0;
            case AHN -> 0.5;
            case CANELEVATION_2M -> 2.0;
            case CANELEVATION_30M -> 30.0;
            case NORWAYDTM1 -> 1.0;
            case USGS -> 10.0;
            case COPERNICUS -> 30.0;
            case HMA -> 8.0;
            case ARCTICDEM -> 2.0;
            case REMA -> 8.0;
            case JAPANGSI -> Double.NaN;
         };
      }

      public Component label() {
         return Component.translatable("property.tellus.dem_provider.value." + this.providerId);
      }
   }

   public record ElevationDiagnostic(
      double elevation, TellusElevationSource.DemUsage primaryProvider, int providerMask, double sourceResolutionMeters
   ) {
      public ElevationDiagnostic(
         double elevation, TellusElevationSource.DemUsage primaryProvider, int providerMask, double sourceResolutionMeters
      ) {
         this.elevation = elevation;
         this.primaryProvider = Objects.requireNonNull(primaryProvider, "primaryProvider");
         this.providerMask = providerMask;
         this.sourceResolutionMeters = sourceResolutionMeters;
      }

      public boolean usesMultipleProviders() {
         return Integer.bitCount(this.providerMask) > 1;
      }

      public double displayResolutionMeters() {
         return Double.isFinite(this.sourceResolutionMeters) && this.sourceResolutionMeters > 0.0
            ? this.sourceResolutionMeters
            : this.primaryProvider.nominalResolutionMeters();
      }
   }

}
