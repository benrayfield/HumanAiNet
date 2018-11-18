/** Ben F Rayfield offers this software opensource MIT license */
package immutable.datasets;
import static mutable.util.Lg.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

import javax.imageio.ImageIO;
import javax.xml.transform.stream.StreamSource;

import immutable.datasets.mnistOcr.MnistLabeledImage;
import immutable.datasets.mnistOcr.MnistOcrDataset;
import immutable.learnloop.RBM;
import immutable.util.MathUtil;
import immutable.util.Text;
import mutable.games.mmgMouseAI.mouseRecorder.CommandlineMouseYRecorder;
import mutable.rbm.ui.LearningVec_OLD;
import mutable.rbm.ui.PaintSlidingVecUi;
import mutable.util.Files;
import mutable.util.Obvars;
import mutable.util.Rand;
import mutable.util.Var;

/** TODO these funcs are called, and fields read and written, by ArmedBearCommonLisp by java reflection,
in LispConsole class on screen or things it creates and stores through the FileStringVars class.
*/
public class ExperimentUtil{
	private ExperimentUtil(){}
	
	/** set by rbm.ui.PaintSlidingVecUi.main
	FIXME make sure only to write to this between when AI is running,
	maybe like PaintSlidingVecUi does "synchronized(learningVecs)"?
	*/
	public static Var<RBM> rbmVar;
	
	/** set by rbm.ui.PaintSlidingVecUi.main. See comment (in this class) of rbmVar. */
	public static List<LearningVec_OLD> learningVecs;
	
	public static double scoreRbmExperimentState(Object rbmExperimentState){
		return .0001; //FIXME score it based on it reaching minimum ok precision, rbmParams, dataset size, etc.
		//I want to stringmind explore between only those that are past some minimum usefulness,
		//where usefulness is a concept I'm trying to define here to guide my research paths automaticly.
	}
	
	public static int visibleNodes(){
		return RBM.nolaySize(rbmVar.get().weight, 0);
	}
	
	public static void pushTrainingVec(float[] vec){
		learningVecs.add(new LearningVec_OLD(vec));
	}
	
	public static void setDataset(float[][] dataset){
		synchronized(learningVecs){
			learningVecs.clear();
			Arrays.asList(dataset).stream().forEach(s->learningVecs.add(new LearningVec_OLD(s)));
		}
	}
	
	static{todo("invarepTestCaseSequenceOfNonoverlappingPairsToSwap");}
	static{todo("turn off auto booting lisp");}
	static{todo("ufnodeCallTakesExtraParamOfNamespaceFunc instead of lisp.");}
	
	/*TODO invarepTestCaseSequenceOfNonoverlappingPairsToSwap. TURN OFF AUTO BOOTING LISP. JUST MAKE THIS WORK
	CUZ ITS AN EXPONENTIAL SIZE VERY INVAREP DATASET WITH LINEAR SIZE COMPLEXITY
	AND VERY WELL ALIGNED TO WHAT RBMS CAN LEARN, AND THIS ALONE IS ALL THATS NEEDED TO LEARN BEFORE UMOUSE,
	SO PROCEED IN JAVA, AND MAKE A UI FOR VISUALIZING THE RANDOM SWAPS AND STACKING OF THEM.
	
	generateInvarepDatasetsInLispAndFindEquationsOfThemAndRbmparamsAndHowMuchItShouldLearnThem QUOTE
	NO, ACTUALLY THE ONLY KIND OF DATASET I WANT RIGHT NOW IS invarepTestCaseSequenceOfNonoverlappingPairsToSwap.
	OLD:
	TODO generate datasets in lisp, in invarep ways, and find equations about rbmParams and learning of them,
	with and without sliding (automatic by average accuracy over all the vecs).
	Create a model of the RBM that knows how well a dataset should be learnable before learning it,
	for certain parameterizations of generating datasets by simple functions, not just for all possible datasets.
	UNQUOTE.
	*/
	
	//TODO I want every func and field in this class to be auto loaded like fj vars in abcl.
	
	static float[][] animalEmojiWallpaper70Dataset23x23 = loadAnimalEmojiWallpaper70Dataset(true);
	
	static float[][] animalEmojiWallpaper70Dataset23x23WithAllBrightnessEquallyOften = normDataset(loadAnimalEmojiWallpaper70Dataset(true),fraction->fraction);
	
