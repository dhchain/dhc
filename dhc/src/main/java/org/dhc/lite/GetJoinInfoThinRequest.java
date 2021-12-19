package org.dhc.lite;

import org.dhc.gui.promote.GetJoinInfoRequest;
import org.dhc.gui.promote.JoinInfoEvent;
import org.dhc.network.Network;
import org.dhc.network.Peer;
import org.dhc.util.Coin;
import org.dhc.util.DhcAddress;
import org.dhc.util.DhcLogger;
import org.dhc.util.DhcRunnable;
import org.dhc.util.Listeners;
import org.dhc.util.Message;
import org.dhc.util.ThreadExecutor;

public class GetJoinInfoThinRequest extends Message {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	private DhcAddress myDhcAddress;
	private DhcAddress address;
	private Coin amount;

	public GetJoinInfoThinRequest(DhcAddress myDhcAddress, DhcAddress address, Coin amount) {
		this.myDhcAddress = myDhcAddress;
		this.address = address;
		this.amount = amount;
		
	}

	@Override
	public void process(Peer peer) {
		logger.info("GetJoinInfoThinRequest.process myDhcAddress={}, address={}, amount={}", myDhcAddress, address, amount);
		ThreadExecutor.getInstance().execute(new DhcRunnable("GetJoinInfoThinRequest") {
			public void doRun() {
				getJoinInfo(peer);
			}
		});
	}
	
	private void getJoinInfo(Peer peer) {
		Network network = Network.getInstance();
		Message message = new GetJoinInfoRequest(DhcAddress.getMyDhcAddress(), address, amount);
		message.setCorrelationId(getCorrelationId());
		Listeners.getInstance().addEventListener(JoinInfoEvent.class, new JoinInfoThinEventListener(myDhcAddress, getCorrelationId(), peer));
		network.sendToAddress(address, message);
	}

	public DhcAddress getMyDhcAddress() {
		return myDhcAddress;
	}

	public DhcAddress getAddress() {
		return address;
	}

	public Coin getAmount() {
		return amount;
	}


}
