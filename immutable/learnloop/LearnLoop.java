/** Ben F Rayfield offers this software opensource MIT license */
package immutable.learnloop;
import static mutable.util.Lg.*;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

//import diagonode.plugins.javalambda.JavassistUtil;
import immutable.occamsjsonds.TestJsonDS;
import immutable.util.HashUtil;
import immutable.util.Text;
import mutable.compilers.JavaCompiler;
import immutable.learnloop.WeightAndVelOfEdlay;
import mutable.rbm.func.LearnLoopParam_OLD;
import mutable.util.JavaUtil;
import mutable.util.Lg;

/** Very customizable (by subset of possible java code, optimized by javassist and maybe later by opencl)
learning algorithm for an edlay, replacing what used to be done by RBM.vecsLearnToEdlay func.
TODO verify, this will be sandboxed so can be called with new learning algorithms safely by untrusted code.
The code will be written without []s which will be added automaticly,
so just write it as body of inner inner loop (of doubleLoop).
<br><br>
OLD...
Has many of the same field names as LearnLoopParam, except they're more often
float[] instead of float. This is precomputed before a doubleLoop over all
(lowNodeIndex,highNodeIndex) in an edlay. Then using this and a LearnLoopParam,
a javassist compiled func (similar to the Consumer<LearnLoopParam>'s)
does the doubleLoop, copying from this cache into LearnLoopParam
and getting its return values such as from LearnLoopParam.returnWevel
and LearnLoopParam.returnWeight, and copies those into a float[][].
<br><br>
This is needed cuz the RBM func...
public static WeightAndVelOfEdlay vecsLearnToEdlay(String learnFunc, int edlay, float learnRate, float unlearnRate, float[][] weight, float[][] weightVel,
float[] lowNodeAtt, float[] highNodeAtt,
float[][] vecsLowNodesToLearn, float[][] vecsHighNodesToLearn,
float[][] vecsLowNodesToUnlearn, float[][] vecsHighNodesToUnlearn, float weightDecay, float weightVelDecay)\
...cuz that func would be inefficient to branch on which of the params are being used,
and would be inefficient to compute params that are not being used in a certain learn func.
<br><br>
TODO maybe the doubleLoop javassist compiled func should be merged with the
similar Consumer<LearnLoopParam> func instead of having 2 such funcs.
TODO should it be merged? Try it merged with the caching/preparing done in static funcs
and the main work is in the doubleLoop. The code will be written the same as if it was
just an inner loop body and the []s will be added automaticly.
*/
public abstract class LearnLoop implements Runnable{
	
	//The following are caches that would be used inside RBM.vecsLearnToEdlay (if that func was finished there instead of here).
	//Some of these will be filled as temp calculations by run().
	
	/** size of low nolay */
	public int lows;
	
	/** size of high nolay */
	public int highs;
	
	/** number of vectors learned at once,
	normally randomly selected from recent vectors such as in rbm.Slide or the older code in PaintSlidingVecUi.
	Common values for this are 50-1000 for GPU (about 5 times per second), or 1 for CPU (about 30 times per second)
	on a 4x1.6GhzLaptop, though I havent optimized the GPU as well as I could
	(such as with float4 or maybe LearnLoop is a bottleneck should be gpu optimized too?).
	*/
	public int batchSize;
	
	public float[] tohighWeightAveNoatt;
	public float[] tolowWeightAveNoatt;
	
	public float[] tohighWeightDevNoatt;
	public float[] tolowWeightDevNoatt;
	
	public float[] tolowWeightMedNoatt;
	public float[] tohighWeightMedNoatt;
	
	public float[] tolowWeightMin;
	public float[] tohighWeightMin;
	public float[] tolowWeightMax;
	public float[] tohighWeightMax;
	
	public float[] tolowWeightAveByatt;
	public float[] tohighWeightAveByatt;
	
	public float[] lowAttsum;
	public float[] highAttsum;
	
