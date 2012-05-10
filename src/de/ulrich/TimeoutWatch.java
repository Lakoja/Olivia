package de.ulrich;

public class TimeoutWatch extends Thread {
	private LocationHandler listener;
	private boolean active;
	private int timeoutMinutes;
	
	public TimeoutWatch(LocationHandler father, int timeout) {
		listener = father;
		timeoutMinutes = timeout;
	}
	
	public void start() {
		active = true;
		super.start();
	}
	
	public void shutdown() {
		active = false;
	}
	
	public void run() {
		while (active) {
			try { Thread.sleep(timeoutMinutes * 60 * 1000); } catch (InterruptedException exc) {}
			
			if (active)
				listener.timeoutOccured();
		}
	}
}