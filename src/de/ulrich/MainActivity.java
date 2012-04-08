package de.ulrich;

import java.io.File;
import java.text.DecimalFormat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.mapsforge.android.maps.MapActivity;
import org.mapsforge.android.maps.MapView;
import org.mapsforge.android.maps.mapgenerator.JobQueue;
import org.mapsforge.core.GeoPoint;
import org.mapsforge.map.reader.header.FileOpenResult;

public class MainActivity extends MapActivity implements MyLocationListener, Runnable {
	private GpsHandler gpsHandler;
	
	private MapView mapView;
	private GeoPoint lastMapCenter;
	private TextView textLatitude;
	private TextView textLongitude;
	private TextView textTargetDistance;
	private TextView textTargetBearing;
	private TextView textSatellites;
	private ImageView gpsStateView;
	
	private PositionOverlay overlay;
	
	private Handler handler = new Handler();
	private JobQueue mapQueue;
	private ProgressBar mapProgress;
	
    private DecimalFormat geoFormaterGrades = new DecimalFormat("000.0000");
    private DecimalFormat geoFormaterMinutes = new DecimalFormat("00.000");
    private DecimalFormat distanceFormaterKilo = new DecimalFormat("##0.0");
    
    private SoundHandler bleeper;
    
    private boolean paused = true;
    
    private double distance;
    private double bearing;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        setContentView(R.layout.main);
        
        textLatitude = (TextView)findViewById(R.id.latitude);
        textLongitude = (TextView)findViewById(R.id.longitude);
        textTargetDistance = (TextView)findViewById(R.id.targetDistance);
        textTargetBearing = (TextView)findViewById(R.id.targetBearing);
        textSatellites = (TextView)findViewById(R.id.satellites);
        gpsStateView = (ImageView)findViewById(R.id.gpsActive);
        
        LinearLayout layout = (LinearLayout)findViewById(R.id.dummyLayout);      
        View dummy = findViewById(R.id.dummyMap);       
        layout.removeView(dummy);
               
        mapView = new MapView(this);
        mapView.setClickable(true);
        mapView.setBuiltInZoomControls(false);
        mapView.getMapScaleBar().setShowMapScaleBar(true);
        //mapView.setMapViewMode(MapViewMode.MAPNIK_TILE_DOWNLOAD);
        FileOpenResult result = mapView.setMapFile(new File("/sdcard/mapmap/baden-wuerttemberg-03.map"));
             
		 if (!result.isSuccess()) {
		 	Log.e(this.getClass().toString(), "No valid map");
		 	
		 	AlertDialog.Builder builder = new AlertDialog.Builder(this);
		 	builder.setMessage("Map not valid")
		 	       .setCancelable(false)
		 	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
		 	           public void onClick(DialogInterface dialog, int id) {
		 	        	   MainActivity.this.finish();
		 	           }
		 	       });
		 	AlertDialog alert = builder.create();
		 	alert.show();
		 }
        
        
        mapView.setLayoutParams(dummy.getLayoutParams());    
        layout.addView(mapView, 0);
        
             
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
            
        ImageButton btnHoming = (ImageButton)findViewById(R.id.homing);
        btnHoming.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {

            	if (overlay.getPosition() != null)
            		mapView.getController().setCenter(overlay.getPosition());
            }
        });
        //btnHoming.getBackground().setColorFilter(0xFFc0c0c0, PorterDuff.Mode.MULTIPLY);

        bleeper = new SoundHandler(this, R.raw.notify);

        mapQueue = mapView.getJobQueue();
        mapProgress = (ProgressBar)findViewById(R.id.mapStateSpinner);

        
        overlay = new PositionOverlay(getWindowManager().getDefaultDisplay(), mapView);
        overlay.setTarget(new GeoPoint(48.05, 7.8));
        
        mapView.getOverlays().add(overlay);
        
        gpsHandler = new GpsHandler(this);

        if (savedInstanceState != null) {
	        Object lastMapCenterO = savedInstanceState.getSerializable("lastMapCenter");
	        if (lastMapCenterO != null) {
	        	lastMapCenter = (GeoPoint)lastMapCenterO;
	        	mapView.getController().setCenter(lastMapCenter);
	        }
	        
	        Object lastTargetO = savedInstanceState.getSerializable("lastTarget");
	        if (lastTargetO != null)
	        	overlay.setTarget((GeoPoint)lastTargetO);
	        
	        gpsHandler.restore(savedInstanceState);
        }
        
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        
        new Thread(this).start();
    }

	
	@Override
	public void run() {
		while (!isFinishing()) {
			
			if (!paused) {
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
						
						if (mapQueue.isEmpty())
							mapProgress.setVisibility(View.INVISIBLE);
						else
							mapProgress.setVisibility(View.VISIBLE);
						
						//long now = System.currentTimeMillis();
						//if (now - lastLocationChange >= 3000)
						//	gpsStateView.setImageResource(R.drawable.gpsInactive);
					
						if (distance != 0)
							textTargetDistance.setText(formatDistance(distance));
						
						//if (overlay.getPosition() != null)
							textTargetBearing.setText(Math.round(bearing)+"°");
						//else
						//	textTargetBearing.setText(" ");
					}
				});
			}
			
			try { Thread.sleep(500); } catch (InterruptedException exc) {}
		}		
	}
	
	@Override
    protected void onResume() {
        super.onResume();
        paused = false;
        gpsHandler.resume();
    }

	@Override
    protected void onPause() {
        super.onPause();
        paused = true;
        gpsHandler.pause();
    }
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		bleeper.deconstruct();
	}
	
	protected void onSaveInstanceState(Bundle outState) {
		if (lastMapCenter != null)
			outState.putSerializable("lastMapCenter", lastMapCenter);
		if (overlay.getTarget() != null)
			outState.putSerializable("lastTarget", overlay.getTarget());
		gpsHandler.save(outState);
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
	    	showTargetDefiner();
	    	return true;
	    case R.id.settingsMenu:
	    	//showTargetDefiner();
	    	return true;
	    default:
	    	return super.onOptionsItemSelected(item);

	    }
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent dataIntent) {
		if (dataIntent != null) {
			Bundle data = dataIntent.getExtras();
			if (data != null) {
				GeoPoint gp = (GeoPoint)data.getSerializable("targetPoint");
				if (gp != null)
					overlay.setTarget(gp);
			}
		}
	}
	
	public void onLocationChanged(GeoPoint location, float accuracy) {
		
		setLocationTexts(location.getLatitude(), location.getLongitude());
		
		overlay.setPosition(location, accuracy);
	}

	public void onSatelliteText(String status) {
		textSatellites.setText(status);
	}
	
	private void setLocationTexts(double lat, double lon) {
		
		String latitude = (lat >= 0 ? "N " : "S ");
		String longitude = (lon >= 0 ? "E " : "W "); // TODO locale
		
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
	
	private void showTargetDefiner() {
        Intent intent = new Intent();
        if (overlay.getTarget() != null) {
        	Bundle extras = new Bundle();
        	extras.putSerializable("targetPoint", overlay.getTarget());
        	intent.putExtras(extras);
        }
        intent.setClass(this, TargetActivity.class);
        startActivityForResult(intent, 0);
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
