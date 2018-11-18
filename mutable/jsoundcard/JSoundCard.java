/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard;
import javax.sound.sampled.*;

import mutable.jsoundcard.hardware.*;
import mutable.jsoundcard.math.*;

import java.util.*;

/** The most important functions are play and stop.
<br><br>
This software simplifies the way to do realtime audio input and output.
This is the central place for playing sounds, interactive musical instruments,
or any other interaction of speakers and/or microphones you define as a SoundFunc.
Simply choose how many speakers and microphones you want,
how many updates to the sound you want per second
(Example: Audio CDs use 44100 for 44.1 khz but most sound cards can handle more),
and create a SoundFunc to listen to microphones and calculate sounds for speakers.
<br><br>
This is a general speakers/microphones software. An example of what it could
be used for is...
Electric guitars are normally played
with a network of sound effects devices connected to each other
with 2 or more wires per device, often including cycles
which create recursive patterns in the sounds. A computer
running this software can be 1 of those devices by plugging
2 of those wires into the microphone hole and speaker hole
of the sound-card. There is always a little delay between
microphone and speakers in a computer, compared to those normal
devices which are only delayed by the speed of electricity,
but other than that delay, this software should be able to
simulate any of those devices if code is plugged into it
to define such patterns of floating point numbers (range -1 to 1).
I've used CodeSimian 0.65 with an electric guitar that way,
and Audivolv 0.1.7 demonstrates evolving musical instruments
but uses only speakers and no microphone hole.
This JSoundCard software will simplify access to speaker(s) and
microphone(s) so I can continue what I started in those 2 software.
<br><br>
OPTIONAL: You do not need to know about audio buffers, sound-cards, ports,
Mixers, etc, but if you want to, here's some info about the design of this software:
Most functions use units of frames instead of samples.
There are different sizes of frame.
A MicrophonesBuffer uses a TargetDataLine and has 1 or more microphone channels.
A SpeakersBuffer uses a SourceDataLine and has 1 or more speakers channels.
A SoundFunc has channels for all microphones, speakers, and/or extra channels
for reading/writing whatever numbers it wants.
MicrophonesBuffer, SpeakersBuffer, and SoundFunc all implement FrameSize.
One number (audio amplitude or other data) from 1 channel is a sample.
A frame is multiple samples.
In javax.sound.sampled.AudioFormat, a frame means the same thing for
the simple PCM way this software uses sound without compression or decompression. 
*/ 
public class JSoundCard{
	private JSoundCard(){}
	
	public static final boolean doLog = false;
	
	//TODO Include bufferBetweenSpeakersAndMicrophone[each index].size() in total
	//buffer size (also includes microphones.bufferUsed() and speakers.bufferUsed())
	
	public static final int DEFAULT_SPEAKER_QUANTITY = 2;
	private static int speakerQuantity = DEFAULT_SPEAKER_QUANTITY;
	public static int speakerQuantity(){ return speakerQuantity; }
	
	public static final int DEFAULT_MICROPHONE_QUANTITY = 1;
	private static int microphoneQuantity = DEFAULT_MICROPHONE_QUANTITY;
	public static int microphoneQuantity(){ return microphoneQuantity; }
	
	public static final double DEFAULT_FRAMES_PER_SECOND = 44100;
	private static double framesPerSecond = DEFAULT_FRAMES_PER_SECOND;
	public static double framesPerSecond(){ return framesPerSecond; }
	
	/** TODO Allow microphones from multiple sound-cards, so this would be a List<MicrophoneStream>,
	but that is hard to design the timing code for, since different sound-cards can have
	slightly different timing slightly different average speeds even when told to be the same speed. 
	*/
	private static volatile MicrophonesStream microphones;
	
	/** This buffer slowly adjusts for small differences of data read/write speeds of microphones
	and speakers, including differences in total quantity of data read/written over time.
	*/
	private static InterpolateBuffer bufferBetweenSpeakersAndMicrophone[] = new InterpolateBuffer[0];
	
