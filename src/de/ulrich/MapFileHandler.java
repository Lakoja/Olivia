package de.ulrich;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.mapsforge.core.GeoPoint;

import android.os.Bundle;
import android.os.Environment;

public class MapFileHandler {
	
	private static MapFileHandler oneInstance;
	
	private boolean available;
	private String lastValidPath;
	
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
	    	
	    	
	    }
	}
	
	public void restore(Bundle savedState) {
		if (savedState != null) {
			lastValidPath = savedState.getString("lastValidPath");
		}		
	}
	
	public void save(Bundle outState) {
		if (hasValidPath())
			outState.putString("lastValidPath", lastValidPath);
	}
	
	public boolean hasValidPath() {
		// TODO must this also rely on "available"?
		return lastValidPath != null && lastValidPath.length() > 0;
	}
	
	public String getValidPath() {
		return lastValidPath;
	}
	
	public void setValidPath(String path) {
		lastValidPath = path;
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
