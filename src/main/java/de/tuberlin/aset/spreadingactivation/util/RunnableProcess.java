package de.tuberlin.aset.spreadingactivation.util;

public abstract class RunnableProcess implements Runnable {

	boolean started = false;
	boolean finished = false;
	boolean interrupted = false;

	public boolean isStarted() {
		return started;
	}

	public boolean isFinished() {
		return finished;
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public void interrupt() {
		interrupted = true;
	}

	protected void started() {
		if (isStarted()) {
			throw new IllegalStateException("already started");
		}
		started = true;
	}

	protected void finished() {
		if (isFinished()) {
			throw new IllegalStateException("already finished");
		}
		finished = true;
	}

}