	/** TODO Similar to microphones, expand this to be a List<SpeakersStream>. */
	private static volatile SpeakersStream speakers;
	
	private static volatile SoundFunc playing;
	
	/** null if nothing is playing */
	public static SoundFunc playingSoundFunc(){ return playing; }
	
	public static boolean isPlaying(){ return playing != null; }
	
	/** Only count block sizes when speakers buffer is not empty *
	private static final DecayingBellCurveWithRangeLimit speakersFramesPerBlockStatistics_whenNotEmpty =
		new DecayingBellCurveWithRangeLimit(2000., 1000., 0., 1000000.);
	
	private static final double speakersFramesPerBlockStatistics_decayFraction = 1./300;
	*/
	
	private static volatile long lastNanosecondTime = System.nanoTime();
	
	/** If there are long pauses, I don't want them to change the statistics.
	Let the sound skip then. For normal operation of the program, the delays between
	cycles should not exceed this. This is the border to say something external to
	the program was interfering too much and to ignore it in the statistics.
	Its best for many cycles to run per second.
	This number is a little lower than it should be because the first cycle
	may exceed this because of the time it takes to get access to sound cards.
	*/
	private static final double maxSecondsBetweenCyclesRecently_maxToUseInStatistics = .1;
	
	/** A number a little more than 1. When calculating targetBufferAmountUsed,
	maxSecondsBetweenCyclesRecently is multiplied by this so the delay is a little more
	than is expected to be needed.
	TODO Maybe this should be calculated from a DecayingBellCurveWithRangeLimit
	which keeps statistics on maxSecondsBetweenCyclesRecently.
	Or maybe it should be calculated from the standard deviation of the seconds between cycles
	this way: aveSecondsBetweenCycles + 3.5*stdDevSecondsBetweenCycles, where 3.5 is target std dev.
	*/
	private static final double maxSecondsBetweenCyclesRecently_multForExtraDelay = 1.5;
	
	private static final double maxSecondsBetweenCyclesRecently_decaySeconds = 15;
	
	private static double maxSecondsBetweenCyclesRecently = maxSecondsBetweenCyclesRecently_maxToUseInStatistics/2;
	
	/** If this was 2 then every 1 in 20 blocks (of frames) the sound would skip,
	because 2 standard deviations contains 95% of data points, and 1/(1-.95) is 20.
	Set it higher because sound skipping should be rare, but don't set it too high
	because that causes the delay between microphones and speaker to be bigger
	to avoid that skipping.
	*
	private static final double stdDevOfHowOftenSpeakersBufferIsAllowedToBecomeEmpty = 4.7;
	*/
	
	/** speakersFramesPerBlockStatistics_whenNotEmpty is statistics of how many frames
	are consumed by sound-card between the end of 1 block (which fills speakers buffer some)
	and the start of the next block (before it fills speakers buffer).
	This var remembers speaker buffer used at the end of the last block.
	*
	private static int speakersBufferUsedAfterAddedToItLastTime = 0;
	//private static int speakersBufferUsed_lastCycle = 0;
	*/
	
	/** Parameter array of the SoundFunc. The SoundFunc can store data in this array
	between calls in the indexs starting at speakers.frameSize()+microphones.frameSize().
	Speakers are the first indexs, microphones are after that,
	then general vars for the SoundFunc to store data.
	This array is size SoundFunc.frameSize().
	*/
	private static volatile double frame[];
	
	/** the Thread that reads from microphones, runs the SoundFunc, and writes to speakers. */
	private static volatile Thread soundThread;
	
