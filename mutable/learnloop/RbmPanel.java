/** Ben F Rayfield offers this software opensource MIT license */
package mutable.learnloop;
//import static humanaicore.common.CommonFuncs.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.DoubleToIntFunction;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javax.swing.JPanel;

import immutable.datasets.mnistOcr.MnistLabeledImage;
import immutable.datasets.mnistOcr.MnistOcrDataset;
import immutable.learnloop.RBM;
import immutable.util.MathUtil;
import mutable.rbm.RbmDisplay;
import mutable.rbm.ui.AiGraphics;
import mutable.rbm.ui.LearningVec_OLD;
import mutable.util.Lg;
import mutable.util.Rand;
import mutable.util.Time;
import mutable.util.Var;

/** TODO Uses float[zigzag][nolay][0][node] which aligns with GPU datastruct float[zigzag][nolay][whichVec][node],
unlike RbmPanelOld which uses float[zigzag][nolay][node].
RBM.getOneVecFromZigzag(float[][][][],int) gets 1 vec out of that to be used in the nw RbmPanel.
This was needed cuz cpu learning code vs gpu learning code got out of sync during research.
There are up to 2 float[zigzag][nolay][0][node] in LearningVec* class, one for predict,
and if learning was also done that time (aligned to that prediction),
one for learn (else LearningVec*.learn==null). These will both be displayed right of the diagonal.
So I'm adding Var<LearningVec*>, and this class will only display it instead of call RBM,
and will also display RBM weights.
*/
public class RbmPanel extends JPanel{
	
	/** display the LearningVec*.learn and .predict (float[][][0][]) this right of diagonal, and RBM.weight left/under diagonal. */
	public final Var<RbmDisplay> varDisplayWhat;
	
	static{Lg.todo("VERY IMPORTANT I want rbmpanel upgraded to display directional bias and att (RBM.biasPerNodeside and RBM.attPerNodeside) per node on the top and right edges of each rectangle edlay, a few pixels wide, and space the edlays on diag out those few pixels extra.");}
	
	/** immutable snapshot of RBM */
	//public RBM rbm;
	
	
	//public float[][][] weight;
	
	
	//public float[][][] derivDown, derivUp;
	
	//public float[][] targetNodeAve;
	
	//DONT USE NODEAVE TO CHOOSE WEIGHT CHANGES BASED ON derivDown and derivUp. INSTEAD,
	//CHANGE WEIGHTS EVERY CYCLE, SUCH AS IF YOU WANT .25 CHANCE OF A NODE THEN
	//WHEN ITS 1, LOWER IT 3 TIMES FASTER THAN RAISING IT WHEN ITS 0.
	//public float[][] nodeAve;
	
	//public float derivDecay = 1/2.5f;
	
	
	//public float[][] atts;
	
	//public float[][][] zigzagPredict, zigzagLearn;
	
	protected BufferedImage[] edgeLayerImage;
	
	/** display each row in edlay0 as a square, instead of its usual 1 pixel tall and visibleNodes pixels wide */
	protected BufferedImage featureVectorImage;
	
	/** move it down right this many pixels */
	protected int startDownRightPixels = 10;
	
	protected Point[] edgeLayerPoint;
	
	public final NavigableMap<String,Supplier<String>> logOnScreen = new TreeMap();
	
	private final Consumer<Var> rbmListener;
	
	/** if this changes, prepareForCertainSizeOfRbm again */
	protected String hashRbmSizes;
	
	protected int mouseY, mouseX;
	
	protected int widthIfVisNodesAreSquare;
	
