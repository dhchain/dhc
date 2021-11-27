package org.dhc.util;

import org.dhc.network.Peer;
import org.dhc.network.ReceiverPool;

public class ReceiverRunnable extends DhcRunnable {

	private Message message;
	private Peer peer;
	private ReceiverPool pool;

	public ReceiverRunnable(Peer peer, Message message, ReceiverPool pool) {
		super(message.getClass().getSimpleName() + "@" + message.hashCode() + " " + peer.socketToString());
		this.peer = peer;
		this.message = message;
		this.pool = pool;
	}

	@Override
	public void doRun() {
		try {
			pool.put(peer, message);
			peer.process(message);
		} finally {
			pool.remove(peer, message);
			pool = null;
			peer = null;
			message = null;
		}
	}

	@Override
	public String toString() {
		String result = getNewName() + " message " + message;
		return result;
	}

}
