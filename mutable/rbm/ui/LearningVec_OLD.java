/** Ben F Rayfield offers this software opensource MIT license */
package mutable.rbm.ui;

import immutable.learnloop.RBM;
import immutable.util.MathUtil;
import mutable.util.MutDecayBell;
import mutable.util.Rand;
import mutable.util.Time;

public class LearningVec_OLD{
	
	public MutDecayBell aveDiffOfErrDecayBell = new MutDecayBell();
	
	//public double stdDevOfErr = 1;
	public double aveDiffOfErr = 1;
	
	/** Slidinglearnrandvecui displays in this order */
	public double timeCreated;
	
	/** time predicted by a RBM */
	public double timePredicted;
	
	/** visibleNodes. Use as immutable */
	public final float[] vec;
	
	/** [zigzag][nolay][which vec but in this case is size 1][node]. This could be from cpu or gpu.
	TODO Modify RbmPanel to use this datastruct instead of the earlier design [zigzag][nolay][node].
	*/
	public float[][][][] predict;
	
	/** [zigzag][nolay][which vec but in this case is size 1][node]. This could be from cpu or gpu.
	TODO Modify RbmPanel to use this datastruct instead of the earlier design [zigzag][nolay][node].
	*/
	public float[][][][] learn;
	
	/** randomness or reversing a timewindow etc. Randomness is used to avoid learning identityFunc. TODO high rbm layer should learn to tell difference between a forward and reversed timewindow recording. */
	public boolean isCounterexample = false;
	
	public boolean isCounterexampleRandomInsteadOfPattern = false;
	
	/** How many times was this vec learned (counting by calls of update).
	Example: nolay0 may have a constant set of training vecs,
	but nolay1 has exponentially more cuz of weightedCoinFlips of what nolay0 and edlay0 generate.
	That may cause problems learning higher layers, depending how much invarep (invariant representation) those patterns are.
	*/ 
	public int cycles;
	
	/** false if is part of the test dataset, which is predicted to know accuracy on things that should
	be indirectly learned by inference from combos of things directly learned (invarep).
	FIXME this var is ignored as of 2018-4-12-1p except for display of it.
	*/
	public boolean enableLearningIfExample = true;
	//public boolean enableLearning = MathUtil.weightedCoinFlip(.7);
	
	public boolean shouldLearn(){
		return enableLearningIfExample && !isCounterexample;
	}
	
	public LearningVec_OLD(float[] vec){
		this(Time.time(), vec);
	}
	
	public LearningVec_OLD(double timeCreated, float[] vec){
		this.vec = vec;
	}
	
	/** does not update vec since thats constant */
	public void update(double timePredicted, float[] rbmOut){
		this.timePredicted = timePredicted;
		//stdDevOfErr = RBM.stdDevOfDiff(vec, rbmOut);
		aveDiffOfErr = RBM.aveDiff(vec, rbmOut);
		aveDiffOfErrDecayBell.add(aveDiffOfErr, .2); //TODO make decay a param
		cycles++;
	}
	
	/** does not update vec since thats constant */
	public void update(float[] rbmOut){
		update(Time.time(), rbmOut);
	}
	
	public String toString(){
		return super.toString()+" aveDiffOfErr="+aveDiffOfErr+" predict="+predict+" learn="+learn;
	}

}
