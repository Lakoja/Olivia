package de.ulrich;

import org.mapsforge.core.GeoPoint;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;


public class TargetActivity extends Activity {
	private boolean isCancelled;
	
	private Spinner targetNumber;

  	private EditText fieldLat1;
	private EditText fieldLat2;
	private EditText fieldLon1;
	private EditText fieldLon2;
	private Button northIndicator;
	private Button eastIndicator;
	private Button copyCenter;
	private Button copyPosition;
	
	private EditText fieldGrades;
	private EditText fieldMeters;
	
	private GeoPoint mapCenter;
	private GeoPoint gpsPosition;
	
	// TODO take care of "return button"
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        setContentView(R.layout.target);
        
        targetNumber = (Spinner)findViewById(R.id.targetNumber);
 
	  	fieldLat1 = (EditText)findViewById(R.id.targetLatitude1);
    	fieldLat2 = (EditText)findViewById(R.id.targetLatitude2);
    	fieldLon1 = (EditText)findViewById(R.id.targetLongitude1);
    	fieldLon2 = (EditText)findViewById(R.id.targetLongitude2);
    	
    	northIndicator = (Button)findViewById(R.id.northIndicator);
    	eastIndicator = (Button)findViewById(R.id.eastIndicator);
    	
    	copyCenter = (Button)findViewById(R.id.copyCenter);
    	copyPosition = (Button)findViewById(R.id.copyPosition);
    	
    	fieldGrades = (EditText)findViewById(R.id.grades);
    	fieldMeters = (EditText)findViewById(R.id.meters);
    	
        Bundle extras = getIntent().getExtras();
        
