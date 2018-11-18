/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util;

public class JavaUtil{
	private JavaUtil(){}
	
	public static String removeComments(String javaCode){
		char c[] = javaCode.toCharArray();
		int indexOfLastCharInComment = -1;
		StringBuilder sb = new StringBuilder(c.length);
		boolean inSlashSlashComment = false;
		boolean inSlashStarComment = false;
		boolean inStringLiteral = false;
		int lastCharIndex = c.length-1;
		int consecutiveBackslashes = 0; //Reset to 0 at every nonbackslash char. If not in comment,
		//and even backslash count, followed by a double-quote, then a string-literal starts or ends.
		for(int i=0; i<=lastCharIndex; i++){
			//if(c[i] == '\r') throw new Exception( //require newlines be exactly \n
			//	"Char 13 (CARRIAGE RETURN) found in Java code: "+javaCode.replace("\r", "\nCARRIAGE-RETURN-WAS-HERE"));
			//TODO Do something about all unicode newline chars. Theres approximately 10 of them.
			if(inSlashSlashComment){
				if(i == lastCharIndex || c[i] == '\r' || c[i] == '\n'){ //end comment
					inSlashSlashComment = false;
					indexOfLastCharInComment = (c[i] == '\r' || c[i] == '\n') ? i-1 : i;
				}
			}else if(inSlashStarComment){
				if(i == lastCharIndex){ //TODO combine duplicate code
					throw new Error("Slash-star comment did not end: "+javaCode);
				}else if(i+1 == lastCharIndex){
					if(c[i] != '*' || c[i+1] != '/') throw new Error("Slash-star comment did not end: "+javaCode);
				}else{
					if(c[i] == '*' && c[i+1] == '/'){ //end comment
						inSlashStarComment = false;
						i++; //consume 2 chars this iteration instead of 1
						indexOfLastCharInComment = i;
					}
				}
			}else{ //not in a comment
				if(c[i] == '\\'){
					consecutiveBackslashes++;
				}else{
					if(c[i] == '"' && (consecutiveBackslashes&1)==0){ //start or end string-literal
						inStringLiteral = !inStringLiteral;
					}else{
						if(!inStringLiteral){
							//TODO combine duplicated code
							if(i < lastCharIndex && c[i] == '/' && c[i+1] == '/'){
								//start slash-slash comment, and use the Java code before it
								inSlashSlashComment = true;
								sb.append(javaCode.substring(indexOfLastCharInComment+1,i));
								i++; //consume 2 chars this iteration instead of 1
							}else if(i < lastCharIndex && c[i] == '/' && c[i+1] == '*'){
								//start slash-star comment, and use the Java code before it
								inSlashStarComment = true;
								sb.append(javaCode.substring(indexOfLastCharInComment+1,i));
								i++; //consume 2 chars this iteration instead of 1
							}
						}
						//If in string-literal, that is Java code and will be used later
					}
					consecutiveBackslashes = 0; //TODO test this function. This line was moved down here.
				}
			}
		}
		if(inSlashSlashComment) throw new Error("inSlashSlashComment at end of javaCode="+javaCode);
		if(inSlashStarComment) throw new Error("inSlashStarComment at end of javaCode="+javaCode);
		if(inStringLiteral) throw new Error("inStringLiteral at end of javaCode="+javaCode);
		sb.append(javaCode.substring(indexOfLastCharInComment+1,lastCharIndex+1)); //code after last comment
		return sb.toString();
	}

}
