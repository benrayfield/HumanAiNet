/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard.example;
import mutable.jsoundcard.*;

public class ExampleSoundFuncMetaphysicalGameControllerEarlyExperiment implements SoundFunc{
	
	public final double[] freqs;
	
	protected double relativeTime = 0;
	
	public final double[] measureReal;
	
	public final double[] measureImaginary;
	
	/** TODO extremely optimize using complex number multiply instead of calling sine,
	so can do much more freqs at once
	*/
	public ExampleSoundFuncMetaphysicalGameControllerEarlyExperiment(double... freqs){
		if(freqs.length == 0) throw new RuntimeException("no freqs");
		this.freqs = freqs;
		this.measureReal = new double[freqs.length];
		this.measureImaginary = new double[freqs.length];
	}
	
	double volume = .12;
	
	//double averageAbsValMicrophoneAmplitude = .5;
	
	double maxMicrophoneAmplitude = 1;
	
	double maxOutputAmplitude = 1;
	
	public int frameSize(){ return 3; }
	
	public double amplitude(int freqIndex){
		double r = measureReal[freqIndex], i = measureImaginary[freqIndex];
		return Math.sqrt(r*r+i*i);
	}
	
	int frames=0;
	
	public void readWriteFrame(double frame[]){
		frames++;
		double microphoneAmplitude = frame[2];
		int estimatedFramesPerSecond = 44100;
		double dt = 1./estimatedFramesPerSecond;
		relativeTime += dt;
		
		//freqs[0] *= 1+dt*.01; //FIXME remove this
		
		//if(frames++ % estimatedFramesPerSecond == 0) JSoundCard.log("microphoneAmplitude="+microphoneAmplitude);
		double decaySeconds = .2;
		double decay = dt/decaySeconds;
		//averageAbsValMicrophoneAmplitude = averageAbsValMicrophoneAmplitude*(1-decay)
		//	+ decay*Math.abs(microphoneAmplitude);
		maxMicrophoneAmplitude = Math.max(maxMicrophoneAmplitude, microphoneAmplitude);
		maxMicrophoneAmplitude = maxMicrophoneAmplitude*(1-decay);
		
		for(int f=0; f<freqs.length; f++){
			//double wavelengthOfDecay = freqs[f]/52.34;
			//double wavelengthOfDecay = freqs[f]*12.34;
			double wavelengthOfDecay = freqs[f]*25;
			double freqDecay = 1/wavelengthOfDecay;
			double x = relativeTime*freqs[f];
			double fourierReal = Math.cos(x);
			double fourierImaginary = Math.sin(x);
			double targetReal = fourierReal*microphoneAmplitude;
			double targetImaginary = fourierImaginary*microphoneAmplitude;
			measureReal[f] = measureReal[f]*(1-freqDecay) + freqDecay*targetReal;
			measureImaginary[f] = measureImaginary[f]*(1-freqDecay) + freqDecay*targetImaginary;
		}
		
		//frame[0] = microphoneAmplitude/maxMicrophoneAmplitude * volume;
		//frame[1] = microphoneAmplitude/maxMicrophoneAmplitude * volume;
		//frame[0] = frame[1] = 0; //silent
		/*
		double a = amplitude(0);
		//if(frames%10000==0)
		System.out.println(""+a);
		frame[0] = frame[1] = volume*a*Math.sin(5000*relativeTime); //output first frequency as volume of const frequency
		*/
		double out = 0;
		for(int f=0; f<freqs.length; f++){ //only output freqs measured as themself
			out += amplitude(f)*Math.sin(relativeTime*freqs[f])/freqs.length;
		}
		maxOutputAmplitude = Math.max(maxOutputAmplitude, out);
		maxOutputAmplitude = maxOutputAmplitude*(1-decay);
		out /= maxOutputAmplitude;
		frame[0] = frame[1] = volume*out;
	}

}