	/** Goal value of microphones().bufferUsed()+speakers().bufferUsed().
	If speakers and microphones are used, then the flow of information is
	read audio amplitudes from microphones,
	copy 1 frame at a time to the SoundFunc's parameter array and run the SoundFunc,
	copy 1 frame at a time out of the SoundFunc's parameter array and into speakers.
	That is done in blocks of frames for efficiency.
	The delay from sound vibrations into the microphone to sound vibrations
	coming out of the speakers is approximately the sum of their bufferUsed().
	Each of their bufferSpeed() (absolute value of it) should be equal,
	so that delay should stay approximately constant while the block size
	and timing of processing the blocks varies, as long as that timing
	is not slow enough to let the speakers buffer get empty.
	The purpose of this targetBufferAmountUsed var is to slowly adjust
	the goal total buffer used amount to be as small as possible
	without letting the speakers buffer become empty.
	If the speakers buffer gets close to empty, quickly increase
	this targetBufferAmountUsed var. Always slowly decay this var
	toward 0. That strategy should stabilize this var
	at a small value while avoiding the sound skipping. It should result
	in a slowly changing and small delay between microphones and speakers.
	*/
	private static DecayingBellCurveWithRangeLimit targetBufferAmountUsed =
		new DecayingBellCurveWithRangeLimit(
			.02*DEFAULT_FRAMES_PER_SECOND, .002*DEFAULT_FRAMES_PER_SECOND, 10., 10000.);
	
	private static final double targetBufferAmountUsed_decaySeconds = 30;
	
	/** A decaying average of microphones().bufferUsed()+speakers().bufferUsed().
	targetBufferAmountUsed is the goal value of this var.
	*/
	private static DecayingBellCurveWithRangeLimit averageBufferAmountUsedRecently =
		new DecayingBellCurveWithRangeLimit(
			targetBufferAmountUsed.ave(), targetBufferAmountUsed.stdDev(), 0., 1000000.);
	
	//private static final double averageBufferAmountUsedRecently_decaySeconds = 2; //commentedout 2017-7-19
	private static final double averageBufferAmountUsedRecently_decaySeconds = 10;
	
	/** Current interpolating ratio between microphones and speakers buffers.
	If no interpolating was necessary, this would be 1.
	If the amount of buffer used is too much, decrease this and it will slowly shrink.
	*
	private static DecayingBellCurveWithRangeLimit bufferDerivative =
		new DecayingBellCurveWithRangeLimit(1., .01, .8, 1.2);
	*/
	private static double bufferDerivative = 1;
	
	/** Small values of this directly cause the sound to speed up and slow down,
	which should be avoided to make the sound more accurate.
	When this var is bigger, delays between microphones and speakers will be bigger,
	and the speakers buffer is more likely to become empty because bigger values
	of this var limit the acceleration of reaction to those things starting. 
	*
	private static final double bufferDerivative_decaySeconds = 20;
	*/
	
	/** Same as the other play function except it uses default options the first time,
	and if you later change those options (by calling the other play function, for example)
	then later calls of this use those options.
	*/
	public static void play(SoundFunc s) throws Exception{
		play(s, speakerQuantity, microphoneQuantity, framesPerSecond);
	}
	
	static long test;

