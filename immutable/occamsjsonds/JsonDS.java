/** Ben F Rayfield offers this software opensource MIT license.
C# Life3d/OccamsJson ported to Java (no true/false/null) by Ben F Rayfield, both opensource MIT license.
*/
package immutable.occamsjsonds;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;

/** //TODO then modified for Zing
//TODO upload occamsjsonds to github java dir before further modify it for zing.
DS (of JsonDS) means a subset of json with Doubles and Strings
but excluding booleans and null, and all numbers are float64.
Maps are sorted first doubles then strings, such as new TreeMap(JsonDS.mapKeyComparator).
<br><br>
Functions parse(String) and toJson(Object) create and use any acyclicNet (TODO find duplicates?)
of these types: NavigableMap, List, String, Double, (TODO) double[].
<br><br>
TODO optimize parsing by scanning the whole string to be parsed before tokenizing.
Scan for { } [ ] and ". Between every [ and ] with none of the others between them,
parse it as a double array. The later tokenize and parseTokens steps
will need to be slightly modified or used in smaller parts.
This will make it efficient enough to do neuralnets and other math in parsed strings.
<br><br>
Returned objects are immutable NavigableMaps, immutable Lists, (immutable of course) Double,
and (caller is not supposed to modify, which led to Zing which is incomplete) float[].
TODO verify that.
*/
public class JsonDS{

	public static Object jsonParse(String json){
		String[] tokens = tokenize(json);
		return parseTokens(tokens);
	}

	public static String jsonString(Object mapOrList){
		StringBuilder sb = new StringBuilder();
		toJson(sb, mapOrList, 0);
		return sb.toString().trim(); //TODO optimize by not having to trim()
	}
	
	public static final String n = "\r\n";

	//public static final NullObject nullObject = new NullObject();
	//protected static final class NullObject{};

	/*public static Object parseNumber(String jsonNumber){
		if (jsonNumber.contains(".")) return Double.parseDouble(jsonNumber);
		return Long.parseLong(jsonNumber);
	}*/
	
	public static Object parseNumber(String jsonNumber){
		return Double.parseDouble(jsonNumber);
	}

	public static Object parseTokens(String[] tokens){
		int intPtr[] = {0};
		return parseTokens(tokens, intPtr);
	}

