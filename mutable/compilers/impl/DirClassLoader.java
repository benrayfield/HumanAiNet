/** Ben F Rayfield offers this software opensource MIT license */
package mutable.compilers.impl;

import java.io.File;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

import mutable.util.Files;

public class DirClassLoader extends ClassLoader{
	
	public final File dir;
	
	public final ClassLoader parent;
	
	protected final Map<String,Class> classesCreated = new HashMap();
	
	public DirClassLoader(File dir, ClassLoader parent){
		this.dir = dir;
		this.parent = parent;
	}

	public synchronized Class findClass(String name){
		Class c = classesCreated.get(name);
		if(c != null) return c;
		try{
			return parent.loadClass(name);
		}catch(ClassNotFoundException e){
			File f = classFileFromClassName(name);
			byte[] bytecode = Files.read(f);
			c = defineClass(name, bytecode, 0, bytecode.length);
			classesCreated.put(name, c);
			return c;
		}
	}
	
	public File classFileFromClassName(String className){
		String[] tokens = className.trim().split("\\.");
		File f = this.dir;
		for(int i=0; i<tokens.length; i++){
			f = new File(f, i==tokens.length-1 ? tokens[i]+".class" : tokens[i]);
		}
		return f;
	}

}
