/** Ben F Rayfield offers this software opensource MIT license */
package mutable.rbm.ui;
import static mutable.util.Lg.*;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import javax.swing.JFrame;
import javax.swing.JPanel;

import immutable.learnloop.RBM;
import immutable.util.MathUtil;
import mutable.rbm.RbmDisplay;
import mutable.util.Rand;
import mutable.util.Time;
import mutable.util.Var;
import mutable.util.ui.ScreenUtil;
import mutable.util.ui.VertBarGraphics;

public class Slidinglearnrandvecui extends JPanel implements MouseMotionListener, MouseListener{
	
	public boolean removeVecsWhenLearnThemWellEnough = true;
	
	//TODO Redesign this to use a Slidat.
	
	//FIXME todoWhyIsRandomVecLessThanHalfHeight
	
	public final Var<RBM> rbmVar;
	
	public final Var<RbmDisplay> varDisplayWhat;
	
	/** Prototype RBM is for RBM.pushLayerFrom(RBM). */
	public final Var<RBM> prototype;
	
	public final int vecSize;
	
	public final int howManyVecsAtOnce;
	
	public final List<LearningVec_OLD> learningVecs = new ArrayList();
	
	public boolean enableLearning = true;
	
	/** When vector on left is learned enough, remove it and add a random vector on right. Thats why its called sliding. */
	public final float maxLearnErrorToRemove;
	
	public String display = "";
	
	public final UnaryOperator<float[]> confuser;
	
	public int changeLayerSizeThisMuch;
	
	/** confuser swaps some of the indexs, selected randomly each time its called, to show what the RBM does
	when its inputs are not exactly what it learned, that it can rebuild from partial pattern (invarep).
	Prototype RBM is for RBM.pushLayerFrom(RBM).
	*/
	public Slidinglearnrandvecui(UnaryOperator<float[]> confuser, Var<RBM> rbmVar, Var<RbmDisplay> varDisplayWhat,
			Var<RBM> prototype, int vecSize, int howManyVecsAtOnce, float maxLearnErrorToRemove){
		this.confuser = confuser;
		this.rbmVar = rbmVar;
		this.varDisplayWhat = varDisplayWhat;
		this.prototype = prototype;
		this.vecSize = vecSize;
		this.howManyVecsAtOnce = howManyVecsAtOnce;
		this.maxLearnErrorToRemove = maxLearnErrorToRemove;
		addRemoveVecs();
		addMouseMotionListener(this);
		addMouseListener(this);
		//testRandVecsAndComparing();
		//testRandVecsAndComparing2();
		//System.exit(0);
		setFocusable(true); //for KeyListener
		/*addKeyListener(new KeyListener(){
			public void keyTyped(KeyEvent e){}
			public void keyPressed(KeyEvent e){
				if(e.getKeyCode() == KeyEvent.VK_SPACE){
					SPACE USED FOR SOMETHING ELSE THAN... RBM.gpu = !RBM.gpu;
					display = "RBM.gpu="+RBM.gpu;
				}
			}
			public void keyReleased(KeyEvent e){}
		});*/
	}
	
	public int zigzagsLearn(){
		return varDisplayWhat.get().rbm.zigzagsLearn;
	}
	
