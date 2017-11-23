package com.mill.accessibility.thread;

import android.os.Process;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程优先级默认Process.THREAD_PRIORITY_BACKGROUND
 */
public class PriorityThreadFactory implements ThreadFactory {
	public static final int THREAD_PRIORITY_DEFAULT = Process.THREAD_PRIORITY_BACKGROUND;
	private int mPriority;
	private final AtomicInteger mNumber = new AtomicInteger();
	private final String mName;

	public PriorityThreadFactory(String name) {
		mName = name;
		mPriority = THREAD_PRIORITY_DEFAULT;
	}

	public PriorityThreadFactory(String name, int priority) {
		mName = name;
		mPriority = priority;
	}

	@SuppressWarnings("NullableProblems")
	@Override
	public Thread newThread(Runnable r) {
		return new Thread(r, "PTF-" + mName + '-' + mNumber.getAndIncrement()) {
			@Override
			public void run() {
				Process.setThreadPriority(mPriority);
				super.run();
			}
		};
	}

	public static Thread newThread(String name, Runnable r) {
		return newThread(name, THREAD_PRIORITY_DEFAULT, r);
	}

	public static Thread newThread(String name, final int priority, Runnable r) {
		return new Thread(r, "PTF-" + name) {
			@Override
			public void run() {
				int priorityNew = priority;
				if (priority > Process.THREAD_PRIORITY_LOWEST) {
					priorityNew = Process.THREAD_PRIORITY_LOWEST;
				} else if (priority < Process.THREAD_PRIORITY_FOREGROUND) {
					priorityNew = Process.THREAD_PRIORITY_FOREGROUND;
				}
				Process.setThreadPriority(priorityNew);
				super.run();
			}
		};
	}
}