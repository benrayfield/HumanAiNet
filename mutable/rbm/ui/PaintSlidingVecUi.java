/** Ben F Rayfield offers this software opensource MIT license */
package mutable.rbm.ui;
import static mutable.util.Lg.*;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import immutable.learnloop.RBM;
import mutable.learnloop.RbmPanel;
import net.java.games.input.Version;
import immutable.datasets.ExperimentUtil;
import mutable.rbm.RbmDisplay;
import mutable.rbm.func.LearnLoopParam_OLD;
import mutable.util.Files;
import immutable.util.MathUtil;
import mutable.util.Rand;
import immutable.util.Text;
import mutable.util.Time;
import mutable.util.Var;
//import util.ui.LispPanel;
import mutable.util.ui.ScreenUtil;
import mutable.util.ui.StretchVideo;
//import whatFuncsCanRbmLearn.DatasetRotateRandomBitstring1d;

public class PaintSlidingVecUi extends Slidinglearnrandvecui{
	
	/*TODO relicense as opensource MIT? Its less motivating for anyone to lie about where they got it since it doesnt force copyleft.
	Or which allpermissive license?
	I might want AGPL for binufnode, but this is plainjava (with native opencl)
	and can be ported to binufnode later.
	*/
	
	
	
	
	/*TODO use HypercubeAftransVisualizer.java as dataset cuz its extreme invarep
	and extremely simple (just a hypercube where one side is negative and other is positive so xor, except on first n dims which pascalstri blur).
	Also, I want to create a game based on that since its very interesting to Humans (at least to me)
	and since its npcomplete as proven by subsetsum and you can see that
	in how it can make curves that do not look like a regular grid.
	I want RBM to learn this and play the game by painting this and it recognizing this,
	or if RBM cant learn this to enough qubits (log2 of that many image offsets and weightedaves)
	then maybe a new kind of AI is needed that merges RBM with this kind of aftrans/subsetsum/wave.
	*/
	
	public final float[][] paintGrid;
	
	protected final StretchVideo paintGameUi;
	
	//public final static int squareSide = 8;
	//public final static int squareSide = 12;
	public final static int squareSide = 16;
	//public final static int squareSide = 28;
	//public final static int squareSide = 23; //Animal-Emoji-Wallpaper-70-with-Animal-Emoji-Wallpaper.scaledTo23x23.deduplicated.jpg
	
	/** set of buttons held down, including MouseEvent.BUTTON1+buttonIndex and KeyEvent.VK_A */
	public final Set<Integer> buttons = new HashSet();
	
	public int selectedY, selectedX;
	
	protected double lastTimeGameControllerInput = Time.time();
	
	/** prototype RBM is for RBM.pushLayerFrom(RBM) */
	public PaintSlidingVecUi(Var<RBM> rbmVar, Var<RbmDisplay> varDisplayWhat, Var<RBM> prototype, int vecSize,
			int howManyVecsAtOnce, float maxLearnErrorToRemove){
		super(/*defaultConfuser*/(float[] in)->in,rbmVar, varDisplayWhat, prototype, vecSize, howManyVecsAtOnce, maxLearnErrorToRemove);
		paintGrid = new float[squareSide][squareSide];
		paintGameUi = new StretchVideo(false, paintGrid.length, paintGrid[0].length, (int y, int x)->{
			//return x==y ? 0xa0ffffff : 0;
			float b = paintGrid[y][x];
			//return ScreenUtil.color(.7f, b, b, b);
			return ScreenUtil.color(.1f, b, b, b);
		});
		for(int y=0; y<paintGrid.length; y++){
			for(int x=0; x<paintGrid[0].length; x++){
				paintGrid[y][x] = MathUtil.weightedCoinFlip(.2) ? 1 : 0;
			}
		}
		addKeyListener(new KeyListener(){
			public void keyTyped(KeyEvent e){}
			public void keyPressed(KeyEvent e){
				buttons.add(e.getKeyCode());
				onGameControllerInput();
			}
			public void keyReleased(KeyEvent e) {
				buttons.remove(e.getKeyCode());
				onGameControllerInput();
			}
		});
		addMouseListener(new MouseListener(){
			public void mouseClicked(MouseEvent e){}
			public void mousePressed(MouseEvent e){
				buttons.add(e.getButton());
				onGameControllerInput();
			}
			public void mouseReleased(MouseEvent e){
				buttons.remove(e.getButton());
				onGameControllerInput();
			}
			public void mouseEntered(MouseEvent e){}
			public void mouseExited(MouseEvent e){}
		});
		addMouseMotionListener(new MouseMotionListener(){
			public void mouseMoved(MouseEvent e){
				selectedY = MathUtil.holdInRange(0, e.getY()*squareSide/getHeight(), squareSide-1);
				selectedX = MathUtil.holdInRange(0, e.getX()*squareSide/getWidth(), squareSide-1);
				onGameControllerInput();
			}
			public void mouseDragged(MouseEvent e){ mouseMoved(e); }
		});
		
		addRemoveVecs();
	}
	
	
	int trainingDatasUsed = 0;
	
