package de.ulrich;

import org.mapsforge.core.GeoPoint;

public interface MyLocationListener {
	public void onLocationChanged(GeoPoint location, float accuracy);
	public void onSatelliteText(String status);
}
