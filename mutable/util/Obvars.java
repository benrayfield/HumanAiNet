/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;
import java.io.File;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import immutable.util.Text;
//import lisp.todoMakeSureTheDotlispFilesGetLoadedInJarTooLikeDidInHumanainet063.TODO;

/** Object Variables. Can be used with ObvarEditor or programmatically. Built on FileVars.
These are self contained objects that each have a byte[] state() func
and constructor of byte[] param, not a forest of Objects like in ufnode or binacyc.
*/
public class Obvars{
	private Obvars(){}
	
	
	//TODO a mutable object is a var. All vars have only 1 possible value type: jsonThenEmptyLineThenBinaryData,
	//but that value is often not observed directly.
	
	
	
	
	/*TODO "lzh$randomvarnamedsfasdf" (lazyevalhash) will go in the json and be replaced when hashed.
	
	key "sz" is optional and will be size of the binary section in bits.
	
	
	
	jsonThenEmptyLineThenBinaryData QUOTE 
	FIXME since immutable ByteState can only be named by secureHash,
	how should we do the name RBM (which isImmutableDeep) in ObvarEditor since it would be inefficient
	to hash every RBM displayed and instead only want to save some of them to harddrive,
	usually just paint them and soon let them be garbcoled?
	If ObvarEditor can display a ByteState without a name (and get name later if needed or never)...
	
	TODO I want RBM stored as json where every string starts with sometype: like g: means string,
	and sha256b64: means sha256 as my preferred base64 chars, so RBM can be stored as json
	with efficient binary, and that means pointers can be in a standard way tracked in the obvars on harddrive.
	I imagine storing each float[][] (weights of an edlay, array of vectors) in its own file
	prefixed by int number of dims and that many ints for dim sizes, then the floats,
	which only works for hyperrects (such as any 1d float[], rectangle float[][], etc),
	and its class would be a wrapper of float[][] as ByteState, so actually need such a class for each number of dims.
	...
	I'm worried that the immutable types could be vulnerable to replacing their class name which is stored separately,
	so maybe the data should always start with package.xyz.ClassName: then the binary data
	which would make it harder to read or change the classname if I happen to refactor it
	but I think its a needed change for that security,
	also could cause problems with the hash being derived from classname so actually could not change it,
	so maybe it should start with arbitraryName: then the data, and have a map from arbitraryNames to classNames
	or maybe allow both. Maybe each file should start with json then have a binary section
	after \n__DATA__\n (what was that string how perl does it?),
	or maybe it should be after the first (any of these evaled): \r\n\r\n OR \n\n OR \r\r
	so data starts after the first empty line, which would not occur in json, so json then empty line then binary data.
	Juse use \r\n\r\n as the prefix of the raw data like in the http datastruct (tested in occamserver).
	Yes, do that.
	...
	In this jsonThenEmptyLineThenBinaryData datastruct (which will normally be stored in files and the binary part goes in ByteState obs),
	I will put float[][] in binary section and list of dim sizes (2 in that case),
	and for other types (such as RBM) they have more metadata such as pointers at
	other jsonThenEmptyLineThenBinaryData by sha256b64$the...hash.
	Use "type$somethingofthattype" instead of "type:somethingofthattype" cuz $ is one of my base64 chars and textedits better.
	UNQUOTE.
	*/
	
	public static void delete(String key){
		fileVarsClass.put(key, null);
		fileVarsContent.put(key, null);
	}
	
	public static void put(String key, ByteState value){
		if(value == null) throw new Error("Use delete func instead. This is to avoid accidental deletions by null errors.");
		fileVarsClass.put(key, Text.stringToBytes(value.getClass().getName()));
		fileVarsContent.put(key, value.state());
	}
	
	private static final Map<String,ByteState> liveObs = Collections.synchronizedMap(new WeakHashMap<String,ByteState>());
	
	/** null if not exist. Returns same ByteState as last time if WeakHashMap keeps it (if its reachable outside this class then keeps, else may garbcol).
	The same ByteState can be the value of any number of keys but will be multiple ByteStates if loaded after garbcol,
	so a ByteState cant know "its key".
	*/
	public static ByteState get(String key){
		ByteState ob = liveObs.get(key);
		if(ob != null) return ob;
		try{
			byte[] classNameBytes = fileVarsClass.get(key);
			if(classNameBytes == null) return null;
			String className = Text.bytesToString(classNameBytes);
			Class cl = Class.forName(className);
			if(!ByteState.class.isAssignableFrom(cl)) throw new Error(cl+" !isAssignableFrom "+ByteState.class);
			Constructor ct = cl.getConstructor(byte[].class);
			byte[] content = fileVarsContent.get(key);
			if(content == null) throw new Error("className exists but not content for key="+key);
			ob = (ByteState) ct.newInstance(content);
			liveObs.put(key, ob);
			return ob;
		}catch(Exception e){ throw new Error(e); }
	}
	
	/** returns an OutputStream marked as type of java.lang.String in the fileVarsClass dir.
	Make sure not to call other funcs on that key (write, append, etc) until close it.
	*/
	public static OutputStream appendingUtf8(String key){
		fileVarsClass.put(key, Text.stringToBytes(String.class.getName()));
		return fileVarsContent.appending(key);
	}
	
	public static String[] keys(){
		return fileVarsClass.keys();
	}
	
	static final File objectVarsDir = new File(Files.acycDir,"objectVars");
	
	/** values are the byte[] that goes in object constructor */
	public static final FileVars fileVarsContent = new FileVars(new File(objectVarsDir,"content"));
	
	/** values are java class name, like "util.inputqueue.UnlimitedInputQueue".
	Be careful when moving/renaming classes but not this data. You could change the class and it would work again.
	*/
	public static final FileVars fileVarsClass = new FileVars(new File(objectVarsDir,"class"));

}