	static float[][] animalEmojiWallpaper70Dataset16x16WithAllBrightnessEquallyOften = normDataset(loadAnimalEmojiWallpaper70Dataset(false),fraction->fraction);
	
	static float[][] animalEmojiWallpaper70Dataset23x23WithQuarterOfBitsOn = normDataset(loadAnimalEmojiWallpaper70Dataset(true),fraction->(fraction>=.75)?1:0);
	
	static float[][] animalEmojiWallpaper70Dataset16x16WithQuarterOfBitsOn = normDataset(loadAnimalEmojiWallpaper70Dataset(false),fraction->(fraction>=.75)?1:0);
	
	static float[][] animalEmojiWallpaper70Dataset16x16WithHalfOfBitsOn = normDataset(loadAnimalEmojiWallpaper70Dataset(false),fraction->(fraction>=.5)?1:0);
	
	static MnistLabeledImage[] mnistOcrTestSet = null;
	/** dont modify whats returned */
	static MnistLabeledImage[] loadMnistTestSetIdempotently(){
		if(mnistOcrTestSet == null){
			mnistOcrTestSet = MnistOcrDataset.readTestLabeledImages();
			if(mnistOcrTestSet.length != 10000) throw new Error("Wrong number of images: "+mnistOcrTestSet.length);
		}
		return mnistOcrTestSet;
	}
	
	static String mainMouseYBitsAppendedLongtermAsString = Text.bytesToString(Obvars.fileVarsContent.get(CommandlineMouseYRecorder.fileVar));
	
	public static int trainingVecQuantity(String dataset){
		if(dataset.equals("animalEmojiWallpaper70Dataset23x23")){
			return animalEmojiWallpaper70Dataset23x23.length;
		}
		if(dataset.equals("animalEmojiWallpaper70Dataset23x23WithQuarterOfBitsOn")){
			return animalEmojiWallpaper70Dataset23x23WithQuarterOfBitsOn.length;
		}
		if(dataset.equals("animalEmojiWallpaper70Dataset16x16WithQuarterOfBitsOn")){
			return animalEmojiWallpaper70Dataset16x16WithQuarterOfBitsOn.length;
		}
		if(dataset.equals("animalEmojiWallpaper70Dataset23x23WithAllBrightnessEquallyOften")){
			return animalEmojiWallpaper70Dataset23x23WithAllBrightnessEquallyOften.length;
		}
		if(dataset.equals("animalEmojiWallpaper70Dataset16x16WithAllBrightnessEquallyOften")){
			return animalEmojiWallpaper70Dataset16x16WithAllBrightnessEquallyOften.length;
		}
		if(dataset.equals("animalEmojiWallpaper70Dataset16x16WithHalfOfBitsOn")){
			return animalEmojiWallpaper70Dataset16x16WithHalfOfBitsOn.length;
		}
		if(dataset.equals("mainMouseYBitsAppendedLongtermAs128BitVecs")){
			return mainMouseYBitsAppendedLongtermAsString.length()-128+1;
		}
		if(dataset.equals("mainMouseYBitsAppendedLongtermAs144BitVecs")){
			return mainMouseYBitsAppendedLongtermAsString.length()-144+1;
		}
		if(dataset.equals("mnistOcrTestFile16x16From28x28ShrunkToHalfSizeAndOnehotLabelsAddedAlongASizeExpandingTo16x16")){
			return loadMnistTestSetIdempotently().length;
		}
		if(dataset.equals("mnistOcrTestFile28x28BitofispixelnonzeroAsFloatNoLabel")){
			return loadMnistTestSetIdempotently().length;
		}
		if(dataset.equals("mnistOcrTestFile28x28ScalarsSortedPointersNormedByCubed")){
			return loadMnistTestSetIdempotently().length;
		}
		throw new Error("dataset not found: "+dataset);
	}
	