        if (extras != null) {
        	GeoPoint targetPoint = (GeoPoint)extras.getSerializable("targetPoint");
        	if (targetPoint != null) {
        		setPointToFields(targetPoint);
        	}
        	
        	mapCenter = (GeoPoint)extras.getSerializable("mapCenter");
        	gpsPosition = (GeoPoint)extras.getSerializable("gpsPosition");
        	
        	if (mapCenter == null)
        		copyCenter.setClickable(false);
        	if (gpsPosition == null)
        		copyPosition.setClickable(false);
        	
        }

        
        Button btnCancel = (Button)findViewById(R.id.targetCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	isCancelled = true;
            	TargetActivity.this.finish();
            }
        });
        
        Button btnOk = (Button)findViewById(R.id.targetOk);
        btnOk.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	isCancelled = false;
            	calcResult();
            	TargetActivity.this.finish();
            }
        });
        
        northIndicator.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	if (northIndicator.getText().equals("N"))
            		northIndicator.setText("S");
            	else
            		northIndicator.setText("N");
            }
        });
        
        eastIndicator.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              	if (eastIndicator.getText().equals("E"))
              		eastIndicator.setText("W");
            	else
            		eastIndicator.setText("W");
            }
        });
        
        copyCenter.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              	if (mapCenter != null) {
              		showNotification(getString(R.string.center_taken));
              		setPointToFields(mapCenter);
              	}
            }
        });
        
        copyPosition.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
              	if (gpsPosition != null) {
              		showNotification(getString(R.string.position_taken));
              		setPointToFields(gpsPosition);
              	}
            }
        });
        
        Button btnBearing = (Button)findViewById(R.id.doBearing);
        btnBearing.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	GeoPoint current = extractPointFromFields();
            	
            	GeoPoint newTarget = doBearing(current);
            	
            	// TODO
          		//showNotification(getString(R.string.position_taken));
            	
            	if (newTarget != null)
            		setPointToFields(newTarget);
            	// TODO else
            	
            	// TODO NumberFormatException
            }
        });
        
        fieldLat1.requestFocus();
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
       
    private void calcResult() {
   	
    	GeoPoint targetPoint = null;
    	if (!isCancelled) {
        	try {
        		targetPoint = extractPointFromFields();
	        	
        	} catch (NumberFormatException exc) {
			 	// TODO make proper error
			 	AlertDialog.Builder builder = new AlertDialog.Builder(this);
			 	
			 	builder.setMessage(getString(R.string.number_wrong)+". "+
			 			getString(R.string.error_message)+": "+exc)
			 	       .setCancelable(false)
			 	       .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
			 	           public void onClick(DialogInterface dialog, int id) {
			 	        	   dialog.dismiss();
			 	           }
			 	       });
			 	AlertDialog alert = builder.create();
			 	alert.show();
			 	
			 	// TODO correction possibility
        	}
    	}
    	
    	Bundle data = new Bundle();
    	data.putSerializable("targetPoint", targetPoint);
    	
    	int selectedNumber = 0;
    	if (targetNumber.getSelectedItemPosition() != Spinner.INVALID_POSITION)
    		selectedNumber = targetNumber.getSelectedItemPosition();
    	data.putInt("targetNumber", selectedNumber);
    	
    	Intent dataIntent = new Intent();
    	dataIntent.putExtras(data);
    	setResult(0, dataIntent);
    }
    
    private GeoPoint extractPointFromFields() throws NumberFormatException {
		int lat1 = Integer.parseInt(fieldLat1.getText().toString());
		double lat2Raw = Double.parseDouble(fieldLat2.getText().toString());
		double lat2 = lat2Raw / 60;
		int lon1 = Integer.parseInt(fieldLon1.getText().toString());
		double lon2Raw = Double.parseDouble(fieldLon2.getText().toString());
		double lon2 = lon2Raw / 60;
		
		if (lat1 >= 0 && lat1 < 180 && lon1 >= 0 && lon1 < 180 
				&& lat2Raw >= 0 && lat2Raw < 60 && lon2Raw >= 0 && lon2Raw < 60) {
			
			if (northIndicator.getText().equals(getString(R.string.south_prefix))) {
				lat1 *= -1;
				lat2 *= -1;
			}
			
			if (eastIndicator.getText().equals(getString(R.string.west_prefix))) {
				lon1 *= -1;
				lon2 *= -1;
			}
			
			return new GeoPoint(lat1 * 1000000 + (int)Math.round(lat2 * 1000000), 
					lon1 * 1000000 + (int)Math.round(lon2 * 1000000));
		}
		
		return null;
	}

	private void setPointToFields(GeoPoint targetPoint) {
		
		int lat1 = Math.abs(targetPoint.latitudeE6 / 1000000);
		double lat2 = Math.abs((targetPoint.latitudeE6 / 1000000.0 - lat1) * 60);
		lat2 = Math.round(lat2 * 1000) / 1000.0; // 3 digits after comma
		int lon1 = Math.abs(targetPoint.longitudeE6 / 1000000);
		double lon2 = Math.abs((targetPoint.longitudeE6 / 1000000.0 - lon1) * 60);
		lon2 = Math.round(lon2 * 1000) / 1000.0;
		
		fieldLat1.setText(lat1+"");
		fieldLat2.setText(lat2+"");
		fieldLon1.setText(lon1+"");
		fieldLon2.setText(lon2+"");
		
		if (targetPoint.latitudeE6 < 0)
			northIndicator.setText(getString(R.string.south_prefix));
		else
			northIndicator.setText(getString(R.string.north_prefix));
		
		if (targetPoint.longitudeE6 < 0)
			eastIndicator.setText(getString(R.string.west_prefix));
		else
			eastIndicator.setText(getString(R.string.east_prefix));
    }
	

	private GeoPoint doBearing(GeoPoint current) throws NumberFormatException {
		double grades = Double.parseDouble(fieldGrades.getText().toString());
		double meters = Double.parseDouble(fieldMeters.getText().toString());
    	
		double digestible = (grades / 180) * Math.PI;
		
		double northMeters = Math.cos(digestible) * meters;
		double eastMeters = Math.sin(digestible) * meters;
		
		// 1850m per minute (for latitude)
    	
		double lat1 = current.getLatitude();
		double lat2 = lat1 + northMeters / (1850 * 60);
		double lon1 = current.getLongitude();
		double digestible2 = (((lat1 + lat2) / 2) / 180) * Math.PI;
		double below = 1850 * 60 * Math.cos(digestible2);
		double lon2 = lon1;
		if (below != 0)
			lon2 += eastMeters / below;
		
		GeoPoint newTarget = new GeoPoint(lat2, lon2);
		
		return newTarget;
	}
    
	public void showNotification(String text) {
		Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
	}
}

