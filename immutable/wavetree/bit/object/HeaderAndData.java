/** Ben F Rayfield offers this software opensource MIT license */
package immutable.wavetree.bit.object;
import immutable.wavetree.bit.Bits;

public interface HeaderAndData{
	
	public Bits data();

	/** may be empty */
	public Bits header();
	
	public Bits headerThenData();

}