	/** has RBM learn on a random one of the LearningVecs and update its statistics
	FIXME TODO This should do many vecs at once with opencl.
	*/
	public void nextState(){
		while(changeLayerSizeThisMuch > 0){
			rbmVar.set(rbmVar.get().pushLayerFromNoThrow(prototype.get()));
			changeLayerSizeThisMuch--;
		}
		while(changeLayerSizeThisMuch < 0){
			rbmVar.set(rbmVar.get().popLayerNoThrow());
			changeLayerSizeThisMuch++;
		}
		
		
		//boolean individualZigzagsAfterFullUp = true;
		//lgErr("TODO iWantRbmClassRedesignedToEasilyVerifyCpuAndGpuDoApproxSameThing");
		if(learnInBatchInsteadOfOneVecAtATime){
			if(learningVecs.isEmpty()) return; //nothing to learn
			//int randomVecsAtOnce = Math.min(Math.max(1,learningVecs.size()/3),300);
			int randomVecsAtOnce = Math.min(Math.max(1,learningVecs.size()/3),100);
			boolean[] whichVecs = MathUtil.xChooseYArray(learningVecs.size(),randomVecsAtOnce,Rand.strongRand);
			float[][] vecsToPredict = new float[randomVecsAtOnce][];
			int foundVecs = 0;
			int learnHowManyVecs = 0;
			for(int v=0; v<whichVecs.length; v++){
				if(whichVecs[v]){
					LearningVec_OLD lv = learningVecs.get(v);
					if(lv.shouldLearn()) learnHowManyVecs++;
					vecsToPredict[foundVecs] = lv.vec;
					foundVecs++;
				}
			}
			RBM rbm = rbmVar.get();
			//boolean divideAttentionByNolaySize = false; //IMPORTANT, RECENTLY EXPERIMENTING WITH THIS PARAM. Affects learnrate scaling.
			float[][] attMergedForPredict = MathUtil.multiplyScalars(rbm.newAttLev1(false),rbm.attLev2PerNodeside);
			float[][] attMergedForLearn = MathUtil.multiplyScalars(rbm.newAttLev1(true),rbm.attLev2PerNodeside);
			//int zigzags = rbm.zigzagLearn.length;
			int zigzags = zigzagsLearn();
			
			//[zigzagIndex][nolay][vecIndex][nodeInNolay]
			//float[][][][] predictions = rbm.predict(vecsToPredict, att, zigzags, individualZigzagsAfterFullUp);
			
			//TODO optimize extremely: Doing this twice is the bottleneck, makes the whole learning twice as slow
			//and is only needed for displaying accuracy, which I want to see updated quickly
			//but in the final product predictionsNotIndividualZigzag will be done maybe only 10% as often as predictionsIndividualZigzag.
			float[][][][] predictionsIndividualZigzag = rbm.predict(vecsToPredict, attMergedForLearn, true); //for training rbm
			float[][][][] predictionsNotIndividualZigzag = rbm.predict(vecsToPredict, attMergedForPredict, false); //for measuring prediction accuracy (or normally would also be for using rbm to predict visibleNodes)
			
			//predictions[zigzag][nolay][vec][node]
			foundVecs = 0;
			for(int v=0; v<whichVecs.length; v++){
				if(whichVecs[v]){
					float[] prediction = predictionsNotIndividualZigzag[predictionsNotIndividualZigzag.length-1][0][foundVecs];
					LearningVec_OLD lv = learningVecs.get(v);
					if(lv.vec != vecsToPredict[foundVecs]) throw new Error("WTF");
					lv.update(prediction);
					lv.predict = RBM.getOneVecFromZigzag(predictionsNotIndividualZigzag, foundVecs);
					//System.out.println("v="+v+" set lv.predict to "+lv.predict);
					lv.learn = null;
					foundVecs++;
				}
			}
			
			//Copy to new array (efficiently reusing innermost arrays) only including vecs to learn.
			//More are predicted than learn.
			int nolays = predictionsNotIndividualZigzag[0].length;
			//[zigzagIndex][nolay][vecIndex][nodeInNolay]
			float[][][][] predictionsToLearn = enableLearning ? new float[zigzags][nolays][learnHowManyVecs][] : null;
			
			if(enableLearning) for(int zigzag=0; zigzag<zigzags; zigzag++){
				for(int nolay=0; nolay<nolays; nolay++){
					int vecsToLearnSize = 0;
					int foundVecs2 = 0;
					for(int v=0; v<whichVecs.length; v++){
						if(whichVecs[v]){
							//reuse innermost array, the node states.
							try{
								LearningVec_OLD lv = learningVecs.get(v);
								if(lv.shouldLearn()){
									predictionsToLearn[zigzag][nolay][vecsToLearnSize] = predictionsIndividualZigzag[zigzag][nolay][foundVecs2];
									lv.learn = RBM.getOneVecFromZigzag(predictionsIndividualZigzag, foundVecs2);
									vecsToLearnSize++;
								}
							}catch(Throwable t){
								throw new Error(t);
							}
							foundVecs2++;
						}
					}
				}
			}
			
			
			//if(enableLearning && lv.enableLearningIfNonrandom && !lv.isRandom) predictionsToLearn[predictionsToLearnSize++] =
			//	TODO wait its per edlay first, not per input, so have to copy it or use it as is but
			//	with a filter such as learnRate per vec and check if its 0.
			//	Its [zigzagIndex][nolay][vecIndex][nodeInNolay] so the innermost array can be reused,
			//	so it is maybe low enough bigo to copy efficiently except maybe it would be a problem for cacheLocality
			//	and maybe GPU is so much faster that CPU doing that would still be a bottleneck.
			//	Look into learnRate per vec.
			
			if(enableLearning){
				
				////predict on all of vecsToLearn[], but only learn on some of them, leaving others as the "test set" never learned.
				//int predictionsToLearnSize = 0;
				//float[][][][] predictionsToLearn = new float[learnHowManyVecs][][][];
				//foundVecs = 0;
				//for(int v=0; v<whichVecs.length; v++){
				//	if(whichVecs[v]){
				//		LearningVec lv = learningVecs.get(v);
				//		if(lv.enableLearning){
				//			predictionsToLearn[predictionsToLearnSize++] = predictions[foundVecs];
				//			if(lv.vec != predictions[foundVecs][0][0]) throw new Error("WTF");
				//		}
				//		foundVecs++;
				//	}
				//}
				
				//FIXME make weightDecay a RBM param
				
				//float weightDecay = .001f;//.015f;//.01f; //FIXME what should this be? Verify L2 norm is working in RBM.learn
				//float weightDecay = .002f;
				float weightDecay = .02f;
				//float weightDecay = .005f;//.015f;//.01f; //FIXME what should this be? Verify L2 norm is working in RBM.learn
				//float weightDecay = -.01f; //FIXME cant be negative, just making sure its decaying at all
				//float weightDecay = .1f;
				//float weightDecay = 2.1f;
				//float weightDecay = .5f;
				//float weightDecay = .2f;
				//float weightDecay = .05f;
				RBM forkEditedRbmAfterLearn = rbm.learn(predictionsToLearn, attMergedForLearn);
				//RBM forkEditedRbmAfterLearn = rbm.learn(predictions, att, zigzags, weightDecay);
				if(rbmVar.get() == rbm){ //else something else set it during rbm.learn, such as quickload rbm from file using number buttons on keyboard
					rbmVar.set(forkEditedRbmAfterLearn);
				}
				
				
				////float maxAllowedRadius = 300f;
				////float maxAllowedRadius = 30f;
				////float maxAllowedRadius = 7f;
				//float maxAllowedRadius = 70f; //has worked for a long time
				//
				////2018-4-23 commentout this norming cuz doing L2 (or trying to experimentally) norm instead (TODO)
				////in the RBM.learn func.
				//RBM forkEditedRbmAfterLearnAndNorm = rbm.setWeight(RBM.edlayNormByMaxRadius(
				//	forkEditedRbmAfterLearn.weight, maxAllowedRadius));
				//rbmVar.set(forkEditedRbmAfterLearnAndNorm);
			}
			addRemoveVecs();
			repaint();
			updateDisplayIfLearningVecChanged();
		}else{
			throw new Error("RBM redesigned so batch is the only way, and you can still do batch of size 1 (TODO allow CPU to do that low lag for prediction and use GPU for learning).");
			/*if(1<2) throw new Error("TODO individualZigzagsAfterFullUp");
			LearningVec_OLD lv = learningVecs.get(Rand.strongRand.nextInt(learningVecs.size()));
			rbmVar.set(rbmVar.get().setIn(lv.vec).think());
			lv.update(rbmVar.get().prediction());
			addRemoveVecs();
			repaint();
			lgErr("TODO use RBM.learnGpu");
			*/
		}
	}
	
