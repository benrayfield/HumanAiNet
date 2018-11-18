/** Ben F Rayfield offers this software opensource MIT license */
package immutable.util;
import immutable.learnloop.RBM;

/** boltzmann neuralnet energy equation andOr variations of it */
public class BoltzenUtil{
	
	/** (TODO verify) Approx same as sum of energyOneNode* funcs for all those nodes,
	except for roundoff and weightedCoinFlips of sigmoid chance instead of sigmoid directly.
	*/
	public static float energy(float[] lowNodes, float[][] weights, float[] highNodes){
		float energy = 0;
		for(int lowNode=0; lowNode<lowNodes.length; lowNode++){
			for(int highNode=0; highNode<highNodes.length; highNode++){
				energy -= lowNodes[lowNode]*highNodes[highNode]*weights[lowNode][highNode];
			}
		}
		return energy;
	}

	/** arrays of RBM, but it would be ambiguous to put this func in RBM
	since it has multiple zigzags of observing nodes change.
	*/
	public static float energy(float[][][] weights, float[][] nodes){
		float sum = 0;
		for(int i=0; i<weights.length; i++){
			sum += energy(nodes[i], weights[i], nodes[i+1]);
		}
		return sum;
	}
	
	/** Part of the boltzmann energy contributed by highNode */
	public static float energyOneNodeUpIfOn(float[] lowNodes, float[][] weights, int highNode){
		float weightedSum = weightedSumUp(lowNodes,weights,highNode);
		return -(float)(RBM.sigmoid(weightedSum)*weightedSum)/2;
	}
	
	/** Part of the boltzmann energy contributed by lowNode */
	public static float energyOneNodeDownIfOn(int lowNode, float[][] weights, float[] highNodes){
		float weightedSum = weightedSumDown(lowNode,weights,highNodes);
		return -(float)(RBM.sigmoid(weightedSum)*weightedSum)/2;
	}
	
	public static float weightedSumUp(float[] lowNodes, float[][] weights, int highNode){
		float sum = 0;
		for(int lowNode=0; lowNode<lowNodes.length; lowNode++){
			sum += lowNodes[lowNode]*weights[lowNode][highNode];
		}
		return sum;
	}
	
	public static float weightedSumDown(int lowNode, float[][] weights, float[] highNodes){
		float sum = 0;
		for(int highNode=0; highNode<highNodes.length; highNode++){
			sum += highNodes[lowNode]*weights[lowNode][highNode];
		}
		return sum;
	}

}
