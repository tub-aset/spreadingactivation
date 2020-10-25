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

	protected void checkInterrupted() {
		if (isInterrupted()) {
			throw new IllegalStateException("interrupted");
		}
	}

	public void interrupt() {
		interrupted = true;
	}

	protected void started() {
		checkStarted();
		started = true;
	}

	protected void checkStarted() {
		if (isStarted()) {
			throw new IllegalStateException("already started");
		}
	}

	protected void finished() {
		checkFinished();
		finished = true;
	}

	protected void checkFinished() {
		if (isFinished()) {
			throw new IllegalStateException("already finished");
		}
	}

}
