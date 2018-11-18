/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;
import static mutable.util.Lg.*;

import immutable.util.Text;

public class Cmd{
	
	/** system command, like "c:\\java\\jdk1.8.0_151\\bin\\java.exe -version". Returns concat(stderr,stdout). */
	public static String eval(String cmd){
		lg("Cmd.eval: "+cmd);
		try{
			Process p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			String stderr = Text.bytesToString(Files.readFully(p.getErrorStream()));
			String stdout = Text.bytesToString(Files.readFully(p.getInputStream()));
			return stderr+stdout;
		}catch(Exception e){
			throw new Error(e);
		}
	}
	
	public static void main(String[] args){
		double start = Time.time();
		System.out.println(eval("c:\\java\\jdk1.8.0_151\\bin\\java.exe -version"));
		double end = Time.time();
		System.out.println((end-start)+" seconds");
	}

}