	public static float[] trainingVec(String dataset, int whichVec){
		if(dataset.equals("animalEmojiWallpaper70Dataset23x23")){
			return animalEmojiWallpaper70Dataset23x23[whichVec];
		}
		if(dataset.equals("animalEmojiWallpaper70Dataset23x23WithQuarterOfBitsOn")){
			return animalEmojiWallpaper70Dataset23x23WithQuarterOfBitsOn[whichVec];
		}
		if(dataset.equals("animalEmojiWallpaper70Dataset16x16WithQuarterOfBitsOn")){
			return animalEmojiWallpaper70Dataset16x16WithQuarterOfBitsOn[whichVec];
		}
		if(dataset.equals("animalEmojiWallpaper70Dataset23x23WithAllBrightnessEquallyOften")){
			return animalEmojiWallpaper70Dataset23x23WithAllBrightnessEquallyOften[whichVec];
		}
		if(dataset.equals("animalEmojiWallpaper70Dataset16x16WithAllBrightnessEquallyOften")){
			return animalEmojiWallpaper70Dataset16x16WithAllBrightnessEquallyOften[whichVec];
		}
		if(dataset.equals("animalEmojiWallpaper70Dataset16x16WithHalfOfBitsOn")){
			return animalEmojiWallpaper70Dataset16x16WithHalfOfBitsOn[whichVec];
		}
		if(dataset.equals("mainMouseYBitsAppendedLongtermAs128BitVecs") || dataset.equals("mainMouseYBitsAppendedLongtermAs144BitVecs")){
			int siz = 0;
			if(dataset.equals("mainMouseYBitsAppendedLongtermAs128BitVecs")) siz = 128;
			if(dataset.equals("mainMouseYBitsAppendedLongtermAs144BitVecs")) siz = 144;
			String vecAsString = mainMouseYBitsAppendedLongtermAsString.substring(whichVec, whichVec+siz); //chars '1' and '0'
			float[] vec = new float[vecAsString.length()];
			for(int i=0; i<vec.length; i++){
				vec[i] = (vecAsString.charAt(i)=='1')?1:0;
			}
			return vec;
		}
		if(dataset.equals("mnistOcrTestFile16x16From28x28ShrunkToHalfSizeAndOnehotLabelsAddedAlongASizeExpandingTo16x16")){
			MnistLabeledImage[] images = loadMnistTestSetIdempotently();
			return MathUtil.toFloats(MnistOcrDataset.to16x16(images[whichVec]));
		}
		if(dataset.equals("mnistOcrTestFile28x28BitofispixelnonzeroAsFloatNoLabel")){
			return MnistOcrDataset.to28x28BitofispixelnonzeroAsFloatNoLabel(loadMnistTestSetIdempotently()[whichVec]);
		}
		if(dataset.equals("mnistOcrTestFile28x28ScalarsSortedPointersNormedByCubed")){
			return MnistOcrDataset.to28x28ScalarsSortedPointersNormedByCubed(loadMnistTestSetIdempotently()[whichVec]);
		}
		throw new Error("dataset not found: "+dataset);
		/*TODO I want datasets to be named by string and int which vec in the dataset, so I can add 1 at a time
		instead of having to empty the LearningVecs and replace some of them with equal vec but loss of cached data.
		Also, I want float[] params to generate dataset, like found in the UnaryOperator<float[]> of earlier RBM experiments,
		but maybe that should be a different func. with (String,float...) params.
		
		public static float[][] loadAnimalEmojiWallpaper70Dataset16x16(){
			return PaintSlidingVecUi.loadAnimalEmojiWallpaper70Dataset(false);
		}
		
		public static float[][] loadAnimalEmojiWallpaper70Dataset23x23(){
			return PaintSlidingVecUi.loadAnimalEmojiWallpaper70Dataset(true);
		}*/
	}
	
	public static float newRbmWeightAve = -1;
	
	public static float newRbmWeightDev = 10;
	
	/** Keeps all other RBM properties but replaces weights using newRbmWeightAve and newRbmWeightDev and a Random */
	public static void randomizeRbmWeights(){
		RBM rbm = rbmVar.get();
		float[][][] newWeights = RBM.newRandomEdges(Rand.strongRand, newRbmWeightAve, newRbmWeightDev, RBM.nolaySizes(rbm.weight));
		rbmVar.set(rbm.setWeight(newWeights));
	}
	
