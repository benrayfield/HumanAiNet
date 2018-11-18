/** Ben F Rayfield offers this software opensource MIT license */
package immutable.learnloop;
import static mutable.util.Lg.*;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.security.SecureRandom;
import java.util.Random;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.WeakHashMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Consumer;
import java.util.function.DoubleUnaryOperator;

import immutable.occamsjsonds.JsonDS;
import immutable.util.MathUtil;
import mutable.compilers.JavaCompiler;
import mutable.compilers.JavaCompilers;
import immutable.learnloop.WeightAndVelOfEdlay;
import immutable.learnloop.WeightAndVelOfRbm;
import immutable.util.BoltzenUtil;
//import mutable.rbm._oldOrTodo.old.LearnFunc;
import mutable.rbm.func.LearnLoopParam_OLD;
import mutable.util.Lg;
import mutable.util.Parallel;
import mutable.util.Rand;

/** Realtime, meant to be used at gaming speed such as for prediction of
mouse movements in timeWindow, so it takes only 1 data point at a time.
Immutable and ForkEditable (so you can try multiple variations in parallel),
but its callers responsibility (and could be enforced in javassist code extending lambda classes)
not to modify arrays after any other func sees it, instead to create new array
and can reuse inner arrays since its all immutable.
<br><br>
TODO thread it using ForkJoinPool, in learning thread per edlay,
and in prediction multiple threads write into their own section
of same node array per inference between 2 adjacent node layers.
Keep option to run single threaded since its probably slightly lower lag
not to have to deal with threads even if they have more compute power,
and maybe that would be used with a separate RBM per thread somehow
communicating as if they're part of the p2p network.
<br><br>
TODO copy some of the relevant comments of these fields from RbmPanel.
<br><br>
TODO After ufnode is working, use that instead of Serializable
since Serializable isnt guaranteed compatible across java versions
and isnt forkEditable in storage format.
<br><br>
Consider sandboxing individual funcs, letting them call other funcs, naming them all by hash,
locally editing them as humanReadable names in .java file (no eclipse plugin etc) but their global name
is by hash when they name other funcs by hash. Forest of funcs, so no recursion,
and in RBM code I havent yet needed recursion, though I do want it for the more general ufnode system later.
Limit total memory of the object and compute time from each call, not recursively just once,
and prove limits on that not by observing timing but by the code being less than turingComplete
such as weightedSums and backprop and statistics and triple loops etc.
I want this to be something people can build on together
without having to argue about who owns namespaces
and without having to trust downloaded code (so sandbox and limit memory and compute).
Its not a general computing system but will be general enough for forkEditable RBM and variations
of it such as float[][][] derivUp and float derivDecay and any fields can be added and removed
during forkEdit, and all funcs are separate and take Object param and return Object so
is functional not objectOriented, so its really just a namespace of fields
and a set of objects that can be stored with humanReadableNames while actually being hash named
so when ppl disagree on name of the same func in their local humanReadable namespaces
the global names still work between them.
The memory must never be more than exists in the object at the start of the call,
so its like a constant set of float[][][] and float and other fields
that can be written by forkEdit. Also funcs are stored by string of code and sandboxed.
...
I imagine a similar system for neuralMeasureHelpPredict when it goes p2p mmgMouseai,
especially the varyAttContinuouslyChosenByNeuralMeasureHelpPredict kind of neuralMeasureHelpPredict.
Such Att would be just another array field.
...
TODO after this is working better, try a second set of fastWeights and other rbmParams.
<br><br>
<br><br>
<br><br>
MOVED THESE COMMENTS FROM AI CLASS, WHEN MOVED ALL THE AI CODE HERE TOO..
<br><br>
REMEMBER TO ALWAYS USE THESE ARRAYS AS IMMUTABLE AFTER RETURNED.
<br><br>
Updated boltzmann machine energy function is sum (for all pairs of nodes x y) of:
Att[x]*chance[x]*weight[x][y]*chance[y]*Att[y].
This generalizes node state to be the multiply of Att and chance instead of just chance.
This is allowed because weightedSum is divided by boltzmann machine temperature.
<br><br>
Normal energy function:
<br><br>
sum of chance[x]*chance[y]*weight[x][y]
<br><br>
Upgraded energy function:
<br><br>
sum of Att[x]*chance[x]*Att[y]*chance[y]*weight[x][y]
<br><br>
WeightedSum for x uses
<br><br>
Att[y]*chance[y]*weight[x][y]/sumForAllY(Att[y])
<br><br>
contrastiveDivergence should multiply learnRate by Att[x]*Att[y] per weight?
<br><br>
weightedSums should be normed by dividing by total Att in the adjacent layer? This is technically an adjustment of boltzmann machine temperature.
<br><br>
//TODO? want learnrandvecui for different AttVectors partially overlapping, and maybe do them as scalars no weightedCoinFlips
//No, do the weightedCoinFlips cuz scalars would converge or diverge too much during zigzagging,
//and dont do learnrandvecui until it may be needed later,
//since the mouseai1d rbm editor (used as 2d input for training data generation) allows checking many varied inputs per second.
*/
public class RBM implements Serializable{
	private static final long serialVersionUID = 3243463457567343567L;
	
	
	/** return[zigzag][nolay][vector][node]. vecs[vector][visibleNode].
	If individualZigzagsAfterFullUp then after stat inference from lowest to highest nolay,
	each zigzag is within each edlay individually, not inferring between edlays to other edlays,
	for purpose of learning with intheory more timesymmetry (nodes having about the same value regardless of inference direction).
	For associative prediction of the visibleNodes (to use instead of train), individualZigzagsAfterFullUp is false.
	Learning can be done with individualZigzagsAfterFullUp true or false, different kinds of learning.
	<br><br>
	The number of zigzags is zigzagsLearn, even though if it is ONLY for prediction you only need zigzagsLearn-1.
	*/
	public float[][][][] predict(float[][] vecs, float[][] attsMerged, boolean individualEdlaysAfterFullUp){
		return vecsZigzag(zigzagsLearn, attsMerged, vecs, weight, lowNNolaysAreScalar, individualEdlaysAfterFullUp, biasPerNodeside);
	}
	
	/** predict[zigzag][node layer][vector][node].
	Atts[nolay*2 for down, nolay*2+1 for up][node].
	*/
	public RBM learn(float[][][][] predict, float[][] attsMerged){
		
		//Consumer<LearnLoopParam> learnFunc = learnFuncCompiled(); //sandboxed and compiled from (String) this.learnFunc
		
		WeightAndVelOfRbm wv = vecsLearnFromZigzagToWholeRbm(learnFunc, learnRatePerEdlay, attsMerged, predict, weight, weightVelocity, learnByContrastiveDivergenceSquared, weightDecay, weightVelocityDecay);
		return setWeight(wv.weight).setWeightVelocity(wv.weightVelocity);
		
		//for(float bias : biasPerNodeside) if(bias != 0) throw new Error("TODO Use every nolay index 0 as bias node, and use "+LearnLoopParam.class.getName()+"'s fields lowNodeIsBias and highNodeIsBias, and TODO update RbmPanel's ave and dev of weights to exclude nolay index 0 and to display it differently.");
		//float learnRateAdjusted = learnRateMultToMatchBoltzen*learnRate;
		//float learnRateAdjusted = learnRate;
		
		//float[] learnRates = new float[weight.length];
		//Arrays.fill(learnRates, learnRateAdjusted);
		
		
		/*for(int i=0; i<weight.length; i++){
			int scalarNolays = 0;
			if(lowNNolaysAreScalar[i]) scalarNolays++;
			if(lowNNolaysAreScalar[i+1]) scalarNolays++;
			learnRates[i] = learnRateAdjusted*(float)Math.pow(.01, scalarNolays); //FIXME this is just a guess
		}*/
		
		//This was the line of code 2018-5-5 but I'm adding weightVelocity (neural momentum) so have to change
		//what some func (will it be vecsLearnFromZigzagToWholeRbm?) returns to be a derivative instead of update to weight,
		//and forkEdit weightVelocity by adding that, and forkEdit weight (as position) to move by dt*weightVelocity (and what is dt?).
		//I would like to keep the option to add to weight directly without neural momentum, in case its not always an improvement,
		//but for that TODO I'll need another RBM param to say is it using momentum or not (or maybe use weightVelocity==null to mean that,
		//but JsonDS does not support null (by design leafs can only be String, Double, or primitive array).
		//
		//"this was the line of code":
		//return setWeight(vecsLearnFromZigzagToWholeRbm(learnRates, Atts, predict, weight, learnByContrastiveDivergenceSquared, weightDecay));
		
		//By design, weightDecay affects velocity proportional to weightDecay*weight*changeOfWeight^2
		//so thats why weight array is param even though we still have to (if not neural momentum) weight (the old code, will bring it back as an option later)
		//(or if neural momemntum, add it to weightVelocity).

		
		/*float[][][] deriv = vecsLearnFromZigzagToWholeRbm(learnRatePerEdlay, Atts, predict, weight, learnByContrastiveDivergenceSquared, weightDecay, weightVelocityDecay);
		
		FIXME learnRate*weightVelocityDecay instead of just weightVelocityDecay,
		but that would require scaling learnRate to be near 0 (and never more than 1),
		and I'm undecided what dt to use with weight = weight+dt*weightVelocity,
		such as dt=learnRate, considering that learnRate is per edlay
		(which sometimes is 0 everywhere except top edlay and sometimes is all at once).
		I could have LearnFunc write weight and weightVelocity (LearnParam.returnWeight and LearnParam.returnWeightVelocity).
		Could change vecsLearnFromZigzagToWholeRbm func to return 2 of float[][][], but I dont want to since
		it could be used to just ask RBM what would be a deriv, without actually wanting to update the weights,
		but maybe thats ok since we can ignore weightVelocity too.
		
		//float dt = TODO should dt be each changeOfWeight?; //FIXME
		float[][][] newWeight = newEmptyArraySameSizesAs(weight);
		//TODO optimize? Could store cumulative weightVelocity multiplier in a float instead of replacing weightVelocity[][][] every time,
		//but it would be a little more confusing. This is not a bottleneck so do it the simpler way at least for now.
		float[][][] newWeightVelocity = newEmptyArraySameSizesAs(weight);
		float multVelocity = 1-weightVelocityDecay;
		for(int edlay=0; edlay<weight.length; edlay++){
			int lowNodes = weight[edlay].length, highNodes = weight[edlay][0].length;
			for(int lowNode=0; lowNode<lowNodes; lowNode++){
				for(int highNode=0; highNode<highNodes; highNode++){
					newWeight[edlay][lowNode][highNode] = weight[edlay][lowNode][highNode]+weightVelocity[edlay][lowNode][highNode];
					newWeightVelocity[edlay][lowNode][highNode] =
						weightVelocity[edlay][lowNode][highNode]*multVelocity + deriv[edlay][lowNode][highNode];
				}
			}
		}
		return setWeight(newWeight).setWeightVelocity(newWeightVelocity);
		*/
		
	}
	
	
	
	/** FIXME? This isnt used in some of the newer code, but it is in LearnLoopParam and is hardcoded as .25f instead of reading it from here.
	TODO? I wanted to have a min and max allowed ave per node,
	but since I'm not keeping nodeAve statistics, instead changing weights each cycle
	based on if they are 0 or 1 ("SUCH AS IF YOU WANT .25 CHANCE OF A NODE THEN
	WHEN ITS 1, LOWER IT 3 TIMES FASTER THAN RAISING IT WHEN ITS 0."),
	target min and target max must equal.
	For visibleNodes, this should probably be the same as observed,
	or if you can adjust incoming data to always have that number of nodes on.
	*
	public final float[][] targetNodeAve;
	*/
	
	/** TODO For visibleNodes, slidingvec (a set of LearningVec being gradually learned in random set per batch)
	should write targetNodeAve[0][node] and targetNodeDev[0][node] like a DecayBell of those vecs,
	and set downward node bias of visiblenodes (somewhere in biasPerNodeside) to inverseSigmoid(targetNodeAve[0][node]).
	<br><br>
	OLD: 
	target average node value within each nolay *
	public final float[] targetNolaySparsity; "replaces targetNodeAve[][]"
	...
	where to store ave per dim of visibleNodes in slide? Does that replace the need for targetNolaySparsity at least of nolay0?
	*/
	public final float[][] targetNodeAve;
	
	/** TODO? target stdDev around targetNodeAve. If this is very big, it means not to push toward targetNodeAve */
	public final float[][] targetNodeDev;
	
	/*@Deprecated //for(float bias : biasPerNolay) if(bias != 0) throw new Error("TODO Use every nolay index 0 as bias node, and use "+LearnLoopParam+"'s fields lowNodeIsBias and highNodeIsBias, and TODO update RbmPanel's ave and dev of weights to exclude nolay index 0 and to display it differently.");
	public final float[] biasPerNolay;
	*/
	
	
	/** [nolay][nodeFrom][nodeTo]. "neural measure help predict".
	varyAttContinuouslyChosenByNeuralMeasureHelpPredict, per nolay,
	only counted between the 2 contrastiveDivergence learnPostive and learnNegative steps.
	When Att of a node x is high and Att of node y is low, thats inference from x to y more than from y to x.
	Decaying ave of precision of prediction is stored here for both directions between all pairs of nodes in each nolay.
	(the "Att PER NODE. The random neuralDropout-like kind, which can also be used nonrandomly for stat inference
	from somewhere and to somewhere especially in neuralMeasusureHelpPredict...byAttVec" kind of the 3 Att kinds).
	InTheory, by exploring the paths in the float[][] stochasticMatrix (per nolay),
	we will find which nodes help in predicting which other nodes. Those which less help predict a certain few nodes, 
	such as those representing mouse movements being predicted on screen in a game, are less useful for that and may be removed.
	<br><br>
	This is optional, so if one of the float[][] is all 0s (or what range are they in? sum to 1? unit bellcurve?), ignore it.
	<br><br>
	TODO nmhp? After chancedo.999ConvergeRbmToConstFractalPatternOfHowIMoveWhenSeeingGoodPredictionsOfHowIWillMoveChaostimeInFuture,
	and remember chancedo.99PortMyResearchToUfnodeAfterBasicUmouseAndNoRedesignOfRbmDatastructsOrUseOfUfnodeEtcUntilThen is in the order of events too.
	<br><br>
	As of 2018-6 this is not yet used but is very important and planned for p2p net and local experiments leading to that.
	*/
	public final float[][][] nmhp;
	
	
	
	/** Per nolay (such as zigzagPredict[any][nolay][node]), is it sigmoid or weightedCoinFlip of sigmoid chance? *
	public final boolean[] lowNNolaysAreScalar;
	*/
	public final int lowNNolaysAreScalar;
	
	//public final float learnRate;
	
	/** TODO opimize by not doing the whole calculation if some of these are 0, wherever it has no effect */
	public final float[] learnRatePerEdlay;
	
	
	
	
	//static{System.err.println("RBM and all classes other than graphics, streaming, etc, will be replaced by javapackage://funcs which are all lambdas that take varargs Object param used as map (key val key val...) and interpret any Object[] as such a smallmap. These when compiled in javassist will use hashname as their names so they are immutable forest, and for convenience of human programmers will use a hopfieldnamespace of hashname to name actually see in text editor (such as in eclipse or netbeans) of those funcs which  are static funcs anywhere. This is a compromise between ufnode design and plainJava.");}
	
	//static{System.err.println("TODO? Use JsonDS.set and get funcs for NavigableMap/List/float[][][] etc, and all funcs should be static and use arrays/maps/lists/etc immutably. Later can optimize that with ufnode which has log cost per forkModify (and still can wrap arrays as long as you dont fork them)");}
	
	//TODO? use Serializable's "ANY-ACCESS-MODIFIER Object writeReplace() throws ObjectStreamException" and "ANY-ACCESS-MODIFIER Object readResolve() throws ObjectStreamException"
	//(and maybe I can put ufnodeLike forkEditable in it?)
	
	//Put all the AI code in this class. Make it a self contained file.
	
	public final String comment;
	
	/*Should bias be float[][][] parallel to weight[][][] and weightVelocity[][][]?
	It would allow bias to vary depending on Att, viewing every node as 2 nodes,
	one as usual and the other always has value 1, and bias from a node is from the latter.
	But there wouldnt be biasWeightVelocity so it could get confusing.
	Imagine defining 2 weights that copy between 2 nodes: 4 and bias -2 (which would only work if Att is 1 there).
	The bias float[][] would not be processed by GPU, instead the GPU would get 2 of float[] for low nodes bias and
	high nodes bias IN CONTEXT OF Atts, which is the same for the whole batch (so a double loop in cpu, gpu does triple loop).
	Or I could have each Consumer<LearnLoopParam> compute LearnLoopParam.returnBias and average those returnBias into node,
	where the layer above and layer below gets half the influence even if they're different sizes,
	instead of having a separate such bias per edge.
	*
	FIXME TODO replace biasPerNolay with weight and weightVelocity get 1 extra node per nolay, always index 0,
	and its value is always 1 as computed by the usual boltzmann math (I'm not writing special logic to make it 1).
	Its always 1 cuz weight[anyEdlay][0][0]==Float.POSITIVE_INFINITY.
	There will be special logic for updating weight and weightVelocity where 1 or both nodes are biasNodes (nolay index 0),
	but that special logic will be in each Consumer<LearnLoopParam> using LearnLoopParam.lowNodeIsBias and LearnLoopParam.highNodeIsBias.
	...
	Its very important that bias be one of the weights so each node has up to 2 biases, one from below and one from above,
	which can be used to solve asymmetry in average node value,
	and secondarily its important cuz Consumer<LearnLoopParam> will update it using the vars it already uses
	plus 2 more to tell if low node and if high node are bias nodes.
	...
	Att of nolay index 0 must always be 1. The sum of Atts of all other nolay indexs (in same nolay) will normally be 1,
	or TODO maybe the Att of index 0 could be adjusted trying to get bias node to act like normal nodes,
	but there is too much danger of overfitting so probably bias nodes need special logic (in the Consumer<LearnLoopParam>).
	*
	NO, I WANT float[][][] bias (PARALLEL TO WEIGHT[][][] AND WEIGHTVELOCITY[][][]),
	OR MAYBE I WANT FLOAT[][] BIAS OF NOLAY AND FLOAT[][] BIASVELOCITY OF NOLAY,
	EITHER WAY I MAYBE WANT THAT (INSTEAD OF NOLAY INDEX 0 BEING BIAS NODE, BUT I DO LIKE ITS ASYMMETRY) CUZ
	ITS SIMPLER Consumer<LearnLoopParam> to have a LearnLoopParam.returnBiasOfWeight. Its more like a cellmata.
	*/
	
	public final float[][][] weight;
	
	/** As in neural momentum. FIXME this is not hooked into learning yet as of 2018-5-6.
	Its important to consider the Atts (which are normally partially random for learning and flat for prediction)
	when updating (by forkEdit) these velocities, and it may be a problem that slightly more or less cumulative Att
	happens on different nodes since Att is partially random. But since thats computed in GPU which gives us
	a float[][][] (TODO change that code to be derivatives instead of new weights, and update them here,
	just add that to weightVelocity, but that seems like it would wrongly vary when AttLev1RelRange varies
	if AttLev1RelRange of x ranges 1-x to 1, so I'm going to change that to range 1-x/2 to 1+x/2.
	<br><br>
	FIXME what is the unit of time? learnRate? learnRate^2? sqrt(learnRate)?
	Cuz accel is deriv of velocity is deriv of position.
	*/
	public final float[][][] weightVelocity;
	
	public final float weightDecay;
	
	/** Decay weightVelocity (which is added to weight (positions)) else would spring around faster and faster.
	Decay is similar to friction but divides instead of subtracts from velocity so is not overpowered by large velocities.
	TODO should this be proportional to learnRate, sqrt(learnRate), learnRate^2,
	etc? Cuz accel is deriv of velocity is deriv of position.
	*/
	public final float weightVelocityDecay;
	
	
	
	
	
	
	
	
	
	//Att (inverse temperature) is scaled by 1 if downward, and scaled by a number per edside if upward,
	//to adjust for layer sizes and number of possible states etc.
	//Att will still be generated partially randomly and scaled by those.
	
	//bias is some number (inverseSigmoid(target ave node value) scaled by Att) if downward, and is some number per node if upward,
	//to adjust for layer sizes and number of possible states etc.
	
	/** Att aka 1/temperature, per nolay, same indexs as upwardNodeBias except per nolay instead of per node. Downward Att is 1.
	Bias is like a weight, scaled by Att.
	See mindmapnames:
	rbmTemperatureWillBeDirectedAndAlwaysBe1DownwardAndVariablePerEdsideUpward
	"Sparsity should only be adjusted in upward inference, and only by upwardbias, using bias per nodeUpward, and using constant biasPerNolayDownward."
	*
	public final float[] upwardNolayAtt;

	/** upwardNodeBias[nolay-1][node], all nolays except the first.
	Bias is like a weight, scaled by Att.
	See mindmapnames:
	rbmTemperatureWillBeDirectedAndAlwaysBe1DownwardAndVariablePerEdsideUpward
	"Sparsity should only be adjusted in upward inference, and only by upwardbias, using bias per nodeUpward, and using constant biasPerNolayDownward."
	*
	public final float[][] upwardNodeBias;
	
	/** downwardNolayBias[nolay], all nolays except the last.
	Bias is like a weight, scaled by Att.
	See mindmapnames:
	rbmTemperatureWillBeDirectedAndAlwaysBe1DownwardAndVariablePerEdsideUpward
	"Sparsity should only be adjusted in upward inference, and only by upwardbias, using bias per nodeUpward, and using constant biasPerNolayDownward."
	<br><br>
	downwardNolayBias should be inverseSigmoid(target ave node value) scaled by Att.
	*
	public final float[] downwardNolayBias;
	*/
	
	/** [nolay*2 for down, nolay*2+1 for up][node]
	<br><br>
	dont scale bias by temperature (1/AttLev*), cuz bias is to adjust for where other weights average about 0, so sigmoid of near 0 is about .5 without bias, so we dont want to allow temperature to change sigmoid's average value. temperature is 1/Att.
	<br><br>
	downwardNolayBias should be inverseSigmoid(target ave node value) scaled by Att,
	and since default attLev2PerNodeside for downward is 1, no adjustment for that.
	*/
	public final float[][] biasPerNodeside;
	
	/** [nolay*2 for down, nolay*2+1 for up][node]
	<br><br>
	This is the middle of those 3 Att types. There will be 3 Levs of Att which are multiplied together:
	-- Att PER NODE. The random neuralDropout-like kind, which can also be used nonrandomly for stat inference from somewhere and to somewhere especially in neuralMeasusureHelpPredict...byAttVec
	-- TODO ASAP: Att PER NODESIDE. Adjustment of Att for layer sizes and averages of parts of trainingVecs.
 	-- TODO IN LATER VERSION OF THIS SOFTWARE: Att PER NODE. Sparse navigation of mmg p2p neuralnet, where nodes come in at very low Att and earn higher Att by neuralMeasusureHelpPredict...byAttVec.
 	<br><br>
 	Default downward Att is inverseSigmoid(target ave node value). Normally only upward Att is chosen in Consumer<LearnLoopParam>.
 	<br><br>
 	dont scale bias by temperature (1/Att), cuz bias is to adjust for where other weights average about 0, so sigmoid of near 0 is about .5 without bias, so we dont want to allow temperature to change sigmoid's average value. temperature is 1/Att.
 	<br><br>
 	TODO How will I adjust upwardNolayAtt? (of course in Consumer<LearnLoopParam> but what code string?)
	Do I want some approx range of stddev of incoming weightedsum? upwardNodeBias will handle the ave, but what of the dev?
 	*/
	public final float[][] attLev2PerNodeside;
			
