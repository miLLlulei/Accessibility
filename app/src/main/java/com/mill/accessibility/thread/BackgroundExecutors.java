package com.mill.accessibility.thread;

import android.os.Build;
import android.os.Process;
import android.os.SystemClock;

import com.mill.accessibility.utils.LogUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 后台单线程执行器：
 * 1、只有一个线程用于执行任务，适用于执行用户无感知的后台任务；
 * 2、对定时时间要求不严格的任务，使用static公用的{@link #getGlobalExecutor}；
 * 3、对定时时间要求严格的任务，使用新创建的单独执行器{@link #newSingleThreadScheduledExecutor}，因为{@link BackgroundScheduledThreadPoolExecutor}是单线程的，可能正在被占用执行其他任务；
 * <p/>
 * 支持功能包括：
 * 1、立刻执行
 * 2、延迟执行
 * 3、循环执行
 * 4、是否在执行中
 * 5、取消任务
 * 6、打印每个任务的时间
 */
public class BackgroundExecutors {
    private static final String TAG = "BackgroundExecutors";
    private static volatile BackgroundScheduledThreadPoolExecutor sBackgroundExecutor;

    /**
     * 获取一个全局static的后台单线程执行器
     *
     * @return
     */
    public static BackgroundScheduledThreadPoolExecutor getGlobalExecutor() {
        if (sBackgroundExecutor == null) {
            synchronized (BackgroundExecutors.class) {
                if (sBackgroundExecutor == null) {
                    sBackgroundExecutor = newSingleThreadScheduledExecutor();
                }
            }
        }
        return sBackgroundExecutor;
    }

    /**
     * 创建一个独立的单线程执行器，如：对定时执行时间要求严格的任务不能使用{@link BackgroundExecutors#getGlobalExecutor}，有可能被阻塞；
     *
     * @return
     */
    public static BackgroundScheduledThreadPoolExecutor newSingleThreadScheduledExecutor() {
        return new BackgroundScheduledThreadPoolExecutor();
    }

    public static class BackgroundScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor {
        private volatile Map<RunnableScheduledFuture<?>, Runnable> mTasks;
        private volatile Queue<Integer> mRunningTasks;
        private Map<Integer, Long> mDebugTimes;

        public BackgroundScheduledThreadPoolExecutor() {
            super(1, new PriorityThreadFactory(TAG, Process.THREAD_PRIORITY_BACKGROUND));
            setMaximumPoolSize(1);
            setKeepAliveTime(10 * 1000L, TimeUnit.MILLISECONDS);
            allowCoreThreadTimeOut(true);
        }

        /**
         * 立刻执行任务
         * {@link android.os.Handler#post(Runnable)}
         *
         * @param task
         * @return
         */
        public void post(Runnable task) {
            postDelayed(task, 0);
        }

        /**
         * 延迟执行任务
         * {@link android.os.Handler#postDelayed(Runnable, long)}
         *
         * @param task
         * @param delayMillis 执行任务前的延迟时间，单位是毫秒
         * @return
         */
        public void postDelayed(Runnable task, long delayMillis) {
            schedule(task, delayMillis, TimeUnit.MILLISECONDS);
        }

        /**
         * 循环执行任务
         * {@link java.util.Timer#schedule(java.util.TimerTask, long, long)}
         *
         * @param task 所要安排的任务
         * @param delayMillis 执行任务前的延迟时间，单位是毫秒
         * @param periodMillis  执行各后续任务之间的时间间隔，单位是毫秒
         * @return
         */
        public void schedule(Runnable task, long delayMillis, long periodMillis) {
            scheduleWithFixedDelay(task, delayMillis, periodMillis, TimeUnit.MILLISECONDS);
        }

        /**
         * 获取所有任务
         *
         * @return
         */
        public Collection<Runnable> getTasks() {
            return mTasks != null ? mTasks.values() : new ArrayList<Runnable>();
        }

        /**
         * 任务是否在执行中
         *
         * @param task
         * @return
         */
        public boolean isRunning(Runnable task) {
            return mRunningTasks != null && mRunningTasks.contains(task.hashCode());
        }

        /**
         * 任务是否正在被执行或者等待被执行
         *
         * @param task
         * @return
         */
        public boolean contains(Runnable task) {
            return isRunning(task) || getQueue().contains(task);
        }

        /**
         * @deprecated 使用{@link #cancel(Runnable)} 代替{@link #remove(Runnable)}
         * 因为参数task是{@link RunnableScheduledFuture}而不是传入的Runnable
         *
         * @param task
         * @return
         */
        @Deprecated
        @Override
        public boolean remove(Runnable task) {
            return super.remove(task);
        }

        /**
         * 取消执行任务并从待执行队列删除
         *
         * @param task
         * @return
         */
        public boolean cancel(Runnable task) {
            boolean isIncludeTask = false;
            boolean isIncludeCancelFailed = false;
            if (mTasks != null) {
                for (Map.Entry<RunnableScheduledFuture<?>, Runnable> entry : mTasks.entrySet()) {
                    if (entry.getValue().equals(task)) {
                        isIncludeTask = true;
                        RunnableScheduledFuture<?> future = entry.getKey();
                        boolean result = future.cancel(true);
                        result &= remove(future);
                        result &= mTasks.remove(future) != null ;
                        if (mRunningTasks != null) {
                            mRunningTasks.remove(task.hashCode());
                        }
                        if (mDebugTimes != null) {
                            mDebugTimes.remove(future.hashCode());
                        }
                        if (LogUtils.isDebug()) {
                            LogUtils.d(TAG, "cancel.task = " + task + ", future = " + future + ", result = " + result);
                        }
                        if (!result && !isIncludeCancelFailed) {
                            isIncludeCancelFailed = true;
                        }
                    }
                }
            }
            boolean result = isIncludeTask && !isIncludeCancelFailed;
            if (LogUtils.isDebug()) {
                LogUtils.d(TAG, "cancel.task = " + task + ", isCanceled = " + result);
            }
            return result;
        }

        @Override
        protected <V> RunnableScheduledFuture<V> decorateTask(Runnable runnable, RunnableScheduledFuture<V> task) {
            RunnableScheduledFuture<V> future = super.decorateTask(runnable, task);
            if (mTasks == null) {
                synchronized (BackgroundScheduledThreadPoolExecutor.class) {
                    if (mTasks == null) {
                        mTasks = new ConcurrentHashMap<>();
                    }
                }
            }
            mTasks.put(future, runnable);
            return future;
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            if (r instanceof RunnableScheduledFuture) {
                RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) r;
                Runnable task = mTasks.get(future);
                if (future.isCancelled() || task == null) { // 当Runnable在外部线程cancel时（mTasks.get(future)为空），beforeExecute有可能还会被执行；
                    if (LogUtils.isDebug()) {
                        LogUtils.d(TAG, "beforeExecute.isCancelled.futrue = " + future + ", throwable = " + t);
                    }
                } else {
                    if (mRunningTasks == null) {
                        synchronized (BackgroundScheduledThreadPoolExecutor.class) {
                            if (mRunningTasks == null) {
                                mRunningTasks = new ConcurrentLinkedQueue();
                            }
                        }
                    }
                    mRunningTasks.add(task.hashCode());
                    if (LogUtils.isDebug()) {
                        if (mDebugTimes == null) {
                            synchronized (BackgroundScheduledThreadPoolExecutor.class) {
                                if (mDebugTimes == null) {
                                    mDebugTimes = new ConcurrentHashMap<>();
                                }
                            }
                        }
                        mDebugTimes.put(future.hashCode(), SystemClock.elapsedRealtime());
                        LogUtils.d(TAG, "beforeExecute.task = " + task
                                + ", sBackgroundExecutor = " + this);
                    }
                }
            }
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            super.afterExecute(r, t);
            if (r instanceof RunnableScheduledFuture) {
                RunnableScheduledFuture<?> future = (RunnableScheduledFuture<?>) r;
                Runnable task = mTasks.get(future);
                if (future.isCancelled() || task == null) { // 当Runnable在run里面cancel自己时还会执行afterExecute方法；
                    if (LogUtils.isDebug()) {
                        LogUtils.d(TAG, "afterExecute.isCancelled.futrue = " + r + ", throwable = " + t);
                    }
                } else {
                    int futureHashCode = future.hashCode();
                    if (LogUtils.isDebug() && mDebugTimes != null) { // LogUtils.isDebug()是动态设置的，有可能在beforeExecute里为false，在afterExecute里为true了；
                        LogUtils.d(TAG, "afterExecute.task = " + task
                                + ", time = " + (SystemClock.elapsedRealtime() - mDebugTimes.get(futureHashCode))
                                + ", throwable = " + t
                                + ", sBackgroundExecutor = " + this);
                        if (!future.isPeriodic()) {
                            mDebugTimes.remove(futureHashCode);
                        }
                    }
                    if (!future.isPeriodic()) {
                        mRunningTasks.remove(task.hashCode());
                        mTasks.remove(future);
                    }
                }
            }
            checkAndThrowThreadPoolExecutorThrowable(TAG + ".afterExecute", r, t);
        }

        @Override
        public void shutdown() {
            clear();
            super.shutdown();
        }

        @Override
        public List<Runnable> shutdownNow() {
            clear();
            return super.shutdownNow();
        }

        private void clear() {
            if (mDebugTimes != null) {
                mDebugTimes.clear();
            }
            if (mRunningTasks != null) {
                mRunningTasks.clear();
            }
            if (mTasks != null) {
                mTasks.clear();
            }
        }

        /**
         * 检查并抛出：线程中runnable.run()执行时未捕获的异常
         * 放在{@link java.util.concurrent.ThreadPoolExecutor#afterExecute(Runnable, Throwable)}方法的最后调用
         *
         * @param tag
         * @param r
         * @param t
         */
        private void checkAndThrowThreadPoolExecutorThrowable(String tag, Runnable r, Throwable t) {
            Throwable tr = null;
            if (t != null) {
                tr = t;
            } else {
                // 当向ScheduledThreadPoolExecutor执行scheduleAtFixedRate或者scheduleWithFixedDelay时，
                // 使用future.get()捕获Runnable的异常，会导致整个线程池队列阻塞
                if (r instanceof RunnableScheduledFuture && ((RunnableScheduledFuture) r).isPeriodic()) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) { // Android 4.2
                        try {
                            Field syncField = FutureTask.class.getDeclaredField("sync");
                            syncField.setAccessible(true);
                            Object sync = syncField.get(r);
                            Field exceptionField = sync.getClass().getDeclaredField("exception");
                            exceptionField.setAccessible(true);
                            Throwable throwable = (Throwable) exceptionField.get(sync);
                            if (throwable != null) {
                                tr = new ExecutionException(throwable);
                            }
                        } catch (NoSuchFieldException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        try {
                            Field stateField = FutureTask.class.getDeclaredField("state");
                            stateField.setAccessible(true);
                            int state = (int) stateField.get(r);
                            Field EXCEPTIONALField = FutureTask.class.getDeclaredField("EXCEPTIONAL"); // private static final int EXCEPTIONAL  = 3;
                            EXCEPTIONALField.setAccessible(true);
                            int EXCEPTIONAL = (int) EXCEPTIONALField.get(null);
                            if (state == EXCEPTIONAL) {
                                Field outcomeField = FutureTask.class.getDeclaredField("outcome");
                                outcomeField.setAccessible(true);
                                tr = new ExecutionException((Throwable) outcomeField.get(r));
                            }
                        }/* catch (NoSuchFieldException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }*/ catch(Throwable e) {
                            // 特别奇怪的崩溃，Android SDK版本号对不上，samsung SM-W2014 4.3(16)、vivo Y11 4.4.4(16)
//                            java.lang.RuntimeException: java.lang.NoSuchFieldException: state
//                            at com.qihoo.utils.thread.BackgroundExecutors$BackgroundScheduledThreadPoolExecutor.a(AppStore:313)
//                            at com.qihoo.utils.thread.BackgroundExecutors$BackgroundScheduledThreadPoolExecutor.afterExecute(AppStore:240)
//                            at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1084)
//                            at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:569)
//                            at java.lang.Thread.run(Thread.java:856)
//                            at com.qihoo.utils.thread.PriorityThreadFactory$1.run(AppStore:34)
//                            Caused by: java.lang.NoSuchFieldException: state
//                            at java.lang.Class.getDeclaredField(Class.java:631)
//                            at com.qihoo.utils.thread.BackgroundExecutors$BackgroundScheduledThreadPoolExecutor.a(AppStore:301)
                            String msg = "sdkInt = " + Build.VERSION.SDK_INT + ", release = " + Build.VERSION.RELEASE + ", fields = " + Arrays.toString(FutureTask.class.getDeclaredFields());
                        }
                    }
                } else if (r instanceof Future) {
                    try {
                        ((Future<?>) r).get();
                    } catch (InterruptedException | CancellationException | ExecutionException e) {
                        tr = e;
                    }
                }
            }
            if (tr != null) {
                if (tr instanceof ExecutionException) {
                    throw new CheckAndThrowThreadPoolExecutorException(tag, tr);
                } else {
                    if (LogUtils.isDebug()) {
                        LogUtils.d(tag, "checkAndThrowThreadPoolExecutorThrowable", tr);
                    }
                }
            }
        }
    }

    public static class CheckAndThrowThreadPoolExecutorException extends RuntimeException {
        private Throwable mOriginal;

        public CheckAndThrowThreadPoolExecutorException(String detailMessage, Throwable throwable) {
            super(detailMessage, throwable);
            if (throwable instanceof ExecutionException) {
                mOriginal = throwable.getCause();
            }
        }

        public Throwable getOriginal() {
            return mOriginal;
        }
    }
}