	/** After each range of tokens is consumed, *intPtr[0] is the index right after it.
	String literal, number, booleaneans, and null are single token. Brackets recurse.
	Unsafe part is int pointer. Its target value is added to during recursive parsing.
	TODO change that to int[] size 1? It would remove the need for unsafe keyword.
	*/
	public static Object parseTokens(String[] tokens, int intPtr[]){
		String firstToken = tokens[intPtr[0]];
		intPtr[0]++;
		char firstChar = firstToken.charAt(0);
		if(firstChar == '{'){
			
			NavigableMap map = new TreeMap(mapKeyComparator);
			while(intPtr[0] < tokens.length){
				String token = tokens[intPtr[0]];
				intPtr[0]++;
				if(token.equals("}")) return map;
				intPtr[0]--;  //parse starting with what could have been ]
				Object key = parseTokens(tokens, intPtr);
				String nextToken = tokens[intPtr[0]];
				intPtr[0]++;
				if(!nextToken.equals(":")) throw new RuntimeException("Expected colon after key but got "+nextToken);
				Object value = parseTokens(tokens, intPtr);
				map.put(key, value);
				if(intPtr[0] == tokens.length) throw new RuntimeException("Unclosed dictionary/map at end");
				nextToken = tokens[intPtr[0]];
				intPtr[0]++;
				if(nextToken.equals("}")){
					return Collections.unmodifiableNavigableMap(map);
				}
				//then it must be comma
				if(!nextToken.equals(",")) throw new RuntimeException(
					"Expected comma at token index "+intPtr[0]);
			}
			throw new RuntimeException("Unclosed dictionary/map at end");
		}else if(firstChar == '['){
			//TODO return double[] if all are numbers
			List<Object> list = new ArrayList();
			boolean allHaveBeenDoubles = true;
			boolean allDoublesFitInFloats = true;
			while(intPtr[0] < tokens.length){
				String token = tokens[intPtr[0]];
				intPtr[0]++;
				if(token.equals("]")){
					if(!list.isEmpty() && allHaveBeenDoubles){
						//return listToArrayOfDoubles_todoOptimizeByNotCreatingList(list);
						if(allDoublesFitInFloats){
							return listOfDoublesToArrayOfFloats_todoOptimizeByNotCreatingList(list);
						}
					}
					return Collections.unmodifiableList(list);
				}else{
					intPtr[0]--;  //parse starting with what could have been ]
					Object o = parseTokens(tokens, intPtr);
					if(o instanceof Double){
						if(!token.equalsIgnoreCase(""+Float.parseFloat(token))){
							allDoublesFitInFloats = false;
						}
					}else{
						allHaveBeenDoubles = false;
					}
					list.add(o);
					if(intPtr[0] == tokens.length) throw new RuntimeException("Unclosed list at end");
					String nextToken = tokens[intPtr[0]];
					intPtr[0]++;
					if(nextToken.equals("]")){
						if(allHaveBeenDoubles){
							//return listToArrayOfDoubles_todoOptimizeByNotCreatingList(list);
							if(allDoublesFitInFloats){
								return listOfDoublesToArrayOfFloats_todoOptimizeByNotCreatingList(list);
							}
						}
						return Collections.unmodifiableList(list);
					}else{ //then it must be comma
						if(!nextToken.equals(",")) throw new RuntimeException(
							"Expected comma at token index "+intPtr[0]+" firstToken="+firstToken+" nextToken="+nextToken);
					}
				}
			}
			throw new RuntimeException("Unclosed list at end. firstToken="+firstToken);
		}else if(firstChar == '"'){
			return unescape(firstToken);
		}else if(firstChar == '-' || firstChar == '.' || Character.isDigit(firstChar)){
			return parseNumber(firstToken);
		//}else if(firstChar == 't' && "true".equals(firstToken)){
		//	return true;
		//}else if(firstChar == 'f' && "false".equals(firstToken)){
		//	return false;
		//}else if(firstChar == 'n' && "null".equals(firstToken)){
		//	return nullObject;
		}
		throw new RuntimeException("First token (in a recursion, intPtr="+intPtr[0]
			+") must be { or [ or quote or - or digit or in true, false, or null,"
			+" but its length is "+firstToken.length()+" and String is "+firstToken);
	}

	public static String[] tokenize(String json){
		List<String> tokens = new ArrayList<String>();
		String[] bigTokens = tokenizeStringLiterals(json);
		for(int i=0; i<bigTokens.length; i++){
			if(bigTokens[i].charAt(0) == '"'){
				tokens.add(bigTokens[i]);
			}else{
				String[] smallTokens = tokenizeBetweenStringLiterals(bigTokens[i]);
				for(int s=0; s<smallTokens.length; s++){
					tokens.add(smallTokens[s]);
				}
			}
		}
		return tokens.toArray(new String[0]);
	}

	public static String escape(String s){
		StringBuilder sb = new StringBuilder();
		sb.append('"');
		for(int i=0; i<s.length(); i++){
			char c = s.charAt(i);
			switch(c){
				case '\t': sb.append("\\t"); break;
				case '\r': sb.append("\\r"); break;
				case '\n': sb.append("\\n"); break;
				case '\\': sb.append("\\\\"); break;
				case '\"': sb.append("\\\""); break;
				default: sb.append(c); break;
			}
		}
		sb.append('"');
		return sb.toString();
	}

	public static Object unescape(String quote){
		if(quote.charAt(0) != '"') throw new RuntimeException("Does not start with quote: "+quote);
		boolean backslash = false;
		StringBuilder sb = new StringBuilder();
		for(int i=1; i<quote.length(); i++){
			char c = quote.charAt(i);
			if(backslash){
				switch(c){
				case 't': sb.append('\t'); break;
				case 'r': sb.append('\r'); break;
				case 'n': sb.append('\n'); break;
				case '\\': sb.append('\\'); break;
				case '"': sb.append('"'); break;
				//case '\r': case '\n':
				//	throw new Exception("Unclosed String literal before line end. quote["+quote+"]");
				//break;
				default:
					//TODO do we want error if unrecognized escape code?
					//sb.append('\\').append(c);
					sb.append(c);
				break;
				}
				backslash = false;
			}else{ // !backslash
				switch(c){
				case '"':
					if(i != quote.length()-1) throw new RuntimeException("String literal ended early at char index "+i
						+" of "+quote.length()+" in StringLiteral["+quote+"]");
					return sb.toString();
				case '\\':
					backslash = true;
				break;
				default:
					sb.append(c);
				break;
				}
			}
		}
		throw new RuntimeException("String literal did not end ["+quote+"]");
	}

