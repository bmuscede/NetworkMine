package ca.uwaterloo.cs.cs846Boa.bmuscede.mine;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;
import ca.uwaterloo.cs.cs846Boa.bmuscede.common.ContributionBuilder;
import javax.swing.JSpinner;
import javax.swing.JSeparator;
import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.SpinnerNumberModel;
import ca.uwaterloo.cs.cs846Boa.bmuscede.common.FinishedCallback;
import javax.swing.JScrollPane;
import javax.swing.ImageIcon;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class BoaProg implements FinishedCallback {
	private ContributionBuilder contribution;
	private JFrame frmBoaMiner;
	private JTable tblProj;
	private JPanel pnlWait;
	private JPanel pnlStage2;
	private JLabel lblTitle;
	private JLabel lblNoProjects;
	private JLabel lblWait2;
	private JComboBox<String> cmbDataset;
	private JSpinner spnContrib;
	private JSpinner spnFiles;
	private JButton btnClose;
	private JButton btnFind;
	private static BoaProg window;
	
	private final String FIRST_MESSAGE =
			"<html><center>Getting projects "
			+ "that have more than<br>NUM contributors and NUM files.</center></html>";
	private final String SECOND_MESSAGE = 
			"<html><center>Currently mining "
			+ "contribution network for<br>project ID #NUM.</center></html>";
	private final String TITLE_TEXT = 
			"Projects With More Than <NUM> Contributors and <NUM> Files";
	
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
			} else {
				JOptionPane.showMessageDialog(null, "Login failed!\n"
			    		+ "Check your username and password.", "Boa Login",
			    		JOptionPane.ERROR_MESSAGE);
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
		
		pnlWait = new JPanel();
		pnlWait.setVisible(false);
		pnlWait.setBounds(0, 0, 533, 238);
		frmBoaMiner.getContentPane().add(pnlWait);
		pnlWait.setLayout(null);
		
		JLabel lblIcon = new JLabel("");
		lblIcon.setHorizontalAlignment(SwingConstants.CENTER);
		lblIcon.setIcon(new ImageIcon(
				BoaProg.class.getResource("/com/sun/java/swing/plaf/windows/icons/Inform.gif")));
		lblIcon.setBounds(15, 55, 503, 32);
		pnlWait.add(lblIcon);
		
		JLabel lblWait1 = new JLabel("Please Wait!");
		lblWait1.setHorizontalAlignment(SwingConstants.CENTER);
		lblWait1.setFont(new Font("Tahoma", Font.PLAIN, 26));
		lblWait1.setBounds(15, 103, 503, 20);
		pnlWait.add(lblWait1);
		
		lblWait2 = new JLabel("");
		lblWait2.setHorizontalAlignment(SwingConstants.CENTER);
		lblWait2.setFont(new Font("Tahoma", Font.PLAIN, 26));
		lblWait2.setBounds(15, 139, 503, 54);
		pnlWait.add(lblWait2);
		
		pnlStage2 = new JPanel();
		pnlStage2.setVisible(false);
		pnlStage2.setBounds(0, 0, 533, 238);
		frmBoaMiner.getContentPane().add(pnlStage2);
		pnlStage2.setLayout(null);
		
		lblTitle = new JLabel("Projects With More Than <NUM> Contributors and <NUM> Files");
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitle.setBounds(15, 16, 503, 20);
		pnlStage2.add(lblTitle);
		
		lblNoProjects = new JLabel("No Projects Found!");
		lblNoProjects.setHorizontalAlignment(SwingConstants.CENTER);
		lblNoProjects.setBounds(15, 101, 503, 20);
		lblNoProjects.setVisible(false);
		pnlStage2.add(lblNoProjects);
		
		JScrollPane scrProj = new JScrollPane();
		scrProj.setBounds(15, 42, 503, 118);
		pnlStage2.add(scrProj);
		
		tblProj = new JTable();
		tblProj.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"Project ID", "Project Name", "Contributors", "Files"
			}
		));
		tblProj.getColumnModel().getColumn(0).setPreferredWidth(202);
		tblProj.getColumnModel().getColumn(1).setPreferredWidth(189);
		scrProj.setViewportView(tblProj);
		
		JButton btnBuild = new JButton("Build Network(s)");
		btnBuild.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//First, we get selected rows.
				int[] rows = tblProj.getSelectedRows();
				if (rows.length == 0){
				    JOptionPane.showMessageDialog(window.frmBoaMiner, "You must"
				    		+ " select at least one project.", "Error",
				    		JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				//Sets up the wait screen.
				lblWait2.setText("Starting to mine...");
				pnlStage2.setVisible(false);
				pnlWait.setVisible(true);
				
				//Gets selected rows.
				String[] ids = new String[rows.length];
				for (int i = 0; i < rows.length; i++){
					ids[i] = (String) tblProj.getModel()
							.getValueAt(rows[i], 0);
				}
				
				//We now run our program to mine.
				contribution.buildContributionNetwork(window, ids);
			}
		});
		btnBuild.setBounds(348, 176, 170, 45);
		pnlStage2.add(btnBuild);
		
		JButton btnClose2 = new JButton("Close");
		btnClose2.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		btnClose2.setBounds(15, 176, 170, 45);
		pnlStage2.add(btnClose2);
		
		JLabel lblDataset = new JLabel("Run on Dataset:");
		lblDataset.setBounds(15, 19, 117, 20);
		frmBoaMiner.getContentPane().add(lblDataset);
		
		//We pull the dataset information.
		String[] datasets  = contribution.getDatasets();
		cmbDataset = new JComboBox<String>(datasets);
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
		
		spnFiles = new JSpinner();
		spnFiles.setModel(new SpinnerNumberModel(0, 0, 100000, 1));
		spnFiles.setBounds(327, 35, 91, 26);
		pnlCandidate.add(spnFiles);
		
		JLabel lblNumContrib = new JLabel("Minimum Number of Contributors:");
		lblNumContrib.setBounds(66, 74, 246, 20);
		pnlCandidate.add(lblNumContrib);
		
		spnContrib = new JSpinner();
		spnContrib.setModel(new SpinnerNumberModel(0, 0, 100000, 1));
		spnContrib.setBounds(327, 71, 91, 26);
		pnlCandidate.add(spnContrib);
		
		btnFind = new JButton("Find Projects");
		btnFind.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				manageFirstPhase(false);
				
				//Sets up the wait screen.
				String message = FIRST_MESSAGE;
				message = message.replaceFirst("NUM", 
						String.valueOf((int)spnContrib.getValue()));
				message = message.replaceFirst("NUM", 
						String.valueOf((int)spnFiles.getValue()));
				lblWait2.setText(message);
				pnlWait.setVisible(true);
				
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
		
		btnClose = new JButton("Close");
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
	public void onProjectFindFinish(boolean result) {
		if (result = false){
		    JOptionPane.showMessageDialog(this.frmBoaMiner, "There was a problem"
		    		+ "finding projects.", "Error",
		    		JOptionPane.ERROR_MESSAGE);
		    
			manageFirstPhase(true);
			pnlWait.setVisible(false);
			return;
		}
		
		String title = TITLE_TEXT;
		title = title.replaceFirst("<NUM>", String.valueOf(spnContrib.getValue()))
				.replaceFirst("<NUM>", String.valueOf(spnFiles.getValue()));
		lblTitle.setText(title);
		
		//We collect the projects.
		String[] projects = null;
		try {
			projects = contribution.collectProjects();
		} catch (Exception e) {
			lblNoProjects.setVisible(true);
			pnlStage2.setVisible(true);
			pnlWait.setVisible(false);
			return;
		}
		
		//Populate the rows.
		DefaultTableModel model = (DefaultTableModel) tblProj.getModel();
		for (int i = 0; i < projects.length; i++){
			String[] curProject = projects[i].split(",");
			model.addRow(new Object[]{curProject[0], 
					curProject[1], curProject[2], curProject[3]});
		}
		
		//Now, we remove the wait screen and set our current screen to visible.
		pnlStage2.setVisible(true);
		pnlWait.setVisible(false);
	}
	
	@Override
	public void onNetworkFinish(boolean result) {
		// TODO Do something different here.
		System.out.println("DONE!");
		System.exit(0);
	}

	@Override
	public void informCurrentMine(String project) {
		//Print the project being mined.
		String mineTitle = SECOND_MESSAGE;
		mineTitle = mineTitle.replace("NUM", project);
		lblWait2.setText(mineTitle);
	}
	
	private void manageFirstPhase(boolean enable){
		btnClose.setEnabled(enable);
		btnFind.setEnabled(enable);
		spnContrib.setEnabled(enable);
		spnFiles.setEnabled(enable);
		cmbDataset.setEnabled(enable);
		btnClose.setVisible(enable);
		btnFind.setVisible(enable);
		spnContrib.setVisible(enable);
		spnFiles.setVisible(enable);
		cmbDataset.setVisible(enable);
	}
}
