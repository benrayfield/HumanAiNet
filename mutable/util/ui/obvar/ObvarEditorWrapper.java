/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util.ui.obvar;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import mutable.util.ByteState;
import mutable.util.Obvars;

/** OLD Text, todo rewrite:
A ui form normally has a textfield to type ObjectVar key, and another panel loads and edits that object
which may be a variety of types such as a recording of mouse movements or RBM editor,
and either a save button or a checkbox to autosave on some interval or (inefficiently) after every change.
This is only the editor controls. A type specific editor is loaded under those controls when users changes the text,
autosaving when load anotherone (unloading the previous) and maybe autosaving other times.
Think of this like a web browser for a few kinds of interactive AI research content that browsers dont normally support.
*/
public class ObvarEditorWrapper extends JPanel{
	
	protected final JTextField obvarTextfield;
	
	public String obvar(){
		return obvarTextfield.getText();
	}
	
	public void setObvar(String obvar){
		obvarTextfield.setText(obvar);
		ByteState ob = Obvars.get(obvar);
		if(ob != null){
			
		}
	}
	
	public ObvarEditorWrapper(String firstObvar){
		super(new BorderLayout());
		obvarTextfield = new JTextField(firstObvar);
		obvarTextfield.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e){
				//TODO what to do with existing ob if its been modified?
				//Also, dont want to copy it to new name just cuz I'm typing.
				
				setObvar(obvarTextfield.getText());
			}
		});
		add(obvarTextfield,BorderLayout.NORTH);
		add(new JLabel("TODO object goes here"), BorderLayout.CENTER);
		setObvar(firstObvar);
	}
	

}
