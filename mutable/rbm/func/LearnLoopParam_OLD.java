/** Ben F Rayfield offers this software opensource MIT license */
package mutable.rbm.func;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import immutable.learnloop.LearnLoop;
import immutable.util.HashUtil;
import immutable.util.Text;
import mutable.util.JavaUtil;

/** mutable param of a Consumer<LearnLoopParam> called in a doubleLoop in RBM.learn func.
Consumer<LearnLoopParam> must not modify it. The loop which calls LearnFunc modifies it
to avoid writing some of these vars inside the loop.
<br><br>
I'm fixing the confusion betweeen FROM and TO in those names by always writing tohigh
tolow fromhigh fromlow, like fromhighWeightsAveByatt is indexed by LOW nodes cuz for each
low node there is an ave (by attention) of the weights between it and high (up 1 nolay) nodes.
<br><br>
Old comment of LearnFunc interface before replaced it with Consumer<LearnParam>:
Returns acceleration of a certain RBM weight (used with neuralMomentum), during contrastiveDivergence.
This is added into RBM.weightVelocity. RBM.weightVelocity decays and changes RBM.weight.
The calculation of weightVelocity and weight are outside scope of this func.
This is an interface instead of hardcoded cuz these params can affect eachother nonlinearly.
*/
public class LearnLoopParam_OLD{
	
	public LearnLoopParam_OLD(){
		throw new Error("TODO use "+LearnLoop.class+" instead.");
	}
	
	//TODO (mindmapname) "Sparsity should only be adjusted in upward inference, and only by upwardbias, using bias per nodeUpward, and using constant biasPerNolayDownward.".
	
	//TODO (mindmapname) rbmTemperatureWillBeDirectedAndAlwaysBe1DownwardAndVariablePerEdsideUpward.
	
	/** next value of weight */
	public float returnWeight;
	
	/** next value of weightVelocity */
	public float returnWeightVel;
	
	public float weight;
	
	public float weightVel;
	
	public float batchSize;
	public float tolowNodes;
	public float tohighNodes;
	public float edlay;
	public float learnRate;
	public float tolowNodeAtt;
	public float tohighNodeAtt;
	public float toLearn; //batch sum of lowNodeState*highNodeState in the learnPositive step of contrastiveDivergence
	public float toUnlearn; //batch sum of lowNodeState*highNodeState in the learnNegative step of contrastiveDivergence
	public float weightDecay;
	public float aveTolowNodeToLearn; //ave in batch of that one node
	public float aveTolowNodeToUnlearn; //ave in batch of that one node
	public float aveTohighNodeToLearn; //ave in batch of that one node
	public float aveTohighNodeToUnlearn; //ave in batch of that one node
	
	public float targetAveTolowNode; //that one node
	public float targetDevTolowNode; //that one node
	public float targetAveTohighNode; //that one node
	/** if this stdDev (around targetAveHighNode) is big, then it means not to push toward targetAveHighNode. */ 
	public float targetDevTohighNode; //that one node
	
	public float tolowNodesAttRange; //whole nolay. attention of lowNodes is (normally randomly) selected between 1-lowNodesAttRange/2 and 1+lowNodesAttRange/2
	public float tohighNodesAttRange; //whole nolay.
	
	/** average. Ignoring attention. FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name */
	public float fromhighWeightsAveNoatt;
	
	/** stdDev. Ignoring attention. FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name */
	public float fromlowWeightsDevNoatt;
	
	/** median breaking ties by ave cuz weights can be pos or neg. Ignoring attention.
	FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name.
	*/
	public float fromlowWeightsMedNoatt;
	
	/** average.  Ignoring attention. FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name */
	public float fromlowWeightsAveNoatt;
	
	/** stdDev. Ignoring attention. FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name */
	public float fromhighWeightsDevNoatt;
	
	/** median breaking ties by ave cuz weights can be pos or neg. Ignoring attention.
	FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name.
	*/
	public float fromhighWeightsMedNoatt;
	
	/** average. Weighted by attention. FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name */
	public float fromhighWeightsAveByatt;
	
	/** stdDev. Weighted by attention. FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name */
	public float fromlowWeightsDevByatt;
	
	/** median breaking ties by ave cuz weights can be pos or neg. Weighted by attention.
	FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name.
	*/
	public float fromlowWeightsMedByatt;
	
