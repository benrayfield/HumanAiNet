/** Ben F Rayfield offers this software opensource MIT license */
package mutable.rbm.ui;
import java.awt.image.BufferedImage;
import java.util.function.DoubleToIntFunction;

public class AiGraphics{
	
	public static BufferedImage newBufferedImageYX(int y, int x){
		return new BufferedImage(x, y, BufferedImage.TYPE_4BYTE_ABGR);
	}
	
	/** The DoubleToIntFunction chooses int ARGB color to write into BufferedImage, for each weight */
	public static void paintEdgeLayerOneNodePerPixel(float[][] edlay, BufferedImage paintMe, DoubleToIntFunction painter){
		int w = edlay.length, h = edlay[0].length;
		if(paintMe.getHeight() != h || paintMe.getWidth() != w) throw new Error("diff sizes");
		for(int lowNode=0; lowNode<w; lowNode++){
			for(int highNode=0; highNode<h; highNode++){
				paintMe.setRGB(lowNode, highNode, painter.applyAsInt(edlay[lowNode][highNode]));
			}
		}
	}

}
