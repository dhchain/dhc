package org.dhc.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadExecutor {

	private static final DhcLogger logger = DhcLogger.getLogger();
	private static final ThreadExecutor instance = new ThreadExecutor();

	private final ExecutorService exService = newCachedThreadPool(Configurator.getInstance().getIntProperty("org.dhc.util.ThreadExecutor.maxthreadnumber", 500));
	private final ScheduledExecutorService scheduledExecutor = newScheduledThreadPool(5);

	public static ThreadExecutor getInstance() {
		return instance;
	}

	public void execute(Runnable runnable) {
		exService.submit(runnable);
	}

	public ScheduledFuture<?> schedule(Runnable runnable, long delayInMilliseconds) {
		int queueSize = ((ScheduledThreadPoolExecutor)scheduledExecutor).getQueue().size();
		if(queueSize > 10000) {
			throw new RuntimeException("scheduledExecutor queue size is " + queueSize);
		}
		ScheduledFuture<?> scheduledFuture = scheduledExecutor.schedule(runnable, delayInMilliseconds, TimeUnit.MILLISECONDS);
		return scheduledFuture;
	}

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {

		}
	}

	public static void logPid(int port) {
		try {
			String jvmName = ManagementFactory.getRuntimeMXBean().getName();
			logger.debug("jvmName={}", jvmName);
			File file = new File("./log/" + port + ".txt");
			file.getParentFile().mkdirs();
			if (!file.exists()) {
				file.createNewFile();
			}
			OutputStream os = new FileOutputStream(file);
			Writer w = new OutputStreamWriter(os, StandardCharsets.UTF_8);
			w.write(jvmName);
			w.flush();
			w.close();
			os.close();
		} catch (Exception e) {
			logger.debug(e.getMessage());
		}
	}

	public static ExecutorService newCachedThreadPool(int maxPoolSize) {
		DhcThreadFactory factory = new DhcThreadFactory("cached-pool");
		return new ThreadPoolExecutor(0, maxPoolSize, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), factory);
	}

	public static ScheduledExecutorService newScheduledThreadPool(int maxPoolSize) {
		DhcThreadFactory factory = new DhcThreadFactory("scheduled-pool");
		ScheduledThreadPoolExecutor service = new ScheduledThreadPoolExecutor(maxPoolSize, factory);
		service.setKeepAliveTime(60L, TimeUnit.SECONDS);
		service.allowCoreThreadTimeOut(true);
		return service;
	}

	public static ExecutorService newFixedThreadPool(int maxPoolSize, String poolName, BlockingQueue<Runnable> blockingQueue) {
		DhcThreadFactory factory = new DhcThreadFactory(poolName);
		ThreadPoolExecutor service = new ThreadPoolExecutor(maxPoolSize, maxPoolSize, 60L, TimeUnit.SECONDS, blockingQueue, factory);
		service.allowCoreThreadTimeOut(true);
		return service;
	}

}
