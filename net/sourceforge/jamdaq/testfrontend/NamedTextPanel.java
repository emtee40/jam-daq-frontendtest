/**
 * 
 */
package net.sourceforge.jamdaq.testfrontend;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

/**
 * @author Administrator
 * 
 */
public class NamedTextPanel extends JPanel {

	private transient final JLabel label = new JLabel();

	private transient String text;

	public NamedTextPanel(final String sname, final String init) {
		super();
		final Border border = BorderFactory
				.createEtchedBorder(EtchedBorder.RAISED);
		this.setBorder(BorderFactory.createTitledBorder(border, sname));
		this.setText(init);
		this.add(this.label);
	}

	public final void setText(final String text) {
		synchronized (label) {
			this.text = text;
			this.updateLabel();
		}
	}

	private void updateLabel() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				NamedTextPanel.this.label.setText(text);
			}
		});
	}

}