	public float[] nextTrainingVecOrCounterexample(boolean isExample, boolean isRandom){
		throw new UnsupportedOperationException("Implement this in subclasses");
	}
	
	public boolean userSaysToAddAnotherVec;
	
	public static final int maxNewVecsPerSlideCycle = 30;
	
	public static final int maxVecsInSlidingWindow = 300;
	
	//TODO make these fractions params...
	
	/** predicted, not trained, should NOT be learned indirectly (such as learning identityFunc).
	This red line rises but less than the blue and green.
	*/
	public final float fractionOfVecsInSlidingWindowShouldBeCounterexamples = .15f;
	
	/** predicted, not trained, should be learned indirectly. This is the blue line. */
	public final float fractionOfVecsAreTestSet = .15f;
	
	/** the green line */
	public final float frractionOfVecsInSlidingWindowToLearn(){
		return 1-fractionOfVecsAreTestSet-fractionOfVecsInSlidingWindowShouldBeCounterexamples;
	}
	
	protected void addRemoveVecs(){
		synchronized(learningVecs){
			if(learningVecs.size() < howManyVecsAtOnce){
				//only create the tests and counterexamples once since their learnrate is always 0,
				//but maybe should do persistentContrastiveDivergence to unlearn the counterexamples?
				float end = howManyVecsAtOnce*fractionOfVecsInSlidingWindowShouldBeCounterexamples;
				for(int i=0; i<end; i++){ //counterexamples
					boolean isRandom = i<end/2;
					LearningVec_OLD lv = new LearningVec_OLD(nextTrainingVecOrCounterexample(false,isRandom));
					lv.isCounterexample = true;
					lv.isCounterexampleRandomInsteadOfPattern = isRandom;
					lv.enableLearningIfExample = false;
					learningVecs.add(lv);
				}
				for(int i=0; i<howManyVecsAtOnce*fractionOfVecsAreTestSet; i++){ //predict only, learned indirectly
					boolean isRandom = false;
					LearningVec_OLD lv = new LearningVec_OLD(nextTrainingVecOrCounterexample(true,isRandom));
					lv.isCounterexample = false;
					lv.isCounterexampleRandomInsteadOfPattern = isRandom;
					lv.enableLearningIfExample = false;
					learningVecs.add(lv);
				}
			}else{
				/*
				int foundCounterexamples = 0, foundTests = 0, foundLearns = 0; 
				for(LearningVec lv : learningVecs){
					if(lv.isRandom) foundCounterexamples++;
					else if(lv.enableLearningIfNonrandom) foundLearns++;
					else foundTests++;
				}
				nextTrainingVecOrCounterexample
				*/
				
				if(removeVecsWhenLearnThemWellEnough){
					Iterator<LearningVec_OLD> iter = learningVecs.iterator();
					while(iter.hasNext()){
						LearningVec_OLD lv = iter.next();
						if(!lv.isCounterexample && lv.enableLearningIfExample){
							if(lv.aveDiffOfErrDecayBell.ave() <= maxLearnErrorToRemove) {
								iter.remove(); //TODO use aveDiffOfErrDecayBell but I want it displayed if its to be used
								System.out.println("Learned so removing "+lv);
							}
						}
					}
				}
			}
			while(learningVecs.size() < howManyVecsAtOnce){ //learn these
				boolean isRandom = false;
				LearningVec_OLD lv = new LearningVec_OLD(nextTrainingVecOrCounterexample(true,isRandom));
				lv.isCounterexample = false;
				lv.isCounterexampleRandomInsteadOfPattern = isRandom;
				lv.enableLearningIfExample = true;
				learningVecs.add(lv);
			}
		}
		
		/*
		//while(aveErrNearCenter() <= maxLearnErrorToRemove){
		int removed = 0;
		while((aveErr() <= maxLearnErrorToRemove || userSaysToAddAnotherVec) && removed < maxNewVecsPerSlideCycle){
		//if(aveErr() <= maxLearnErrorToRemove || userSaysToAddAnotherVec){
			learningVecs.remove(0);
			userSaysToAddAnotherVec = false;
			removed++;
		}
		while(learningVecs.size() < maxVecsInSlidingWindow){
			learningVecs.add(newLearningVec());
		}
		//TODO remove old LearningVecs that have been learned well enough
		*/
	}
	
