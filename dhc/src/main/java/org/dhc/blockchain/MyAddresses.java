package org.dhc.blockchain;

import java.util.ArrayList;
import java.util.List;

import org.dhc.util.DhcAddress;

public class MyAddresses {
	
	private List<String> list = new ArrayList<>();
	
	public MyAddresses() {
		load();
	}

	private void load() {
		
		list.add("42pKbvTuPq8XYNQpxZmHe5NVMEiUnv43FTHG"); //00 011000=24  
		
	}
	
	public DhcAddress get(int i) {
		return new DhcAddress(list.get(i));
	}

	public List<String> getList() {
		return list;
	}


}
