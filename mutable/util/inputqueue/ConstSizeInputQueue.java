/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util.inputqueue;
import java.util.function.DoubleUnaryOperator;
import mutable.util.LinearInterpolate1Var;
import immutable.util.MathUtil;

/** queues, and linearly interpolates between, numbers from input (such as mouseY) over a time window,
but this object only knows what input has been put in and does not call the system clock.
As a DoubleUnaryOperator, nonnegative param is time how far back (todo should that be negative?),
and return is linear interpolation between nearest 2 inputs or TODO what to return if older than oldest?
<br><br>
TODO extremely optimize: this is very slow to add(double,double) and should be optimized by using a circleQueue,
but it will be ok if its only used for a few minutes of mouse movements instead of numberCrunching,
which would require a change to (in subclass of) LinearInterpolate1Var. 
*/
public class ConstSizeInputQueue extends LinearInterpolate1Var implements InputQueue{
	
	protected double dtSum;
	public double timeSize(){ return dtSum; }
	
	public ConstSizeInputQueue(){
		super(new double[1],new double[1]);
	}
	
	public ConstSizeInputQueue(byte[] state){
		super(state);
		dtSum = in[in.length-1];
	}
	
	public void add(double dt, double value){
		if(dt <= 0) throw new Error("dt="+dt);
		dtSum += dt;
		//TODO optimize with circleQueue (bigO(1) to write, still bigO(log) to read)
		//instead of arraycopy the whole thing,
		//but for now I'm writing so infrequently (only once when mouse moves)
		//this is not a bottleneck.
		int siz = in.length;
		System.arraycopy(in, 1, in, 0, siz-1);
		System.arraycopy(out, 1, out, 0, siz-1);
		in[siz-1] = dtSum;
		out[siz-1] = value;
		setModified(true);
	}

	public double applyAsDouble(double dt){
		return interpolate(-dt+dtSum); //FIXME is this backward? 2 ways it could be backward.
	}

}