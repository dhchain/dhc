package org.dhc.blockchain;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.dhc.util.DhcAddress;

public class MyAddresses {
	
	private List<String> list = new ArrayList<>();
	
	public MyAddresses() {
		load();
	}

	private void load() {
		
		list.add("42pKbvTuPq8XYNQpxZmHe5NVMEiUnv43FTHG"); //00 011000=24
		list.add("8aTbk8t8Mc23x2mHRskTfESGbWd9gFwCz5oo"); //01 111101=61
		list.add("4TTvUxkHiaFaxqwiYHwARYb4WLCsaAHKnri3"); //02 011100=28
		list.add("3KmxhD2twupSkTRWgtPVqg3DgQq1XqQzQJmf"); //03 010010=18
		list.add("2QL9FhY4gm6qyqHzGzHaH4SJnzxwmfWR65dt"); //04 001011=11
		list.add("7h5dEB2vPYV7cnbBsafqWSyxyoiSTroQtCxU"); //05 110110=54
		list.add("5Ro9EKDhEpNeKosmZAnX9kWkJQj7SEC42XXC"); //06 100100=36
		list.add("5EtM4Gk2S7fNnnSSzD4BxVADr9a7wtYDt2bZ"); //07 100010=34
		
	}
	
	public DhcAddress get(int i) {
		return new DhcAddress(list.get(i));
	}

	public List<String> getList() {
		return list;
	}
	
	public DhcAddress getRandomAddress() {
		int i = ThreadLocalRandom.current().nextInt(list.size());
		return get(i);
	}


}
