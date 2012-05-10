package de.ulrich;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.JobQueue;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.map.reader.header.FileOpenResult;

public class MainActivity extends MapActivity 
implements MyLocationListener, OnSharedPreferenceChangeListener, OnTouchListener {
	
	private static MainActivity oneInstance;
	
	private static final int TARGET_ACTIVITY_CODE = 1;
	private static final int SELECT_ACTIVITY_CODE = 2;
	private static final int SETTINGS_ACTIVITY_CODE = 3;
	
	private LocationHandler gpsHandler;
	private long lastValidLocationMs;
	private int lastActiveSatellites;
	private boolean doSoundFirstFix;
	private boolean didSoundFirstFix;
	private boolean trackPosition;
	private Point touchStart;
	private boolean touchDown;
	
	private MapFileHandler mapHandler;
	
	private MapView mapView;
	private TextView textLatitude;
	private TextView textLongitude;
	private TextView textTargetDistance;
	private TextView textTargetBearing;
	private TextView textSatellites;
	private MyImageView gpsStateView;
	private ToggleButton homingToggle;
	
	private PositionOverlay overlay;
	
	private boolean mapViewReplaced;
	
	private Handler handler = new Handler();
	
    private DecimalFormat geoFormaterMinutes = new DecimalFormat("00.000");
    private DecimalFormat distanceFormaterKilo = new DecimalFormat("##0.0");
    
    private SoundHandler sounder;
    
    private boolean paused = true;
    
    private double distance;
    private double bearing;
    
    public static MainActivity instance() {
    	return oneInstance;
    }
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        setContentView(R.layout.main);
        
        oneInstance = this;
        
        // Set  default values in prefs.xml (only once)
        PreferenceManager.setDefaultValues(this, R.xml.prefs, false);      
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPrefs.registerOnSharedPreferenceChangeListener(this);
        onSharedPreferenceChanged(sharedPrefs, null);
        
        textLatitude = (TextView)findViewById(R.id.latitude);
        textLongitude = (TextView)findViewById(R.id.longitude);
        textTargetDistance = (TextView)findViewById(R.id.targetDistance);
        textTargetBearing = (TextView)findViewById(R.id.targetBearing);
        textSatellites = (TextView)findViewById(R.id.satellites);
        gpsStateView = (MyImageView)findViewById(R.id.gpsActive);
               
        mapView = new MapView(this);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(false);
        mapView.getMapScaleBar().setShowMapScaleBar(true);
        mapView.setOnTouchListener(this);
        //mapView.setMapViewMode(MapViewMode.MAPNIK_TILE_DOWNLOAD);
  
        	//loadMapFile(new File("/sdcard/mapmap/baden-wuerttemberg-03.map"));

                 
        ImageButton btnZoomIn = (ImageButton)findViewById(R.id.zoomIn);
        btnZoomIn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	mapView.getController().zoomIn();
            }
        });
        //btnZoomIn.getBackground().setColorFilter(0xFFc0c0c0, PorterDuff.Mode.MULTIPLY);
        
        ImageButton btnZoomOut = (ImageButton)findViewById(R.id.zoomOut);
        btnZoomOut.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	mapView.getController().zoomOut();
            }
        });
        //btnZoomOut.getBackground().setColorFilter(0xFFc0c0c0, PorterDuff.Mode.MULTIPLY);
        
        //PorterDuffColorFilter filter = new PorterDuffColorFilter(Color.RED, PorterDuff.Mode.SRC_ATOP);  
        //btnZoomOut.getDrawable().setColorFilter(filter); 
            
        homingToggle = (ToggleButton)findViewById(R.id.homing);
        homingToggle.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
            	trackPosition = homingToggle.isChecked();
            	
            	SharedPreferences sharedPrefs = 
            			PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
            	Editor ed = sharedPrefs.edit();
            	ed.putBoolean("trackPosition", trackPosition);
            	ed.commit();

            	if (trackPosition && overlay.getPosition() != null)
            		mapView.getController().setCenter(overlay.getPosition());
            }
        });
        //btnHoming.getBackground().setColorFilter(0xFFc0c0c0, PorterDuff.Mode.MULTIPLY);

        ImageButton btnTarget = (ImageButton)findViewById(R.id.target);
        btnTarget.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	showTargetSelector();
            }
        });
        
        Button btnMapSelectStart = (Button)findViewById(R.id.selectMapFile);
        btnMapSelectStart.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	showMapSelector();
            }
        });
        
        sounder = new SoundHandler(this);
        
        overlay = new PositionOverlay(getWindowManager().getDefaultDisplay(), mapView);
        
        mapView.getOverlays().add(overlay);
        
        gpsHandler = new LocationHandler(this);
        
        mapHandler = MapFileHandler.instance();
        if (mapHandler.hasValidPath()) {
        	boolean mapSuccess = loadMapFile(new File(mapHandler.getValidPath()));
        	if (mapSuccess)
        		replaceMapView();        	
        }

        if (savedInstanceState != null) {/*
	        Object lastMapCenterO = savedInstanceState.getSerializable("lastMapCenter");
	        if (lastMapCenterO != null) {
	        	lastMapCenter = (GeoPoint)lastMapCenterO;
	        	mapView.getController().setCenter(lastMapCenter);
	        }
	        
	        Object lastTargetO = savedInstanceState.getSerializable("lastTarget");
	        if (lastTargetO != null)
	        	overlay.setTarget((GeoPoint)lastTargetO);
	        
	        trackPosition = savedInstanceState.getBoolean("trackPosition", trackPosition);
	        if (trackPosition)
	        	homingToggle.setChecked(true);
	        
	        mapHandler.restore(savedInstanceState);
	        */
	        gpsHandler.restore(savedInstanceState);
	        
        }
        
        restorePreferences(sharedPrefs);
             
		calcDistanceAndBearing();
		setDistanceAndBearingTexts();
              
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }


	private void restorePreferences(SharedPreferences sharedPrefs) {
		
		// TODO care about "instance state" vs. shared preferences
        
		// TODO handle in PositionOverlay?
		
		ArrayList<GeoPoint> targetsNew = new ArrayList<GeoPoint>(10);
		
		int i = 0;
		while (sharedPrefs.contains("targetLatitude" + i)) {
    		float lat = sharedPrefs.getFloat("targetLatitude" + i, 0);
    		float lon = sharedPrefs.getFloat("targetLongitude" + i, 0);
    		targetsNew.add(new GeoPoint(lat, lon));		
    		i++;
		}		
		if (targetsNew.size() <= 0) {
			targetsNew.add(new GeoPoint(48.05, 7.8));
		}
		
		overlay.setTargets(targetsNew);

    
        /*   
        if (!mapHandler.hasValidPath()) {
        	if (sharedPrefs.contains("lastValidPath")) {
        		String lastValidPath = sharedPrefs.getString("lastValidPath", null);
        		boolean mapSuccess = loadMapFile(new File(lastValidPath));
        		if (mapSuccess)
        			mapHandler.setValidPath(lastValidPath);
        	}
        }*/
        
        homingToggle.setChecked(sharedPrefs.getBoolean("trackPosition", false));
        
        // TODO map saves the last map center...
		
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
		if (key == null || key.equals("soundFirstFix"))
    		doSoundFirstFix = prefs.getBoolean("soundFirstFix", false);
	}
	
	/*
	@Override
	public void run() {
		while (!isFinishing() && !paused) {
			
			if (overlay.getTarget() != null) {
				// Calculate distance
				GeoPoint origin = null;
				if (overlay.getPosition() != null)
					origin = overlay.getPosition();
				else
					origin = mapView.getMapPosition().getMapCenter();
				
				GeoPoint target = overlay.getTarget();

				double lat1 = (origin.getLatitude() / 180) * Math.PI;
				double lat2 = (target.getLatitude() / 180) * Math.PI;
				double lon1 = (origin.getLongitude() / 180) * Math.PI;
				double lon2 = (target.getLongitude() / 180) * Math.PI;
				
				// From http://www.kompf.de/gps/distcalc.html
				distance = 6378388 * Math.acos(Math.sin(lat1) * Math.sin(lat2) + 
						Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1));
				
				if (overlay.getPosition() != null) {
					int diffy = target.latitudeE6 - origin.latitudeE6;
					int diffx = target.longitudeE6 - origin.longitudeE6;
					
					bearing = 0;
					
					if (diffy != 0 || diffx != 0) {
						bearing = Math.toDegrees(Math.atan2(diffx, diffy));
						if (bearing < 0)
							bearing = 360 + bearing;
					}
				}
			}
			
			
			handler.post(new Runnable() {
				public void run() {
					
					//long now = System.currentTimeMillis();
					//if (now - lastLocationChange >= 3000)
					//	gpsStateView.setImageResource(R.drawable.gpsInactive);
				
					if (distance != 0)
						textTargetDistance.setText(formatDistance(distance));
					
					textTargetBearing.setText(Math.round(bearing)+"°");
				}
			});
		
			try { Thread.sleep(500); } catch (InterruptedException exc) {}
		}		
	}*/
	
	@Override
    protected void onResume() {
        super.onResume();
        boolean isScreenOn = ((PowerManager) getSystemService(Context.POWER_SERVICE)).isScreenOn();
        if (isScreenOn) {
        	paused = false;
        	gpsHandler.resume();
           
            //new Thread(this).start();
        }
    }

	@Override
    protected void onPause() {
        super.onPause();
        if (!paused) {
        	paused = true;
        	gpsHandler.pause();
        }
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		sounder.shutdown();
		gpsHandler.shutdown();
	}
	
	protected void onSaveInstanceState(Bundle outState) {
		/*
		if (lastMapCenter != null)
			outState.putSerializable("lastMapCenter", lastMapCenter);
		if (overlay.getTarget() != null)
			outState.putSerializable("lastTarget", overlay.getTarget());
		outState.putBoolean("trackPosition", trackPosition);
		mapHandler.save(outState);
		*/
		gpsHandler.save(outState);
		// TODO what does this use?
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.mainmenu, menu);
	    return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		
	    switch (id) {
	    case R.id.targetMenu:
	    	showTargetSelector();
	    	return true;
	    case R.id.settingsMenu:
	    	showSettingsSelector();
	    	return true;
	    case R.id.fileMenu:
	    	showMapSelector();
	    	return true;
	    default:
	    	return super.onOptionsItemSelected(item);

	    }
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
		if (requestCode == TARGET_ACTIVITY_CODE) {
			if (dataIntent != null) {
				Bundle data = dataIntent.getExtras();
				if (data != null) {
					GeoPoint gp = (GeoPoint)data.getSerializable("targetPoint");
					if (gp != null) {
						ArrayList<GeoPoint> targetsNew = new ArrayList<GeoPoint>(10);
						targetsNew.add(gp);
						// TODO multiple targets!
						overlay.setTargets(targetsNew);
						
						SharedPreferences sharedPrefs = 
								PreferenceManager.getDefaultSharedPreferences(this);
						Editor ed = sharedPrefs.edit();
						int i = 0;
						for (; i<targetsNew.size(); i++) {
							GeoPoint gpWrite = targetsNew.get(0);
						
							ed.putFloat("targetLatitude" + i, (float)gpWrite.getLatitude());
							ed.putFloat("targetLongitude" + i, (float)gpWrite.getLongitude());
						}
						
						while (sharedPrefs.contains("targetLatitude" + i)) {
							ed.remove("targetLatitude" + i);
							ed.remove("targetLongitude" + i);
							i++;
						}
						
						ed.commit();
						
						// TODO a geo handler should handle this?
					}
				}
			}
		} else if (requestCode == SELECT_ACTIVITY_CODE) {
			if (dataIntent != null) {
				Bundle data = dataIntent.getExtras();
				if (data != null) {
					File selectedFile = (File)data.getSerializable("selectedFile");
					boolean mapSuccess = loadMapFile(selectedFile);
					if (mapSuccess) {
						mapHandler.setValidPath(selectedFile.getAbsolutePath());
						
						if (!mapViewReplaced)
							replaceMapView();
					}
				}
			}
		}
	}
	


	@Override
	public boolean onTouch(View v, MotionEvent event) {
		switch(event.getAction()) {
	        case(MotionEvent.ACTION_DOWN):
	        	touchStart = new Point((int)event.getX(), (int)event.getY());
	        	touchDown = true;
	            break;
	        case(MotionEvent.ACTION_UP):
	        	Point touchEnd = new Point((int)event.getX(), (int)event.getY());
	        	if (!touchEnd.equals(touchStart)) {
	        		homingToggle.setChecked(false);
	        		trackPosition = false;
	        	}
	        	
	        	touchStart = null;
	        	touchDown = false;
	        	
	        	calcDistanceAndBearing();
	    		setDistanceAndBearingTexts();
	        	
	        	break;
        }
		return false;
	}


	public void onLocationChanged(GeoPoint location, float accuracy) {
		
		if (location != null) {
			setLocationTexts(location.getLatitude(), location.getLongitude());		
			overlay.setPosition(location, accuracy);
			
			calcDistanceAndBearing();
			setDistanceAndBearingTexts();
			

			if (lastActiveSatellites > 3) {
				lastValidLocationMs = System.currentTimeMillis();
				if (doSoundFirstFix && !didSoundFirstFix) {
					sounder.play(true);
					didSoundFirstFix = true;
				}
			}
			
			if (trackPosition && !touchDown) {
				mapView.getController().setCenter(location);
			}
						
		} else {
			//sounder.play(false);
		}
	}

	public void onSatelliteInfo(int activeSatellites, int totalSatellites, float accuracy) {
		String accuracyText = "";
		//if (accuracy > 0)
		//	accuracyText = " ("+Math.round(accuracy)+"m)";
		
		textSatellites.setText(activeSatellites+"/"+totalSatellites+accuracyText);
		
		lastActiveSatellites = activeSatellites;
	}
	
	public void onOrientation(float orientation) {
		gpsStateView.setOrientation(orientation);
		overlay.setOrientation(orientation);
		//textSatellites.setText(orientation+""); //geoFormaterGrades.format(orientation));
	}
	

	private void calcDistanceAndBearing() {
		if (overlay.hasTargets()) {
			// Calculate distance
			GeoPoint origin = null;
			if (overlay.getPosition() != null)
				origin = overlay.getPosition();
			else
				origin = mapView.getMapPosition().getMapCenter();
			
			// TODO multiple targets!
			GeoPoint target = overlay.getTargets().get(0);

			double lat1 = (origin.getLatitude() / 180) * Math.PI;
			double lat2 = (target.getLatitude() / 180) * Math.PI;
			double lon1 = (origin.getLongitude() / 180) * Math.PI;
			double lon2 = (target.getLongitude() / 180) * Math.PI;
			
			// From http://www.kompf.de/gps/distcalc.html
			distance = 6378388 * Math.acos(Math.sin(lat1) * Math.sin(lat2) + 
					Math.cos(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1));
			
			if (overlay.getPosition() != null) {
				int diffy = target.latitudeE6 - origin.latitudeE6;
				int diffx = target.longitudeE6 - origin.longitudeE6;
				
				bearing = 0;
				
				if (diffy != 0 || diffx != 0) {
					bearing = Math.toDegrees(Math.atan2(diffx, diffy));
					if (bearing < 0)
						bearing = 360 + bearing;
				}
			}
		}		
	}
	
	private void setDistanceAndBearingTexts() {
		
		if (distance != 0)
			textTargetDistance.setText(formatDistance(distance));
		
		if (overlay.hasTargets())
			textTargetBearing.setText(Math.round(bearing)+"°");
		else
			textTargetBearing.setText("");
	}
	
	private void setLocationTexts(double lat, double lon) {
		
		String latitude = (lat >= 0 ? getString(R.string.north_prefix) : 
			getString(R.string.south_prefix)) + " ";
		String longitude = (lon >= 0 ? getString(R.string.east_prefix) : 
			getString(R.string.west_prefix)) + " ";
		
		lat = Math.abs(lat);
		
		latitude += ((int)lat) + "° ";
		longitude += ((int)lon) + "° ";
		
		double latRest = lat - Math.floor(lat);
		lon = Math.abs(lon);
		double lonRest = lon - Math.floor(lon);
		
		latitude += geoFormaterMinutes.format(latRest * 60);
		longitude += geoFormaterMinutes.format(lonRest * 60);
			
		textLatitude.setText(latitude);
		textLongitude.setText(longitude);	
	}
	
	private void showTargetSelector() {
        Intent intent = new Intent();
        if (overlay.hasTargets()) {
        	Bundle extras = new Bundle();
        	// TODO multiple targets!
        	extras.putSerializable("targetPoint", overlay.getTargets().get(0));
        	extras.putSerializable("mapCenter", mapView.getMapPosition().getMapCenter());
        	if (overlay.getPosition() != null)
        		extras.putSerializable("gpsPosition", overlay.getPosition());
        	intent.putExtras(extras);
        }
        intent.setClass(this, TargetActivity.class);
        startActivityForResult(intent, TARGET_ACTIVITY_CODE);
	}
	
	private void showMapSelector() {
     	//bleeper.play(true);
     	
        Intent intent = new Intent();
        if (mapHandler.hasValidPath()) {
        	Bundle extras = new Bundle();
        	extras.putSerializable("selectedFile", new File(mapHandler.getValidPath()));
        	intent.putExtras(extras);
        }
        intent.setClass(MainActivity.this, FileSelectActivity.class);
        startActivityForResult(intent, SELECT_ACTIVITY_CODE);
	}
	
	private void showSettingsSelector() {
        Intent intent = new Intent();
        intent.setClass(this, SettingsActivity.class);
        startActivityForResult(intent, SETTINGS_ACTIVITY_CODE);
	}
	
	private boolean loadMapFile(File selected) {
		File activeMap = mapView.getMapFile();
		
		if (!selected.equals(activeMap)) {
	        FileOpenResult result = mapView.setMapFile(selected);
	        
			if (!result.isSuccess()) {
			 	Log.e(this.getClass().toString(), "No valid map");
			 	
			 	// TODO make proper error
			 	AlertDialog.Builder builder = new AlertDialog.Builder(this);
			 	
			 	builder.setMessage(getString(R.string.invalid_map_file)+". "+
			 			getString(R.string.error_message)+": "+result.getErrorMessage())
			 	       .setCancelable(false)
			 	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			 	           public void onClick(DialogInterface dialog, int id) {
			 	        	   dialog.dismiss();
			 	           }
			 	       });
			 	AlertDialog alert = builder.create();
			 	alert.show();
			 	
			 	return false;
			}
		}
		
		return true;
	}
	
	
	private void replaceMapView() {
        
        LinearLayout layout = (LinearLayout)findViewById(R.id.dummyLayout);      
        View dummy = findViewById(R.id.dummyMap);       
        layout.removeView(dummy);
       
        mapView.setLayoutParams(dummy.getLayoutParams());    
        layout.addView(mapView, 0);
        
        mapViewReplaced = true;
	}
	
	private String formatDistance(double distanceMeters) {
		// 342 m
		// 2.3 km
		// 21.4 km
		// 233 km
		
		if (distanceMeters >= 100*1000)
			return Math.round(distanceMeters / 1000) + " km";
		else if (distanceMeters >= 1000)
			return distanceFormaterKilo.format(distanceMeters / 1000) + " km";
		else
			return Math.round(distanceMeters) + " m";
	}


}
