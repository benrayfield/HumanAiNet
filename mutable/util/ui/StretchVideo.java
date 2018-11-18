/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util.ui;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.function.*;

import javax.swing.JFrame;
import javax.swing.JPanel;

import mutable.util.Parallel;
import mutable.util.Rand;
import mutable.util.Time;

/** Interactive pixel grid that stretches to panel size, normally low resolution
because of the slowness of an IntBinaryOperator function call per magnified pixel.
Remembers which (TODO) keys and mouse buttons are up and down and mouse position
and which square the mouse is over. Displays colored squares
as IntBinaryOperator of (y,x) to colorARGB.
Example: BiFunction which displays the vars in Ainodes each in an NxN smaller square.
*/
public class StretchVideo extends JPanel{
	
	//TODO optimize: use DrawPixelsAsInts which uses MemoryImageSource which is inTheory faster than BufferedImage
	
	public IntBinaryOperator painter;
	
	public final int squaresTall, squaresWide;
	
	protected final BufferedImage img;
	
	public int mouseY, mouseX;

	/** If true, divides pixels into as many threads as Parallel.cpus,
	cuz "IntBinaryOperator painter" may be very slow such as calling an RBM for a different input at each pixel
	to meausure how much its learned (which Learn2d class does).
	*/
	public final boolean multithread;
	
	/** The Rect is how many squares tall and wide. The Bifunction takes (y,x) and returns colorARGB.
	If painter is null, then caller must set the pixels in the BufferedImage directly.
	*/
	public StretchVideo(boolean multithread, int squaresTall, int squaresWide, IntBinaryOperator painter){
		this.multithread = multithread;
		this.squaresTall = squaresTall;
		this.squaresWide = squaresWide;
		this.painter = painter;
		img = new BufferedImage(squaresWide, squaresTall, BufferedImage.TYPE_4BYTE_ABGR);
		addMouseMotionListener(new MouseMotionListener(){
			public void mouseMoved(MouseEvent e){
				mouseY = e.getY();
				mouseX = e.getX();
				StretchVideo.this.repaint();
			}
			public void mouseDragged(MouseEvent e){ mouseMoved(e); }
		});
	}
	
	public void paint(Graphics g){
		paint(g,getHeight(),getWidth());
	}
	
	/** so can call this from another Component.paint without this one being in the tree */
	public void paint(Graphics g, int height, int width){
		if(painter != null){ //else caller already set BufferedImage pixels directly
			if(multithread){
				Parallel.forkAndWait(squaresTall*squaresWide, (int i)->{
					int y = i/squaresWide;
					int x = i%squaresWide;
					int colorARGB = painter.applyAsInt(y, x);
					img.setRGB(x, y, colorARGB);
				});
			}else{
				for(int y=0; y<squaresTall; y++){
					for(int x=0; x<squaresWide; x++){
						int colorARGB = painter.applyAsInt(y, x);
						img.setRGB(x, y, colorARGB);
					}
				}
			}
		}
		double yMagnify = (double)height/squaresTall;
		double xMagnify = (double)width/squaresWide;
		if(g instanceof Graphics2D && (xMagnify != 1 || yMagnify != 1)){ //stretch to panel size
			Graphics2D G = (Graphics2D)g;
			AffineTransform aftrans = new AffineTransform(xMagnify, 0, 0, yMagnify, 0, 0);
			G.drawImage(img, aftrans, this);
		}else{ //so you can see something but it may be wrong size
			g.drawImage(img, 0, 0, this);
		}
	}
	
	public static void main(String[] args){
		JFrame window = new JFrame("test");
		int w = 80, h = 60;
		IntBinaryOperator painter = (int y, int x)->{
			return 0xff000000 | Rand.weakRand.nextInt(0x1000000);
		};
		window.add(new StretchVideo(false, h, w, painter));
		window.setSize(800,600);
		ScreenUtil.moveToScreenCenter(window);
		window.setVisible(true);
		while(true){
			window.repaint();
			Time.sleepNoThrow(.01);
		}
	}

}
