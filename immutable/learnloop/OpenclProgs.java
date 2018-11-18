/** Ben F Rayfield offers this software opensource MIT license */
package immutable.learnloop;

import mutable.opencl.OpenclUtil;

/** You can of course use the more general OpenclUtil.callOpencl(String,Object...),
but these are just for convenience of calling some things as normal java funcs.
*/
public class OpenclProgs{
	
	/** given float[b][c] and float[c][d] returns float[b][d] */
	public static synchronized float[][] matmul(float[][] bc, float[][] cd){
		int bSize = bc.length, cSize = bc[0].length, dSize = cd[0].length;
		if(cd.length != cSize) throw new Error("Sizes dont match");
		//FIXME verify sizes match and are rectangle arrays
		float[] bd1d = matmul(bSize, cSize, dSize, OpenclUtil.array2dTo1d(bc), OpenclUtil.array2dTo1d(cd));
		return OpenclUtil.array1dTo2d(bd1d,bSize);
	}
	
	static boolean isFirstCallOf_matmulThenSigmoid = true;
	
	/** lower lag than doing multiple opencl calls using matmul first */
	public static synchronized float[][] matmulWithBiasThenSigmoid(float[][] bias, float[][] bc, float[][] cd){
		if(isFirstCallOf_matmulThenSigmoid){ //FIXME testing a theory that opencl needs to run it once which has nans and later it works?
			isFirstCallOf_matmulThenSigmoid = false;
			matmulWithBiasThenSigmoid(bias,bc,cd);
		}
		int bSize = bc.length, cSize = bc[0].length, dSize = cd[0].length;
		if(cd.length != cSize) throw new Error("Sizes dont match");
		if(bias.length != bc.length || bias[0].length != cd[0].length) {
			throw new Error("Sizes dont match");
		}
		//FIXME verify sizes match and are rectangle arrays
		float[] bd1d = matmulWithBiasThenSigmoid(OpenclUtil.array2dTo1d(bc), bSize, cSize, dSize, OpenclUtil.array2dTo1d(bc), OpenclUtil.array2dTo1d(cd));
		return OpenclUtil.array1dTo2d(bd1d,bSize);
	}
	
	public static synchronized float[][] matmulWithBiasThenSigmoid_usingCpu(float[] bias, float[][] bc, float[][] cd){
		/*"kernel void "+OpenclUtil.newKernelName()+"(global const float* bias, int const bSize, int const cSize, int const dSize, global const float* bc, global const float* cd, global float* bdOut){\r\n"+
				"	int bd = get_global_id(0);\r\n"+
				"	const int b = bd/dSize;\r\n"+ //TODO optimize allow get_global_id(more dims)?//
				"	const int d = bd%dSize;\r\n"+ //TODO optimize allow get_global_id(more dims)?
				"	float sum = bias[bd];\r\n"+
				"	for(int c=0; c<cSize; c++){\r\n"+
				"		sum += bc[b*cSize+c]*cd[c*dSize+d];\r\n"+ //TODO optimize allow get_global_id(more dims)?
				"	}\r\n"+
				"	float chance = 1/(1+exp(-sum));\r\n"+
				"	bdOut[bd] = chance;\r\n"+
				"}";
		*/
		throw new Error("TODO");
	}
	
	/** lower lag than doing multiple opencl calls using matmul first */
	public static synchronized float[][] matmulThenSigmoidThenWeightedCoinFlip(float[][] bias, float[][] bc, float[][] cd){
		int bSize = bc.length, cSize = bc[0].length, dSize = cd[0].length;
		if(cd.length != cSize) throw new Error("Sizes dont match");
		if(bias.length != bc.length || bias[0].length != cd[0].length) {
			throw new Error("Sizes dont match");
		}
		//FIXME verify sizes match and are rectangle arrays
		float[] bd1d = matmulThenSigmoidThenWeightedCoinFlip(OpenclUtil.array2dTo1d(bias), bSize, cSize, dSize, OpenclUtil.array2dTo1d(bc), OpenclUtil.array2dTo1d(cd));
		return OpenclUtil.array1dTo2d(bd1d,bSize);
	}
	
