/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard.hardware;
import javax.sound.sampled.*;

/** For microphones or anything you can plug into a microphone hole in
the computer, like an electric guitar. This is for 1 or more
microphone holes in the same sound-card.
Sound is a sequence of numbers oscillating between -1 and 1.
*/
public class MicrophonesStream extends TimedFrameStream{
	
	public MicrophonesStream(TargetDataLine targetLine){
		super(targetLine);
	}
	
	/** Like InputStream.read and TargetDataLine.read but for doubles instead of bytes */
	public void readOrWriteFrames(double flos[], int offset, int frames){
		//TODO Is this bit math correct? Audio sounds smooth, so probably it is, but verify.
		byte b[] = new byte[2*frameSize*frames];
		((TargetDataLine)dataLine).read(b, 0, b.length);
		int samples = frames*frameSize;
		for(int s=0; s<samples; s++){
			byte highByte = b[s*2];
			byte lowByte = b[s*2+1];
			flos[offset+s] = (double) (highByte*256 + (lowByte&255)) / 0x8000;
		}
	}

	public int bufferUsed(){
		final int bytesPerSample = 2;
		return dataLine.available()/(bytesPerSample*frameSize);
	}

}