	public float[] tolowWeightDevByatt;
	public float[] tohighWeightDevByatt;
	
	public float[] tolowWeightMedByatt;
	public float[] tohighWeightMedByatt;
	
	//The following were params of RBM.vecsLearnToEdlay and should be filled before calling run().
	
	public String learnFunc;
	
	public int edlay;
	
	public float learnRate;
	
	public float unlearnRate;
	
	public float[] tolowNodeAtt;
	
	public float[] tohighNodeAtt;
	
	public float[][] vecsLowNodesToLearn;
	
	public float[][] vecsHighNodesToLearn;
	
	public float[][] vecsLowNodesToUnlearn;
	
	public float[][] vecsHighNodesToUnlearn;
	
	public float weightDecay;
	
	public float wevelDecay;
	
	/** weight position */
	public float[][] theWeight;
	
	//"FIXME this and PaintSlidingVecUi's learnFunc var need these string replacements made consistent with eachothers substrings"
	//Doing that in *_chooseSetOfLearnloopParamsWhereNoneIsPrefixOfAnotherAndAreEnoughToEmulateTheEarlierRbmCodeThatLearnedVeryAccuratelyBeforeLearnloop
	
	/** weight velocity */
	public float[][] theWevel;
	
	//public WeightAndVelOfEdlay returnWeightAndVelOfEdlay;
	public float[][] returnWeight;
	
	/** WHY ITS NAMED WEVEL (WEIGHT VELOCITY): 
	Since many vars are about stats of weight, not weightvelocity, I dont want to
	have to rename all of them to weightpos (weight position) to avoid substring substitution conflicts
	with weightvel (weight velocity), so choose a name for weightvel that weight is not a substring of,
	like wevel.
	*/
	public float[][] returnWevel;

	/** For efficiency of only computing the arrays used by each variation of learning algorithm,
	this will be overridden in a subclass by javassist andOr opencl.
	After calling run(), read returnWeightAndVelOfEdlay. This func should fill many of the instance fields as temp calculations.
	*/
	public abstract void run();
	
	/** reuses runtime compiled Class fast on future calls.
	If !debuggable, the default is Javassist (or maybe Beanshell for first n runs before that compiling finishes).
	FIXME TODO If debuggable, the default is to look for JDK on harddrive and compile it much slower then put the code into
	whatever IDE (such as eclipse or netbeans) so you can put breakpoints in it.
	TODO I wrote code code in codesimian long ago which compiled like that, so bring that in.
	*/
	public static synchronized LearnLoop newInstanceSandboxed(String learnFunc, JavaCompiler compiler){
		Lg.todo("FIXME LearnLoop isnt sandboxed yet. Dont run untrusted code in it else your computer is in danger.");
		lg("learnFunc="+learnFunc);
		Class<? extends LearnLoop> compiled = LearnLoop.compileSandboxed(learnFunc,compiler); //returns same Class fast on future calls
		LearnLoop loop = null;
		try{
			return compiled.newInstance();
		}catch(Exception e){
			throw new Error(e);
		}
	}

