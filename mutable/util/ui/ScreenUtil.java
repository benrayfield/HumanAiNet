/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util.ui;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.Window;

import immutable.learnloop.RBM;

public class ScreenUtil{
	private ScreenUtil(){}
	
	public static void moveToScreenCenter(Window w){
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		w.setLocation((screen.width-w.getWidth())/2, (screen.height-w.getHeight())/2);
	}
	
	public static void moveToHorizontalScreenCenter(Window w){
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		w.setLocation((screen.width-w.getWidth())/2, w.getLocation().y);
	}
	
	public static int color(float bright){
		return color(bright, bright, bright);
	}
	
	/** Unlike new Color(r,g,b).getRGB(), 1-epsilon rounds to brightest.
	Truncates red, green, and blue into range 0 to 1 if needed.
	*/
	public static int color(float red, float green, float blue){
		return 0xff000000 |
			(RBM.holdInRange(0, (int)(red*0x100), 0xff) << 16) |
			(RBM.holdInRange(0, (int)(green*0x100), 0xff) << 8) |
			RBM.holdInRange(0, (int)(blue*0x100), 0xff);
	}
	
	public static int color(float alpha, float red, float green, float blue){
		return (RBM.holdInRange(0, (int)(alpha*0x100), 0xff) << 24) |
			(RBM.holdInRange(0, (int)(red*0x100), 0xff) << 16) |
			(RBM.holdInRange(0, (int)(green*0x100), 0xff) << 8) |
			RBM.holdInRange(0, (int)(blue*0x100), 0xff);
	}
	
	public static Window getWindow(Component c){
		while(!(c instanceof Window)) c = c.getParent();
		return (Window)c;
	}

}
