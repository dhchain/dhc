package org.dhc.util;

import java.net.InetSocketAddress;
import java.security.PublicKey;
import java.util.BitSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

public class GsonUtil {
	
	private static final GsonUtil instance = new GsonUtil();
	
	private Gson gson = initializeGson();
	
	public static GsonUtil getInstance() {
		return instance;
	}
	
	private Gson initializeGson() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(PublicKey.class, new PublicKeySerialiser());
		builder.registerTypeAdapter(InetSocketAddress.class, new InetSocketAddressSerialiser());
		builder.registerTypeAdapter(BitSet.class, new BitSetSerialiser());
        Gson gson = builder.setLenient().create();
        return gson;
	}

	public Gson getGson() {
		return gson;
	}
	
	public void write(Message message, JsonWriter writer) throws Exception {
		synchronized (writer) {
			JsonClassMarker marker = new JsonClassMarker(message.getClass());
			getGson().toJson(marker, JsonClassMarker.class, writer);
			message.setTimestamp(System.currentTimeMillis());
			getGson().toJson(message, message.getClass(), writer);
			writer.flush();
		}
	}

	public Message read(JsonReader reader) {
		synchronized (reader) {
			JsonClassMarker marker = getGson().fromJson(reader, JsonClassMarker.class);
			if(marker == null) {
				return null;
			}
			Class<? extends Message> clazz = marker.getType();
			if(clazz == null) {
				return null;
			}
			Message message = getGson().fromJson(reader, clazz);
			return message;
		}
	}

}