	/** TODO or return from cache */
	public static synchronized Class<? extends LearnLoop> compileSandboxed(String learnFunc, JavaCompiler compiler){
		lgErr("FIXME LearnLoop.compileSandboxed is not yet proven sandboxed completely.");
		
		Class<? extends LearnLoop> ret = learnFuncCodeToClass.get(learnFunc);
		if(ret != null) return ret;
		
		StringBuilder code = new StringBuilder();
		
		boolean computetohighWeightAveNoatt = learnFunc.contains("tohighWeightAveNoatt");
		boolean computetohighWeightDevNoatt = learnFunc.contains("tohighWeightDevNoatt");
		boolean computetohighWeightMedNoatt = learnFunc.contains("tohighWeightMedNoatt");
		boolean computetolowWeightAveNoatt = learnFunc.contains("tolowWeightAveNoatt");
		boolean computetolowWeightDevNoatt = learnFunc.contains("tolowWeightDevNoatt");
		boolean computetolowWeightMedNoatt = learnFunc.contains("tolowWeightMedNoatt");
		
		boolean computetohighWeightAveByatt = learnFunc.contains("tohighWeightAveByatt");
		boolean computetohighWeightDevByatt = learnFunc.contains("tohighWeightDevByatt");
		boolean computetohighWeightMedByatt = learnFunc.contains("tohighWeightMedByatt");
		boolean computetolowWeightAveByatt = learnFunc.contains("tolowWeightAveByatt");
		boolean computetolowWeightDevByatt = learnFunc.contains("tolowWeightDevByatt");
		boolean computetolowWeightMedByatt = learnFunc.contains("tolowWeightMedByatt");
		
		boolean computetohighWeightsMin = learnFunc.contains("tohighWeightMin");
		boolean computetolowWeightMin = learnFunc.contains("tolowWeightMin");
		boolean computetohighWeightMax = learnFunc.contains("tohighWeightMax");
		boolean computetolowWeightMax = learnFunc.contains("tolowWeightMax");
		
		//Probably if you use toLearn or toUnlearn you use both, but for consistency I'm doing this per field
		boolean computeTolearn = learnFunc.contains("toLearn");
		boolean computeTounlearn = learnFunc.contains("toUnlearn");
		
		//TODO? This is a doubleloop in cpu so probably wont bottleneck gpu doing tripleloop,
		//but if it does then do this in gpu and pay the extra lag.
		
		//FIXME check the compute* vars
		//FIXME set the LearnLoopParam.* fields if those compute* vars say to. This code is incomplete as of 2018-6-4-10a.
		boolean aveAndDevNoatt =
			computetohighWeightAveNoatt ||
			computetohighWeightDevNoatt ||
			computetolowWeightAveNoatt ||
			computetolowWeightDevNoatt;
		boolean aveAndDevByatt =
			computetohighWeightAveByatt ||
			computetohighWeightDevByatt ||
			computetolowWeightAveByatt ||
			computetolowWeightDevByatt;
		
		boolean randTest = false;
		String n = Text.n;
		//code.append("public void run(){"+n);
		if(aveAndDevNoatt){
			code.append("	aveAndDevNoatt();"+n);
		}
		
		//TODO optimize lag, this (vecsLearnToEdlay) func is called in sequence for edlays but they could be done in parallel,
		//and so could the 2 calls of matmul here.
		//Older code: p.toLearn = learnLowHigh[low][high]; p.toUnlearn = unlearnLowHigh[low][high];
		code.append("//FIXME fill vecsLowNodesToUnlearn etc"+n);
		if(computeTolearn){
			code.append("float[][] toLearn = "+OpenclProgs.class.getName()+".matmul("+RBM.class.getName()+".swapDims(vecsLowNodesToLearn), vecsHighNodesToLearn);"+n);
		}
		if(computeTounlearn){
			code.append("float[][] toUnlearn = "+OpenclProgs.class.getName()+".matmul("+RBM.class.getName()+".swapDims(vecsLowNodesToUnlearn), vecsHighNodesToUnlearn);"+n);
		}
		//TODO compute toLearn and toUnlearn vars from this. This is independent of attention
		//and normally is multiplied by that but you can explore whatever sequential code variations at runtime.
		
		code.append("	returnWeight = new float[lows][highs];"+n);
		code.append("	returnWevel = new float[lows][highs];"+n);
		code.append("	for(int low=0; low<lows; low++){"+n);
		code.append("		for(int high=0; high<highs; high++){ //TODO if this is a bottleneck then do parts in lwjgl opencl but IO and compute is same bigO (unlike tripleloop compute for doubleloop IO in matmul)"+n);
		//TODO include (String)learnFunc here, sandboxed
		if(randTest){
			code.append("			returnWeight[low][high] = util.Rand.strongRand.nextFloat()*2-1;"+n);
		}else{
			String t = learnFunc;//transform learnFunc by adding [indexVarName] to vars.
			//TODO indent
			//FIXME throw if contains string literal, which isnt a security hole but will replace code even in the string.
			
			t = t.replace("tohighWeightAveNoatt", "tohighWeightAveNoatt[high]");
			t = t.replace("tohighWeightDevNoatt", "tohighWeightDevNoatt[high]");
			t = t.replace("tohighWeightMedNoatt", "tohighWeightMedNoatt[high]");
			t = t.replace("tolowWeightAveNoatt", "tolowWeightAveNoatt[low]");
			t = t.replace("tolowWeightDevNoatt", "tolowWeightDevNoatt[low]");
			t = t.replace("tolowWeightMedNoatt", "tolowWeightMedNoatt[low]");
			
			t = t.replace("tohighWeightAveByatt", "tohighWeightAveByatt[high]");
			t = t.replace("tohighWeightDevByatt", "tohighWeightDevByatt[high]");
			t = t.replace("tohighWeightMedByatt", "tohighWeightMedByatt[high]");
			t = t.replace("tolowWeightAveByatt", "tolowWeightAveByatt[low]");
			t = t.replace("tolowWeightDevByatt", "tolowWeightDevByatt[low]");
			t = t.replace("tolowWeightMedByatt", "tolowWeightMedByatt[low]");
			
			t = t.replace("tohighWeightsMin", "tohighWeightsMin[high]");
			t = t.replace("tolowWeightsMin", "tolowWeightMin[low]");
			t = t.replace("tohighWeightsMax", "tohighWeightMax[high]");
			t = t.replace("tolowWeightsMax", "tolowWeightMax[low]");
			
			t = t.replace("theWeight", "theWeight[low][high]");
			t = t.replace("theWevel", "theWevel[low][high]");
			t = t.replace("returnWeight", "returnWeight[low][high]");
			t = t.replace("returnWevel", "returnWevel[low][high]");
			
			t = t.replace("tolowNodeAtt", "tolowNodeAtt[low]");
			t = t.replace("tohighNodeAtt", "tohighNodeAtt[high]");
			
			t = t.replace("toLearn", "toLearn[low][high]");
			t = t.replace("toUnlearn", "toUnlearn[low][high]");
			
			StringBuilder untrustedCodeSB = new StringBuilder();			
			for(String line : Text.lines(t)){
				untrustedCodeSB.append(n+"\t\t\t").append(line);
			}
			String untrustedCode = untrustedCodeSB.toString();
			//SANDBOX. FIXME TODO verify that sandboxes it all the needed ways (which it doesnt yet 2018-7-20
			//sandbox com and mem, just supposedly prevents it from touching the world outside the emulator).
			String sandboxedCode = LearnLoopParam_OLD.sandboxElseThrow(untrustedCode);
			code.append(sandboxedCode).append(n);
		}
		code.append("		}"+n);
		code.append("	}"+n);
		//code.append("}");
		String c = code.toString();
		lg("LearnLoop transformed code (FIXME sandbox):\r\n"+c);		
		
		//TODO use javassist similar to LearnLoopParam_OLD.compileSandboxed andOr
		//in future versions if this becomes a bottleneck then also do OpenclUtil.callOpencl for some parts of this,
		//but probably this wont be a bottleneck cuz its a doubleLoop while matrixMultiply is a tripleLoop.
		//Also TODO try float4 in opencl for matrix multiply optimization, like in AMD sample code.
		String packageName = "immutable.generated";
		String simpleClassName = "LearnLoop_"+Text.bytesToHex(HashUtil.sha256(Text.stringToBytes(learnFunc)));
		String className = packageName+"."+simpleClassName;
		String[] classParts = {
			"package "+packageName+";",
			"public class "+simpleClassName+" extends "+LearnLoop.class.getName()+"{",
			"public void run(){"+n+c+n+"}",
			"}"
		};
		ret = compiler.compile(classParts);
		//ret = JavassistUtil.newClass(classParts);
		
		learnFuncCodeToClass.put(learnFunc, ret);
		return ret;
	}
	
