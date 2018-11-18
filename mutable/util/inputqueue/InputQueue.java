/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util.inputqueue;
import java.util.function.DoubleUnaryOperator;
import mutable.util.ByteState;

public interface InputQueue extends DoubleUnaryOperator, ByteState{
	
	public void add(double dt, double value);
	
	/** dt is a negative number of seconds since now.
	Returns the (linear interpolated in some implementations) value at that time.
	*/
	public double applyAsDouble(double dt);
	
	public double timeSize();

}