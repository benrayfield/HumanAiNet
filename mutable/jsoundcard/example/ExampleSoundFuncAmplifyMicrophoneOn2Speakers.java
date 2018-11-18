/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard.example;
import mutable.jsoundcard.*;

public class ExampleSoundFuncAmplifyMicrophoneOn2Speakers implements SoundFunc{
	
	double volume = .7;
	
	//double averageAbsValMicrophoneAmplitude = .5;
	
	double maxMicrophoneAmplitude = .002;
	
	public int frameSize(){ return 3; }
	
	//int frames=0;
	
	public void readWriteFrame(double frame[]){
		double microphoneAmplitude = frame[2];
		int estimatedFramesPerSecond = 44100;
		//if(frames++ % estimatedFramesPerSecond == 0) JSoundCard.log("microphoneAmplitude="+microphoneAmplitude);
		double decaySeconds = .01;
		double decay = 1/(estimatedFramesPerSecond*decaySeconds);
		//averageAbsValMicrophoneAmplitude = averageAbsValMicrophoneAmplitude*(1-decay)
		//	+ decay*Math.abs(microphoneAmplitude);
		//maxMicrophoneAmplitude = Math.max(maxMicrophoneAmplitude, microphoneAmplitude);
		//maxMicrophoneAmplitude = maxMicrophoneAmplitude*(1-decay);
		frame[0] = microphoneAmplitude/maxMicrophoneAmplitude * volume;
		frame[1] = microphoneAmplitude/maxMicrophoneAmplitude * volume;
	}

}