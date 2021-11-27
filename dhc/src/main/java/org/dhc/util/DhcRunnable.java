package org.dhc.util;

public abstract class DhcRunnable implements Runnable {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private String newName;
	
	public DhcRunnable(String newName) {
		this.newName = newName;
	}

	public void run() {
		Thread currentThread = Thread.currentThread();
		String oldName = currentThread.getName();
		try {
			currentThread.setName(newName);
			doRun();
		} catch(Throwable e) {
			logger.error(e.getMessage(), e);
		} finally {
			currentThread.setName(oldName);
		}
	}

	public abstract void doRun();

	public String getNewName() {
		return newName;
	}

}
