/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;
import static mutable.util.Lg.*;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import immutable.util.EscapeNameUtil;

/** one static map of short string to byte[] */
public class FileVars{
	
	public final File dir;
	
	public FileVars(File dir){
		this.dir = dir;
	}
	
	public byte[] get(String key){
		return Files.read(fileOf(key));
	}
	
	public void put(String key, byte[] valueOrNull){
		File f = fileOf(key);
		if(valueOrNull == null) Files.delete(f);
		else Files.write(valueOrNull, f);
	}
	
	/** make sure to close before other (write, append, etc) funcs on same key */
	public OutputStream appending(String key){
		return Files.appending(fileOf(key));
	}
	
	public String[] keys(){
		List<String> keys = new ArrayList();
		for(File f : dir.listFiles()){
			String n = f.getName();
			if(!".".equals(n) && !"..".equals(n)) keys.add(unescape(n));
		}
		Collections.sort(keys);
		return keys.toArray(new String[0]);
	}
	
	public File fileOf(String key){
		return new File(dir,escape(key));
	}
	
	/** throws if escaped form is too long */
	public static String escape(String name){
		return EscapeNameUtil.escapeName(name);
	}
	
	public static String unescape(String escaped){
		return EscapeNameUtil.unescapeName(escaped);
	}

}