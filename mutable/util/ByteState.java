/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;
import java.util.List;

import mutable.util.ui.obvar.ObvarEditor;

/** Normally constructor takes byte[] param, such as used by ObjectVars class. */
public interface ByteState{
	
	public byte[] state();
	
	/** Does state() always return same bytes (not necessarily the same array)?
	Can change from mutable to immutable, but cant change from immutable to mutable.
	*/
	public boolean isImmutableLocal();
	
	/** also includes the other obs reachable recursively by obvar name.
	isImmutableDeep objects must be named by secureHash (like sha256base64).
	If isImmutableDeep then isImmutableLocal.
	*/
	public boolean isImmutableDeep();
	
	/** Normally external code checks isModified() to choose if it should save later, so this normally does not save.
	Does nothing if isImmutableLocal.
	*/
	public void setModified(boolean m);
	
	public boolean isModified();
	
	/** Used by ObvarEditor. First in the list is used by default. */ 
	public Class<? extends ObvarEditor> defaultEditorClass();

}
