package net.autocrm.api.util;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;

public class WdmsEncryption {
	
	private final String vector = "!QAZ2WSX#EDC4RFV"; 
	private final String secretKey = "5TGB&YHN7UJM(IK<5TG111HN8UJM(111"; 
	 
		//암호화
		public String AES_Encode(String str) throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
			byte[] keyData = secretKey.getBytes();
			SecretKey secureKey = new SecretKeySpec(keyData, "AES");
			Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
			c.init(Cipher.ENCRYPT_MODE, secureKey, new IvParameterSpec(vector .getBytes()));
		
			byte[] encrypted = c.doFinal(str.getBytes("UTF-8"));
			String enStr = new String(Base64.encodeBase64(encrypted));
		
			return enStr;
		}
		
		//복호화
		public String AES_Decode(String str) throws java.io.UnsupportedEncodingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException{
			byte[] keyData = secretKey.getBytes();
			SecretKey secureKey = new SecretKeySpec(keyData, "AES");
		 	Cipher c = Cipher.getInstance("AES/CBC/PKCS5Padding");
		 	c.init(Cipher.DECRYPT_MODE, secureKey, new IvParameterSpec(vector .getBytes("UTF-8")));
		
		 	byte[] byteStr = Base64.decodeBase64(str.getBytes());
		
		 	return new String(c.doFinal(byteStr),"UTF-8");
		}
}