	/** Sets the current SoundFunc which receives audio amplitudes from microphones
	and calculates audio amplitudes to send to speakers.
	If this is called with a different SoundFunc and the same other parameters
	as the previous call and the current SoundFunc is still playing, replaces it
	instantly without interacting with the sound-card.
	*/
	public static void play(SoundFunc s, int speakers, int microphones, double framesPerSecond)
			throws Exception{
		if(s == null) throw new NullPointerException("Use stop() to end playing.");
		if(speakers == speakerQuantity && microphones == microphoneQuantity
				&& framesPerSecond == JSoundCard.framesPerSecond && playing != null){
			playing = s;
			return;
		}
		speakerQuantity = speakers;
		microphoneQuantity = microphones;
		JSoundCard.framesPerSecond = framesPerSecond;
		synchronized(JSoundCard.class){
			playing = s;
			frame = new double[s.frameSize()];
		}
		bufferBetweenSpeakersAndMicrophone = new InterpolateBuffer[microphones];
		for(int i=0; i<microphones; i++) bufferBetweenSpeakersAndMicrophone[i] = new InterpolateBuffer();
		if(playing != null && soundThread == null){
			startOrChangeSoundCard(speakers, microphones, framesPerSecond);
			soundThread = new Thread(){
				public void run(){
					SoundFunc soundFunc = null;
					try{
						while((soundFunc = playing) != null){
							pumpNextBlockOfAudioThenSleep();
						}
					}catch(Exception e){
						throw new RuntimeException("Was playing "+soundFunc, e);
					}finally{
						soundThread = null; //pointer to this Thread
					}
				}
			};
			int thisPri = Thread.currentThread().getPriority();
			int maxPri = Thread.currentThread().getThreadGroup().getMaxPriority();
			try{
				soundThread.setPriority(maxPri);
				log("Set sound thread's priority to "+maxPri+" (Current thread's priority is "+thisPri+")");
			}catch(Exception e){
				log("Could not set sound thread's priority to "+maxPri
					+" (Current thread's priority is "+thisPri+"). Will use it without increasing priority.");
			}
			soundThread.start();
		}
	}

	public static void sleepSeconds(double seconds) throws InterruptedException{
		double dMilliseconds = 1000*seconds;
		long milliseconds = (long) dMilliseconds;
		double dMillisecondsRemainder = dMilliseconds-milliseconds;
		double dNanosecondsRemainder = 1000000*dMillisecondsRemainder;
		int nanosecondsRemainder = (int)Math.floor(dNanosecondsRemainder);
		Thread.sleep(milliseconds, nanosecondsRemainder);
	}
	
	public static void stop(){
		playing = null;
		while(soundThread != null){
			//When soundThread sees the playing var is null, it will end and set soundThread to null.
			Thread.yield();
		}
		stopSoundCard();
	}
	
	private static void stopSoundCard(){
		if(speakers != null) speakers.close();
		if(microphones != null) microphones.close();
		speakers = null;
		microphones = null;
	}
	
