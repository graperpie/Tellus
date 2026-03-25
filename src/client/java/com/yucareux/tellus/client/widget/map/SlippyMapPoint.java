package com.yucareux.tellus.client.widget.map;

import net.minecraft.util.Mth;

public class SlippyMapPoint {
	private static final double MAX_WEB_MERCATOR_LAT = 85.05112878D;

	private final double latitude;
	private final double longitude;

	public SlippyMapPoint(double latitude, double longitude) {
		this.latitude = Mth.clamp(latitude, -MAX_WEB_MERCATOR_LAT, MAX_WEB_MERCATOR_LAT);
		this.longitude = wrapLongitude(longitude);
	}

	public SlippyMapPoint(int x, int y, int zoom) {
		double maximumX = SlippyMap.TILE_SIZE * (1 << zoom);
		this.longitude = x / maximumX * 360.0 - 180.0;

		double maximumY = SlippyMap.TILE_SIZE * (1 << zoom);
		this.latitude = Math.toDegrees(Math.atan(Math.sinh(Math.PI - (2.0 * Math.PI * y) / maximumY)));
	}

	public double getLatitude() {
		return this.latitude;
	}

	public double getLongitude() {
		return this.longitude;
	}

	public int getX(int zoom) {
		double maximumX = SlippyMap.TILE_SIZE * (1 << zoom);
		return Mth.floor((this.longitude + 180.0) / 360.0 * maximumX);
	}

	public int getY(int zoom) {
		double maximumY = SlippyMap.TILE_SIZE * (1 << zoom);
		double angle = Math.toRadians(this.latitude);
		return Mth.floor((1.0 - Math.log(Math.tan(angle) + 1.0 / Math.cos(angle)) / Math.PI) / 2.0 * maximumY);
	}

	public SlippyMapPoint translate(int x, int y, int zoom) {
		int currentX = this.getX(zoom);
		int currentY = this.getY(zoom);
		return new SlippyMapPoint(currentX + x, currentY + y, zoom);
	}

	private static double wrapLongitude(double longitude) {
		double wrapped = longitude;
		while (wrapped < -180.0D) {
			wrapped += 360.0D;
		}
		while (wrapped >= 180.0D) {
			wrapped -= 360.0D;
		}
		return wrapped;
	}
}
