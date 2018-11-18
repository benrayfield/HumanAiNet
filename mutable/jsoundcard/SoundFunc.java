/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard;

public interface SoundFunc extends FrameSize{
	
	/** This is where you define sound effects and musical instruments.
	<br><br>
	In the double array, speaker channels are first, then microphone channels,
	and if anything is left it is variables to be used by the SoundFunc
	and stored in that same array for use in the next call of the SoundFunc.
	This function may do nothing, but its best for it to read the microphone
	channels and whats at higher indexs, and write the speakers channels
	and whats at higher indexs.
	*/
	public void readWriteFrame(double frame[]);

}