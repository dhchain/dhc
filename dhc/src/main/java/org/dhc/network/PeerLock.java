package org.dhc.network;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.dhc.util.Message;
import org.dhc.util.DhcLogger;

public class PeerLock extends ReentrantLock {

	private static final long serialVersionUID = -6254166571873655860L;
	private static final DhcLogger logger = DhcLogger.getLogger();

	private Message request;
	private Message response;
	private long timeout;
	private Peer peer;
	private boolean canceled;
	private Condition condition = newCondition();

	public PeerLock(long timeout, Message request, Peer peer) {
		this.request = request;
		this.timeout = timeout;
		this.peer = peer;
	}

	public Message getResponse() {
		long time = System.currentTimeMillis();
		lock();
		try {
			if (response != null) {
				return response;
			}
			try {
				if(!canceled) {
					condition.await(timeout, TimeUnit.MILLISECONDS);
				}
			} catch (InterruptedException e) {
				logger.error(e.getMessage(), e);
			}
		} finally {
			unlock();
		}

		time = System.currentTimeMillis() - time;
		if (time >= timeout) {
			logger.info("Wait time greater than " + timeout + " ms for peer {} message={}", peer, request);
			peer.close();
		}
		return response;
	}

	public void setResponse(Message response) {
		lock();
		try {
			this.response = response;
			condition.signal();
		} finally {
			unlock();
		}
	}

	public void cancel() {
		canceled = true;
		if(tryLock()) {// There is still a possibility that another thread is about to execute "condition.await(timeout, TimeUnit.MILLISECONDS);" 
			           // then tryLock will fail and that thread have to wait until timeout. But at least there is no possibility of a deadlock on calling cancel()
			try {
				condition.signal();
			} finally {
				unlock();
			}
		}
	}

}
