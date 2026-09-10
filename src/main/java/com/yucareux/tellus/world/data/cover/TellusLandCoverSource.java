package com.yucareux.tellus.world.data.cover;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.yucareux.tellus.Tellus;
import com.yucareux.tellus.cache.TellusCacheDomain;
import com.yucareux.tellus.cache.TellusCacheHandle;
import com.yucareux.tellus.cache.TellusCacheRegistry;
import com.yucareux.tellus.world.data.satellite.SatelliteTileSampler;
import com.yucareux.tellus.world.data.source.DownloadProgressReporter;
import com.yucareux.tellus.worldgen.EarthProjection;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.InflaterInputStream;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Mth;

public final class TellusLandCoverSource implements TellusCacheHandle {

    private static final double MIN_LAT = -60.0;
    private static final double MAX_LAT = 84.0;
    private static final double MIN_LON = -180.0;
    private static final double MAX_LON = 180.0;
    private static final int TILE_DEGREES = 3;
    private static final int SNOW_ICE_CLASS = 70;
    private static final int WATER_CLASS = 80;
    private static final int MANGROVES_CLASS = 95;
    private static final int BUILT_UP_CLASS = 50;
    private static final int NO_DATA_CLASS = 0;
    private static final int MAX_CACHE_TILES = intProperty(
        "tellus.landcover.cacheTiles",
        64
    );
    private static final double RESOLUTION_METERS = 10.0;
    private static final int TILE_CACHE_ENTRIES = intProperty(
        "tellus.landcover.tileCacheEntries",
        32
    );
    private static final int SMOOTH_RADIUS_PIXELS = 1;
    private static final ThreadLocal<
        TellusLandCoverSource.CoverSmoothScratch
    > COVER_SMOOTH_SCRATCH = ThreadLocal.withInitial(
        TellusLandCoverSource.CoverSmoothScratch::new
    );
    private static final ThreadLocal<
        TellusLandCoverSource.CoverBlendScratch
    > COVER_BLEND_SCRATCH = ThreadLocal.withInitial(
        TellusLandCoverSource.CoverBlendScratch::new
    );
    private static final String ENDPOINT =
        "https://esa-worldcover.s3.eu-central-1.amazonaws.com/v200/2021/map";
    private static final String TILE_PATTERN =
        "ESA_WorldCover_10m_2021_v200_%s_Map.tif";
    private final Path cacheRoot = FabricLoader.getInstance()
        .getGameDir()
        .resolve("tellus/cache/worldcover2021");
    private final LoadingCache<
        TellusLandCoverSource.TileKey,
        TellusLandCoverSource.GeoTiffTile
    > cache = CacheBuilder.newBuilder()
        .maximumSize(MAX_CACHE_TILES)
        .removalListener(notification -> {
            TellusLandCoverSource.GeoTiffTile tile =
                (TellusLandCoverSource.GeoTiffTile) notification.getValue();
            if (tile != null) {
                tile.close();
            }
        })
        .build(
            new CacheLoader<
                TellusLandCoverSource.TileKey,
                TellusLandCoverSource.GeoTiffTile
            >() {
                public TellusLandCoverSource.GeoTiffTile load(
                    TellusLandCoverSource.TileKey key
                ) throws Exception {
                    return TellusLandCoverSource.this.loadTile(key);
                }
            }
        );
    private final SatelliteTileSampler satelliteSampler =
        new SatelliteTileSampler(
            HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5))
                .build(),
            FabricLoader.getInstance()
                .getGameDir()
                .resolve("tellus/cache/satellite")
        );

    public TellusLandCoverSource() {
        TellusCacheRegistry.register(this);
    }

    public boolean isSnowIce(double blockX, double blockZ, double worldScale) {
        return (
            this.sampleCoverClass(blockX, blockZ, worldScale) == SNOW_ICE_CLASS
        );
    }

    public int sampleCoverClass(
        double blockX,
        double blockZ,
        double worldScale
    ) {
        return this.sampleCoverClass(blockX, blockZ, worldScale, worldScale);
    }

    public int sampleCoverClass(
        double blockX,
        double blockZ,
        double worldScale,
        double previewResolutionMeters
    ) {
        return this.sampleCoverClass(
            blockX,
            blockZ,
            worldScale,
            previewResolutionMeters,
            false
        );
    }

    public int sampleCoverClassLocalOnly(
        double blockX,
        double blockZ,
        double worldScale,
        double previewResolutionMeters
    ) {
        return this.sampleCoverClass(
            blockX,
            blockZ,
            worldScale,
            previewResolutionMeters,
            true
        );
    }

    public int sampleCoverClassMemoryOnly(
        double blockX,
        double blockZ,
        double worldScale,
        double previewResolutionMeters
    ) {
        if (worldScale <= 0.0) {
            return Integer.MIN_VALUE;
        } else {
            int step = downsampleStep(
                worldScale,
                RESOLUTION_METERS,
                previewResolutionMeters
            );
            if (step > 1) {
                blockX = downsampleBlock(blockX, step);
                blockZ = downsampleBlock(blockZ, step);
            }

            double lon = EarthProjection.blockXToLon(blockX, worldScale);
            double lat = EarthProjection.blockZToLat(blockZ, worldScale);
            return this.sampleSatelliteCoverClassAtLonLatMemoryOnly(
                lon,
                lat,
                worldScale,
                previewResolutionMeters
            );
        }
    }

    private int sampleCoverClass(
        double blockX,
        double blockZ,
        double worldScale,
        double previewResolutionMeters,
        boolean localOnly
    ) {
        if (worldScale <= 0.0) {
            return 0;
        } else {
            int step = downsampleStep(
                worldScale,
                RESOLUTION_METERS,
                previewResolutionMeters
            );
            if (step > 1) {
                blockX = downsampleBlock(blockX, step);
                blockZ = downsampleBlock(blockZ, step);
            }

            double lon = EarthProjection.blockXToLon(blockX, worldScale);
            double lat = EarthProjection.blockZToLat(blockZ, worldScale);
            return localOnly
                ? this.sampleSatelliteCoverClassAtLonLatLocalOnly(
                      lon,
                      lat,
                      worldScale,
                      previewResolutionMeters
                  )
                : this.sampleSatelliteCoverClassAtLonLat(
                      lon,
                      lat,
                      worldScale,
                      previewResolutionMeters
                  );
        }
    }

    public int sampleSmoothedCoverClass(
        double blockX,
        double blockZ,
        double worldScale
    ) {
        return this.sampleSmoothedCoverClass(
            blockX,
            blockZ,
            worldScale,
            worldScale
        );
    }

    public int sampleSmoothedCoverClass(
        double blockX,
        double blockZ,
        double worldScale,
        double previewResolutionMeters
    ) {
        if (worldScale <= 0.0) {
            return 0;
        } else {
            int step = downsampleStep(
                worldScale,
                RESOLUTION_METERS,
                previewResolutionMeters
            );
            if (step > 1) {
                blockX = downsampleBlock(blockX, step);
                blockZ = downsampleBlock(blockZ, step);
            }

            double blocksPerDegree = EarthProjection.blocksPerDegree(
                worldScale
            );
            double lon = EarthProjection.blockXToLon(blockX, worldScale);
            double lat = EarthProjection.blockZToLat(blockZ, worldScale);
            return this.sampleSmoothedCoverClassAtLonLat(
                lon,
                lat,
                SMOOTH_RADIUS_PIXELS
            );
        }
    }

    public int sampleVisualCoverClass(
        double blockX,
        double blockZ,
        double worldScale
    ) {
        return this.sampleVisualCoverClass(
            blockX,
            blockZ,
            worldScale,
            worldScale
        );
    }

    public int sampleVisualCoverClass(
        double blockX,
        double blockZ,
        double worldScale,
        double previewResolutionMeters
    ) {
        return this.sampleVisualCoverClass(
            blockX,
            blockZ,
            worldScale,
            previewResolutionMeters,
            false
        );
    }

    public int sampleVisualCoverClassLocalOnly(
        double blockX,
        double blockZ,
        double worldScale,
        double previewResolutionMeters
    ) {
        return this.sampleVisualCoverClass(
            blockX,
            blockZ,
            worldScale,
            previewResolutionMeters,
            true
        );
    }

    public int sampleVisualCoverClassMemoryOnly(
        double blockX,
        double blockZ,
        double worldScale,
        double previewResolutionMeters
    ) {
        if (worldScale <= 0.0) {
            return Integer.MIN_VALUE;
        } else {
            int step = downsampleStep(
                worldScale,
                RESOLUTION_METERS,
                previewResolutionMeters
            );
            if (step > 1) {
                blockX = downsampleBlock(blockX, step);
                blockZ = downsampleBlock(blockZ, step);
            }

            double lon = EarthProjection.blockXToLon(blockX, worldScale);
            double lat = EarthProjection.blockZToLat(blockZ, worldScale);
            return this.sampleSatelliteVisualCoverClassAtLonLatMemoryOnly(
                lon,
                lat,
                worldScale,
                previewResolutionMeters
            );
        }
    }

    private int sampleVisualCoverClass(
        double blockX,
        double blockZ,
        double worldScale,
        double previewResolutionMeters,
        boolean localOnly
    ) {
        if (worldScale <= 0.0) {
            return 0;
        } else {
            int step = downsampleStep(
                worldScale,
                RESOLUTION_METERS,
                previewResolutionMeters
            );
            if (step > 1) {
                blockX = downsampleBlock(blockX, step);
                blockZ = downsampleBlock(blockZ, step);
            }

            double lon = EarthProjection.blockXToLon(blockX, worldScale);
            double lat = EarthProjection.blockZToLat(blockZ, worldScale);
            int sampled = localOnly
                ? this.sampleSatelliteVisualCoverClassAtLonLatLocalOnly(
                      lon,
                      lat,
                      worldScale,
                      previewResolutionMeters
                  )
                : this.sampleSatelliteVisualCoverClassAtLonLat(
                      lon,
                      lat,
                      worldScale,
                      previewResolutionMeters
                  );
            return sampled == Integer.MIN_VALUE ? 0 : sampled;
        }
    }

    private int sampleCoverClassAtLonLat(double lon, double lat) {
        return this.sampleSatelliteCoverClassAtLonLat(lon, lat, 10.0D, 10.0D);
    }

    private int sampleSatelliteCoverClassAtLonLat(
        double lon,
        double lat,
        double worldScale,
        double previewResolutionMeters
    ) {
        int rgb = this.sampleSatelliteRgb(
            lon,
            lat,
            worldScale,
            previewResolutionMeters,
            false
        );
        return rgb < 0
            ? NO_DATA_CLASS
            : this.classifySatelliteCoverClass(rgb, lat, lon);
    }

    private int sampleSatelliteCoverClassAtLonLatLocalOnly(
        double lon,
        double lat,
        double worldScale,
        double previewResolutionMeters
    ) {
        int rgb = this.sampleSatelliteRgb(
            lon,
            lat,
            worldScale,
            previewResolutionMeters,
            true
        );
        return rgb < 0
            ? NO_DATA_CLASS
            : this.classifySatelliteCoverClass(rgb, lat, lon);
    }

    private int sampleSatelliteCoverClassAtLonLatMemoryOnly(
        double lon,
        double lat,
        double worldScale,
        double previewResolutionMeters
    ) {
        int rgb = this.sampleSatelliteRgb(
            lon,
            lat,
            worldScale,
            previewResolutionMeters,
            true
        );
        return rgb < 0
            ? Integer.MIN_VALUE
            : this.classifySatelliteCoverClass(rgb, lat, lon);
    }

    private int sampleSatelliteVisualCoverClassAtLonLat(
        double lon,
        double lat,
        double worldScale,
        double previewResolutionMeters
    ) {
        int rgb = this.sampleSatelliteRgb(
            lon,
            lat,
            worldScale,
            previewResolutionMeters,
            false
        );
        return rgb < 0
            ? NO_DATA_CLASS
            : this.classifySatelliteCoverClass(rgb, lat, lon);
    }

    private int sampleSatelliteVisualCoverClassAtLonLatLocalOnly(
        double lon,
        double lat,
        double worldScale,
        double previewResolutionMeters
    ) {
        int rgb = this.sampleSatelliteRgb(
            lon,
            lat,
            worldScale,
            previewResolutionMeters,
            true
        );
        return rgb < 0
            ? NO_DATA_CLASS
            : this.classifySatelliteCoverClass(rgb, lat, lon);
    }

    private int sampleSatelliteVisualCoverClassAtLonLatMemoryOnly(
        double lon,
        double lat,
        double worldScale,
        double previewResolutionMeters
    ) {
        int rgb = this.sampleSatelliteRgb(
            lon,
            lat,
            worldScale,
            previewResolutionMeters,
            true
        );
        return rgb < 0
            ? Integer.MIN_VALUE
            : this.classifySatelliteCoverClass(rgb, lat, lon);
    }

    private int sampleSatelliteRgb(
        double lon,
        double lat,
        double worldScale,
        double previewResolutionMeters,
        boolean nonBlocking
    ) {
        double effectiveResolutionMeters = Math.max(
            1.0D,
            Math.max(worldScale, previewResolutionMeters)
        );
        int zoom = this.satelliteZoomForResolution(effectiveResolutionMeters);
        return nonBlocking
            ? this.satelliteSampler.sampleRgbNonBlocking(lat, lon, zoom)
            : this.satelliteSampler.sampleRgb(lat, lon, zoom);
    }

    private int classifySatelliteCoverClass(
        int rgb,
        double latitude,
        double longitude
    ) {
        final int r = (rgb >> 16) & 0xFF;
        final int g = (rgb >> 8) & 0xFF;
        final int b = rgb & 0xFF;
        final double rf = r / 255.0D;
        final double gf = g / 255.0D;
        final double bf = b / 255.0D;
        final double max = Math.max(rf, Math.max(gf, bf));
        final double min = Math.min(rf, Math.min(gf, bf));
        final double chroma = max - min;
        final double saturation = max <= 1.0E-5D ? 0.0D : chroma / max;
        final double value = max;
        final double blueDominance = bf - Math.max(rf, gf);
        final double greenDominance = gf - Math.max(rf, bf);
        final double redDominance = rf - Math.max(gf, bf);
        final double vari = (gf - rf) / Math.max(0.12D, gf + rf - bf);
        final double exg = (2.0D * gf) - rf - bf;
        final double vegetationStrength = Mth.clamp(
            (vari * 0.9D) +
                (Math.max(0.0D, exg) * 0.55D) +
                (Math.max(0.0D, greenDominance) * 0.6D) +
                (Math.max(0.0D, (gf - rf) / Math.max(1e-6D, gf + rf)) * 0.4D),
            0.0D,
            1.0D
        );
        final double absLat = Math.abs(latitude);

        if (
            this.looksLikeWater(
                rf,
                gf,
                bf,
                saturation,
                value,
                blueDominance,
                greenDominance
            )
        ) {
            return WATER_CLASS;
        }

        if (
            value > 0.90D &&
            saturation < 0.12D &&
            Math.abs(rf - gf) < 0.05D &&
            Math.abs(gf - bf) < 0.05D
        ) {
            return SNOW_ICE_CLASS;
        }

        if (
            value > 0.74D &&
            saturation < 0.20D &&
            Math.abs(rf - gf) < 0.08D &&
            Math.abs(gf - bf) < 0.08D
        ) {
            return BUILT_UP_CLASS;
        }

        if (vegetationStrength > 0.62D && value < 0.74D) {
            return 10;
        }

        if (vegetationStrength > 0.50D) {
            if (absLat < 30.0D && blueDominance > -0.04D && value < 0.62D) {
                return MANGROVES_CLASS;
            }

            return value < 0.52D ? 20 : 30;
        }

        if (value < 0.40D && saturation > 0.18D && redDominance > -0.03D) {
            return 60;
        }

        if (value > 0.56D && saturation < 0.20D) {
            return absLat > 55.0D ? 100 : 50;
        }

        if (greenDominance > 0.01D) {
            return value < 0.58D ? 20 : 30;
        }

        return 60;
    }

    private boolean looksLikeWater(
        double r,
        double g,
        double b,
        double saturation,
        double value,
        double blueDominance,
        double greenDominance
    ) {
        return (
            blueDominance > 0.12D &&
            saturation > 0.12D &&
            value < 0.58D &&
            g < 0.55D &&
            (b > r || blueDominance > 0.18D) &&
            greenDominance < 0.05D
        );
    }

    private int satelliteZoomForResolution(double effectiveResolutionMeters) {
        if (!(effectiveResolutionMeters > 0.0D)) {
            return 12;
        }

        double ratio = 10.0D / effectiveResolutionMeters;
        int zoom = 14 + (int) Math.round(Math.log(ratio) / Math.log(2.0D));
        return Mth.clamp(zoom, 6, 15);
    }

    private int tileCountForRadius(int radius) {
        return Math.max(0, radius);
    }

    private int sampleSatelliteCoverClassAtLonLat(double lon, double lat) {
        TellusLandCoverSource.TileKey key = tileKeyForLonLat(lon, lat);
        if (key == null) {
            return NO_DATA_CLASS;
        } else {
            TellusLandCoverSource.GeoTiffTile tile = this.getTile(key);
            return tile.sample(lon, lat);
        }
    }

    private int sampleCoverClassAtLonLatLocalOnly(double lon, double lat) {
        TellusLandCoverSource.TileKey key = tileKeyForLonLat(lon, lat);
        if (key == null) {
            return NO_DATA_CLASS;
        } else {
            TellusLandCoverSource.GeoTiffTile tile = this.getTileLocalOnly(key);
            return tile.sample(lon, lat);
        }
    }

    private int sampleCoverClassAtLonLatMemoryOnly(double lon, double lat) {
        TellusLandCoverSource.TileKey key = tileKeyForLonLat(lon, lat);
        if (key == null) {
            return Integer.MIN_VALUE;
        } else {
            TellusLandCoverSource.GeoTiffTile tile = this.getTileMemoryOnly(
                key
            );
            return tile == null ? Integer.MIN_VALUE : tile.sample(lon, lat);
        }
    }

    private int sampleSmoothedCoverClassAtLonLat(
        double lon,
        double lat,
        int radiusPixels
    ) {
        TellusLandCoverSource.TileKey key = tileKeyForLonLat(lon, lat);
        if (key == null) {
            return NO_DATA_CLASS;
        } else {
            TellusLandCoverSource.GeoTiffTile tile = this.getTile(key);
            TellusLandCoverSource.Pixel center = tile.toPixel(lon, lat);
            if (center == null) {
                return NO_DATA_CLASS;
            } else {
                int centerValue = tile.sampleValue(center.x(), center.y());
                if (
                    radiusPixels > 0 &&
                    centerValue != WATER_CLASS &&
                    centerValue != NO_DATA_CLASS
                ) {
                    TellusLandCoverSource.CoverSmoothScratch scratch =
                        COVER_SMOOTH_SCRATCH.get();
                    scratch.reset();
                    if (
                        tile.isNeighborhoodInBounds(
                            center.x(),
                            center.y(),
                            radiusPixels
                        )
                    ) {
                        for (int dy = -radiusPixels; dy <= radiusPixels; dy++) {
                            int py = center.y() + dy;

                            for (
                                int dx = -radiusPixels;
                                dx <= radiusPixels;
                                dx++
                            ) {
                                int px = center.x() + dx;
                                int value = tile.sampleValue(px, py);
                                if (
                                    value != WATER_CLASS &&
                                    value != NO_DATA_CLASS
                                ) {
                                    scratch.add(value);
                                }
                            }
                        }
                    } else {
                        for (int dy = -radiusPixels; dy <= radiusPixels; dy++) {
                            int py = center.y() + dy;

                            for (
                                int dxx = -radiusPixels;
                                dxx <= radiusPixels;
                                dxx++
                            ) {
                                int px = center.x() + dxx;
                                int value = this.sampleValueAcrossTiles(
                                    tile,
                                    px,
                                    py
                                );
                                if (
                                    value != WATER_CLASS &&
                                    value != NO_DATA_CLASS
                                ) {
                                    scratch.add(value);
                                }
                            }
                        }
                    }

                    return scratch.pickMajority(centerValue);
                } else {
                    return centerValue;
                }
            }
        }
    }

    private int sampleVisualCoverClassAtLonLat(
        double lon,
        double lat,
        double blockX,
        double blockZ,
        double effectiveResolutionMeters
    ) {
        TellusLandCoverSource.TileKey key = tileKeyForLonLat(lon, lat);
        if (key == null) {
            return NO_DATA_CLASS;
        } else {
            TellusLandCoverSource.GeoTiffTile tile = this.getTile(key);
            TellusLandCoverSource.ContinuousPixel center =
                tile.toContinuousPixel(lon, lat);
            if (center == null) {
                return NO_DATA_CLASS;
            } else {
                int centerPixelX = Mth.floor(center.x());
                int centerPixelY = Mth.floor(center.y());
                int centerValue = tile.sampleValue(centerPixelX, centerPixelY);
                if (
                    effectiveResolutionMeters >= RESOLUTION_METERS ||
                    isHardRawCoverClass(centerValue)
                ) {
                    return centerValue;
                } else {
                    double transitionStrength = Mth.clamp(
                        (RESOLUTION_METERS - effectiveResolutionMeters) / 9.0,
                        0.0,
                        1.0
                    );
                    if (!(transitionStrength > 0.0)) {
                        return centerValue;
                    } else {
                        double blendX = center.x() - 0.5;
                        double blendY = center.y() - 0.5;
                        int x0 = Mth.floor(blendX);
                        int y0 = Mth.floor(blendY);
                        int x1 = x0 + 1;
                        int y1 = y0 + 1;
                        double fx = blendX - x0;
                        double fy = blendY - y0;
                        int value00 = this.sampleValueAcrossTiles(tile, x0, y0);
                        int value10 = this.sampleValueAcrossTiles(tile, x1, y0);
                        int value01 = this.sampleValueAcrossTiles(tile, x0, y1);
                        int value11 = this.sampleValueAcrossTiles(tile, x1, y1);
                        if (
                            isHardRawCoverClass(value00) ||
                            isHardRawCoverClass(value10) ||
                            isHardRawCoverClass(value01) ||
                            isHardRawCoverClass(value11)
                        ) {
                            return centerValue;
                        } else {
                            TellusLandCoverSource.CoverBlendScratch scratch =
                                COVER_BLEND_SCRATCH.get();
                            scratch.reset();
                            double inverseFx = 1.0 - fx;
                            double inverseFy = 1.0 - fy;
                            scratch.add(centerValue, 1.0 - transitionStrength);
                            scratch.add(
                                value00,
                                inverseFx * inverseFy * transitionStrength
                            );
                            scratch.add(
                                value10,
                                fx * inverseFy * transitionStrength
                            );
                            scratch.add(
                                value01,
                                inverseFx * fy * transitionStrength
                            );
                            scratch.add(value11, fx * fy * transitionStrength);
                            return scratch.pickWeighted(
                                centerValue,
                                hashToUnitDouble(
                                    Mth.floor(blockX),
                                    Mth.floor(blockZ),
                                    9154887495218319081L
                                )
                            );
                        }
                    }
                }
            }
        }
    }

    private int sampleVisualCoverClassAtLonLatLocalOnly(
        double lon,
        double lat,
        double blockX,
        double blockZ,
        double effectiveResolutionMeters
    ) {
        TellusLandCoverSource.TileKey key = tileKeyForLonLat(lon, lat);
        if (key == null) {
            return NO_DATA_CLASS;
        } else {
            TellusLandCoverSource.GeoTiffTile tile = this.getTileLocalOnly(key);
            TellusLandCoverSource.ContinuousPixel center =
                tile.toContinuousPixel(lon, lat);
            if (center == null) {
                return NO_DATA_CLASS;
            } else {
                int centerPixelX = Mth.floor(center.x());
                int centerPixelY = Mth.floor(center.y());
                int centerValue = tile.sampleValue(centerPixelX, centerPixelY);
                if (
                    effectiveResolutionMeters >= RESOLUTION_METERS ||
                    isHardRawCoverClass(centerValue)
                ) {
                    return centerValue;
                } else {
                    double transitionStrength = Mth.clamp(
                        (RESOLUTION_METERS - effectiveResolutionMeters) / 9.0,
                        0.0,
                        1.0
                    );
                    if (!(transitionStrength > 0.0)) {
                        return centerValue;
                    } else {
                        double blendX = center.x() - 0.5;
                        double blendY = center.y() - 0.5;
                        int x0 = Mth.floor(blendX);
                        int y0 = Mth.floor(blendY);
                        int x1 = x0 + 1;
                        int y1 = y0 + 1;
                        double fx = blendX - x0;
                        double fy = blendY - y0;
                        int value00 = this.sampleValueAcrossTilesLocalOnly(
                            tile,
                            x0,
                            y0
                        );
                        int value10 = this.sampleValueAcrossTilesLocalOnly(
                            tile,
                            x1,
                            y0
                        );
                        int value01 = this.sampleValueAcrossTilesLocalOnly(
                            tile,
                            x0,
                            y1
                        );
                        int value11 = this.sampleValueAcrossTilesLocalOnly(
                            tile,
                            x1,
                            y1
                        );
                        if (
                            isHardRawCoverClass(value00) ||
                            isHardRawCoverClass(value10) ||
                            isHardRawCoverClass(value01) ||
                            isHardRawCoverClass(value11)
                        ) {
                            return centerValue;
                        } else {
                            TellusLandCoverSource.CoverBlendScratch scratch =
                                COVER_BLEND_SCRATCH.get();
                            scratch.reset();
                            double inverseFx = 1.0 - fx;
                            double inverseFy = 1.0 - fy;
                            scratch.add(centerValue, 1.0 - transitionStrength);
                            scratch.add(
                                value00,
                                inverseFx * inverseFy * transitionStrength
                            );
                            scratch.add(
                                value10,
                                fx * inverseFy * transitionStrength
                            );
                            scratch.add(
                                value01,
                                inverseFx * fy * transitionStrength
                            );
                            scratch.add(value11, fx * fy * transitionStrength);
                            return scratch.pickWeighted(
                                centerValue,
                                hashToUnitDouble(
                                    Mth.floor(blockX),
                                    Mth.floor(blockZ),
                                    9154887495218319081L
                                )
                            );
                        }
                    }
                }
            }
        }
    }

    private int sampleVisualCoverClassAtLonLatMemoryOnly(
        double lon,
        double lat,
        double blockX,
        double blockZ,
        double effectiveResolutionMeters
    ) {
        TellusLandCoverSource.TileKey key = tileKeyForLonLat(lon, lat);
        if (key == null) {
            return Integer.MIN_VALUE;
        } else {
            TellusLandCoverSource.GeoTiffTile tile = this.getTileMemoryOnly(
                key
            );
            if (tile == null) {
                return Integer.MIN_VALUE;
            } else {
                TellusLandCoverSource.ContinuousPixel center =
                    tile.toContinuousPixel(lon, lat);
                if (center == null) {
                    return Integer.MIN_VALUE;
                } else {
                    int centerPixelX = Mth.floor(center.x());
                    int centerPixelY = Mth.floor(center.y());
                    int centerValue = tile.sampleValue(
                        centerPixelX,
                        centerPixelY
                    );
                    if (
                        effectiveResolutionMeters >= RESOLUTION_METERS ||
                        isHardRawCoverClass(centerValue)
                    ) {
                        return centerValue;
                    } else {
                        double transitionStrength = Mth.clamp(
                            (RESOLUTION_METERS - effectiveResolutionMeters) /
                                9.0,
                            0.0,
                            1.0
                        );
                        if (!(transitionStrength > 0.0)) {
                            return centerValue;
                        } else {
                            double blendX = center.x() - 0.5;
                            double blendY = center.y() - 0.5;
                            int x0 = Mth.floor(blendX);
                            int y0 = Mth.floor(blendY);
                            int x1 = x0 + 1;
                            int y1 = y0 + 1;
                            double fx = blendX - x0;
                            double fy = blendY - y0;
                            int value00 = this.sampleValueAcrossTilesMemoryOnly(
                                tile,
                                x0,
                                y0
                            );
                            int value10 = this.sampleValueAcrossTilesMemoryOnly(
                                tile,
                                x1,
                                y0
                            );
                            int value01 = this.sampleValueAcrossTilesMemoryOnly(
                                tile,
                                x0,
                                y1
                            );
                            int value11 = this.sampleValueAcrossTilesMemoryOnly(
                                tile,
                                x1,
                                y1
                            );
                            if (
                                value00 == Integer.MIN_VALUE ||
                                value10 == Integer.MIN_VALUE ||
                                value01 == Integer.MIN_VALUE ||
                                value11 == Integer.MIN_VALUE
                            ) {
                                return Integer.MIN_VALUE;
                            } else if (
                                isHardRawCoverClass(value00) ||
                                isHardRawCoverClass(value10) ||
                                isHardRawCoverClass(value01) ||
                                isHardRawCoverClass(value11)
                            ) {
                                return centerValue;
                            } else {
                                TellusLandCoverSource.CoverBlendScratch scratch =
                                    COVER_BLEND_SCRATCH.get();
                                scratch.reset();
                                double inverseFx = 1.0 - fx;
                                double inverseFy = 1.0 - fy;
                                scratch.add(
                                    centerValue,
                                    1.0 - transitionStrength
                                );
                                scratch.add(
                                    value00,
                                    inverseFx * inverseFy * transitionStrength
                                );
                                scratch.add(
                                    value10,
                                    fx * inverseFy * transitionStrength
                                );
                                scratch.add(
                                    value01,
                                    inverseFx * fy * transitionStrength
                                );
                                scratch.add(
                                    value11,
                                    fx * fy * transitionStrength
                                );
                                return scratch.pickWeighted(
                                    centerValue,
                                    hashToUnitDouble(
                                        Mth.floor(blockX),
                                        Mth.floor(blockZ),
                                        9154887495218319081L
                                    )
                                );
                            }
                        }
                    }
                }
            }
        }
    }

    private int sampleValueAcrossTiles(
        TellusLandCoverSource.GeoTiffTile tile,
        int pixelX,
        int pixelY
    ) {
        if (tile.isInside(pixelX, pixelY)) {
            return tile.sampleValue(pixelX, pixelY);
        } else {
            double neighborLon = tile.lonForPixel(pixelX);
            double neighborLat = tile.latForPixel(pixelY);
            return this.sampleCoverClassAtLonLat(neighborLon, neighborLat);
        }
    }

    private int sampleValueAcrossTilesLocalOnly(
        TellusLandCoverSource.GeoTiffTile tile,
        int pixelX,
        int pixelY
    ) {
        if (tile.isInside(pixelX, pixelY)) {
            return tile.sampleValue(pixelX, pixelY);
        } else {
            double neighborLon = tile.lonForPixel(pixelX);
            double neighborLat = tile.latForPixel(pixelY);
            return this.sampleCoverClassAtLonLatLocalOnly(
                neighborLon,
                neighborLat
            );
        }
    }

    private int sampleValueAcrossTilesMemoryOnly(
        TellusLandCoverSource.GeoTiffTile tile,
        int pixelX,
        int pixelY
    ) {
        if (tile.isInside(pixelX, pixelY)) {
            return tile.sampleValue(pixelX, pixelY);
        } else {
            double neighborLon = tile.lonForPixel(pixelX);
            double neighborLat = tile.latForPixel(pixelY);
            return this.sampleCoverClassAtLonLatMemoryOnly(
                neighborLon,
                neighborLat
            );
        }
    }

    private static boolean isHardRawCoverClass(int coverClass) {
        return (
            coverClass == NO_DATA_CLASS ||
            coverClass == WATER_CLASS ||
            coverClass == MANGROVES_CLASS ||
            coverClass == BUILT_UP_CLASS
        );
    }

    private static double hashToUnitDouble(long x, long z, long salt) {
        long seed = (x * 3129871L) ^ (z * 116129781L) ^ salt;
        seed = seed * seed * 42317861L + seed * 11L;
        return ((seed >>> 11) & ((1L << 53) - 1L)) * 1.1102230246251565E-16;
    }

    public void prefetchTiles(
        double blockX,
        double blockZ,
        double worldScale,
        int radius
    ) {
        this.prefetchTiles(blockX, blockZ, worldScale, radius, worldScale);
    }

    public void prefetchTiles(
        double blockX,
        double blockZ,
        double worldScale,
        int radius,
        double previewResolutionMeters
    ) {
        if (worldScale <= 0.0 || radius <= 0) {
            return;
        }

        double lon = EarthProjection.blockXToLon(blockX, worldScale);
        double lat = EarthProjection.blockZToLat(blockZ, worldScale);
        int zoom = this.satelliteZoomForResolution(
            Math.max(1.0D, Math.max(worldScale, previewResolutionMeters))
        );
        int tileRadius = this.tileCountForRadius(radius);
        int n = 1 << zoom;
        SatelliteTileSampler.TileKey center =
            this.satelliteSampler.tileKeyForLatLon(lat, lon, zoom);
        for (int dz = -tileRadius; dz <= tileRadius; dz++) {
            for (int dx = -tileRadius; dx <= tileRadius; dx++) {
                int tileX = Math.max(0, Math.min(n - 1, center.x() + dx));
                int tileY = Math.max(0, Math.min(n - 1, center.y() + dz));
                this.satelliteSampler.requestTile(
                    new SatelliteTileSampler.TileKey(tileX, tileY, zoom)
                );
            }
        }
    }

    private static int downsampleStep(
        double worldScale,
        double resolutionMeters,
        double previewResolutionMeters
    ) {
        if (!(worldScale > 0.0)) {
            return 1;
        } else if (
            !(effectiveSampleResolutionMeters(
                    worldScale,
                    previewResolutionMeters
                ) >=
                resolutionMeters)
        ) {
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

    private static TellusLandCoverSource.TileKey tileKeyForBlock(
        double blockX,
        double blockZ,
        double worldScale,
        double previewResolutionMeters
    ) {
        if (worldScale <= 0.0) {
            return null;
        } else {
            int step = downsampleStep(
                worldScale,
                RESOLUTION_METERS,
                previewResolutionMeters
            );
            if (step > 1) {
                blockX = downsampleBlock(blockX, step);
                blockZ = downsampleBlock(blockZ, step);
            }

            double blocksPerDegree = EarthProjection.blocksPerDegree(
                worldScale
            );
            double lon = EarthProjection.blockXToLon(blockX, worldScale);
            double lat = EarthProjection.blockZToLat(blockZ, worldScale);
            return tileKeyForLonLat(lon, lat);
        }
    }

    private static double effectiveSampleResolutionMeters(
        double worldScale,
        double previewResolutionMeters
    ) {
        return Double.isFinite(previewResolutionMeters) &&
            previewResolutionMeters > 0.0
            ? Math.max(worldScale, previewResolutionMeters)
            : worldScale;
    }

    private static TellusLandCoverSource.TileKey tileKeyForLonLat(
        double lon,
        double lat
    ) {
        if (
            !(lat < MIN_LAT) &&
            !(lat > MAX_LAT) &&
            !(lon < MIN_LON) &&
            !(lon > MAX_LON)
        ) {
            int tileLat = (int) Math.floor(lat / TILE_DEGREES) * TILE_DEGREES;
            int tileLon = (int) Math.floor(lon / TILE_DEGREES) * TILE_DEGREES;
            return new TellusLandCoverSource.TileKey(tileLat, tileLon);
        } else {
            return null;
        }
    }

    private void prefetchTile(TellusLandCoverSource.TileKey key) {
        if (this.cache.getIfPresent(key) == null) {
            try {
                this.cache.get(key);
            } catch (Exception var3) {
                if (isInterruptedLoad(var3)) {
                    Thread.currentThread().interrupt();
                    return;
                }

                Tellus.LOGGER.debug(
                    "Failed to prefetch land cover tile {}",
                    key,
                    var3
                );
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

    private TellusLandCoverSource.GeoTiffTile getTile(
        TellusLandCoverSource.TileKey key
    ) {
        try {
            return (TellusLandCoverSource.GeoTiffTile) this.cache.get(key);
        } catch (Exception var3) {
            if (isInterruptedLoad(var3)) {
                Thread.currentThread().interrupt();
                return TellusLandCoverSource.GeoTiffTile.MISSING;
            }

            Tellus.LOGGER.warn("Failed to load land cover tile {}", key, var3);
            return TellusLandCoverSource.GeoTiffTile.MISSING;
        }
    }

    private TellusLandCoverSource.GeoTiffTile getTileLocalOnly(
        TellusLandCoverSource.TileKey key
    ) {
        TellusLandCoverSource.GeoTiffTile cached =
            (TellusLandCoverSource.GeoTiffTile) this.cache.getIfPresent(key);
        if (cached != null) {
            return cached;
        } else {
            Path cachePath = this.cacheRoot.resolve(key.fileName());
            if (!Files.exists(cachePath)) {
                return TellusLandCoverSource.GeoTiffTile.MISSING;
            } else {
                try {
                    TellusLandCoverSource.GeoTiffTile opened =
                        TellusLandCoverSource.GeoTiffTile.open(cachePath);
                    TellusLandCoverSource.GeoTiffTile raced =
                        (TellusLandCoverSource.GeoTiffTile) this.cache.asMap().putIfAbsent(
                            key,
                            opened
                        );
                    if (raced != null) {
                        opened.close();
                        return raced;
                    } else {
                        return opened;
                    }
                } catch (IOException error) {
                    Tellus.LOGGER.debug(
                        "Failed to load cached land cover tile {}",
                        key,
                        error
                    );
                    return TellusLandCoverSource.GeoTiffTile.MISSING;
                }
            }
        }
    }

    private TellusLandCoverSource.GeoTiffTile getTileMemoryOnly(
        TellusLandCoverSource.TileKey key
    ) {
        TellusLandCoverSource.GeoTiffTile cached =
            (TellusLandCoverSource.GeoTiffTile) this.cache.getIfPresent(key);
        return cached == null ||
            cached == TellusLandCoverSource.GeoTiffTile.MISSING
            ? null
            : cached;
    }

    private static boolean isInterruptedLoad(Throwable throwable) {
        if (Thread.currentThread().isInterrupted()) {
            return true;
        } else {
            for (
                Throwable current = throwable;
                current != null;
                current = current.getCause()
            ) {
                if (
                    current instanceof ClosedByInterruptException ||
                    current instanceof InterruptedException ||
                    current instanceof InterruptedIOException
                ) {
                    return true;
                }
            }

            return false;
        }
    }

    private TellusLandCoverSource.GeoTiffTile loadTile(
        TellusLandCoverSource.TileKey key
    ) throws IOException {
        Path cachePath = this.cacheRoot.resolve(key.fileName());
        if (Files.exists(cachePath)) {
            return TellusLandCoverSource.GeoTiffTile.open(cachePath);
        } else {
            byte[] data = this.downloadTile(key);
            if (data == null) {
                return TellusLandCoverSource.GeoTiffTile.MISSING;
            } else {
                this.cacheTile(cachePath, data);
                return TellusLandCoverSource.GeoTiffTile.open(cachePath);
            }
        }
    }

    private byte[] downloadTile(TellusLandCoverSource.TileKey key)
        throws IOException {
        URI uri = URI.create(String.format("%s/%s", ENDPOINT, key.fileName()));
        HttpURLConnection connection = (HttpURLConnection) uri
            .toURL()
            .openConnection();
        try {
            connection.setConnectTimeout(8000);
            connection.setReadTimeout(8000);
            connection.setRequestProperty(
                "User-Agent",
                "Tellus/1.0 (Minecraft Mod)"
            );
            if (connection.getResponseCode() == 404) {
                return null;
            } else {
                DownloadProgressReporter.requestStarted(
                    connection.getContentLengthLong()
                );

                byte[] var5;
                try (InputStream input = connection.getInputStream()) {
                    var5 = DownloadProgressReporter.readAllBytesWithProgress(
                        input
                    );
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
            Path tempPath = cachePath.resolveSibling(
                cachePath.getFileName() + ".tmp"
            );
            Files.write(tempPath, data);
            Files.move(
                tempPath,
                cachePath,
                StandardCopyOption.REPLACE_EXISTING
            );
        } catch (IOException var4) {
            Tellus.LOGGER.warn(
                "Failed to cache land cover tile {}",
                cachePath,
                var4
            );
        }
    }

    private static final class CoverSmoothScratch {

        private final int[] counts = new int[256];
        private final int[] used = new int[256];
        private int usedCount;

        private void reset() {
            for (int i = 0; i < this.usedCount; i++) {
                this.counts[this.used[i]] = 0;
            }

            this.usedCount = 0;
        }

        private void add(int value) {
            if (this.counts[value] == 0) {
                this.used[this.usedCount++] = value;
            }

            this.counts[value]++;
        }

        private int pickMajority(int centerValue) {
            if (this.usedCount == 0) {
                return centerValue;
            } else {
                int bestValue = centerValue;
                int bestCount = -1;

                for (int i = 0; i < this.usedCount; i++) {
                    int value = this.used[i];
                    int count = this.counts[value];
                    if (
                        count > bestCount ||
                        (count == bestCount && value == centerValue)
                    ) {
                        bestCount = count;
                        bestValue = value;
                    }
                }

                return bestValue;
            }
        }
    }

    private static final class CoverBlendScratch {

        private final double[] weights = new double[256];
        private final int[] used = new int[256];
        private int usedCount;

        private void reset() {
            for (int i = 0; i < this.usedCount; i++) {
                this.weights[this.used[i]] = 0.0;
            }

            this.usedCount = 0;
        }

        private void add(int value, double weight) {
            if (weight > 0.0) {
                if (!(this.weights[value] > 0.0)) {
                    this.used[this.usedCount++] = value;
                }

                this.weights[value] += weight;
            }
        }

        private int pickWeighted(int fallbackValue, double threshold) {
            if (this.usedCount == 0) {
                return fallbackValue;
            } else {
                double total = 0.0;

                for (int i = 0; i < this.usedCount; i++) {
                    total += this.weights[this.used[i]];
                }

                if (!(total > 0.0)) {
                    return fallbackValue;
                } else {
                    double target =
                        Mth.clamp(threshold, 0.0, 0.9999999999999999) * total;
                    double cumulative = 0.0;
                    int lastValue = fallbackValue;

                    for (int i = 0; i < this.usedCount; i++) {
                        int value = this.used[i];
                        lastValue = value;
                        cumulative += this.weights[value];
                        if (target < cumulative || i + 1 == this.usedCount) {
                            return value;
                        }
                    }

                    return lastValue;
                }
            }
        }
    }

    @Override
    public TellusCacheDomain cacheDomain() {
        return TellusCacheDomain.LAND_COVER;
    }

    @Override
    public void clearCache() {
        this.cache.invalidateAll();
        this.cache.cleanUp();
        this.satelliteSampler.clearCache();
    }

    private static final class GeoTiffTile {

        private static final int TAG_IMAGE_WIDTH = 256;
        private static final int TAG_IMAGE_HEIGHT = 257;
        private static final int TAG_TILE_WIDTH = 322;
        private static final int TAG_TILE_HEIGHT = 323;
        private static final int TAG_TILE_OFFSETS = 324;
        private static final int TAG_TILE_BYTE_COUNTS = 325;
        private static final int TAG_COMPRESSION = 259;
        private static final int TAG_MODEL_PIXEL_SCALE = 33550;
        private static final int TAG_MODEL_TIEPOINT = 33922;
        private static final int TYPE_SHORT = 3;
        private static final int TYPE_LONG = 4;
        private static final int COMPRESSION_DEFLATE = 8;
        private static final TellusLandCoverSource.GeoTiffTile MISSING =
            new TellusLandCoverSource.GeoTiffTile();
        private final Path path;
        private final FileChannel channel;
        private final int width;
        private final int height;
        private final int tileWidth;
        private final int tileHeight;
        private final int tilesPerRow;
        private final long[] tileOffsets;
        private final int[] tileByteCounts;
        private final double pixelScaleX;
        private final double pixelScaleY;
        private final double tieLon;
        private final double tieLat;
        private final Map<Integer, byte[]> tileCache;

        private GeoTiffTile() {
            this.path = null;
            this.channel = null;
            this.width = 0;
            this.height = 0;
            this.tileWidth = 0;
            this.tileHeight = 0;
            this.tilesPerRow = 0;
            this.tileOffsets = null;
            this.tileByteCounts = null;
            this.pixelScaleX = 0.0;
            this.pixelScaleY = 0.0;
            this.tieLon = 0.0;
            this.tieLat = 0.0;
            this.tileCache = Map.of();
        }

        private GeoTiffTile(
            Path path,
            FileChannel channel,
            int width,
            int height,
            int tileWidth,
            int tileHeight,
            long[] tileOffsets,
            int[] tileByteCounts,
            double pixelScaleX,
            double pixelScaleY,
            double tieLon,
            double tieLat
        ) {
            this.path = path;
            this.channel = channel;
            this.width = width;
            this.height = height;
            this.tileWidth = tileWidth;
            this.tileHeight = tileHeight;
            this.tilesPerRow = (int) Math.ceil((double) width / tileWidth);
            this.tileOffsets = tileOffsets;
            this.tileByteCounts = tileByteCounts;
            this.pixelScaleX = pixelScaleX;
            this.pixelScaleY = pixelScaleY;
            this.tieLon = tieLon;
            this.tieLat = tieLat;
            this.tileCache = new LinkedHashMap<Integer, byte[]>(
                TellusLandCoverSource.TILE_CACHE_ENTRIES,
                0.75F,
                true
            ) {
                @Override
                protected boolean removeEldestEntry(
                    Entry<Integer, byte[]> eldest
                ) {
                    return (
                        this.size() > TellusLandCoverSource.TILE_CACHE_ENTRIES
                    );
                }
            };
        }

        static TellusLandCoverSource.GeoTiffTile open(Path path)
            throws IOException {
            FileChannel channel = FileChannel.open(
                path,
                StandardOpenOption.READ
            );

            try {
                return readFromChannel(path, channel);
            } catch (IOException var3) {
                channel.close();
                throw var3;
            }
        }

        int sample(double lon, double lat) {
            TellusLandCoverSource.Pixel pixel = this.toPixel(lon, lat);
            return pixel == null ? 0 : this.sampleValue(pixel.x, pixel.y);
        }

        TellusLandCoverSource.ContinuousPixel toContinuousPixel(
            double lon,
            double lat
        ) {
            if (this == MISSING) {
                return null;
            } else {
                double pixelX = (lon - this.tieLon) / this.pixelScaleX;
                double pixelY = (this.tieLat - lat) / this.pixelScaleY;
                return pixelX >= 0.0 &&
                    pixelY >= 0.0 &&
                    pixelX < this.width &&
                    pixelY < this.height
                    ? new TellusLandCoverSource.ContinuousPixel(pixelX, pixelY)
                    : null;
            }
        }

        TellusLandCoverSource.Pixel toPixel(double lon, double lat) {
            if (this == MISSING) {
                return null;
            } else {
                int pixelX = (int) Math.floor(
                    (lon - this.tieLon) / this.pixelScaleX
                );
                int pixelY = (int) Math.floor(
                    (this.tieLat - lat) / this.pixelScaleY
                );
                return pixelX >= 0 &&
                    pixelY >= 0 &&
                    pixelX < this.width &&
                    pixelY < this.height
                    ? new TellusLandCoverSource.Pixel(pixelX, pixelY)
                    : null;
            }
        }

        int sampleValue(int pixelX, int pixelY) {
            if (
                this != MISSING &&
                pixelX >= 0 &&
                pixelY >= 0 &&
                pixelX < this.width &&
                pixelY < this.height
            ) {
                int tileX = pixelX / this.tileWidth;
                int tileY = pixelY / this.tileHeight;
                int tileIndex = tileY * this.tilesPerRow + tileX;

                byte[] tile;
                try {
                    tile = this.getTile(tileIndex);
                } catch (ClosedByInterruptException var9) {
                    Thread.currentThread().interrupt();
                    return 0;
                } catch (IOException var10) {
                    Tellus.LOGGER.warn(
                        "Failed to read land cover tile {} in {}",
                        new Object[] { tileIndex, this.path, var10 }
                    );
                    return 0;
                }

                int localX = pixelX - tileX * this.tileWidth;
                int localY = pixelY - tileY * this.tileHeight;
                return Byte.toUnsignedInt(
                    tile[localX + localY * this.tileWidth]
                );
            } else {
                return 0;
            }
        }

        boolean isInside(int pixelX, int pixelY) {
            return (
                pixelX >= 0 &&
                pixelY >= 0 &&
                pixelX < this.width &&
                pixelY < this.height
            );
        }

        boolean isNeighborhoodInBounds(int pixelX, int pixelY, int radius) {
            return (
                pixelX - radius >= 0 &&
                pixelY - radius >= 0 &&
                pixelX + radius < this.width &&
                pixelY + radius < this.height
            );
        }

        double lonForPixel(int pixelX) {
            return this.tieLon + (pixelX + 0.5) * this.pixelScaleX;
        }

        double latForPixel(int pixelY) {
            return this.tieLat - (pixelY + 0.5) * this.pixelScaleY;
        }

        void close() {
            if (this.channel != null) {
                synchronized (this.tileCache) {
                    this.tileCache.clear();
                }

                try {
                    this.channel.close();
                } catch (IOException var2) {
                    Tellus.LOGGER.warn(
                        "Failed to close land cover tile {}",
                        this.path,
                        var2
                    );
                }
            }
        }

        private byte[] getTile(int tileIndex) throws IOException {
            synchronized (this.tileCache) {
                byte[] cached = this.tileCache.get(tileIndex);
                if (cached != null) {
                    return cached;
                }
            }

            byte[] tile = this.readTile(tileIndex);
            synchronized (this.tileCache) {
                this.tileCache.put(tileIndex, tile);
                return tile;
            }
        }

        private byte[] readTile(int tileIndex) throws IOException {
            long offset = this.tileOffsets[tileIndex];
            int length = this.tileByteCounts[tileIndex];
            byte[] compressed = new byte[length];

            try {
                readFully(this.channel, compressed, offset);
            } catch (ClosedChannelException var12) {
                if (this.path == null) {
                    throw var12;
                }

                try (
                    FileChannel reopened = FileChannel.open(
                        this.path,
                        StandardOpenOption.READ
                    )
                ) {
                    readFully(reopened, compressed, offset);
                }
            }

            return inflate(compressed, this.tileWidth * this.tileHeight);
        }

        private static TellusLandCoverSource.GeoTiffTile readFromChannel(
            Path path,
            FileChannel channel
        ) throws IOException {
            ByteBuffer header = ByteBuffer.allocate(8);
            readFully(channel, header, 0L);
            header.flip();
            short order = header.getShort();

            ByteOrder byteOrder = switch (order) {
                case 18761 -> ByteOrder.LITTLE_ENDIAN;
                case 19789 -> ByteOrder.BIG_ENDIAN;
                default -> throw new IOException("Invalid TIFF byte order");
            };
            header.order(byteOrder);
            short magic = header.getShort();
            if (magic != 42) {
                throw new IOException("Invalid TIFF magic");
            } else {
                int ifdOffset = header.getInt();
                ByteBuffer countBuffer = ByteBuffer.allocate(2).order(
                    byteOrder
                );
                readFully(channel, countBuffer, ifdOffset);
                countBuffer.flip();
                int entryCount = Short.toUnsignedInt(countBuffer.getShort());
                ByteBuffer entries = ByteBuffer.allocate(entryCount * 12).order(
                    byteOrder
                );
                readFully(channel, entries, ifdOffset + 2L);
                entries.flip();
                int width = -1;
                int height = -1;
                int tileWidth = -1;
                int tileHeight = -1;
                int compression = -1;
                long[] tileOffsets = null;
                int[] tileByteCounts = null;
                double[] pixelScale = null;
                double[] tiepoint = null;

                for (int i = 0; i < entryCount; i++) {
                    int tag = Short.toUnsignedInt(entries.getShort());
                    int type = Short.toUnsignedInt(entries.getShort());
                    int count = entries.getInt();
                    int value = entries.getInt();
                    switch (tag) {
                        case TAG_IMAGE_WIDTH:
                            width = readIntValue(type, count, value, byteOrder);
                            break;
                        case TAG_IMAGE_HEIGHT:
                            height = readIntValue(
                                type,
                                count,
                                value,
                                byteOrder
                            );
                            break;
                        case TAG_COMPRESSION:
                            compression = readIntValue(
                                type,
                                count,
                                value,
                                byteOrder
                            );
                            break;
                        case TAG_TILE_WIDTH:
                            tileWidth = readIntValue(
                                type,
                                count,
                                value,
                                byteOrder
                            );
                            break;
                        case TAG_TILE_HEIGHT:
                            tileHeight = readIntValue(
                                type,
                                count,
                                value,
                                byteOrder
                            );
                            break;
                        case TAG_TILE_OFFSETS:
                            tileOffsets = readLongArray(
                                channel,
                                value,
                                count,
                                byteOrder
                            );
                            break;
                        case TAG_TILE_BYTE_COUNTS:
                            tileByteCounts = readIntArray(
                                channel,
                                value,
                                count,
                                byteOrder
                            );
                            break;
                        case TAG_MODEL_PIXEL_SCALE:
                            pixelScale = readDoubleArray(
                                channel,
                                value,
                                count,
                                byteOrder
                            );
                            break;
                        case TAG_MODEL_TIEPOINT:
                            tiepoint = readDoubleArray(
                                channel,
                                value,
                                count,
                                byteOrder
                            );
                    }
                }

                if (compression != COMPRESSION_DEFLATE) {
                    throw new IOException(
                        "Unsupported TIFF compression " + compression
                    );
                } else if (
                    width <= 0 ||
                    height <= 0 ||
                    tileWidth <= 0 ||
                    tileHeight <= 0
                ) {
                    throw new IOException("Missing TIFF size tags");
                } else if (tileOffsets == null || tileByteCounts == null) {
                    throw new IOException("Missing TIFF tile offsets");
                } else if (
                    pixelScale != null &&
                    pixelScale.length >= 2 &&
                    tiepoint != null &&
                    tiepoint.length >= 5
                ) {
                    return new TellusLandCoverSource.GeoTiffTile(
                        path,
                        channel,
                        width,
                        height,
                        tileWidth,
                        tileHeight,
                        tileOffsets,
                        tileByteCounts,
                        pixelScale[0],
                        pixelScale[1],
                        tiepoint[3],
                        tiepoint[4]
                    );
                } else {
                    throw new IOException("Missing TIFF georeference tags");
                }
            }
        }

        private static int readIntValue(
            int type,
            int count,
            int value,
            ByteOrder order
        ) throws IOException {
            if (count != 1) {
                throw new IOException("Expected single TIFF value");
            } else {
                ByteBuffer buffer = ByteBuffer.allocate(4).order(order);
                buffer.putInt(value);
                buffer.flip();
                if (type == TYPE_SHORT) {
                    return Short.toUnsignedInt(buffer.getShort());
                } else if (type == TYPE_LONG) {
                    return buffer.getInt();
                } else {
                    throw new IOException(
                        "Unsupported TIFF value type " + type
                    );
                }
            }
        }

        private static long[] readLongArray(
            FileChannel channel,
            long offset,
            int count,
            ByteOrder order
        ) throws IOException {
            if (count <= 0) {
                return new long[0];
            } else {
                ByteBuffer buffer = ByteBuffer.allocate(count * 4).order(order);
                readFully(channel, buffer, offset);
                buffer.flip();
                long[] values = new long[count];

                for (int i = 0; i < count; i++) {
                    values[i] = Integer.toUnsignedLong(buffer.getInt());
                }

                return values;
            }
        }

        private static int[] readIntArray(
            FileChannel channel,
            long offset,
            int count,
            ByteOrder order
        ) throws IOException {
            if (count <= 0) {
                return new int[0];
            } else {
                ByteBuffer buffer = ByteBuffer.allocate(count * 4).order(order);
                readFully(channel, buffer, offset);
                buffer.flip();
                int[] values = new int[count];

                for (int i = 0; i < count; i++) {
                    values[i] = buffer.getInt();
                }

                return values;
            }
        }

        private static double[] readDoubleArray(
            FileChannel channel,
            long offset,
            int count,
            ByteOrder order
        ) throws IOException {
            if (count <= 0) {
                return new double[0];
            } else {
                ByteBuffer buffer = ByteBuffer.allocate(count * 8).order(order);
                readFully(channel, buffer, offset);
                buffer.flip();
                double[] values = new double[count];

                for (int i = 0; i < count; i++) {
                    values[i] = buffer.getDouble();
                }

                return values;
            }
        }

        private static void readFully(
            FileChannel channel,
            ByteBuffer buffer,
            long offset
        ) throws IOException {
            long position = offset;

            while (buffer.hasRemaining()) {
                int read = channel.read(buffer, position);
                if (read < 0) {
                    throw new EOFException("Unexpected end of file");
                }

                position += read;
            }
        }

        private static void readFully(
            FileChannel channel,
            byte[] dest,
            long offset
        ) throws IOException {
            readFully(channel, ByteBuffer.wrap(dest), offset);
        }

        private static byte[] inflate(byte[] compressed, int expectedSize)
            throws IOException {
            byte[] output = new byte[expectedSize];

            byte[] var8;
            try (
                InflaterInputStream inflater = new InflaterInputStream(
                    new ByteArrayInputStream(compressed)
                )
            ) {
                int offset = 0;

                while (offset < expectedSize) {
                    int read = inflater.read(
                        output,
                        offset,
                        expectedSize - offset
                    );
                    if (read < 0) {
                        break;
                    }

                    offset += read;
                }

                if (offset != expectedSize) {
                    throw new IOException("Unexpected inflated data length");
                }

                var8 = output;
            }

            return var8;
        }
    }

    private record Pixel(int x, int y) {}

    private record ContinuousPixel(double x, double y) {}

    private record TileKey(int lat, int lon) {
        String fileName() {
            return String.format(
                Locale.ROOT,
                TILE_PATTERN,
                formatLatLon(this.lat, this.lon)
            );
        }

        private static String formatLatLon(int lat, int lon) {
            char latPrefix = (char) (lat >= 0 ? 78 : 83);
            char lonPrefix = (char) (lon >= 0 ? 69 : 87);
            int latAbs = Math.abs(lat);
            int lonAbs = Math.abs(lon);
            return String.format(
                Locale.ROOT,
                "%c%02d%c%03d",
                latPrefix,
                latAbs,
                lonPrefix,
                lonAbs
            );
        }
    }
}
