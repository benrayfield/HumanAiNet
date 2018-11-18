/** Ben F Rayfield offers this software opensource MIT license */
package mutable.compilers;
import static mutable.util.Lg.*;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import immutable.util.Text;
import mutable.compilers.impl.JavassistCompiler;
import mutable.compilers.impl.LinuxJdkCompiler;
import mutable.compilers.impl.WindowsJdkCompiler;
import mutable.util.Lg;

public class JavaCompilers{
	
	/** If debuggable it may be much slower to compile and be OS specific and may not find one at all.
	Else default is javassist which does not put debug info in.
	See comment of JavaCompiler.pauseSoPersonCanPutBreakpoint() func for that param.
	*/
	public static JavaCompiler get(boolean debuggable, boolean pauseSoPersonCanPutBreakpoint){
		if(!debuggable && pauseSoPersonCanPutBreakpoint) throw new Error("Not debuggable but pauseSoPersonCanPutBreakpoint");
		return debuggable ? debuggableJavaCompiler(pauseSoPersonCanPutBreakpoint) : javassist;
		/*
		if(!debuggable) return javassist; //faster than JDK cuz doesnt use files
		Lg.todo("detect OS and choose "+WindowsJdkCompiler.class+" or "+LinuxJdkCompiler.class+" or maybe dont have any debuggable compiler, but for now use "+WindowsJdkCompiler.class);
		if(debuggableJavaCompiler == null){
			debuggableJavaCompiler = new WindowsJdkCompiler(); //FIXME what if fails to find it? Does it find java.exe in constructor or on first use of it?
		}
		return debuggableJavaCompiler;
		*/
	}
	
	/** is not debuggable as of Y2017 (java bytecode it generates does not contain "debug info") but could be modified to do that */
	static final JavaCompiler javassist = new JavassistCompiler();
	
	protected static JavaCompiler debuggableJavaCompilerThatPauses;
	protected static JavaCompiler debuggableJavaCompilerThatDoesntPausePause;
	protected static JavaCompiler debuggableJavaCompiler(boolean pauseSoPersonCanPutBreakpoint){
		if(pauseSoPersonCanPutBreakpoint){
			if(debuggableJavaCompilerThatPauses == null){
				//FIXME check for other OSes and implement Compiler for them too.
				//As of 2018-8-19 I'm building the windows compiler wrapper first
				//and eventually will add linux compiler wrapper,
				//probably around the same time I get lwjgl opencl set up on linux (its already working on windows).
				//I need linux for low lag jsoundcard gaming-low-lag interactive generated sound effects.
				debuggableJavaCompilerThatPauses = File.separator.equals("/")
					? new LinuxJdkCompiler(pauseSoPersonCanPutBreakpoint) : new WindowsJdkCompiler(pauseSoPersonCanPutBreakpoint);
			}
			return debuggableJavaCompilerThatPauses;
		}else{
			if(debuggableJavaCompilerThatDoesntPausePause == null){
				//FIXME similar to comment in the IF (instead of this ELSE).
				debuggableJavaCompilerThatDoesntPausePause = File.separator.equals("/")
					? new LinuxJdkCompiler(pauseSoPersonCanPutBreakpoint) : new WindowsJdkCompiler(pauseSoPersonCanPutBreakpoint);
			}
			return debuggableJavaCompilerThatDoesntPausePause;
		}
	}
	
	/** A line like "public class Xyz extends java.lang.Object implements java.util.function.Function, a.b.SomeInterface, c.d.AnotherInterface" */
	static String classLine(String... classParts){
		//FIXME make sure if there is a classLine, its found, even if @Annotations or // or /* comments are there.
		//Failing to find that could be a security hole that prevents seeing the right superclass, interfaces, etc?
		int i = 0;
		while(i<classParts.length){
			String c = classParts[i].trim();
			if(!(c.startsWith("package") || c.startsWith("import"))) break;
			i++;
		}
		return classParts[i];
	}
	
