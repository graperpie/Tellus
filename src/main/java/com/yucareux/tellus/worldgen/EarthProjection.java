package com.yucareux.tellus.worldgen;

public final class EarthProjection {
   public static final double METERS_PER_DEGREE = 111319.49166666667;
   public static final double MAX_MERCATOR_LATITUDE = 85.05112878;
   private static final double EARTH_RADIUS_METERS = METERS_PER_DEGREE * 180.0 / Math.PI;
   private static final EarthProjection.ProjectionMode PROJECTION_MODE = resolveMode(System.getProperty("tellus.projection.mode", "legacy"));
   private static volatile double originOffsetBlockX;
   private static volatile double originOffsetBlockZ;

   private EarthProjection() {
   }

   public static double blocksPerDegree(double worldScale) {
      return worldScale <= 0.0 ? 0.0 : METERS_PER_DEGREE / worldScale;
   }

   public static double worldScaleFromBlocksPerDegree(double blocksPerDegree) {
      return blocksPerDegree <= 0.0 ? 0.0 : METERS_PER_DEGREE / blocksPerDegree;
   }

   public static double latToBlockZ(double latitude, double worldScale) {
      return rawLatToBlockZ(latitude, worldScale) - originOffsetBlockZ;
   }

   public static double blockZToLat(double blockZ, double worldScale) {
      return rawBlockZToLat(blockZ + originOffsetBlockZ, worldScale);
   }

   public static double lonToBlockX(double longitude, double worldScale) {
      return rawLonToBlockX(longitude, worldScale) - originOffsetBlockX;
   }

   public static double blockXToLon(double blockX, double worldScale) {
      return rawBlockXToLon(blockX + originOffsetBlockX, worldScale);
   }

   public static void configureSpawnOrigin(double spawnLatitude, double spawnLongitude, double worldScale) {
      originOffsetBlockX = rawLonToBlockX(spawnLongitude, worldScale);
      originOffsetBlockZ = rawLatToBlockZ(spawnLatitude, worldScale);
   }

   public static void clearSpawnOrigin() {
      originOffsetBlockX = 0.0;
      originOffsetBlockZ = 0.0;
   }

   private static double rawLatToBlockZ(double latitude, double worldScale) {
      if (worldScale <= 0.0) {
         return 0.0;
      } else if (PROJECTION_MODE == EarthProjection.ProjectionMode.LEGACY) {
         return -latitude * blocksPerDegree(worldScale);
      } else {
         double clampedLatitude = clampLatitude(latitude);
         double latitudeRad = Math.toRadians(clampedLatitude);
         double mercatorY = EARTH_RADIUS_METERS * Math.log(Math.tan(Math.PI * 0.25 + latitudeRad * 0.5));
         return -mercatorY / worldScale;
      }
   }

   private static double rawBlockZToLat(double blockZ, double worldScale) {
      if (worldScale <= 0.0) {
         return 0.0;
      } else if (PROJECTION_MODE == EarthProjection.ProjectionMode.LEGACY) {
         return -blockZ / blocksPerDegree(worldScale);
      } else {
         double mercatorY = -blockZ * worldScale;
         double latitudeRad = Math.atan(Math.sinh(mercatorY / EARTH_RADIUS_METERS));
         return clampLatitude(Math.toDegrees(latitudeRad));
      }
   }

   private static double rawLonToBlockX(double longitude, double worldScale) {
      return worldScale <= 0.0 ? 0.0 : longitude * blocksPerDegree(worldScale);
   }

   private static double rawBlockXToLon(double blockX, double worldScale) {
      double blocksPerDegree = blocksPerDegree(worldScale);
      return blocksPerDegree <= 0.0 ? 0.0 : blockX / blocksPerDegree;
   }

   public static double clampLatitude(double latitude) {
      return Math.max(-MAX_MERCATOR_LATITUDE, Math.min(MAX_MERCATOR_LATITUDE, latitude));
   }

   public static String projectionModeId() {
      return PROJECTION_MODE.id();
   }

   private static EarthProjection.ProjectionMode resolveMode(String value) {
      if (value == null) {
         return EarthProjection.ProjectionMode.MERCATOR;
      } else {
         return "legacy".equalsIgnoreCase(value.trim()) ? EarthProjection.ProjectionMode.LEGACY : EarthProjection.ProjectionMode.MERCATOR;
      }
   }

   private static enum ProjectionMode {
      LEGACY("legacy"),
      MERCATOR("mercator");

      private final String id;

      private ProjectionMode(String id) {
         this.id = id;
      }

      private String id() {
         return this.id;
      }
   }
}

