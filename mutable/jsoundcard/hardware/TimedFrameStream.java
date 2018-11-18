/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard.hardware;
import javax.sound.sampled.*;

import mutable.jsoundcard.FrameSize;

public abstract class TimedFrameStream implements FrameSize{
	
	protected final DataLine dataLine;
	protected final int frameSize;
	protected final int bufferSize;
	protected double bufferSpeed;
	
	public TimedFrameStream(DataLine dataLine){
		if(!dataLine.isOpen()) throw new RuntimeException("Not open: "+dataLine);
		this.dataLine = dataLine;
		AudioFormat f = dataLine.getFormat();
		if(f.getSampleSizeInBits() != 16) throw new RuntimeException(
			f.getSampleSizeInBits()+" == getSampleSizeInBits() != 16");
		if(!f.isBigEndian()) throw new RuntimeException("not big endian");
		if(f.getEncoding() != AudioFormat.Encoding.PCM_SIGNED){
			throw new RuntimeException("not PCM_SIGNED: "+f.getEncoding());
		}
		frameSize = f.getChannels();
		bufferSize = dataLine.getBufferSize() / (2*frameSize);
		//TODO Should f.getSampleRate() be divided by channels? Its supposed to be frames per second.
		//TODO Move microphones-specific and speakers-specific code into those subclasses
		if(dataLine instanceof TargetDataLine){ //microphone(s)
			bufferSpeed = f.getSampleRate();
		}else if(dataLine instanceof SourceDataLine){ //speaker(s)
			bufferSpeed = -f.getSampleRate();
		}else{
			throw new RuntimeException("Not a target or source DataLine: "+dataLine);
		}
	}
	
	/** If this is for microphone(s),
	it will copy audio amplitudes from a sound-card into the array.
	If this is for speaker(s),
	it will copy audio amplitudes from the array to a sound-card.
	*/
	public abstract void readOrWriteFrames(double flos[], int offset, int frames);

	/** How many frames are stored in the buffer? Each frame is size channels(). */
	public abstract int bufferUsed();
	
	/** Buffer is big enough for bufferUsed() to be this. */
	public int bufferSize(){ return bufferSize; }
	
	/** Speakers shrink bufferUsed() as sound is played, so this would be negative.
	Microphones add to bufferUsed() as sound is received, so this would be positive.
	This is the speed bufferUsed() changes.
	<br><br>
	TODO Should this be a decaying average or what the AudioFormat says about speed?
	Some code has to keep statistics about the actual speeds.
	*/
	public double bufferSpeed(){ return bufferSpeed; }
	
	/** Number of channels */
	public int frameSize(){ return frameSize; }
	
	/** Starts open. Closes when garbage-collected or close() is called. */
	public boolean isOpen(){ return dataLine.isOpen(); }
	
	public void close(){
		dataLine.flush();
		dataLine.close();
	}
	
	protected void finalize() throws Throwable{ close(); }

}