	//TODO replace AttLev1RelRange with AttStdDev so if doing normal dropout vs smooth dropout, stdDev describes those both on same scale.
	
	/** Did not work well, probably cuz contrastiveDivergence already tries to keep trainingVecs near equal energy as eachother,
	probably will be removed in a later version of this software.
	<br><br>
	TODO move this into the "other" map field.
	*/
	public final boolean learnByIncompleteBoltzenCode;
	
	//"TODO include AttPredict and AttLearn? Or keep those as params of other funcs?"
	
	/** from input up to top layer then alternate all the way down then all the way up.
	Even indexs are up. Odd are down. float[zigzagIndex][nodeLayer][nodeIndexInThatLayer]
	zigzagPredict.length is even, and zigzagLearn.length is odd.
	*
	public final float[][][] zigzagPredict;
	
	/** Can be null. zigzagPredict.length is even, and zigzagLearn.length is odd. *
	public final float[][][] zigzagLearn;
	
	/** Can be null. If zigzagLearn and zigzagPredict both use random Atts (which I'm experimenting with),
	then bidirectional backprop norm still needs zigzag created from evenly spread Atts.
	*
	public final float[][][] zigzagNorm;
	*/
	
	/** Number of inferences between bottom or top nolay to the opposite end nolay, during learning.
	Must be at least 3. Must be odd. Normally is 1 more than zigzagsPredict.
	*/
	public final int zigzagsLearn;
	
	//public final float[] in;
	
	/** AttLev1RelRange of x means randomly in range 1-x/2 to 1+x/2. Normally for learning x is positive. Can be 0-2. */
	public final float attLev1RelRangeForLearn;
	
	/** AttLev1RelRange of x means randomly in range 1-x/2 to 1+x/2. Normally for prediction x is 0. Can be 0-2. */
	public final float attLev1RelRangeForPredict;
	
	/** java (TODO or is it just javalike?) code string of func body of Consumer<LearnLoopParam> whose param is p, such as QUOTE:
	float att = p.lowNodeAtt*p.highNodeAtt;
	float diff = p.learnRate*att*(p.toLearn-p.toUnlearn);
	float diffScaled = diff/p.batchSize; //spread learnRates across vecs instead of each.
	float decay = p.weightDecay*diffScaled*diffScaled;
	float deriv = diffScaled - decay*p.weight;
	float dt = 1; //FIXME this was the old code but its buggy cuz when learnRate changes, weightVelocityDecay does not, but learnRate*weightVelocityDecay would.
	p.returnWeightVelocity = p.weightVelocity*(1-dt*p.weightVelocityDecay) + dt*deriv;
	p.returnWeight = p.weight + dt*p.weightVelocity;
	UNQUOTE.
	<br><br>
	This code is sandboxed, not allowed to use loops or func calls (except some few that may be whitelisted such as Math.exp(double)) etc.
	This is for security, and secondarily cuz this code is called in a double-loop after every gpu learning batch (which does a triple-loop)
	and we dont want CPU to bottleneck the GPU, though technically this double-loop could also be run on GPU but GPU's bottleneck
	in that case would be copying the 2d array of memory (plus some lag in calling the GPU, so its intheory lower lag to do in CPU).
	<br><br>
	learnFuncCompiled() calls and caches LearnLoopParam.compileSandboxed(RBM.learnFunc) which will sandbox the java code string
	then throw or call Javassist to return Consumer<LearnLoopParam>.
	*/
	public final String learnFunc;
	
	protected transient Consumer<LearnLoopParam_OLD> learnFuncCompiled = null;
	
	/** see comment of learnFunc */
	public Consumer<LearnLoopParam_OLD> learnFuncCompiled(){
		if(learnFuncCompiled == null){
			learnFuncCompiled = LearnLoopParam_OLD.compileSandboxed(learnFunc);
		}
		return learnFuncCompiled;
	}
	
	public final NavigableMap other;
	
	/** the JsonDS form of this RBM, or null. Reuses same float arrays since they are to be used immutably. */
	protected NavigableMap cachedAsMap;
	
	public final boolean learnByContrastiveDivergenceSquared = false; //TODO? make this a param
	
	/*
	public static final Random weakRand;
	public static final SecureRandom strongRand;
	static{
		strongRand = new SecureRandom();
		//TODO set seed as bigger byte array, more hashcodes to fill it maybe
		strongRand.setSeed(3+System.nanoTime()*49999+System.currentTimeMillis()*new Object().hashCode());
		weakRand = new Random(strongRand.nextLong());
	}*/

	/** per edlay *
	public final float[] weightAve;
	
	/** per edlay, standard deviation *
	public final float[] weightDev;
	*/
	
	/*"TODO how to get the float[][] node states to go into derivDown and derivUp, since they're observed as bits at each next nolay?"
	...
	Compute weightedSum and sigmoid again for 2 of the zigzags, to get the scalars (computed from adjacent bits) 
	instead of bits (computed from adjacent bits). This will make it maybe 25% slower since its only 2 of the
	zigzags. The main benefit of doing it this way, instead of reusing existing weightedSums before
	weightedCoinFlips, is all AI funcs continue to return only 1 array, and derive it from a zigzag
	array. Can optimize away that 25% when redesign in some future version, but for now
	focus on experimental concepts more than such small amounts of optimizing. Do it alternating
	on the last 2 and the thirdlastWithSecondlast of the learning zigzag, or maybe of the
	prediction zigzag, undecided which but the indexs are decided.
	...
	maybe move these arrays into an immutable RBM class or something like that, since its getting to be too many of them
	therefore including internal RBM code in the graphics classes.
	...
	Should it be from the learning zigzag or the predicting zigzag?
	*/
	
	/*public RBM(float[][][] weight, float derivDecay, float[][] targetNodeAve, int zigzagPredict, int zigzagLearn){
		this(
			"", //comment
			weight,
			AI.newEmptyArraySameSizesAs(weight), //derivUp
			AI.newEmptyArraySameSizesAs(weight), //derivDown
			derivDecay,
			targetNodeAve,
			AI.
		)
	}*/
	
	/** bias added to weightedSum in opencl (FIXME also in cpu version).
	FIXME this should be a param, at least 1 separate bias per nolay, maybe 2*edlays for different bias up vs down,
	or maybe bias per node.
	*
	public static final float bias;
	*/
	
	/** use the set* funcs to add params before using. These are mostly empty arrays */
	public RBM(){
		this(
			"", //comment
			new float[0][][], //weight
			new float[0][][], //weightVelocity
			new float[0][][], //nmhp
			new float[0][], //biasPerNodeside
			new float[0][], //attLev2PerNodeside
			.02f, //weightDecay
			.001f, //weightVelocityDecay
			0, //lowNNolaysAreScalar
			false, //learnByIncompleteBoltzenCode
			new float[0][], //targetNodeAve
			new float[0][], //targetNodeDev
			new float[0], //learnRatePerEdlay
			3, //zigzagsLearn
			.5f, //AttLev1RelRangeForLearn
			0f, //AttLev1RelRangeForPredict
			"returnWeight = 42; returnWevel = 0f;",
			JsonDS.emptyMap //other
		);
	}
	public RBM(
		String comment,
		float[][][] weight,
		float[][][] weightVelocity,
		float[][][] nmhp,
		float[][] biasPerNodeside,
		float[][] attLev2PerNodeside,
		float weightDecay,
		float weightVelocityDecay,
		int lowNNolaysAreScalar,
		boolean learnByIncompleteBoltzenCode,
		//float[][][] derivUp,
		//float[][][] derivDown,
		//float derivDecay,
		float[][] targetNodeAve,
		float[][] targetNodeDev,
		//float[][][] zigzagPredict,
		//float[][][] zigzagLearn,
		//float[][][] zigzagNorm,
		//float[] weightAve,
		//float[] weightDev,
		float[] learnRatePerEdlay,
		int zigzagsLearn,
		//float[] in,
		float AttLev1RelRangeForLearn,
		float AttLev1RelRangeForPredict,
		String learnFunc,
		NavigableMap other
	){
		this.comment = comment;
		this.weight = weight;
		this.weightVelocity = weightVelocity;
		this.nmhp = nmhp;
		this.biasPerNodeside = biasPerNodeside;
		this.attLev2PerNodeside = attLev2PerNodeside;
		this.weightDecay = weightDecay;
		this.weightVelocityDecay = weightVelocityDecay;
		this.lowNNolaysAreScalar = lowNNolaysAreScalar;
		this.learnByIncompleteBoltzenCode = learnByIncompleteBoltzenCode;
		//this.derivUp = derivUp;
		//this.derivDown = derivDown;
		//this.derivDecay = derivDecay;
		this.targetNodeAve = targetNodeAve;
		this.targetNodeDev = targetNodeDev;
		//this.zigzagPredict = zigzagPredict;
		//this.zigzagLearn = zigzagLearn; //TODO dont need 3 zigzags, need at most 2, maybe just 1
		//this.zigzagNorm = zigzagNorm; //TODO dont need 3 zigzags, need at most 2, maybe just 1
		//verifySizes();
		//this.weightAve = weightAve;
		//this.weightDev = weightDev;
		this.learnRatePerEdlay = learnRatePerEdlay;
		//this.in = in;
		this.zigzagsLearn = zigzagsLearn;
		this.attLev1RelRangeForLearn = AttLev1RelRangeForLearn;
		this.attLev1RelRangeForPredict = AttLev1RelRangeForPredict;
		this.learnFunc = learnFunc;
		this.other = other;
	}
	
	/** From a JsonDS map, such as from asMap() and optionally modified */
	public RBM(NavigableMap map){
		this(
			(String)map.get("comment"),
			(float[][][])map.get("weight"),
			(float[][][])map.get("weightVelocity"),
			(float[][][])map.get("nmhp"),
			(float[][])map.get("biasPerNodeside"),
			(float[][])map.get("attLev2PerNodeside"),
			(float)(double)map.get("weightDecay"),
			(float)(double)map.get("weightVelocityDecay"),
			(int)(double)map.get("lowNNolaysAreScalar"),
			JsonDS.doubleToBoolean((double)map.get("learnByIncompleteBoltzenCode")),
			(float[][])map.get("targetNodeAve"),
			(float[][])map.get("targetNodeDev"),
			//(float[][][])map.get("zigzagPredict"),
			//(float[][][])map.get("zigzagLearn"), //TODO dont need 3 zigzags, need at most 2, maybe just 1
			//(float[][][])map.get("zigzagNorm"), //TODO dont need 3 zigzags, need at most 2, maybe just 1
			(float[])map.get("learnRatePerEdlay"),
			(int)(double)map.get("zigzagsLearn"),
			//(float[])map.get("in"),
			(float)(double)map.get("AttLev1RelRangeForLearn"),
			(float)(double)map.get("AttLev1RelRangeForPredict"),
			(String)map.get("learnFunc"),
			(NavigableMap)map.get("other")
		);
		cachedAsMap = map;
	}
	
	public String toString(){
		return asMap().toString();
	}
	
	/** The JsonDS form of this RBM, which can be used in constructor.
	Reuses same float arrays (translates boolean[] to float[], boolean to Double).
	*/
	public NavigableMap asMap(){
		if(cachedAsMap == null){
			//faster than JsonDS every time cuz it forkModifies (TODO ufnode only log instead of linear cost)
			NavigableMap m = new TreeMap();
			m.put("comment", comment);
			m.put("weight", weight);
			m.put("weightVelocity", weightVelocity);
			m.put("nmhp", nmhp);
			m.put("biasPerNodeside", biasPerNodeside);
			m.put("weightDecay", weightDecay);
			m.put("weightVelocityDecay", weightVelocityDecay);
			m.put("lowNNolaysAreScalar", (double)lowNNolaysAreScalar);
			m.put("learnByIncompleteBoltzenCode", JsonDS.booleanToDouble(learnByIncompleteBoltzenCode));
			m.put("targetNodeAve", targetNodeAve);
			m.put("targetNodeDev", targetNodeDev);
			//m.put("zigzagPredict", zigzagPredict);
			//m.put("zigzagLearn", zigzagLearn);
			//m.put("zigzagNorm", zigzagNorm);
			m.put("learnRatePerEdlay", (float[])learnRatePerEdlay);
			m.put("zigzagsLearn", (double)zigzagsLearn);
			//m.put("in", in);
			m.put("AttLev1RelRangeForLearn", (double)attLev1RelRangeForLearn);
			m.put("AttLev1RelRangeForPredict", (double)attLev1RelRangeForPredict);
			m.put("learnFunc", learnFunc);
			m.put("other", other);
			cachedAsMap = Collections.unmodifiableNavigableMap(m);
		}
		return cachedAsMap;
	}

