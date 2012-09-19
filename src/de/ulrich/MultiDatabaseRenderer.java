package de.ulrich;

import java.io.File;
import java.io.FileNotFoundException;

import org.mapsforge.android.maps.mapgenerator.JobTheme;
import org.mapsforge.android.maps.mapgenerator.MapGeneratorJob;
import org.mapsforge.android.maps.mapgenerator.databaserenderer.DatabaseRenderer;
import org.mapsforge.map.reader.MapDatabase;
import org.mapsforge.map.reader.header.FileOpenResult;

import android.graphics.Bitmap;
import android.graphics.Paint;

public class MultiDatabaseRenderer extends DatabaseRenderer {
	private MapDatabase myown;
	private File myownFile;
	
	@Override
	public boolean executeJob(MapGeneratorJob mapGeneratorJob, Bitmap bitmap) {
		
		
		File otherFile = new File(myownFile.getParentFile(), "switzerland-Aug12.map");
		boolean exists = otherFile.exists();
		
		
		MapDatabase md2 = new MapDatabase();
		FileOpenResult res = new FileOpenResult("not initialized");
		if (otherFile.exists())
			res = md2.openFile(otherFile);
		
		if (res.isSuccess())
			super.setMapDatabase(md2);
		
		/*
		JobTheme jt = mapGeneratorJob.jobParameters.jobTheme;
		JobThemeWrapper ww;
		try {
			ww = new JobThemeWrapper(jt.getRenderThemeAsStream());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//mapGeneratorJob.jobParameters.jobTheme = ww;*/
		
		boolean did = super.executeJob(mapGeneratorJob, bitmap);
		
		if (res.isSuccess())
			super.setMapDatabase(myown);
		
		boolean did2 = super.executeJob(mapGeneratorJob, bitmap);
		
		return did2;
	}
	/*
	@Override
	public void renderArea(Paint paint, int level) {
	
	}*/
	
	@Override
	public void setMapDatabase(MapDatabase mapDatabase) {
		myown = mapDatabase;
		super.setMapDatabase(mapDatabase);
	}
	
	public void setFile(File mapFile) {
		myownFile = mapFile;
	}
}
