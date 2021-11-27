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
		list.add("8CKb8VBb6UPqGi82HU38PTb8t57D8LbzoRNL"); //08 111010=58
		list.add("5h3VoBzNhQNLU6C7gca4CxFGLnHCyAK1r9s4"); //09 100110=38
		list.add("5NuNfEjAamjAsnq7B96ksnTCkron9r8N5qt3"); //10 100011=35
		list.add("qdPqmx4Kcqu6eD1peeJvozJjouShLReRKpZ");  //11 000110=06
		list.add("79ww1hdAnPa6n2afAGNRUUZaN7FSxNNGBWZz"); //12 110010=50
		list.add("UMtXqZ8NVqjMQfjrHsjFb9a5dg9Ndi4AUSo");  //13 000011=03
		list.add("28Qbu42bS8mBSJGw3LHjUAnwCoMi48kmC1hh"); //14 001001=09
		list.add("5XnMGo1iFray736QndgHcrewigNp3SeYvcw5"); //15 100100=36
		list.add("2tgjGiFyAZubDrJzxfpWESFUjG97ARRPkYff"); //16 001111=15
		list.add("7EMRFEnf4R8PK6z4SxByCRwdnwEUaLgS6sEo"); //17 110010=50
		list.add("6HuC4vvrSsKSXr5kGE9cNreLYqhL3wFc7vnK"); //18 101011=43
		list.add("4tCCCq6Xk4N4qRSnZUq5W5MyVu96TRcmJH95"); //19 011111=31
		list.add("2ZCDacRpD8MKaEQyWgUYAKxABzUD226tzwgf"); //20 001100=12
		list.add("6ZmZL2SWX2CUQAHsdfGp7HY3o3Uqynn95wyw"); //21 101101=45
		list.add("6Dk4HZVa8XNwFFxeWxidzkCr7AoiWdrF1zqD"); //22 101010=42
		list.add("xoRaDL6CmW6infk3iYtxswTu788f7AgY5i9");  //23 000111=07
		list.add("4Y14CJWuKNWM4R7FAbBic52aGvTSCdo2QF1P"); //24 011100=28
		list.add("8U6NYkqitEvgJMB4HZNN4e2WA8KLrEA8CoCu"); //25 111100=60
		list.add("81LkBF1oG6kdbBRBkjmH9fgthT5UD8TN8k3k"); //26 111001=57
		
		list.add("3o4w4Ho7tjZqXVTXSeft2ZFQBW3yK4tmn5s9"); //27 010110=22
		list.add("3K772aQcqVURu1W4LdyY2yysh4L621Zi4oLt"); //28 010010=18
		list.add("4VBJuZ7mzGH8nBkqA1QuB6wr9MU2UmY61T1");  //29 000000=00
		list.add("4KbsTYcPghG26wzARh8SEALAvUKN8BKP93UD"); //30 011011=27
		list.add("12ZoXjR2SmpoEnUqKZ4hE2T8UVkTcK9eJ5WA"); //31 000000=00
		list.add("8mmAdEfJ4pHtkTS9YLM2m5cvRrbhxXyPwcqN"); //32 111111=63
		list.add("7SkvqPwai2YT2MkFenyU5kr4pNJYRaWKpSvv"); //33 110100=52
		list.add("64833EZBTXCxeheo137ZD4fi3sBvDSBmCb4X"); //34 101001=41
		list.add("6n4ViKXynMwjfcwjU2SPDQrTatygkMWXP5kj"); //35 101111=47
		list.add("JrgN5yxkqAcxoGduM5BgHjLWXWF7tJdxwCo");  //36 000010=02
		list.add("3CTjGtrWnR6R2MKBx86WgnUeUugaNN6ZU6bN"); //37 010001=17
		list.add("5xKYidMNWWuWqwX1pPwoFYyPNxUQvnmG2Fz4"); //38 101000=40
		list.add("3csUtgb8YLkHzQQP9kkJDYy9udzcF9Ymidvw"); //39 010101=21
		list.add("jEBJGwJxXm3tTbejkCpZ5P5hvQVf9raYyhq");  //40 000101=05
		list.add("2rnY965jqVtVgdAMkQyB6W97NxZi3YspL1tY"); //41 001111=15
		list.add("3p2fPcb3Bv1kYQobzK61PvRf1jnYJWguBsaJ"); //42 010110=22
		list.add("4KuX7CNctavi7qANi2jAtjkJ13TpDnFGfqsZ"); //43 011011=27
		list.add("5DSLcaFBkYbFjt4UHNT1uHiJY7mTjqy6gXEm"); //44 100010=34
		list.add("57BG7m4BEZjyfyRrjy1ea6W81XLavLjpRNjN"); //45 100001=33
		list.add("72ACQ4L3uAex9XhU7VKSSLvD2eJyW7mmc634"); //46 110001=49
		list.add("6zkyzauXhYjPb4Vb8BmZTRGKCHDJ1VpgnfQY"); //47 110000=48
		list.add("7zE2K7KCcpKVqUHx9xNZQUXbHXjQbUjK2vcg"); //48 111001=57
		list.add("8eb3PgFZdeFUCtx8D1WQgV2qNVMvMzZYqnqo"); //49 111110=62
		
	}
	
	public DhcAddress getRandomAddress() {
		int i = ThreadLocalRandom.current().nextInt(list.size());
		return get(i);
	}
	
	public DhcAddress get(int i) {
		return new DhcAddress(list.get(i));
	}

	public List<String> getList() {
		return list;
	}


}
