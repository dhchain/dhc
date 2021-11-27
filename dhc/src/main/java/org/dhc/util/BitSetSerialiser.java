package org.dhc.util;

import java.lang.reflect.Type;
import java.util.BitSet;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class BitSetSerialiser implements JsonSerializer<BitSet>, JsonDeserializer<BitSet> {

	public JsonElement serialize(BitSet bitset, Type type, JsonSerializationContext context) {
		JsonObject jsonObject = new JsonObject();
	    jsonObject.addProperty("bitset", StringUtil.toHexString((bitset.toByteArray())));
	    return jsonObject;
	}

	public BitSet deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();
	    JsonElement jsonHost = jsonObject.get("bitset");
	    String bitset = jsonHost.getAsString();
		return BitSet.valueOf(StringUtil.fromHexString(bitset));
	}

}
