/** Ben F Rayfield offers this software opensource MIT license */
package mutable.compilers.impl;
import mutable.compilers.JavaCompiler;
import mutable.compilers.JavaCompilers;
import mutable.util.ui.Ask;
import mutable.util.Cmd;
import mutable.util.Dependencies;
import mutable.util.Files;

import static mutable.util.Lg.*;

import java.io.File;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;

import immutable.util.BellMath;
import immutable.util.Text;

public class WindowsJdkCompiler implements JavaCompiler{
	
	//TODO dont find jdk twice. Share that between 2 instances of WindowsJdkCompiler
	//which differ by contstructor param. But for now I'm testing can a runtime generated class
	//be debugged in eclipse in same runtime, with popup to pause and wait for person to put breakpoint in it,
	//then when click button to close popup, continue using the class.
	
	protected static String probableJavaFolderSubstrings[] = { //lowercase only
		"jdk", "jav", "j2", "prog", "sun", "orac", "compil"};
	
	protected static String improbableJavaFolderSubstrings[] = { //lowercase only
		"jre", "old", "window", "temp", "my", "back", "sys"};
	
	protected static String javacExeFileName = "JAVAC.EXE"; //must be all capital
	protected static String jarExeFileName = "JAR.EXE"; //must be all capital
	protected static String javaExeFileName = "JAVA.EXE"; //must be all capital
	
	protected static File javacExe;
	
	public static final DirClassLoader dirClassLoader = new DirClassLoader(
		Files.dirWhereThisProgramStarted, JavaCompilers.getMainClassloader());
	
	public final boolean pauseSoPersonCanPutBreakpoint;
	
	public WindowsJdkCompiler(boolean pauseSoPersonCanPutBreakpoint){
		this.pauseSoPersonCanPutBreakpoint = pauseSoPersonCanPutBreakpoint;
	}
	
	public boolean pausesSoPersonCanPutBreakpoint(){ return pauseSoPersonCanPutBreakpoint; }

	public String lang(){ return "java"; }

	public boolean debuggable(){ return true; }

	public File compilerExecutable(){
		throw new Error("TODO");
	}

	public File classpathTo(){
		lgErr("If this program started as a jar file (doubleclick to run), then the classpath is that jar, else its forexample src dir in eclipse or netbeans, but for now just return the latter: Files.dirWhereThisProgramStarted="+Files.dirWhereThisProgramStarted);
		return Files.dirWhereThisProgramStarted;
	}
	
	public String classpathString(){
		StringBuilder sb = new StringBuilder();
		sb.append(classpathTo());
		File tempLibDir = new File(Files.acycDir,"tempLib");
		for(File f : tempLibDir.listFiles()){
			if(f.getAbsolutePath().endsWith(".jar")){
				sb.append(File.pathSeparatorChar+f.getAbsolutePath());
			}
		}
		return sb.toString();
	}

