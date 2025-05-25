package black0ut1.gui.static_;

import javax.swing.*;

public class GUI extends JFrame {
	
	public GUI(JPanel panel) {
		setContentPane(panel);
		pack();
		setVisible(true);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
}
