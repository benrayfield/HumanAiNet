/** Ben F Rayfield offers this software opensource MIT license */
package mutable.games.mmgMouseAI.mouseRecorder.ui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import mutable.util.Time;
import mutable.util.inputqueue.UnlimitedInputQueue;
import mutable.util.ui.obvar.ObvarEditor;
import mutable.util.ui.obvar.ObvarEditorWrapper;

/** instantiated by ObvarEditor. (TODO allow viewing/editing on any chosen scale) Data ranges 0 to 1. */
@Deprecated //use CommandlineMouseYRecorder and ExperimentUtil dataset name mainMouseYBitsAppendedLongtermAs128BitVecs for the file it appends to
public class MouseYRecorderUi extends JPanel implements ObvarEditor{
	
	/** is a ByteState so can be saved in Obvars after editing in this ui */
	public final UnlimitedInputQueue data;
	
	/*What about immutable ByteStates such as RBM.java? How would they be edited? RBM.java uses a Var<RBM>, so how to connect that to ObvarEditor?
	SOLUTION: Create a mutable ByteState wrapper of immutable ByteState values. Maybe the wrapper implements Var?
	RbmEditor uses Var<RBM> event to know when to repaint. Should all obvars be Var<? extends ByteState>?
	No, that only makes sense for immutable ByteStates.
	*/
	
	protected double lastTime = Time.time();
	
	public MouseYRecorderUi(UnlimitedInputQueue data){
		this.data = data;
		addMouseMotionListener(new MouseMotionListener(){
			public void mouseDragged(MouseEvent e){ mouseMoved(e); }
			public void mouseMoved(MouseEvent e){
				double now = Time.time();
				double dt = now-lastTime;
				dt = Math.max(0, Math.min(dt, .05));
				double mouseYFraction = Math.max(0, Math.min(1-(double)e.getY()/(getHeight()-1), 1.));
				data.add(dt, mouseYFraction);
			}
		});
	}
	
	public void paint(Graphics g){
		g.setColor(Color.black);
		int w = getWidth(), h = getHeight();
		g.fillRect(0, 0, w, h);
		float add = h;
		float mult = -h; //If the data ranges 0 to 1
		int[] heights = new int[w];
		double timeSize = data.timeSize();
		for(int x=0; x<w; x++){
			double dt = timeSize*(-1+(float)x/(w-1)); //range -data.timeSize() to 0
			heights[x] = (int)(add+mult*data.applyAsDouble(dt)+.5f);
		}
		g.setColor(Color.blue);
		for(int x=1; x<w; x++){
			g.drawLine(x-1, heights[x-1], x, heights[x]);
		}
	}
	
	/*TODO the save/load/obvarname controls are in ObvarEditor which instantiates classes such as this one.
	
	protected final JTextField obvarTextfield;
	
	protected String obvar;

	public String obvar(){ return obvar; }

	public void setObvar(String obvar){
		if(!this.obvar.equals(obvar)){
			this.obvar = obvar;
		}
	}
	
	TODO some obvareditors will open more obvareditors for parts of an object
	which refer to eachother by obvarName.
	
	public MouseYRecorderUi(String firstObvar){
		super(new BorderLayout());
		this.obvar = firstObvar;
		obvarTextfield = new JTextField(firstObvar);
		obvarTextfield.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				TODO what to do with existing ob if its been modified?
				Also, dont want to copy it to new name just cuz I'm typing.
				
				Also, do I really want immutable objects? I guess I do in some cases like in sparsedoppler it would be inefficient
				to create a new array for every sound frame. Its inefficient of the cpu cache.
				This can support immutable and mutable objects, such as RBM.java is immutable.
				
				Also, I want this to be a general behavior of all obvareditors. The textfield and other save/load controls
				should be outside the editor itself.
				Which controls do I want?
				
				setObvar(obvarTextfield.getText());
			}
		});
		add(obvarTextfield,BorderLayout.NORTH);
		add(new JLabel("TODO object goes here"), BorderLayout.CENTER);
	}*/
	
}