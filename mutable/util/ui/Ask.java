/** Ben F Rayfield offers this software opensource MIT license */
package mutable.util.ui;
import static mutable.util.Lg.*;
import javax.swing.JOptionPane;

public class Ask{
	
	public static void ok(String message){
		JOptionPane.showMessageDialog(null, message);
	}
	
	public static void main(String[] args){
		ok("test");
	}

}