	/** this replaces addSomeDataset(). Its 1 at a time.
	Normally counterexamples are random except for having the same average node value as trainingvecs.
	*/
	public float[] nextTrainingVecOrCounterexample(boolean isExample, boolean isRandom){
		if(isExample && isRandom) throw new Error("cant be both example and random");
		int visNodes = squareSide*squareSide;
		
		/*
		//TODO make a datasetName for this, and be specific to its size
		//clibin
		return isExample
			? ExperimentUtil.clibinRand(squareSide, squareSide, squareSide/2, squareSide/2, Rand.strongRand)
			: MathUtil.bitsToFloats(MathUtil.xChooseYArray(visNodes, (int)Math.round(visNodes/4f), Rand.strongRand));
		*/
		
		
		/*int ons = (int)Math.round(squareSide/Math.sqrt(2)); //about half of nodes on
		return isExample
			? ExperimentUtil.clibinRand(squareSide, squareSide, ons, ons, Rand.strongRand)
			: MathUtil.bitsToFloats(MathUtil.xChooseYArray(visNodes, (int)Math.round(visNodes/2f), Rand.strongRand));
		
		/*int ons = 8; //about half of nodes on
		return isExample
			? ExperimentUtil.clibinRand(squareSide, squareSide, ons, ons, Rand.strongRand)
			: MathUtil.bitsToFloats(MathUtil.xChooseYArray(visNodes, ons*ons, Rand.strongRand));
		*/
			
		//TODO mainMouseYBitsAppendedLongtermAs128BitVecs
		
		
		String datasetName = "mnistOcrTestFile16x16From28x28ShrunkToHalfSizeAndOnehotLabelsAddedAlongASizeExpandingTo16x16";
		//String datasetName = "mnistOcrTestFile28x28BitofispixelnonzeroAsFloatNoLabel";
		//String datasetName = "mnistOcrTestFile28x28ScalarsSortedPointersNormedByCubed";
		//String datasetName = "mainMouseYBitsAppendedLongtermAs144BitVecs";
		//String datasetName = "animalEmojiWallpaper70Dataset23x23WithQuarterOfBitsOn";
		//String datasetName = "animalEmojiWallpaper70Dataset16x16WithQuarterOfBitsOn";
		//if(!isExample) return MathUtil.bitsToFloats(MathUtil.xChooseYArray(visNodes, (int)Math.round(visNodes/4f), Rand.strongRand));
		
		//String datasetName = "animalEmojiWallpaper70Dataset16x16WithAllBrightnessEquallyOften";
		//String datasetName = "animalEmojiWallpaper70Dataset16x16WithHalfOfBitsOn";
		
		int datasetSize = ExperimentUtil.trainingVecQuantity(datasetName);
		
		int whichVecInThatDataset = Rand.strongRand.nextInt(datasetSize);
		
		//if(!isExample){
		//	if(isRandom){
		//		return MathUtil.bitsToFloats(MathUtil.xChooseYArray(visNodes, (int)Math.round(visNodes/2f), Rand.strongRand));
		//	}else{
		//		return MathUtil.reverse(ExperimentUtil.trainingVec(datasetName, whichVecInThatDataset)); //FIXME can return duplicate vector if dataset is too small
		//	}
		//}
		
		float[] vec = ExperimentUtil.trainingVec(datasetName, whichVecInThatDataset); //FIXME can return duplicate vector if dataset is too small
		int ons = (int)(MathUtil.sum(vec)+.5f);
		if(isRandom) return MathUtil.bitsToFloats(MathUtil.xChooseYArray(visNodes, ons, Rand.strongRand));
		if(!isExample) return MathUtil.reverse(vec);
		return vec;
		
		
		
		/*if(trainingDatasUsed >= datasetSize){
			throw new Error("Dataset not big enough. datasetSize="+datasetSize+" learningVecsSize(including counterexamples)="+learningVecs.size());
		}
		//return ExperimentUtil.trainingVec(datasetName, Rand.strongRand.nextInt(size)); //FIXME can return duplicate vector if dataset is too small
		//return ExperimentUtil.trainingVec(datasetName, trainingDatasUsed++); //FIXME can return duplicate vector if dataset is too small
		*/
	}
	
	//protected void addRemoveVecs(){
		
		/*while(maxNewVecsPerSlideCycle < learningVecs.size()){
			learningVecs.remove(0);
		}
		
		/*
		//while(aveErrNearCenter() <= maxLearnErrorToRemove){
		//int removed = 0;
		while((aveErr() <= maxLearnErrorToRemove || userSaysToAddAnotherVec) && removed < maxNewVecsPerSlideCycle){
		//if(aveErr() <= maxLearnErrorToRemove || userSaysToAddAnotherVec){
			learningVecs.remove(0);
			userSaysToAddAnotherVec = false;
			removed++;
		}
		while(learningVecs.size() < howManyVecsAtOnce){
			learningVecs.add(newLearningVec());
		}
		//TODO remove old LearningVecs that have been learned well enough
		*/
	//}
	
	//TODO? need counterexamples, maybe persistentContrastiveDivergence. Only need this if the red columns get too high.
	