	public RbmPanel(Var<RbmDisplay> varDisplayWhat){
	//public RbmPanel(float[][][] weight, int zigzagPredict, int zigzagLearn){
		//this.rbm = rbm;
		this.varDisplayWhat = varDisplayWhat;
		varDisplayWhat.startListening(rbmListener = (Var v)->{
			repaint();
		});
		//this.weight = weight;
		//this.zigzagPredict = new float[zigzagPredict][][];
		//for(int i=0; i<zigzagPredict; i++) this.zigzagPredict[i] = AI.newNodes(weight);
		//this.zigzagLearn = new float[zigzagLearn][][];
		//for(int i=0; i<zigzagLearn; i++) this.zigzagLearn[i] = AI.newNodes(weight);
		//this.nodes = nodes;
		
		RbmDisplay rd = varDisplayWhat.get();
		RBM r = rd.rbm;
		hashRbmSizes = hashRbmSizes(r);
		prepareForCertainSizeOfRbm(r);
		
		
		setBackground(new Color(.5f, .5f, .5f));
		setForeground(Color.white);
		logOnScreen.put("nolayAves", ()->{
			/*RBM rbm = varDisplayWhat.get().rbm;
			float[][][] predict = rbm.zigzagPredict;
			*/
			RBM rbm = varDisplayWhat.get().rbm;
			LearningVec_OLD lv = varDisplayWhat.get().lv;
			if(lv.predict == null) return "no prediction yet";
			String s = "";
			int nolays = rbm.nolays();
			for(int i=0; i<nolays; i++){
				s += " "+(MathUtil.ave(lv.predict[lv.predict.length-1][i][0])+"0000000").substring(0,5);
			}
			return s.trim();
		});
		logOnScreen.put("weightAve", ()->""+weightAve());
		logOnScreen.put("weightDev", ()->""+weightDev());
		logOnScreen.put("time", ()->Time.timeStr());
		logOnScreen.put("rbm", ()->""+varDisplayWhat.get().rbm);
		logOnScreen.put("lv", ()->""+varDisplayWhat.get().lv);
		logOnScreen.put("stdDev(lv.predict,lv.learn)", ()->{
			LearningVec_OLD lv = varDisplayWhat.get().lv;
			if(lv.predict==null) return "lv.predict==null";
			if(lv.learn==null) return "lv.learn==null";
			if(lv.predict==lv.learn) return "ERROR: lv.predict==lv.learn";
			return ""+RBM.stdDev(lv.predict, lv.learn);
		});
		logOnScreen.put("attlev1RelRangeForLearn", ()->""+varDisplayWhat.get().rbm.attLev1RelRangeForLearn);
		logOnScreen.put("attlev1RelRangeForPredict", ()->""+varDisplayWhat.get().rbm.attLev1RelRangeForPredict);
		
		addMouseMotionListener(new MouseMotionListener(){
			public void mouseMoved(MouseEvent e){
				mouseY = e.getY();
				mouseX = e.getX();
				repaint();
			}
			public void mouseDragged(MouseEvent e){
				mouseMoved(e);
			}
		});
	}
	
	protected void prepareForCertainSizeOfRbm(RBM prototype){
		RBM rbm = varDisplayWhat.get().rbm;
		int visibleNodes = RBM.nolaySize(rbm.weight, 0);
		widthIfVisNodesAreSquare = (int)Math.sqrt(visibleNodes);
		edgeLayerImage = Arrays.asList(rbm.weight).stream()
			.map((edlay)->AiGraphics.newBufferedImageYX(edlay[0].length,edlay.length))
			.toArray((s)->new BufferedImage[s]);
		featureVectorImage = AiGraphics.newBufferedImageYX(widthIfVisNodesAreSquare,widthIfVisNodesAreSquare);
		edgeLayerPoint = new Point[rbm.weight.length];
		int cumulativePixels = startDownRightPixels;
		for(int e=0; e<rbm.weight.length; e++){
			int nextNodes = rbm.weight[e].length; //FIXME is that backward in [][]? //stat.nodes[0][e].length;
			edgeLayerPoint[e] = new Point(cumulativePixels, cumulativePixels+nextNodes);
			cumulativePixels += nextNodes;
		}
	}
	
