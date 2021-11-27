package org.dhc.util;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class InetSocketAddressSerialiser implements JsonSerializer<InetSocketAddress>, JsonDeserializer<InetSocketAddress> {

	public JsonElement serialize(InetSocketAddress inetSocketAddress, Type type, JsonSerializationContext context) {
		JsonObject jsonObject = new JsonObject();
	    jsonObject.addProperty("host", inetSocketAddress.getHostName());
	    jsonObject.addProperty("port", inetSocketAddress.getPort());
	    return jsonObject;
	}

	public InetSocketAddress deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();
	    JsonElement jsonHost = jsonObject.get("host");
	    String host = jsonHost.getAsString();
	    JsonElement jsonPort = jsonObject.get("port");
	    int port = jsonPort.getAsInt();
		return new InetSocketAddress(host, port);
	}

}