	/** average.  Weighted by attention. FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name */
	public float fromlowWeightsAveByatt;
	
	/** stdDev. Weighted by attention. FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name */
	public float fromhighWeightsDevByatt;
	
	/** median breaking ties by ave cuz weights can be pos or neg. Weighted by attention.
	FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name.
	*/
	public float fromhighWeightsMedByatt;
	
	/** min of high weights, for a specific low node.
	FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name. 
	*/
	public float fromhighWeightsMin;
	
	/** max of high weights, for a specific low node.
	FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name. 
	*/
	public float fromhighWeightsMax;
	
	/** min of low weights, for a specific high node.
	FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name. 
	*/
	public float fromlowWeightsMin;
	
	/** max of low weights, for a specific high node.
	FIXME only loop over weights to set this if (String)RBM.learnFunc contains this var name. 
	*/
	public float fromlowWeightsMax;
	
	//FIXME "TODO choose standard for naming these vars, since it can be named by low vs high its the index of, or by which side its the set of all of"
	
	/** param of RBM.learn func.
	newWeight[edlay][lowNode][highNode] = weight[edlay][lowNode][highNode]+weightVelocity[edlay][lowNode][highNode];
	newWeightVelocity[edlay][lowNode][highNode] =
		weightVelocity[edlay][lowNode][highNode]*multVelocity + deriv[edlay][lowNode][highNode];
	*/
	public float weightVelocityDecay;
	
	
	/** 1 if low node's nolay index is 0 (value is always 1, that nolay's bias node), else 0 *
	public float lowNodeIsBias;
	
	/** 1 if high node's nolay index is 0 (value is always 1, that nolay's bias node), else 0 *
	public float highNodeIsBias;
	*/
	
	
	/*TODO? I plan to use [ave-median] if its efficient to calculate. Remember,
		median can be computed in bigo of linear time (faster than loglinear): https://en.wikipedia.org/wiki/Median_of_medians#Algorithm
		and can be computed deterministicly using min and max of small constant number of things, in an array of size sum<nonnegint>(n/5^nonnegint)
	highNodeAvebyattWeight
	highNodeDevbyattWeight
	highNodeMedianattWeight
	lowNodeAvebyattWeight
	lowNodeDevbyattWeight
	lowNodeMedianattWeight
	
	Imagine all the LearnLoopParam fields as derived from, aligned to an edlay and adjacent 2 nolays: float[][]s.
	Example: float[][] learnPositive, learnNegative, weight, weightVelocity, go on weights.
	Example: float[vec][nodeInNolay].
	Example: float[nodeInNolay] attention.
	
	TODO hook into mindmap and check for responses of:
		https://www.reddit.com/r/MLQuestions/comments/8luvuq/in_a_rbm_neuralnet_with_momentum_each_weight_has/
	*/
	
	public static boolean matches(String s, String regex){
		return Pattern.compile(regex, Pattern.DOTALL|Pattern.MULTILINE).matcher(s).matches();
	}
	
	/** FIXME verify this sandbox throws for ALL the possible nonsandboxed behaviors */
	public static String sandboxElseThrow(String codeToSandbox){
		String withoutComments = JavaUtil.removeComments(codeToSandbox);
		//not using the p. anymore since its instancevars and local vars: String withoutPDot = withoutComments.replace("p.","");
		//TODO relax these rules by not enforcing them on comments
		if(matches(withoutComments,".*((//)|(/\\*)).*")) throw new Error("has comment (after should have been auto removed, so how?), which makes it harder to prove sandboxed. code="+codeToSandbox);
		
		//Nevermind, sandbox what goes in the doubleloop of learnloop but since the rest is generated,
		//OLD: dont need to prove sandboxed of that generated code.
		//Allow loops (including infinite loops), for now. FIXME limit com and mem (basic cx, not the expensive kinds of econacyc)
		if(matches(withoutComments,".*((for)|(while))\\s*\\(.*")) throw new Error("loop. code="+codeToSandbox);
		
		if(matches(withoutComments,".*[^\\+\\*\\%\\-\\/\\=\\?\\:]+\\s*\\(.*")) throw new Error("calls function. code="+codeToSandbox);
		if(matches(withoutComments,".*new.*")) throw new Error("contains the word 'new'. code="+codeToSandbox);
		return codeToSandbox;
	}
	
