package de.ulrich;

import java.util.Iterator;

import org.mapsforge.core.GeoPoint;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

public class LocationHandler 
implements LocationListener, GpsStatus.Listener, SensorEventListener, OnSharedPreferenceChangeListener {

	private MyLocationListener listener;
	
	private SensorManager sensorManager;
	private Sensor orientationSensor;
	private LocationManager locationManager;
	private boolean firstFix;
	private long lastLocationChange;
	private GeoPoint lastLocation;
	private float lastAccuracy;
	private float[] lastOrientationValues;
	private long lastOrientationSending;
	
	private boolean paused = true;
	private boolean gpsActive;
	private TimeoutWatch watcher;
	private int timeoutMinutes;
	
	
	public LocationHandler(Activity father) {
        
        sensorManager = (SensorManager)father.getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        locationManager = (LocationManager)father.getSystemService(Context.LOCATION_SERVICE);
        listener = (MyLocationListener)father;
        
        //Location loc = locationManager.getLastKnownLocation(Context.LOCATION_SERVICE);
        //if (loc != null)
        //	onLocationChanged(loc);
        
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(father);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(sharedPrefs, null);
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key == null || key.equals("gpsTimeout"))
			timeoutMinutes = Integer.parseInt(prefs.getString("gpsTimeout", null));	
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
		if (!paused) {
			Log.w("GpsStatus", "Pausing");
			paused = true;
	
			// TODO pause also called when activity called
			if (gpsActive && timeoutMinutes > 0) {
				Log.w("GpsStatus", "Watcher started in pause() with "+timeoutMinutes);
				watcher = new TimeoutWatch(this, timeoutMinutes);
				watcher.start();
			}
		}
	}
	
	public void resume() {
		if (paused) {
			Log.w("GpsStatus", "Resuming");
			paused = false;
			
	        sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL);

			if (watcher != null) {
				Log.w("GpsStatus", "Watcher removed on resume()");
				watcher.shutdown();
				watcher = null;
			}
	
			if (!gpsActive) {
				gpsActive = true;

				locationManager.addGpsStatusListener(this);
				locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 0.5f, this);
			}
		}
	}
	
	public int getTimoutMinutes() {
		return timeoutMinutes;
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
			
			listener.onSatelliteInfo(countFix, countMax, lastAccuracy);			
		}
		
		if (event == GpsStatus.GPS_EVENT_STOPPED)
			Log.w("GpsStatus", "Provider stopped!");
	}
	
	@Override
	public void onSensorChanged(SensorEvent event) {
		lastOrientationValues = event.values;
		
		long now = System.currentTimeMillis();
		
		if (now - lastOrientationSending > 500) {	
			listener.onOrientation(lastOrientationValues[0]);
			lastOrientationSending = now;
		}
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	
	public void timeoutOccured() {
		Log.w("GpsStatus", "Timeout with "+gpsActive);
		if (gpsActive)
			shutdown();
	}
		
	public void shutdown() {
        locationManager.removeUpdates(this);
        locationManager.removeGpsStatusListener(this);
		if (watcher != null) {
			Log.w("GpsStatus", "Watcher removed on shutdown()");
			watcher.shutdown();
			watcher = null;
		}
		Log.w("GpsStatus", "Signal inactivity");
        listener.onLocationChanged(null, 0); // signal inactivity
        gpsActive = false;
	}
}
