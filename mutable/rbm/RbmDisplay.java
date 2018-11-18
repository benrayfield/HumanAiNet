/** Ben F Rayfield offers this software opensource MIT license */
package mutable.rbm;

import immutable.learnloop.RBM;
import mutable.rbm.ui.LearningVec_OLD;

public class RbmDisplay{
	
	public final RBM rbm;
	
	public final LearningVec_OLD lv;
	
	/** snapshot of lv.cycles, to avoid repainting if the same LearningVec is displayed and hasnt changed */
	public final int cycles;
	
	public RbmDisplay(RBM rbm, LearningVec_OLD lv){
		this.rbm = rbm;
		this.lv = lv;
		this.cycles = lv.cycles;
	}
	
	public boolean equals(Object o){
		if(!(o instanceof RbmDisplay)) return false;
		RbmDisplay r = (RbmDisplay)o;
		return rbm==r.rbm && lv==r.lv && cycles==r.cycles;
	}
	
	public int hashCode(){
		return rbm.hashCode()+lv.hashCode()+cycles;
	}

}
