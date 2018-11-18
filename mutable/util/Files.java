/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;
import static mutable.util.Lg.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import mutable.occamserver.Occamserver;

public class Files{
	private Files(){}

	/** null if not exist */
	public static byte[] read(File file){
		if(!file.exists()) return null;
		try{
			if(file.isDirectory()) return new byte[0];
			InputStream fileIn = null;
			try{
				fileIn = new FileInputStream(file);
				long byteSize = file.length();
				if(byteSize > Integer.MAX_VALUE) throw new Error("File content too big ("+byteSize+" bytes) for byte array: "+file);
				byte fileBytes[] = new byte[(int)byteSize];
				int bytesRead = fileIn.read(fileBytes);
				if(bytesRead != fileBytes.length) throw new IOException(
					"Tried to write "+fileBytes.length+" bytes but did write "+bytesRead+" bytes.");
				return fileBytes;
			}finally{
				if(fileIn!=null) fileIn.close();
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static void write(byte data[], File file){
		write(data, file, false);
	}
	
	public static void append(byte data[], File file){
		write(data, file, true);
	}
	
	/** make sure to close it before calling other write/append funcs */
	public static OutputStream appending(File file){
		try{
			if(!file.exists()) file.createNewFile();
			return new FileOutputStream(file,true);
		}catch(IOException e){ throw new Error(e); }
	}
	
	public static void delete(File f){
		if(!f.delete()) throw new Error("Couldnt delete "+f);
	}
	
	/** Creates dirs under file if not exist already, unless names of those dirs already exist as files */
	protected static void write(byte data[], File file, boolean append){
		lg("Saving "+file);
		try{
			file.getParentFile().mkdirs();
			OutputStream fileOut = null;
			try{
				if(!file.exists()){
					File parent = file.getParentFile();
					if(parent!=null) parent.mkdirs();
					file.createNewFile();
				}
				fileOut = new FileOutputStream(file, append);
				fileOut.write(data, 0, data.length);
				fileOut.flush();
			}finally{
				if(fileOut!=null) fileOut.close();
			}
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}
	
	public static final File dirWhereThisProgramStarted;
	
	public static final File acycDir;
	static{
		dirWhereThisProgramStarted = new File(System.getProperty("user.dir")).getAbsoluteFile();
		acycDir = new File(dirWhereThisProgramStarted,"data");
		//File test = new File(acycDir,"readme.txt");
		File test = acycDir;
		if(!test.exists()){
			lgErr("My data dir not found, as its expected to contain certain files. Did not find: "+test+" If this program is in an IDE such as Eclipse or Netbeans, set the working dir to the source dir / classpath. In Eclipse (after checking the 'allow output folders for source folders') in 'project build path' set 'default output dir' to forExample the src dir which contains package/name.java and package/name.class and acyc/thatfile. In any 'run configuration' there is also a 'working dir' in the options. This would all happen automaticly if doubleclick a jar file containing most of that (including acyc/dataInJar and classes and source, and the rest is generated in a new acyc dir or reuse found acyc dir parallel to where you doubleclicked the jar.");
			System.exit(0);
		}
	}
	
	public static boolean bytesEqual(byte x[], byte y[]){
		if(x.length != y.length) return false;
		for(int i=0; i<x.length; i++){
			if(x[i] != y[i]) return false;
		}
		return true;
	}
	
	public static byte[] readFileRel(String relPath){
		//TODO merge duplicate code
		String r = relPath.startsWith("\\")||relPath.startsWith("/") ? relPath.substring(1) : relPath;
		File f = new File(r);
		if(f.exists()){ //avoid cache problems with using file url
			System.out.println("Reading file "+f);
			InputStream in = null;
			try{
				in = new FileInputStream(f);
				if(Integer.MAX_VALUE < f.length()) throw new RuntimeException("File too big: "+f);
				byte b[] = new byte[(int)f.length()];
				in.read(b);
				return b;
			}catch(IOException e){
				throw new RuntimeException(e);
			}finally{
				if(in != null) try{ in.close(); }catch(IOException e){}
			}
		}else{
			//System.out.println("Reading Class.getResourceAsStream "+relPath);
			InputStream in = null;
			try{
				in = Files.class.getResourceAsStream(relPath);
				return Occamserver.readFully(in, 1<<28, 5, true, false);
			}finally{
				if(in != null) try{ in.close(); }catch(IOException e){}
			}
		}
	}
	
	public static byte[] readFully(InputStream in){
		return Occamserver.readFully(in, 1<<28, 20, true, false);
	}
	
	/** ignores if null */
	public static void close(Closeable c){
		if(c != null){
			try{
				c.close();
			}catch(IOException e){ throw new Error(e); }
		}
	}
	
	/** ignores if null */
	public static void closeNoThrow(Closeable c){
		if(c != null){
			try{
				c.close();
			}catch(IOException e){ lgErr(e); }
		}
	}
	
	/** WARNING: this is not for longterm storage, since the datastruct can change across different java versions,
	and instead I plan to create ufnode which is a programming language and merkle datastruct for longterm storage and gaming low lag.
	*/
	public static byte[] serialize(Serializable s){
		try{
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			new ObjectOutputStream(out).writeObject(s);
			return out.toByteArray();
		}catch (IOException e){
			throw new Error(e);
		}
		
	}
	
	/** WARNING: this is not for longterm storage, since the datastruct can change across different java versions,
	and instead I plan to create ufnode which is a programming language and merkle datastruct for longterm storage and gaming low lag.
	*/
	public static Serializable deserialize(byte[] serialized){
		if(serialized == null) return null;
		try{
			return (Serializable) new ObjectInputStream(new ByteArrayInputStream(serialized)).readObject();
		}catch(ClassNotFoundException | IOException e){
			throw new Error(e);
		}
	}
	
	/** returns file for saving and loading a named object, such as "rbm5" */
	public static File serializeFile(String name){
		return new File(new File(Files.acycDir,"tempSerializable"),name+".ser");
	}
	
	/** Example: shift+number on keyboard saves rbm by Serializable (not for longterm storage) to a file */
	public static void saveBySerialize(Serializable s, String name){
		write(serialize(s), serializeFile(name));
	}
	
	public static Serializable loadBySerialize(String name){
		return deserialize(read(serializeFile(name)));
	}
	
	public static String changeBackToForwardSlashesAndAddSlashAtEndIfFolder(String fileOrFolder){
		fileOrFolder = fileOrFolder.replace('\\','/');
		File file = new File(fileOrFolder);
		if(file.isDirectory() && !fileOrFolder.endsWith("/")) fileOrFolder += "/";
		return fileOrFolder;
	}
	
	public static boolean isFolderPathName(String folderPathName){
		File f = new File(folderPathName);
		return f.exists() && f.isDirectory();
	}
	
	/** includes folder. Returns empty list if folder is not a folder. */
	public static List<File> allFoldersRecursive(File folder){
		if(!folder.isDirectory()) return new ArrayList();
		List<File> folders = new ArrayList();
		folders.add(folder);
		for(File f : folder.listFiles()) folders.addAll( allFoldersRecursive(f) );
		return folders;
	}

}
