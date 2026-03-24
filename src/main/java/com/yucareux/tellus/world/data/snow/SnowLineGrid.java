package com.yucareux.tellus.world.data.snow;

import com.yucareux.tellus.Tellus;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import net.minecraft.util.Mth;

/**
 * Grid-based snow line elevation data.
 * Stores snow line elevations at regular lat/lon intervals and provides
 * interpolation.
 */
public final class SnowLineGrid {
    private static final String RESOURCE_PATH = "tellus/snow/snow_lines.txt";

    private final double gridSpacing; // degrees between grid points
    private final double minLat;
    private final double maxLat;
    private final double minLon;
    private final double maxLon;
    private final int latSteps;
    private final int lonSteps;
    private final int[][] elevations; // [latIndex][lonIndex]

    public SnowLineGrid() {
        GridData data = loadGridData();
        this.gridSpacing = data.gridSpacing;
        this.minLat = data.minLat;
        this.maxLat = data.maxLat;
        this.minLon = data.minLon;
        this.maxLon = data.maxLon;
        this.latSteps = data.latSteps;
        this.lonSteps = data.lonSteps;
        this.elevations = data.elevations;
    }

    /**
     * Get snow line elevation at a specific location using bilinear interpolation.
     * 
     * @param lat latitude in degrees
     * @param lon longitude in degrees
     * @return snow line elevation in meters/blocks, or Integer.MAX_VALUE if no snow
     */
    public int getSnowLineElevation(double lat, double lon) {
        // Clamp to grid bounds
        lat = Mth.clamp(lat, minLat, maxLat);
        lon = Mth.clamp(lon, minLon, maxLon);

        // Find grid cell
        double latIndex = (lat - minLat) / gridSpacing;
        double lonIndex = (lon - minLon) / gridSpacing;

        int lat0 = (int) Math.floor(latIndex);
        int lon0 = (int) Math.floor(lonIndex);
        int lat1 = Math.min(lat0 + 1, latSteps - 1);
        int lon1 = Math.min(lon0 + 1, lonSteps - 1);

        // Get corner elevations
        int e00 = elevations[lat0][lon0];
        int e10 = elevations[lat1][lon0];
        int e01 = elevations[lat0][lon1];
        int e11 = elevations[lat1][lon1];

        // Bilinear interpolation
        double latFrac = latIndex - lat0;
        double lonFrac = lonIndex - lon0;

        double e0 = Mth.lerp(latFrac, e00, e10);
        double e1 = Mth.lerp(latFrac, e01, e11);
        double elevation = Mth.lerp(lonFrac, e0, e1);

        return (int) Math.round(elevation);
    }

    private static GridData loadGridData() {
        try (InputStream input = SnowLineGrid.class.getClassLoader().getResourceAsStream(RESOURCE_PATH)) {
            if (input == null) {
                Tellus.LOGGER.warn("Snow line data not found at {}, using fallback", RESOURCE_PATH);
                return createFallbackGrid();
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(input));

            // Skip comments and read header
            String headerLine = skipCommentsAndRead(reader);
            if (headerLine == null) {
                throw new IOException("No header found in snow line data");
            }
            String[] header = headerLine.split(",");
            double gridSpacing = Double.parseDouble(header[0]);
            double minLat = Double.parseDouble(header[1]);
            double maxLat = Double.parseDouble(header[2]);
            double minLon = Double.parseDouble(header[3]);
            double maxLon = Double.parseDouble(header[4]);

            int latSteps = (int) Math.round((maxLat - minLat) / gridSpacing) + 1;
            int lonSteps = (int) Math.round((maxLon - minLon) / gridSpacing) + 1;

            int[][] elevations = new int[latSteps][lonSteps];

            // Read data rows (skip comments)
            int rowIndex = 0;
            String line;
            while ((line = skipCommentsAndRead(reader)) != null && rowIndex < latSteps) {
                String[] values = line.split(",");
                for (int col = 0; col < lonSteps && col < values.length; col++) {
                    elevations[rowIndex][col] = Integer.parseInt(values[col].trim());
                }
                rowIndex++;
            }

            return new GridData(gridSpacing, minLat, maxLat, minLon, maxLon, latSteps, lonSteps, elevations);

        } catch (IOException | NumberFormatException e) {
            Tellus.LOGGER.warn("Failed to load snow line data, using fallback", e);
            return createFallbackGrid();
        }
    }

    private static String skipCommentsAndRead(BufferedReader reader) throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                return line;
            }
        }
        return null;
    }

    private static GridData createFallbackGrid() {
        // Simple fallback: 10-degree grid with latitude-based snow lines
        double gridSpacing = 10.0;
        double minLat = -90.0;
        double maxLat = 90.0;
        double minLon = -180.0;
        double maxLon = 180.0;

        int latSteps = 19; // -90 to 90 in 10-degree steps
        int lonSteps = 37; // -180 to 180 in 10-degree steps

        int[][] elevations = new int[latSteps][lonSteps];

        for (int latIdx = 0; latIdx < latSteps; latIdx++) {
            double lat = minLat + latIdx * gridSpacing;
            double absLat = Math.abs(lat);

            // Simple elevation model: high at equator, low at poles
            int snowLine = (int) (5000.0 - (4500.0 * absLat / 90.0));

            for (int lonIdx = 0; lonIdx < lonSteps; lonIdx++) {
                elevations[latIdx][lonIdx] = snowLine;
            }
        }

        return new GridData(gridSpacing, minLat, maxLat, minLon, maxLon, latSteps, lonSteps, elevations);
    }

    private record GridData(
            double gridSpacing,
            double minLat,
            double maxLat,
            double minLon,
            double maxLon,
            int latSteps,
            int lonSteps,
            int[][] elevations) {
    }
}