	/** Example: "package some.package;",
		"public class Xyz extends java.lang.Object implements java.util.function.Function, a.b.SomeInterface, c.d.AnotherInterface",
		"public Object apply(Object o){ return \"tostring=\"+o; }");
	*/
	public static String classPartsToJavaFileContent(String... classParts){
		/*
		StringBuilder sb = new StringBuilder();
		sb.append(classLine(classParts).trim());
		if(sb.charAt(sb.length()-1) != '{') sb.append('{');
		for(int i=1; i<classParts.length; i++){
			sb.append(Text.n+classParts[i]);
		}
		sb.append(Text.n+"}");
		return sb.toString();
		*/
		
		StringBuilder sb = new StringBuilder();
		for(String s : classParts){
			if(sb.length()>0) sb.append(Text.n);
			sb.append(s);
		}
		return sb.toString();
	}
	
	/** at least for now (until start using import statements), the classLine is classParts[0] */
	public static String className(String... classParts){
		String[] packageTokens = classParts[0].trim().split("\\s+|;");
		if(!packageTokens[0].equals("package")) throw new Error("No package keyword in "+Arrays.asList(packageTokens));
		String pkg = packageTokens[1];
		String[] tokens = tokenizeClassLine(classLine(classParts));
		if(!Arrays.asList(tokens).contains("class")) throw new Error("No class keyword in "+Arrays.asList(tokens));
		throwIfMoreThan1InList("class", tokens);
		throwIfMoreThan1InList("extends", tokens);
		throwIfMoreThan1InList("implements", tokens);
		return pkg+"."+tokens[Arrays.asList(tokens).indexOf("class")+1];
	}
	
	/** at least for now (until start using import statements), the classLine is classParts[0] */
	public static String superclassName(String... classParts){
		String[] tokens = tokenizeClassLine(classLine(classParts));
		if(!Arrays.asList(tokens).contains("extends")) throw new Error("No extends in "+Arrays.asList(classParts));
		throwIfMoreThan1InList("class", tokens);
		throwIfMoreThan1InList("extends", tokens);
		throwIfMoreThan1InList("implements", tokens);
		return tokens[Arrays.asList(tokens).indexOf("extends")+1];
	}
	
	public static String[] interfaceNames(String... classParts){
		String classLine = classLine(classParts);
		classLine = classLine.trim();
		if(classLine.endsWith("{")) classLine = classLine.substring(0,classLine.length()-1);
		classLine = classLine.trim();
		String s[] = classLine.split("(.*class\\s*)|(\\s*extends\\s*)|(\\s*implements\\s*)|(\\s*\\,\\s*)");
		throwIfMoreThan1InList("class", s);
		throwIfMoreThan1InList("extends", s);
		throwIfMoreThan1InList("implements", s);
		String[] interfaces = new String[s.length-3];
		System.arraycopy(s, 3, interfaces, 0, interfaces.length);
		return interfaces;
	}
	
	/** classLine may be at classParts[0], depending if theres (TODO) import lines */
	static String[] tokenizeClassLine(String classLine){
		classLine = classLine.trim();
		if(classLine.endsWith("{")) classLine = classLine.substring(0,classLine.length()-1);
		classLine = classLine.trim();
		return classLine.trim().split("\\s+");
	}
	
	static void throwIfMoreThan1InList(Object o, String... list){
		int count = 0;
		for(Object b : list) if(o.equals(list)) count++;
		if(count > 1) throw new Error(count+" of these (max 1 allowed): "+o);
	}
	
	public static void main(String[] args) throws InstantiationException, IllegalAccessException{
		String[] classParts = {
			"package generated;",
			"public class Xyz extends java.lang.Object implements java.util.function.Function{",
			"public Object apply(Object o){ return \"tostringTwice=\"+o+o; }",
			"}"};
		lg("className="+className(classParts));
		lg("superclassName="+superclassName(classParts));
		lg("interfaces="+Arrays.asList(interfaceNames(classParts)));
		//boolean debuggable = false; //FIXME test both true and false
		boolean debuggable = true; //FIXME test both true and false
		boolean pauseSoPersonCanPutBreakpoint = true;
		Class c = get(debuggable,pauseSoPersonCanPutBreakpoint).compile(classParts);
		Function f = (Function) c.newInstance();
		lg("testing debuggable compiler: "+f.apply("abc"));
	}
	
	public static ClassLoader getMainClassloader(){
		return JavaCompilers.class.getClassLoader();
	}

}
