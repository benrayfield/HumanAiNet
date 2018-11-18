/** Ben F Rayfield offers this software opensource MIT license */
package mutable.compilers;
import java.io.File;

/** A simple compiler for just 1 new class at a time, given existing classes already in memory.
This is not for multiple files that use eachother with a cycle of dependencies between them.
This is for small pieces of code AI or evolved musical instruments generate, for example.
If you want sandboxing, you should do it before giving the code to a Compiler since it doesnt sandbox.
*/
public interface JavaCompiler{ //TODO rename to JavaCompiler
	
	public String lang();
	
	/** If true, this compiler puts in debug info so can be used in debug mode such as in Eclipse or Netbeans */
	public boolean debuggable();
	
	/** If pausesSoPersonCanPutBreakpoint, a popup will appear when the .java and .class files are created
	and can be loaded in IDE (such as eclipse or netbeans), so a person could open that,
	such as in generated.SomeHashName4325q3e45tdfg345345234sdf class,
	then put breakpoints, then click the button to continue the program, or continue right away.
	If pauseSoPersonCanPutBreakpoint is false, just continues.
	*/
	public boolean pausesSoPersonCanPutBreakpoint();
	
	/** If there is a java.exe or whatever kind of system dependent executable this wraps, return it, else null.
	This does not tell the params used with it.
	*/
	public File compilerExecutable();
	
	/** Returns the dir where .java files are generated so they can be used in Eclipse or Netbeans etc in debug mode
	with breakpoints chosen by Human user with the mouse. Else null if it doesnt generate such files.
	*/
	public File classpathTo();
	
	/** If java, then its 1 part for class, field, or function. See my javassist code for example of using String[] that way. */
	public Class compile(String... classParts);

}