	protected double aveErr(){
		if(learningVecs.isEmpty()) return Double.MAX_VALUE;
		double sum = 0;
		for(int i=0; i<learningVecs.size(); i++){
			sum += learningVecs.get(i).aveDiffOfErr;
		}
		return sum/learningVecs.size();
	}
	
	protected double aveErrNearCenter(){
		if(learningVecs.isEmpty()) return Double.MAX_VALUE;
		double centerIndex = learningVecs.size()/2.;
		double centerIndexStdDev = centerIndex/3;
		double weightedSum = 0;
		double sumOfWeights = 0;
		for(int i=0; i<learningVecs.size(); i++){
			double stdDev = (i-centerIndex)/centerIndexStdDev;
			double weight = MathUtil.zbell(stdDev);
			sumOfWeights += weight;
			//weightedSum += weight*learningVecs.get(i).stdDevOfErr;
			weightedSum += weight*learningVecs.get(i).aveDiffOfErr;
		}
		return weightedSum/sumOfWeights;
	}
	
	public void paint(Graphics g){
		//Rectangle rect = TODO sliding offset by plus/minus 1 bar.
		Rectangle rect = g.getClipBounds();
		float[] heightFraction = new float[learningVecs.size()];
		//lg("learningVecsSize="+learningVecs.size());
		int[] colorARGB = new int[learningVecs.size()];
		double now = Time.time();
		//String s = "";
		float sumHeights = 0;
		int countRands = 0, countLearns = 0, countTests = 0;
		float sumRands=0, sumLearns=0, sumTests=0;
		for(int i=0; i<learningVecs.size(); i++){
			//heightFraction[i] = (float)(1-learningVecs.get(i).stdDevOfErr);
			//heightFraction[i] = (float)(1-learningVecs.get(i).aveDiffOfErr);
			heightFraction[i] = (float)(1-learningVecs.get(i).aveDiffOfErrDecayBell.ave());
			//heightFraction[i] = (i+.5f)/learningVecs.size(); //test. graphics appears correct.
			//s += " ["+i+"]="+heightFraction[i];
			sumHeights += heightFraction[i];
			//colorARGB[i] = 0xff55bbff; //FIXME gradually darker as LearningVec.timePredicted is older.
			float bright = brightnessOfAge((float)(now-learningVecs.get(i).timePredicted));
			float red, green, blue;
			if(learningVecs.get(i).isCounterexample){
				red = bright;
				green = 0;
				blue = 0;
				countRands++;
				sumRands += heightFraction[i];
			}else if(learningVecs.get(i).enableLearningIfExample) {
				red = 0;
				green = bright*.7f;
				blue = 0;
				countLearns++;
				sumLearns += heightFraction[i];
			}else{
				red = bright*.1f;
				green = bright*.1f;
				blue = bright;
				countTests++;
				sumTests += heightFraction[i];
			}
			colorARGB[i] = ScreenUtil.color(red, green, blue);
		}
		float aveHeight = sumHeights/learningVecs.size();
		//lg("Heights: "+s+" aveHeight="+aveHeight);
		int colorBackground = enableLearning ? 0xff000000 : 0xff00ff00;
		VertBarGraphics.paint(g, rect, colorBackground, heightFraction, colorARGB);
		
		if(countRands != 0){
			g.setColor(Color.red);
			int redHeight = (int)Math.round(getHeight()*sumRands/countRands);
			g.drawLine(0, getHeight()-redHeight, getWidth(), getHeight()-redHeight);
		}
		if(countLearns != 0){
			g.setColor(new Color(0f,.7f,0f));
			int greenHeight = (int)Math.round(getHeight()*sumLearns/countLearns);
			g.drawLine(0, getHeight()-greenHeight, getWidth(), getHeight()-greenHeight);
		}
		if(countTests != 0){
			g.setColor(new Color(.1f,.1f,1f));
			int blueHeight = (int)Math.round(getHeight()*sumTests/countTests);
			g.drawLine(0, getHeight()-blueHeight, getWidth(), getHeight()-blueHeight);
		}
		
		/*for(int i=0; i<=30; i++){
			double fraction = Math.sqrt((double)i/30);
			//double fraction = (double)i/30;
			g.setColor(Color.blue);
			int y = (int)(fraction*getHeight());
			g.drawLine(0, y, getWidth(), y);
		}*/
		g.setColor(new Color(0xff00ff));
		g.drawString(display, 30, 30);
	}
	