	/** bc.length==bSize*cSize && cd.length==cSize*dSize */
	public static synchronized float[] matmul(int bSize, int cSize, int dSize, float[] bc, float[] cd){
		Object[] out = OpenclUtil.callOpencl(
			
			//FIXME slower, try this until get the right answer then start using matmulCode1dAs2d instead and make that work
			matmulCode1dAs2d, new int[]{bSize*dSize},
			
			//FIXME This gets about 3.5 gflops on my 4x1.6GhzLaptop, while the other only about 2. Both give wrong answer,
			//this one gives 0 and other one gives it appears 1 of the input numbers, so I'm going back to the slower 1d one
			//while I fix that then come back to this for speed if I can
			//matmulCode2d, new int[]{bSize, dSize},
			
			bSize, cSize, dSize, bc, cd, new float[bSize*dSize]);
		return (float[]) out[out.length-1];
	}
	
	/** bc.length==bSize*cSize && cd.length==cSize*dSize */
	public static synchronized float[] matmulWithBiasThenSigmoid(float[] bias, int bSize, int cSize, int dSize, float[] bc, float[] cd){
		Object[] out = OpenclUtil.callOpencl(matmulCode1dAs2dThenSigmoid, new int[]{bSize*dSize},
			bias, bSize, cSize, dSize, bc, cd, new float[bSize*dSize]);
		return (float[]) out[out.length-1];
	}
	
	/** bc.length==bSize*cSize && cd.length==cSize*dSize */
	public static synchronized float[] matmulThenSigmoidThenWeightedCoinFlip(float[] bias, int bSize, int cSize, int dSize, float[] bc, float[] cd){
		Object[] out = OpenclUtil.callOpencl(matmulCode1dAs2dThenSigmoidThenWeightedCoinFlip, new int[]{bSize*dSize},
			bias, bSize, cSize, dSize, bc, cd, new float[bSize*dSize]);
		return (float[]) out[out.length-1];
	}
	
	public static final String matmulCode1dAs2d =
		"kernel void "+OpenclUtil.newKernelName()+"(int const bSize, int const cSize, int const dSize, global const float* bc, global const float* cd, global float* bdOut){\r\n"+
		"	int bd = get_global_id(0);\r\n"+
		//"	if(bd < size) {\r\n"+
		"		const int b = bd/dSize;\r\n"+ //TODO optimize allow get_global_id(more dims)?//
		"		const int d = bd%dSize;\r\n"+ //TODO optimize allow get_global_id(more dims)?
		//"		if(itemId < size){\r\n"+ //FIXME why did Lwjgl sample code do this? They have indexing problems?
		"		float sum = 0;\r\n"+
		"		for(int c=0; c<cSize; c++){\r\n"+
		"			sum += bc[b*cSize+c]*cd[c*dSize+d];\r\n"+ //TODO optimize allow get_global_id(more dims)?
		"		}\r\n"+
		//"		bdOut[bd] = (float)b;\r\n"+
		//"		bdOut[bd] = bd*10;\r\n"+
		//"		bdOut[bd] = 18;\r\n"+
		//"		bdOut[bd] = cSize;\r\n"+
		"		bdOut[bd] = sum;\r\n"+
		//"		result[itemId] = a[itemId] + b[itemId];\r\n"+
		//"	}\r\n"+
		"}";
	
	public static final String matmulCode1dAs2dThenSigmoid =
		"kernel void "+OpenclUtil.newKernelName()+"(global const float* bias, int const bSize, int const cSize, int const dSize, global const float* bc, global const float* cd, global float* bdOut){\r\n"+
		"	int bd = get_global_id(0);\r\n"+
		"	const int b = bd/dSize;\r\n"+ //TODO optimize allow get_global_id(more dims)?//
		"	const int d = bd%dSize;\r\n"+ //TODO optimize allow get_global_id(more dims)?
		"	float sum = bias[bd];\r\n"+
		"	for(int c=0; c<cSize; c++){\r\n"+
		"		sum += bc[b*cSize+c]*cd[c*dSize+d];\r\n"+ //TODO optimize allow get_global_id(more dims)?
		"	}\r\n"+
		"	float chance = 1/(1+exp(-sum));\r\n"+
		"	bdOut[bd] = chance;\r\n"+
		"}";
	
	public static final String matmulCode1dAs2dThenSigmoidThenWeightedCoinFlip =
		"kernel void "+OpenclUtil.newKernelName()+"(global const float* bias, int const bSize, int const cSize, int const dSize, global const float* bc, global const float* cd, global float* bdOut){\r\n"+
		"	int bd = get_global_id(0);\r\n"+
		"	const int b = bd/dSize;\r\n"+ //TODO optimize allow get_global_id(more dims)?//
		"	const int d = bd%dSize;\r\n"+ //TODO optimize allow get_global_id(more dims)?
		"	float sum = bias[bd];\r\n"+
		"	for(int c=0; c<cSize; c++){\r\n"+
		"		sum += bc[b*cSize+c]*cd[c*dSize+d];\r\n"+ //TODO optimize allow get_global_id(more dims)?
		"	}\r\n"+
		"	float chance = 1/(1+exp(-sum));\r\n"+
		"	float randFraction = fmod(fabs(sum)*49999,1);\r\n"+
		"	float weightedCoinFlip = fmax(0,ceil(chance-randFraction));\r\n"+
		"	bdOut[bd] = weightedCoinFlip;\r\n"+
		"}";
	
