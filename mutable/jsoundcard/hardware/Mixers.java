/** Ben F Rayfield offers this software opensource MIT license */
package mutable.jsoundcard.hardware;
import javax.sound.sampled.*;

import mutable.jsoundcard.*;

import java.util.*;

public class Mixers{
	private Mixers(){}
	
	/** Returns an immutable list of Mixers in descending order of quality.
	<br><br>
	TODO use multiple Mixers simultaneously if there are multiple sound-cards.
	Use 1 or more SoundCardPart for each Mixer.
	Combine all those SoundCardPart into an OverlapFuncsInArray.
	*/
	public static List<Mixer> getMixers(){
		List<Mixer> list = new ArrayList<Mixer>();
		for(Mixer.Info info : AudioSystem.getMixerInfo()){
			list.add(AudioSystem.getMixer(info));
		}
		Comparator<Mixer> compareMixers = new Comparator<Mixer>(){
			public int compare(Mixer x, Mixer y){
				return estimateMixerQuality(x) < estimateMixerQuality(y) ? 1 : -1;
			}
		}; //TODO verify descending order
		Collections.sort(list, compareMixers);
		int i = 0;
		for(Mixer m : list) JSoundCard.log("Mixer "+(i++)+": "+mixerInfoToString(m.getMixerInfo()));
		return Collections.unmodifiableList(list);
	}
	
	public static String mixerInfoToString(Mixer.Info mi){
		return "[Mixer.Info "+mi.getName()+" "+mi.getVendor()+" "+mi.getVersion()+" "+mi.getDescription()+"]";
	}
	
	/** This function should be used to choose the default sound-card(s) to use
	before the user chooses. Score must be positive.
	<br><br>
	This should not cause incompatibility with any sound-card,
	but some businesses may choose to sell versions of this software (with source-code)
	that always prefer the sound-cards they build or do not work with any other cards.
	You should look at this function (and other places) and fix it if it says that.
	*/
	public static double estimateMixerQuality(Mixer mixer){
		Mixer.Info mi = mixer.getMixerInfo();
		String s = mixerInfoToString(mi);
		s = s.toLowerCase();
		double score = .5;
		//Failure messages should be avoided the most
		if(s.matches(".*(error|exception|throw|fail).*")) score *= .1;
		//Emulation is slow. "java" or "soft" in the sound-card name is probably emulated.
		if(s.matches(".*(emu|java|soft|virt|vm|compat).*")) score *= .7;
		//"Primary Sound Driver" sounds bad on my system. General names are bad.
		if(s.matches(".*sound.*driver.*")) score *= .8;
		//Device means its probably lower level and faster.
		if(s.matches(".*device.*")) score *= 1.1;
		//I dont know why operating-system name would be in it, but if it is,
		//you probably have that operating-system, and its more likely to be the right card.
		if(s.matches(".*(unix|linux|mac|win).*")) score *= 1.5;
		//Sound-blaster X-fi is high quality.
		if(s.matches(".*blast.*")) score *= 1.2;
		if(s.matches(".*x.*fi.*")) score *= 1.7;
		
		if(s.matches(".*capture.*")) score += 3;
		if(s.matches(".*primary.*")) score += 3;
		
		//TODO add more sound-card types and interpretations of their quality here.
		//If you change this software in any way, and your card works well,
		//add it here. This list should be long.
		//I don't mean to give my own opinions of sound cards more importance
		//than everyone else's, but I have to start with something.
		return score;
	}

}