	/** Returns String literals (including quotes and escape codes) and whats between them.
	Whats between them has multiple tokens per token returned here, to be further parsed.
	*/
	public static String[] tokenizeStringLiterals(String json){
		char[] chars = json.toCharArray();
		boolean inStrLiteral = false;
		boolean backslash = false;
		List<String> tokens = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		for(int i = 0; i < json.length(); i++){
			char c = chars[i];
			//Start.log("Parsing char: "+c);
			if(inStrLiteral){
				if(backslash){
					switch(c){
					case 't': sb.append("\\t"); break;
					case 'r': sb.append("\\r"); break;
					case 'n': sb.append("\\n"); break;
					case '\\': sb.append("\\\\"); break;
					case '"': sb.append("\\\""); break;
					//case '\r': case '\n':
					//	if(inStrLiteral) throw new Exception("Unclosed String literal before line end");
					//break;
					default:
						//TODO do we want error if unrecognized escape code?
						//sb.append('\\').append(c);
						sb.append(c);
					break;
					}
					backslash = false;
				}else{ //inStrLiteral && !backslash
					switch(c){
					case '"':
						sb.append('"');
						String token = sb.toString();
						//Start.log("Parsed token: "+token);
						//Start.log(n+"=== Parsed token inStrLiteral: "+token+" ==="+n);
						tokens.add(token);
						sb.setLength(0); //was sb.Clear() in c#
						//inStrLiteral = backslash = inOtherLiteral = false;
						inStrLiteral = backslash = false;
						//Also at end check for unclosed String literal
					break;
					case '\\':
						backslash = true;
					break;
					default:
						sb.append(c);
					break;
					}
				}
			}else{ // !inStringLiteral
				if(c=='"'){
					if(sb.length() != 0){
						String token = sb.toString();
						//Start.log(n+"=== Parsed token !inStrLiteral: "+token+" ==="+n);
						tokens.add(token);
						sb.setLength(0); //was sb.Clear() in c#
						//inStrLiteral = backslash = inOtherLiteral = false;
						inStrLiteral = backslash = false;
					}
					if(c == '"'){
						sb.append('"');
						inStrLiteral = true;
					}
				}else{
					sb.append(c);
				}
			}
		}
		if(inStrLiteral) throw new RuntimeException("Unclosed String literal");
		//Get last nonStringLiteral token which ends here.
		if(sb.length() != 0){
			String token = sb.toString();
			//Start.log("Parsed token: "+token);
			//Start.log(n+"=== Parsed token at end: "+token+" ==="+n);
			tokens.add(token);
		}
		return tokens.toArray(new String[0]);
	}



	/** Tokenize 1 of the Strings returned by tokenizeStringLiterals, those that arent String literals.
	Each will become 1 or more tokens.
	*/
	public static String[] tokenizeBetweenStringLiterals(String json){
		//Start.log("START tokenizeBetweenStringLiterals: "+json);
		char[] chars = json.toCharArray();
		List<String> tokens = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		boolean prevOneCharToken = false;
		for(int i = 0; i < json.length(); i++){
			char c = chars[i];
			boolean whitespace = Character.isWhitespace(c);
			//Start.log("Parsing char: "+c);
			boolean oneCharTokenNow = c==',' || c==':' || c == '{' || c == '}' || c == '[' || c == ']';
			if(prevOneCharToken || whitespace || oneCharTokenNow){
				if(sb.length() != 0){
					String token = sb.toString();
					//Start.log(n+"=== Parsed token: "+token+" ==="+n);
					tokens.add(token);
					sb.setLength(0); //was sb.Clear() in c#
				}
			}
			if(!whitespace) sb.append(c);
			prevOneCharToken = oneCharTokenNow; //for if next token starts after a comma etc
		}
		//Get last token which ends here
		if(sb.length() != 0){
			String token = sb.toString();
			//Start.log(n+"=== Parsed token at end: "+token+" ==="+n);
			tokens.add(token);
		}
		//Start.log("END tokenizeBetweenStringLiterals: "+json);
		return tokens.toArray(new String[0]);
	}
	
