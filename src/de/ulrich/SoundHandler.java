package de.ulrich;

import android.app.Activity;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.util.Log;

public class SoundHandler implements OnCompletionListener {

	private MediaPlayer player;
	private boolean ready;
	
	public SoundHandler(Activity father, int resourceId) {
		player = MediaPlayer.create(father, resourceId);
		//prepare(); // Already done by create()
	
		if (ready)
			player.setOnCompletionListener(this);
	}

	@Override
	public void onCompletion(MediaPlayer mp) {
		prepare();
	}
	
	public void play() {
		if (ready)
			player.start();
	}
	
	public void deconstruct() {
		player.release();
		ready = false;
	}
	
	private void prepare() {
		ready = false;
		try {
			player.prepare();
		} catch (Exception exc) {
			Log.e("SoundHandler", "Media player exception "+exc);
			exc.printStackTrace();
		}
		ready = true;
	}
}
