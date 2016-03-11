package ca.uwaterloo.cs.cs846Boa.bmuscede.mine;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import ca.uwaterloo.cs.cs846Boa.bmuscede.common.ContributionBuilder;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.JSeparator;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.SpinnerNumberModel;
import ca.uwaterloo.cs.cs846Boa.bmuscede.common.FinishedCallback;

public class BoaProg implements FinishedCallback {
	private ContributionBuilder contribution;
	private JFrame frmBoaMiner;
	private static BoaProg window;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {					
					window = new BoaProg();
					window.frmBoaMiner.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public BoaProg() {
		//Start by prompting the user for a login.
		contribution = new ContributionBuilder();
		
		boolean notLoggedIn = true;
		LoginDlg login = null;
		while(notLoggedIn){
			login = new LoginDlg(null);
			login.setVisible(true);
			
			//Wait for success.
			if (login.getSuccess() == false) System.exit(0);
			
			//Now we try to log into Boa with this.
			if (contribution.login(
					login.getUsername(), login.getPassword())){
				notLoggedIn = false;
			}
		}
		
		initialize(login.getUsername());
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize(String username) {
		frmBoaMiner = new JFrame();
		frmBoaMiner.setResizable(false);
		frmBoaMiner.setTitle("Boa Miner");
		frmBoaMiner.setBounds(100, 100, 539, 317);
		frmBoaMiner.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmBoaMiner.getContentPane().setLayout(null);
		
		JLabel lblDataset = new JLabel("Run on Dataset:");
		lblDataset.setBounds(15, 19, 117, 20);
		frmBoaMiner.getContentPane().add(lblDataset);
		
		//We pull the dataset information.
		String[] datasets  = contribution.getDatasets();
		JComboBox<String> cmbDataset = new JComboBox<String>(/*datasets*/);
		cmbDataset.setBounds(142, 16, 376, 26);
		frmBoaMiner.getContentPane().add(cmbDataset);
		
		JPanel pnlCandidate = new JPanel();
		pnlCandidate.setBorder(new TitledBorder(null, "Candidate Project Setup", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlCandidate.setBounds(15, 55, 503, 124);
		frmBoaMiner.getContentPane().add(pnlCandidate);
		pnlCandidate.setLayout(null);
		
		JLabel lblNumFile = new JLabel("Minimum Number of Files:");
		lblNumFile.setBounds(66, 38, 246, 20);
		pnlCandidate.add(lblNumFile);
		
		JSpinner spnFiles = new JSpinner();
		spnFiles.setModel(new SpinnerNumberModel(0, 0, 100000, 1));
		spnFiles.setBounds(327, 35, 91, 26);
		pnlCandidate.add(spnFiles);
		
		JLabel lblNumContrib = new JLabel("Minimum Number of Contributors:");
		lblNumContrib.setBounds(66, 74, 246, 20);
		pnlCandidate.add(lblNumContrib);
		
		JSpinner spnContrib = new JSpinner();
		spnContrib.setModel(new SpinnerNumberModel(0, 0, 100000, 1));
		spnContrib.setBounds(327, 71, 91, 26);
		pnlCandidate.add(spnContrib);
		
		JButton btnFind = new JButton("Find Projects");
		btnFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Runs the Boa script that finds projects
				//of this nature.
				contribution.getProjects(window, 
						(int) spnContrib.getValue(), 
						(int) spnFiles.getValue(),
						(String) cmbDataset.getSelectedItem());
			}
		});
		btnFind.setBounds(339, 183, 179, 49);
		frmBoaMiner.getContentPane().add(btnFind);
		
		JButton btnClose = new JButton("Close");
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		btnClose.setBounds(15, 183, 179, 49);
		frmBoaMiner.getContentPane().add(btnClose);
		
		JSeparator sepBottom = new JSeparator();
		sepBottom.setBounds(15, 240, 503, 2);
		frmBoaMiner.getContentPane().add(sepBottom);
		
		JLabel lblUserInfo = new JLabel("Logged into Boa as: " + username);
		lblUserInfo.setHorizontalAlignment(SwingConstants.CENTER);
		lblUserInfo.setFont(new Font("Tahoma", Font.BOLD, 16));
		lblUserInfo.setBounds(15, 248, 503, 20);
		frmBoaMiner.getContentPane().add(lblUserInfo);
	}

	@Override
	public void onFinish(boolean result) {
		//The execution finished.
	}
}
