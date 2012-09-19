package de.ulrich;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;
import android.util.Log;

public class BatteryHandler extends BroadcastReceiver {
    
    private float level = -1;
    private boolean isCharging = false;
    
    private float switchOffLevel = 0.05f;    
    private int belowCount = 0;
    private long lastBelowSecond = 0;
    private boolean notifyDispatched = false;
    
    private MainActivity listener;
    
    public BatteryHandler(MainActivity father, float lowLevel) {
    	switchOffLevel = lowLevel;
    	listener = father;
    	
    	// onReceive is called directly afterwards
    }
      
    @Override
    public void onReceive(Context context, Intent intent) {
        int levelI = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scaleI = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int chargeI = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        
        //temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
        //voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
        
        isCharging = chargeI != 0;       
        level = levelI/(float)scaleI;
        
        if (level <= switchOffLevel && !notifyDispatched) {
        	// Do some filtering: Only if it is 3 (or more) seconds below in a row
        	
        	long second = System.currentTimeMillis() / 1000;
        	if (second != lastBelowSecond) {
        		lastBelowSecond = second;
        		belowCount++;
        		
        		if (belowCount >= 3) {
        			
        			listener.batteryStatus(false);
        			notifyDispatched = true;     			
        		}
        	}
        } else if (notifyDispatched) {
    		notifyDispatched = false;
    		belowCount = 0;
    		lastBelowSecond = 0;
    		
    		listener.batteryStatus(true);
    	}
    }
    
    public boolean isBatteryOk() {
    	return level > switchOffLevel || isCharging;
    }
}
