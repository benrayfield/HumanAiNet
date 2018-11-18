/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util.inputqueue;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

import mutable.games.mmgMouseAI.mouseRecorder.ui.MouseYRecorderUi;
//import recurty.old.*;
import mutable.util.ByteState;
import mutable.util.LinearInterpolate1Var;
import immutable.util.MathUtil;
import mutable.util.ui.obvar.ObvarEditor;
import mutable.util.ui.obvar.ObvarEditorWrapper;

/** Appends only, never removes old data. Fills unused part of LinearInterpolate1Var with Double.MAX_VALUE
whenever replaces it with double size array.
I plan to use this for recorded mouse movements, though it is more generally any 1d sparse time recording.
Stored in a file, it will alternate time value time value, like float[cycle][2] instead of
in LinearInterpolate1Var its actually 2 float[cycle].
While I only need float for mouse movements, I will store doubles since time needs double
and LinearInterpolate1Var already uses doubles for time and value, and to be more general for other uses.
*/
public class UnlimitedInputQueue /*extends AbstractRecurty*/ implements ByteState, InputQueue{
	
	/** You may want to setCapacity(size()) first if storing on harddrive */
	public byte[] state(){
		return data.state();
	}
	
	public UnlimitedInputQueue(byte[] state){
		data = new LinearInterpolate1Var(state);
		dataSize = data.in.length;
		while(dataSize != 0 && data.in[dataSize-1]==Double.MAX_VALUE) dataSize--;
	}
	
	public int size(){ return dataSize; }
	
	public int capacity(){ return data.in.length; }
	
	/** capacity is auto enlarged when add past capacity. You can also set this to smaller to save memory */
	public void setCapacity(int newSize){
		if(newSize < dataSize) throw new Error("newSize=="+newSize+" < dataSize="+dataSize+" TODO maybe thats how old data should be removed?");
		double[] newTimes =  new double[newSize];
		double[] newValues = new double[newSize];
		int copySize = Math.min(dataSize, data.in.length);
		System.arraycopy(data.in, 0, newTimes, 0, copySize);
		System.arraycopy(data.out, 0, newValues, 0, copySize);
		Arrays.fill(newTimes,copySize,newSize,Double.MAX_VALUE);
		Arrays.fill(newValues,copySize,newSize,0);
		data = new LinearInterpolate1Var(newTimes, newValues);
		setModified(true);
	}
	
	public double timeSize(){ return data.in[dataSize-1]; }
	
	/** first dataSize indexs of data.inputNumbers and data.outputNumbers are used. The rest are Double.MAX_VALUE */
	protected int dataSize;
	protected LinearInterpolate1Var data;
	
	public UnlimitedInputQueue(){
		dataSize = 0;
		data = new LinearInterpolate1Var(new double[]{0}, new double[]{Double.MAX_VALUE});
	}
	
	public void add(double dt, double value){
		if(dt <= 0) throw new Error("dt="+dt);
		setModified(true);
		if(dataSize == capacity()) setCapacity((int)(capacity()*1.5+1));
		data.in[dataSize] = data.in[dataSize-1]+dt; //replace a Double.MAX_VALUE
		data.out[dataSize] = value;
		dataSize++;
		setModified(true);
	}

	public double applyAsDouble(double dt){
		return data.interpolate(-dt+timeSize()); //FIXME is this backward? 2 ways it could be backward.
	}

	public void setModified(boolean m){
		data.setModified(m);
	}

	public boolean isModified(){
		return data.isModified();
	}
	
	public Class<? extends ObvarEditor> defaultEditorClass(){
		//I've been using CommandlineMouseYRecorder (which has no editor ui, just a recorder and display on commandline) instead of:
		return MouseYRecorderUi.class; //TODO
	}

	public boolean isImmutableLocal(){
		return false;
	}

	public boolean isImmutableDeep(){
		return false;
	}

}