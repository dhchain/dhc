package org.dhc.network;

import java.util.List;

import org.dhc.util.Constants;
import org.dhc.util.DhcLogger;
import org.dhc.util.Message;
import org.dhc.util.TAddress;

/**
 * Navigator class in DHC address space. 
 * When index = 0 then asks for closer peers to completely opposite address 
 * and with increased index getting closer to original address 
 * until search address equals original address
 * 
 * 
 * @author dhc
 */
public class NavigateMessage extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();
	
	private TAddress hash;
	private int index;

	public NavigateMessage(TAddress hash, int index) {
		this.hash = hash;
		this.index = index;
	}

	@Override
	public void process(Peer peer) {
		logger.trace("NavigateMessage - START");
		String str = String.format("NavigateMessage.process %s-%s-%s", hash, index, peer.getInetSocketAddress());
		if(alreadySent(str)) {
			return;
		}
		TAddress searchAddress = hash.xor(index);// not really xor but result gets closer to hash in xor metrics while index gets bigger. For index 32 (32 bits = 4 bytes) searchAddress == hash
		List<Peer> peers  = Peer.getClosestKPeers(searchAddress);
		Message message = new NavigateReplyMessage(peers, hash, index);
		peer.send(message);
		
	}
	
	public void send(Peer peer) {
		TAddress searchAddress = hash.xor(index);
		if(hash.equals(searchAddress)) {
			logger.trace("index {} is too large, hash==searchAddress, stop navigating", index);
			return;
		}
		String str = String.format("NavigateMessage.send %s-%s-%s", hash, index, peer.getInetSocketAddress());
		if(alreadySent(str, Constants.MINUTE)) {
			return;
		}
		logger.trace(str);
		peer.send(this);
	}

}