	/** FIXME garbcol these classes ever? Or just let them build up until the max of about
	64k classes (or less if they use too much of certain areas of memory?) is reached? Could happen if AI
	or other untrusted code is generating them, which is the plan.
	*/
	static final Map<String,Class<? extends LearnLoop>> learnFuncCodeToClass = new HashMap();
	
	/*"TODO make all params of vecsLearnToEdlay be instancevars? Depends what might want to recompute reusing the shared previous calculations?"
	"Nothing is reused cuz this only runs once per batch, to learn about that batch."
	"So theres no reason any func needs to have "
	*/
	
	protected void aveAndDevNoatt(){
		int lows = theWeight.length, highs = theWeight[0].length;
		tohighWeightAveNoatt = new float[lows];
		tolowWeightAveNoatt = new float[highs];
		for(int low=0; low<lows; low++){
			for(int high=0; high<highs; high++){
				float w = theWeight[low][high];
				tohighWeightAveNoatt[low] += w;
				tolowWeightAveNoatt[high] += w;
			}
		}
		for(int low=0; low<lows; low++) tohighWeightAveNoatt[low] /= lows;
		for(int high=0; high<highs; high++) tolowWeightAveNoatt[high] /= highs;
		tohighWeightDevNoatt = new float[lows];
		tolowWeightDevNoatt = new float[highs];
		for(int low=0; low<lows; low++){
			for(int high=0; high<highs; high++){
				float w = theWeight[low][high];
				float lowDiffNoatt = w-tohighWeightAveNoatt[low];
				tohighWeightDevNoatt[low] += lowDiffNoatt*lowDiffNoatt;
				float highDiffNoatt = w-tolowWeightAveNoatt[high];
				tolowWeightDevNoatt[high] += highDiffNoatt*highDiffNoatt;
			}
		}
		for(int low=0; low<lows; low++) tohighWeightDevNoatt[low] = (float)Math.sqrt(tohighWeightDevNoatt[low]/lows);
		for(int high=0; high<highs; high++) tolowWeightDevNoatt[high] = (float)Math.sqrt(tolowWeightDevNoatt[high]/highs);
	}

	
	