	/** Y2018M4D8+TheTestcaseMustPassBeforeMoveOnToMouseDataForRbm.
	This is a variant of invarepTestCaseSequenceOfNonoverlappingPairsToSwap / swapstackinvarep.
	This is a very specific testcase and way of measuring success. I plan to attempt it directly,
	with no other invarep testcases before it, then to move on to using mouse data such as recorded mozig,
	then interactive umouse then mmgMouseai then ufnodeCallTakesExtraParamOfNamespaceFunc. So here is that testcase...
	<br><br>
	256 visibleNodes. All nodes in all nolays are bit. The dataset is size 4096 vecs. In each vec,
	1/4 of the nodes are on  (every fourth node, before permutations). Each vec is generated as a sequence
	of 3 permutation, each of which are on half the visibleNodes. 1/8 of the nodes are constant value.
	3/8 of the nodes have up to (very near) 16 possible states.
	3/8 of the nodes have up to (very near) 256 possible states.
	1/8 of the nodes have up to (very near) 4096 possible states.
	There are 3 sets of 16 permutations (randomly generated once).
	This is a sequence of 3 things therefore should have at least 3 edlays.
	Use exactly 3 edlays. Randomly choose (once) half of the 4096 vecs
	to train on (maybe in random sets of a few hundred of them each as
	usual in slidingvec, but without the sliding). The other 2048 vecs
	are the test set, never trained on but MUST (to pass this testcase)
	be learned with a minimum accuracy. That minimum accuracy is that when
	you start with any such vec (in the test set), and randomize 30% of it
	(somehow norming the randomness so it still has 1/4 of its nodes on),
	and let it converge (remember to use at least 10 zigzags cuz that was
	needed for the emoji dataset), then from only 70% pattern input it must
	rebuild to have at least 80% accuracy in the hardest 1/8 of the nodes
	(the 1/8 of the nodes that have up to 4096 possible values). That is invarep,
	rebuilding from partial pattern things that were not trained
	on anything very similar by dotProd.
	<br><br>
	After thats working, choose the next testcase for recorded mozig (mouseY where velocity
	is 1 or 0 depending if moving more up or down) data and rebuild some of it from partial
	pattern that was not trained on those parts. After thats working, move on to interactive
	umouse, and so on.
	*/
	static float[][] Y2018M4D8Plus_TheTestcaseMustPassBeforeMoveOnToMouseDataForRbm(Random rand){
		//TODO merge duplicate code, and generalize to any small power of 2 and number of visibleNodes
		float[][] vecs = new float[4096][256];
		int[][][] permutations = new int[3][16][]; //[cycle][whichPermutation][visibleNodeIndex]. 16^3 = 4096.
		/*int[] temp = MathUtil.randPermutation(128,rand);
		Choose
		--hardcode it 3 times here.
		--func to move permutation using boolean array.
		--func to move permutation using another permutation.
		--shift by binary digit index. YES, THIS ONE.
		*/
		for(int c=0; c<permutations.length; c++){
			for(int w=0; w<permutations[0].length; w++){
				//The 7 is log2(256)-1
				permutations[c][w] = MathUtil.randPermutationForHalfOfNodesByBitIndex(7-c, 256, rand);
			}
		}
		for(int vec=0; vec<4096; vec++){
			int p0 = (vec>>8)&15;
			int p1 = (vec>>4)&15;
			int p2 = vec&15;
			for(int n=0; n<256; n++){
				int nPermutated = n;
				nPermutated = permutations[0][p0][nPermutated];
				nPermutated = permutations[1][p1][nPermutated];
				nPermutated = permutations[2][p2][nPermutated];
				vecs[vec][n] = ((nPermutated&3)==0)?1f:0f;
			}
		}
		return vecs;
	}
	
	public static float[][] Y2018M4D8Plus_TheTestcaseMustPassBeforeMoveOnToMouseDataForRbm_exceptSize64(Random rand){
		//TODO merge duplicate code, and generalize to any small power of 2 and number of visibleNodes
		float[][] vecs = new float[64][256];
		int[][][] permutations = new int[3][4][]; //[cycle][whichPermutation][visibleNodeIndex]. 4^3 = 64
		for(int c=0; c<permutations.length; c++){
			for(int w=0; w<permutations[0].length; w++){
				//The 7 is log2(256)-1
				permutations[c][w] = MathUtil.randPermutationForHalfOfNodesByBitIndex(7-c, 256, rand);
			}
		}
		for(int vec=0; vec<64; vec++){
			int p0 = (vec>>4)&3;
			int p1 = (vec>>4)&3;
			int p2 = vec&3;
			for(int n=0; n<256; n++){
				int nPermutated = n;
				nPermutated = permutations[0][p0][nPermutated];
				nPermutated = permutations[1][p1][nPermutated];
				nPermutated = permutations[2][p2][nPermutated];
				vecs[vec][n] = ((nPermutated&3)==0)?1f:0f;
			}
		}
		return vecs;
	}
	
