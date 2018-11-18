/** Ben F Rayfield offers this software opensource MIT license */
package mutable.rbm.ui;
import static mutable.util.Lg.*;
import java.awt.Color;
import java.awt.Component;
import java.util.function.Consumer;

import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import mutable.compilers.JavaCompilers;
import immutable.learnloop.LearnLoop;
import immutable.learnloop.RBM;
import mutable.rbm.func.LearnLoopParam_OLD;
import mutable.util.Var;

public class LearnFuncRbmEditor extends JTextArea{
	
	public final Var<RBM> varRbm;
	
	protected final Consumer<Var> listener;
	
	protected boolean ignoreOnChange;
	
	public LearnFuncRbmEditor(Var<RBM> varRbm){
		this.varRbm = varRbm;
		setText(varRbm.get().learnFunc);
		varRbm.startListening(listener = (Var var)->{
			/*TODO in case something externally changes the RBM.learnFunc (such as quickloading another RBM using number buttons)
			but this is causing ui problem
			ignoreOnChange = true;
			String code = varRbm.get().learnFunc;
			if(!code.equals(getText())){
				setText(code);
			}
			ignoreOnChange = false;
			*/
		});
		getDocument().addDocumentListener(new DocumentListener(){
			public void insertUpdate(DocumentEvent e){
				LearnFuncRbmEditor.this.onChange();
			}
			public void removeUpdate(DocumentEvent e){
				LearnFuncRbmEditor.this.onChange();
			}

			public void changedUpdate(DocumentEvent e){
				LearnFuncRbmEditor.this.onChange();
			}
			
		});
	}
	
	protected void finalize() throws Throwable{
		varRbm.stopListening(listener);
	}
	
	protected void onChange(){
		if(ignoreOnChange) return;
		String editingCode = getText();
		lg("learnFunc textarea: "+editingCode);
		String code = varRbm.get().learnFunc;
		Color pass = Color.WHITE, fail = new Color(.8f, .8f, .8f);
		if(!code.equals(editingCode)){
			try{
				//LearnLoopParam_OLD.compileSandboxed(editingCode);
				LearnLoop.compileSandboxed(editingCode, JavaCompilers.get(false, false));
				lg("learnFunc textarea COMPILED");
				setBackground(pass);
				varRbm.set(varRbm.get().setLearnFunc(editingCode));
			}catch(Throwable t){
				lg("learnFunc textarea FAIL (on CODE: "+code+") cuz "+t);
				setBackground(fail);
				//lgErr(t);
			}
		}else{
			lg("learnFunc textarea NO CHANGE.");
			setBackground(pass);
		}
		lg("learnFunc textarea END");
	}

}
