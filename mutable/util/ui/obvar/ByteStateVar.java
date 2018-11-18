/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util.ui.obvar;
import mutable.util.ByteState;

/** mutable ByteState wraps replacable (normally)immutable ByteState.
TODO implement Var<? extends ByteState>? TODO RbmEditor should use this since RBM (TODO implement ByteState) is immutable
and RbmEditor repaints when Var<RBM> event. Should I keep Var?
*/
public class ByteStateVar implements ByteState{
	
	protected ByteState value;
	
	protected boolean modified;
	
	public ByteStateVar(ByteState firstValue){
		if(!firstValue.isImmutableDeep()) throw new Error("Must be isImmutableDeep: "+firstValue);
		value = firstValue; //dont setModified
	}
	
	public ByteState get(){ return value; }
	
	public void set(ByteState value){
		if(!value.isImmutableDeep()) throw new Error("Must be isImmutableDeep: "+value);
		this.value = value;
		setModified(true);
	}
	
	public byte[] state(){
		return value.state();
	}

	public void setModified(boolean m){
		modified = m;
	}

	public boolean isModified(){
		return modified;
	}

	public Class<? extends ObvarEditor> defaultEditorClass(){
		return value.defaultEditorClass();
	}

	public boolean isMutable(){
		return true;
	}

	public boolean isImmutableLocal(){
		return false;
	}

	public boolean isImmutableDeep(){
		return false;
	}

}
