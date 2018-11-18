/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util.ui;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.NavigableMap;

public class VertBarGraphics{
	
	/*TODO use these together:
	Js class
	VertBarGraphics class
	Controls class
	JsonDS class
	RBM.asMap and RBM(NavigableMap)
	RBM.think() somehow accessed by Js class in sandboxed way (to later be upgraded to cx/com/mem/(optional econacyc if caller pays for it))
	tODO
	*/
	
	/*Should there be String[] to name the bars? Or Object[] such as to contain float[]s
	that would be set in some Var<float[]> and displayed in a RbmPanel?
	Whatever it is, I want it to be generalized for all possible VertBarGraphics,
	not specificly RBMS, so cant be float[]. Whatever it is, I want it writing in a standard way
	into Var<NavigableMap>.
	How should this interact with Controls Var<NavigableMap>?
	In this example, there are 2 objects that only DISPLAY things (not edit them),
	which should interact thru the same Var<NavigableMap>.
	Those displays are RbmPanel and slidinglearnrandvecui (internally uses VertBarGraphics).
	I want them to each do their own thing and not be designed specificly to work together,
	instead to form that connection only thru the Var<NavigableMap>.
	slidinglearnrandvecui will have a selected object (TODO what type) that it puts in the Var<NavigableMap>.
	RbmPanel needs an RBM, which can exist in a NavigbleMap as it has funcs to translate itself to/from that.
	The event system needs to allow subscribing to branches so change of one doesnt trigger display of another.
	Or maybe there should be multiple Var as if they were all 1 level deep in root object,
	such as a Var/child for state of slidinglearnrandvecui and a Var/child for state of (NavigableMap)rbm.
	Either way, theres an event when slidinglearnrandvecui changes its selected object,
	so then how is the (NavigableMap)rbm supposed to get replaced?
	I'd prefer if that connection existed only in the outer NavigableMap,
	such as in a "triggers" branch, but I'm not sure how that could be organized.
	In general, I want to put arbitrary code in the NavigableMap thats run when some child of the NavigableMap is changed,
	as a replacement for the java code that listens.
	This might be too complex to build quickly and best wait for ufnode?
	I could do js through ScriptEngine, if its sandboxed against calling java code.
	https://stackoverflow.com/questions/20793089/secure-nashorn-js-execution says use
	jdk.nashorn.api.scripting.ClassFilter
	*/
	
	
	public static void paint(Graphics g, Rectangle rect, int colorBackground, float[] heightFractions, int[] colorARGB){
		if(heightFractions.length != colorARGB.length) throw new Error("Diff sizes");
		g.setColor(new Color(colorBackground,true));
		g.fillRect(rect.x, rect.y, rect.width, rect.height);
		for(int i=0; i<heightFractions.length; i++){
			int h = Math.max(0, Math.min((int)(heightFractions[i]*rect.height+.5f), rect.height));
			int startX = rect.width*i/heightFractions.length;
			int endX = rect.width*(i+1)/heightFractions.length;
			g.setColor(new Color(colorARGB[i],true));
			g.fillRect(startX, rect.height-h, endX-startX, rect.height);
		}
	}

}
