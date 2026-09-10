package com.yucareux.tellus.world.data.elevation;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yucareux.tellus.legacy.backend.GeoChunk;
import com.yucareux.tellus.legacy.backend.GeoView;
import com.yucareux.tellus.legacy.backend.earth.EarthAttachments;
import com.yucareux.tellus.legacy.backend.earth.EarthLayers;
import com.yucareux.tellus.legacy.backend.earth.EarthTiles;
import com.yucareux.tellus.legacy.backend.loader.ConcurrencyLimiter;
import com.yucareux.tellus.legacy.backend.projection.Projection;
import com.yucareux.tellus.legacy.backend.projection.cylindrical.Equirectangular;
import com.yucareux.tellus.legacy.backend.raster.RasterShape;
import com.yucareux.tellus.legacy.backend.raster.ShortRaster;
import com.yucareux.tellus.legacy.backend.tile.GuavaTileCache;
import com.yucareux.tellus.worldgen.EarthCoordinateShift;
import com.yucareux.tellus.worldgen.EarthGeneratorSettings;
import java.net.http.HttpClient;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * Synchronous chunk-granular facade over the gegy.dev elevation pyramid
 * ({@code EarthTiles}/{@code EarthLayers}, {@code https://terrarium.gegy.dev/geo3}).
 *
 * <p>This is the same dataset the Meridian and Distant Horizons LOD paths sample
 * ({@code TellusTerrainSampler}, {@code LegacyLodGenerator}), exposed here in the
 * blocking per-block form regular chunk generation needs. Elevations are metres
 * above sea level, matching the old preview sampler contract, so all existing
 * metres-to-blocks conversions in callers are unchanged.
 *
 * <p>One 16x16 short raster is fetched per chunk through {@code EarthLayers} (which
 * picks pyramid levels and shares its tile/file caches with the LOD paths) and held
 * in a small Guava cache. Variants mirror the old sampler: blocking (fetches on
 * miss), local-only (memory hits only, {@code 0.0} on miss) and memory-only
 * ({@code NaN} on miss).
 */
public final class GegyElevationSource {
   private static final int CHUNK_SIZE = 16;
   private static final int CHUNK_MASK = CHUNK_SIZE - 1;
   private static final short[] MISSING = new short[0];
   private static final ConcurrentHashMap<GegyElevationSource.GegyKey, GegyElevationSource> INSTANCES = new ConcurrentHashMap<>();

   public static GegyElevationSource forSettings(final EarthGeneratorSettings settings) {
      final GegyElevationSource.GegyKey key = new GegyElevationSource.GegyKey(
         settings.worldScale(),
         EarthCoordinateShift.spawnOffsetX(settings),
         EarthCoordinateShift.spawnOffsetZ(settings)
      );
      return INSTANCES.computeIfAbsent(key, GegyElevationSource::create);
   }

   private final int spawnOffsetX;
   private final int spawnOffsetZ;
   private final EarthLayers earthLayers;
   private final LoadingCache<Long, short[]> chunks;

   private GegyElevationSource(final GegyElevationSource.GegyKey key, final EarthLayers earthLayers) {
      this.spawnOffsetX = key.offsetX();
      this.spawnOffsetZ = key.offsetZ();
      this.earthLayers = earthLayers;
      this.chunks = CacheBuilder.newBuilder()
         .maximumSize(1024)
         .expireAfterAccess(5, TimeUnit.MINUTES)
         .build(new CacheLoader<Long, short[]>() {
            @Override
            public short[] load(final Long key) {
               return GegyElevationSource.this.fetchChunk(key);
            }
         });
   }

   private static GegyElevationSource create(final GegyElevationSource.GegyKey key) {
      final Projection projection = new Equirectangular(key.worldScale());
      final EarthTiles.Config config = new EarthTiles.Config(
         HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build(),
         new ConcurrencyLimiter(16),
         Paths.get("tellus_cache", "legacy"),
         ForkJoinPool.commonPool(),
         ForkJoinPool.commonPool()
      );
      final EarthTiles tiles = config.create(new GuavaTileCache(Duration.ofMinutes(5), 1000));
      return new GegyElevationSource(key, EarthLayers.create(tiles, projection, ForkJoinPool.commonPool()));
   }

   /**
    * Elevation in metres, fetching (including network) on cache miss.
    * Returns {@code 0.0} when the column cannot be resolved.
    */
   public double sampleMeters(final double blockX, final double blockZ) {
      final short[] grid = loadChunkGrid(blockX, blockZ);
      if (grid.length == 0) {
         return 0.0;
      }
      return grid[gridIndex(blockX, blockZ)];
   }

   /**
    * Elevation in metres without any fetching. Returns {@code 0.0} unless the
    * chunk is already in memory.
    */
   public double sampleMetersLocalOnly(final double blockX, final double blockZ) {
      final short[] grid = this.chunks.getIfPresent(chunkKey(blockX, blockZ));
      if (grid == null || grid.length == 0) {
         return 0.0;
      }
      return grid[gridIndex(blockX, blockZ)];
   }

   /**
    * Elevation in metres without any fetching. Returns {@code NaN} unless the
    * chunk is already in memory.
    */
   public double sampleMetersMemoryOnly(final double blockX, final double blockZ) {
      final short[] grid = this.chunks.getIfPresent(chunkKey(blockX, blockZ));
      if (grid == null || grid.length == 0) {
         return Double.NaN;
      }
      return grid[gridIndex(blockX, blockZ)];
   }

   /**
    * Synchronously warms the cache for every chunk intersecting the block
    * square centred on {@code (centerX, centerZ)} with the given chunk radius.
    * Callers already run this off the latency-sensitive path.
    */
   public void prefetch(final double centerX, final double centerZ, final int chunkRadius) {
      final int centerChunkX = chunkCoord(centerX);
      final int centerChunkZ = chunkCoord(centerZ);
      final int radius = Math.max(0, chunkRadius);
      for (int dz = -radius; dz <= radius; dz++) {
         for (int dx = -radius; dx <= radius; dx++) {
            loadChunkGrid(centerChunkX + dx, centerChunkZ + dz);
         }
      }
   }

   private short[] loadChunkGrid(final double blockX, final double blockZ) {
      return loadChunkGrid(chunkCoord(blockX), chunkCoord(blockZ));
   }

   private short[] loadChunkGrid(final int chunkX, final int chunkZ) {
      try {
         final short[] grid = this.chunks.get(chunkKey(chunkX, chunkZ));
         return grid == null ? MISSING : grid;
      } catch (final Exception ignored) {
         return MISSING;
      }
   }

   private short[] fetchChunk(final Long key) {
      final int chunkX = (int) (key >> 32);
      final int chunkZ = (int) (key.longValue());
      final int earthX0 = chunkX * CHUNK_SIZE + this.spawnOffsetX;
      final int earthZ0 = chunkZ * CHUNK_SIZE + this.spawnOffsetZ;
      try {
         final Optional<GeoChunk> chunk = this.earthLayers
            .get(new GeoView(earthX0, earthZ0, earthX0 + CHUNK_SIZE - 1, earthZ0 + CHUNK_SIZE - 1),
               new RasterShape(CHUNK_SIZE, CHUNK_SIZE))
            .join();
         if (chunk.isEmpty()) {
            return MISSING;
         }
         final Optional<EarthAttachments> earth = EarthAttachments.from(chunk.get());
         if (earth.isEmpty()) {
            return MISSING;
         }
         final ShortRaster elevation = earth.get().elevation();
         if (elevation.width() < CHUNK_SIZE || elevation.height() < CHUNK_SIZE) {
            return MISSING;
         }
         final short[] grid = new short[CHUNK_SIZE * CHUNK_SIZE];
         for (int z = 0; z < CHUNK_SIZE; z++) {
            for (int x = 0; x < CHUNK_SIZE; x++) {
               grid[z * CHUNK_SIZE + x] = (short) Math.max(Short.MIN_VALUE,
                  Math.min(Short.MAX_VALUE, elevation.getInt(x, z)));
            }
         }
         return grid;
      } catch (final RuntimeException ignored) {
         return MISSING;
      }
   }

   private static long chunkKey(final double blockX, final double blockZ) {
      return chunkKey(chunkCoord(blockX), chunkCoord(blockZ));
   }

   private static long chunkKey(final int chunkX, final int chunkZ) {
      return ((long) chunkX << 32) | (chunkZ & 0xFFFFFFFFL);
   }

   private static int chunkCoord(final double block) {
      return Math.floorDiv((int) Math.floor(block), CHUNK_SIZE);
   }

   private static int gridIndex(final double blockX, final double blockZ) {
      return (((int) Math.floor(blockZ)) & CHUNK_MASK) * CHUNK_SIZE + (((int) Math.floor(blockX)) & CHUNK_MASK);
   }

   private record GegyKey(double worldScale, int offsetX, int offsetZ) {
   }
}
