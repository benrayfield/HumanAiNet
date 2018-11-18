/** Ben F Rayfield offers this software opensource MIT license */
//test
package immutable.occamsjsonds;
import java.util.NavigableMap;
/** C# Life3d/OccamsJson ported to Java (no true/false/null) by Ben F Rayfield- both opensource MIT license */

//TODO then modified for Zing
//TODO upload occamsjsonds to github java dir before further modify it for zing.
public class TestJsonDS{

	public static final String n = JsonDS.n;

	public static String anExampleOfJson = //TODO more test cases. jsoncpp has some meant to pass and fail
		"{"
		+n+"	\"rnrnabcrn\" : \"\\r\\n\\r\\nabc\\r\\n\"," // "rnrnabcrn" : "\r\n\r\nabc\r\n"
		+n+"	\"ab	\\nc\" : 234,"
		+n+"	\"def\" : { \"ghi\" : \"JKL\", \"mn\" : 55589 },"
		+n+"	null : [false,true],"
		+n+"	false : [null,true],"
		+n+"	true : [false,false,null,false,true,3.14159],"
		+n+"	\"\" : \"value whose key is empty string\","
		+n+"	\"attribute\" : ["
		+n+"		\"ffjjj\","
		+n+"		\"opqr\","
		+n+"		\"stuv\","
		+n+"		5,"
		+n+"		{ \"aab\" : -7, false : 445 }"
		+n+"	],"
		+n+"	\"cce\": { \"22\" :"
		+n+"		{ \"22\" :"
		+n+"			{ null :  { \"ffggg\" : [ 5,-7.7] }"
		+n+"			}"
		+n+"		}"
		+n+"	}"
		+n+"}";
	
	public static String anExampleOfJsonDS =
		"{"
		+n+"	\"rnrnabcrn\" : \"\\r\\n\\r\\nabc\\r\\n\"," // "rnrnabcrn" : "\r\n\r\nabc\r\n"
		+n+"	\"ab	\\nc\" : 234,"
		+n+"	\"def\" : { \"ghi\" : \"JKL\", \"mn\" : 55589 },"
		+n+"	0 : [0,1,3.4,5.6,3.21,2.2222,33.3333,44.44,1.0001e-5],"
		+n+"	1 : [0,0,\"null\",0,1,3.14159],"
		+n+"	\"\" : \"value whose key is empty string\","
		+n+"	\"attribute\" : ["
		+n+"		\"ffjjj\","
		+n+"		\"opqr\","
		+n+"		\"stuv\","
		+n+"		5,"
		+n+"		{ \"aab\" : -7, 0 : 445 }"
		+n+"	],"
		+n+"	\"cce\": { \"22\" :"
		+n+"		{ \"22\" :"
		+n+"			{ \"null\" :  { \"ffggg\" : [ 5,-7.7] }"
		+n+"			}"
		+n+"		}"
		+n+"	}"
		+n+"}";

	static String esc(String s){
		return JsonDS.escape(s);
	}
	
	//TODO still need to test float[] vs Double after getting rid of that double[] code
	
	static void testGetAndSetByPath(){
		System.out.println("Original: "+anExampleOfJsonDS);
		Object parsed = JsonDS.jsonParse(anExampleOfJsonDS);
		Object x = JsonDS.jsonSet(parsed, "cce", "22", "22", "null", "ffggg", 1, -8.8);
		System.out.println("replaced -7.7 with -8.8: "+JsonDS.jsonString(x));
		Object y = JsonDS.jsonSet(x, "cce", "22", "22", "null", "ffggg", 2, -9.9);
		System.out.println("added 9.9: "+JsonDS.jsonString(y));
		Object z = JsonDS.jsonSet(y, "def", "ghi", "replacedDefWithThis");
		System.out.println("replaced JKL with replacedDefWithThis: "+JsonDS.jsonString(z));
	}

	public static void testParseThenReverse(String json){
		Object parsed = JsonDS.jsonParse(json);
		System.out.println("parsed: "+parsed);
		NavigableMap parsedMap = (NavigableMap) parsed;
		Object val = parsedMap.get("rnrnabcrn");
		System.out.println("val of \"rnrnabcrn\" is ["+val+"]");
		//parsedMap.put("rnrnabcrn_testAddValue", "\r\n\r\nabc\r\n"); //would use OccamsJsonDS.set immutably
		String reverse = JsonDS.jsonString(parsed);
		System.out.println("reverse: "+reverse);
	}
	
	public static void main(String[] args){
		System.out.println("IN["+anExampleOfJsonDS+"]");
		testParseThenReverse(anExampleOfJsonDS);
		testGetAndSetByPath();
	}

}