	/*
	protected void addSomeDataset(){
		
		int visNodes = squareSide*squareSide;
		
		//float[][] dataset = normDataset(loadAnimalEmojiWallpaper70Dataset(true),true);
		//float[][] dataset = normDataset(loadAnimalEmojiWallpaper70Dataset(true),false); //false means bits
		
		//It doesnt learn this at all. it goes completely black. Maybe its too jumpy a learning algorithm,
		//cuz I know how to set the weights to do this directly.
		//Or maybe its that I need a certain node that always on.
		//I'll add that. That helped alot but its still a few nodes on at once and only about 1/4 of the time
		//the correct one is on. Also that was with many hiden nodes, just a basic test.
		//I'll move on to more ones, since I know how to set the weights manually to solve this one. 
		//float[][] dataset = DatasetRotateRandomBitstring1d.create(Rand.strongRand, visNodes, 1);
		
		//float[][] dataset = DatasetRotateRandomBitstring1d.create(Rand.strongRand, visNodes, visNodes/4);
		//float[][] dataset = DatasetRotateRandomBitstring1d.create(Rand.strongRand, visNodes, visNodes/2);
		
		//TODO be careful not to learn identityFunc, since even a random RBM associates an input to
		//an output where the dotProduct between them is slightly closer than random.
		//Rebuild from partial pattern to avoid that.
		//Also tune rbmParams such as AttVector.
		
		
		//for(int i=0; i<dataset.length; i++){
		//	dataset[i][0] = 1; //instead of node bias
		//}
		
		//int maxVecs = 200;
		//int maxVecs = 100;
		//int maxVecs = 20;
		//int maxVecs = 4;
		//int maxVecs = 64;
		//int maxVecs = 300;
		//int maxVecs = 100;
		int maxVecs = 1000;
		//int maxVecs = 50;
		//int vecs = 0;
		//for(float[] vec : dataset){
		//	if(vecs < maxVecs){
		//		learningVecs.add(new LearningVec(vec));
		//		vecs++;
		//	}else break;
		//}
		
		//float[][] dataset = ExperimentUtil.Y2018M4D8Plus_TheTestcaseMustPassBeforeMoveOnToMouseDataForRbm(Rand.strongRand);
		//float[][] dataset = ExperimentUtil.Y2018M4D8Plus_TheTestcaseMustPassBeforeMoveOnToMouseDataForRbm_exceptSize64(Rand.strongRand);
		
		float[][] dataset = Stream.generate(()->ExperimentUtil.clibinRand(squareSide, squareSide, squareSide/2, squareSide/2, Rand.strongRand))
			.parallel().limit(maxVecs).toArray(size->new float[size][]);
		
		lgErr("FIXME make sure the red vertical bars stay around half height since they are random, to avoid learning identityFunc which makes it predict worse and makes it appear to learn better than it really does until you try to use it in the real world.");
		//TODO its expected that the RBM will learn mostly identityFunc unless there are counterexamples.
		//I saw this in the new vecs sliding in being already learned around 85%,
		//but I need them to slide in at around 50% until it learns the patterns of the dataset.
		//I need random vecs to continue to be around 50%, random with the same average number of nodes on.
		//Do some kind of normalize using random vecs, maybe related to persistentContrastiveDivergence.
		//I want 3 colors of vertical bars (LearningVecs) displayed:
		//randomOrCounterexampleKeepThisNotLearned(red) learnThis(green) onlyPredictThis(blue).
		
		//boolean[] b = MathUtil.xChooseYArray(dataset.length, maxVecs, Rand.strongRand);
		//for(int i=0; i<dataset.length; i++){
		//	if(b[i]){
		//		LearningVec lv = new LearningVec(dataset[i]);
		//		//lv.enableLearning = TODO;
		//		learningVecs.add(lv);
		//	}
		//}
		
		float sum = 0;
		for(int i=0; i<dataset.length; i++){
			LearningVec lv = new LearningVec(dataset[i]);
			sum += MathUtil.ave(dataset[i]);
			//lv.enableLearning = TODO;
			lv.enableLearningIfNonrandom = MathUtil.weightedCoinFlip(.7);
			learningVecs.add(lv);
		}
		float ave = sum/dataset.length;
		
		for(int i=0; i<dataset.length/4; i++){
			LearningVec lv = new LearningVec(
				MathUtil.bitsToFloats(MathUtil.xChooseYArray(visNodes, (int)Math.round(visNodes*ave), Rand.strongRand)));
			lv.isRandom = true;
			learningVecs.add(lv);
		}
		
		
		lgErr("TODO clibin");
		//float[][] dataset = 
		//		TODO use ExperimentUtil.clibinRand, and I want some of them displayed but not trained on,
		//		so modify LearningVec or which datastruct to say some of the vecs are to be predicted but learnRate is 0 for them.
		//		What about there being an exponential number of such possible vecs? Use sliding for that?
		//		Watch it slide faster and faster, and eventually it already knows them all, intheory.
	}*/
	
	static BufferedImage testImage;
	
	public float avePixelBright(){
		float sum = 0;
		for(int y=0; y<squareSide; y++){
			for(int x=0; x<squareSide; x++){
				sum += paintGrid[y][x];
			}
		}
		return sum/(squareSide*squareSide);
	}
	
