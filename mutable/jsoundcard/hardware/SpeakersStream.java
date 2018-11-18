/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard.hardware;
import javax.sound.sampled.*;

/** Sound is a sequence of numbers oscillating between -1 and 1.
This class is for speakers or anything you can plug into a
speaker hole in the computer.
*/
public class SpeakersStream extends TimedFrameStream{
	
	public SpeakersStream(SourceDataLine sourceLine){
		super(sourceLine);
	}
	
	/** Like OutputStream.write and SourceDataLine.write but for doubles instead of bytes */
	public void readOrWriteFrames(double flos[], int offset, int frames){
		byte b[] = new byte[2*frameSize*frames];
		int samples = frames*frameSize;
		for(int s=0; s<samples; s++){
			//TODO Is this bit math correct?
			short sample = (short)(flos[offset+s]*Short.MAX_VALUE);
			byte highByte = (byte)(sample >> 8);
			byte lowByte = (byte) sample;
			b[s*2] = highByte;
			b[s*2+1] = lowByte;
		}
		((SourceDataLine)dataLine).write(b, 0, b.length);
	}

	public int bufferUsed(){
		final int bytesPerSample = 2;
		return bufferSize - dataLine.available()/(bytesPerSample*frameSize);
	}

}