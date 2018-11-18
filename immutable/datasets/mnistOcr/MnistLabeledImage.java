/** Ben F Rayfield offers this software opensource MIT license */
package immutable.datasets.mnistOcr;
import java.awt.Color;
import java.awt.Graphics;

public class MnistLabeledImage{
	
	public final byte pixels[][];
	
	public final byte label;
	
	public MnistLabeledImage(byte pixels[][], byte label){
		this.pixels = pixels;
		this.label = label;
	}
	
	/** todo optimize using BufferedImage instead of fillRect per pixel */
	public void paint(Graphics g, int y, int x){
		for(int yy=0; yy<pixels.length; yy++){
			for(int xx=0; xx<pixels.length; xx++){
				int b = pixels[yy][xx]&0xff;
				g.setColor(new Color(b,b,b));
				g.fillRect(x+xx, y+yy, 1, 1);
			}
		}
	}

}
