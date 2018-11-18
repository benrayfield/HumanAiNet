/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;
import immutable.util.MathUtil;
import mutable.util.ui.obvar.ObvarEditor;

//import util.ui.obvar.ObvarEditor;
//import util.ui.obvar.ObvarEditorWrapper;

/* An object that has an interpolate(x) function that returns y,
where y is 1 of the outputNumbers if x is 1 of the inputNumbers,
or y is between 2 adjacent outputNumbers if x is between 2 adjacent inputNumbers.
inputNumbers must be sorted ascending, but outputNumbers can be any numbers.
The arrays must be the same size.
Does not modify or copy the arrays. Uses them directly.
If interpolate(x) and x < inputNumbers[0] || inputNumbers[inputNumbers.length-1] < x,
then that last number is used.
*/
public class LinearInterpolate1Var implements ByteState{
	
	public final double in[], out[];
	
	/** default false, unless caller changes it (and not allowed to change it back from immutable to mutable) */
	public boolean isImmutableDeep = false;
	
	public LinearInterpolate1Var(byte[] state){
		if((state.length&15)!=0) throw new Error(state.length+" bytes is not divisible by 16");
		double[] alternateInOut = MathUtil.bytesToDoubles(state);
		in = MathUtil.evens(alternateInOut);
		out = MathUtil.odds(alternateInOut);
	}
	
	public LinearInterpolate1Var(double inputNumbers[], double outputNumbers[]){ //TODO swap parameter order since outputs are normally first
		this.in = inputNumbers; //x
		this.out = outputNumbers; //y
	}
	
	public double interpolate(double x){
		int lowIndex = 0;
		int highIndex = in.length-1;
		double lowX = in[lowIndex]; //below known range
		double highX = in[highIndex]; //above known range
		double lowY = out[lowIndex];
		double highY = out[highIndex];
		if(x < lowX) return lowY;
		if(highX < x) return highY;
		for(int i=0; i<30; i++){ //Should end before this. Avoid infinite loops.
			int midIndex = (lowIndex+highIndex)/2;
			double midX = in[midIndex];
			double midY = out[midIndex];
			if(x == midX) return midY; //TODO optimize. This happens rarely.
			if(x < midX){
				if(lowIndex+1 == midIndex){
					double rangeX = midX - lowX;
					double fractionX = (x-lowX)/rangeX;
					return lowY*(1-fractionX) + fractionX*midY;
				}else{
					highIndex = midIndex;
					highX = midX;
					highY = midY;
				}
			}else{
				if(midIndex+1 == highIndex){
					double rangeX = highX - midX;
					double fractionX = (x-midX)/rangeX;
					return midY*(1-fractionX) + fractionX*highY;
				}else{
					lowIndex = midIndex;
					lowX = midX;
					lowY = midY;
				}
			}
		}
		//nlmi.err('interpolate('+x+') function ended wrong (or would not end): this_node='+nlmi.toLongString(this_node));
		return Double.NaN;
	}

	public byte[] state(){
		return MathUtil.doublesToBytes(MathUtil.joinEvensOdds(in, out));
	}
	
	protected boolean modified;

	public void setModified(boolean m){
		modified = m;
	}

	public boolean isModified(){
		return modified;
	}

	public Class<? extends ObvarEditor> defaultEditorClass(){
		throw new Error("TODO");
	}

	public boolean isImmutableLocal(){
		return isImmutableDeep;
	}

	public boolean isImmutableDeep(){
		return isImmutableDeep;
	}

}
