package org.dhc.network;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.dhc.util.Callback;
import org.dhc.util.Configurator;
import org.dhc.util.Constants;
import org.dhc.util.DoubleMap;
import org.dhc.util.Message;
import org.dhc.util.SenderLinkedBlockingQueue;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;

public class SenderPool {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static SenderPool instance = new SenderPool();
	
	public static SenderPool getInstance() {
		return instance;
	}

	private ExecutorService executorService = ThreadExecutor.newFixedThreadPool(Configurator.getInstance().getIntProperty("org.dhc.network.SenderPool.size", 3), "sender-pool", 
			new SenderLinkedBlockingQueue<>(2000));
	private DoubleMap<Peer, Message, Long> currentlyExecuting = new DoubleMap<>();
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	
	private void put(Peer peer, Message message) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			currentlyExecuting.put(peer, message, System.currentTimeMillis());
		} finally {
			writeLock.unlock();
		}
	}
	
	private void remove(Peer peer, Message message) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			currentlyExecuting.remove(peer, message);
		} finally {
			writeLock.unlock();
		}
	}
	
	private void closeHangedPeers() {
		Set<Peer> peers;
		Lock readLock = readWriteLock.readLock();
		readLock.lock();
		try {
			peers = new HashSet<>(currentlyExecuting.getFirstKeys());
		} finally {
			readLock.unlock();
		}
		
		for(Peer peer : peers) {
			if(peer.isClosed()) {
				continue;
			}
			Map<Message, Long> map;
			readLock.lock();
			try {
				map = currentlyExecuting.getByFirstKey(peer);
				if(map == null) {
					continue;
				}
				map = new HashMap<>(map);
			} finally {
				readLock.unlock();
			}
			for(Message message: map.keySet()) {
				if(System.currentTimeMillis() - map.get(message) > Constants.MINUTE) {
					logger.info("Peer hanged, closing connection with {}", peer);
					logger.info("Peer hanged on message {}", message);
					String str = message.getClass().getSimpleName() + "@" + message.hashCode() + " " + peer.socketToString();
					logger.info("SenderPool hanged on message {}", str);
					peer.close();
					break;
				}
			}
		}
	}
	
	public void send(Peer peer, Message message) {

		closeHangedPeers();
		try {
			executorService.execute(new DhcRunnable("Peer sender " + peer.socketToString() + " " + message) {

				@Override
				public void doRun() {
					// ThreadExecutor.sleep(200);
					try {
						put(peer, message);
						peer.doSend(message);
					} finally {
						remove(peer, message);
					}
				}
			});
		} catch (RejectedExecutionException e) {
			logger.info(e.getMessage());
		}
	}

	public void sendWithCallback(Peer peer, Message message, Callback callBack, long timeout) {
		
		closeHangedPeers();
		executorService.execute(new DhcRunnable("Peer sender " + peer.socketToString() + " " + message) {
			
			@Override
			public void doRun() {
				//ThreadExecutor.sleep(200);
				try {
					put(peer, message);
					peer.doSendWithCallback(message, callBack, timeout);
				} finally {
					remove(peer, message);
				}
			}
		});
	}

}
