package de.ulrich;

import java.util.Iterator;

import org.mapsforge.core.GeoPoint;

import android.app.Activity;
import android.content.Context;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class GpsHandler implements LocationListener, GpsStatus.Listener {
	private SensorManager sensorManager;
	private LocationManager locationManager;
	private boolean firstFix;
	private long lastActiveFix;
	private long lastLocationChange;
	private GeoPoint lastLocation;
	private float lastAccuracy;
	
	private boolean paused;
	
	private MyLocationListener listener;
	
	public GpsHandler(Activity father) {
        
        sensorManager = (SensorManager)father.getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager)father.getSystemService(Context.LOCATION_SERVICE);
        listener = (MyLocationListener)father;
        
        //Location loc = locationManager.getLastKnownLocation(Context.LOCATION_SERVICE);
        //if (loc != null)
        //	onLocationChanged(loc);
	}
	
	
	public void restore(Bundle savedState) {
		if (savedState != null) {
			lastLocationChange = savedState.getLong("lastLoaction");
			lastLocation = (GeoPoint)savedState.getSerializable("lastLocation");
		}		
	}
	
	public void save(Bundle outState) {
		if (lastLocationChange != 0)
			outState.putLong("lastLocationChange", lastLocationChange);
		if (lastLocation != null)
			outState.putSerializable("lastLocation", lastLocation);
	}
	
	public void pause() {
		paused = true;
        locationManager.removeUpdates(this);
        locationManager.removeGpsStatusListener(this);
	}
	
	public void resume() {
		paused = false;
        locationManager.addGpsStatusListener(this);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0.5f, this);
	}
	

	@Override
	public void onLocationChanged(Location location) {
		
		lastLocation = new GeoPoint(location.getLatitude(), location.getLongitude());
		lastAccuracy = location.getAccuracy();
		
		listener.onLocationChanged(lastLocation, lastAccuracy);
		
		long now = System.currentTimeMillis();
		
		/*
		if (now - lastLocationChange < 3000) {
			
			lastActiveFix = location.getTime();
			
			if (!firstFix)
				bleeper.play();
			
			firstFix = true;
			//gpsStateView.setImageResource(R.drawable.gpsActive);			
		}*/	
		
		lastLocationChange = now;
	}

	@Override
	public void onProviderDisabled(String provider) {	
	}

	@Override
	public void onProviderEnabled(String provider) {		
	}

	@Override
	public void onStatusChanged(String provider, int status, Bundle extras) {	
	}
	
	@Override
	public void onGpsStatusChanged(int event) {
		
		if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS || event == GpsStatus.GPS_EVENT_FIRST_FIX) {
			int countFix = 0;
			int countMax = 0;
			
			GpsStatus status = locationManager.getGpsStatus(null);		
			Iterator<GpsSatellite> satellites = status.getSatellites().iterator();
			while(satellites.hasNext()) {
				GpsSatellite sat = satellites.next();
				if (sat.usedInFix())
					countFix++;
				countMax++;
			}
			
			String accuracy = "";
			if (lastLocation != null && lastAccuracy > 0)
				accuracy = " ("+Math.round(lastAccuracy)+"m)";
			
			listener.onSatelliteText(countFix+"/"+countMax+accuracy);			
		}
		
		if (event == GpsStatus.GPS_EVENT_STOPPED)
			Log.w("GpsStatus", "Provider stopped!");
	}
}
