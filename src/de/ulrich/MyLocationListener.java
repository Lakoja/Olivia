package de.ulrich;

import org.mapsforge.core.GeoPoint;

public interface MyLocationListener {
	public void onLocationChanged(GeoPoint location, float accuracy);
	public void onSatelliteInfo(int activeSatellites, int totalSatellites, float accuracy);
	public void onOrientation(float orientation);
}
