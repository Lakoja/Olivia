package de.ulrich;

import android.app.Activity;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.SoundPool;
import android.util.Log;

public class SoundHandler {

	private SoundPool player;
	private int notifyId;
	private int unnotifyId;
	
	public SoundHandler(Activity father) {
		player = new SoundPool(2, AudioManager.STREAM_NOTIFICATION, 0);
		notifyId = player.load(father, R.raw.clinking, 1);
		unnotifyId = player.load(father, R.raw.ting, 1);
	}
	
	public void play(boolean notify) {
		if (notify)
			player.play(notifyId, 1, 1, 0, 0, 1);
		else
			player.play(unnotifyId, 1, 1, 0, 0, 1);
	}
	
	public void shutdown() {
		player.release();
	}
}