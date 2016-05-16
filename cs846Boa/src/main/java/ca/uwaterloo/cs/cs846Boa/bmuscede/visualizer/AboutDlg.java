package ca.uwaterloo.cs.cs846Boa.bmuscede.visualizer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ImageIcon;

public class AboutDlg extends JDialog {

	private static final long serialVersionUID = 1287929201576444418L;
	private final JPanel contentPanel = new JPanel();

	/**
	 * Create the dialog.
	 */
	public AboutDlg(Frame parent) {
		super(parent, "About NetworkMine", true);
		
		setResizable(false);
		setTitle("About NetworkMine");
		setBounds(100, 100, 560, 389);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		
		JLabel lblTitle = new JLabel("NetworkMine - Software Analysis Tool");
		lblTitle.setVerticalTextPosition(SwingConstants.BOTTOM);
	    lblTitle.setHorizontalTextPosition(SwingConstants.CENTER);
		lblTitle.setIcon(new ImageIcon(AboutDlg.class.getResource("/transport.png")));
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitle.setFont(new Font("Tahoma", Font.BOLD, 24));
		lblTitle.setBounds(15, 0, 524, 173);
		contentPanel.add(lblTitle);
		
		JPanel pnlDescript = new JPanel();
		pnlDescript.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		pnlDescript.setBounds(15, 176, 524, 118);
		contentPanel.add(pnlDescript);
		pnlDescript.setLayout(null);
		
		JLabel lblallowsUsersTo = new JLabel("<html><center>Allows users "
				+ "to download projects from Iowa State's Boa project and "
				+ "analyze the projects for <i>\"bug prone\"</i> files.\r\n<br><br>"
				+ "Created by Bryan J Muscedere and Michael Godfrey at the "
				+ "University of Waterloo in 2016.</center></html>");
		lblallowsUsersTo.setBounds(15, 0, 494, 118);
		pnlDescript.add(lblallowsUsersTo);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("OK");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
		}
	}
}
