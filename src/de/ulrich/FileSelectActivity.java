package de.ulrich;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.mapsforge.core.GeoPoint;

import android.app.ListActivity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;

public class FileSelectActivity extends ListActivity 
implements AdapterView.OnItemClickListener, Comparator<File> {
	
	private FileAdapter adapter;
	
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);       
        setContentView(R.layout.select);
        
        MapFileHandler filer = MapFileHandler.instance();
        
        List<File> fileLevel = null;
        Bundle extras = getIntent().getExtras();        
        if (extras != null) {
        	File selectedFile = (File)extras.getSerializable("selectedFile");
        	if (selectedFile != null) {
        		fileLevel = filer.getNextLevel(selectedFile.getParentFile());
        	}
        }
        
        if (fileLevel == null)
        	fileLevel = filer.getFirstLevel();
        
        Collections.sort(fileLevel, this);
        
        fileLevel.add(0, filer.getRootDirectory());
        
		adapter = new FileAdapter(this, 0, fileLevel);		
		setListAdapter(adapter);
        
        Button btnCancel = (Button)findViewById(R.id.selectCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	FileSelectActivity.this.finish();
            }
        });
        
        ListView list = getListView();
        list.setOnItemClickListener(this);
        
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }


	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		File file = (File)parent.getItemAtPosition(position);
		
		if (file.isDirectory()) {
	        MapFileHandler filer = MapFileHandler.instance();
	        
	        if (position == 0) {
	        	// Move upwards
	        	if (!file.equals(filer.getRootDirectory()))
	        		file = file.getParentFile();
	        }
	        
	        List<File> nextLevel = filer.getNextLevel(file);
			adapter.clear();
			Collections.sort(nextLevel, this);
			nextLevel.add(0, file);
			adapter.addAll(nextLevel);
			getListView().invalidateViews();
		} else {
	    	Bundle data = new Bundle();
	    	data.putSerializable("selectedFile", file);
	    	Intent dataIntent = new Intent();
	    	dataIntent.putExtras(data);
	    	setResult(0, dataIntent);
	    	
	    	FileSelectActivity.this.finish();
		}
	}


	@Override
	public int compare(File file1, File file2) {
		if (file1.isDirectory()) {
			if (file2.isDirectory())
				return file1.getPath().compareToIgnoreCase(file2.getPath());
			else
				return -1;
		} else {
			if (file2.isDirectory())
				return 1;
			else
				return file1.getPath().compareToIgnoreCase(file2.getPath());
		}
	}
}