	/** Like visibleNodes are viewed as a square in topright corner, these are squares of nolay1 down to visibleNodes,
	displayed the same color as those edges.
	*/
	public void paintFeatureVecs(Graphics2D g, DoubleToIntFunction edgePainter, float magnify){
		float[][] edlay0 = varDisplayWhat.get().rbm.weight[0];
		int vecs = edlay0[0].length;
		int w = getWidth(), h = getHeight();
		float width = widthIfVisNodesAreSquare*magnify;
		int cols = (int)(w/width); //size of each row of vecs. Wrap to next row after that.
		int maxRows = (int)((h+width-1)/width);
		int maxV = Math.min(vecs, cols*maxRows);
		for(int v=0; v<maxV; v++){
			int row = v/cols;
			int col = v%cols;
			float y = row*width;
			float x = col*width;
			paintFeatureVec(v, g, y, x, magnify, magnify, edgePainter);
		}
	}
	
	public void paintFeatureVec(int whichFeatureVec, Graphics2D g, float y, float x, float magnifyY, float magnifyX, DoubleToIntFunction edgePainter){
		paintFeatureVec(featureVectorImage, edgePainter, whichFeatureVec);
		paint(g, featureVectorImage, y, x, magnifyY, magnifyX);
	}
	
	/** edlay0[BufferedImage pixel][whichFeatureVec]. edgePainter(weight) returns color.
	FIXME this may be slow compared to doing the math without a DoubleToIntFunction, impractical to cover the screen in these without magnifying?
	*/
	public void paintFeatureVec(BufferedImage bi, DoubleToIntFunction edgePainter, int whichFeatureVec){
		float[][] edlay0 = varDisplayWhat.get().rbm.weight[0];
		int i = 0;
		for(int y=0; y<widthIfVisNodesAreSquare; y++){
			for(int x=0; x<widthIfVisNodesAreSquare; x++){
				bi.setRGB(x, y, edgePainter.applyAsInt(edlay0[i][whichFeatureVec]));
				if(++i==edlay0.length) return;
			}
		}
	}
	
	public void paint(Graphics2D g, BufferedImage im, float y, float x, float magnifyY, float magnifyX){
		AffineTransform aftrans = new AffineTransform(magnifyY, 0, 0, magnifyX, x, y);
		g.drawImage(im, aftrans, this);
	}
	
	protected void finalize() throws Throwable{
		varDisplayWhat.stopListening(rbmListener);
	}

	public float weightAve(){ return weightAve; }
	public float weightDev(){ return weightDev; }
	protected float weightAve, weightDev;
	
	/** on diagonal, 1 pixel tall, and this many pixels wide, right of diagonal */
	protected int nodeDisplayWidth = 11;
	
	protected String hashRbmSizes(RBM prototype){
		String hash = "hashRbmSizes ";
		for(int nolaySize : RBM.nolaySizes(prototype.weight)){
			hash += " "+nolaySize;
		}
		return hash;
	}
	