	public Class compile(String... classParts){
		/*String s = findCompilerOnHardDrive(10000)+"bin/"+javacExeFileName;
		lgErr("found compiler?: "+s);
		lgErr("Getting version from it... "+Cmd.eval(s+" -version"));
		throw new Error("TODO");
		*/
		
		String className = JavaCompilers.className(classParts); //includes package
		File cp = classpathTo();
		lg("classpathDirTo="+cp);
		//FIXME include classpath (such as src dir in eclipse or netbeans) in javac call,
		//but for now just hardcode one
		String javaFileContent = JavaCompilers.classPartsToJavaFileContent(classParts);
		//Generated classes should be named by hash of their code string,
		//so there should never be a disagreement on which generate class name has which behaviors,
		//so if theres already such a class, verify it has the expected content else throw,
		//and if there is not such a class, create the file. Either way return class from that file,
		//which may already be in memory or be compiled by the JDK etc found.
		byte[] javaFileContentBytes = Text.stringToBytes(javaFileContent);
		File javaFile = cp;
		String[] pathParts = className.split("\\.");
		for(int i=0; i<pathParts.length; i++){
			String pathPart = i==pathParts.length-1 ? pathParts[i]+".java" : pathParts[i];
			javaFile = new File(javaFile,pathPart);
		}
		String jfPath = javaFile.getAbsolutePath();
		int i = jfPath.lastIndexOf('.');
		File classFile = new File(jfPath.substring(0,i)+".class");
		lg("javaFileContent="+javaFileContent);
		lg("javaFile="+javaFile);
		lg("classFile="+classFile);
		if(classFile.exists()){
			if(javaFile.exists()){ //both exist
				String javaFileContentExisting = Text.bytesToString(Files.read(javaFile));
				if(!javaFileContentExisting.equals(javaFileContent)) throw new Error(
					"Different javaFileContent in param vs file="+javaFile); //for security, though didnt compile again and check that against class file
				lg(".java and .class files exist. Trying to load classFile="+classFile);
				//read classFile below
			}else{ //only .class exists
				throw new Error("class file exists but not java file. classFile="+classFile+" javaFile="+javaFile);
			}
		}else{
			if(javaFile.exists()){ //only .java exists
				throw new Error("java file exists but not class file. classFile="+classFile+" javaFile="+javaFile);
			}else{ //neither exist
				lg("Neither .java or .class file exists: "+javaFile+" "+classFile
					+" Will look for java compiler then save to .java file and compile it.");
				File javac = javacExe();
				lg("javac="+javac);
				Files.write(javaFileContentBytes, javaFile);
				String compileCmd = javac.getAbsolutePath()+" -nowarn -cp "+classpathString()+" "+javaFile;
				String out = Cmd.eval(compileCmd);
				String outTrimmed = out.trim();
				//Example "Note:" in javac.exe output:
				//Note: C:\g\q27x\eclw\2\paint_and_mmgMouseai_2018-7plus\src\rbm\func\LearnLoop.java uses unchecked or unsafe operations.
				//Note: Recompile with -Xlint:unchecked for details.
				if(!"".equals(outTrimmed) && !outTrimmed.startsWith("Note:")) throw new Error(
					"Compile error="+out+" for code["+javaFileContent+"]");
				if(!classFile.exists()) throw new Error("Compiling["+compileCmd+"] Didnt create class file: "+classFile);
				//read classFile below
			}
			lg("Compiled? Trying to load classFile="+classFile);
		}
		
		/*FIXME I want 2 instances of each jdk wrapper to share a finding of jdk, cuz only the pauseSoPersonCanPutBreakpoint differs.
		Share a WindowsJavaCompilerFinder between them.
		FIXME if pauseSoPersonCanPutBreakpoint then when should it popup with a button to copy (to clipboard) the class name and wait for person to put breakpoint in IDE?
		*/
		
		if(pausesSoPersonCanPutBreakpoint()){
			Ask.ok("You can put breakpoint, about to compile "+className);
		}
		try{
			//FIXME display classpath. find why its not loading the generated/Xyz.class file. Maybe I need to create custom ClassLoader?
			//lgErr("classloader is a "+JavaCompilers.getMainClassloader().getClass());
			//return JavaCompilers.getMainClassloader().loadClass(className); //FIXME does this always load from classFile?
			return dirClassLoader.loadClass(className); //loads from classFile if not already in memory
		}catch(ClassNotFoundException e){ throw new Error(e); }
		
		/*
		TODO if file exists and differs in content, throw.getClass()
		
		TODO put breakpoint in the generated class, while the program is running, but those 2 .java and .class files didnt exist when program started.
		
		//lgErr("TODO "+c+" "+Arrays.asList(classParts)+" TODO use classPartsToJavaFileContent");
		throw new Error("TODO");
		*/
	}
	
	/** the .java file may exist or not. Example packageAndClass: "generated.Xyz" */
	public File javaFileForPackageClass(String packageAndClass){
		return new File(classpathTo(),packageAndClass.replace('.','/'));
	}
	
	/** finds it on harddrive (TODO else throws) */
	public File javacExe(){
		if(javacExe == null){
			String s = findCompilerOnHardDrive(10000)+"bin/"+javacExeFileName;
			javacExe = new File(s);
		}
		return javacExe;
		
	}
	
