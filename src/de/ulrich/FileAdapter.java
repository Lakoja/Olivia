package de.ulrich;

import java.io.File;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class FileAdapter extends ArrayAdapter<File> {
	public FileAdapter(Context context, int textViewResourceId, List<File> items) {
		super(context, textViewResourceId, items);
		//resourceId = textViewResourceId;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = convertView;
		if (view == null) {
			LayoutInflater vi = (LayoutInflater) getContext().getSystemService(
					Context.LAYOUT_INFLATER_SERVICE);
			view = vi.inflate(R.layout.filerow, null); // TODO
			
			WindowHolder holder = new WindowHolder();
			holder.image = (ImageView)view.findViewById(R.id.type);
			holder.text = (TextView)view.findViewById(R.id.name);
			view.setTag(holder);
		}
		File entry = getItem(position);
		
		if (entry != null) {
			WindowHolder holder = (WindowHolder)view.getTag();
			
			if (entry.isFile())
				holder.image.setImageResource(R.drawable.ic_list_disk);
			else
				holder.image.setImageResource(R.drawable.ic_list_dir);
			
			if (position != 0)
				holder.text.setText(entry.getName());
			else
				holder.text.setText(entry.getAbsolutePath());
		}

		return view;
	}
	
	public void addAll(Collection<File> collection) {
		for (File f : collection)
			add(f);
	}
		
	private class WindowHolder {
		public ImageView image;
		public TextView text;
	}
}
