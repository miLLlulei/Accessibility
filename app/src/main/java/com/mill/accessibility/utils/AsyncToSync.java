package com.mill.accessibility.utils;

/**
 * 异步转同步
 */
public class AsyncToSync {
	private boolean mIsPaused;
	private boolean mIsExit;

	public AsyncToSync() {
		mIsPaused = true;
	}

	public synchronized boolean isPaused() {
		return mIsPaused;
	}

	public synchronized void pause() {
		mIsPaused = true;
	}

	public synchronized void resume() {
		mIsPaused = false;
		notifyAll();
	}

	public synchronized void callWait() {
		while (mIsPaused) {
			try {
				this.wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	

	public synchronized void callWait(long millis) {
		while (mIsPaused) {
			try {
				this.wait(millis);
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				mIsPaused = false;
			}
		}
	}

	public synchronized boolean isExit() {
		return mIsExit;
	}

	public synchronized void exit() {
		mIsPaused = false;
		mIsExit = true;
		notifyAll();
	}
}