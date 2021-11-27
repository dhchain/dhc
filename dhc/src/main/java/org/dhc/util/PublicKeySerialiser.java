package org.dhc.util;

import java.lang.reflect.Type;
import java.security.GeneralSecurityException;
import java.security.PublicKey;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

public class PublicKeySerialiser implements JsonSerializer<PublicKey>, JsonDeserializer<PublicKey> {
	
	private static final DhcLogger logger = DhcLogger.getLogger();

	public JsonElement serialize(PublicKey publicKey, Type type, JsonSerializationContext context) {
		JsonObject jsonObject = new JsonObject();
	    jsonObject.addProperty("encoded", Base58.encode(publicKey.getEncoded()));
	    return jsonObject;
	}

	public PublicKey deserialize(JsonElement json, Type type, JsonDeserializationContext context) throws JsonParseException {
		JsonObject jsonObject = json.getAsJsonObject();

	    JsonElement jsonEncoded = jsonObject.get("encoded");
	    String encoded = jsonEncoded.getAsString();
	    PublicKey publicKey = null;
	    try {
	    	publicKey = CryptoUtil.loadPublicKey(encoded);
		} catch (GeneralSecurityException e) {
			logger.error(e.getMessage(), e);
		}
	    
		return publicKey;
	}

}