	public RBM setComment(String comment){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setWeight(float[][][] weight){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setWeightVelocity(float[][][] weightVelocity){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setNmhp(float[][][] nmhp){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setWeightDecay(float weightDecay){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setWeightVelocityDecay(float weightVelocityDecay){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setLowNNolaysAreScalar(int lowNNolaysAreScalar){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setLearnByIncompleteBoltzenCode(boolean learnByIncompleteBoltzenCode){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setTargetNodeAve(float[][] targetNodeAve){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setLearnRate(float learnRateForAllEdlays){
		float[] a = new float[weight.length];
		Arrays.fill(a, learnRateForAllEdlays);
		return setLearnRatePerEdlay(a);
	}
	
	public RBM setLearnRatePerEdlay(float[] learnRatePerEdlay){
		//if(learnRate < -1 || 1 < learnRate) throw new Error("learnRate "+learnRate+" must be in range -1 to 1 (FIXME this is far too low for the old code but should align well to boltzen experimental code which uses it as decay toward certain energy instead of about individual weights).");
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setZigzagsLearn(int zigzagsLearn){
		if((zigzagsLearn&1)!=1) throw new Error("zigzagsLearn must be odd: "+zigzagsLearn);
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setAttLev1RelRangeForLearn(float AttLev1RelRangeForLearn){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, AttLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setAttLev1RelRangeForPredict(float AttLev1RelRangeForPredict){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, AttLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setAttLev2PerNodeside(float[][] attLev2PerNodeside){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	/** See comment of learnFunc field */
	public RBM setLearnFunc(String learnFunc){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM setBiasPerNodeside(float[][] biasPerNodeside){
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	/** in Map other. TODO use ufnode forkEditable maplists for log cost instead of linear. */
	public RBM put(Object key, Object value){
		NavigableMap other = JsonDS.jsonSet(this.other, key, value);
		//return new RBM(weight, targetNodeAve, zigzagPredict, zigzagLearn, zigzagNorm, learnRate, in, AttLev1RelRangeForLearn, rand, other);
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, attLev1RelRangeForLearn, attLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM pushLayerFromNoThrow(RBM prototype){
		if(nolays() >= prototype.nolays()) return this;
		return pushLayerFrom(prototype);
	}
	
	/** Example: If I have 3 nolays and r has at least 4 nolays, returns a forkEdit of me with 4 nolays including r's 4th,
	and all the related data such as weights between those 2 nolays.
	This is used so 
	*/
	public RBM pushLayerFrom(RBM prototype){
		String comment = this.comment;
		float[][][] weight = pushLayerFromArray(this.weight, prototype.weight);
		int newNolays = weight.length+1;
		int sizeOfNolayToPush = nolaySize(weight, newNolays-1);
		float[][][] weightVelocity = pushLayerFromArray(this.weightVelocity, prototype.weightVelocity);
		float weightVelocityDecay = this.weightVelocityDecay;
		//if(lowNNolaysAreScalar != prototype.lowNNolaysAreScalar) throw new Error("TODO choose which, or min or max of those 2, or what logic?");
		int lowNNolaysAreScalar = Math.min(prototype.lowNNolaysAreScalar,newNolays);
		boolean learnByIncompleteBoltzenCode = this.learnByIncompleteBoltzenCode;
		//float[][][] derivUp,
		//float[][][] derivDown,
		//float derivDecay,
		float[][] targetNodeAve = pushLayerFromArray(this.targetNodeAve, prototype.targetNodeAve);
		float[][] attLev2PerNodeside = pushLayerFromArray(this.attLev2PerNodeside, prototype.attLev2PerNodeside);
		//[zigzag][nolay][nodeInNolay]. We want to change nolay.length to nolay.length+1.
		//float[][][] zigzagPredict = pushSinglezigzagforcpuLayerFromArray(this.zigzagPredict, sizeOfNolayToPush); //FIXME ignoring prototype.zigzag* cuz might not have those
		//float[][][] zigzagLearn = this.zigzagLearn==null ? null : pushSinglezigzagforcpuLayerFromArray(this.zigzagLearn, sizeOfNolayToPush); //FIXME ignoring prototype.zigzag* cuz might not have those
		//float[][][] zigzagNorm = this.zigzagNorm==null ? null : pushSinglezigzagforcpuLayerFromArray(this.zigzagNorm, sizeOfNolayToPush); //FIXME ignoring prototype.zigzag* cuz might not have those
		//verifySizes();
		//float[] weightAve,
		//float[] weightDev,
		float[] learnRatePerEdlay = pushLayerFromArray(this.learnRatePerEdlay, prototype.learnRatePerEdlay);
		//float[] in = this.in;
		float AttLev1RelRangeForLearn = this.attLev1RelRangeForLearn;
		float AttLev1RelRangeForPredict = this.attLev1RelRangeForPredict;
		NavigableMap other = this.other;
		if(1<2) throw new Error("FIXME check all fields are pushed and popped.");
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, AttLev1RelRangeForLearn, AttLev1RelRangeForPredict, learnFunc, other);
	}
	
	/** opposite of pushLayerFrom(RBM) */
	public RBM popLayer(){
		String comment = this.comment;
		float[][][] weight = popLayerFromArray(this.weight);
		float[][][] weightVelocity = popLayerFromArray(this.weightVelocity);
		float weightVelocityDecay = this.weightVelocityDecay;
		int lowNNolaysAreScalar = Math.min(nolays(),this.lowNNolaysAreScalar);
		boolean learnByIncompleteBoltzenCode = this.learnByIncompleteBoltzenCode;
		//float[][][] derivUp,
		//float[][][] derivDown,
		//float derivDecay,
		float[][] targetNodeAve = popLayerFromArray(this.targetNodeAve);
		float[][] biasPerNodeside = popLayerFromArray(popLayerFromArray(this.biasPerNodeside));
		float[][] attLev2PerNodeside = popLayerFromArray(popLayerFromArray(this.attLev2PerNodeside));
		float[][][] nmhp = popLayerFromArray(this.nmhp);
		//[zigzag][nolay][nodeInNolay]. We want to change nolay.length to nolay.length-1.
		//float[][][] zigzagPredict = popSinglezigzagforcpuLayerFromArray(this.zigzagPredict);
		//float[][][] zigzagLearn = this.zigzagLearn==null ? null : popSinglezigzagforcpuLayerFromArray(this.zigzagLearn);
		//float[][][] zigzagNorm = this.zigzagNorm==null ? null : popSinglezigzagforcpuLayerFromArray(this.zigzagNorm);
		//verifySizes();
		//float[] weightAve,
		//float[] weightDev,
		float[] learnRatePerEdlay = popLayerFromArray(this.learnRatePerEdlay);
		//float[] in = this.in;
		float AttLev1RelRangeForLearn = this.attLev1RelRangeForLearn;
		float AttLev1RelRangeForPredict = this.attLev1RelRangeForPredict;
		NavigableMap other = this.other;
		//FIXME if(1<2) throw new Error("FIXME check all fields are pushed and popped.");
		return new RBM(comment, weight, weightVelocity, nmhp, biasPerNodeside, attLev2PerNodeside, weightDecay, weightVelocityDecay, lowNNolaysAreScalar, learnByIncompleteBoltzenCode, targetNodeAve, targetNodeDev, learnRatePerEdlay, zigzagsLearn, AttLev1RelRangeForLearn, AttLev1RelRangeForPredict, learnFunc, other);
	}
	
	public RBM popLayerNoThrow(){
		if(nolays() < 2) return this;
		return popLayer();
	}
	
	/** [zigzag][nolay][nodeInNolay]. We want to change nolay.length to nolay.length+1. */
	public static float[][][] pushSinglezigzagforcpuLayerFromArray(float[][][] zigzag, int sizeOfNolayToPush){
		int nolays = zigzag[0].length;
		float[][][] ret = new float[zigzag.length][][];
		float[] nolayToPush = new float[sizeOfNolayToPush];
		for(int i=0; i<nolayToPush.length; i++) nolayToPush[i] = (float)i/nolayToPush.length; //arbitrary node states just to make sure it displays if theres an error right after this change
		for(int z=0; z<zigzag.length; z++){
			ret[z] = new float[nolays+1][];
			System.arraycopy(zigzag[z], 0, ret[z], 0, nolays);
			ret[z][nolays] = nolayToPush;
		}
		return ret;
	}
	
	public static <T> T[] pushLayerFromArray(T[] stack, T[] prototype){
		if(stack.length >= prototype.length) throw new Error("stack[] too small for prototype[]");
		T[] ret = (T[]) Array.newInstance(stack.getClass().getComponentType(), stack.length+1);
		System.arraycopy(stack, 0, ret, 0, stack.length);
		ret[stack.length] = prototype[stack.length];
		return ret;
	}
	
	public static float[] pushLayerFromArray(float[] stack, float[] prototype){
		if(stack.length >= prototype.length) throw new Error("stack[] too small for prototype[]");
		float[] ret = new float[stack.length+1];
		System.arraycopy(stack, 0, ret, 0, stack.length);
		ret[stack.length] = prototype[stack.length];
		return ret;
	}
	
	public static boolean[] pushLayerFromArray(boolean[] stack, boolean[] prototype){
		if(stack.length >= prototype.length) throw new Error("stack[] too small for prototype[]");
		boolean[] ret = new boolean[stack.length+1];
		System.arraycopy(stack, 0, ret, 0, stack.length);
		ret[stack.length] = prototype[stack.length];
		return ret;
	}
	
	
	/** [zigzag][nolay][nodeInNolay]. We want to change nolay.length to nolay.length-1. */
	public static float[][][] popSinglezigzagforcpuLayerFromArray(float[][][] zigzag){
		int nolays = zigzag[0].length;
		float[][][] ret = new float[zigzag.length][][];
		for(int z=0; z<zigzag.length; z++){
			ret[z] = new float[nolays-1][];
			System.arraycopy(zigzag[z], 0, ret[z], 0, nolays-1);
		}
		return ret;
	}
	
	public static <T> T[] popLayerFromArray(T[] stack){
		if(stack.length == 0) throw new Error("empty array");
		T[] ret = (T[]) Array.newInstance(stack.getClass().getComponentType(), stack.length-1);
		System.arraycopy(stack, 0, ret, 0, stack.length-1);
		return ret;
	}
	
	public static float[] popLayerFromArray(float[] stack){
		if(stack.length == 0) throw new Error("empty array");
		float[] ret = new float[stack.length-1];
		System.arraycopy(stack, 0, ret, 0, stack.length-1);
		return ret;
	}
	
	public static boolean[] popLayerFromArray(boolean[] stack){
		if(stack.length == 0) throw new Error("empty array");
		boolean[] ret = new boolean[stack.length-1];
		System.arraycopy(stack, 0, ret, 0, stack.length-1);
		return ret;
	}
	
	/** returns this. throws if sizes of arrays etc are not a valid combo *
	public RBM verifySizes(){
		if((zigzagLearn != null && zigzagPredict[0].length != zigzagLearn[0].length) || (zigzagNorm != null && zigzagPredict[0].length != zigzagNorm[0].length)){
			throw new Error("Diff sizes");
		}
		return this;
	}*/

	/** MERGING THIS INTO learn(float[][][], ...) func cuz thats where weightDecay happens.
	<br><br>
	forkEdits weights by weightVelocity and weightVelocityDecay, given a change in "time" (dt) whatever time means
	such as maybe its proportional to learnRate or sqrt(learnRate) or learnRate^2 (cuz accel is deriv of velocity is deriv of position)
	or I'm not sure what dt should be yet (as of 2018-5-6.
	<br><br>
	This is done with CPU (instead of GPU) cuz its BigO is same as memory reads/writes. GPU only helps if that BigO is higher.
	<br><br>
	TODO optimize, merge this with weightDecay (of weight position, compared to this is decay of weightVelocity).
	*
	public RBM velocityContinues(float dt){
		float[][][] newWeight = newEmptyArraySameSizesAs(weight);
		//TODO optimize? Could store cumulative weightVelocity multiplier in a float instead of replacing weightVelocity[][][] every time,
		//but it would be a little more confusing. This is not a bottleneck so do it the simpler way at least for now.
		float[][][] newWeightVelocity = newEmptyArraySameSizesAs(weight);
		float multVelocity = 1-dt*weightVelocityDecay;
		for(int edlay=0; edlay<weight.length; edlay++){
			int lowNodes = weight[edlay].length, highNodes = weight[edlay][0].length;
			for(int lowNode=0; lowNode<lowNodes; lowNode++){
				for(int highNode=0; highNode<highNodes; highNode++){
					newWeight[edlay][lowNode][highNode] = weight[edlay][lowNode][highNode]+dt*weightVelocity[edlay][lowNode][highNode];
					newWeightVelocity[edlay][lowNode][highNode] = weightVelocity[edlay][lowNode][highNode]*multVelocity;
				}
			}
		}
		return setWeight(newWeight).setWeightVelocity(newWeightVelocity);
	}*/
	
	public Object get(Object key){
		return other.get(key);
	}
	
	/** in Map other, else NaN or ClassCastException */
	public double getD(Object key){
		Object v = get(key);
		return v==null ? 0./0 : (double)v;
	}
	
	/** in Map other, else NaN or ClassCastException */
	public float getF(Object key){
		Object v = get(key);
		return v==null ? 0f/0 : (float)v;
	}
	
	/** in Map other, else null or ClassCastException */
	public String getS(Object key){
		return (String)get(key);
	}
	
	/*public RBM setWeightAve(float[] weightAve){
		return new RBM(comment, weight, derivUp, derivDown, derivDecay, targetNodeAve, zigzagPredict, zigzagLearn);
	}
	
	public RBM setWeightDev(float[] weightDev){
		return new RBM(comment, weight, derivUp, derivDown, derivDecay, targetNodeAve, zigzagPredict, zigzagLearn);
	}*/
	
	//static final boolean bidirectionalBackpropNormUsesZigzagOfLearnInsteadOfPredict = false; //FIXME remove this when find which works better
	
	/** TODO choose if this will be random, flat, have an option for that and in what cases (learn, predict, norm), etc.
	This isnt divided by nolay size cuz if that is done its in attLev2.
	*/
	public float[][] newAttLev1(boolean forLearn){
		//return evenlySpreadAtts(weight); //FIXME use random Atts (per vec or per batch?)
		//boolean randAttAlsoInVisibleNodes = true;
		boolean randAttAlsoInVisibleNodes = false;
		float attRelRange = forLearn ? attLev1RelRangeForLearn : attLev1RelRangeForPredict;
		return randomAttsLev1InAllHiddenLayers(
			weight, attRelRange, Rand.strongRand, randAttAlsoInVisibleNodes);
	}
	
	/** chooses random attLev1 and multiplies them by attLev2 *
	public float[][][][] predictOnly(float[][] vecs){
		return predict(vecs, MathUtil.multiplyScalars(newAttLev1(true), attLev2PerNodeside), false);
	}
	
	public float[][][][] predictBeforeLearn(float[][] vecs){
		return predict(vecs, MathUtil.multiplyScalars(newAttLev1(true), attLev2PerNodeside), true);
	}*/
	

	
	//static final float learnRateMultToMatchBoltzen = 2e7f;
	
	/** Example: float[] out = rbm.setIn(someFloatArray).setLearnRate(.1f*dt).setAttLev1RelRangeForLearn(.5f)
		.setRandom(Rand.strongRand).think().prediction(); 
	in[] becomes the new visibleNOdes at zigzagLearn[0][0]
	*
	public RBM think(){
	//public RBM think(float[] in, float learnRate, float AttLev1RelRangeForLearn, Random rand){
	//public RBM learn(float[] in, float learnRate, float AttLev1RelRange, Random rand){
		RBM rbm = this;
		float[] in = rbm.in;
		//float learnRateAdjusted = learnRateMultToMatchBoltzen*rbm.learnRate;
		//float learnRateAdjusted = rbm.learnRate;
		float AttLev1RelRangeForLearn = rbm.AttLev1RelRangeForLearn;
		Random rand = Rand.strongRand;
		
		boolean alsoInVisibleNodes = true;
		
		//learnRate /= 10000; //FIXME
		
		boolean divideAttsByNolaySize = true; //FIXME this has no effect cuz is normed to Att sum to 1 per nolay
		float[][] evenlySpreadAtts = evenlySpreadAtts(rbm.weight, divideAttsByNolaySize);
		float[][] randomAtts = randomAttsInAllHiddenLayers(rbm.weight, AttLev1RelRangeForLearn, rand, alsoInVisibleNodes, divideAttsByNolaySize);
		
		float[][] AttLearn = randomAtts;
		
		float[][] AttPredict = evenlySpreadAtts;
		//float[][] AttPredict = randomAtts;
		
		//float[][] AttNorm = evenlySpreadAtts;
		float[][] AttNorm = randomAtts;
		
		float[][][] zigzagLearned = null;
		
		int zigzags = rbm.zigzagPredict.length|1;
		
		float[][][] zigzagFull = zigzag(zigzags, AttPredict, in, rbm.weight, lowNNolaysAreScalar, rand, biasPerNolay);
		//float[][][] zigzagFull = zigzag(rbm.zigzagLearn.length, AttPredict, in, rbm.weight, lowNNolaysAreScalar, rand, biasPerNolay);
		//float[][][] zigzagIndividual = fullUpThenZigzagWithinEachEdlay(rbm.zigzagLearn.length, AttLearn, in, rbm.weight, rand);
		
		if(MathUtil.sum(learnRatePerEdlay) != 0){
			zigzagLearned = zigzagFull;
			//zigzagLearned = fullUpThenZigzagWithinEachEdlay(rbm.zigzagLearn.length, AttLearn, in, rbm.weight, rand);
			//zigzagLearned = zigzag(rbm.zigzagPredict.length, AttPredict, in, rbm.weight, rand);
			//zigzagLearned = fullUpThenZigzagWithinEachEdlay(rbm.zigzagLearn.length, AttLearn, in, rbm.weight, rand);
			//float[] learnRates = AI.learnRates(learnRate,Att); //per edlay
			//float[] learnRates = {learnRate, learnRate*.005f, learnRate*.005f};
			//float[] learnRates = {learnRate};
			//float[] learnRates = {learnRate, learnRate*.01f};
			//float[] learnRates = {learnRate, learnRate*.1f};
			//float[] learnRates = {learnRate, learnRate*5f};
			//float[] learnRates = {learnRateAdjusted, learnRateAdjusted, learnRateAdjusted, learnRateAdjusted, learnRateAdjusted, learnRateAdjusted, learnRateAdjusted};
			//float[] learnRates = {learnRate, learnRate, learnRate};
			//float[] learnRates = {learnRate, .05f*learnRate};
			
			if(learnByIncompleteBoltzenCode){
				//float energyBorder = -50000; //FIXME choose this how?
				//float energyBorder = -2000; //FIXME choose this how?
				//float energyBorder = -27000; //FIXME choose this how?
				//float energyBorder = -3000; //FIXME choose this how?
				//float energyBorder = 0; //FIXME choose this how?
				//float targetEnergyAve = 0, targetEnergyStdDev = 5000;
				
				float targetEnergyAve = -20000, targetEnergyStdDev = 20000;
				rbm = rbm.setWeight(learnFromZigzagToWholeRbmByBoltzen(learnRatePerEdlay, AttLearn,
					zigzagLearned, rbm.weight, targetEnergyAve, targetEnergyStdDev));
			}else{
				//FIXME 2017-12-23 why is learnFromZigzagToWholeRbmByBoltzen working but not (was working earlier) learnFromZigzagToWholeRbm?
				rbm = rbm.setWeight(learnFromZigzagToWholeRbm(learnRatePerEdlay, AttLearn, zigzagLearned, rbm.weight));
			}
			
			rbm = rbm.setZigzagLearn(zigzagLearned);
		}
		
		//FIXME diff learnrates in edlay1 between norm and learn
		
		
		//rbm = rbm.setZigzagPredict(zigzag(rbm.zigzagPredict.length, AttPredict, in, rbm.weight, rand));
		rbm = rbm.setZigzagPredict(zigzagFull);
	
		//normWeightBidirectionalBackprop...
		//decayDerivStatistics must change faster than learning, else it will always be far off
		//float decayDerivStatistics = learnRate*5;
		//rbm = rbm.setDerivDecay(learnRate*5);
		//float decayTowardBidirectionalBackpropNorm = rbm.derivDecay*.01f; //FIXME as small as works (.01?)
		//float decayTowardBidirectionalBackpropNorm = learnRate*.1f; //FIXME as small as works (.01?)
		//float decayTowardBidirectionalBackpropNorm = learnRate*.3f; //FIXME as small as works (.01?)
		//float decayTowardBidirectionalBackpropNorm = learnRate*.05f; //FIXME as small as works (.01?)
		//float decayTowardBidirectionalBackpropNorm = learnRate*1000f; //FIXME as small as works (.01?)
		//float decayTowardBidirectionalBackpropNorm = learnRate*1000f; //FIXME as small as works (.01?)
		//FIXME Should decayDerivStatistics be faster or slower than use of the statistics?
		//
		//2017-8-13 updated func called by normWeightBidirectionalBackprop to
		//mult decayTowardBidirectionalBackpropNorm by a measure of how much it needs norm
		//based on sum of nodes on and sum of targets and Att,
		//so decayTowardBidirectionalBackpropNorm should be much higher and will auto reduce as needed.
		//This allows it to vary independently of learnRate (still mult by learnRate but varying ratio).
		//float decayTowardBidirectionalBackpropNorm = learnRate*10;
		//float decayTowardBidirectionalBackpropNorm = learnRate*3;
		//float decayTowardBidirectionalBackpropNorm = learnRate*30f;
		//float decayTowardBidirectionalBackpropNorm = learnRate*1.5f;
		//float decayTowardBidirectionalBackpropNorm = learnRate*.1f;
		float decayTowardBidirectionalBackpropNorm = 0;
		
		
		if(decayTowardBidirectionalBackpropNorm != 0){
			if((rbm.zigzagLearn.length&1)!=1) throw new Error("First and last learn inference must be up, so odd size");
			//
			
			//rbm = rbm.setZigzagNorm(zigzag(rbm.zigzagNorm.length, AttNorm, in, rbm.weight, rand));
			rbm = rbm.setZigzagNorm(zigzagFull);
			//rbm = rbm.setZigzagNorm(fullUpThenZigzagWithinEachEdlay(rbm.zigzagNorm.length, AttNorm, in, rbm.weight, rand));
			float[][] normUsingWhichZigzagUp = rbm.zigzagNorm[rbm.zigzagNorm.length-(rand.nextBoolean()?2:4)];
			float[][] normUsingWhichZigzagDown = rbm.zigzagNorm[rbm.zigzagNorm.length-(rand.nextBoolean()?1:3)];
			//float[][] normUsingWhichZigzagUp, normUsingWhichZigzagDown;
			//if(bidirectionalBackpropNormUsesZigzagOfLearnInsteadOfPredict){
			//	normUsingWhichZigzagUp = rbm.zigzagLearn[rbm.zigzagLearn.length-(rand.nextBoolean()?2:4)];
			//	normUsingWhichZigzagDown = rbm.zigzagLearn[rbm.zigzagLearn.length-(rand.nextBoolean()?1:3)];
			//}else{
			//	//FIXME should be doing this from learning arrays
			//	normUsingWhichZigzagUp = rbm.zigzagPredict[rbm.zigzagPredict.length-(rand.nextBoolean()?2:4)];
			//	normUsingWhichZigzagDown = rbm.zigzagPredict[rbm.zigzagPredict.length-(rand.nextBoolean()?1:3)];
			//}
			//
	
			//Do 1 layer inference of scalars before weightedCoinFlips.
			//TODO OPTIMIZE This duplicates some earlier computing (maybe whole system is 10% slower), but thats
			//the price we pay for using arrays immutably andOr dividing the functions into params these ways.
			normUsingWhichZigzagUp = upOneNolayGetScalars(AttNorm, normUsingWhichZigzagUp, rbm.weight, biasPerNolay);
			normUsingWhichZigzagDown = downOneNolayGetScalars(AttNorm, normUsingWhichZigzagDown, rbm.weight, biasPerNolay);
			//rbm = rbm.setDerivUp(AI.nextDeriv(rbm.derivUp, normUsingWhichZigzagUp, rbm.derivDecay, true));
			//rbm = rbm.setDerivDown(AI.nextDeriv(rbm.derivDown, normUsingWhichZigzagDown, rbm.derivDecay, false));
			rbm = rbm.setWeight(normWeightBidirectionalBackprop(
				rbm.weight, decayTowardBidirectionalBackpropNorm,
				//rbm.derivUp, rbm.derivDown,
				rbm.targetNodeAve,
				normUsingWhichZigzagUp,
				normUsingWhichZigzagDown,
				AttNorm
			));
		}
		
		
		//float maxAllowedWeightMagnitude = 1.8f;
		//weight = AI.edlayNormByAllowedIndividualWeightRange(
		//	weight, -maxAllowedWeightMagnitude, maxAllowedWeightMagnitude); //TODO optimize by only doing this every n cycles
		//float maxAllowedRadius = 4.28f;
		//float maxAllowedRadius = 10f;
		//float maxAllowedRadius = 20f;
		//float maxAllowedRadius = 50f;
		//float maxAllowedRadius = 200f; //FIXME lower?
		//float maxAllowedRadius = .4f;
		//float maxAllowedRadius = 15f;
		//float maxAllowedRadius = 3f;
		float maxAllowedRadius = 70f;
		rbm = rbm.setWeight(edlayNormByMaxRadius(rbm.weight, maxAllowedRadius));
		
		
		
		//float minAllowedWeightAve = -.05f, maxAllowedWeightAve = -minAllowedWeightAve;
		//float minAllowedWeightAve = -.35f, maxAllowedWeightAve = minAllowedWeightAve;
		//float minAllowedWeightAve = 0, maxAllowedWeightAve = 0;
		//weight = AI.edlayNormByApproxTruncateWeightAveIntoRange(weight, minAllowedWeightAve, maxAllowedWeightAve);
		
		//statistics of zigzagPredict
		//float[] out = AI.getPredictionOutOfZigzag(rbm.zigzagPredict);
		return rbm;
	}*/
	
	//At least until learn doesnt depend on predict (bidirectionalBackpropNormUsesZigzagOfLearnInsteadOfPredict),
	//the learn and predict funcs will be merged into think func.
	/** get prediction from prediction(). in[] is the new visibleNodes at zigzagPredict[0][0]. *
	public RBM predict(float in[]){
		throw new Todo();
	}*/
	
	/** visibleNodes after zigzagging, so zigzagPredict[zigzagPredict.length-1][0] *
	public float[] prediction(){
		return zigzagPredict[zigzagPredict.length-1][0];
	}*/
	
	
	/*
	TODO instantBidirectionalBackpropNorm
	To solve possibleCausesOfBidirectionalBackpropCodeNotWorkingSoFar: get rid of the derivup and derivdown arrays and do it instantly. still do bit at othernode and recompute scalar at thisnode. no more reacting to past.
	this can be coded as set derivdecay to 1/dt but smaller code soon.
	also use derivofthisnode of 1 like i did in feedforward backprop? i want this to be very similar to contrastivedivergence, considering 0 1, 1 0, and the usual 1 1. with scalars rederived? im already planning to make it timeless. use the old bidirectionalbackpropnorm (with scalars rederived?) thos way, to scale it for the asymmetry. keep it separate from learning so can test it by itself on random rbm.

	** 2 derivatives of nodeChance/weight for a weight between 2 nodes.
	These decay based on observation of otherNode*thisNode*(1-thisNode),
	dependent on neuralFunc being sigmoid(x)=1/(1+e^-x),
	cuz derivative(sigmoid(x))=sigmoid(x)*(1-sigmoid(x)).
	This is statistics on one step of backprop done in parallel at all layers.
	FIXME start all these at a high enough number that it will decay downward
	so when dividing by sums of those to choose how much to change weights its
	too slow at first instead of too fast.
	*
	public final float[][][] derivUp;
	
	public final float[][][] derivDown;
	
	/** Example: 1/2.5f. derivDown and derivUp decay at this rate per second. *
	public final float derivDecay;
	*/
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	
	//BELOW THIS LINE, ALL FUNCS MUST BE STATIC AND NOT USE ANYTHING OUTSIDE THIS CLASS EXCEPT JAVA
	
	public static float[] array1dAllSameFloat(int size, float f){
		float[] a = new float[size];
		Arrays.fill(a, f);
		return a;
	}
	
	/** float[layerIndex][nodeIndexInLowLayer][nodeIndexInHighLayer] */
	public static int nolaySize(float[][][] weight, int layer){
		return layer==0 ? weight[0].length : weight[layer-1][0].length;
	}
	
	public static int[] nolaySizes(float[][][] weight){
		int[] ret = new int[weight.length+1];
		for(int i=0; i<ret.length; i++) ret[i] = nolaySize(weight,i);
		return ret;
	}
	
	/** edlays()-1 */
	public int nolays(){
		return weight.length+1;
	}
	
	/** nolays()+1 */
	public int edlays(){
		return weight.length;
	}
	
	/** array of different size arrays of all 0s */
	public static float[][] newNodes(float[][][] edges){
		int nolays = edges.length+1;
		float[][] n = new float[nolays][];
		for(int i=0; i<nolays; i++){
			n[i] = new float[nolaySize(edges, i)];
		}
		return n;
	}
	
	//private static final boolean experimentalUseWeightedAveByNeuralActivationsAndAttInsteadOfJustAtt = false;
	
	public static float[] sumUp(float[] lowNodeAtts, float[] lowNodes, float[][] weight, float[] highNodesBias){
		int highNodes = weight[0].length;
		float[] highSum = highNodesBias.clone();
		//float[] highSum = new float[highNodes];
		//Arrays.fill(highSum, bias);
		for(int lowNode=0; lowNode<lowNodes.length; lowNode++){
			for(int highNode=0; highNode<highNodes; highNode++){
				highSum[highNode] += lowNodeAtts[lowNode]*lowNodes[lowNode]*weight[lowNode][highNode];
			}
		}
		//if(experimentalUseWeightedAveByNeuralActivationsAndAttInsteadOfJustAtt){
		//	for(int highNode=0; highNode<sum.length; highNode++){
		//		sum[highNode] /= dotProd(lowNodeAtts, lowNodes); //FIXME this makes it not be a "sum"
		//	}
		//}
		return highSum;
	}
	
	public static float[] sumDown(float[] highNodeAtts, float[] highNodes, float[][] weight, float[] lowNodesBias){
		int lowNodes = weight.length;
		float[] lowSum = lowNodesBias.clone();
		//float[] lowSum = new float[lowNodes];
		//Arrays.fill(lowSum, bias);
		for(int lowNode=0; lowNode<lowNodes; lowNode++){
			for(int highNode=0; highNode<highNodes.length; highNode++){
				lowSum[lowNode] += highNodeAtts[highNode]*highNodes[highNode]*weight[lowNode][highNode];
			}
		}
		//if(experimentalUseWeightedAveByNeuralActivationsAndAttInsteadOfJustAtt){
		//	for(int lowNode=0; lowNode<sum.length; lowNode++){
		//		sum[lowNode] /= dotProd(highNodeAtts, highNodes); //FIXME this makes it not be a "sum"
		//	}
		//}
		return lowSum;
	}
	
	/** UPDATED DESIGN, ONLY USES weightedCoinFlipPseudorandom FOR NODE STATES OR normTotalCoins.
	Number of heads if flip 100 coins tends to be 45-55 is 1 stdDev so about 2/3 of flips are in that range.
	We dont want that distorting the statistics during contrastiveDivergence,
	so if normTotalCoins==true then after coins have been flipped, randomly change a few of them toward
	making the total heads be the total chance of heads for all weightedCoins,
	such as if they were all .5 chance then there would be total 50 heads every time.
	*/
	public static float[] sigmoidThenWeightedCoinFlip(float[] a, Random rand, boolean normTotalCoins){
		float[] s = new float[a.length];
		float totalChance = 0;
		int howManyNodesAreOn = 0;
		for(int i=0; i<a.length; i++){
			float chance = (float)sigmoid(a[i]);
			totalChance += chance;
			boolean on = weightedCoinFlipPseudorandom(chance);
			if(on){
				howManyNodesAreOn++;
				s[i] = 1;
			}
		}
		if(normTotalCoins){
			int howManyNodesShouldBeOn = (int)totalChance + (weightedCoinFlipPseudorandom(totalChance-(int)totalChance)?1:0);
			while(howManyNodesShouldBeOn != howManyNodesAreOn){
				//FIXME IF most nodes should be on or most should be off, this could get as slow as squared of nodes
				int i = rand.nextInt(a.length);
				if(howManyNodesShouldBeOn < howManyNodesAreOn){
					if(s[i] == 1){
						s[i] = 0;
						howManyNodesAreOn--;
					}
				}else{
					if(s[i] == 0){
						s[i] = 1;
						howManyNodesAreOn++;
					}
				}
			}
		}
		return s;
	}
	
	public static float[] sigmoid(float[] a){
		float[] s = new float[a.length];
		for(int i=0; i<a.length; i++){
			s[i] = (float)sigmoid(a[i]);
		}
		return s;
	}
	
	/** 2d arrays must be same size as eachother. This is much faster than matrix multiply so should be done in CPU. */
	public static float[][] multiplyPerXY(float[][] bc1, float[][] bc2){
		int outer = bc1.length, inner = bc1[0].length;
		float[][] ret = new float[outer][inner];
		for(int o=0; o<outer; o++){
			for(int i=0; i<inner; i++){
				ret[o][i] = bc1[o][i]*bc2[o][i];
			}
		}
		return ret;
	}
	
	
	/** returns sigmoids if rand==null else weightedCoinFlips (0 or 1) of sigmoid chance.
	vecLowNodes[vec][low]
	weight[low][high]
	vecLowNodeAtts is attLev1*attLev2*(in future version of this software there may also be attLev3 view of p2p net)
	*/
	public static float[][] vecsUp(float[][] vecLowNodeAtts, float[][] vecLowNodes, float[][] weight, boolean highNolaysAreScalar, float[] highNodesBias){
		if(weight[0].length != highNodesBias.length){
			throw new Error("sizes dont match");
		}
		float[][] nodeMultAtt = multiplyPerXY(vecLowNodeAtts,vecLowNodes);
		throwIfHasAnyNans(vecLowNodeAtts, -1000, -1000); //FIXME remove this slow test
		throwIfHasAnyNans(weight, -1000, -1000); //FIXME remove this slow test
		throwIfHasAnyNans(weight, -1000, -1000); //FIXME remove this slow test
		throwIfHasAnyNans(highNodesBias, -1000, -1000, -1000); //FIXME remove this slow test
		throwIfHasAnyNans(nodeMultAtt, -1000, -1000); //FIXME remove this slow test
		float[][] ret;
		//MathUtil.nPointersToThenSwapDims(highNodesBias);
		float[][] bias2d = MathUtil.nPointersTo(nodeMultAtt.length,highNodesBias); //FIXME swap dims?
		if(highNolaysAreScalar){
			//bias is not affected by Att
			ret = OpenclProgs.matmulWithBiasThenSigmoid(bias2d, nodeMultAtt, weight);
		}else{
			ret = OpenclProgs.matmulThenSigmoidThenWeightedCoinFlip(bias2d, nodeMultAtt, weight);
		}
		try{
			throwIfHasAnyNans(ret, -1000, -1000); //FIXME remove this slow test
		}catch(Error e){
			for(int j=0; j<100; j++){ //use this in debugger
				ret = OpenclProgs.matmulWithBiasThenSigmoid(bias2d, nodeMultAtt, weight);
				try{
					throwIfHasAnyNans(ret, -1000, -1000); //FIXME remove this slow test
					lg("After nan found on same input to opencl, No nan in "+ret);
				}catch(Throwable t){
					lg("After nan found on same input to opencl, FOUND nan in "+ret);
					lgErr(t);
				}
			}
			throw e;
		}
		return ret;
		//float[] sums = sumUp(lowNodeAtts,lowNodes,weight);
		//return rand==null ? sigmoid(sums) : sigmoidThenWeightedCoinFlip(sums,rand,true);
	}
	
	static final WeakHashMap<float[][],float[][]> swapDims = new WeakHashMap();
	
	/** uses cache to return same float[][] if multiple calls, especially during RBM zigzagging */
	public static synchronized float[][] swapDims(float[][] a){
		float[][] swapped = swapDims.get(a);
		if(swapped == null){
			int outer = a.length, inner = a[0].length;
			swapped = new float[inner][outer];
			for(int o=0; o<outer; o++){
				for(int i=0; i<inner; i++){
					swapped[i][o] = a[o][i];
				}
			}
			swapDims.put(a, swapped);
		}
		return swapped;
	}
	
	/** returns sigmoids or weightedCoinFlips (0 or 1) of sigmoid chance.
	vecHighNodes[vec][high]
	weight[low][high]
	vecHighNodeAtts is attLev1*attLev2*(in future version of this software there may also be attLev3 view of p2p net)
	*/
	public static float[][] vecsDown(float[][] vecHighNodeAtts, float[][] vecHighNodes, float[][] weight,
			boolean lowNolayIsScalar, float[] lowNodesBias){
		float[][] nodeMultAtt = multiplyPerXY(vecHighNodeAtts,vecHighNodes);
		throwIfHasAnyNans(vecHighNodeAtts, -1000, -1000); //FIXME remove this slow test
		throwIfHasAnyNans(vecHighNodes, -1000, -1000); //FIXME remove this slow test
		throwIfHasAnyNans(weight, -1000, -1000); //FIXME remove this slow test
		throwIfHasAnyNans(lowNodesBias, -1000, -1000, -1000); //FIXME remove this slow test
		throwIfHasAnyNans(nodeMultAtt, -1000, -1000); //FIXME remove this slow test
		//weightSwappedDims[high][low]
		float[][] weightSwappedDims = swapDims(weight);
		throwIfHasAnyNans(weightSwappedDims, -1000, -1000); //FIXME remove this slow test
		//TODO optimize: copy and modify OpenclProgs to use swapped dims. Verify the RBM learns first.
		float[][] ret;
		//MathUtil.nPointersToThenSwapDims(highNodesBias);
		float[][] bias2d = MathUtil.nPointersTo(nodeMultAtt.length,lowNodesBias); //FIXME swap dims?
		if(lowNolayIsScalar){
			ret = OpenclProgs.matmulWithBiasThenSigmoid(bias2d, nodeMultAtt, weightSwappedDims);
		}else{
			ret = OpenclProgs.matmulThenSigmoidThenWeightedCoinFlip(bias2d, nodeMultAtt, weightSwappedDims);
		}
		throwIfHasAnyNans(ret, -1000, -1000); //FIXME remove this slow test
		return ret;
		//float[] sums = sumDown(highNodeAtts,highNodes,weight);
		//return rand==null ? sigmoid(sums) : sigmoidThenWeightedCoinFlip(sums, rand,true);
	}
	
	//No, instead doing learning as fullUp then zigzag within each layer
	/*TODO replace each up with upDownUp and each down with downUpDown, to reduce asymmetry in zigzag?
	WARNING: This means the learnPositive will be more overfit to what it already thinks.
	How about just change down to downUpDown and leave up as up?
	...
	Or how about just do zigzag per layer after first fullup instead of repeating fullup fulldown?
	If so, may also need persistentContrastiveDivergence to replace whats lost.
	*/
	
	/** returns sigmoids if rand==null else weightedCoinFlips (0 or 1) of sigmoid chance */
	public static float[] up(float[] lowNodeAtts, float[] lowNodes, float[][] weight, Random rand, float[] highNodesBias){
		float[] sums = sumUp(lowNodeAtts,lowNodes,weight,highNodesBias);
		return rand==null ? sigmoid(sums) : sigmoidThenWeightedCoinFlip(sums, rand, true);
	}
	
	/** returns sigmoids or weightedCoinFlips (0 or 1) of sigmoid chance */
	public static float[] down(float[] highNodeAtts, float[] highNodes, float[][] weight, Random rand, float[] lowNodesBias){
		float[] sums = sumDown(highNodeAtts,highNodes,weight,lowNodesBias);
		return rand==null ? sigmoid(sums) : sigmoidThenWeightedCoinFlip(sums, rand, true);
	}
	
	/** similar to downOneNolayGetScalars */
	public static float[][] upOneNolayGetScalars(float[][] Att, float[][] bitsOrScalars, float[][][] weight, float[][] biasPerNodeside){
		float[][] ret = new float[bitsOrScalars.length][];
		ret[0] = bitsOrScalars[0]; //all AI arrays are used as immutable
		for(int nolayOut=1; nolayOut<ret.length; nolayOut++){
			//ret[nolayOut] = sigmoid(sumDown(Att[nolayOut-1], bitsOrScalars[nolayOut-1], weight[nolayOut-1])); commented out 2017-7-29-3p
			float[] highNodesBias = biasPerNodeside[nolayOut*2+1];
			ret[nolayOut] = sigmoid(sumUp(Att[nolayOut-1], bitsOrScalars[nolayOut-1], weight[nolayOut-1], highNodesBias));
		}
		return ret;
	}
	
	/** The purpose is to get scalars one one side of each edlay to do backprop on,
	even though all you have is a zigzag of each node as bit.
	Given observed bit (or scalar) values (in float[][]), from each nolay (except lowest),
	do inference down again but dont weightedCoinFlip. The one below that, ignore those scalar outputs
	and use the bits already have.
	FIXME Should this use Att?
	*/
	public static float[][] downOneNolayGetScalars(float[][] Att, float[][] bitsOrScalars, float[][][] weight, float[][] biasPerNodeside){
		float[][] ret = new float[bitsOrScalars.length][];
		ret[ret.length-1] = bitsOrScalars[ret.length-1]; //all AI arrays are used as immutable
		for(int nolayOut=ret.length-2; nolayOut>=0; nolayOut--){
			float[] lowNodesBias = biasPerNodeside[nolayOut*2];
			ret[nolayOut] = sigmoid(sumDown(Att[nolayOut+1], bitsOrScalars[nolayOut+1], weight[nolayOut], lowNodesBias));
		}
		return ret;
	}
	
	/** downZigzag is node states after a down (except the top which is from the last up
	FIXME Should this use Att?
	*
	public static float[][][] nextDeriv(float[][][] oldDerivUpOrDown, float[][] zigzagUpOrDown, float decay, boolean up){
		float[][][] newDerivUpOrDown = AI.newEmptyArraySameSizesAs(oldDerivUpOrDown);
		for(int edlay=0; edlay<oldDerivUpOrDown.length; edlay++){
			int lowNodes = oldDerivUpOrDown[edlay].length, highNodes = oldDerivUpOrDown[edlay][0].length;
			for(int low=0; low<lowNodes; low++){
				for(int high=0; high<highNodes; high++){
					//otherNode*thisNode*(1-thisNode)
					float highNode = zigzagUpOrDown[edlay+1][high], lowNode = zigzagUpOrDown[edlay][low];
					float thisNode = up ? highNode : lowNode;
					float otherNode = up ? lowNode : highNode;
					//deriv of sigmoid is sigmoid*(1-sigmoid)
					float derivOfThisNodeByWeightedSum = thisNode*(1-thisNode);
					//float derivOfOtherNodeByWeightedSum = otherNode*(1-otherNode); //ERROR, backward
					float target = derivOfThisNodeByWeightedSum*otherNode;
					//float target = thisNode*derivOfOtherNodeByWeightedSum; //ERROR, backward
					newDerivUpOrDown[edlay][low][high] =
						oldDerivUpOrDown[edlay][low][high]*(1-decay) + decay*target;
				}
			}
		}
		return newDerivUpOrDown;
	}*/
	
	/** FIXME Should this use Att? */
	public static float[][][] normWeightBidirectionalBackprop(
		float[][][] oldWeight, float decayTowardNorm,
		//float[][][] derivUp, float[][][] derivDown,
		float[][] targetNodeChance,
		float[][] zigzagUp,
		float[][] zigzagDown,
		float[][] Att
	){
		if(targetNodeChance.length != oldWeight.length+1) throw new Error(
			"Incompatible array sizes. targetNodeChance.length="+targetNodeChance.length
			+" oldWeight.length(should be 1 less)="+oldWeight.length);
		float[][][] newWeight = new float[oldWeight.length][][];
		float[] nolayNeedNorm = new float[targetNodeChance.length];
		//for(int n=0; n<nolayNeedNorm.length; n++){
		//	nolayNeedNorm[n] = RBM.needNormHowMuchFraction(targetNodeChance[n], )
		//}
		for(int edlay=0; edlay<oldWeight.length; edlay++){
			//Higher power makes it norm faster when its more unbalanced.
			//With a higher power, the decayTowardNorm param
			//can be much higher and just as smooth when near normed,
			//but then it may not norm enough to adjust for learning, so its a subtle balance.
			//float pow = 1; //TODO 1.5? What number?
			//float pow = 2; //TODO 1.5? What number?
			float pow = 1.5f; //TODO 1.5? What number?
			float needNormDown = (float)Math.pow(
				RBM.needNormHowMuchFraction(targetNodeChance[edlay], zigzagDown[edlay], Att[edlay]),pow);
			float needNormUp = (float)Math.pow(
				RBM.needNormHowMuchFraction(targetNodeChance[edlay+1], zigzagUp[edlay+1], Att[edlay+1]),pow);
			float needNorm = (needNormDown+needNormUp)/2;
			System.out.println("edlay"+edlay+" needNorm="+needNorm);
			float localDecayTowardNorm = decayTowardNorm*needNorm;
			newWeight[edlay] = normWeightBidirectionalBackprop(
				oldWeight[edlay], localDecayTowardNorm,
				//derivUp[edlay], derivDown[edlay],
				targetNodeChance[edlay], targetNodeChance[edlay+1],
				zigzagUp[edlay], zigzagUp[edlay+1],
				zigzagDown[edlay], zigzagDown[edlay+1],
				Att[edlay], Att[edlay+1]
			);
		}
		return newWeight;
	}
	
	
	//static float testAdd20;
	
	/** Example: If target chance is .25 and observed is 1,
	lower it 3 times faster than raise it if observed 0,
	so no need to keep statistic of average node value (which caused vibration so this replaces that code).
	<br><br>
	OPTIMIZATION: This could have been designed to chain up and down norming as 2 funcs,
	but its faster to only create the new weight array once.
	Separate would be easier to understand, but they would always be used together anyways.
	<br><br>
	FIXME Should this use Att?
	<br>
	<br>//This block of text is various designs I considered before deciding on one, the last line.
	<br>//
	<br>//From here, theres a bipartite net
	<br>//with derivative numbers on each edge (eXY and eYX) and node (nX nY).
	<br>//We want to find a multiplier mXY for each [eXY with eYX] so
	<br>//at each nX the sum of mXY*eXY equals nX, and at each nY the sum of mXY*eYX equals nY,
	<br>//while minimizing the sumOfSquares of mXY.
	<br>//In rare cases it will not be possible to solve the equation exactly,
	<br>//but while the target chances and node layer sizes are not too different it should be.
	<br>//TODO How to choose each mXY toward that goal?
	<br>//
	<br>//At each mXY think of it as mAve at both and directionally plus/minus mDev.
	<br>//
	<br>//TODO?[
	<br>//	https:<br>//www.reddit.com/r/askmath/comments/6po20s/for_any_given_random_vectors_find_a_weighted_sum/
	<br>//	If Ax=b and the rows of A are linearly independent (in your case where you have more variables
	<br>//than equations) the smallest solution x is given by x = A* (AA* )-1 b, where A* is denoted as
	<br>//the transpose of A.
	<br>//]
	<br>// https:<br>//en.wikipedia.org/wiki/Invertible_matrix https:<br>//en.wikipedia.org/wiki/Transpose
	<br>//
	<br>//Or... Norm each (nX,eXY,eYX,nY) so every n* is 1,
	<br>//so looking for sum of edges that make all their nodes be approx 1,
	<br>//where each edge has 2 numbers which may be positive or negative.
	<br>//This isnt exactly balanced in giving higher priority to very small n*
	<br>//such as n=.01 gets twice as much priority as n=.02 when neither of those is much important
	<br>//since they're so small, but on average nodes will be different by a ratio of .9 to 1.1,
	<br>//which gets adjusted continuously to stay near that, so its ok.
	<br>//Yes, pursue this path.
	<br>//How would I choose a weightedSet of such edges to make all nodes sum to near 1?
	<br>//To adjust for differences in scale before norming,
	<br>//have a weight per node that the sumOfSquares is multiplied by.
	<br>//
	<br>//No, here's how going to do it: Norm by manhattanDistance and add the derivatives directly to each side.
	*
	public static float[][] normWeightBidirectionalBackprop(
		float[][] oldWeight, float decayTowardNorm,
		float[][] derivUp, float[][] derivDown,
		float[] downTargetChance, float[] upTargetChance,
		float[] lowNodesDuringUp, float[] highNodesDuringUp,
		float[] lowNodesDuringDown, float[] highNodesDuringDown
	){
		int lowNodes = oldWeight.length, highNodes = oldWeight[0].length;
		float[] manhattanDown = new float[lowNodes];
		float[] manhattanUp = new float[highNodes];
		for(int low=0; low<lowNodes; low++){
			for(int high=0; high<highNodes; high++){
				float mag = Math.abs(oldWeight[low][high]);
				manhattanDown[low] += mag; //ERROR, this was supposed to be from derivDown
				manhattanUp[high] += mag;  //ERROR, this was supposed to be from derivUp
			}
		}
		float[] multLow = new float[lowNodes], multHigh = new float[highNodes];
		for(int low=0; low<lowNodes; low++){
			float downTryAddToChance = (downTargetChance[low]-lowNodesDuringDown[low])*decayTowardNorm;
			if(manhattanDown[low] == 0) throw new Err("All backprop derivatives are 0 for low node "+low
				+". This norm was either called before statistics accumulate or learning is broken or astronomically small chance happened.");
			multLow[low] = downTryAddToChance/manhattanDown[low];
		}
		for(int high=0; high<highNodes; high++){
			float upTryAddToChance = (upTargetChance[high]-highNodesDuringUp[high])*decayTowardNorm;
			if(manhattanUp[high] == 0) throw new Err("All backprop derivatives are 0 for high node "+high
				+". This norm was either called before statistics accumulate or learning is broken or astronomically small chance happened.");
			multHigh[high] = upTryAddToChance/manhattanUp[high];
		}
		float[][] newWeight = new float[lowNodes][highNodes];
		for(int low=0; low<lowNodes; low++){
			for(int high=0; high<highNodes; high++){
				float addLow = derivDown[low][high]*multLow[low];
				float addHigh = derivUp[low][high]*multHigh[high];
				newWeight[low][high] = oldWeight[low][high] + addLow + addHigh;
			}
		}
		System.out.println("add[20][20]="+(newWeight[20][20]-oldWeight[20][20])+" oldWeight[20][20]="+oldWeight[20][20]);
		//System.out.println("derivUp[20][20]="+derivUp[20][20]+" TODO approx what range should this deriv be?");
		System.out.println("downTargetChance[20]="+downTargetChance[20]+" upTargetChance[20]="+upTargetChance[20]);
		System.out.println("manhattanDown[20]="+manhattanDown[20]+" manhattanUp[20]="+manhattanUp[20]);
		return newWeight;
	}*/
	
	
	
	public static float[][] normWeightBidirectionalBackprop(
		float[][] oldWeight, float decayTowardNorm,
		float[] downTargetChance, float[] upTargetChance,
		float[] lowNodesDuringUp, float[] highNodesDuringUp,
		float[] lowNodesDuringDown, float[] highNodesDuringDown,
		float[] lowNodesAtt, float[] highNodesAtt //FIXME verify each Att array sums to 1 (except roundoff)
	){
		throw new Error("TODO use biasPerNolay");
		/*int lowNodes = oldWeight.length, highNodes = oldWeight[0].length;
		float[] downScalars = sigmoid(sumDown(highNodesAtt, highNodesDuringDown, oldWeight));
		float[] upScalars = sigmoid(sumUp(lowNodesAtt, lowNodesDuringUp, oldWeight));
		//float nodeSumLowDuringUp = dotProd(lowNodesDuringUp,lowNodesAtt);
		//float nodeSumHighDuringDown = dotProd(highNodesDuringDown,highNodesAtt);
		//if(nodeSumLowDuringUp==0 || nodeSumHighDuringDown==0) throw new Error(
		//	"nodeSumLowDuringUp="+nodeSumLowDuringUp+" nodeSumHighDuringDown="+nodeSumHighDuringDown);
		float[][] newWeight = new float[lowNodes][highNodes];
		for(int low=0; low<lowNodes; low++){
			for(int high=0; high<highNodes; high++){
				
				//TODO OPTIMIZE, MOST OF THE CODE IN THIS DOUBLE LOOP CAN BE MOVED TO 2 SINGLE LOOPS
				
				//derivative of sigmoid(x)=1/(1+e^-x) is sigmoid*(1-sigmoid)
				float derivDown = downScalars[low]*(1-downScalars[low]);
				float derivUp = upScalars[high]*(1-upScalars[high]);
				if(derivUp==0 || derivDown==0) throw new Error("derivDown="+derivDown+" derivUp="+derivUp);
				
				float downTryAddToChance = (downTargetChance[low]-downScalars[low])*decayTowardNorm*lowNodesAtt[low];
				float upTryAddToChance = (upTargetChance[high]-upScalars[high])*decayTowardNorm*highNodesAtt[high];
				
				//FIXME TODO verify this math. Should be instant bidirectional backprop norm.
				float addLow = downTryAddToChance/derivDown * highNodesDuringDown[high]*highNodesAtt[high];
				float addHigh = upTryAddToChance/derivUp * lowNodesDuringUp[low]*lowNodesAtt[low];
				//float addLow = downTryAddToChance/derivDown * highNodesDuringDown[high]/nodeSumHighDuringDown;
				//float addHigh = upTryAddToChance/derivUp * lowNodesDuringUp[low]/nodeSumLowDuringUp;
				//float addLow = downTryAddToChance/lowNodes; //FIXME
				//float addHigh = upTryAddToChance/highNodes; //FIXME
				
				//float addLow = derivDown[low][high]*multLow[low];
				//float addHigh = derivUp[low][high]*multHigh[high];
				newWeight[low][high] = oldWeight[low][high] + addLow + addHigh;
			}
		}
		//float add20 = (newWeight[20][20]-oldWeight[20][20]);
		//float decayAdd20 = .01f;
		//testAdd20 = testAdd20*(1-decayAdd20) + decayAdd20*add20;
		
		//System.out.println("add[20][20]="+add20+" oldWeight[20][20]="+oldWeight[20][20]);
		//System.out.println("downTargetChance[20]="+downTargetChance[20]+" upTargetChance[20]="+upTargetChance[20]);
		return newWeight;
		*/
	}
	/*public static float[][] normWeightBidirectionalBackprop(
		float[][] oldWeight, float decayTowardNorm,
		float[] downTargetChance, float[] upTargetChance,
		float[] lowNodesDuringUp, float[] highNodesDuringUp,
		float[] lowNodesDuringDown, float[] highNodesDuringDown,
		float[] lowNodesAtt, float[] highNodesAtt
	){
		int lowNodes = oldWeight.length, highNodes = oldWeight[0].length;
		float[] lowNodeAtts = new float[lowNodes];
		float[] highNodeAtts = new float[highNodes];
		Arrays.fill(lowNodeAtts, 1f/lowNodes);
		Arrays.fill(highNodeAtts, 1f/highNodes);
		float[] downScalars = sigmoid(sumDown(highNodeAtts, highNodesDuringDown, oldWeight));
		float[] upScalars = sigmoid(sumUp(lowNodeAtts, lowNodesDuringUp, oldWeight));
		float nodeSumLowDuringUp = sum(lowNodesDuringUp);
		float nodeSumHighDuringDown = sum(highNodesDuringDown);
		if(nodeSumLowDuringUp==0 || nodeSumHighDuringDown==0) throw new Error(
			"nodeSumLowDuringUp="+nodeSumLowDuringUp+" nodeSumHighDuringDown="+nodeSumHighDuringDown);
		float[][] newWeight = new float[lowNodes][highNodes];
		for(int low=0; low<lowNodes; low++){
			for(int high=0; high<highNodes; high++){
				
				//TODO OPTIMIZE, MOST OF THE CODE IN THIS DOUBLE LOOP CAN BE MOVED TO 2 SINGLE LOOPS
				
				//derivative of sigmoid(x)=1/(1+e^-x) is sigmoid*(1-sigmoid)
				float derivDown = downScalars[low]*(1-downScalars[low]);
				float derivUp = upScalars[high]*(1-upScalars[high]);
				if(derivUp==0 || derivDown==0) throw new Error("derivDown="+derivDown+" derivUp="+derivUp);
				
				float downTryAddToChance = (downTargetChance[low]-downScalars[low])*decayTowardNorm;
				float upTryAddToChance = (upTargetChance[high]-upScalars[high])*decayTowardNorm;
				
				//FIXME TODO verify this math. Should be instant bidirectional backprop norm.
				float addLow = downTryAddToChance/derivDown * highNodesDuringDown[high]/nodeSumHighDuringDown;
				float addHigh = upTryAddToChance/derivUp * lowNodesDuringUp[low]/nodeSumLowDuringUp;
				//float addLow = downTryAddToChance/lowNodes; //FIXME
				//float addHigh = upTryAddToChance/highNodes; //FIXME
				
				TODO use lowNodesAtt and highNodesAtt. Align to how weights change by that multiply.
				
				//float addLow = derivDown[low][high]*multLow[low];
				//float addHigh = derivUp[low][high]*multHigh[high];
				newWeight[low][high] = oldWeight[low][high] + addLow + addHigh;
			}
		}
		float add20 = (newWeight[20][20]-oldWeight[20][20]);
		//float decayAdd20 = .01f;
		//testAdd20 = testAdd20*(1-decayAdd20) + decayAdd20*add20;
		
		System.out.println("add[20][20]="+add20+" oldWeight[20][20]="+oldWeight[20][20]);
		System.out.println("downTargetChance[20]="+downTargetChance[20]+" upTargetChance[20]="+upTargetChance[20]);
		return newWeight;
	}*/
	
	/** returns float[zigzagIndex][nolay][nodeInNolay] */
	public static float[][][] fullUpThenZigzagWithinEachEdlay(
			int zigzags, float[][] Att, float[] visibleNodes, float[][][] weight, int lowNNolaysAreScalar, Random rand, float[][] biasPerNodeside){
		int nolays = weight.length+1;
		float[][][] ret = new float[zigzags][nolays][];
		ret[0][0] = visibleNodes; //its ok to return this cuz arrays must be used as immutable
		for(int nolayOut=1; nolayOut<nolays; nolayOut++){
			//float[] highNodesBias = biasPerNolay[nolayOut];
			float[] highNodesBias = biasPerNodeside[nolayOut*2+1];
			boolean nolayOutIsScalar = nolayOut<lowNNolaysAreScalar;
			ret[0][nolayOut] = up(Att[nolayOut-1], ret[0][nolayOut-1], weight[nolayOut-1], nolayOutIsScalar?null:rand, highNodesBias);
		}
		//TODO optimize by threading per edlay using Parallel class's ForkJoinPool.
		for(int z=1; z<zigzags; z++){
			if((z&1)==1){ //down per edlay
				ret[z][nolays-1] = ret[z-1][nolays-1]; //same top node layer as last zigorzag
				for(int nolayOut=0; nolayOut<nolays-1; nolayOut++){ //inference zigzagging per edlay
					float[] lowNodesBias = biasPerNodeside[nolayOut*2];
					boolean nolayOutIsScalar = nolayOut<lowNNolaysAreScalar;
					ret[z][nolayOut] = down(Att[nolayOut+1], ret[z-1][nolayOut+1], weight[nolayOut], nolayOutIsScalar?null:rand, lowNodesBias);
				}
			}else{ //up per edlay
				ret[z][0] = ret[z-1][0]; //same visibleNode layer as last zigorzag
				for(int nolayOut=1; nolayOut<nolays; nolayOut++){ //inference zigzagging per edlay
					float[] highNodesBias = biasPerNodeside[nolayOut*2+1];
					boolean nolayOutIsScalar = nolayOut<lowNNolaysAreScalar;
					ret[z][nolayOut] = up(Att[nolayOut-1], ret[z-1][nolayOut-1], weight[nolayOut-1], nolayOutIsScalar?null:rand, highNodesBias);
				}
			}
		}
		return ret;
	}
	
	/** Since all arrays in this software are used as immutable, its ok to copy just the pointer */
	static float[][] arrayOfPtrsTo(int outerSize, float[] a){
		float[][] aa = new float[outerSize][];
		Arrays.fill(aa, a);
		return aa;
	}
	
	public static boolean gpu = true;
	
	/** gpu optimized in parallel. float[zigzagIndex][nolay][vecIndex][nodeInNolay].
	vecsVisibleNodes[vecIndex][nodeInNolay0].
	AttMerged is attLev1*attLev2 (and future version of this software will probably have attLev3)
	*/
	public static float[][][][] vecsZigzag(int zigzags, float[][] attMerged, float[][] vecsVisibleNodes,
			float[][][] weight, int lowNNolaysAreScalar, boolean individualZigzagsAfterFullUp, float[][] biasPerNodeside){
		if(gpu){
			int nolays = weight.length+1;
			for(int n=0; n<nolays; n++){
				if(biasPerNodeside[n*2].length != biasPerNodeside[n*2+1].length){
					throw new Error("sizes dont match");
				}
			}
			for(int nolayside=0; nolayside<nolays*2; nolayside++){
				int aNolay = nolayside/2;
				int nolaySize = RBM.nolaySize(weight,aNolay);
				if(biasPerNodeside[nolayside].length != nolaySize){
					throw new Error("sizes dont match");
				}
			}
			if(biasPerNodeside.length != nolays*2){
				throw new Error("sizes dont match");
			}
			//ret[zigzagIndex][nolay][vecIndex][nodeInNolay].
			float[][][][] ret = new float[zigzags][nolays][][];
			int vecs = vecsVisibleNodes.length;
			float[][][] Atts = new float[nolays*2][vecs][];
			for(int nolayside=0; nolayside<nolays*2; nolayside++){
				for(int v=0; v<vecs; v++){
					Atts[nolayside][v] = attMerged[nolayside];
				}
			}
			
			if(individualZigzagsAfterFullUp){ //other kind of learning, never used for predicting visibleNodes
				
				/* TODO optimize extremely, lag of GPU by doing those zigzags in parallel after the first fullUp,
				and dont need to return from opencl to java until all those zigzags are done since its the same float[][] weights.
				It can be done in about edlays times less steps that are each that much bigger but still needs to be
				multiple opencl kernels in some combo of parallel and sequential clqueues andOr dependnet,
				which would require a more flexible API wrapping lwjgl than my OpenclUtil just does 1 kernel
				per java call as of 2018-4-24. lwjgl is flexible enough to do that efficiently as it has
				pointers to opencl objects and can do things between them without copying between java memory until you tell it to.
				*/
				
				ret[0][0] = vecsVisibleNodes;
				for(int nolayOut=1; nolayOut<nolays; nolayOut++){ //inference from visibleNodes all the way up
					//float bias = biasPerNolay[nolayOut];
					float[] highNolayBias = biasPerNodeside[nolayOut*2+1];
					boolean highNolayIsScalar = nolayOut<lowNNolaysAreScalar;
					ret[0][nolayOut] = vecsUp(Atts[nolayOut-1], ret[0][nolayOut-1],
						weight[nolayOut-1], highNolayIsScalar, highNolayBias);
					throwIfHasAnyNans(ret[0][nolayOut],-1000,-1000); //FIXME remove this slow test
				}
				for(int z=1; z<zigzags; z+=2){
					//FIXME verify this code does individualZigzagsAfterFullUp
					ret[z][nolays-1] = ret[z-1][nolays-1]; //same top node layer as last zigorzag
					for(int nolayOut=nolays-2; nolayOut>=0; nolayOut--){ //each edlay down 1 individually
						//TODO optimize extremely, this loop parallel and move it outside the now outer loop
						float[] lowNolayBias = biasPerNodeside[nolayOut*2];
						boolean lowNolayIsScalar = nolayOut<lowNNolaysAreScalar;
						ret[z][nolayOut] = vecsDown(Atts[nolayOut+1], ret[z-1][nolayOut+1],
							weight[nolayOut], lowNolayIsScalar, lowNolayBias);
						throwIfHasAnyNans(ret[z][nolayOut],-1000,-1000); //FIXME remove this slow test
					}
					if(z+1 < zigzags){
						//TODO optimize extremely, this loop parallel and move it outside the now outer loop
						ret[z+1][0] = ret[z][0]; //same visibleNode layer as last zigorzag
						for(int nolayOut=1; nolayOut<nolays; nolayOut++){ //each edlay up 1 individually
							float[] highNolayBias = biasPerNodeside[nolayOut*2+1];
							boolean highNolayIsScalar = nolayOut<lowNNolaysAreScalar;
							ret[z+1][nolayOut] = vecsUp(Atts[nolayOut-1], ret[z][nolayOut-1],
								weight[nolayOut-1], highNolayIsScalar, highNolayBias);
							throwIfHasAnyNans(ret[z+1][nolayOut],-1000,-1000); //FIXME remove this slow test
						}
					}
				}
			}else{ //normal predicting (which can be used for a kind of learning), not individualZigzagsAfterFullUp
				for(int z=0; z<zigzags; z+=2){
					ret[z][0] = z==0 ? vecsVisibleNodes : ret[z-1][0]; //same visibleNode layer as last zigorzag
					for(int nolayOut=1; nolayOut<nolays; nolayOut++){ //inference from visibleNodes all the way up
						float[] highNolayBias = biasPerNodeside[nolayOut*2+1];
						boolean highNolayIsScalar = nolayOut<lowNNolaysAreScalar;
						ret[z][nolayOut] = vecsUp(Atts[nolayOut-1], ret[z][nolayOut-1],
							weight[nolayOut-1], highNolayIsScalar, highNolayBias);
					}
					if(z+1 < zigzags){
						ret[z+1][nolays-1] = ret[z][nolays-1]; //same top node layer as last zigorzag
						for(int nolayOut=nolays-2; nolayOut>=0; nolayOut--){ //inference from top layer all the way down
							float[] lowNolayBias = biasPerNodeside[nolayOut*2];
							boolean lowNolayIsScalar = nolayOut<lowNNolaysAreScalar;
							ret[z+1][nolayOut] = vecsDown(Atts[nolayOut+1], ret[z+1][nolayOut+1],
								weight[nolayOut], lowNolayIsScalar, lowNolayBias);
						}
					}
				}
			}
			throwIfHasAnyNans(ret); //FIXME this is a very slow test cuz checking for nans removes nan optimizations in hardware
			Lg.todo("Commentout throwIfHasAnyNans cuz its a very slow test");
			return ret;
		}else{
			if(individualZigzagsAfterFullUp) throw new Error("TODO");
			//FIXME just looping over old single vec code in cpu for now.
			//OLD: [zigzagIndex][nolay][nodeInNolay]
			//NEW: [zigzagIndex][nolay][vecIndex][nodeInNolay]
			int vecs = vecsVisibleNodes.length;
			int nolays = weight.length+1;
			float[][][][] ret = new float[zigzags][nolays][vecs][];
			
			/*float[][][][] oldCodeCalls = new float[vecs][][][];
			Parallel.forkAndWait(vecs, (int v)->{
				oldCodeCalls[v] = zigzag(zigzags, Att, vecsVisibleNodes[v], weight, lowNNolaysAreScalar, Rand.strongRand);
			});
			for(int v=0; v<vecs; v++){
				float[][][] oldZZ = oldCodeCalls[v];
				for(int z=0; z<zigzags; z++){
					for(int n=0; n<nolays; n++){
						ret[z][n][v] = oldZZ[z][n];
					}
				}
			}*/
			
			for(int v=0; v<vecs; v++){
			//Parallel.forkAndWait(vecs, (int v)->{
				float[][][] oldZZ = zigzag(zigzags, attMerged, vecsVisibleNodes[v], weight, lowNNolaysAreScalar, Rand.strongRand, biasPerNodeside);
				if(oldZZ == null){
					throw new Error("WTF");
				}
				for(int z=0; z<zigzags; z++){
					for(int n=0; n<nolays; n++){
						//synchronized(ret[z][n]){
							ret[z][n][v] = oldZZ[z][n];
						//}
					}
				}
			//});
			}
			return ret;
		}
	}
	
	/** returns float[zigzagIndex][nolay][nodeInNolay]
	FIXME, READ THIS CODE AND VERIFY ITS CORRECT
	*/
	public static float[][][] zigzag(int zigzags, float[][] Att, float[] visibleNodes,
			float[][][] weight, int lowNNolaysAreScalar, Random rand, float[][] biasPerNodeside){
		int nolays = weight.length+1;
		float[][][] ret = new float[zigzags][nolays][];
		ret[0][0] = visibleNodes; //its ok to return this cuz arrays must be used as immutable
		for(int z=0; z<zigzags; z+=2){
			//TODO verify the logic in these 2 loops. Wrote it quickly,
			//copying the behavior of earlier version of the software which was more complex.
			ret[z][0] = z==0 ? visibleNodes : ret[z-1][0]; //same visibleNode layer as last zigorzag
			for(int nolayOut=1; nolayOut<nolays; nolayOut++){ //inference from visibleNodes all the way up
				float[] highNodesBias = biasPerNodeside[nolayOut*2+1];
				boolean nolayOutIsScalar = nolayOut<lowNNolaysAreScalar;
				ret[z][nolayOut] =
					up(Att[nolayOut-1], ret[z][nolayOut-1], weight[nolayOut-1], nolayOutIsScalar?null:rand, highNodesBias);
				//if(z==0) ret[z][nolayOut] = ret[z][nolayOut-1]; //FIXME this is a test of zigzag symmetry
			}
			if(z+1 < zigzags){
				ret[z+1][nolays-1] = ret[z][nolays-1]; //same top node layer as last zigorzag
				for(int nolayOut=nolays-2; nolayOut>=0; nolayOut--){ //inference from top layer all the way down
					float[] lowNodesBias = biasPerNodeside[nolayOut*2];
					boolean nolayOutIsScalar = nolayOut<lowNNolaysAreScalar;
					ret[z+1][nolayOut] =
						down(Att[nolayOut+1], ret[z+1][nolayOut+1], weight[nolayOut], nolayOutIsScalar?null:rand, lowNodesBias);
				}
			}
		}
		return ret;
	}
	
	public static float[] getPredictionOutOfZigzag(float[][][] zigzag){
		return zigzag[zigzag.length-1][0];
	}
	
	/* commentedout cuz should use (Consumer<LearnLoopParam>) learnFuncCompiled() instead. 
	//FIXME make LearnFunc a param of RBM, but it has to (FIXME later do this) be storable in the serialized, json, and ufnode forms
	//without needing the class bytecode or sourcecode outside that stored form.
	static final Consumer<LearnLoopParam> experimentalLearnFunc = (LearnLoopParam p)->{
		
		//FIXME check p.lowNodeIsBias and p.highNodeIsBias
		
		float att = p.lowNodeAtt*p.highNodeAtt;
		float diff = p.learnRate*att*(p.toLearn-p.toUnlearn);
		float diffScaled = diff/p.batchSize; //spread learnRates across vecs instead of each.
		//ret[low][high] = weight[low][high] + diff; //This was the code for a long time up to 2018-4-23
		float decay = p.weightDecay*diffScaled*diffScaled;
		//FIXME SHOULD IT INSTEAD BE?: ret[low][high] = diff - decay*weight[low][high];
		//DEPENDS IF WANT WEIGHTDECAY TO AFFECT VELOCITY OR JUST POSITION (OF weight[][][]).
		//ret[low][high] = diff;
		//I'LL MAKE WEIGHTDECAY AFFECT VELOCITY SO IT WONT BE OVERPOWERED BY OTHER THINGS THAT AFFECT VELOCITY.
		//return diffScaled - decay*weight;
		float deriv = diffScaled - decay*p.weight;
		
		float dt = 1; //FIXME this was the old code but its buggy cuz when learnRate changes, weightVelocityDecay does not, but learnRate*weightVelocityDecay would.
		//float dt = TODO; //If dt is learnRate, need to scale learnRate to be near 0 and never more than 1.
		
		//return diff +.1f - decay*weight;
		//ret[low][high] = diff;
		
		//FIXME could this be causing aveEdlay1WeightsBecomingEverMoreNegativeMustBeReproducibleIn1EdlayWithSameInputsThatEdlay0DidOutputAndMustBeFixableSameWayItsFixedInEdlay0?
			
		p.returnWeightVelocity = p.weightVelocity*(1-dt*p.weightVelocityDecay) + dt*deriv;
		p.returnWeight = p.weight + dt*p.weightVelocity;
	};*/
	
	/** vecs(Low|High)NodesTo(Learn|Unlearn)[vec][(low|high)Node]. learnRates are total, not per vec.
	UPDATED DESIGN: Returns derivative instead of directly weight (in case you want to do neural moomentum, or not).
	Even though weight[][] is not directly included in the returned deriv[][], it is used for weightDecay to affect velocity
	instead of to act on weight directly, cuz else it would be overpowered by other things that affect velocity
	or at least out of sync with them since velocity extremely varies. Its important that weightDecay be a function
	of the change to each specific float weight and specific change to it as:
	velocity += thisReturns[][index] = (diff - (weightDecay*diff*diff)*weight[low][high]).
	This is intheory but I havent tested it yet as of 2018-5-6-10a, expecting it to work that way
	cuz its been working when weight += (diff - (weightDecay*diff*diff)*weight[low][high])
	and I'm changing it to neural momentum (weightVelocity array and a second decay param).
	*/
	public static WeightAndVelOfEdlay vecsLearnToEdlay(String learnFunc,
			int edlay, float learnRate, float unlearnRate, float[][] weight, float[][] weightVelocity,
			float[] lowNodeAtt, float[] highNodeAtt,
			float[][] vecsLowNodesToLearn, float[][] vecsHighNodesToLearn,
			float[][] vecsLowNodesToUnlearn, float[][] vecsHighNodesToUnlearn, float weightDecay, float weightVelDecay){
		//boolean debuggable = true; //jdk
		boolean debuggable = false; //javassist, faster but no debug info
		boolean pauseSoPersonCanPutBreakpoint = false; //should be false
		//boolean pauseSoPersonCanPutBreakpoint = true;
		JavaCompiler compiler = JavaCompilers.get(debuggable, pauseSoPersonCanPutBreakpoint);
		LearnLoop loop = LearnLoop.newInstanceSandboxed(learnFunc,compiler); //reuses runtime compiled Class fast on future calls
		loop.batchSize = vecsLowNodesToLearn.length;
		if(vecsHighNodesToLearn.length != loop.batchSize || vecsLowNodesToUnlearn.length != loop.batchSize || vecsHighNodesToUnlearn.length != loop.batchSize)
			throw new Error("Different number of vecs in the 4 arrays of low/high nodes and to learn/unlearn");
		loop.lows = weight.length;
		loop.highs = weight[0].length;
		loop.edlay = edlay;
		loop.learnRate = learnRate;
		loop.unlearnRate = unlearnRate;
		loop.theWeight = weight;
		loop.theWevel = weightVelocity;
		loop.vecsLowNodesToLearn = vecsLowNodesToLearn;
		loop.vecsLowNodesToUnlearn = vecsLowNodesToUnlearn;
		loop.vecsHighNodesToLearn = vecsHighNodesToLearn;
		loop.vecsHighNodesToUnlearn = vecsHighNodesToUnlearn;
		loop.weightDecay = weightDecay;
		loop.wevelDecay = weightVelDecay;
		loop.tolowNodeAtt = lowNodeAtt;
		loop.tohighNodeAtt = highNodeAtt;
		loop.run();
		return new WeightAndVelOfEdlay(loop.returnWeight, loop.returnWevel);
	}
	
	public static WeightAndVelOfEdlay vecsLearnToEdlay_OLDAndBrokenDuringRedesign(String learnFunc, int edlay, float learnRate, float unlearnRate, float[][] weight, float[][] weightVelocity,
			float[] lowNodeAtt, float[] highNodeAtt,
			float[][] vecsLowNodesToLearn, float[][] vecsHighNodesToLearn,
			float[][] vecsLowNodesToUnlearn, float[][] vecsHighNodesToUnlearn, float weightDecay, float weightVelocityDecay){
		int lows = vecsLowNodesToLearn[0].length, highs = vecsHighNodesToLearn[0].length;
		int vecs = vecsLowNodesToLearn.length;
		if(learnRate != -unlearnRate) throw new Error(learnRate+" == learnRate != unlearnRate == "+unlearnRate+" I plan to merge those 2 after the experiment extremely failed where they were allowed to differ even slightly.");
		if(gpu){
			
			Consumer<LearnLoopParam_OLD> learnFuncCompiled = LearnLoopParam_OLD.compileSandboxed(learnFunc); //looks up in map after first time
			
			/*TODO can this efficiently be modified to have a different learnRate per vec?
			Want that to turn off learning for the test dataset.
			Not here, instead should be done where RBM.learn is called (in PaintSlidingVecUi).
			*/
			
			//TODO optimize lag, this (vecsLearnToEdlay) func is called in sequence for edlays but they could be done in parallel,
			//and so could the 2 calls of matmul here.
			float[][] learnLowHigh = OpenclProgs.matmul(swapDims(vecsLowNodesToLearn), vecsHighNodesToLearn);
			float[][] unlearnLowHigh = OpenclProgs.matmul(swapDims(vecsLowNodesToUnlearn), vecsHighNodesToUnlearn);
			
			LearnLoopParam_OLD p = new LearnLoopParam_OLD();
			
			//FIXME these are named wrong so wont match in dynamic code string
			
			//optimization: only loop over weights to set this if RBM.learnCode contains this var name
			//TODO optimize by storing these booleans in a new class that goes in value of compileCache map in LearnLoopParam:
			boolean computefromLowWeightsAveNoatt = learnFunc.contains("fromLowWeightAveNoatt");
			boolean computefromlowWeightsDevNoatt = learnFunc.contains("fromlowWeightDevNoatt");
			boolean computefromlowWeightsMedNoatt = learnFunc.contains("fromlowWeightMedNoatt");
			boolean computefromhighWeightsAveNoatt = learnFunc.contains("fromhighWeightsAveNoatt");
			boolean computefromhighWeightsDevNoatt = learnFunc.contains("fromhighWeightDevNoatt");
			boolean computefromhighWeightsMedNoatt = learnFunc.contains("fromhighWeightMedNoatt");
			
			boolean computefromhighWeightsAveByatt = learnFunc.contains("fromhighWeightAveByatt");
			boolean computefromlowWeightsDevByatt = learnFunc.contains("fromlowWeightDevByatt");
			boolean computefromlowWeightsMedByatt = learnFunc.contains("fromlowWeightMedByatt");
			boolean computefromlowWeightsAveByatt = learnFunc.contains("fromlowWeightAveByatt");
			boolean computefromhighWeightsDevByatt = learnFunc.contains("fromhighWeightDevByatt");
			boolean computefromhighWeightsMedByatt = learnFunc.contains("fromhighWeightMedByatt");
			
			boolean computefromlowWeightsMin = learnFunc.contains("fromlowWeightMin");
			boolean computefromhighWeightsMin = learnFunc.contains("fromhighWeightMin");
			boolean computefromlowWeightsMax = learnFunc.contains("fromlowWeightMax");
			boolean computefromhighWeightsMax = learnFunc.contains("fromhighWeightMax");
			
			//TODO? This is a doubleloop in cpu so probably wont bottleneck gpu doing tripleloop,
			//but if it does then do this in gpu and pay the extra lag.
			
			//FIXME check the compute* vars
			//FIXME set the LearnLoopParam.* fields if those compute* vars say to. This code is incomplete as of 2018-6-4-10a.
			boolean aveAndDevNoatt =
				computefromLowWeightsAveNoatt ||
				computefromlowWeightsDevNoatt ||
				computefromhighWeightsAveNoatt ||
				computefromhighWeightsDevNoatt;
			boolean aveAndDevByatt =
				computefromlowWeightsAveByatt ||
				computefromlowWeightsDevByatt ||
				computefromhighWeightsAveByatt ||
				computefromhighWeightsDevByatt;
			float[] fromhighWeightsAveNoatt = null;
			float[] fromlowWeightsAveNoatt = null;
			float[] lowWeightDevNoatt = null;
			float[] highWeightDevNoatt = null;
			float[] lowWeightAveByatt = null;
			float[] highWeightAveByatt = null;
			float[] lowAttsum = null;
			float[] highAttsum = null;
			float[] lowWeightDevByatt = null;
			float[] highWeightDevByatt = null;
			float[] fromlowWeightsMedNoatt = null;
			float[] fromhighWeightsMedNoatt = null;
			float[] fromlowWeightsMedByatt = null;
			float[] fromhighWeightsMedByatt = null;
			float[] fromlowWeightsMin = null;
			float[] fromlowWeightsMax = null;
			float[] fromhighWeightsMin = null;
			float[] fromhighWeightsMax = null;
			if(aveAndDevNoatt){
				fromhighWeightsAveNoatt = new float[lows];
				fromlowWeightsAveNoatt = new float[highs];
				for(int low=0; low<lows; low++){
					for(int high=0; high<highs; high++){
						float w = weight[low][high];
						fromhighWeightsAveNoatt[low] += w;
						fromlowWeightsAveNoatt[high] += w;
					}
				}
				for(int low=0; low<lows; low++) fromhighWeightsAveNoatt[low] /= lows;
				for(int high=0; high<highs; high++) fromlowWeightsAveNoatt[high] /= highs;
				lowWeightDevNoatt = new float[lows];
				highWeightDevNoatt = new float[highs];
				for(int low=0; low<lows; low++){
					for(int high=0; high<highs; high++){
						float w = weight[low][high];
						float lowDiffNoatt = w-fromhighWeightsAveNoatt[low];
						lowWeightDevNoatt[low] += lowDiffNoatt*lowDiffNoatt;
						float highDiffNoatt = w-fromlowWeightsAveNoatt[high];
						highWeightDevNoatt[high] += highDiffNoatt*highDiffNoatt;
					}
				}
				for(int low=0; low<lows; low++) lowWeightDevNoatt[low] = (float)Math.sqrt(lowWeightDevNoatt[low]/lows);
				for(int high=0; high<highs; high++) highWeightDevNoatt[high] = (float)Math.sqrt(highWeightDevNoatt[high]/highs);
			}
			if(aveAndDevByatt){
				lowAttsum = new float[lows]; //sums of high Atts per low index
				highAttsum = new float[highs]; //sums of low Atts per high index
				lowWeightAveByatt = new float[lows];
				highWeightAveByatt = new float[highs];
				for(int low=0; low<lows; low++){
					for(int high=0; high<highs; high++){
						float w = weight[low][high];
						lowWeightAveByatt[low] += w*highNodeAtt[high]; //FIXME? is this reversed?
						lowAttsum[low] += highNodeAtt[high];
						highWeightAveByatt[high] += w*lowNodeAtt[low]; //FIXME? is this reversed?
						highAttsum[high] += lowNodeAtt[low];
					}
				}
				for(int low=0; low<lows; low++) lowWeightAveByatt[low] /= lowAttsum[low]; //WARNING: may extremely differ per Att vs per node
				for(int high=0; high<highs; high++) highWeightAveByatt[high] /= highAttsum[high];
				lowWeightDevByatt = new float[lows];
				highWeightDevByatt = new float[highs];
				for(int low=0; low<lows; low++){
					for(int high=0; high<highs; high++){
						float w = weight[low][high];
						float lowDiffByatt = w-lowWeightAveByatt[low];
						lowWeightDevByatt[low] += lowDiffByatt*lowDiffByatt*highNodeAtt[high]; //FIXME? is this reversed?
						float highDiffByatt = w-highWeightAveByatt[high];
						highWeightDevByatt[high] += highDiffByatt*highDiffByatt*lowNodeAtt[low]; //FIXME? is this reversed?
					}
				}
				for(int low=0; low<lows; low++) {
					lowWeightDevByatt[low] = (float)Math.sqrt(lowWeightDevByatt[low]/lowAttsum[low]); //FIXME? is this reversed?
				}
				for(int high=0; high<highs; high++) {
					highWeightDevByatt[high] = (float)Math.sqrt(highWeightDevByatt[high]/highAttsum[high]); //FIXME? is this reversed?
				}
			}
			//medians
			if(computefromlowWeightsMedNoatt || computefromlowWeightsMedByatt || computefromlowWeightsMin || computefromlowWeightsMax){
				if(computefromlowWeightsMedNoatt) fromlowWeightsMedNoatt = new float[lows];
				if(computefromlowWeightsMedByatt) fromlowWeightsMedByatt = new float[lows];
				if(computefromlowWeightsMin) fromlowWeightsMin = new float[lows];
				if(computefromlowWeightsMax) fromlowWeightsMax = new float[lows];
				float[] sort = new float[highs];
				if(computefromlowWeightsMedByatt) {
					throw new Error("TODO count Atts up to attsum/2");
				}
				for(int low=0; low<lows; low++){
					for(int high=0; high<highs; high++){
						//TODO 2 of the 4 sorts can use System.arraycopy, and the other 2 are wrong order of dims for that optimization.
						sort[high] = weight[low][high];
					}
					//TODO optimize maybe this should be done in gpu cuz is log*linear^2 which could bottleneck gpu's linear^3 for small sizes.
					//TODO optimize if this is only for min or max (not median or other percentiles) then do in linear loop instead of loglinear sort
					Arrays.sort(sort);
					if(computefromlowWeightsMedNoatt){
						fromlowWeightsMedNoatt[low] = ((highs&1)==1) ? sort[highs/2] : (sort[highs/2-1]+sort[highs/2])/2;
					}
					if(computefromlowWeightsMin){
						fromlowWeightsMin[low] = sort[0];
					}
					if(computefromlowWeightsMin){
						fromlowWeightsMax[low] = sort[sort.length-1];
					}
					if(computefromlowWeightsMedByatt){
						throw new Error("TODO use what was computed outside this loop, the count Atts up to attsum/2");
					}
				}
			}
			if(computefromhighWeightsMedNoatt || computefromhighWeightsMedByatt || computefromhighWeightsMin || computefromhighWeightsMax){
				if(computefromhighWeightsMedNoatt) fromhighWeightsMedNoatt = new float[highs];
				if(computefromhighWeightsMedByatt) fromhighWeightsMedByatt = new float[highs];
				if(computefromhighWeightsMin) fromhighWeightsMin = new float[highs];
				if(computefromhighWeightsMax) fromhighWeightsMax = new float[highs];
				float[] sort = new float[lows];
				if(computefromhighWeightsMedByatt) {
					throw new Error("TODO count Atts up to attsum/2");
				}
				for(int high=0; high<highs; high++){
					for(int low=0; low<lows; low++){
						sort[low] = weight[low][high];
					}
					//TODO optimize maybe this should be done in gpu cuz is log*linear^2 which could bottleneck gpu's linear^3 for small sizes.
					//TODO optimize if this is only for min or max (not median or other percentiles) then do in linear loop instead of loglinear sort
					Arrays.sort(sort);
					if(computefromhighWeightsMedNoatt){
						fromhighWeightsMedNoatt[high] = ((lows&1)==1) ? sort[lows/2] : (sort[lows/2-1]+sort[lows/2])/2;
					}
					if(computefromhighWeightsMin){
						fromhighWeightsMin[high] = sort[0];
					}
					if(computefromhighWeightsMin){
						fromhighWeightsMax[high] = sort[sort.length-1];
					}
					if(computefromhighWeightsMedByatt){
						throw new Error("TODO use what was computed outside this loop, the count Atts up to attsum/2");
					}
				}
			}
			//TODO put vars for aveAtt of low and high node in LearnLoopParam, since I'm already computing it here.
			//FIXME what other vars?
			
			
			/*
			//float learn = Math.abs(learnRate)+Math.abs(unlearnRate);
			
			//TODO which one is L2 vs L1? Derivative vs linear? Square the weight, the decay, the learnrate, andOr combo?
			//float decay = totalLearnMagnitude*weightDecay;
			float decay = weightDecay*totalLearnMagnitude*;
			float multOldWeight = 1-decay;
			//float multOldWeight = (1-totalLearnMagnitude*weightDecay);
			//WARNING: I tend to use learnrates around a 1e6 cuz of Att sums to 1 per nolay (instead of nolay size)
			//so weightDecay scaling is extremely affected.
			if(multOldWeight < .9f || 1f < multOldWeight) throw new Error("multOldWeight="+multOldWeight);
			*/
			
			float sumLowNodesToLearn = 0, sumLowNodesToUnlearn = 0, sumHighNodesToLearn = 0, sumHighNodesToUnlearn = 0;
			for(int vec=0; vec<vecs; vec++){
				//Doubleloop here in CPU doesnt bottleneck GPU which does tripleloop.
				//Memory copy is same BigO as this loop so GPU wouldnt help much.
				for(int low=0; low<lows; low++){
					sumLowNodesToLearn += vecsLowNodesToLearn[vec][low];
					sumLowNodesToUnlearn += vecsLowNodesToUnlearn[vec][low];
				}
				for(int high=0; high<highs; high++){
					sumHighNodesToLearn += vecsHighNodesToLearn[vec][high];
					sumHighNodesToUnlearn += vecsHighNodesToUnlearn[vec][high];
				}
			}
			float targetAveLowNode = .25f; //FIXME this is param of RBM though it hasnt been used for a long time so hardcoding it here temporarily
			float targetAveHighNode = .25f; //FIXME
			
			p.batchSize = vecs;
			p.tolowNodes = lows;
			p.tohighNodes = highs;
			p.edlay = edlay;
			p.learnRate = learnRate;
			p.weightDecay = weightDecay;
			p.weightVelocityDecay = weightVelocityDecay;
			p.aveTolowNodeToLearn = sumLowNodesToLearn/lows;
			p.aveTolowNodeToUnlearn = sumLowNodesToUnlearn/lows;
			p.aveTohighNodeToLearn = sumHighNodesToLearn/highs;
			p.aveTohighNodeToUnlearn = sumHighNodesToUnlearn/highs;
			p.targetAveTolowNode = targetAveLowNode;
			p.targetAveTohighNode = targetAveHighNode;
			p.tolowNodesAttRange = 0f/0f; //FIXME, set to NaN so if try to use it before fix this, will notice
			p.tohighNodesAttRange = 0f/0f; //FIXME
			
			float[][] newWeight = new float[lows][highs];
			float[][] newWeightVelocity = new float[lows][highs];
			for(int low=0; low<lows; low++){
				//p.lowNodeIsBias = low==0 ? 1f : 0f;
				
				p.fromhighWeightsAveNoatt = fromhighWeightsAveNoatt[low];
				
				//FIXME var names may have changed since: See RBM.upwardNolayAtt and upwardNodeBias and downwardNolayBias.
				
				for(int high=0; high<highs; high++){
					//Gpu has already done the inner loop, unlike in the !learnUsingGpu code
				
					//p.highNodeIsBias = high==0 ? 1f : 0f;
					p.tolowNodeAtt = lowNodeAtt[low];
					p.tohighNodeAtt = highNodeAtt[high];
					p.weight = weight[low][high];
					p.weightVel = weightVelocity[low][high];
					p.toLearn = learnLowHigh[low][high];
					p.toUnlearn = unlearnLowHigh[low][high];
					p.fromlowWeightsAveNoatt = fromlowWeightsAveNoatt[high];
					
					//RBM.learnFunc is a code string (compiled by Javassist to Consumer<LearnLoopParam>) that sets p.return* vars here.
					learnFuncCompiled.accept(p);
					//experimentalLearnFunc.accept(p); //set p.return* vars
					
					newWeight[low][high] = p.returnWeight;
					newWeightVelocity[low][high] = p.returnWeightVel;
					
					
					//ret[low][high] = experimentalLearnFunc.weightAccel(p);
					
					/*ret[low][high] = experimentalLearnFunc.weightAccel(
						vecs, lowNodes, highNodes, edlay,
						learnRate, lowNodeAtt[low], highNodeAtt[high],
						learnLowHigh[low][high], unlearnLowHigh[low][high], weightDecay, weight[low][high],
						aveLowNode, aveHighNode, targetAveLowNode, targetAveHighNode);
					*/
					
					/*
					float att = lowNodeAtt[low]*highNodeAtt[high];
					float diff = att*(learnLowHigh[low][high]*learnRate + unlearnLowHigh[low][high]*unlearnRate);
					diff /= vecs; //spread learnRates across vecs instead of each.
					//ret[low][high] = weight[low][high] + diff; //This was the code for a long time up to 2018-4-23
					
					float decay = weightDecay*diff*diff;
					
					//FIXME SHOULD IT INSTEAD BE?: ret[low][high] = diff - decay*weight[low][high];
					//DEPENDS IF WANT WEIGHTDECAY TO AFFECT VELOCITY OR JUST POSITION (OF weight[][][]).
					//ret[low][high] = diff;
					//I'LL MAKE WEIGHTDECAY AFFECT VELOCITY SO IT WONT BE OVERPOWERED BY OTHER THINGS THAT AFFECT VELOCITY.
					ret[low][high] = diff - decay*weight[low][high];
					//ret[low][high] = diff;
					
					//FIXME could this be causing aveEdlay1WeightsBecomingEverMoreNegativeMustBeReproducibleIn1EdlayWithSameInputsThatEdlay0DidOutputAndMustBeFixableSameWayItsFixedInEdlay0? 
					*/
					
					
					/*
					//Trying to do L2 norm here. FIXME verify this is doing L2.
					//average square of sum of c coins, counting all possible flips exactly once, equals c.
					//float decay = weightDecay*Math.abs(diff);
					float decay = weightDecay*diff*diff; //works ok
					//float decay = weightDecay*Math.abs(diff); //FIXME experiment
					ret[low][high] = weight[low][high]*(1-decay) + diff;
					
					
					FIXME How can this return derivative[][] instead of weight[][] if weightDecay is param here
					and weightDecay depends on change of that specific weight?
					Maybe must move weightDecay to the velocityContinues(dt) func instead of being a param here,
					and velocityContinues(dt) (which should be generalized to the option of not doing neural momentum (add to weights directly))
					would do the "decay = weightDecay*diff*diff;" and "weight[low][high]*(1-decay) + diff".
					That way, the updates of weight and weightVelocity (including 2 kinds of decay) are all done one place.
					*/
					
					
					//FIXME when using weight decay dont also maxradius norm. maxradius norm may be causing problems
					//cuz some weights rising makes other weights fall too directly instead of just through node value stats.
				}
			}
			return new WeightAndVelOfEdlay(newWeight, newWeightVelocity);
		}else{
			if(1<2) throw new Error("TODO UPDATED DESIGN: Returns derivative instead of directly weight (in case you want to do neural moomentum, or not).");
			if(weightDecay != 0) throw new Error("TODO");
			System.out.println("TODO gpu, but for now testing this cpu code with the other gpu code, and after thats the default, remove this message to quietly allow cpu again");
			throw new Error("design changed, diff return type");
			/*float[][] ret = new float[lowNodes][highNodes];
			for(int low=0; low<lowNodes; low++){
			//Parallel.forkAndWait(lowNodes, (int low)->{
				//synchronized(ret[low]){
					for(int high=0; high<highNodes; high++){
						float att = lowNodeAtt[low]*highNodeAtt[high];
						float learnSum = 0, unlearnSum = 0;
						for(int v=0; v<vecs; v++){ //If learnUsingGpu (which its not), gpu would do this loop
							learnSum += vecsLowNodesToLearn[v][low]*vecsHighNodesToLearn[v][high];
							unlearnSum += vecsLowNodesToUnlearn[v][low]*vecsHighNodesToUnlearn[v][high];
						}
						float diff = att*(learnSum*learnRate + unlearnSum*unlearnRate);
						diff /= vecs; //spread learnRates across vecs instead of each.
						ret[low][high] = weight[low][high] + diff;
					}
				//}
			//});
			}
			return ret;
			*/
		}
	}
	
	/** Part of contrastiveDivergence learningRule. Returns replacement for weight[][].
	learnRate should be positive, unlearnRate negative.
	*/
	public static float[][] learnToEdlay(float learnRate, float unlearnRate, float[][] weight,
			float[] lowNodeAtt, float[] highNodeAtt,
			float[] lowNodesToLearn, float[] highNodesToLearn,
			float[] lowNodesToUnlearn, float[] highNodesToUnlearn){
		float[][] ret = new float[lowNodesToLearn.length][highNodesToLearn.length];
		for(int low=0; low<lowNodesToLearn.length; low++){
			for(int high=0; high<highNodesToLearn.length; high++){
				/*ret[low][high] = weight[low][high] + learnRate/2*lowNodeAtt[low]*highNodeAtt[high]*(
					lowNodesToLearn[low]*highNodesToLearn[high] //learnRate/2
					- lowNodesToUnlearn[low]*highNodesToUnlearn[high] //learnRate/2
				);*/
				float diff = lowNodeAtt[low]*highNodeAtt[high]*(
					lowNodesToLearn[low]*highNodesToLearn[high]*learnRate
					+ lowNodesToUnlearn[low]*highNodesToUnlearn[high]*unlearnRate
				);
				ret[low][high] = weight[low][high] + diff;
				//lg("learnToEdlay diff["+low+"]["+high+"]="+diff);
			}
		}
		return ret;
	}
	
	public static float[][][] learnFromZigzagToWholeRbmByBoltzen(float learnRates[], float[][] Att,
			float[][][] zigzag, float[][][] oldWeight, float targetEnergyAve, float targetEnergyStdDev){
		boolean foundNonzero = false;
		for(float r : learnRates) if(r != 0) foundNonzero = true;
		if(!foundNonzero) return oldWeight; //FIXME the new design is to return deriv[][][] instead of weights directly, so return float[][][] of all 0s.
		return learnFromZigzagToWholeRbmByBoltzen(learnRates, Att,
			zigzag[0], zigzag[zigzag.length-1], oldWeight, targetEnergyAve, targetEnergyStdDev);
	}
	
	/** energyBorder is an experimental boltzen param, that learning decays down to or does nothing if below,
	and unlearning decays up to or does nothing if above.
	targetMinEnergy and targetMaxEnergy are a gradual range defined by 2 sigmoids
	for learnPositive and learnNegative, instead of a hard min and max energy
	which would not work anyways cuz learning one thing unbalances earlier learning on another thing.
	*/
	public static float[][][] learnFromZigzagToWholeRbmByBoltzen(
			float learnRates[], float[][] Att, float[][] nodesToLearn, float[][] nodesToUnlearn, float[][][] oldWeight,
			float targetEnergyAve, float targetEnergyStdDev){
		float[][][] newWeight = new float[oldWeight.length][][];
		for(int edlay=0; edlay<oldWeight.length; edlay++){
			//if(learnRates[edlay] < -1 || 1 < learnRates[edlay]) throw new Error(
			//	"learnRate "+learnRates[edlay]+" must range -1 to 1 cuz boltzen uses it as decay toward an energy");
			
			float lowNodes = nodesToLearn[edlay].length, highNodes = nodesToLearn[edlay+1].length;
			
			float learnLowNodeSum = 0, learnHighNodeSum = 0, unlearnLowNodeSum = 0, unlearnHighNodeSum = 0;
			for(int low=0; low<lowNodes; low++){
				learnLowNodeSum += nodesToLearn[edlay][low]*Att[edlay][low];
				unlearnLowNodeSum += nodesToUnlearn[edlay][low]*Att[edlay][low];
			}
			for(int high=0; high<highNodes; high++){
				learnHighNodeSum += nodesToLearn[edlay+1][high]*Att[edlay+1][high];
				unlearnHighNodeSum += nodesToUnlearn[edlay+1][high]*Att[edlay+1][high];
			}
			float changeOfLearnEnergyPerAveWeight = learnLowNodeSum*learnHighNodeSum;
			float changeOfUnlearnEnergyPerAveWeight = unlearnLowNodeSum*unlearnHighNodeSum;
			
			
			
			newWeight[edlay] = new float[oldWeight[edlay].length][oldWeight[edlay][0].length];
			
			float energyOfLearn = BoltzenUtil.energy(nodesToLearn[edlay], oldWeight[edlay], nodesToLearn[edlay+1]);
			float energyOfUnlearn = BoltzenUtil.energy(nodesToUnlearn[edlay], oldWeight[edlay], nodesToUnlearn[edlay+1]);
			
			//float targetEnergyOfLearn = Math.min(energyBorder, energyOfLearn);
			//float targetEnergyOfUnlearn = Math.max(energyBorder, energyOfUnlearn);
			//float targetEnergyOfUnlearn = Math.max(energyOfLearn, energyOfUnlearn);
			
			
			//I was going to simply try a MIN and MAX energy that everything. Better to smooth it...
			//The 2 curves 1/(1+e^-(x+5)) and 1/(1+e^-(-x+5))
			//are flat to infinity on one end and sigmoid on the other, with a near flat area between.
			//Use these as a smooth MIN and MAX. Going down toward MIN is slower than going up away from it,
			//and slow up toward MAX and faster down away from it. The 2 to learn and unlearn will do this.
			//
			//Or... This shows the sigmoid nearly aligned to one side of bellcurve:
			//graph y=1/(1+e^-(4*x+4)) + i*e^(-x*x), x=-4..4
			
			//FIXME learnWallMult and unlearnWallMult need better translation from targetEnergyStdDev.
			
			/*
			//Fraction to multiply learnPositive by cuz its gradually hitting the low energy wall
			float energyOfLearnAsStdDev = (energyOfLearn-targetEnergyAve)/targetEnergyStdDev;
			float learnWallMult = (float)sigmoid(energyOfLearnAsStdDev*4+6);
			//Fraction to multiply learnNegative by cuz its gradually hitting the high energy wall
			float energyOfUnlearnAsStdDev = (energyOfUnlearn-targetEnergyAve)/targetEnergyStdDev;
			float unlearnWallMult = (float)sigmoid(energyOfUnlearnAsStdDev*-4+6);
			*/
			//Reproduce what normal contrastiveDivergence does before adjusting with these walls...
			//float learnWallMult = 1, unlearnWallMult = 1;
			
			
			//float learnRateAdjusted = 50*targetEnergyStdDev*learnRates[edlay];
			float learnRateAdjusted = 1000f*targetEnergyStdDev*learnRates[edlay];
			
			//float actualAddToEnergyOfLearn = -learnRateAdjusted*learnWallMult;
			//float actualAddToEnergyOfUnlearn = learnRateAdjusted*unlearnWallMult;
			//
			//more like normal contrastiveDivergence but still with fuzzy min and max
			//float actualAddToEnergyOfLearn = -learnRateAdjusted*learnWallMult*changeOfLearnEnergyPerAveWeight;
			//float actualAddToEnergyOfUnlearn = learnRateAdjusted*unlearnWallMult*changeOfLearnEnergyPerAveWeight;
			
			/*float wantToAddToEnergyOfLearn = targetEnergyOfLearn-energyOfLearn;
			float wantToAddToEnergyOfUnlearn = targetEnergyOfUnlearn-energyOfUnlearn;
			
			//as would be measured by Boltzen.energy, except for doing 2 of these at once
			float actualAddToEnergyOfLearn = wantToAddToEnergyOfLearn*learnRates[edlay];
			float actualAddToEnergyOfUnlearn = wantToAddToEnergyOfUnlearn*learnRates[edlay];
			*/
			
			//float learnMult = actualAddToEnergyOfLearn/changeOfLearnEnergyPerAveWeight;
			//float unlearnMult = actualAddToEnergyOfUnlearn/changeOfUnlearnEnergyPerAveWeight;
			//more like normal contrastiveDivergence but still with fuzzy min and max
			float learnMult = learnRateAdjusted;
			float unlearnMult = -learnRateAdjusted; //FIXME do boltzen
			//if(energyOfLearn < -12000) learnMult = 0;
			//if(energyOfUnlearn > -8000) unlearnMult = 0;
			//if(energyOfLearn < -10000) learnMult *= .99f;
			//if(energyOfLearn < -10000) learnMult = 0;
			
			System.err.println("FIXME do boltzen. Its normal contrastiveDivergence as of Y2017M12D14T830a as I commentedout the experimental boltzen code that wasnt learning a simple pattern but plan to come back to it soon");
			
			for(int low=0; low<lowNodes; low++){
				for(int high=0; high<highNodes; high++){
					newWeight[edlay][low][high] = oldWeight[edlay][low][high] + Att[edlay][low]*Att[edlay+1][high]*(
						nodesToLearn[edlay][low]*nodesToLearn[edlay+1][high]*learnMult
						+ nodesToUnlearn[edlay][low]*nodesToUnlearn[edlay+1][high]*unlearnMult
					); 
				}
			}
			
			float energyOfLearnAfter = BoltzenUtil.energy(nodesToLearn[edlay], newWeight[edlay], nodesToLearn[edlay+1]);
			float energyOfUnlearnAfter = BoltzenUtil.energy(nodesToUnlearn[edlay], newWeight[edlay], nodesToUnlearn[edlay+1]);
			System.out.println("energyOfLearn before="+energyOfLearn+" after"+energyOfLearnAfter+" unlearn before="+energyOfUnlearn+" after="+energyOfUnlearnAfter
				+" learnMult="+learnMult+" unlearnMult="+unlearnMult);
			//System.out.println("energyOfLearn before="+energyOfLearn+" after"+energyOfLearnAfter+" unlearn before="+energyOfUnlearn+" after="+energyOfUnlearnAfter
			//	+" (inTheoryShouldHave...)actualAddToEnergyOfLearn="+actualAddToEnergyOfLearn+" actualAddToEnergyOfUnlearn="+actualAddToEnergyOfUnlearn
			//	+" learnWallMult="+learnWallMult+" unlearnWallMult="+unlearnWallMult);
		}
		return newWeight;
	}
	
	/** UPDATED DESIGN: Returns derivative instead of directly weight (in case you want to do neural moomentum, or not).
	vecsZigzag[zigzagIndex][nolay][vecIndex][nodeInNolay].
	A zigzag is updating from lowest or highest layer 2 adjacent nodeLayers at a time to the opposite side,
	using (in some cases weightedCoinFlip of) sigmoid of weightedSum,
	weighted by nodeAtt * nodeValue * neuralWeight.
	It was a 3d array [zigzag][node layer][node] but now I'm upgrading it for GPU
	which totals 5 dimensions of looping but only 4 are stored at once (instead of 4 and 3).
	*/
	public static WeightAndVelOfRbm vecsLearnFromZigzagToWholeRbm(
			String learnFunc,
			float[] learnRatePerEdlay, float[][] attMerged, float[][][][] vecsZigzag, float[][][] oldWeight, float[][][] oldWeightVelocity, boolean contrastiveDivergenceSquared,
			float weightDecay, float weightVelocityDecay){
		int nolays = oldWeight.length+1;
		if(attMerged.length != nolays*2) throw new Error();
		if((vecsZigzag.length&1)!=1) throw new Error("Must be odd number of zigzags but is "+vecsZigzag.length);
		if(contrastiveDivergenceSquared){
			if(vecsZigzag.length < 5) throw new Error("Must be at least 5 zigzags for contrastiveDivergenceSquared but is "+vecsZigzag.length);
			if(1<2) throw new Error("TODO 'UPDATED DESIGN: Returns derivative instead of directly weight (in case you want to do neural moomentum, or not).'. Thats not don in contrastiveDivergenceSquared code yet? Or does it share the funcs which do that?");
			return vecsLearnByContrastiveDivergenceSquaredToWholeRbm(learnFunc, learnRatePerEdlay, attMerged,
				vecsZigzag[0], vecsZigzag[1], vecsZigzag[vecsZigzag.length-2], vecsZigzag[vecsZigzag.length-1], oldWeight, oldWeightVelocity,
				weightDecay, weightVelocityDecay);
		}else{
			return vecsLearnPositiveAndNegativeToWholeRbm(learnFunc, learnRatePerEdlay, attMerged, vecsZigzag[0], vecsZigzag[vecsZigzag.length-1], oldWeight, oldWeightVelocity,
				weightDecay, weightVelocityDecay);
		}
	}
	
	/** learnRates is per edlay. They normally differ if Att is not evenly spread
	which causes high edlays to change much faster.
	*/
	public static float[][][] learnFromZigzagToWholeRbm(float learnRates[], float[][] Att,
			float[][][] zigzag, float[][][] oldWeight){
		boolean foundNonzero = false;
		for(float r : learnRates) if(r != 0) foundNonzero = true;
		if(!foundNonzero) return newEmptyArraySameSizesAs(oldWeight); //updated design: return deriv[][][] instead of new values for weight[][][].
		return learnPositiveAndNegativeToWholeRbm(learnRates, Att, zigzag[0], zigzag[zigzag.length-1], oldWeight);
	}
	
	/** nodesTo(Learn|Unlearn)[nolay][vecIndex][nodeInNolay].
	Normal contrastiveDivergence updates weights based on a pair of nodes in adjacent layers in 2 times,
	one early in inference and one after convergence. contrastiveDivergenceSquared uses 4 times,
	2 early and 2 after convergence, including both up and down directions of inference.
	Its very experimental, but its a possible solution to the problem of the same node being
	very different average values in inference up vs inference down.
	contrastiveDivergenceSquared multiplies 4 nodes states together instead of 2, for each of learn and unlearn,
	though I'm also considering just using 3 times since what I really want is for the up and down
	to have equal node states after convergence but it would be ok if they differed before convergence.
	<br><br>
	This will use the same vecsLearnToEdlay func as normal contrastiveDivergence, which is opencl optimized,
	by preprocessing (at a lower bigO) pairs of float[][] into a single float[][]. matrixMultiply is the bottleneck.
	*/
	public static WeightAndVelOfRbm vecsLearnByContrastiveDivergenceSquaredToWholeRbm(
			String learnFunc,
			float learnRates[], float[][] Att,
			float[][][] nodesToLearnUp, float[][][] nodesToLearnDown,
			float[][][] nodesToUnlearnDown, float[][][] nodesToUnlearnUp, float[][][] oldWeight, float[][][] oldWeightVelocity, float weightDecay, float weightVelocityDecay){
		//float[][][] newWeight = new float[oldWeight.length][][];
		WeightAndVelOfEdlay[] wv = new WeightAndVelOfEdlay[oldWeight.length];
		for(int edlay=0; edlay<oldWeight.length; edlay++){
			//TODO optimize this calls multiplyScalars nearly twice as many times as would have to if would instead do this in caller
			//and reuse those arrays in the loop.
			float[][] lowNodesToLearn = MathUtil.multiplyScalars(nodesToLearnUp[edlay],nodesToLearnDown[edlay]);
			float[][] lowNodesToUnlearn = MathUtil.multiplyScalars(nodesToUnlearnUp[edlay],nodesToUnlearnDown[edlay]);
			float[][] highNodesToLearn = MathUtil.multiplyScalars(nodesToLearnUp[edlay+1],nodesToLearnDown[edlay+1]);
			float[][] highNodesToUnlearn = MathUtil.multiplyScalars(nodesToUnlearnUp[edlay+1],nodesToUnlearnDown[edlay+1]);
			wv[edlay] = vecsLearnToEdlay(
				learnFunc, edlay,
				learnRates[edlay]/2, -learnRates[edlay]/2, oldWeight[edlay], oldWeightVelocity[edlay],
				Att[edlay], Att[edlay+1],
				lowNodesToLearn, highNodesToLearn,
				lowNodesToUnlearn, highNodesToUnlearn, weightDecay, weightVelocityDecay
			);
		}
		return new WeightAndVelOfRbm(wv);
	}
	
	/** nodesTo(Learn|Unlearn)[nolay][vecIndex][nodeInNolay].
	UPDATED DESIGN: Returns derivative instead of directly weight (in case you want to do neural moomentum, or not).
	*/
	public static WeightAndVelOfRbm vecsLearnPositiveAndNegativeToWholeRbm(
			String learnFunc,
			float learnRates[], float[][] attMerged, float[][][] nodesToLearn, float[][][] nodesToUnlearn, float[][][] oldWeight, float[][][] oldWeightVelocity,
			float weightDecay, float weightVelocityDecay){
		int nolays = oldWeight.length+1;
		if(attMerged.length != nolays*2) throw new Error("wrong size att");
			
		//float[][][] changeWeight = new float[oldWeight.length][][];
		WeightAndVelOfEdlay[] wv = new WeightAndVelOfEdlay[oldWeight.length];
		for(int edlay=0; edlay<oldWeight.length; edlay++){
		//Parallel.forkAndWait(oldWeight.length, (int edlay)->{
			int lowNolay = edlay, highNolay = lowNolay+1;
			wv[edlay] = vecsLearnToEdlay(
				learnFunc, edlay,
				learnRates[edlay]/2, -learnRates[edlay]/2, oldWeight[edlay], oldWeightVelocity[edlay],
				attMerged[lowNolay*2+1], attMerged[highNolay*2],
				nodesToLearn[edlay], nodesToLearn[edlay+1],
				nodesToUnlearn[edlay], nodesToUnlearn[edlay+1],
				weightDecay, weightVelocityDecay
			);
			//synchronized(newWeight){
				//changeWeight[edlay] = w;
			//}
		//});
		}
		return new WeightAndVelOfRbm(wv);
	}
	
	public static float[][][] learnPositiveAndNegativeToWholeRbm(
			float learnRates[], float[][] Att, float[][] nodesToLearn, float[][] nodesToUnlearn, float[][][] oldWeight){
		float[][][] newWeight = new float[oldWeight.length][][];
		for(int edlay=0; edlay<oldWeight.length; edlay++){
			newWeight[edlay] = learnToEdlay(
				learnRates[edlay]/2, -learnRates[edlay]/2, oldWeight[edlay],
				Att[edlay], Att[edlay+1],
				nodesToLearn[edlay], nodesToLearn[edlay+1],
				nodesToUnlearn[edlay], nodesToUnlearn[edlay+1]
			);
		}
		return newWeight;
	}
	
	/** FIXME havent tuned this */
	public static float[] learnRates(float edlay0LearnRate, float[][] Att){
		float[] r = new float[Att.length-1];
		r[0] = edlay0LearnRate;
		for(int edlay=1; edlay<r.length; edlay++){
			float aveLow = ave(Att[edlay]);
			float aveHigh = ave(Att[edlay]);
			float devLow = devGivenAve(aveLow, Att[edlay]);
			float devHigh = devGivenAve(aveHigh, Att[edlay+1]);
			float relDevLow = devLow/aveLow;
			float relDevHigh = devHigh/aveHigh;
			float relDev = (relDevLow+relDevHigh)/2; //FIXME should this be multiply? Or what?
			r[edlay] = (float)(r[edlay-1]*Math.pow(1+relDev, -4));
		}
		return r;
	}
	
	/** OLD: AttLev1RelRange ranges 0 (all Atts are 1/layerSize) to 1 (random between 0 and approx 2/size).
	Returned array sums to 1 and is all nonnegative.
	*/
	public static float[] randomAttsLev1(int size, float approxMinAtt, float approxMaxAtt, Random rand){
		//if(AttLev1RelRange < 0 || 1 < AttLev1RelRange) throw new Error("AttLev1RelRange="+AttLev1RelRange);
		float[] r = new float[size];
		if(approxMinAtt == approxMaxAtt){
			Arrays.fill(r, approxMinAtt); //dont waste Random
		}else{
			float sum;
			float range = approxMaxAtt-approxMinAtt;
			do{
				sum = 0;
				for(int i=0; i<size; i++) sum += r[i] = approxMinAtt+range*rand.nextFloat();
			}while(sum == 0); //could happen rarely (no rare bugs allowed) if layer is very small or Random is low quality
			float observedAve = sum/size;
			float targetAve = (approxMaxAtt-approxMinAtt)/2;
			float mult = targetAve/observedAve;
			for(int j=0; j<size; j++) r[j] *= mult;
		}
		return r;
	}
	
	/** /** [nolaySide][node], where nolaySide ranges 0 to nolays*2-1.
	<br><br>
	UPDATE: Since attention is now per nodeside instead of per node, just copying the rand attention 2 per node.
	<br><br>
	Updated this code to range 1-AttLev1RelRange/2 to 1+AttLev1RelRange/2 (optionally dividing by nolaySize)
	so it has the same averge Att while varying AttLev1RelRange. Neural momentum (RBM.weightVelocity) needs that.
	<br><br>
	FIXME divideAttByNolaySize param appears to have no effect cuz the randomAtts func
	always norms float[] Atts to sum to 1 per nolay, so if you want Atts to have a constant average per node
	instead of per nolay (which I tested and found does not work as well when layer sizes are very different) FIXME.
	<br><br>
	OLD:
	AttLev1RelRange ranges 0 (all Atts are 1/layerSize) to 1 (random between 0 and approx 2/layerSize).
	Higher AttLev1RelRange reduces overfitting. Its similar to neuralDropout except is gradual instead of binary
	and (in other functions) can (TODO) be chosen randomly or with purpose of which things Att should be on.
	*/
	public static float[][] randomAttsLev1InAllHiddenLayers(float[][][] weight, float attLev1RelRange, Random rand, boolean alsoInVisibleNodes){
		int nolays = weight.length+1;
		float[][] attLev1 = new float[nolays*2][];
		for(int nolay=0; nolay<nolays; nolay++){
			float AttLev1RelRangeForLayer = (nolay==0 && !alsoInVisibleNodes) ? 0 : attLev1RelRange;
			int nolaySize = nolaySize(weight,nolay);
			float min = 1-AttLev1RelRangeForLayer/2;
			float max = 1+AttLev1RelRangeForLayer/2;
			//FIXME divideAttByNolaySize's effect is normed away by randomAtts
			boolean divideAttByNolaySize = false; //cuz if do that, its done in attLev2
			float approxMinAtt = divideAttByNolaySize ? min/nolaySize : min;
			float approxMaxAtt = divideAttByNolaySize ? max/nolaySize : max;
			attLev1[nolay*2] = attLev1[nolay*2+1] = randomAttsLev1(nolaySize, approxMinAtt, approxMaxAtt, rand);
		}
		return attLev1;
	}
	
	/** [nolaySide][node], where nolaySide ranges 0 to nolays*2-1 */
	public static float[][] evenlySpreadAttsLev1(float[][][] weight){
		return randomAttsLev1InAllHiddenLayers(weight, 0f, null, true);
	}
	
	/** [edlay][lowNode][highNode] */
	public static float[][][] newRandomEdges(Random rand, float ave, float stdDev, int... nodeLayerSizes){
		float[][][] edges = new float[nodeLayerSizes.length-1][][];
		for(int edgeLayer=0; edgeLayer<edges.length; edgeLayer++){
			edges[edgeLayer] = new float[nodeLayerSizes[edgeLayer]][nodeLayerSizes[edgeLayer+1]];
			for(int lowNode=0; lowNode<nodeLayerSizes[edgeLayer]; lowNode++){
				for(int highNode=0; highNode<nodeLayerSizes[edgeLayer+1]; highNode++){
					edges[edgeLayer][lowNode][highNode] = ave+stdDev*(float)rand.nextGaussian();
				}
			}
		}
		return edges;
	}
	
	/** [edlay][lowNode][highNode] */
	public static float[][][] newEmptyEdges(int... nodeLayerSizes){
		float[][][] edges = new float[nodeLayerSizes.length-1][][];
		for(int edgeLayer=0; edgeLayer<edges.length; edgeLayer++){
			edges[edgeLayer] = new float[nodeLayerSizes[edgeLayer]][nodeLayerSizes[edgeLayer+1]];
		}
		return edges;
	}
	
	public static float aveDiff(float[] x, float[] y){
		if(x.length != y.length) throw new Error("Diff sizes");
		float sum = 0;
		for(int i=0; i<x.length; i++){
			float diff = Math.abs(x[i]-y[i]);
			sum += diff;
		}
		return sum/x.length;
	}
	
	/** compares rbm output to correct output. This does not always apply to deep invarep,
	but is useful for simple tests. Randomness averages .5. All numbers range 0 to 1.
	*/
	public static float stdDevOfDiff(float[] x, float[] y){
		if(x.length != y.length) throw new Error("Diff sizes");
		float sumOfSquares = 0;
		for(int i=0; i<x.length; i++){
			//sum += (x[i]*y[i] + (1-x[i])*(1-y[i]))*2;
			//if((x[i] != 0 && x[i] != 1) || (y[i] != 0 && y[i] != 1)){
			//	throw new Error("TODO between 0 and 1");
			//}
			//if(x[i] == y[i]) sum++;
			float diff = x[i]-y[i];
			sumOfSquares += diff*diff;
		}
		float aveSquare = sumOfSquares/x.length;
		return (float)Math.sqrt(aveSquare);
	}
	
	public static float[][][] edlayNormByAllowedIndividualWeightRange(float[][][] oldWeight, float minWeight, float maxWeight){
		float[][][] newWeight = new float[oldWeight.length][][];
		for(int i=0; i<newWeight.length; i++){
			newWeight[i] = edlayNormByAllowedIndividualWeightRange(oldWeight[i], minWeight, maxWeight);
		}
		return newWeight;
	}
	
	public static float[][] edlayNormByAllowedIndividualWeightRange(float[][] oldWeight, float minWeight, float maxWeight){
		int highNodes = oldWeight[0].length;
		float[][] newWeight = new float[oldWeight.length][highNodes];
		for(int low=0; low<newWeight.length; low++){
			for(int high=0; high<highNodes; high++){
				newWeight[low][high] = holdInRange(minWeight, oldWeight[low][high], maxWeight);
			}
		}
		return newWeight;
	}
	
	public static float[][][] edlayNormByMaxRadius(final float[][][] oldWeight, float maxAllowedRadius){
		//lgErr("TODO gpu edlayNormByMaxRadius, then remove this message and just default to gpu.");
		float[][][] newWeight = new float[oldWeight.length][][];
		for(int edlay=0; edlay<newWeight.length; edlay++){
		//Parallel.forkAndWait(newWeight.length, (int edlay)->{
			newWeight[edlay] = edlayNormByMaxRadius(oldWeight[edlay], maxAllowedRadius);
		//});
		}
		return newWeight;
	}
	
	/** Remember Att per layer sums to 1 (or at least it did Y2017M7D21) so weights should be unusually large,
	but dimensions are counted as 1/nolaySize each so its actually sqrt of the average squared weight.
	<br><br>
	GPU DONT OPTIMIZE, cuz IO is proportional to compute cycles.
	*/
	public static float[][] edlayNormByMaxRadius(float[][] oldWeight, float maxAllowedRadius){
		int lowNodes = oldWeight.length, highNodes = oldWeight[0].length;
		float[][] newWeight = new float[lowNodes][highNodes];
		float[] lowSumOfSquares = new float[lowNodes], highSumOfSquares = new float[highNodes];
		for(int low=0; low<lowNodes; low++){
			for(int high=0; high<highNodes; high++){
				float w = oldWeight[low][high];
				float w2 = w*w;
				lowSumOfSquares[low] += w2;
				highSumOfSquares[high] += w2;
			}
		}
		float[] lowRadius = new float[lowNodes], highRadius = new float[highNodes];
		for(int low=0; low<lowNodes; low++){
			lowRadius[low] = (float)Math.sqrt(lowSumOfSquares[low]/highNodes);
		}
		for(int high=0; high<highNodes; high++){
			highRadius[high] = (float)Math.sqrt(highSumOfSquares[high]/lowNodes);
		}
		for(int low=0; low<lowNodes; low++){
			for(int high=0; high<highNodes; high++){
				float maxObservedRadius = Math.max(lowRadius[low], highRadius[high]);
				newWeight[low][high] = maxObservedRadius<=maxAllowedRadius
					? oldWeight[low][high]
					: oldWeight[low][high]*maxAllowedRadius/maxObservedRadius;
			}
		}
		return newWeight;
	}
	
	public static float[][][] edlayNormByApproxTruncateWeightAveIntoRange(
			float[][][] oldWeight, float minAllowedAve, float maxAllowedAve){
		float[][][] newWeight = new float[oldWeight.length][][];
		for(int i=0; i<newWeight.length; i++){
			newWeight[i] = edlayNormByApproxTruncateWeightAveIntoRange(oldWeight[i], minAllowedAve, maxAllowedAve);
		}
		return newWeight;
	}
	
	public static float[][] edlayNormByApproxTruncateWeightAveIntoRange(
			float[][] oldWeight, float minAllowedAve, float maxAllowedAve){
		if(minAllowedAve > maxAllowedAve) throw new Error("min > max");
		int lowNodes = oldWeight.length, highNodes = oldWeight[0].length;
		float[][] newWeight = new float[lowNodes][highNodes];
		float[] lowAve = new float[lowNodes], highAve = new float[highNodes];
		for(int low=0; low<lowNodes; low++){
			for(int high=0; high<highNodes; high++){
				lowAve[low] += oldWeight[low][high];
				highAve[high] += oldWeight[low][high];
			}
		}
		for(int low=0; low<lowNodes; low++) lowAve[low] /= lowNodes;
		for(int high=0; high<highNodes; high++) highAve[high] /= highNodes;
		//Theres no perfect solution when number of nodes differ. Avoid changing it past min or max.
		int maxNodes = Math.max(lowNodes, highNodes);
		for(int low=0; low<lowNodes; low++){
			for(int high=0; high<highNodes; high++){
				float ave = (lowAve[low]+highAve[high])/2;
				float newAve = holdInRange(minAllowedAve, ave, maxAllowedAve);
				float addToAve = newAve-ave;
				newWeight[low][high] = oldWeight[low][high]+addToAve/maxNodes;
			}
		}
		return newWeight;
	}
	
	public static float[][][] newEmptyArraySameSizesAs(float[][][] x){
		float c[][][] = new float[x.length][][];
		for(int i=0; i<x.length; i++){
			c[i] = newEmptyArraySameSizesAs(x[i]);
		}
		return c;
	}
	
	public static float[][] newEmptyArraySameSizesAs(float[][] x){
		float c[][] = new float[x.length][];
		for(int i=0; i<x.length; i++){
			c[i] = new float[x[i].length];
		}
		return c;
	}
	
	//BELOW, MOVED FROM MathUtil (same rules as moved from AI)
	
	/** derivative(sigmoid(x)) at param inverseSigmoid(x). One use of this is param in Edlay funcs,
	which adjusts estimated average value per neuralNode when weights change (and decays toward observation).
	<br><br>
	derivative(1/(1+e^-x)) = e^x/(1+e^x)^2,
	but we would need the param to be output of sigmoid (which is fraction range),
	so its e^inverseSigmoid(x)/(1+e^inverseSigmoid(x))^2
	<br><br>
	= e^(-log(1/fraction-1))/(1+e^(-log(1/fraction-1)))^2 //e^(-log(x)) = 1/x
	<br><br>
	= (1/(1/fraction-1))/(1+(1/(1/fraction-1)))^2
	<br><br>
	= derivOfNeuralFuncAtItsOutput(fraction)
	<br><br>
	newFraction = derivOfNeuralFuncAtItsOutput(fraction)*derivative(weightedSum)
	<br><br>
	Equal equation: sigmoidOut - sigmoidOut^2
	*/
	public static final DoubleUnaryOperator sigmoid_derivOfNeuralFuncAtItsOutput = (sigmoidOut)->{
		/*if(sigmoidOut <= 0 || 1 <= sigmoidOut) return 0;
		double d = 1/(1/sigmoidOut-1);
		double dPlusOne = d+1;
		return d/(dPlusOne*dPlusOne);
		*/
		return sigmoidOut - sigmoidOut*sigmoidOut; //equal equation, faster
		
	};
	
	static void testDerivOfSigmoidAtItsOutput(){
		for(int i=0; i<1000; i++){
			double in = Rand.strongRand.nextGaussian()*3;
			double out = sigmoid(in);
			double d = sigmoid_derivOfNeuralFuncAtItsOutput.applyAsDouble(out);
			double changeOfIn = Rand.strongRand.nextGaussian()*.03;
			double approxCorrectOut2 = out+d*changeOfIn;
			double observedOut2 = sigmoid(in+changeOfIn);
			double err = Math.abs(observedOut2-approxCorrectOut2);
			System.out.println("testDerivOfSigmoidAtItsOutput in="+in+" changeOfIn="+changeOfIn+" out="+out+" approxCorrectOut2="+approxCorrectOut2+" observedOut2="+observedOut2+"\r\nerr="+err);
			if(.001 < err) throw new Error("err="+err); //TODO how small should it be?
		}
	}
	
	public static double sigmoid(double x){
		return 1/(1+Math.exp(-x));
		//return Math.sqrt(1/(1+Math.exp(-x)));
	}
	
	//public static final double veryPositive = Double.MAX_VALUE/0x1000000;
	
	//public static final double veryNegative = -veryPositive;
	
	/** If fraction is 0, returns -Infinity. If 1, returns Infinity.
	Derivation:
	s = 1/(1+e^-x).
	s*(1+e^-x) = 1.
	1+e^-x = 1/s.
	e^-x = 1/s - 1.
	-x = logBaseE(1/s - 1).
	x = -logBaseE(1/s - 1).
	*/
	public static double inverseSigmoid(double fraction){
		//x = -logBaseE(1/s - 1)
		//if(s == 0) return .5; //TODO verify this is on the curve
		//if(fraction == 0) return veryNegative; //TODO verify this is on the curve
		return -Math.log(1/fraction - 1);
	}
	
	public static double derivativeOfSigmoid(double x){
		return sigmoid(x)*(1-sigmoid(x)); //TODO optimize
	}
	
	public static void main(String args[]){
		testDerivOfSigmoidAtItsOutput();
		//testSigmoidAndItsInverse();
		//testWeightedCoinFlip();
	}
	
	public static void testWeightedCoinFlip(){
		System.out.print("Testing weightRandomBit...");
		for(double targetChance=0; targetChance<1; targetChance+=.03){
			int countZeros = 0, countOnes = 0;
			for(int i=0; i<100000; i++){
				if(weightedCoinFlip(targetChance,Rand.strongRand)) countOnes++;
				else countZeros++;
			}
			double observedChance = (double)countOnes/(countZeros+countOnes);
			System.out.println("targetChance="+targetChance+" observedChance="+observedChance);
			if(Math.abs(targetChance-observedChance) > .01) throw new RuntimeException("targetChance too far from observedChance");
		}
	}
		
	public static void testSigmoidAndItsInverse(){
		System.out.println("Testing sigmoid and inverseSigmoid");
		double epsilon = 1e-12;
		for(double s=0; s<=1; s+=1./64){
			double x = inverseSigmoid(s);
			double ss = sigmoid(x);
			System.out.println("s="+s+" x="+x+" ss="+ss);
			if(Math.abs(s-ss) > epsilon) throw new RuntimeException("s != ss and is not close");
		}	
	}
	
	/** Uses SecureRandom and only an average of 2 random bits from it */
	public static boolean weightedCoinFlip(double chance){
		return weightedCoinFlip(chance, Rand.strongRand);
	}
	
	/** Consumes an average of 2 random bits (so its practical to use SecureRandom which is slow)
	by consuming random bits until get the first 1 then going directly to that digit
	in the chance as a binary fraction and returning it as the weighted random bit observe.
	TODO I wrote that code somewhere, copy it here so its more practical more often to use SecureRandom.
	*/
	public static boolean weightedCoinFlip(double chance, Random rand){
		if(chance < 0 || 1 < chance) throw new ArithmeticException("chance="+chance);
		while(rand.nextBoolean()){
			if(.5 <= chance) chance -= .5;
			chance *= 2;
		}
		return .5 <= chance;
	}
	
	/** same as in OpenclProgs, deterministic except roundoff. I got suspicious of the difference between
	that and RBM.think func may be causing ave weight to be too negative? 
	so am removing the Random object (except as I use it as a boolean is it null or not to mean other things) from RBM.
	*/
	public static boolean weightedCoinFlipPseudorandom(float chance){
		double weightedSum = inverseSigmoid(chance);
		double pseudorandFraction = (Math.abs(weightedSum)*49999)%1;
		return pseudorandFraction <= chance;
	}
	
	public static boolean isPowerOf2(long i){
		return i>0 && (i&(i-1)) == 0;
	}
	
	public static void normBySortedPointers(double min, double max, double[] d){
		normBySortedPointers(min, max, d, sortedPointersInto(d));
	}
	
	/** If you already have sortedPointers(d) */
	public static void normBySortedPointers(double min, double max, double[] d, int[] pointers){
		int siz = d.length;
		double range = max-min;
		for(int i=0; i<siz; i++){
			double fraction = (double)i/(siz-1);
			d[pointers[i]] = min+fraction*range;
		}
	}
	
	public static int[] randomPermutation(int size, Random rand){
		int[] a = new int[size];
		for(int i=0; i<size; i++) a[i] = i;
		for(int i=0; i<size-1; i++){
			int j = i+rand.nextInt(size-i);
			int temp = a[i];
			a[i] = a[j];
			a[j] = temp;
		}
		return a;
	}
	
	/** Example: normBySortedPointers data in arbitrary range, run neuralnet on it,
	then normBySortedPointersInverse to put neuralnet's output back into that same spread of data
	but a different permutation of it.
	*/
	public static void normBySortedPointersInverse(double[] d, int[] pointers){
		double[] inverse = new double[d.length];
		for(int i=0; i<d.length; i++){
			inverse[i] = d[pointers[i]];
		}
		System.arraycopy(inverse, 0, d, 0, d.length);
	}
	
	/** curve receives a fraction and returns the new double */
	public static void normBySortedPointers(DoubleUnaryOperator curve, double d[]){
		int siz = d.length;
		int pointers[] = sortedPointersInto(d);
		for(int i=0; i<siz; i++){
			double fraction = (double)i/(siz-1);
			d[pointers[i]] = curve.applyAsDouble(fraction);
		}
	}
	
	public static int[] sortedPointersInto(double d[]){
		return sortedPointersInto_tryingToImproveSpeed(d);
	}
	
	public static strictfp int[] sortedPointersInto(final long d[]){
		Integer Ints[] = new Integer[d.length];
		for(int i=0; i<d.length; i++) Ints[i] = i;
		Comparator<Integer> compare = new Comparator<Integer>(){
			public int compare(Integer x, Integer y){
				long xd = d[x], yd = d[y];
				if(xd < yd) return -1;
				if(xd > yd) return 1;
				return 0;
			}
		};
		Arrays.sort(Ints, compare);
		int ints[] = new int[d.length];
		for(int i=0; i<d.length; i++) ints[i] = Ints[i];
		return ints;
	}
	
	public static int[] sortedPointersInto_tryingToImproveSpeed(final double d[]){
		/*int pointers[] = new int[d.length];
		for(int i=0; i<d.length; i++) pointers[i] = i;
		//TODO? Arrays.parallelSort(arg0);
		*/
		
		/*for(int i=0; i<d.length; i++){
			double x = d[i];
			if(x != x){ //NaN, because it may be causing sorting inconsistency
				d[i] = Double.MAX_VALUE;
			}
		}*/
		
		Integer Ints[] = new Integer[d.length];
		for(int i=0; i<d.length; i++) Ints[i] = d.length-1-i;
		Comparator<Integer> compare = new Comparator<Integer>(){
			public int compare(Integer x, Integer y){
				double xd = d[x], yd = d[y];
				if(xd < yd) return -1;
				if(xd > yd) return 1;
				return 0;
			}
		};
		/*while(true){
			try{
				Arrays.sort(Ints, compare);
				break;
			}catch(Exception e){
				System.out.println("This is probably 'Comparison method violates its general contract' which strictfp avoids always singlethreaded but it appears some thread is using it, but which one could it be since its a local var? For now, since it happens only 1 20000 times its faster to just catch this and do it again those times. TODO find that thread and synchronize here and there! "+e.getMessage());
				e.printStackTrace(System.out);
			}
		}*/
		Arrays.sort(Ints, compare);
		int ints[] = new int[d.length];
		for(int i=0; i<d.length; i++) ints[i] = Ints[i];
		return ints;
	}
	
	/** Fast because it leaves it the complexity of NaN and positive/negative zero.
	TODO consider using java.lang.Math funcs instead of this in case its native optimized internal to JVM?
	*/
	public static double max(double x, double y){
		return x>y ? x : y;
	}
	
	/** TODO consider using java.lang.Math funcs instead of this in case its native optimized internal to JVM? */
	public static float max(float x, float y){
		return x>y ? x : y;
	}
	
	/** Fast because it leaves it the complexity of NaN and positive/negative zero.
	TODO consider using java.lang.Math funcs instead of this in case its native optimized internal to JVM?
	*/
	public static double min(double x, double y){
		return x<y ? x : y;
	}
	
	/** TODO consider using java.lang.Math funcs instead of this in case its native optimized internal to JVM? */
	public static float min(float x, float y){
		return x<y ? x : y;
	}
	
	/** Same as max(minValue, min(value, maxValue))
	TODO consider using java.lang.Math funcs instead of this in case its native optimized internal to JVM?
	*/
	public static double holdInRange(double min, double value, double max){
		if(value < min) return min;
		if(value > max) return max;
		return value;
	}
	
	public static void holdInRange(double min, double[] values, double max){
		for(int i=0; i<values.length; i++){
			values[i] = holdInRange(min, values[i], max);
		}
	}
	
	/** TODO consider using java.lang.Math funcs instead of this in case its native optimized internal to JVM? */
	public static float holdInRange(float min, float value, float max){
		if(value < min) return min;
		if(value > max) return max;
		return value;
	}
	
	/** TODO consider using java.lang.Math funcs instead of this in case its native optimized internal to JVM? */
	public static int holdInRange(int min, int value, int max){
		if(value < min) return min;
		if(value > max) return max;
		return value;
	}

	public static int firstPowerOf2AtLeast(int i){
		//TODO this can be done in log steps. YES, wheres that code? Somewhere in the xorlisp experimental code.
		//Also related is Long.highestOneBit and Long.lowestOneBit.
		//This can be done with log number of if statements and bit shifts by half as many bits each time.
		int j = 1;
		int powerOf2 = 0;
		while(j < i){
			powerOf2++;
			j <<= 1;
		}
		return powerOf2;
	}

	public static int lastPowerOf2NotExceeding(int i){
		//TODO this can be done in log steps. YES, wheres that code? Somewhere in the xorlisp experimental code.
		//Also related is Long.highestOneBit and Long.lowestOneBit.
		//This can be done with log number of if statements and bit shifts by half as many bits each time.
		int j = 1;
		int powerOf2 = 0;
		while(j <= i){
			powerOf2++;
			j <<= 1;
		}
		return powerOf2-1;
	}
	
	public static double vectorLengthDyDx(double dy, double dx){
		return Math.sqrt(dy*dy + dx*dx);
	}
	
	public static double dotProd(double x[], double y[]){
		if(x.length != y.length) throw new RuntimeException("Arrays must be same size");
		double sum = 0;
		for(int i=0; i<x.length; i++) sum += x[i]*y[i];
		return sum;
	}
	
	public static float dotProd(float x[], float y[]){
		if(x.length != y.length) throw new RuntimeException("Arrays must be same size");
		float sum = 0;
		for(int i=0; i<x.length; i++) sum += x[i]*y[i];
		return sum;
	}
	
	/** useful in contrastiveDivergence and backprop */
	public static void addMultOfSameSize1dArraysIntoSquareArray(double x[], double y[], double square[][]){
		final int siz = x.length;
		if(siz!=y.length || siz!=square.length || siz!=square[0].length)
			throw new RuntimeException("Arrays must be same size");
		for(int i=0; i<siz; i++){
			final double mult = x[i];
			for(int j=0; j<siz; j++){
				square[i][j] += mult*y[j];
			}
		}
	}
	
	public static double len(double vec[]){
		double sumOfSquares = 0;
		for(double d : vec) sumOfSquares += d*d;
		return Math.sqrt(sumOfSquares);
	}
	
	/** sum of absvals */
	public static double lenManhattan(double vec[]){
		double sum = 0;
		for(double d : vec) sum += Math.abs(d);
		return sum;
	}
	
	/*public static double[] eachFourHexDigitsToScalar(String hex){
		double d[] = new double[hex.length()/4];
		for(int i=0; i<d.length; i++){
			int uint16 = Integer.parseInt(hex.substring(i*4,(i+1)*4), 16);
			d[i] = ((double)uint16-(1<<15))/(1<<15);
		}
		return d;
	}*/
	
	public static boolean readBit(byte b[], int index){
		return (b[index>>3] & (128>>(index&7))) != 0;
	}
	
	public static void writeBit(byte b[], int index, boolean value){
		if(value) b[index>>3] |= (128>>(index&7));
		else b[index>>3] &= ~(128>>(index&7));
	}
	
	public static double ave(double d[]){
		return sum(d)/d.length;
	}
	
	public static float ave(float f[]){
		return sum(f)/f.length;
	}
	
	public static float sum(float a[]){
		float sum = 0;
		for(float f : a) sum += f;
		return sum;
	}
	
	public static double sum(double d[]){
		double sum = 0;
		for(double dd : d) sum += dd;
		return sum;
	}
	
	public static void addToEach(float[] a, float add){
		for(int i=0; i<a.length; i++) a[i] += add;
	}
	
	public static void addToEach(float[][] aa, float add){
		for(float[] a : aa) addToEach(a, add);
	}
	
	/** standard deviation */
	public static double dev(double d[]){
		return devGivenAve(ave(d), d);
	}
	
	/** standard deviation */
	public static float dev(float d[]){
		return devGivenAve(ave(d), d);
	}
	
	/** standard deviation, after you've already computed average */
	public static double devGivenAve(double ave, double d[]){
		double sumOfSquares = 0;
		for(double dd : d){
			double diff = dd-ave;
			sumOfSquares += diff*diff;
		}
		double aveSquare = sumOfSquares/d.length;
		return Math.sqrt(aveSquare);
	}
	
	/** standard deviation, after you've already computed average */
	public static float devGivenAve(float ave, float d[]){
		float sumOfSquares = 0;
		for(float dd : d){
			float diff = dd-ave;
			sumOfSquares += diff*diff;
		}
		float aveSquare = sumOfSquares/d.length;
		return (float)Math.sqrt(aveSquare);
	}
	
	public static float[] toFloats(double[] d){
		float[] f = new float[d.length];
		for(int i=0; i<f.length; i++) f[i] = (float)d[i];
		return f;
	}
	
	public static double[] toDoubles(float[] f){
		double[] d = new double[f.length];
		for(int i=0; i<d.length; i++) d[i] = f[i];
		return d;
	}
	
	/** only the values at odd indexs */
	public static double[] odds(double[] d){
		double[] odds = new double[d.length/2];
		for(int i=0; i<odds.length; i++){
			odds[i] = d[i+i+1];
		}
		return odds;
	}
	
	/** only the values at even indexs */
	public static double[] evens(double[] d){
		double[] evens = new double[(d.length+1)/2];
		for(int i=0; i<evens.length; i++){
			evens[i] = d[i+i];
		}
		return evens;
	}
	
	/** returns random float[] with choose 1s and size-choose 0s.
	FIXME: If choose is near size, gets squared slow.
	*/
	public static float[] sizeChooseY(int size, int choose){
		float[] f = new float[size];
		int ones = 0;
		while(ones < choose){
			int i = Rand.strongRand.nextInt(size);
			if(f[i] == 0){
				f[i] = 1;
				ones++;
			}
		}
		return f;
	}
	
	public static double[] joinEvensOdds(double[] evens, double[] odds){
		if(evens.length!=odds.length && evens.length!=odds.length+1) throw new Error(
			"There can be same number of odds and evens or 1 more even");
		double[] d = new double[evens.length+odds.length];
		for(int i=0; i<odds.length; i++){
			d[i+i+1] = odds[i];
		}
		for(int i=0; i<evens.length; i++){
			d[i+i] = evens[i];
		}
		return d;
	}
	
	/** decays write toward read. decay ranges 0 to 1 and is normally very near 0. Arrays must be same size. */
	public static void decay(float[] write, float[] read, float decay){
		if(write.length != read.length) throw new Error("Diff sizes");
		for(int i=0; i<write.length; i++){
			//float a = 1-decay, b = decay, c = write[i], d = read[i]; //FIXME remove this
			//if(c != c){ //FIXME remove this
			//	throw new Err("nan");
			//}
			write[i] = write[i]*(1-decay) + decay*read[i];
			//if(write[i] != write[i]){ //FIXME remove this
			//	throw new Err("nan");
			//}
		}
	}
	
	/*public static float[] randFloatsInRange(int size, float min, float max, Random rand){
		float[] r = new float[size];
		for(int i=0; i<size; i++) r[i] = min+(max-min)*rand.nextFloat();
		return r;
	}*/
	
	/** compares sum of target node averages to sum of observed nodes.
	Multiply instantBidirectionalBackpropNorm by some function of this fraction, such as linear or maybe squared.
	You probably want to average this for the 2 nolays around the edlay being normed.
	*/
	private static float needNormHowMuchFraction(float[] targetNolayAve, float[] nolayObserve, float[] nolayAtt){
		float target = dotProd(targetNolayAve,nolayAtt);
		float observe = dotProd(nolayObserve,nolayAtt);
		return 1-Math.min(target,observe)/Math.max(target,observe);
	}
	
	public byte[] tempToBytesTodoUseTobitsInterfaceNotJavaSerializable(){
		try{
			ByteArrayOutputStream o = new ByteArrayOutputStream();
			new ObjectOutputStream(o).writeObject(this);
			return o.toByteArray();
		}catch(Exception e){ throw new Error(e); }
	}
	
	/** Gets a single vector's zigzag data (like for RbmPanel, TODO modifying that to take this datastruct)
	from multiple vec zigzag data like used in GPU.
	Param and return are [zigzag][nolay][whichVec][node]. Param can have multiple whichVec size. Return has 1 whichVec size.
	Since arrays are immutable in this software, reuses the innermost arrays.
	*/
	public static float[][][][] getOneVecFromZigzag(float[][][][] zigzag, int whichVec){
		int zigzags = zigzag.length;
		int nolays = zigzag[0].length;
		float[][][][] ret = new float[zigzags][nolays][1][];
		for(int z=0; z<zigzags; z++){
			for(int n=0; n<nolays; n++){
				ret[z][n][0] = zigzag[z][n][whichVec];
			}
		}
		return ret;
	}
	
	/** sqrt of ave squared diff of each pair of floats (not comparing floats in the same array to eachother).
	Example: between 2 [zigzag][nolay][vec][node]. I'm writing this code cuz the LearningVec*.learn and .predict
	look too similar, but I expected them to differ even in 1 edlay cuz the AttLev1RelRange differs.
	They must be same size recursively.
	*/
	public static float stdDev(float[][][][] b, float[][][][] c) {
		float sumOfSquares = 0;
		if(b.length!=c.length || b[0][0].length!=c[0][0].length || b[0][0][0].length!=c[0][0][0].length)
			throw new Error("diff sizes");
		int count = 0;
		for(int z=0; z<b.length; z++){
			for(int nolay=0; nolay<b[z].length; nolay++){
				for(int v=0; v<b[z][nolay].length; v++){
					int nodes = b[z][nolay][v].length; 
					for(int node=0; node<nodes; node++){
						float diff = b[z][nolay][v][node]-c[z][nolay][v][node];
						sumOfSquares += diff*diff;
						count++;
					}
				}
			}
		}
		return (float)Math.sqrt(sumOfSquares/count);
	}
	
	public static final boolean enableNanTests = false;
	static{lgErr("enableNanTests set to false but some code still appears to test for throwIfHasAnyNans which then doesnt test");}

	/** A slow test */
	public static void throwIfHasAnyNans(float[][][][] a){
		if(!enableNanTests) return;
		for(int i=0; i<a.length; i++){
			throwIfHasAnyNans(a[i], i);
		}
	}
	
	/** A slow test */
	public static void throwIfHasAnyNans(float[][][] a, int x){
		if(!enableNanTests) return;
		for(int i=0; i<a.length; i++){
			throwIfHasAnyNans(a[i], x, i);
		}
	}
	
	/** A slow test */
	public static void throwIfHasAnyNans(float[][] a, int x, int y){
		if(!enableNanTests) return;
		for(int i=0; i<a.length; i++){
			throwIfHasAnyNans(a[i], x, y, i);
		}
	}
	
	/** A slow test */
	public static void throwIfHasAnyNans(float[] a, int x, int y, int z){
		if(!enableNanTests) return;
		for(int i=0; i<a.length; i++){
			float f = a[i];
			if(f != f){
				throw new Error("NaN at "+x+" "+y+" "+z+" "+i);
			}
		}
	}

}
