/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard;
import mutable.jsoundcard.example.*;

public class TestJSoundCard{
	private TestJSoundCard(){}

	/** Play some test sounds, including microphone-speakers interaction */
	public static void main(String args[]) throws Exception{
		JSoundCard.log("In this version of JSoundCard, the timing of the buffers is adjusted ok but could be improved. You have to have 1 microphone. In future versions, it will allow any small number of speakers and/or microphones and multiple sound cards at once. To do that, it will have to listen to all microphones to make sure they can hear anything, and play something on the speakers to make sure the microphones hear them. A fast test can be run when the software starts and when the user clicks a button to do the test again after changing the speakers/microphones, in later versions. For now you can use it with simple options like 2 speakers and 1 microphone.");
		JSoundCard.sleepSeconds(1.);
		//Play 2 test sounds
		SoundFunc testSounds[] = new SoundFunc[]{
			//new ExampleSoundFuncMeasureFreqs(200, 400),
			//new ExampleSoundFuncMeasureFreqs(200, 400, 800),
			//new ExampleSoundFuncMeasureFreqs(1400),
			new ExampleSoundFuncMetaphysicalGameControllerEarlyExperiment(7000,2000,3000)//,
			//new ExampleSoundFuncMeasureFreqs(300, 400, 500, 1500),
			//new ExampleSoundFuncMeasureFreqs(200, 400, 800, 900, 1000),
			//new ExampleSoundFuncMeasureFreqs(200, 300, 400, 500, 600),
			//new ExampleSoundFuncAmplifyMicrophoneOn2Speakers(),
			//new ExampleSoundFuncSimpleSineWave(3)
		};
		long seconds = 60;
		int i = 0;
		for(SoundFunc f : testSounds){
			JSoundCard.log("About to play: "+f+" for "+seconds+" seconds...");
			JSoundCard.play(f, 2, 1, JSoundCard.DEFAULT_FRAMES_PER_SECOND);
			JSoundCard.log("Started playing "+f);
			Thread.sleep(1000*seconds);
			JSoundCard.log("About to end playing...");
			JSoundCard.stop();
			JSoundCard.log("Ended playing. You should have heard "+f+" for "+seconds+" seconds.");
		}
	}
}