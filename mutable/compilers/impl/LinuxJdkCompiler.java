/** Ben F Rayfield offers this software opensource MIT license */
package mutable.compilers.impl;
import java.io.File;

import mutable.compilers.JavaCompiler;

public class LinuxJdkCompiler implements JavaCompiler{
	
	public final boolean pauseSoPersonCanPutBreakpoint;
	
	public LinuxJdkCompiler(boolean pauseSoPersonCanPutBreakpoint){
		this.pauseSoPersonCanPutBreakpoint = pauseSoPersonCanPutBreakpoint;
	}
	
	public boolean pausesSoPersonCanPutBreakpoint(){ return pauseSoPersonCanPutBreakpoint; }

	public String lang(){ return "java"; }

	public boolean debuggable(){ return true; }

	public File compilerExecutable(){
		throw new Error("TODO");
	}

	public File classpathTo(){
		throw new Error("TODO");
	}

	public Class compile(String... classParts){
		throw new Error("TODO");
	}

}