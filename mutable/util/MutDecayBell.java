/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;

/** Decaying ave and dev.
Changes faster when less data has been added,
up to 1.0 dataAveraged which increases from 0 by decay.
*/
public class MutDecayBell{
	
	protected double ave, dev, howMuchData;
	
	public double ave(){ return ave; }
	
	public double dev(){ return dev; }
	
	public double devOf(double value){
		return (value-ave)/dev;
	}
	
	/** If decayFraction is 0, does nothing. If decayFraction is .1, value replaces
	10% of existing data. Normally decayFraction should be very close to 0 to change it a little.
	If decayFraction is 1, the standard deviation would become 0
	because all the data points would equal, so you probably don't want to do that.
	After calling this, the average and standard deviation will change.
	TODO Test this.
	*/
	public void add(double value, double decayFraction){
		if(decayFraction < 0 || 1 < decayFraction) throw new RuntimeException("decayFraction="+decayFraction);
		if(howMuchData == 1){
			//This is caclulated as an infinite number of data points,
			//but instead of using a whole number for the quantity of data points,
			//the total number of data points is 1 and each is epsilon width.
			ave = ave*(1-decayFraction) + decayFraction*value;
			double newDiff = value-ave;
			double sumOfSquares = dev*dev*(1-decayFraction) + decayFraction*newDiff*newDiff;
			//Don't divide sumOfSquares by quantity of data points because its 1.0
			dev = Math.sqrt(sumOfSquares);
		}else if(howMuchData == 0){
			ave = value;
			dev = 0;
			howMuchData = decayFraction;
		}else{
			double newHowMuchData = howMuchData+decayFraction;
			ave = (ave*howMuchData + decayFraction*value)/newHowMuchData;
			double newDiff = value-ave;
			double sumOfSquares = (dev*dev*howMuchData + decayFraction*newDiff*newDiff)/newHowMuchData;
			dev = Math.sqrt(sumOfSquares);
			howMuchData = Math.min(1,newHowMuchData);
		}
	}

	public String toString(){
		if(howMuchData < 1) return "[ave="+ave()+" dev="+dev()+" dataAveraged="+howMuchData+"]";
		return "[ave="+ave()+" dev="+dev()+"]";
	}
	
}