	protected void onGameControllerInput(){
		boolean b1 = buttons.contains(MouseEvent.BUTTON1),
			b2 = buttons.contains(MouseEvent.BUTTON1+1), b3 = buttons.contains(MouseEvent.BUTTON1+2);
		boolean addTrainingVec = false; //FIXME this isnt used
		//System.out.println("buttons: "+buttons);
		
		for(int i=0; i<10; i++){
			if(buttons.contains(KeyEvent.VK_0+i)){
				if(buttons.contains(KeyEvent.VK_SHIFT)){
					saveRbm(i);
				}else{
					loadRbm(i);
				}
				break;
			}
		}
		
		if(buttons.contains(KeyEvent.VK_PLUS) || buttons.contains(KeyEvent.VK_SPACE)){
			changeLayerSizeThisMuch = 1;
		}
		if(buttons.contains(KeyEvent.VK_MINUS)){
			changeLayerSizeThisMuch = -1;
		}
		
		if(buttons.contains(KeyEvent.VK_R)){
			writePixelsRand();
		}else if(b1){
			if(paintGrid[selectedY][selectedX] != 1) addTrainingVec = true;
			paintGrid[selectedY][selectedX] = 1;
		}else if(b2 || (b1&b3) || buttons.contains(KeyEvent.VK_F)){
			paintGrid[selectedY][selectedX] = MathUtil.weightedCoinFlip(avePixelBright())?1:0;
		}else if(buttons.contains(MouseEvent.BUTTON1+2)){
			if(paintGrid[selectedY][selectedX] != 0) addTrainingVec = true;
			paintGrid[selectedY][selectedX] = 0; 
		}
		lastTimeGameControllerInput = Time.time();
		
		//writePixelsFromVec(learningVecs.get(Rand.strongRand.nextInt(learningVecs.size())).vec);
		
		/*if(addTrainingVec){
			synchronized(learningVecs){
				learningVecs.add(new LearningVec_todoReplaceWithRbmdimsCols(getPixelsAsNewVec()));
				while(maxVecsInSlidingWindow < learningVecs.size()){
					learningVecs.remove(0);
				}
			}
		}*/
		
		repaint();
	}

	public void paint(Graphics g){
		super.paint(g);
		paintGameUi.paint(g,getHeight(),getWidth());
		g.setColor(Color.blue);
		g.drawLine(0, 0, getWidth(), getHeight());
		if(testImage != null) g.drawImage(testImage, 0, 0, this);
		g.setColor(Color.black);
		g.setFont(new Font(Font.MONOSPACED,Font.BOLD,20));
		g.drawString("learnRatePerNolay="+Text.floatsToString(rbmVar.get().learnRatePerEdlay), 60, getHeight()*2/3);
	}
	
	public float[] getPixelsAsNewVec(){
		float[] ret = new float[squareSide*squareSide];
		for(int y=0; y<paintGrid.length; y++){
			for(int x=0; x<paintGrid[0].length; x++){
				ret[y*squareSide+x] = paintGrid[y][x];
			}
		}
		return ret;
	}
	
	public void writePixelsRand(){
		for(int y=0; y<paintGrid.length; y++){
			for(int x=0; x<paintGrid[0].length; x++){
				paintGrid[y][x] = Rand.strongRand.nextFloat();
			}
		}
	}
	
	public void writePixelsFromVec(float[] vec){
		if(vec.length != squareSide*squareSide) throw new Error("Wrong size "+vec.length+" needed "+squareSide*squareSide);
		for(int y=0; y<paintGrid.length; y++){
			for(int x=0; x<paintGrid[0].length; x++){
				paintGrid[y][x] = vec[y*squareSide+x];
			}
		}
	}
	
	double timeLastState = Time.time();
	
	public boolean onlyLearnInTopEdlay = true;
	
	public void nextState(){
		synchronized(learningVecs){ //TODO optimize by not syncing on this cuz it may slow ui events
			super.nextState();
			double now = Time.time();
			double dt = MathUtil.holdInRange(0, now-timeLastState, .2);
			timeLastState = now;
			double changeLearnRate = 0;
			if(buttons.contains(MouseEvent.BUTTON1)) changeLearnRate++;
			if(buttons.contains(MouseEvent.BUTTON1+2)) changeLearnRate--;
			changeLearnRate *= dt*1.5;
			//lg("buttons: "+buttons+" changeLearnRate="+changeLearnRate+" dt="+dt);
			
			if(onlyLearnInTopEdlay){
				float uiMultLearnRate = (float)Math.exp(changeLearnRate);
				RBM r = rbmVar.get();
				float[] learnRates = new float[r.learnRatePerEdlay.length];
				learnRates[learnRates.length-1] = MathUtil.max(r.learnRatePerEdlay)*uiMultLearnRate;
				rbmVar.set(r.setLearnRatePerEdlay(learnRates));
			}else{
				//FIXME this would erase any difference in learnrate ratios between edlays
				rbmVar.set(rbmVar.get().setLearnRate(MathUtil.ave(rbmVar.get().learnRatePerEdlay)*(float)Math.exp(changeLearnRate)));
				//rbmVar.set(rbmVar.get().setLearnRate(55f));
			}
		}
		//float[] in = getPixelsAsNewVec();
		//float[] out = rbmVar.get().setIn(in).think().prediction();
		//writePixelsFromVec(out);
		
	}
	
