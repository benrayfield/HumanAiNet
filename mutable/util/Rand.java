/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;
import java.security.SecureRandom;
import java.util.Random;

public class Rand{
	private Rand(){}
	
	public static final Random weakRand;
	public static final SecureRandom strongRand;
	public static final long comCostPerRandBit;
	static{
		strongRand = new SecureRandom();
		//TODO set seed as bigger byte array, more hashcodes to fill it maybe
		strongRand.setSeed(3+System.nanoTime()*49999+System.currentTimeMillis()*new Object().hashCode());
		weakRand = new Random(strongRand.nextLong());
		comCostPerRandBit = 234; //FIXME how much?
	}

}