	public static final Class listType = List.class; //TODO use isAssignableFrom instead of ==

	public static final Class mapType = NavigableMap.class; //TODO use isAssignableFrom instead of ==

	public static final Class StringType = String.class;

	public static final Class scalarType = Double.class; //TODO primitive or object wrapper?

	//public static final Class longType = Long.class; //TODO primitive or object wrapper?
	
	/*protected static enum Ty{
		tyScalar,
		tyLong,
		tyString,
		tyDoubleArray,
		tyLongArray,
		tyMap,
		tyList,
		tyNull
	};

	public static Ty typeToIntForComparator(Class c){
		if(c == scalarType) return Ty.tyScalar;
		if(c == longType) return Ty.tyLong;
		if(c == StringType) return Ty.tyString;
		if(c == double[].class) return Ty.tyDoubleArray; //TODO optimization instead of List<Double>
		if(c == long[].class) return Ty.tyLongArray; //TODO optimization instead of List<Long>
		if(mapType.isAssignableFrom(c)) return Ty.tyMap;
		if(listType.isAssignableFrom(c)) return Ty.tyList;
		if(c == NullObject.class) return Ty.tyNull;
	}*/
	
	/** Doubles first, then Strings.
	TODO optimize. double[] (TODO float[] and multi dim array of those, since rbm weights are float[][][])
	is allowed as value but not key. Keys can only be Double or String.
	*/
	public static final Comparator mapKeyComparator = new Comparator(){
		public int compare(Object x, Object y){
			if(x instanceof Double){
				if(y instanceof Double){
					if((Double)x < (Double)y) return -1;
					if((Double)x > (Double)y) return 1;
					return 0;
				}else{ //y instanceof String
					return -1; //TODO is this backward?
				}
			}else{ //x instanceof String
				if(y instanceof Double){
					return 1; //TODO is this backward?
				}else{ //y instanceof String
					return ((String)x).compareTo((String)y);
				}
			}
			/*Ty xt = typeToIntForComparator(x.getClass());
			Ty yt = typeToIntForComparator(y.getClass());
			if(xt != yt) return xt.ordinal()-yt.ordinal(); //TODO is this backward?
			switch(xt){
			case tyScalar: case tyLong: case tyString:
				return ((Comparable)x).compareTo(y);
			case tyDoubleArray:
			case tyLongArray:
			case tyMap:
			case tyList:
			
			if(c == double[].class) return 3; //TODO optimization instead of List<Double>
			if(c == long[].class) return 4; //TODO optimization instead of List<Long>
			if(mapType.isAssignableFrom(c)) return 5;
			if(listType.isAssignableFrom(c)) return 6;
			if(c == NullObject.class) return 7;
			*/
		}
	};
	
	//TODO optimize
	public static String doubleToString(double d){
		//scientific notation using "e" instead of "E", for compatibility with javascript
		String s = Double.toString(d).toLowerCase();
		if(s.endsWith(".0")) s = s.substring(0, s.length()-2); //TODO use DecimalFormat
		//if(!s.contains(".")) s += ".0";
		return s;
	}

