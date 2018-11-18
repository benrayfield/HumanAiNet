/** Ben F Rayfield offers this software opensource MIT license */
package immutable.learnloop;

/** immutable */
public final class WeightAndVelOfRbm{
	
	public final float[][][] weight;
	
	public final float[][][] weightVelocity;
	
	public WeightAndVelOfRbm(WeightAndVelOfEdlay... a){
		weight = new float[a.length][][];
		weightVelocity = new float[a.length][][];
		for(int i=0; i<a.length; i++){
			weight[i] = a[i].weight;
			weightVelocity[i] = a[i].weightVelocity;
		}
	}
	
	public WeightAndVelOfRbm(float[][][] weight, float[][][] weightVelocity){
		this.weight = weight;
		this.weightVelocity = weightVelocity;
	}

}