	/** a float[] of how to score it, weighting toward the last 1/8 */
	public static float[] Y2018M4D8Plus_TheTestcaseMustPassBeforeMoveOnToMouseDataForRbm_scoreWeight(){
		float[] f = new float[256];
		Arrays.fill(f, 0, f.length*7/8, 0);
		Arrays.fill(f, f.length*7/8, f.length, 8f/f.length);
		return f;
	}
	
	/** A set of rows and columns are on. Where they meet, those nodes are 1. All others are 0.
	The number of rows and the number of columns should be constant, but positions vary, per dataset.
	<br><br>
	This is an exponential size dataset, though very predictable since only combos of 4 nodes
	need to be learned together, which is a rectangle corners in any 2 rows and cols that are on,
	for all possible sets of 2 rows and 2 cols. But its not exactly that easy since it needs
	to be learned in combination with all the others on at the same time, as these sets of 4
	nodes on together overlap other such sets of 4.
	<br><br>
	This is a basic kind of testcase for can RBM learn things without being trained on them
	but only trained on things similar. Its basically a web of simple logic gates,
	so it will be trained on all possible states of every logic gate
	but wont be trained on all combos of the gates together,
	yet still should predict that from partial pattern.
	*/
	public static float[] clibinRand(int rows, int cols, int rowsOn, int colsOn, Random rand){
		float[] ret = new float[rows*cols];
		boolean[] whichRowsOn = MathUtil.xChooseYArray(rows, rowsOn, rand);
		boolean[] whichColsOn = MathUtil.xChooseYArray(cols, colsOn, rand);
		for(int i=0; i<ret.length; i++){
			ret[i] = whichRowsOn[i/cols]&whichColsOn[i%cols] ? 1 : 0;
		}
		return ret;
	}
	
	/** size 23x23 (big) or 16x16 (!big) */
	static float[][] loadAnimalEmojiWallpaper70Dataset(boolean big){
		int squareSide = big?23:16;
		byte[] picGrid;
		if(big) picGrid = Files.readFileRel("/data/tempDatasets/animal-emoji-wallpaper/Animal-Emoji-Wallpaper-70-with-Animal-Emoji-Wallpaper.scaledTo23x23.deduplicated.jpg");
		else picGrid = Files.readFileRel("/data/tempDatasets/animal-emoji-wallpaper/Animal-Emoji-Wallpaper-70-with-Animal-Emoji-Wallpaper.16x16Each.grayscale.removedDuplicates.jpg");
		try{
			BufferedImage image = ImageIO.read(new ByteArrayInputStream(picGrid));
			//testImage = image;
			int h = image.getHeight(), w = image.getWidth();
			List<float[]> vecs = new ArrayList();
			for(int gridY=0; gridY<h/squareSide; gridY++){
				for(int gridX=0; gridX<w/squareSide; gridX++){
					float[] vec = new float[squareSide*squareSide];
					for(int picY=0; picY<squareSide; picY++){
						for(int picX=0; picX<squareSide; picX++){
							int rgb = image.getRGB(gridX*squareSide+picX, gridY*squareSide+picY);
							int red = (rgb>>16)&0xff;
							int green = (rgb>>8)&0xff;
							int blue = rgb&0xff;
							vec[picY*squareSide+picX] = 1-(red+green+blue)/(255*3f);
							//vec[picY*16+picX] = picX/16f;
						}
					}
					vecs.add(vec);
				}
			}
			return vecs.toArray(new float[0][]);
		}catch(Exception e){ throw new Error(e); }
	}
	
	static float[][] normDataset(float[][] vecs, DoubleUnaryOperator curver){
		float[][] ret = new float[vecs.length][vecs[0].length];
		for(int i=0; i<vecs.length; i++){
			float[] newVec = vecs[i].clone();
			//DoubleUnaryOperator curver = isScalar
			//	? ((double fraction)->fraction*fraction*fraction) //ave 1/4
				//: ((double fraction)->fraction*3>2?1:0);
				//: ((double fraction)->fraction*4>3?1:0);
			//	: ((double fraction)->fraction>=.75?1:0); //ave 1/4
			MathUtil.normBySortedPointers(curver, newVec);
			ret[i] = newVec;
		}
		return ret;
	}

}
