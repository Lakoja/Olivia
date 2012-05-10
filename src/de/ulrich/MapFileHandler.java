package de.ulrich;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.mapsforge.core.GeoPoint;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;

public class MapFileHandler {
	
	private static MapFileHandler oneInstance;
	
	private boolean available;
	private String validPath;
	
	private MapFileHandler() {

		boolean writable;
		
	    String state = Environment.getExternalStorageState();
	    if (Environment.MEDIA_MOUNTED.equals(state)) {
	    	available = writable = true;
	    } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
	    	available = true;
	    	writable = false;
	    } else {
	    	available = writable = false;
	    }

	    if (available) {
	    	File dir = Environment.getExternalStorageDirectory();
	    	// "/sdcard/mapmap/baden-wuerttemberg-03.map"
	    	
	    	SharedPreferences sharedPrefs = 
	    		PreferenceManager.getDefaultSharedPreferences(MainActivity.instance());
	    			
	    	validPath = sharedPrefs.getString("validPath", null);
	    }
	}
	
	public boolean hasValidPath() {
		// TODO must this also rely on "available"?
		return validPath != null && validPath.length() > 0;
	}
	
	public String getValidPath() {
		return validPath;
	}
	
	public void setValidPath(String path) {
		validPath = path;
		
		SharedPreferences sharedPrefs = 
				PreferenceManager.getDefaultSharedPreferences(MainActivity.instance());
		Editor ed = sharedPrefs.edit();
		ed.putString("validPath", path);
		ed.commit();
	}
	
	public File getRootDirectory() {
		File dir = null;
		
		if (available)
			dir = Environment.getExternalStorageDirectory();
		
		return dir;
	}

	public List<File> getFirstLevel() {
		File dir = null;
		
		if (available)
			dir = Environment.getExternalStorageDirectory();
		
		return getNextLevel(dir);
	}
	
	public List<File> getNextLevel(File startDir) {
		LinkedList<File> level = new LinkedList<File>();
	    if (available && startDir != null) {	    	
	    	File[] files = startDir.listFiles();
	    	for (File f : files) {
	    		level.add(f);
	    	}
	    }
	    return level;
	}
	
	
	public static MapFileHandler instance() {
		if (oneInstance == null)
			oneInstance = new MapFileHandler();
		return oneInstance;
	}
}
