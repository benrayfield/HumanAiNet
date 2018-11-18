/** Ben F Rayfield offers this software opensource MIT license */
package mutable.compilers.impl;
import static mutable.util.Lg.lg;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.Modifier;
import mutable.compilers.JavaCompiler;
import mutable.compilers.JavaCompilers;

public class JavassistCompiler implements JavaCompiler{
	
	protected static ClassPool classPool;
	
	public String lang(){ return "java"; }

	public boolean debuggable(){ return false; }
	
	public boolean pausesSoPersonCanPutBreakpoint(){ return false; }

	public File compilerExecutable() {
		throw new Error("TODO");
	}

	public File classpathTo(){
		throw new Error("TODO");
	}

	public Class compile(String... classParts){
		String className = JavaCompilers.className(classParts);
		String superclassName = JavaCompilers.superclassName(classParts);
		String[] interfaceNames = JavaCompilers.interfaceNames(classParts);
		List<String> classNames = new ArrayList();
		classNames.add(className);
		classNames.add(superclassName);
		classNames.addAll(Arrays.asList(interfaceNames));
		String regExpName = "[a-zA-Z_][a-zA-Z0-9_]{0,100}";
		String regExpClassLongName = regExpName+"(\\."+regExpName+"){0,30}";
		for(String cn : classNames){
			if(!cn.matches(regExpClassLongName)) throw new Error(
				cn+" is not a class name. regExpClassLongName="+regExpClassLongName);
		}
		try{
			lg("getting CtClass "+superclassName);
			CtClass superClass = classPool().get(superclassName);
			lg("creating CtClass "+className);
			CtClass newCtClass = classPool().makeClass(className, superClass);
			lg("New CtClass "+newCtClass+" frozen="+newCtClass.isFrozen());
			for(String interfaceName : interfaceNames){
				lg("getting interface CtClass: "+interfaceName);
				CtClass ctInterface = classPool().get(interfaceName);
				lg("adding interface to the new class");
				newCtClass.addInterface(ctInterface);
				lg("added interface");
			}
			//for(int i=1; i<classParts.length; i++){ //if theres no package
			for(int i=0; i<classParts.length-1; i++){
				//TODO check if startsWith "package", "class",
				String javaFuncCode = classParts[i];
				if(!javaFuncCode.startsWith("package") & !javaFuncCode.startsWith("public class") && !javaFuncCode.startsWith("class") && !javaFuncCode.startsWith("}")){
					lg("javaFuncCode = "+javaFuncCode);
					String vaPrefix = "javassistCompileAsVarargs:";
					CtMethod func;
					if(javaFuncCode.startsWith(vaPrefix)){
						func = CtMethod.make(javaFuncCode.substring(vaPrefix.length()), newCtClass);
						func.setModifiers(func.getModifiers() | Modifier.VARARGS);
					}else{
						func = CtMethod.make(javaFuncCode, newCtClass);
					}
					newCtClass.addMethod(func);
				}
			}
			lg("Creating class from CtClass: "+newCtClass);
			Class newClass = newCtClass.toClass(getMainClassLoader(), null);
			lg("Created new class: "+newClass.getName()+" but have not verified it can be instantiated.");
			lg("Testing new Class object: "+newClass);
			Constructor constructors[] = newClass.getConstructors();
			for(int con=0; con<constructors.length; con++){
				lg("Constructor "+con+" is "+constructors[con]);
			}
			lg("Basic testing of "+newClass+" passes.");
			return newClass;
		}catch(Exception e){
			throw new Error(e);
		}
	}
	
	public static synchronized ClassPool classPool(){
		if(classPool == null){
			lg("Getting "+ClassPool.class.getName()+" for the first time.");
			classPool = ClassPool.getDefault(); //Javassist
			lg("It is "+classPool);
		}
		return classPool;
	}
	
	private static ClassLoader getMainClassLoader(){
		return JavassistCompiler.class.getClassLoader();
	}
	
	/*public static synchronized Class newClass(String... classParts){
		try{
		//TODO for each String, check if its a class name, field, function, static block, etc.
		if(classParts.length < 2) throw new Exception(
			"classParts.length ("+classParts.length+") < 2");
		//TODO allow "{" in classParts[0], which means to start the class body
		String regExpName = "[a-zA-Z_][a-zA-Z0-9_]{0,100}";
		String regExpClassLongName = regExpName+"(\\."+regExpName+"){0,30}";
		//public class a.b.c extends e.f implements g.h, i.j.k, l.m
		String regExp0 = "(.*class\\s*)|(\\s*extends\\s*)|(\\s*implements\\s*)|(\\s*\\,\\s*)";
		String s[] = classParts[0].split(regExp0);
		if(s.length < 3) throw new Exception(
			"class name should be at index 1, and extends at index 2, but array size is "+s.length);
		for(int i=1; i<s.length; i++){
			if(!s[i].matches(regExpClassLongName)) throw new Exception(
				s[i]+" is not a class name. regExpClassLongName="+regExpClassLongName);
		}
		lg("getting CtClass "+s[2]);
		CtClass superClass = classPool().get(s[2]);
		lg("creating CtClass "+s[1]);
		CtClass newCtClass = classPool().makeClass(s[1], superClass);
		lg("New CtClass "+newCtClass+" frozen="+newCtClass.isFrozen());
		for(int i=3; i<s.length; i++){
			lg("getting interface CtClass: "+s[i]);
			CtClass ctInterface = classPool().get(s[i]);
			lg("adding interface to the new class");
			newCtClass.addInterface(ctInterface);
			lg("added interface");
		}
		for(int i=1; i<classParts.length; i++){
			String javaFuncCode = classParts[i];
			lg("javaFuncCode = "+javaFuncCode);
			String vaPrefix = "javassistCompileAsVarargs:";
			CtMethod func;
			if(javaFuncCode.startsWith(vaPrefix)){
				func = CtMethod.make(javaFuncCode.substring(vaPrefix.length()), newCtClass);
				func.setModifiers(func.getModifiers() | Modifier.VARARGS);
			}else{
				func = CtMethod.make(javaFuncCode, newCtClass);
			}
			newCtClass.addMethod(func);
		}
		lg("Creating class from CtClass: "+newCtClass);
		Class newClass = newCtClass.toClass(getMainClassLoader(), null);
		lg("Created new class: "+newClass.getName()+" but have not verified it can be instantiated.");
		try{
			lg("Testing new Class object: "+newClass);
			Constructor constructors[] = newClass.getConstructors();
			for(int con=0; con<constructors.length; con++){
				lg("Constructor "+con+" is "+constructors[con]);
			}
			lg("Basic testing of "+newClass+" passes.");
		}catch(Exception e){
			lg("Tests of "+newClass+" failed.");
			throw e;
		}
		return newClass;
		}catch(Exception e){ throw new RuntimeException(e); }
	}*/

}