	private static void startOrChangeSoundCard(int speakers, int microphones, double framesPerSecond)
			throws Exception{
		//TODO stop old DataLines if they exist
		stopSoundCard();
		if(framesPerSecond < 1000 || framesPerSecond > 1000000) throw new IllegalArgumentException(
			"framesPerSecond="+framesPerSecond+" A normal value is "
			+DEFAULT_FRAMES_PER_SECOND+" for "+(DEFAULT_FRAMES_PER_SECOND/1000)+" khz sound.");
		if(speakers < 1) throw new RuntimeException("Must have at least 1 speaker channel"
			+" (TODO allow 0 speakers channels if there is at least 1 microphone channel).");
		if(speakers < 1 && microphones < 1) throw new IllegalArgumentException(
			"No speakers or microphones requested.");
		AudioFormat spkFormat = new AudioFormat((float)framesPerSecond, 16, speakers, true, true);
		AudioFormat micFormat = new AudioFormat((float)framesPerSecond, 16, microphones, true, true);
		List<Mixer> mixers = Mixers.getMixers();
		if(mixers.isEmpty()) throw new RuntimeException("Could not find any sound-cards (Mixers)");
		//Mixer bestMixer = mixers.get(0);
		int bufferSizeInFrames = (int)(framesPerSecond/2);
		int bytesPerSample = 2; //This is assumed in other parts of code (16 bit audio)
		int spkBufferSizeInBytes = bufferSizeInFrames*speakers*bytesPerSample;
		int micBufferSizeInBytes = bufferSizeInFrames*microphones*bytesPerSample;
		//TODO Move some of this into jsoundcard.hardware package?
		List<Exception> exceptions = new ArrayList<Exception>();
		boolean foundSpeakers = false, foundMicrophones = false;
		if(speakers > 0){
			DataLine.Info spkInfo = new DataLine.Info(SourceDataLine.class, spkFormat, spkBufferSizeInBytes);
			for(Mixer mixer : mixers){
				try{
					SourceDataLine speakersLine = (SourceDataLine) mixer.getLine(spkInfo);
					speakersLine.open(spkFormat, spkBufferSizeInBytes);
					speakersLine.start();
					JSoundCard.speakers = new SpeakersStream(speakersLine);
					log("Got access to speakers through "+Mixers.mixerInfoToString(mixer.getMixerInfo())+" format="+spkFormat);
					foundSpeakers = true;
					for(Control c : speakersLine.getControls()){
						log("Speaker control: "+c);
					}
					break;
				}catch(Exception e){
					exceptions.add(e);
					log("Could not access speakers through "+Mixers.mixerInfoToString(mixer.getMixerInfo()));
				}
			}
		}
		if(microphones > 0){
			DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, micFormat, micBufferSizeInBytes);
			for(Mixer mixer : mixers){
				try{
					TargetDataLine microphonesLine = (TargetDataLine) mixer.getLine(micInfo);
					microphonesLine.open(micFormat, micBufferSizeInBytes);
					microphonesLine.start();
					JSoundCard.microphones = new MicrophonesStream(microphonesLine);
					log("Got access to microphones through "+Mixers.mixerInfoToString(mixer.getMixerInfo())+" format="+micFormat);
					foundMicrophones = true;
					for(Control c : microphonesLine.getControls()){
						log("Microphone control: "+c);
					}
					break;
				}catch(Exception e){
					exceptions.add(e);
					log("Could not access microphones through "+Mixers.mixerInfoToString(mixer.getMixerInfo()));
				}
			}
		}
		if((speakers > 0 && !foundSpeakers) || (microphones > 0 && !foundMicrophones)){
			StringBuilder sb = new StringBuilder("Did not find all of speakers="+speakers
				+" microphones="+microphones+" The "+exceptions.size()+" Exceptions are:");
			for(Exception e : exceptions) sb.append(" --- "+e.getMessage());
			throw new LineUnavailableException(sb.toString());
		}
	}
	
	public static void log(String line){
		System.out.println(line);
		//TODO If JSoundCard is connected to JSelfModify, use JSelfModify.log(String)
		//or create an Appendable object for a separate log for JSoundCard and mount that
		//appendable into JSelfModify's tree, somewhere like /do/logs/jsoundcard
	}
	
	/** Called many times per second by a Thread created only for calling this.
	The amount of time it will sleep may depend on the statistics of audio buffers timing.
	A different thread calls play and stop.
	*/
	static void pumpNextBlockOfAudioThenSleep() throws Exception{
		//playing may be set to null before this function returns. Use this value of it.
		SoundFunc playing;
		double frame[];
		synchronized(JSoundCard.class){
			playing = JSoundCard.playing;
			frame = JSoundCard.frame;
		}
		MicrophonesStream mic = JSoundCard.microphones;
		SpeakersStream spk = JSoundCard.speakers;
		if(playing != null && spk != null){
			int spkBufferUsed = spk.bufferUsed();
			double chanceSpkBufferIsEmpty = spkBufferUsed==0 ? 1. : 0.; //TODO gradual chance if its close to 0
			int micBufferUsed = -1;
			
			/*
			int speakerFramesConsumedSinceLastBlock =
				speakersBufferUsedAfterAddedToItLastTime - spkBufferUsed;
			
			
			//TODO Only add to statistics if speakers buffer did not become empty (or close to it),
			//but how do I know what is too close? Maybe the speakers buffer is done
			//in blocks below the Java level and it would still contain hundreds
			//of frames and it made the sound skip. If the sound skipped, don't add to statistics.
			double decay = (1-chanceSpkBufferIsEmpty) * speakersFramesPerBlockStatistics_decayFraction;
			//Only count block sizes when speakers buffer is not empty
			speakersFramesPerBlockStatistics_whenNotEmpty.add(decay, (double)speakerFramesConsumedSinceLastBlock);
			*/
			
			
			//Sum of average of 3 buffer types: microphones, interpolate, speakers
			double bufferAmountUsed = spkBufferUsed;
			int micFrames;
			int spkFrames;
			int spkChannels = spk.frameSize();
			int micChannels = 0;
			double micAmplitudes[] = null;
			double spkAmplitudes[];
			
			double sumInterpolateBufferUsedThisCycle = 0;
			double minInterpolateBufferUsedThisCycle = Double.MAX_VALUE;
			int minInterpolateBufferUsedThisCycle_index = -1; //for testing
			/*
			for(int i=0; i<bufferBetweenSpeakersAndMicrophone.length; i++){
				double siz = bufferBetweenSpeakersAndMicrophone[i].size();
				sumInterpolateBufferUsedThisCycle += siz;
				if(siz < minInterpolateBufferUsedThisCycle){
					minInterpolateBufferUsedThisCycle = siz;
					minInterpolateBufferUsedThisCycle_index = i;
				}
			}
			double aveInterpolateBufferUsedThisCycle = bufferBetweenSpeakersAndMicrophone.length==0
				? 0 : sumInterpolateBufferUsedThisCycle/bufferBetweenSpeakersAndMicrophone.length;
			bufferAmountUsed += aveInterpolateBufferUsedThisCycle;
			*/
			
			//double bufferConsumeDerivative = 1/bufferDerivative.ave();
			double bufferConsumeDerivative = 1/bufferDerivative;
			
			if(mic != null){
				micBufferUsed = mic.bufferUsed();
				//Count mic buffers after they're put into the interpolate buffers. bufferAmountUsed += micBufferUsed;
				micFrames = micBufferUsed;
				micChannels = mic.frameSize();
				micAmplitudes = new double[micFrames*micChannels];
				//Read all the audio amplitudes in the microphones buffer
				mic.readOrWriteFrames(micAmplitudes, 0, micFrames);
				
				//Push all microphone amplitudes onto the interpolation buffers to handle
				//small differences in the speeds between microphones and speakers buffers.
				for(int micChannel=0; micChannel<micChannels; micChannel++){
					InterpolateBuffer buf = bufferBetweenSpeakersAndMicrophone[micChannel];
					for(int micFrame=0; micFrame<micFrames; micFrame++){
						buf.add(micAmplitudes[micFrame*micChannels+micChannel]);
					}
				}

				for(int i=0; i<bufferBetweenSpeakersAndMicrophone.length; i++){
					double siz = bufferBetweenSpeakersAndMicrophone[i].size();
					sumInterpolateBufferUsedThisCycle += siz;
					if(siz < minInterpolateBufferUsedThisCycle){
						minInterpolateBufferUsedThisCycle = siz;
						minInterpolateBufferUsedThisCycle_index = i;
					}
				}
				double aveInterpolateBufferUsedThisCycle = bufferBetweenSpeakersAndMicrophone.length==0
					? 0 : sumInterpolateBufferUsedThisCycle/bufferBetweenSpeakersAndMicrophone.length;
				bufferAmountUsed += aveInterpolateBufferUsedThisCycle*bufferDerivative;

				//There are different quantities of speakers and microphones frames
				//because they are slightly different in the hardware(s)
				//and interpolating buffer(s) are used to adjust for that.
				//
				//For now, all microphones are from the same sound-card,
				//and I've only tested it with 1 microphone at a time,
				//so I'll do this the simple way: Use the min size of all
				//interpolating buffers, and consume them all at the same speed.
				//
				//As many speakers frames as the current bufferDerivative allows
				spkFrames = (int)(minInterpolateBufferUsedThisCycle/bufferConsumeDerivative);
			}else{
				//Don't use microphones.
				//Generate exactly as many speakers amplitudes as target size says. 
				micFrames = -1;
				spkFrames = (int)Math.max(0, targetBufferAmountUsed.ave()-bufferAmountUsed);
			}
			
			spkAmplitudes = new double[spkFrames*spkChannels];
			//"buffer derivative" means the rate of change of the buffer size.
			//Consuming more of the buffer makes it shrink.
			for(int spkFrame=0; spkFrame<spkFrames; spkFrame++){
				//In frames[], speakers are first, then microphones,
				//then general vars for SoundFunc to use any way it wants.
				for(int micChannel=0; micChannel<micChannels; micChannel++){
					//Copy microphone amplitudes into parameter array so SoundFunc can see them
					//frame[spkChannels+m] = micAmplitudes[f*micChannels+m];
					InterpolateBuffer buf = bufferBetweenSpeakersAndMicrophone[micChannel];
					frame[spkChannels+micChannel] = buf.remove(bufferConsumeDerivative);
					//TODO slowly change bufferDerivative each frame.
				}
				playing.readWriteFrame(frame);
				for(int spkChannel=0; spkChannel<spkChannels; spkChannel++){
					spkAmplitudes[spkFrame*spkChannels+spkChannel] = frame[spkChannel];
				}
			}
			
			double seconds = spkFrames / JSoundCard.framesPerSecond;
			if(seconds != 0){
			
				double decay = seconds/targetBufferAmountUsed_decaySeconds;
				/*
				double observedSpkAve = speakersFramesPerBlockStatistics_whenNotEmpty.ave();
				double observedSpkStdDev = speakersFramesPerBlockStatistics_whenNotEmpty.stdDev();
				double devMult = stdDevOfHowOftenSpeakersBufferIsAllowedToBecomeEmpty;
				double targetTargetBufferAmountUsed = observedSpkAve+observedSpkStdDev*devMult;
				if(mic != null){
					//Speakers and microphones are supposed to stream frames
					//at approximately the same speed, so statistics on the speakers
					//buffer can be doubled to choose a goal for total buffer size.
					targetTargetBufferAmountUsed *= 2;
				}
				targetBufferAmountUsed.add(decay, targetTargetBufferAmountUsed);
				*/
				double targetTargetBufferAmountUsed = JSoundCard.framesPerSecond
					* maxSecondsBetweenCyclesRecently * maxSecondsBetweenCyclesRecently_multForExtraDelay;
				targetBufferAmountUsed.add(decay, targetTargetBufferAmountUsed);
				
				decay = seconds/averageBufferAmountUsedRecently_decaySeconds;
				averageBufferAmountUsedRecently.add(decay, bufferAmountUsed);
				
				//Should average 1.
				//If its more than 1, consume more microphone time for each unit of speaker time.
				//If less than 1, consume less. Change the bufferDerivative var to do that.
				//double normalizedAveBufferAmountUsed =
				//	averageBufferAmountUsedRecently.ave()/targetBufferAmountUsed.ave();
				//double normalizedBufferAmountUsed = bufferAmountUsed/targetBufferAmountUsed.ave();
				
				//decay = seconds/bufferDerivative_decaySeconds;
				//decay = 1;
				//double targetBufferDerivative = bufferDerivative.ave()/normalizedAveBufferAmountUsed;
				double epsilonFrames = 10;
				double targetBufferDerivative = (targetBufferAmountUsed.ave()+epsilonFrames)
					/ (averageBufferAmountUsedRecently.ave()+epsilonFrames);
				//bufferDerivative.add(decay, targetBufferDerivative);
				bufferDerivative = targetBufferDerivative;
				
				//bufferDerivative.add(1., 1/normalizedAveBufferAmountUsed); //set to exact value
				
				long newNanosecondTime = System.nanoTime();
				double secondsSinceLastCycle = (newNanosecondTime-lastNanosecondTime)*.000000001;
				lastNanosecondTime = System.nanoTime();
				secondsSinceLastCycle = Math.min(secondsSinceLastCycle,
					maxSecondsBetweenCyclesRecently_maxToUseInStatistics);
				maxSecondsBetweenCyclesRecently = Math.max(maxSecondsBetweenCyclesRecently, secondsSinceLastCycle);
				decay = seconds/maxSecondsBetweenCyclesRecently_decaySeconds;
				maxSecondsBetweenCyclesRecently *= 1 - decay;
		
				//TODO remove this duplicate line
				InterpolateBuffer smallestBuf = bufferBetweenSpeakersAndMicrophone[minInterpolateBufferUsedThisCycle_index];
		
				if(doLog && test++ % 100 == 0){
					String n = "\r\n";
					JSoundCard.log(
						n+n+"seconds="+seconds
						+n+"secondsSinceLastCycle="+secondsSinceLastCycle
						+n+"maxSecondsBetweenCyclesRecently="+maxSecondsBetweenCyclesRecently
						+n+"averageBufferAmountUsedRecently="+averageBufferAmountUsedRecently
						//+n+"chanceSpeakersBufferWasTooLowRecently="+chanceSpeakersBufferWasTooLowRecently
						//+n+"targetAndObservedSimilarityFraction="+targetAndObservedSimilarityFraction
						+n+"targetBufferAmountUsed="+targetBufferAmountUsed
						//+n+"targetTargetBufferAmountUsed="+targetTargetBufferAmountUsed
						//+n+"speakersFramesPerBlockStat...="+speakersFramesPerBlockStatistics_whenNotEmpty
						//+n+"speakerFramesConsumedSinceLastBlock="+speakerFramesConsumedSinceLastBlock
						+n+"bufferAmountUsed="+bufferAmountUsed
						//+n+"normalizedAveBufferAmountUsed="+normalizedAveBufferAmountUsed
						//+n+"bufferAmountUsedPerSecond="+bufferAmountUsedPerSecond
						//+n+"targetBufferDerivative="+targetBufferDerivative
						+n+"bufferDerivative="+bufferDerivative
						+n+"bufferBetweenSpeakersAndMicrophone[0].size()="+bufferBetweenSpeakersAndMicrophone[0].size()
						+n+"smallestBuf.size()="+smallestBuf.size()
						//+n+"speakersBufferUsedAfterAddedToItLastTime="+speakersBufferUsedAfterAddedToItLastTime
						//+n+"speakersFramesPerBlockStatistics_whenNotEmpty="+speakersFramesPerBlockStatistics_whenNotEmpty
						+n+"minInterpolateBufferUsedThisCycle="+minInterpolateBufferUsedThisCycle
						+n+"spkFrames (software) = "+spkFrames
						+n+"micFrames (software) = "+micFrames
						+n+"spkBufferUsed (hardware) = "+spkBufferUsed
						+n+"micBufferUsed (hardware) = "+micBufferUsed
					);
				}
			}
			
			InterpolateBuffer smallestBuf = bufferBetweenSpeakersAndMicrophone[minInterpolateBufferUsedThisCycle_index];
			if(smallestBuf.size() > bufferConsumeDerivative){
				throw new Exception(smallestBuf.size()+" == smallestBuf.size() > bufferConsumeDerivative == "+bufferConsumeDerivative
					+" Should have consumed almost all of it (loaded from microphone) to use in speakers.");
			}
			
			//Write a block of audio amplitudes to the speakers
			if(spkFrames > 0) spk.readOrWriteFrames(spkAmplitudes, 0, spkFrames);
			
			//speakersBufferUsedAfterAddedToItLastTime = spkBufferUsed+spkFrames;
			
			//double targetSeconds = targetBufferAmountUsed.ave()/JSoundCard.framesPerSecond;
			//double targetSeconds = spkBufferUsed/JSoundCard.framesPerSecond;
			//Try to sleep much less time than allowed because something could delay it after
			//this earlier time that it sleeps instead.
			sleepSeconds(maxSecondsBetweenCyclesRecently/5);
		}
	}

}