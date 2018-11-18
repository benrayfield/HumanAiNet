/** Ben F Rayfield offers this software opensource MIT license */
package mutable.opencl.connectors.lwjgl;

import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.Util;

public class CompiledKernel{
	
	public final String code;
	
	public final CLProgram prog;
	
	public final CLKernel kernel;
	
	/** normally generated randomly. FIXME is this part of String code? */
	public final String kernelName;
	
	public final int error;
	
	public CompiledKernel(String code, CLProgram prog, CLKernel kernel, String kernelName, int error){
		this.code = code;
		this.prog = prog;
		this.kernel = kernel;
		this.kernelName = kernelName;
		this.error = error;
	}

}
