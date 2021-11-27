package org.dhc.util;

import java.util.concurrent.LinkedBlockingQueue;

public class ReceiverLinkedBlockingQueue<E> extends LinkedBlockingQueue<E> {

	private static final long serialVersionUID = -7637911041542507064L;
	private static final DhcLogger logger = DhcLogger.getLogger();

	public ReceiverLinkedBlockingQueue(int maxNumberOfPendingTasks) {
		super(maxNumberOfPendingTasks);
	}

	@Override
	public boolean offer(E entry) {

		boolean result = super.offer(entry);
		if (!result) {
			ReceiverRunnable runnable = (ReceiverRunnable) entry;
			logger.info("Failed to receive {}", runnable);
		}

		return true;
	}

}
