/** Ben F Rayfield offers this software opensource MIT license */
package immutable.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil{
	
	public static byte[] sha256(byte[] b){
		try{
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(b);
			return md.digest();
		}catch(NoSuchAlgorithmException e){ throw new Error(e); }
		
	}

}
