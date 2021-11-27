package org.dhc.util;

public class GetTotalBalanceEventListener implements EventListener {

	@Override
	public void onEvent(Event event) {
		GetBalanceEvent getBalanceEvent = (GetBalanceEvent)event;
		Registry.getInstance().getTotalBalance().updateBalance(getBalanceEvent.getAddress().toString(), getBalanceEvent.getBalance(), getBalanceEvent.getBlockhash(), getBalanceEvent.getCorrelationId());
	}

}