	public float brightnessOfAge(float secondsOld){
		//return 1.5f/(1.5f+secondsOld);
		return .5f/(.5f+secondsOld);
	}
	
	/*public LearningVec newLearningVec(){
		//Time.sleepNoThrow(.000001); //so they have unique times
		return new LearningVec(newRandVec());
	}*/
	
	public void testRandVecsAndComparing(){
		float sumAveDiffs = 0;
		int r = 10000;
		for(int i=0; i<r; i++){
			float[] a = newRandVec();
			float[] b = newRandVec();
			float aveDiff = RBM.aveDiff(a, b);
			sumAveDiffs += aveDiff;
			//lg("aveDiff="+aveDiff);
		}
		float aveAveDiff = sumAveDiffs/r;
		lg("aveAveDiff="+aveAveDiff);
	}
	
	/*commentedout cuz RBM redesigned
	public void testRandVecsAndComparing2(){
		RBM rbm = rbmVar.get();
		byte[] ser = rbm.tempToBytesTodoUseTobitsInterfaceNotJavaSerializable();
		long serHash = 0;
		for(byte b : ser) serHash = serHash*15+b;
		
		int r = 1000;
		double sumAveDiff = 0, sumAveDiffR = 0;
		for(int i=0; i<r; i++){
			if(i%100==0) System.out.println("i="+i);
			float[] univecIn = newRandVec();
			float[] univecOut = rbm.setLearnRate(0).setIn(univecIn).think().prediction();
			float[] univecRand = newRandVec();
			//univecOut = new float[]{0.008462126f,0.84542155f,0.07008242f,0.7228746f,0.07668927f,0.05322222f,0.085964896f,0.10463536f,0.99947125f,0.99750316f,0.924663f,0.82653826f,0.19199476f,0.7407165f,0.8056566f,0.99897254f,0.83630675f,0.17602141f,0.29635486f,0.67708373f,0.97792506f,0.023311203f,0.53272456f,0.7545443f,0.55671954f,0.8174713f,0.996626f,0.9951523f,0.3681549f,0.92763156f,0.95791495f,0.9947089f,0.92631227f,0.97516584f,0.8286145f,0.063367605f,0.53943396f,0.2085132f,0.36449885f,0.3592844f,0.91166383f,0.33183062f,0.93193555f,0.86369413f,0.38700223f,0.7178857f,0.38751355f,0.22312589f,0.6003849f,0.30102646f,0.0033140099f,0.18307652f,0.9979142f,0.7469062f,0.33726192f,0.44976026f,0.30465043f,0.80877376f,0.11489773f,0.6350446f,0.39952788f,0.084963895f,0.0103436485f,0.99213284f,0.6383715f,0.028061755f,0.7067349f,0.09747269f,0.14416936f,0.0928415f,0.6989677f,0.6986973f,0.8645475f,0.96349245f,0.9690271f,0.92291003f,0.6118977f,0.90014243f,0.113979675f,0.78207785f};
			//for(int i=0; i<vecSize; i++) univecOut[i] = (i+.5f)/vecSize;
			float aveIn = RBM.sum(univecIn)/vecSize;
			float aveOut = RBM.sum(univecOut)/vecSize;
			float aveRand = RBM.sum(univecRand)/vecSize;
			float aveDiff = RBM.aveDiff(univecIn, univecOut);
			float aveDiffR = RBM.aveDiff(univecRand, univecOut);
			if(i==0){
				String out = "new float[]{"+univecOut[0];
				for(int j=1; j<vecSize; j++) out += "f,"+univecOut[j];
				out += "f}";
				display = "i="+i+" aveDiff="+aveDiff+" aveIn="+aveIn+" aveOut="+aveOut+" aveDiffR="+aveDiffR+" serHash="+serHash;
				lgErr("testRandVecsAndComparing2 "+out+" "+display);
			}
			sumAveDiff += aveDiff;
			sumAveDiffR += aveDiffR;
		}
		double aveAveDiff = sumAveDiff/r;
		double aveAveDiffR = sumAveDiffR/r;
		lgErr("testRandVecsAndComparing2 aveAveDiff="+aveAveDiff+" aveAveDiffR="+aveAveDiffR);
	}*/
	
