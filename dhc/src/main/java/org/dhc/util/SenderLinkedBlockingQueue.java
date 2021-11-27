package org.dhc.util;

import java.util.concurrent.LinkedBlockingQueue;

public class SenderLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {

	private static final long serialVersionUID = -7637911041542507064L;
	private static final DhcLogger logger = DhcLogger.getLogger();

	public SenderLinkedBlockingQueue(int maxNumberOfPendingTasks) {
		super(maxNumberOfPendingTasks);
	}

	@Override
	public boolean offer(E entry) {

		boolean result = super.offer(entry);
		if (!result) {
			DhcRunnable runnable = (DhcRunnable) entry;
			logger.info("Failed to send {}", runnable.getNewName());
		}

		return true;
	}

}
