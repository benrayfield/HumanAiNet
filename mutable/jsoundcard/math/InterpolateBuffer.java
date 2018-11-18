/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard.math;

/** To solve problems caused by small differences in the speeds of
various sound-cards being used simultaneously (like requesting 44.1 khz
but getting 44.2 khz on microphones and 44.0 khz on speakers),
this class can be used between such buffers to convert between
their different speeds of sending/receiving numbers (audio amplitudes).
This class receives data points on an even interval while
data points are removed from the other end of this buffer on uneven
intervals that slowly change over time. You can put in 100
data points and take out 105 data points, for example,
and the 105 data points will be an approximation of the 100.
<br><br>
TODO Use a curved equation instead of linear interpolation,
and use it on adjacent sets of 4 or maybe 6 or 8 data points
at a time to calculate what should be between the middle 2 data points.
Until then, do a simple linear interpolation between each adjacent 2.
*/
public class InterpolateBuffer{

	protected double buffer[] = new double[1];

	/** Range 0 to bufferUsed-1 indexs of buffer contain data points */
	protected int bufferUsed;
	
	/** If this was always an integer and increased by 1 each time
	data was removed from this buffer then interpolation would have no effect.
	The purpose of this buffer is to allow removing more or less relative time than 1.
	If this is 3.9 then it will be an interpolation between buffer[3] and buffer[4].
	<br><br>
	TODO If its curved equations instead of linear interpolation,
	then it would also consider buffer[2] and buffer[5] to calculate those curves,
	and maybe a few more data points for more accuracy.
	*/
	protected double positionInBuffer;
	
	public void add(double value){
		if(bufferUsed == buffer.length){
			double newBuffer[] = new double[buffer.length*2];
			System.arraycopy(buffer, 0, newBuffer, 0, bufferUsed);
			buffer = newBuffer;
		}
		if(positionInBuffer > buffer.length/2){
			//Move the useful part of the array down.
			
			//TODO Move a few less than that so there will be more adjacent data points
			//for curve interpolation, when this software is modified to use curve interpolation
			//instead of simply linear interpolation between 2 at a time.
			
			int lowIndex = (int)positionInBuffer;
			int moveHowMany = bufferUsed-lowIndex;
			System.arraycopy(buffer, lowIndex, buffer, 0, moveHowMany);
			bufferUsed -= lowIndex;
			positionInBuffer -= lowIndex;
		}
		buffer[bufferUsed++] = value;
	}
	
	public double remove(double sizeToPop){
		//TODO Return average value over the removed area instead of only the value at 1 position.
		if(positionInBuffer+sizeToPop > bufferUsed){
			//throw new IndexOutOfBoundsException("positionInBuffer="+positionInBuffer+" sizeToPop="+sizeToPop+" end="+end);
			return 0.;
		}
		double positionToObserve = positionInBuffer+sizeToPop/2;
		int lowIndex = (int)positionToObserve;
		int highIndex = bufferUsed==1 ? lowIndex : lowIndex+1;
		double fractionPartOfPos = positionToObserve - lowIndex;
		double valueObserved = buffer[lowIndex]*(1-fractionPartOfPos)
			+ fractionPartOfPos*buffer[highIndex];
		positionInBuffer += sizeToPop;
		return valueObserved;
	}
	
	public double size(){
		return bufferUsed - positionInBuffer;
	}
	
}