	/** Example: shift+number on keyboard saves rbm by Serializable (not for longterm storage) to a file */
	public void saveRbm(int slot){
		Files.saveBySerialize(rbmVar.get(), "rbm"+slot);
		lg("RBM saved to slot "+slot);
	}
	
	/** opposite of saveRbm func */
	public void loadRbm(int slot){
		RBM rbm = (RBM)Files.loadBySerialize("rbm"+slot);
		if(rbm != null){
			rbmVar.set(rbm);
			updateDisplayIfLearningVecChanged();
			lg("RBM loaded from slot slot "+slot);
		}else{
			lg("No RBM loaded from slot "+slot);
		}
	}

	/**
	 * @param args
	 */
	public static void main(String[] args){
		JFrame window = new JFrame("Human AI Net - RBM editor - opensource MIT license");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Random rand = Rand.strongRand;
		
		//float neuralMin = 0, neuralMax = 1;

		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 100,1000);
		int visibleNodes = squareSide*squareSide;
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, visibleNodes, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, visibleNodes, 1500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, visibleNodes, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, visibleNodes, 500, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, visibleNodes, 500, 50);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, visibleNodes, 500, 256);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 100f, neuralMin, neuralMax, visibleNodes, 500, 256);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 100f, neuralMin, neuralMax, visibleNodes, 500, 256, 256);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, neuralMin, neuralMax, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, neuralMin, neuralMax, visibleNodes, 200, 200);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, neuralMin, neuralMax, visibleNodes, 20);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, neuralMin, neuralMax, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, neuralMin, neuralMax, visibleNodes, visibleNodes*4);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 10f, neuralMin, neuralMax, visibleNodes, 12);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 10f, neuralMin, neuralMax, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 10f, neuralMin, neuralMax, visibleNodes, visibleNodes/2);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 10f, neuralMin, neuralMax, visibleNodes, visibleNodes/4, visibleNodes/4, visibleNodes/4, visibleNodes/4);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 10f, neuralMin, neuralMax, visibleNodes, visibleNodes, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, neuralMin, neuralMax, visibleNodes, visibleNodes, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, neuralMin, neuralMax, visibleNodes, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -5, 30f, neuralMin, neuralMax, visibleNodes, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -5, 100f, neuralMin, neuralMax, visibleNodes, visibleNodes*2, visibleNodes*2, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 100f, neuralMin, neuralMax, visibleNodes, 500, 256, 256);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -3, 100f, neuralMin, neuralMax, visibleNodes, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 10f, neuralMin, neuralMax, visibleNodes, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, neuralMin, neuralMax, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 100f, neuralMin, neuralMax, visibleNodes, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, visibleNodes*5);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, visibleNodes*5);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 30f, visibleNodes, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, visibleNodes*5);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, visibleNodes*5, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 128);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 512, 256);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 512,300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 512, 512);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 200);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 200, 200);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 400, 400);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 400);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 400, 400);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 300, 200);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 10f, visibleNodes, 400);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 10f, visibleNodes, 400, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 400, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 500, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 400);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 800);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 256, 256, 256);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 256, 256);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 400,400);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0, 100f, visibleNodes, 500,256,256);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1f, 100f, visibleNodes, 500,256,256);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1f, 100f, visibleNodes, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1f, 100f, visibleNodes, 500, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 20f, visibleNodes, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 500, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 500, 500, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 400, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 400, 100, 100, 100);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 100);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 400, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 400);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 1000);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 100);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, 200);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 100f, visibleNodes, visibleNodes*2, visibleNodes*5);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 20f, visibleNodes, visibleNodes*2, visibleNodes*5);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 20f, visibleNodes, visibleNodes*3);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 20f, visibleNodes, visibleNodes/2);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 20f, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 20f, visibleNodes, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 50f, visibleNodes, visibleNodes*3, visibleNodes*2);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 50f, visibleNodes, visibleNodes*3);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 20f, visibleNodes, 200);
		
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 20f, visibleNodes, 200, 300, 250);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 50f, visibleNodes, 200, 500, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 50f, visibleNodes, visibleNodes, visibleNodes, visibleNodes);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 50f, visibleNodes, visibleNodes*2, visibleNodes*2, visibleNodes*2);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 50f, visibleNodes, 1000, 400, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 50f, visibleNodes, 100, 400, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 50f, visibleNodes, 30, 400, 300);
		final float[][][] rbmEdges = RBM.newRandomEdges(rand, 0f, 50f, visibleNodes, visibleNodes, 400, 300);
		
		//FIXME cant specify zigzagPredict separately from zigzagLearn anymore since RBM.zigzagsLearn is the only place its stored,
		//but the below code is ok in that zigzagsPredict is by default 1 less than zigzagsLearn.
		
		//int zigzagPredict = 10;
		//int zigzagPredict = 2; //FIXME this is far too low
		//int zigzagPredict = 6;
		int zigzagPredict = 10; //TOOD
		//int zigzagPredict = 20;
		int zigzagLearn = zigzagPredict+1;
		int zigzagNorm = zigzagLearn;
		
		int zigzagDisplay = zigzagPredict+1+zigzagLearn+1+zigzagNorm;
		
		int totalNodes = 0;
		int nolays = rbmEdges.length+1;
		for(int nolay=0; nolay<nolays; nolay++) totalNodes += RBM.nolaySize(rbmEdges, nolay);
		
		float[][] targetNodeAve = RBM.newNodes(rbmEdges);
		for(float[] a : targetNodeAve) Arrays.fill(a, .25f);
		//for(float[] a : targetNodeAve) Arrays.fill(a, .8f); //FIXME should be around .25
		float[][][] zigzagPredictArray = new float[zigzagPredict][][];
		for(int z=0; z<zigzagPredictArray.length; z++) zigzagPredictArray[z] = RBM.newNodes(rbmEdges);
		float[][][] zigzagLearnArray = new float[zigzagLearn][][];
		for(int z=0; z<zigzagLearnArray.length; z++) zigzagLearnArray[z] = RBM.newNodes(rbmEdges);
		float[][][] zigzagNormArray = new float[zigzagNorm][][];
		for(int z=0; z<zigzagNormArray.length; z++) zigzagNormArray[z] = RBM.newNodes(rbmEdges);
		
		//common combos are:
		//only first nolay or 2 nolays are scalar (for predicting scalars using bits for featureVectors),
		//OR none are scalar (least overfitting),
		//OR all are scalar (for research on boltzmann energy, or for determinism, but not much practical use).
		//boolean[] isScalar = new boolean[nolays];
		//Arrays.fill(isScalar, true); //FIXME
		//isScalar[0] = true;
		//isScalar[1] = true;
		//isScalar[2] = true;
		
		int lowNNolaysAreScalar = 0;
		//int lowNNolaysAreScalar = nolays; //FIXME leave this as nolays until the NaN bug is fixed (which appears to happen either in opencl or my code wrapping it
		
		//FIXME targetAveNodeValue of nolay0 must be about the same as average node value in training data.
		//For clibin trainingData its always 1/4, so I'm just doing 1/4 everywhere. Might otherwise prefer 1/3?
		//But its important for it to always be significantly less than 1/2 so when few nodes are on
		//consider what that (weightedSum being near 0) does to node values inference is to.
		//This is designed for average weights to be about 0.
		//
		//TODO try biasInBoltzEnergyFuncIsWeightBetweenSelfAndSelfSoTryThatInContrastiveDivergence instead.
		//
		//float targetAveNodeValue = .25f;
		//float biasAll = (float)MathUtil.inverseSigmoid(targetAveNodeValue);
		//float biasAll = -4; //FIXME
		//float biasAll = 0; //FIXME
		//float biasAll = -.5f; //FIXME
		//float[] biasPerNolay = new float[nolays];
		//Arrays.fill(biasPerNolay, biasAll);
		//biasPerNolay[1] = 1.5f;
		//biasPerNolay[0] = 0; //targetAveNodeValue .5 for visibleNodes
		//biasPerNolay[2] = -1.5f;

		/*String learnFunc = "float att = p.tolowNodeAtt*p.tohighNodeAtt;\r\n" + 
			"float diff = p.learnRate*att*(p.toLearn-p.toUnlearn);\r\n" + 
			"float diffScaled = diff/p.batchSize;\r\n"+ //spread learnRates across vecs instead of each.\r\n" + 
			"float decay = p.weightDecay*diffScaled*diffScaled;\r\n" + 
			"float deriv = diffScaled - decay*p.weight;\r\n" + 
			"float dt = 1;\r\n"+ //FIXME this was the old code but its buggy cuz when learnRate changes, weightVelocityDecay does not, but learnRate*weightVelocityDecay would.\r\n"+ 
			"p.returnWeightVelocity = p.weightVelocity*(1-dt*p.weightVelocityDecay) + dt*deriv;\r\n" + 
			"p.returnWeight = p.weight + dt*p.weightVelocity;";//+
				//"System.out.println(\"learnFunc... p=\"+p);";
		*/
		
		/*String learnFunc = "float att = tolowNodeAtt*tohighNodeAtt;\r\n" + 
				"float diff = learnRate*att*(toLearn-toUnlearn);\r\n" + 
				"float diffScaled = diff/batchSize;\r\n"+ //spread learnRates across vecs instead of each.\r\n" + 
				"float decay = weightDecay*diffScaled*diffScaled;\r\n" + 
				"float deriv = diffScaled - decay*weightPos;\r\n" + 
				"float dt = 1;\r\n"+ //FIXME this was the old code but its buggy cuz when learnRate changes, weightVelocityDecay does not, but learnRate*weightVelocityDecay would.\r\n"+ 
				"returnWeightVel = weightVel*(1-dt*weightvelDecay) + dt*deriv;\r\n" + 
				"returnWeightPos = weightPos + dt*weightVel;";
		*/
		
		String learnFunc = "float att = tolowNodeAtt*tohighNodeAtt;\r\n" + 
			"float diff = learnRate*att*(toLearn-toUnlearn);\r\n" + 
			"float diffScaled = diff/batchSize;\r\n"+ //spread learnRates across vecs instead of each.\r\n" + 
			"float decay = weightDecay*diffScaled*diffScaled;\r\n" + 
			"float deriv = diffScaled - decay*theWeight;\r\n" + 
			"float dt = 1;\r\n"+ //FIXME this was the old code but its buggy cuz when learnRate changes, weightVelocityDecay does not, but learnRate*weightVelocityDecay would.\r\n"+ 
			"returnWevel = theWevel*(1-dt*wevelDecay) + dt*deriv;\r\n" + 
			"returnWeight = theWeight + dt*theWevel;";
		
		
		float[][][] nmhp = new float[nolays][][];
		for(int i=0; i<nolays; i++){
			int nolaySize = RBM.nolaySize(rbmEdges, i);
			nmhp[i] = new float[nolaySize][nolaySize];
		}
		float[][] biasPerNodeside = new float[nolays*2][];
		float[][] attLev2PerNodeside = new float[nolays*2][];
		float bias = -.5f;
		for(int i=0; i<nolays*2; i++){
			int nolay = i/2; //down and up directions, or could set those separately
			int nolaySize = RBM.nolaySize(rbmEdges, nolay);
			attLev2PerNodeside[i] = new float[nolaySize];
			Arrays.fill(attLev2PerNodeside[i], 1f/attLev2PerNodeside[i].length); //make attLev2 sum to 1 per nolay. TODO later explore other variations of it
			biasPerNodeside[i] = new float[nolaySize];
			Arrays.fill(biasPerNodeside[i], bias);
		}
		
		RBM rbm = new RBM()
			.setComment("mouseai1dAtTime"+Time.stardateStr())
			.setNmhp(nmhp)
			.setBiasPerNodeside(biasPerNodeside)
			.setAttLev2PerNodeside(attLev2PerNodeside)
			.setLearnFunc(learnFunc)
			.setLowNNolaysAreScalar(lowNNolaysAreScalar)
			.setLearnByIncompleteBoltzenCode(false)
			//.setAttLev1RelRangeForPredict(0f)
			//.setAttLev1RelRangeForLearn(0f) //turn off temporarily cuz second edlay isnt learning well
			.setAttLev1RelRangeForLearn(.5f)
			//.setAttLev1RelRangeForLearn(1f)
			
			.setAttLev1RelRangeForPredict(.5f)
			
			//FIXME setAttRelRangeForLearn appears to have no effect. Test this by setting them to NaN (then I found bug in newAtts func)
			//.setAttRelRangeForLearn(0f/0f)
			//.setAttRelRangeForPredict(0f/0f)
			
			//.setAttRelRangeForPredict(2f)
			//.setAttRelRangeForLearn(.2f)
			//.setIn(new float[visibleNodes])
			.setTargetNodeAve(targetNodeAve) //this code does nothing (hasnt for maybe a year as of Y2018M4)
			.setWeight(rbmEdges)
			.setWeightVelocity(RBM.newEmptyArraySameSizesAs(rbmEdges))
			
			//FIXME weightDecay is hardcoded. Its param of RBM.learn func.
			
			.setWeightDecay(.02f)
			.setWeightVelocityDecay(.2f)
			//.setWeightVelocityDecay(.05f)
			//.setWeightVelocityDecay(.5f)
			//.setWeightVelocityDecay(.01f)
			//.setZigzagPredict(zigzagPredictArray)
			//.setZigzagLearn(null)
			//.setZigzagNorm(null)
			.setZigzagsLearn(zigzagLearn)
			//.setLearnRate(.2f);
			//.setLearnRate(1f);
			//.setLearnRate(.02f);
			//.setLearnRate(2e7f);
			//.setLearnRate(2e8f);
			//.setLearnRate(1.5e6f);
			//.setLearnRate(5.5e6f);
			//.setLearnRate(5e4f);
			//.setLearnRate(5e5f);
			//.setLearnRate(5e6f);
			//.setLearnRate(5e3f);
			//.setLearnRate(5e2f);
			//.setLearnRate(5e4f);
			//.setLearnRate(5e5f);
			//.setLearnRate(5e4f);
			//.setLearnRate(5e4f);
			//.setLearnRate(1.5e6f); //works for the 23x23 emoji dataset with 3 edlays all scalars but is still blurry
			//.setLearnRate(1.5e7f);
			//.setLearnRate(1.5e8f);
			//.setLearnRate(100f);
			//.setLearnRate(.1f);
			//.setLearnRate(1.5e6f);
			//.setLearnRate(1.5e4f);
			//.setLearnRate(1);
			//.setLearnRate(5e6f);
			//.setLearnRate(0);
			//.setLearnRate(5e3f);
			//.setLearnRate(5e4f);
			//.setLearnRate(1.5e6f);
			//.setLearnRate(3e5f);
			//.setLearnRate(1e6f);
			//.setLearnRate(1e5f);
			//.setLearnRate(3e5f);
			//.setLearnRate(3e4f);
			.setLearnRate(1e5f); //works 2018-5-22
			//.setLearnRate(1e2f); //very low, want to snapshot it before it learns.
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		/*Need to display 3 things (in the 2d view and 1d view):
		correct inputandoutput, confuser input, observed output.
		Cuz I cant tell if its outputs are correct and if incorrect is it caused by the confuser.
		Green and blue feel similar. Use them as the correct and the observed.
		Confuser input will be red. Green is correctinandout. Blue observed output.
		If its learning well, the green and blue will mostly match.
		If not, then the red may help understand why.
		Those colors will work for the 2d view of visibleNodes.
		What about the diag with zigzags? How to display it there?
		Output will stay as it is. Input needs to be 2 things,
		but its only 1 pixel tall, so it will have to be side by side,
		but which comes first? Also, do I want the correctinandout on the left and right?
		*/
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		/*TODO I want aftrans hidim dataset generator. Theres a 1d version of this. do it in 2d. aftrans still works in 2d.
		Go find that code. Actually stick with the 1d Version and just ignore how it looks in 2d for now.
		Or maybe rotating a 3d object made of balls would be an interesting testcase in 2d.
		If it cant learn aftrans pointers into pixel index, its not useful for my research purposes,
		but many AIs (at least partially neuralnet based) do learn that. So thats the testcase I'll get working NOW...
		IT WILL STRONGLY DEMONSTRATE INVAREP CUZ THERE WILL BE MANY TIMES MORE VALID VECTORS THAN ITS TRAINED ON
		AND THOSE ITS NOT TRAINED ON ARE NOT DOTPRODUCT SIMILAR TO ANY IT IS TRAINED ON,
		SUCH AS 3D ROTATION DISPLAYED IN 2D HAS 3 DIMENSIONS TO ROTATE (AND HOW MANY DOES 4D HAVE? 5D HAVE?).
		MAKE IT AS HIGH DIMENSIONAL AS IT CAN LEARN. PUSH IT TO THE LIMIT. THEN MOVE ON TO INTERACTIVE TESTCASES.
		To start with, choose 1d or 2d display.
		*/
		
		Var<RBM> rbmVarPrototype = new Var(rbm);
		
		while(rbm.edlays() > 1) rbm = rbm.popLayer();
		
		Var<RBM> rbmVar = new Var(rbm);
		
		//Var<RBM> rbmVarMouseSelectPredict = new Var(rbm);
		float[] fakeData = new float[visibleNodes];
		for(int i=0; i<fakeData.length; i++) fakeData[i] = (float)i/(fakeData.length-1);
		Var<RbmDisplay> varDisplayWhat = new Var(new RbmDisplay(rbm,new LearningVec_OLD(fakeData)));
		
		//int howManyVecsAtOnce = learnInBatchInsteadOfOneVecAtATime ? 1000 : 50; //gpu could do much more but still need them to fit on screen (TODO scroll instead)
		//int howManyVecsAtOnce = learnInBatchInsteadOfOneVecAtATime ? 2000 : 50; //gpu could do much more but still need them to fit on screen (TODO scroll instead)
		//int howManyVecsAtOnce = 80;
		int howManyVecsAtOnce = 200;
		//int howManyVecsAtOnce = 500;
		
		//float maxLearnErrorToRemove = .01f;
		float maxLearnErrorToRemove = .05f;
		//float maxLearnErrorToRemove = .1f;
		//float maxLearnErrorToRemove = .06f;
		//float maxLearnErrorToRemove = .15f;
		//float maxLearnErrorToRemove = .2f;
		final PaintSlidingVecUi slidingBars = new PaintSlidingVecUi(rbmVar, varDisplayWhat, rbmVarPrototype, visibleNodes, howManyVecsAtOnce, maxLearnErrorToRemove);
		ExperimentUtil.rbmVar = rbmVar;
		ExperimentUtil.learningVecs = slidingBars.learningVecs;
		
		JPanel panel = new JPanel(new GridLayout(0,1));
		
		final RbmPanel p = new RbmPanel(varDisplayWhat);
		JFrame windowB = new JFrame("RBM");
		windowB.add(p);
		//panel.add(p);
		windowB.setSize(1000, 900);
		windowB.setVisible(true);
		
		
		panel.add(new JScrollPane(slidingBars, ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED));
		boolean bootLispBeforePersonPushesEnter = false;
		panel.add(new LearnFuncRbmEditor(rbmVar));
		//panel.add(new JScrollPane(new LispPanel(bootLispBeforePersonPushesEnter),ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER));
		window.add(panel);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int windowH = 500;
		int windowW = windowH;
		
		window.setSize(windowW, windowH);
		window.setLocation(screen.width/2-windowW/2, 260);
		//window.setLocation(screen.width/2-windowW/2, screen.height-60-windowH);
		window.setVisible(true);
		
		while(true){
			double gameControllerInputHowLongAgo = Time.time()-slidingBars.lastTimeGameControllerInput;
			//double sleep = Math.min(Math.pow(1.1,gameControllerInputHowLongAgo)*.005,3); //TODO event based instead of slowing the polling to 3 seconds, but I still want it gradual slowing how often it does nextState and paint, just not waiting after the next movement of mouse etc
			double sleep = gameControllerInputHowLongAgo < 3600 ? .005 : 3;
			//if(gameControllerInputHowLongAgo < 30){
			if(gameControllerInputHowLongAgo < 3600){
				slidingBars.nextState();
				slidingBars.repaint();
			}
			Time.sleepNoThrow(sleep);
			//Thread.yield();
		}
		
	}

}