	/** a good value for maxFoldersToSearch is 10000 */
	public static String findCompilerOnHardDrive(int maxFoldersToSearch){
		File systemRoots[] = File.listRoots();
		int foldersSearched = 0;
		SortedSet<StringAndDouble> folderPathNamesToBeSearched = new TreeSet();
		for(int r=0; r<systemRoots.length; r++){
			String aRootPath = systemRoots[r].getAbsolutePath();
			if(/*!WindowsOS.startsWithBannedFolderPrefix(aRootPath.toLowerCase())*/true){
				String rootChildNames[] = systemRoots[r].list();
				if(rootChildNames==null){
					System.out.println(WindowsJdkCompiler.class
						+" ignoring null subfolders of "+systemRoots[r]);
					continue;
				}
				String rootPrefix = Files.changeBackToForwardSlashesAndAddSlashAtEndIfFolder(
					systemRoots[r].getAbsolutePath().toLowerCase());
				for(int c=0; c<rootChildNames.length; c++){
					//go 1 level deeper than necessary to avoid ruling out whole drive letters
					String s = rootPrefix+rootChildNames[c];
					if(!Files.isFolderPathName(s)) continue;
					double d = chanceFolderRecursivelyContainsJavaCompiler(s);
					System.out.println("adding initial folder to be searched: "+s);
					folderPathNamesToBeSearched.add(new StringAndDouble(s,d));
				}
			}
		}
		while(foldersSearched++ < maxFoldersToSearch && folderPathNamesToBeSearched.size()>0){
			StringAndDouble sd = folderPathNamesToBeSearched.first();
			folderPathNamesToBeSearched.remove(sd);
			String possibleCompilerFolderPathName = sd.s;
			System.out.println("Searching folder: "+possibleCompilerFolderPathName);
			if(isCompilerFolderNameAndAtLeastCSVersion(possibleCompilerFolderPathName)){
				System.out.println("FOUND COMPILER folder="+possibleCompilerFolderPathName);
				return possibleCompilerFolderPathName;
			}
			File possibleCompilerFolder = new File(possibleCompilerFolderPathName);
			String folderPrefix = Files.changeBackToForwardSlashesAndAddSlashAtEndIfFolder(
				possibleCompilerFolder.getAbsolutePath().toLowerCase());
			String childNames[] = possibleCompilerFolder.list();
			for(int c=0; c<childNames.length; c++){
				String s = Files.changeBackToForwardSlashesAndAddSlashAtEndIfFolder(
					folderPrefix+childNames[c]);
				if(!Files.isFolderPathName(s)) continue;
				double d = chanceFolderRecursivelyContainsJavaCompiler(s);
				System.out.println("chance="+d+"  folder="+s);
				folderPathNamesToBeSearched.add(new StringAndDouble(s,d));
			}
		}
		throw new RuntimeException("Searched "+foldersSearched
			+" folders but could not find a java compiler (assuming Windows OS).");
	}
	
	/** Example: "C:/program files/abcsunxyz/" should return a high chance.
	Does not test if it is a java compiler folder. Only predicts itself and children recursively.
	*/
	public static double chanceFolderRecursivelyContainsJavaCompiler(String folderName){
		folderName = folderName.toLowerCase();
		double aveFolderNameLength = 30, stdDevFolderNameLength = 15;
		double percentileFolderNameLength = gaussianToPercentileFraction(
			(folderName.length()-aveFolderNameLength)/stdDevFolderNameLength ); //range 0 - 1
		String good[] = probableJavaFolderSubstrings, bad[] = improbableJavaFolderSubstrings;
		int countGood = 0, countBad = 0;
		for(int i=0; i<good.length; i++) if(folderName.contains(good[i])) countGood++;
		for(int i=0; i<bad.length; i++) if(folderName.contains(bad[i])) countBad++;
		double chance;
		if(countGood >= 2){
			if(countBad>=2) chance = .7;
			else if(countBad==1) chance = .8;
			else chance = .9;
		}else if(countGood == 1){
			if(countBad>=2) chance = .4;
			else if(countBad==1) chance = .5;
			else chance = .6;
		}else{ //no good
			if(countBad>=2) chance = .1;
			else if(countBad==1) chance = .2;
			else chance = .3;
		}
		double chanceByLengthOnly = Math.pow(1-percentileFolderNameLength, .7); //should this exponent be less or more than 1.0?
		return chance*.6 + .4*chanceByLengthOnly;
	}
	
	static class StringAndDouble implements Comparable{
		public String s;
		public double d;
		public StringAndDouble(String s, double d){ this.s=s; this.d=d; }
		public int compareTo(Object obj){
			try{
				double objd = ((StringAndDouble)obj).d;
				if(objd==d) return 0;
				return objd<d ? -1 : 1;
			}catch(ClassCastException e){ return 0; }
		}
	}
	