	public float[] newRandVec(){
		//return RBM.sizeChooseY(vecSize, vecSize/4);
		//return RBM.sizeChooseY(vecSize, vecSize/2);
		//What if the xChooseY is causing slidinglearnrandvecuiIsAddingNewVecsThatOnAveAlreadyScoreHigherThanHalfSoSomethingIsBrokenThatCantBeTrueOfRandomVecsAndWorkingRbm?
		float[] vec = new float[vecSize];
		//Random rand = new SecureRandom(); //FIXME Is Rand.strongRand creating patterns between RBM and newRandVec? Lets find out.
		//rand.setSeed(System.nanoTime()+new Object().hashCode()); 
		//Random rand = new Random(); //FIXME Is Rand.strongRand creating patterns between RBM and newRandVec? Lets find out.
		//rand.setSeed(System.nanoTime());
		Random rand = Rand.strongRand;
		for(int i=0; i<vec.length; i++){
			vec[i] = rand.nextBoolean()?1:0;
		}
		return vec;
	}

	public void mouseDragged(MouseEvent e){ mouseMoved(e); }

	public void mouseMoved(MouseEvent e){
		if(!learningVecs.isEmpty()){
			updateDisplayIfLearningVecChanged(
				learningVecs.get(Math.max(0,Math.min(learningVecs.size()*e.getX()/getWidth(),learningVecs.size()-1))));
		}
	}
	
