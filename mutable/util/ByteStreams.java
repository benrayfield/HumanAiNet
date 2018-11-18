/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;

import java.io.IOException;
import java.io.InputStream;

public class ByteStreams{
	private ByteStreams(){}
	
	

	public static byte[] bytes(InputStream in){
		System.out.println("Reading "+in);
		try{
			byte b[] = new byte[1];
			int avail;
			int totalBytesRead = 0;
			while((avail = in.available()) != 0){
				int maxInstantCapacityNeeded = totalBytesRead+avail;
				if(b.length < maxInstantCapacityNeeded){
					byte b2[] = new byte[maxInstantCapacityNeeded*2];
					System.arraycopy(b, 0, b2, 0, totalBytesRead);
					b = b2;
				}
				//System.out.println("totalBytesRead="+totalBytesRead+" avail="+avail);
				int instantBytesRead = in.read(b, totalBytesRead, avail);
				if(instantBytesRead > 0) totalBytesRead += instantBytesRead; //last is -1
			}
			byte b2[] = new byte[totalBytesRead];
			System.arraycopy(b, 0, b2, 0, totalBytesRead);
			return b2;
		}catch(IOException e){
			throw new RuntimeException(e);
		}
	}
	
	

}