	/** Param is RBM.learnFunc. See comment of that field.
	FIXME java is limited to about 2^16 classes in same ClassLoader, so make sure this (and other uses of Javassist,
	so do that in JavassistUtil.newClass func) dont exceed that before it may cause bigger problems than throwing>
	*/
	public static Consumer<LearnLoopParam_OLD> compileSandboxed(String learnFunc){
		Consumer<LearnLoopParam_OLD> compiledInstance = classNameToInstance.get(learnFunc);
		if(compiledInstance == null){
			sandboxElseThrow(learnFunc);
			String className = "LearnFunc_"+Text.bytesToHex(HashUtil.sha256(Text.stringToBytes(learnFunc)));
			Class c = null;
			try{
				c = Class.forName(className); //in case the WeakHashMap lets it be garbcoled
			}catch(ClassNotFoundException e) {

				/*FIXME this is not the whole code like how its used in RBM.java, and its saying
				/*learnFunc textarea FAIL (on CODE: float att = tolowNodeAtt*tohighNodeAtt;
				float diff = learnRate*att*(toLearn-toUnlearn);
				float diffScaled = diff/batchSize;
				float decay = weightDecay*diffScaled*diffScaled;
				float deriv = diffScaled - decay*theWeight;
				float dt = 1;
				returnWevel = theWevel*(1-dt*wevelDecay) + dt*deriv;
				returnWeight = theWeight + dt*theWevel;) cuz java.lang.RuntimeException: javassist.CannotCompileException: [source error] syntax error near "oat dt = ;"
				*
				TODO use LearnLoop.compileSandboxed, which will cache it after its compiled
				WAIT thats circularlogic. Where is the extra code that fills in toLearn etc coming from?
				No, its not circularlogic. Thats in LearnLoop, and this is the old class LearnLoopParam.
				*/
				if(1<2) throw new Error("TODO change PaintSlidingVecUi to use LearnLoop.");
				
				
				/*c = JavassistUtil.newClass(
					"class "+className+" extends java.lang.Object implements "+java.util.function.Consumer.class.getName(),
					"public void accept(java.lang.Object pAsObject){\r\n"
						+LearnLoopParam_OLD.class.getName()+" p = ("+LearnLoopParam_OLD.class.getName()+") pAsObject;\r\n"+learnFunc+"\r\n}"
				);*/
			}
			try{
				compiledInstance = (Consumer<LearnLoopParam_OLD>) c.newInstance();
				classNameToInstance.put(className, compiledInstance); //quickly reuse it next time
			}catch(Exception e){ throw new Error(e); }
		}
		return compiledInstance;
	}
	
	static final Map<String,Consumer<LearnLoopParam_OLD>> classNameToInstance = Collections.synchronizedMap(new WeakHashMap());
	
	
	
	
	
	
	
	
	
	/*UPDATE: LearnParam now has LearnParam.returnWeight and LearnParam.returnWeightVelocity fields,
	so will use Consumer<LearnParam> instead of LearnFunc.
	
	
	TODO I want a simple math expression syntax, similar to codesimian and mathevo,
	for defining a LearnFunc as something like ((toLearn-toUnlearn)#xyz * (xyz+.2)),
	especially the #name syntax which defines a forest of simple math ops without recomputing shared branches.
	LearnFunc is not allowed to call funcs cuz it must be very fast as its in a loop body processing what GPU outputs
	This code must therefore be compiled, such as by javassist, even though its only for such math expressions.
	Store such a math expression as a field in RBM.java, with getter and setter func.
	
	TODO I want some param in here, maybe something with stdDev andOr median of weights as viewed from each low node and each high node
	to an adjacent nolay, which will allow LearnFunc to adjust sparsity of weights, not just sparsity of nodes,
	similar to how in mnist ocr we often see lowest featurevecs that are affected mostly by a few small areas of the image,
	and I may want to (in the math expression) make that relative to median of such weights.
	
	TODO node bias? Should there be a different func for changing that? Or leave it constant?
	Or a boolean param to say it is a self edge or not? Some of the params wont make sense if its a self edge.
	In some learning algorithms, bias is the same as weight from any other node (ignoring which nolay its in, this nolay vs adjacent nolay),
	but in other algorithms theres different math for bias. Often, bias is set to a constant.
	*/

}