	public static void toJson(StringBuilder sb, Object acyclicNet, int recurse){
		appendLineThenTabs(sb, recurse);
		//if(acyclicNet == nullObject || acyclicNet == null){
		//	sb.append("null");
		//	return;
		//}
		Class t = acyclicNet.getClass();
		if(listType.isAssignableFrom(t)){
			sb.append('[');
			List<Object> list = (List<Object>) acyclicNet;
			for(int i=0; i<list.size(); i++){
				Object o = list.get(i);
				toJson(sb, o, recurse+1);
				if(i != list.size()-1) sb.append(',');
			}
			appendLineThenTabs(sb, recurse);
			sb.append(']');
		}else if(mapType.isAssignableFrom(t)){
			sb.append('{');
			NavigableMap<Object,Object> map = (NavigableMap<Object,Object>) acyclicNet;
			int i = 0;
			for(Object key : map.keySet()){
				Object value = map.get(key);
				toJson(sb, key, recurse+1);
				sb.append(" :");
				toJson(sb, value, recurse+1);
				if(i != map.size()-1) sb.append(',');
				i++;
			}
			appendLineThenTabs(sb, recurse);
			sb.append('}');
		}else if(StringType.isAssignableFrom(t)){
			sb.append(escape((String)acyclicNet));
		}else if(scalarType.isAssignableFrom(t)){
			sb.append(doubleToString((double)acyclicNet));
		}else if(t == double[].class){
			double a[] = (double[])acyclicNet;
			sb.append('[');
			for(int i=0; i<a.length; i++){
				sb.append(doubleToString(a[i]));
				if(i != a.length-1) sb.append(',');
			}
			sb.append(']');
		//}else if(longType.isAssignableFrom(t)){
		//	sb.append((long)acyclicNet);
		//}else if(acyclicNet.equals(true)){
		//	sb.append("true");
		//}else if(acyclicNet.equals(false)){
		//	sb.append("false");
		}else throw new RuntimeException("Object type not recognized: "+acyclicNet);
	}

	public static void appendLineThenTabs(StringBuilder sb, int tabs){
		sb.append(n);
		for(int i=0; i<tabs; i++) sb.append('\t');
	}
	
	/** Not the optimization I was looking for in computing time, but at least it makes List<Double> garbcolable */
	public static double[] listToArrayOfDoubles_todoOptimizeByNotCreatingList(List list){
		int siz = list.size();
		double a[] = new double[siz];
		for(int i=0; i<siz; i++) a[i] = (Double)list.get(i);
		return a;
	}
	
	public static float[] listOfDoublesToArrayOfFloats_todoOptimizeByNotCreatingList(List list){
		int siz = list.size();
		float a[] = new float[siz];
		for(int i=0; i<siz; i++) a[i] = (float)(double)list.get(i);
		return a;
	}
	
	public static final NavigableMap emptyMap = Collections.emptyNavigableMap();
	
	public static final List emptyList = Collections.emptyList();
	
	/** If pathAndValue[pathAndValue.length-1]==null, forkDeletes (FIXME this only works for maps so far).
	Sets a path to a value. Return and params are NavigableMap, List, and other jsonDS types.
	Params start with any such object.
	Then a path from it (all except first and last param).
	Then a value to set.
	If there are only 2 params, by that logic, returns last param.
	If there are 3 params x y z, returns x with key y set to z.
	<br><br>
	When path doesnt exist, has to choose types to create:
	Only float[] (TODO or more dimensions) can contain value of Float.
	NavigableMaps take keys as String or Double.
	Lists take keys as Integer
	<br><br>
	For immutable NavigableMaps, Lists, etc, need a way to set data in them.
	Technically this may contain float[] and maybe double[],
	which cant be immutable but caller can still use them that way.
	<br><br>
	If param "in" is null, creates a NavigableMap, List, or float[] by first in pathAndValue,
	if pathAndValue size is at least 2 (cuz else would be just the value).
	*/ 
	public static <T> T jsonSet(T in, Object... pathAndValue){
		if(pathAndValue.length < 1) throw new RuntimeException("No value");
		if(pathAndValue.length == 1) return (T) pathAndValue[0]; //null if delete
		if(in == null){
			if(pathAndValue[0] instanceof String || pathAndValue[0] instanceof Double){
				in = (T) new TreeMap(JsonDS.mapKeyComparator);
			}else if(pathAndValue[0] instanceof Integer){
				int i = (Integer)pathAndValue[0];
				if(pathAndValue.length == 2 && pathAndValue[1] instanceof Float){
					//Very inefficient to replace float[] every time 1 bigger,
					//so caller would probably create the float[].
					in = (T) new float[1];
				}else{
					in = (T) new ArrayList();
				}
			}else{
				throw new Error(
					"Dont know what type to create for in=null and pathAndValue="+Arrays.asList(pathAndValue));
			}
		}
		if(in instanceof NavigableMap){
			Object child = ((NavigableMap)in).get(pathAndValue[0]); //if null, newChild creates by param types
			Object newChild = jsonSet(child, newArrayWithFirstRemoved(pathAndValue));
			NavigableMap newMap = new TreeMap(JsonDS.mapKeyComparator);
			newMap.putAll((NavigableMap)in);
			if(newChild == null) newMap.remove(pathAndValue[0]);
			else newMap.put(pathAndValue[0], newChild);
			return (T) Collections.unmodifiableNavigableMap(newMap);
		}else if(in instanceof List){
			//FIXME remove from list (by creating smaller list) if pathAndValue[pathAndValue.length-1]==null. Just doing that for Maps for now.
			int index = (Integer)pathAndValue[0];
			Object newChild;
			boolean append = index == ((List)in).size();
			if(append){
				newChild = jsonSet(null, newArrayWithFirstRemoved(pathAndValue)); //replace Null by first type in path
			}else{
				Object child = ((List)in).get(index);
				newChild = jsonSet(child, newArrayWithFirstRemoved(pathAndValue));
			}
			List newList = new ArrayList((List)in);
			if(append) newList.add(newChild);
			else newList.set(index,newChild);
			return (T) Collections.unmodifiableList(newList);
		}else if(in instanceof float[]){
			//FIXME remove from float[] (by creating smaller float[]) if pathAndValue[pathAndValue.length-1]==null. Just doing that for Maps for now.
			if(pathAndValue.length == 2){
				float[] out = ((float[])in).clone();
				out[(Integer)pathAndValue[0]] = ((Number)pathAndValue[1]).floatValue();
				return (T) out;
			}
			throw new RuntimeException("Path ended early when got float[], in="+in
				+" and pathAndValue="+Arrays.asList(pathAndValue));
		}else{
			throw new RuntimeException("Unknown type: "+in.getClass());
		}
	}
	
