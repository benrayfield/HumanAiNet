/** Ben F Rayfield offers this software opensource MIT license */
package mutable.games.mmgMouseAI.mouseRecorder;
import static mutable.util.Lg.*;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Robot;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import immutable.util.DecayBell;
import mutable.util.Files;
import mutable.util.Obvars;
import mutable.util.Time;

public class CommandlineMouseYRecorder{
	
	public static final String fileVar = "mainMouseYBitsAppendedLongterm";
	
	public static void main(String[] args){
		OutputStream out = null;
		try{
			//DecayBell yRange = new DecayBell();
			int prevY = 0;
			List<Boolean> data = new ArrayList();
			double timeStart = Time.time();
			long cycles = 0;
			long ones = 0;
			long onesThisLine = 0, zerosThisLine = 0;
			double prevTime = timeStart;
			StringBuilder sb = new StringBuilder();
			lg("Appending to fileVar: "+fileVar+" in file "+Obvars.fileVarsContent.fileOf(fileVar));
			out = Obvars.appendingUtf8(fileVar);
			while(true){
				double now = Time.time();
				double duration = now-timeStart;
				double dt = now-prevTime;
				prevTime = now;
				if(duration > 8*60*60) return; //in case I leave it on
				int y = MouseInfo.getPointerInfo().getLocation().y;
				if(y != prevY){
					cycles++;
					boolean b = prevY>y;
					//if(b) ones++;
					if(b) onesThisLine++;
					else zerosThisLine++;
					prevY = y;
					char c = b?'1':'0';
					try{
						out.write((byte)c); //only works for ascii, which 1 and 0 are
					}catch(IOException e) { throw new Error(e); }
					sb.append(c);
					System.out.print(c);
					if(cycles%128==0){
						//long zeros = cycles-ones;
						System.out.print(" 1s="+onesThisLine+" 0s="+zerosThisLine);
						String line = sb.toString();
						if(Math.abs(onesThisLine-zerosThisLine)/2 < 4) System.out.print(" OK");
						System.out.println();
						//String normedLine = norm(line);
						//System.out.println(normedLine+" normed");
						onesThisLine = zerosThisLine = 0;
						sb.setLength(0);
					}
				}
				double hz = 32;
				double sleepTime = Math.min(5.,(1+dt/60)/hz); //between 1/hz and 5 seconds, very near 1/32 seconds if moved in last minute and become that fast again after first mouse move
				Time.sleepNoThrow(sleepTime);
			}
		}finally{
			Files.close(out);
		}
	}
	
	/** TODO make there be equal number of 1s and 0s */
	static String norm(String bits){
		/*if((bits.length()&1)!=0) throw new Error("Must be even length: "+bits);
		int ones = 0;
		char[] chars = bits.toCharArray();
		for(int i=0; i<chars.length; i++){
			if(chars[i]=='1') ones++;
		}*/
		throw new Error("TODO");
		//return bits; //FIXME
	}

}