	//"TODO do it merged. see comment of this class, last paragraph."
	
	//TODO
	 
	//"TODO plan for ufnode integration. where would it hook in?" Do this: binufnodeLearnloopparam
	
	/** copied from (incomplete) generated code *
	public void run(){
		float[][] toLearn = util.OpenclProgs.matmul(rbm.RBM.swapDims(vecsLowNodesToLearn), vecsHighNodesToLearn);
		float[][] toUnlearn = util.OpenclProgs.matmul(rbm.RBM.swapDims(vecsLowNodesToUnlearn), vecsHighNodesToUnlearn);
			returnWeight = new float[lows][highs];
			returnWevel = new float[lows][highs];
			for(int low=0; low<lows; low++){
				for(int high=0; high<highs; high++){ //TODO if this is a bottleneck then do parts in lwjgl opencl but IO and compute is same bigO (unlike tripleloop compute for doubleloop IO in matmul)

					float att = tolowNodeAtt[low]*tohighNodeAtt[high];
					float diff = learnRate*att*(toLearn[low][high]-toUnlearn[low][high]);
					float diffScaled = diff/batchSize;
					float decay = weightDecay*diffScaled*diffScaled;
					float deriv = diffScaled - decay*weightPos[low][high];
					float dt = 1;
					returnWeight[low][high]Vel = weightVel*(1-dt*weightvelDecay) + dt*deriv;
					returnWeight[low][high]Pos = weightPos[low][high] + dt*weightVel;
				}
			}

		}*/

}