	public void updateDisplayIfLearningVecChanged(){
		//only sets if not Object.equals prev RbmDisplay, which will not if same RBM, LearningVec*, and LearningVec*.cycles
		varDisplayWhat.set(new RbmDisplay(rbmVar.get(), varDisplayWhat.get().lv));
	}
	
	public void updateDisplayIfLearningVecChanged(LearningVec_OLD selectedLearningVec){
		//only sets if not Object.equals prev RbmDisplay, which will not if same RBM, LearningVec*, and LearningVec*.cycles
		varDisplayWhat.set(new RbmDisplay(rbmVar.get(), selectedLearningVec));
	}
	
	public static final boolean learnInBatchInsteadOfOneVecAtATime = true; //FIXME should be true
	
	public static final UnaryOperator<float[]> defaultConfuser = (float[] in)->{
		float[] out = in.clone();
		Random r = Rand.strongRand;
		for(int i=0; i<in.length/8; i++){
			int b = r.nextInt(in.length);
			int c = r.nextInt(in.length);
			float temp = out[b];
			out[b] = out[c];
			out[c] = temp;
		}
		return out;
	};
	
	/*commentedout cuz RBM redesigned
	public static void main(String[] args){
		JFrame window = new JFrame("RBM experiment. + pushes layer. - pops layer. mouseRightClick toggles learning. move mouse over a vertical bar to see that input in other window.");
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		Random rand = Rand.strongRand;
		
		//int squaresY = 8, squaresX = 8;
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, squaresY*squaresX, 200, 200);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 50, 1000);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 100, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 30, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 30, 500);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 30, 2000);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 80, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 80, 80);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 100, 100);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 300, 300);
		//final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, neuralMin, neuralMax, 300, 300, 300);
		final float[][][] rbmEdges = RBM.newRandomEdges(rand, -1, 10f, 100,1000);
		
		int visibleNodes = rbmEdges[0].length;
		
		//int zigzagPredict = 10;
		int zigzagPredict = 2;
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
		boolean[] isScalar = new boolean[nolays];
		//Arrays.fill(isScalar, true); //FIXME
		isScalar[0] = true;
		
		RBM rbm = new RBM()
			.setComment("mouseai1dAtTime"+Time.stardateStr())
			.setIsScalar(isScalar)
			.setLearnByIncompleteBoltzenCode(false)
			.setAttentionRelRangeForLearn(.5f)
			.setIn(new float[visibleNodes])
			.setRand(Rand.strongRand)
			.setTargetNodeAve(targetNodeAve)
			.setWeight(rbmEdges)
			.setZigzagPredict(zigzagPredictArray)
			.setZigzagLearn(zigzagLearnArray)
			.setZigzagNorm(zigzagNormArray)
			//.setLearnRate(.003f); //works for 1 vec at a time (cpu) but gpu divides it among all vecs, so need more
			//.setLearnRate(.003f); //gpu divides this among all vecs in a batch
			//.setLearnRate(.0003f); //works with 4 nodes learned at once, slowly (on cpu, and thats not big enough for gpu)
			//.setLearnRate(.00003f);
			//.setLearnRate(.01f);
			//.setLearnRate(.1f);
			.setLearnRate(.2f);
			//.setLearnRate(.001f);
			//.setLearnRate(1f);
		
		Var<RBM> rbmVar = new Var(rbm);
		//final Mouseai1d p = new Mouseai1d(rbmEdges, zigzagPredict, zigzagLearn, rand);
		
		Var<RBM> rbmVarMouseSelectPredict = new Var(rbm);
		
		Var<RBM> prototype = new Var(rbm);
		
		final RbmPanel p = new RbmPanel(rbmVarMouseSelectPredict);
		
		//int howManyVecsAtOnce = gpu ? 400 : 50; //gpu could do much more but still need them to fit on screen (TODO scroll instead)
		//int howManyVecsAtOnce = learnInBatchInsteadOfOneVecAtATime ? 500 : 50; //gpu could do much more but still need them to fit on screen (TODO scroll instead)
		//int howManyVecsAtOnce = learnInBatchInsteadOfOneVecAtATime ? 100 : 50; //gpu could do much more but still need them to fit on screen (TODO scroll instead)
		int howManyVecsAtOnce = learnInBatchInsteadOfOneVecAtATime ? 1000 : 50; //gpu could do much more but still need them to fit on screen (TODO scroll instead)
		
		//float maxLearnErrorToRemove = .03f;
		//float maxLearnErrorToRemove = .1f;
		float maxLearnErrorToRemove = .2f;
		//float maxLearnErrorToRemove = .06f;
		//float maxLearnErrorToRemove = .15f;
		//float maxLearnErrorToRemove = .2f;
		//float maxLearnErrorToRemove = .25f;
		//float maxLearnErrorToRemove = .35f;
		final Slidinglearnrandvecui slidingBars = new Slidinglearnrandvecui(defaultConfuser, rbmVar, rbmVarMouseSelectPredict, prototype, visibleNodes, howManyVecsAtOnce, maxLearnErrorToRemove);
		
		//final Mouseai1d p = new Mouseai1d(rbmVar, rand);
		//
		//window.setSize(totalNodes+p.nodeDisplayWidth*zigzagDisplay+35,Math.max(totalNodes+55,700));
		//ScreenUtil.moveToHorizontalScreenCenter(window);
		//
		//p.logOnScreen.put("fps", ()->""+p.fps);
		//p.logOnScreen.put("learnMultiplier", ()->""+p.learnMultiplier);
		//p.logOnScreen.put("weightAve", ()->""+p.weightAve);
		//p.logOnScreen.put("weightDev", ()->""+p.weightDev);
		////p.logOnScreen.put("edlay0Ratio", ()->""+rbmEdges[0].ratio);
		//p.logOnScreen.put("lastSaveOrLoadMessage", ()->p.lastSaveOrLoadMessage);
		////p.logOnScreen.put("fractionCorrectNow", ()->""+p.fractionCorrectNow);
		////p.logOnScreen.put("fractionCorrectRecently", ()->""+p.fractionCorrectRecently);
		//p.logOnScreen.put("aveInputNow", ()->""+p.aveInputNow);
		//p.logOnScreen.put("aveOutputNowNolay0", ()->""+p.aveOutputNolay0Now);
		//p.logOnScreen.put("aveOutputNowNolay1", ()->""+p.aveOutputNolay1Now);
		//p.logOnScreen.put("aveOutputNowNolay2", ()->""+p.aveOutputNolay2Now);
		//p.logOnScreen.put("aveInputRecently", ()->""+p.aveInputRecently);
		//p.logOnScreen.put("aveOutputRecentlyNolay0", ()->""+p.aveOutputNolay0Recently);
		//p.logOnScreen.put("aveOutputRecentlyNolay1", ()->""+p.aveOutputNolay1Recently);
		//p.logOnScreen.put("aveOutputRecentlyNolay2", ()->""+p.aveOutputNolay2Recently);
		//p.logOnScreen.put("~ en.pr.el0", ()->{
		//	float[][] nodes = rbmVar.get().zigzagPredict[rbmVar.get().zigzagPredict.length-1];
		//	return ""+BoltzenUtil.energy(nodes[0], rbmVar.get().weight[0], nodes[1]);
		//});
		////p.alsoLearnOppositeVector = false;
		////p.randomizeChaostimeBackBeforePredict = false;
		////p.doPersistentContrastiveDivergence = false; //FIXME true
		
		JPanel panel = new JPanel(new GridLayout());
		
		//panel.add(new RbmView2d(squaresY, squaresX, rbmVar), BorderLayout.WEST);
		
		panel.add(p);
		panel.add(slidingBars);
		window.add(panel);
		//window.setSize(900, 500);
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int windowH = 300;
		window.setSize(screen.width, windowH);
		//ScreenUtil.moveToScreenCenter(window);
		window.setLocation(0, screen.height-60-windowH);
		window.setVisible(true);
		while(true){
			slidingBars.nextState();
			//Time.sleepNoThrow(.01);
			Thread.yield();
		}
	}*/

	public void mouseClicked(MouseEvent e){}

	public void mousePressed(MouseEvent e){
		if(e.getButton() == MouseEvent.BUTTON1){
			userSaysToAddAnotherVec = true;
		}else if(e.getButton() == MouseEvent.BUTTON2){
			enableLearning = !enableLearning;
		}else  if(e.getButton() == MouseEvent.BUTTON3){
			//testRandVecsAndComparing2();
		}
		lg("enableLearning="+enableLearning);
	}
	
	public void mouseReleased(MouseEvent e){}
	
	public void mouseEntered(MouseEvent e){}
	
	public void mouseExited(MouseEvent e){}

}


