package immutable.generated;
public class LearnLoop_dec4045eb7e9cbfe66c89c78cfbc6dcd77b2a27c37b7b744446e54cc573629f7 extends immutable.learnloop.LearnLoop{
public void run(){
//FIXME fill vecsLowNodesToUnlearn etc
float[][] toLearn = immutable.learnloop.OpenclProgs.matmul(immutable.learnloop.RBM.swapDims(vecsLowNodesToLearn), vecsHighNodesToLearn);
float[][] toUnlearn = immutable.learnloop.OpenclProgs.matmul(immutable.learnloop.RBM.swapDims(vecsLowNodesToUnlearn), vecsHighNodesToUnlearn);
	returnWeight = new float[lows][highs];
	returnWevel = new float[lows][highs];
	for(int low=0; low<lows; low++){
		for(int high=0; high<highs; high++){ //TODO if this is a bottleneck then do parts in lwjgl opencl but IO and compute is same bigO (unlike tripleloop compute for doubleloop IO in matmul)

			float att = tolowNodeAtt[low]*tohighNodeAtt[high];
			float diff = learnRate*att*(toLearn[low][high]-toUnlearn[low][high]);
			float diffScaled = diff/batchSize;
			float decay = weightDecay*diffScaled*diffScaled;
			float deriv = diffScaled - decay*theWeight[low][high];
			float dt = 1;
			returnWevel[low][high] = theWevel[low][high]*(1-dt*wevelDecay) + dt*deriv;
			returnWeight[low][high] = theWeight[low][high] + dt*theWevel[low][high];
		}
	}

}
}