	public void paint(Graphics g){
		try{
			RbmDisplay rd = varDisplayWhat.get();
			RBM rbm = rd.rbm;
			LearningVec_OLD lv = rd.lv;
			String newHash = hashRbmSizes(rbm);
			//System.out.println("newHash of rbm sizes: "+newHash);
			if(!newHash.equals(hashRbmSizes)){
				prepareForCertainSizeOfRbm(rbm);
				hashRbmSizes = newHash;
			}
			g.setColor(getBackground());
			int h = getHeight(), w = getWidth();
			g.fillRect(0, 0, w, h);
			
			float weightSum = 0;
			int weightCount = 0;
			for(float[][] edlay : rbm.weight){
				for(float[] v : edlay){
					for(float f : v){
						weightSum += f;
					}
					weightCount += v.length;
				}
			}
			//rbm = rbm.setweweightAve = weightSum/weightCount;
			weightAve = weightSum/weightCount;
			float weightSumOfSquares = 0;
			for(float[][] edlay : rbm.weight){
				for(float[] v : edlay){
					for(float f : v){
						float diff = f-weightAve;
						weightSumOfSquares += diff*diff;
					}
				}
			}
			weightDev = (float)Math.sqrt(weightSumOfSquares/weightCount);
			
			
			//final float range = minWeight==maxWeight ? 1 : maxWeight-minWeight;
			//final float min = minWeight;
			DoubleToIntFunction fractionToColor = (fraction)->{
				int byt = (int)(fraction*256);
				if(byt < 0) byt = 0;
				else if(255 < byt) byt = 255;
				return 0xff000000 | (byt<<16) | byt<<8 | byt; //red green and blue equal
			};
			//DoubleToIntFunction edgePainter = (w)->fractionToColor.applyAsInt((w-min)/range); //TODO multiply by color vector
			DoubleToIntFunction edgePainter = //TODO multiply by color vector
				(wt)->fractionToColor.applyAsInt(RBM.holdInRange(0, .5+.1*(wt-weightAve)/weightDev, 1));
				//(w)->fractionToColor.applyAsInt(MathUtil.holdInRange(0, .5+.2*(w-weightAve)/weightDev, 1));
			DoubleToIntFunction nodePainter = fractionToColor;
			
			if(!(g instanceof Graphics2D)) throw new Error("Not "+Graphics2D.class.getName()+" TODO paint without magnify");
			paintFeatureVecs((Graphics2D)g, edgePainter, 5);
			
			IntStream.range(0,rbm.weight.length).forEach((e)->paint(g,e,edgeLayerPoint[e],edgePainter));
			int cumulativePixels = startDownRightPixels;
			int nodeLayers = rbm.weight.length+1;
			int nolay0Size = RBM.nolaySize(rbm.weight, 0);
			int nolay1Size = RBM.nolaySize(rbm.weight, 1);
			for(int nodeLayer=0; nodeLayer<nodeLayers; nodeLayer++){
				int nodes = RBM.nolaySize(rbm.weight, nodeLayer);
				for(int n=0; n<nodes; n++){
					int yAndX = cumulativePixels+n;
					//g.setColor(new Color(nodePainter.applyAsInt(stat.nodes[0][nodeLayer][n])));
					//g.drawLine(yAndX, yAndX, yAndX+5, yAndX-5);
					//g.setColor(new Color(nodePainter.applyAsInt(stat.nodesAtStart[nodeLayer][n])));
					//g.drawLine(yAndX+5, yAndX-5, yAndX+10, yAndX-10);
					int lp = 0;
					for(int predictOrLearn=0; predictOrLearn<2; predictOrLearn++){
					//for(int predictOrLearnOrNorm=0; predictOrLearnOrNorm<3; predictOrLearnOrNorm++){
						float[][][][] zigzag;
						if(predictOrLearn == 0) zigzag = lv.predict;
						else if(predictOrLearn == 1) zigzag = lv.learn;
						else throw new Error("zigzag = rbm.zigzagNorm; removed");
						if(zigzag == null){
							//
						}else{
							for(int zz=0; zz<zigzag.length; zz++){
								//TODO rewrite this code to not mirror the zigzag var but still do it on screen. Its confusing.
								int z = zigzag.length-1-zz;
								int color = nodePainter.applyAsInt(zigzag[z][nodeLayer][0][n]);
								g.setColor(new Color(color));
								g.drawLine(yAndX+nodeDisplayWidth*zz+lp, yAndX, yAndX+nodeDisplayWidth*(zz+1)+lp, yAndX);
								
								//FIXME Move this code somewhere else. It assumes visibleNodes are a square picture
								//and displays it, both input and output (as 2 colorDims) in top right.
								int mag = 12; //magnify
								if(predictOrLearn==0 && nodeLayer==0 && z==0){
									int picY = mag*n/widthIfVisNodesAreSquare;
									int picX = (n%widthIfVisNodesAreSquare)*mag + w - widthIfVisNodesAreSquare*mag;
									int whichEdlay0FeatureVector = mouseY-edgeLayerPoint[0].y; //index in nolay1
									if(whichEdlay0FeatureVector < 0 || whichEdlay0FeatureVector >= nolay1Size || mouseX < edgeLayerPoint[0].x || mouseX >= edgeLayerPoint[0].x+nolay0Size){
										whichEdlay0FeatureVector = -1;
									}
									boolean displayPredictionInsteadOfEdlay0FeatureVector = whichEdlay0FeatureVector==-1;
									int colorPredict = displayPredictionInsteadOfEdlay0FeatureVector
										? nodePainter.applyAsInt(zigzag[zigzag.length-1][nodeLayer][0][n])
										: edgePainter.applyAsInt(rbm.weight[0][n][whichEdlay0FeatureVector]);
									//int colorPic = (color&0xff00ff00)|(colorPredict&0xff0000ff);
									
									g.setColor(new Color(colorPredict));
									//g.setColor(new Color(0xff000000 | Rand.strongRand.nextInt(0x1000000)));
									g.fillRect(picX, picY, mag, mag);
									
									g.setColor(new Color(color&0xff00ff00));
									g.fillRect(picX+mag/2-3, picY+mag/2-3, 6, 6);
									
								}
								
								/*boolean up = (z&1)==0;
								//paint a dot representing recent average node value in the middle of every painted line of current value,
								//alternating diagonal rows which are the zigzags of rbm inference
								//UPDATE: 2 dots at the ends of such lines cuz it was visually confusing in the middle.
								if(up){
									if(0 < nodeLayer){
										color = nodePainter.applyAsInt(MathUtil.holdInRange(0, weight[nodeLayer-1].highNodeAve[n], 1));
									}
								}else{
									if(nodeLayer < zigzag[0].length-1){ //not highest
										color = nodePainter.applyAsInt(MathUtil.holdInRange(0, weight[nodeLayer].lowNodeAve[n], 1));
									}
								}
								g.setColor(new Color(color));
								g.drawRect(yAndX+(nodeLen*zz), yAndX, 2, 1);
								*/
							}
							//paint zigzagPredict, 1 empty node, zigzagLearn, 1 empty node, zigzagNorm
							lp += (zigzag.length+1)*nodeDisplayWidth;
						}
					}
				}
				//predict and learn have same number of nodes per layer
				//int nolaySize = rbm.zigzagPredict[0][nodeLayer].length;
				//int nolaySize = lv.predict[0][nodeLayer][0].length; //lv.predict doesnt exist until first prediction of that vec
				int nolaySize = rbm.nolaySize(rbm.weight, nodeLayer);
				cumulativePixels += nolaySize;
			}
			int displayTextY = getHeight()-20;
			int displayTextX = 10;
			g.setColor(new Color(.8f, .8f, .8f));
			g.setFont(g.getFont().deriveFont(Font.BOLD));
			for(Map.Entry<String,Supplier<String>> entry : logOnScreen.entrySet()){
				String display = entry.getKey()+": "+entry.getValue().get();
				g.drawString(display, displayTextX, displayTextY);
				displayTextY -= 15;
			}
			/*for(int d=0; d<10; d++){
				MnistLabeledImage[] p = picsByDigit[d];
				p[(int)(((long)(Time.time()*3456))%p.length)].paint(g, 100, 100+28*d);
			}*/
			//setSize(cumulativePixels,cumulativePixels); //FIXME what if this causes slow ui event when not changing size?
		}catch(Exception e){
			throw new Error(e);
		}
	}
	
	/*
	public static final MnistLabeledImage[] pics = MnistOcrDataset.readTestLabeledImages();
	public static final MnistLabeledImage[][] picsByDigit;
	static{
		picsByDigit = new MnistLabeledImage[10][];
		for(int d=0; d<10; d++){
			int dd = d;
			picsByDigit[d] = Arrays.asList(pics).stream().filter((pic)->(pic.label==dd))
				.toArray(siz->new MnistLabeledImage[siz]);
		}
	}*/
	
	protected void paint(Graphics g, int edgeLayer, Point p, DoubleToIntFunction painter){
		AiGraphics.paintEdgeLayerOneNodePerPixel(varDisplayWhat.get().rbm.weight[edgeLayer], edgeLayerImage[edgeLayer], painter);
		g.drawImage(edgeLayerImage[edgeLayer], p.x, p.y, this);
	}

}
