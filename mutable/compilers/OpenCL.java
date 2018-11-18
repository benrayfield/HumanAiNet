/** Ben F Rayfield offers this software opensource MIT license */
package mutable.compilers;
import java.util.regex.Pattern;

import org.lwjgl.LWJGLException;
import org.pushingpixels.substance.internal.utils.SubstanceStripingUtils;

import immutable.util.Text;
import mutable.opencl.connectors.lwjgl.Lwjgl;

public class OpenCL{
	private OpenCL(){}
	
	/** TODO returns same size Object[] as params,
	with those that were written first copied (not modify params) and the others reused.
	*/
	public static synchronized Object[] callOpencl(String kernelCode, int[] ndRange, Object... params){
	//public static synchronized void callOpencl(String kernelCode, int[] ndRange, Object[] paramsRead, Object[] paramsWrite){
		try{
			return Lwjgl.instance().callOpencl(kernelCode, ndRange, params);
			//Lwjgl.instance().callOpencl(kernelCode, ndRange, paramsRead, paramsWrite);
		}catch(LWJGLException e){ throw new Error(e); }
	}
	
	public static String findKernelName(String kernelCode){
		int firstLparen = kernelCode.indexOf('(');
		if(firstLparen == -1) throw new Error("No lparen in "+kernelCode);
		String endsWithName = kernelCode.substring(0,firstLparen).trim();
		String[] tokens = endsWithName.split("\\s+");
		return tokens[tokens.length-1];
	}
	
	/** whats between the first ( and ).
	Example: "global const float* a, global const float* b, global float* result, int const size"
	*/
	public static String getParamsString(String kernelCode){
		int start = kernelCode.indexOf('(');
		if(start == -1) throw new Error("No ( found so must not be opencl kernel code: "+kernelCode);
		int end = kernelCode.indexOf(')',start);
		if(end == -1) throw new Error("No ) found after the ( so must not be opencl kernel code: "+kernelCode);
		return kernelCode.substring(start+1, end).trim();
	}
	
	public static int countIntParams(String kernelCode){
		return count("int ", getParamsString(kernelCode));
	}
	
	/** Includes params that are read and written */ 
	public static int countFloat1dParams(String kernelCode){
		return count("float* ", getParamsString(kernelCode));
	}
	
	//static final Pattern splitComma = Pattern.compile("\\s*\\,\\s*");
	
	//static final Pattern splitWhitespace = Pattern.compile("\\s+");
	
	/** Every param is either read only or write only. FIXME Is that always true?
	Array is same size as number of params.
	*/
	public static boolean[] openclWritesParams(String kernelCode){
		String s = getParamsString(kernelCode); //is trimmed
		String[] sa = s.split("\\s*\\,\\s*");
		boolean[] write = new boolean[sa.length];
		for(int p=0; p<sa.length; p++){
			String[] paramTokens = sa[p].trim().split("\\s+"); //Example:
			//Example: ["global","const","float*","a"]
			//Example: ["global","float*","result"]
			//Example: ["int","const","size"]
			//boolean foundGlobal = false, foundLocal = false, foundConst = false;
			boolean foundConst = false;
			for(String token : paramTokens){
				//FIXME Are __global and __const also valid keywords? What are all the relevant keywords and alias of them?
				//if("global".equals(token)) foundGlobal = true;
				//if("local".equals(token)) foundLocal = true;
				if("const".equals(token)) foundConst = true;
			}
			write[p] = !foundConst;
		}
		return write;
	}
	
	static int count(String find, String inMe){
		int count = 0, i = 0;
		while(true){
			i = inMe.indexOf(find,i);
			if(i == -1) return count;
			count++;
		}
	}
	
	public static float[] array2dTo1d(float[][] in){
		int b = in.length, c = in[0].length;
		float[] out = new float[b*c];
		for(int i=0; i<b; i++){
			System.arraycopy(in[i], 0, out, i*c, c);
		}
		return out;
	}
	
	/** returns a float[firstDim][in.length/firstDim] where in.length%firstDim==0 */
	public static float[][] array1dTo2d(float[] in, int firstDim){
		int secondDim = in.length/firstDim;
		if(firstDim*secondDim != in.length) throw new Error(in.length+" not divisible by "+firstDim);
		float[][] out = new float[firstDim][secondDim];
		for(int i=0; i<firstDim; i++){
			System.arraycopy(in, i*secondDim, out[i], 0, secondDim);
		}
		return out;
	}
	
	public static String newKernelName(){
		return Text.newJibberishWord(Math.pow(2, 128));
	}
	
	/** TODO check which params are read and written in kernel code and optimize by using
	CLMem thats only readable, only writable, or both, and in java only copy those modified,
	but for now copy all params since not checking which could be modified.
	*
	public Object[] copyAllParams(Object... params){
		
	}
	
	public Object copy(Object o){
		
	}*/
	
	public static float[][] copyRectArray(float[][] a){
		int innerSize = a[0].length;
		float[][] b = new float[a.length][innerSize];
		for(int i=0; i<a.length; i++){
			System.arraycopy(a[i], 0, b[i], 0, innerSize);
		}
		return b;
	}

}