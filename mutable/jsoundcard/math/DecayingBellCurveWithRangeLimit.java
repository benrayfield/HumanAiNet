/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard.math;

/** When data is added, it is forced into the specified range if its outside.
Then the decaying average and standard deviation are updated.
*/
public class DecayingBellCurveWithRangeLimit{
	
	protected double ave, stdDev, minAllowed, maxAllowed;
	
	public DecayingBellCurveWithRangeLimit(
			double ave, double stdDev, double minAllowed, double maxAllowed){
		this.ave = ave;
		this.stdDev = stdDev;
		this.minAllowed = minAllowed;
		this.maxAllowed = maxAllowed;
	}
	
	public DecayingBellCurveWithRangeLimit(DecayingBellCurveWithRangeLimit copyMe){
		this(copyMe.ave, copyMe.stdDev, copyMe.minAllowed, copyMe.maxAllowed);
	}
	
	public double ave(){ return ave; }
	
	public double stdDev(){ return stdDev; }
	
	public double minAllowed(){ return minAllowed; }
	
	public double maxAllowed(){ return maxAllowed; }
	
	/** If decayFraction is 0, does nothing. If decayFraction is .1, value replaces
	10% of existing data. Normally decayFraction should be very close to 0 to change it a little.
	If decayFraction is 1, the standard deviation would become 0
	because all the data points would equal, so you probably don't want to do that.
	After calling this, the average and standard deviation will change.
	TODO Test this.
	*/
	public void add(double decayFraction, double value){
		if(decayFraction < 0 || decayFraction > 1) throw new IllegalArgumentException(
			"decayFraction="+decayFraction);
		value = Math.max(minAllowed, Math.min(value, maxAllowed));
		//This is caclulated as an infinite number of data points,
		//but instead of using a whole number for the quantity of data points,
		//the total number of data points is 1 and each is epsilon width.
		ave = ave*(1-decayFraction) + decayFraction*value;
		double newDiff = value-ave;
		double sumOfSquares = stdDev*stdDev*(1-decayFraction) + decayFraction*newDiff*newDiff;
		//Don't divide sumOfSquares by quantity of data points because its 1.0
		stdDev = Math.sqrt(sumOfSquares);
	}

	public String toString(){
		return "[ave="+ave()+" stdDev="+stdDev()+" minAllowed="+minAllowed()+" maxAllowed="+maxAllowed()+"]";
	}
	
}
