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


public class TargetActivity extends Activity {
	private boolean isCancelled;

  	private EditText fieldLat1;
	private EditText fieldLat2;
	private EditText fieldLon1;
	private EditText fieldLon2;
	private Button northIndicator;
	private Button eastIndicator;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        setContentView(R.layout.target);
 
	  	fieldLat1 = (EditText)findViewById(R.id.targetLatitude1);
    	fieldLat2 = (EditText)findViewById(R.id.targetLatitude2);
    	fieldLon1 = (EditText)findViewById(R.id.targetLongitude1);
    	fieldLon2 = (EditText)findViewById(R.id.targetLongitude2);
    	
    	northIndicator = (Button)findViewById(R.id.northIndicator);
    	eastIndicator = (Button)findViewById(R.id.eastIndicator);
    	
        Bundle extras = getIntent().getExtras();
        
        if (extras != null) {
        	GeoPoint targetPoint = (GeoPoint)extras.getSerializable("targetPoint");
        	if (targetPoint != null) {
        		// TODO might be negative??
        		
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
        
        fieldLat1.requestFocus();
        
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
       
    private void calcResult() {
   	
    	GeoPoint targetPoint = null;
    	if (!isCancelled) {
        	try {
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
        			
        			targetPoint = new GeoPoint(lat1 * 1000000 + (int)Math.round(lat2 * 1000000), 
        					lon1 * 1000000 + (int)Math.round(lon2 * 1000000));
        		}
	        	
        	} catch (NumberFormatException exc) {
			 	// TODO make proper error
			 	AlertDialog.Builder builder = new AlertDialog.Builder(this);
			 	
			 	builder.setMessage(getString(R.string.number_wrong)+". "+
			 			getString(R.string.error_message)+": "+exc)
			 	       .setCancelable(false)
			 	       .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
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
    	Intent dataIntent = new Intent();
    	dataIntent.putExtras(data);
    	setResult(0, dataIntent);
    }
}