	/** is it a java compiler folder and a version that can compile codesimian? */
	public static boolean isCompilerFolderNameAndAtLeastCSVersion(String folderName){
		File compilerFolder = new File(folderName);
		String minVersion = Dependencies.minJavaVersionForThisSoftware;
		return isJavaCompilerAndAtLeastVersion(compilerFolder,minVersion);
	}
	
	static double gaussianToPercentileFraction(double gaussian){
		return .5+.5*BellMath.stdDevToBifraction(gaussian);
	}
	
	static double percentileFractionToGaussianFraction(double percentileFraction){
		return BellMath.bifractionToStdDev(percentileFraction*2-1);
	}
	
	public static boolean isJavaCompilerAndAtLeastVersion(File compilerFolder, String minVersion){
		String ver = compilerVersionOrNullIfNotCompiler(compilerFolder);
		if(ver==null) return false;
		return 0 <= versionStrCompare(minVersion,ver);
	}
	
	/** java compiler for Windows OS only */
	public static String compilerVersionOrNullIfNotCompiler(File folder){
		try{
			File binFolder = new File(folder,"bin");
			if(!binFolder.exists()) return null;
			File javacExe = new File(binFolder,javacExeFileName);
			if(!javacExe.exists()) return null;
			File jarExe = new File(binFolder,jarExeFileName);
			if(!jarExe.exists()) return null;
			File javaExe = new File(binFolder,javaExeFileName);
			if(!javaExe.exists()) return null;
			File tempOutputFile = new File(folder, "javaVersion.txt");
			String outerBatText = javaExe.getAbsolutePath()+" -version 2> "+tempOutputFile.getAbsolutePath();
			File outerBatFile = new File(folder, "createVersionFile.bat");
			Files.write(outerBatText.getBytes(), outerBatFile);
			Cmd.eval(outerBatFile.getAbsolutePath());
			String versionOutput = new String(Files.read(tempOutputFile));
			return javaVersionSubstringOrNull(versionOutput);
		}catch(Exception e){
			//if(Ask.exec("IOException: "+e+" Should I throw it as a RuntimeException?"))
				throw new RuntimeException(e);
		}
	}
	
	/** returns 1 if verA<verB, returns 0 if equal, returns -1 if verA>verB */
	public static int versionStrCompare(String verA, String verB){
		int a[] = versionStringToIntsOrNull(verA);
		int b[] = versionStringToIntsOrNull(verB);
		if(a==null){
			return b==null ? 0 : 1;
		}else{
			if(b==null){
				return -1;
			}else{
				int shortest = Math.min(a.length,b.length);
				for(int i=0; i<shortest; i++){
					if(a[i]<b[i]) return 1;
					if(a[i]>b[i]) return -1;
				}
				if(shortest<a.length) return -1;
				if(shortest<b.length) return 1;
				return 0;
			}
		}
	}
	
	/** returns something like "1.5.0" or null if cant find a version in the string.
	TODO: use natural language neural net to find which numbers are most likely to be a java version.
	*/
	public static String javaVersionSubstringOrNull(String containsJavaVersion){
		int minVerLen = "1.5".length();
		int maxVerLen = "12.34.5678".length();
		for(int start=0; start<containsJavaVersion.length()-minVerLen; start++){ //get first version string
			for(int i=maxVerLen; i>=minVerLen; i--){ //loop backward to get longest string
				if(containsJavaVersion.length() < start+i) continue;
				String possibleVersionString = containsJavaVersion.substring(start,start+i);
				int ints[] = versionStringToIntsOrNull(possibleVersionString);
				if(ints!=null) return possibleVersionString;
			}
		}
		return null;
	}
	
	/** returns new int[]{1,5,0} for "1.5.0" or null if its not a version string */
	public static int[] versionStringToIntsOrNull(String s){
		char c[] = s.toCharArray();
		String betweenDots[] = s.split("\\.");
		int v[] = new int[betweenDots.length];
		if(betweenDots.length < 2) return null;
		for(int i=0; i<betweenDots.length; i++) try{
			v[i] = Integer.parseInt(betweenDots[i]);
		}catch(NumberFormatException e){ return null; }
		return v;
	}

}