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

import org.dhc.util.BoundedMap;
import org.dhc.util.Configurator;
import org.dhc.util.Constants;
import org.dhc.util.DoubleMap;
import org.dhc.util.Message;
import org.dhc.util.ReceiverLinkedBlockingQueue;
import org.dhc.util.ReceiverRunnable;
import org.dhc.util.ThreadExecutor;
import org.dhc.util.DhcLogger;

public class ReceiverPool {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	private static ReceiverPool instance = new ReceiverPool();
	
	private ExecutorService executorService = ThreadExecutor.newFixedThreadPool(Configurator.getInstance().getIntProperty("org.dhc.network.ReceiverPool.size", 3), 
			"receiver-pool", new ReceiverLinkedBlockingQueue<>(500));
	private DoubleMap<Peer, Message, Long> currentlyExecuting = new DoubleMap<>();
	private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	private final BoundedMap<String, String> alreadyReceivedMessages = new BoundedMap<>(100);
	
	public static ReceiverPool getInstance() {
		return instance;
	}

	public void put(Peer peer, Message message) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			currentlyExecuting.put(peer, message, System.currentTimeMillis());
		} finally {
			writeLock.unlock();
		}
	}
	
	public void remove(Peer peer, Message message) {
		Lock writeLock = readWriteLock.writeLock();
		writeLock.lock();
		try {
			currentlyExecuting.remove(peer, message);
		} finally {
			writeLock.unlock();
		}
	}
	
	private void listHangedMessages() {
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
					logger.info("\n");
					logger.info("ReceiverPool timed out processing message from peer {}", peer);
					logger.info("ReceiverPool hanged processing message {}", message);
					String str = message.getClass().getSimpleName() + "@" + message.hashCode() + " " + peer.socketToString();
					logger.info("ReceiverPool hanged on message {}", str);
					//peer.close();
					//break;
				}
			}
		}
	}
	
	public void process(Peer peer, Message message) {

		listHangedMessages();
		String key = message.toString();
		if(alreadyReceivedMessages.containsKey(key)) {
			return;
		}
		alreadyReceivedMessages.put(key, key);
		try {
			executorService.execute(new ReceiverRunnable(peer, message, this));
		} catch (RejectedExecutionException e) {
			logger.info(e.getMessage());
		}
	}


}