	/** Gets else null. See jsonset(Object...) */
	public static Object jsonGet(Object in, Object... path){
		Object o = in;
		for(int i=0; i<path.length; i++){
			if(o instanceof NavigableMap){
				o = ((NavigableMap)o).get(path[i]); //null if not found
			}else if(o instanceof List){
				int listIndex = (Integer)path[i];
				if(0 <= listIndex && listIndex < ((List)o).size()){
					o = ((List)o).get(listIndex);
				}else{
					return null; //viewing List as Map of int to value
				}
			}else if(o instanceof float[]){
				//FIXME what if its float[][] or more?
				//Rbm uses float[][][] weights, but probably that wouldnt be stored in json.
				if(i != path.length-1) throw new RuntimeException("Got float array before path ended");
				int arrayIndex = (Integer)path[i];
				if(0 <= arrayIndex && arrayIndex < ((float[])o).length){
					o = ((float[])o)[arrayIndex];
				}else{
					return null; //viewing float[] as Map of int to float
				}
			}
		}
		return o;
	}
	
	/** immutable */
	public static List removeValueFromList(List list, Object value){
		List newList = new ArrayList(list);
		newList.remove(value);
		return Collections.unmodifiableList(newList);
	}
	
	/** Size of NavigableMap or List or float[], else 1 if String or Double */
	public static int jsonSize(Object in, Object... path){
		Object o = jsonGet(in,path);
		if(o instanceof Collection) return ((Collection)o).size();
		if(o instanceof float[]) return ((float[])o).length;
		if(o instanceof String || o instanceof Double) return 1;
		throw new RuntimeException("Unknown type: "+o.getClass());
	}
	
	public static Object[] newArrayWithFirstRemoved(Object... a){
		Object[] b = new Object[a.length-1];
		System.arraycopy(a, 1, b, 0, b.length);
		return b;
	}
	
	public static boolean doubleToBoolean(double d){
		return d > 0;
	}
	
	public static double booleanToDouble(boolean b){
		return b ? 1 : 0;
	}
	
	public static float[] booleanArrayToFloatArray(boolean[] b){
		float[] a = new float[b.length];
		for(int i=0; i<a.length; i++){
			a[i] = (float)booleanToDouble(b[i]);
		}
		return a;
	}
	
	public static boolean[] floatArrayToBooleanArray(float[] f){
		boolean[] a = new boolean[f.length];
		for(int i=0; i<a.length; i++){
			a[i] = doubleToBoolean(f[i]);
		}
		return a;
	}
	
	public static String jsonToSingleLine(String json){
		return json.replaceAll("(\\r|\\n|\\r\\n)\\t*", "");
	}
	
}
