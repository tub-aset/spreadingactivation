package de.tuberlin.aset.spreadingactivation.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ExecutorQueue {
	private final ExecutorService executor;
	private final int maxParallelTasks;
	private final int maxFinishedTasks;

	private boolean interrupted = false;
	private int submittedTasks = 0;
	private Collection<Iterator<Runnable>> tasksQueue = new LinkedHashSet<>();
	private Collection<Future<?>> futures = new HashSet<>();

	public ExecutorQueue(ExecutorService executor, int maxSubmittedTasks, int maxFinishedTasks) {
		this.executor = executor;
		this.maxParallelTasks = maxSubmittedTasks;
		this.maxFinishedTasks = maxFinishedTasks;
	}

	public synchronized void submit(Iterator<Runnable> tasks) {
		interrupted = false;
		tasksQueue.add(tasks);

		executeNext();
	}

	public synchronized void interrupt() {
		interrupted = true;
		tasksQueue.clear();
		notify();
	}

	public boolean isInterrupted() {
		return interrupted;
	}

	public boolean hasCompleted() {
		return tasksQueue.isEmpty() && submittedTasks == 0 && futures.isEmpty();
	}

	private synchronized void executeNext() {
		while (!interrupted && submittedTasks < maxParallelTasks && futures.size() < maxFinishedTasks) {
			Iterator<Iterator<Runnable>> taskQueueIterator = tasksQueue.iterator();
			if (taskQueueIterator.hasNext()) {
				Iterator<Runnable> tasksIterator = taskQueueIterator.next();
				if (tasksIterator.hasNext()) {
					Runnable next = null;
					Exception exception = null;
					try {
						next = tasksIterator.next();
					} catch (Exception e) {
						exception = e;
					}
					if (exception != null) {
						Future<?> failed = new FailedFuture<>(exception);
						futures.add(failed);
					} else if (next == null) {
						Future<?> failed = new FailedFuture<>(new NullPointerException("runnable is null"));
						futures.add(failed);
					} else {
						RunnableTask runnableTask = new RunnableTask(this, next);
						Future<?> submit = executor.submit(runnableTask);
						futures.add(submit);
						submittedTasks++;
					}
					notify();
				} else {
					taskQueueIterator.remove();
					continue;
				}
			} else {
				break;
			}
		}
	}

	private synchronized void finishedTask(RunnableTask runnableTask) {
		submittedTasks--;
		executeNext();
	}

	public void awaitCompleted() throws InterruptedException, ExecutionException {
		while ((!interrupted && !hasCompleted()) || (interrupted && !futures.isEmpty())) {
			if (!interrupted && (executor.isShutdown() || executor.isTerminated())) {
				throw new IllegalStateException();
			}

			Future<?> future;
			synchronized (this) {
				while (futures.isEmpty()) {
					wait();
				}
				Iterator<Future<?>> iterator = futures.iterator();
				future = iterator.next();
				iterator.remove();
			}
			try {
				future.get();
			} finally {
				executeNext();
			}
		}
	}

	private static class RunnableTask implements Runnable {

		private ExecutorQueue queue;
		private Runnable runnable;

		public RunnableTask(ExecutorQueue queue, Runnable runnable) {
			this.queue = queue;
			this.runnable = runnable;
		}

		@Override
		public void run() {
			try {
				runnable.run();
			} finally {
				queue.finishedTask(this);
			}
		}

	}

	private static class FailedFuture<V> implements Future<V> {

		private boolean canceled = false;
		private Exception exception;

		public FailedFuture(Exception exception) {
			this.exception = exception;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			this.canceled = true;
			return true;
		}

		@Override
		public boolean isCancelled() {
			return canceled;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public V get() throws ExecutionException {
			throw new ExecutionException(exception);
		}

		@Override
		public V get(long timeout, TimeUnit unit) throws ExecutionException {
			throw new ExecutionException(exception);
		}

	}

}
