/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard.example;
import mutable.jsoundcard.SoundFunc;

public class ExampleSoundFuncSimpleSineWave implements SoundFunc{
	
	double x;
	
	int channels;
	
	double volume = .4;
	
	public ExampleSoundFuncSimpleSineWave(int channels){
		this.channels = channels;
	}
	
	public int frameSize(){ return channels; }
	
	public void readWriteFrame(double frame[]){
		x += .3;
		double circle = 2*Math.PI;
		for(int i=0; i<channels; i++){
			frame[i] = volume*Math.sin(x+i*circle/channels);
		}
	}

}