	/*public static final String matmulCode1dAs2d =
		"kernel void "+OpenclUtil.newKernelName()+"(int const bSize, int const cSize, int const dSize, global const float* bc, global const float* cd, global float* bdOut){\r\n"+
		"	const int bd = get_global_id(0);\r\n"+
		"	const int b = bd/dSize;\r\n"+ //TODO optimize allow get_global_id(more dims)?
		"	const int d = bd%dSize;\r\n"+ //TODO optimize allow get_global_id(more dims)?
		"	int bcOffset = b*cSize;\r\n"+
		"	int cdOffset = d;\r\n"+
		//"	if(itemId < size){\r\n"+ //FIXME why did Lwjgl sample code do this? They have indexing problems?
		"	float sum = 0;\r\n"+
		"	for(int c=0; c<cSize; c++){\r\n"+
		"		sum += bc[bcOffset]*cd[cdOffset];\r\n"+
		"		bcOffset++;\r\n"+
		"		cdOffset += dSize;\r\n"+
		//"		sum += bc[b*cSize+c]*cd[c*dSize+d];\r\n"+ //TODO optimize allow get_global_id(more dims)?
		"	}\r\n"+
		"	bdOut[bd] = sum;\r\n"+
		//"		result[itemId] = a[itemId] + b[itemId];\r\n"+
		//"	}\r\n"+
		"}";
	*/
	
	/*public static final String matmulCode2d =
		//	FIXME verify the indexs are right
		"kernel void "+OpenclUtil.newKernelName()+"(int const bSize, int const cSize, int const dSize, global const float* bc, global const float* cd, global float* bdOut){\r\n"+
		"	int b = get_global_id(0);\r\n"+
		"	int d = get_global_id(1);\r\n"+
		//"	int d = b;\r\n"+ //FIXME
		//"	float sum = 0;\r\n"+
		//"	int bcOffset = b*cSize;\r\n"+
		//"	int cdOffset = d;\r\n"+
		//"	for(int c=0; c<cSize; c++){\r\n"+
		//"		sum += bc[bcOffset]*cd[cdOffset];\r\n"+
		//"		bcOffset++;\r\n"+
		//"		cdOffset += dSize;\r\n"+
		//"	}\r\n"+
		"	bdOut[b*dSize+d] = 1;\r\n"+
		"}";*/
	
	public static float[][] matmulCpu(float[][] bc, float[][] cd){
		int B = bc.length;
		int C = bc[0].length;
		int D = cd[0].length;
		//FIXME verify sizes match and are rectangle arrays
		float[][] bd = new float[B][D];
		for(int b=0; b<B; b++){
			for(int d=0; d<D; d++){
				float sum = 0;
				for(int c=0; c<C; c++){
					sum += bc[b][c]*cd[c][d];
				}
				bd[b][d] = sum;
			}
		}
		return bd;
	}
	
	public static final String simpleTest =
		"kernel void "+OpenclUtil.newKernelName()+"(global const float* in, global float* out, int const size){\r\n"+
		"	const int i = get_global_id(0);\r\n"+
		//"	if(0 <= i && i < size){\r\n"+
		"		const float b = in[i];\r\n"+
		//"		out[i] = (b*4);\r\n"+
		//"		float r = (float)size;\r\n"+
		"		const float r = (b*2);\r\n"+
		"		out[i] = r;\r\n"+
		//"	}\r\n"+
		"}";
	
	public static void main(String[] args){
		int size = 20;
		float[] a = new float[size], b = new float[size];
		for(int i=0; i<size; i++){
			a[i] = 1000+i;
			b[i] = 2000+i;
		}
		Object[] outs = OpenclUtil.callOpencl(simpleTest, new int[]{size}, a, b, size);
		float[] out = (float[])outs[1];
		for(int i=0; i<size; i++){
			System.out.println("out["+i+"]="+out[i]+" (intbits)"+Float.floatToIntBits(out[i]));
		}
	}
	

}
