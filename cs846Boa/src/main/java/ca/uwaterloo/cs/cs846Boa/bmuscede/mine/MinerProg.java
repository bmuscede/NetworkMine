package ca.uwaterloo.cs.cs846Boa.bmuscede.mine;

import java.awt.EventQueue;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import org.kohsuke.github.GHIssueState;

import ca.uwaterloo.cs.cs846Boa.bmuscede.mine.GitMiner.StorageType;

import java.awt.Font;
import javax.swing.SwingConstants;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import java.awt.Toolkit;

public class MinerProg {
	private JFrame frmGithubIssueMiner;
	private JTextField txtRepo;
	private JTextField txtOrg;
	private JLabel lblOrg;
	private GitMiner gitHubMiner;
	private JComboBox<GHIssueState> cmbType;
	private JComboBox<StorageType> cmbStore;
	private JLabel lblBottom;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MinerProg window = new MinerProg();
					window.frmGithubIssueMiner.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public MinerProg() {
		initialize();
	}
	
	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		//Initalizes the GitHubMiner to null.
		gitHubMiner = null;
		
		frmGithubIssueMiner = new JFrame();
		frmGithubIssueMiner.setIconImage(Toolkit.getDefaultToolkit().getImage(MinerProg.class.getResource("/transport.png")));
		frmGithubIssueMiner.setResizable(false);
		frmGithubIssueMiner.setTitle("GitHub Issue Miner");
		frmGithubIssueMiner.setBounds(100, 100, 636, 400);
		frmGithubIssueMiner.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmGithubIssueMiner.getContentPane().setLayout(null);
		
		JPanel pnlWait = new JPanel();
		pnlWait.setVisible(false);
		pnlWait.setBounds(0, 0, 630, 360);
		frmGithubIssueMiner.getContentPane().add(pnlWait);
		pnlWait.setLayout(null);
		
		JLabel lblWait = new JLabel("Please Wait!");
		lblWait.setHorizontalAlignment(SwingConstants.CENTER);
		lblWait.setFont(new Font("Tahoma", Font.PLAIN, 45));
		lblWait.setBounds(15, 113, 600, 54);
		pnlWait.add(lblWait);
		
		lblBottom = new JLabel("Mining issues for the repository <ORG>/<REPO>");
		lblBottom.setHorizontalAlignment(SwingConstants.CENTER);
		lblBottom.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblBottom.setBounds(15, 170, 600, 54);
		pnlWait.add(lblBottom);
		
		JLabel lblAuthenticate = new JLabel("Authenticate with GitHub:");
		lblAuthenticate.setBounds(15, 28, 215, 20);
		frmGithubIssueMiner.getContentPane().add(lblAuthenticate);
		
		JLabel lblUser = new JLabel("Logged in as <USERNAME>");
		lblUser.setVisible(false);
		lblUser.setBounds(245, 28, 370, 20);
		frmGithubIssueMiner.getContentPane().add(lblUser);
		
		JButton btnConnectWithGithub = new JButton("GitHub Login (Optional)");
		btnConnectWithGithub.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Gets user information.
				LoginDlg login = new LoginDlg(frmGithubIssueMiner);
                login.setVisible(true);
                
                //Check whether it succeeded.
                if (login.getSuccess() == false)
					return;
                
                //Otherwise, we get the info.
                String username = login.getUsername();
                String password = login.getPassword();
				
				//Attempts to log in.
				try {
					gitHubMiner = new GitMiner(username, password);
				} catch (Exception e1) {
					JOptionPane.showMessageDialog
						(frmGithubIssueMiner, 
						"Login for " + username + " failed!", "GitHub Login Failed!",
						JOptionPane.ERROR_MESSAGE);
					gitHubMiner = null;
					return;
				}
				//Now, removes the button and replaces it with username.
				btnConnectWithGithub.setVisible(false);
				lblUser.setText("Logged in as " + username + " (" +
						gitHubMiner.getRateLim() + ")");
				lblUser.setVisible(true);
			}
		});
		btnConnectWithGithub.setBounds(245, 24, 370, 29);
		frmGithubIssueMiner.getContentPane().add(btnConnectWithGithub);
		
		lblOrg = new JLabel("Enter the organization name:");
		lblOrg.setBounds(15, 66, 215, 20);
		frmGithubIssueMiner.getContentPane().add(lblOrg);
		
		txtOrg = new JTextField();
		txtOrg.setBounds(245, 63, 370, 26);
		frmGithubIssueMiner.getContentPane().add(txtOrg);
		txtOrg.setColumns(10);
		
		JLabel lblRepo = new JLabel("Enter repository name:");
		lblRepo.setBounds(15, 108, 215, 20);
		frmGithubIssueMiner.getContentPane().add(lblRepo);
		
		txtRepo = new JTextField();
		txtRepo.setBounds(245, 105, 370, 26);
		frmGithubIssueMiner.getContentPane().add(txtRepo);
		txtRepo.setColumns(10);
		
		JButton btnMine = new JButton("Mine!");
		btnMine.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//First, perform an error check.
				if (txtOrg.getText().equals("") || 
						txtRepo.getText().equals("")){
					JOptionPane.showMessageDialog
						(frmGithubIssueMiner, 
						 "No organization or repo is supplied!", "Failure",
						 JOptionPane.ERROR_MESSAGE);
					return;
				}
				
				//Sets up the git miner object.
				if (gitHubMiner == null){
					//We set the org and repo to mine from.
					try {
						gitHubMiner = new GitMiner(new String[]{txtOrg.getText()}, 
								new String[]{txtRepo.getText()});
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				} else {
					//We set the org and repo.
					try {
						gitHubMiner.setNames(new String[]{txtOrg.getText()}, 
									new String[]{txtRepo.getText()});
					} catch (Exception e1) {
						e1.printStackTrace();
					}
				}
				
				//Next we get the issues flag and storage type.
				GHIssueState type = (GHIssueState) cmbType.getSelectedItem();
				StorageType storage = (StorageType) cmbStore.getSelectedItem();
				
				//Mine for issues.
				pnlWait.setVisible(true);
				lblBottom.setText("Mining issues for " + txtOrg.getText() + "/" +
						txtRepo.getText());
				gitHubMiner.mineAllIssueData(type);
				
				//Store the data.
				gitHubMiner.storeAllData(storage);
				
				//Now we're done.
				txtOrg.setText("");
				txtRepo.setText("");
				pnlWait.setVisible(false);
			}
		});
		
		JPanel pnlIssues = new JPanel();
		pnlIssues.setBorder(new TitledBorder(null, "Issues Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlIssues.setBounds(15, 146, 600, 133);
		frmGithubIssueMiner.getContentPane().add(pnlIssues);
		pnlIssues.setLayout(null);
		
		JLabel lblType = new JLabel("Set Issue Type:");
		lblType.setBounds(15, 40, 153, 20);
		pnlIssues.add(lblType);
		
		cmbType = new JComboBox<GHIssueState>();
		cmbType.setBounds(166, 37, 419, 26);
		cmbType.setModel(new DefaultComboBoxModel<GHIssueState>(GHIssueState.values()));
		pnlIssues.add(cmbType);
		
		JLabel lblStore = new JLabel("Set Storage Type:");
		lblStore.setBounds(15, 86, 162, 20);
		pnlIssues.add(lblStore);
		
		cmbStore = new JComboBox<StorageType>();
		cmbStore.setModel(new DefaultComboBoxModel<StorageType>(StorageType.values()));
		cmbStore.setBounds(166, 83, 419, 26);
		pnlIssues.add(cmbStore);
		btnMine.setBounds(455, 295, 160, 49);
		frmGithubIssueMiner.getContentPane().add(btnMine);
		
		JButton btnClose = new JButton("Close");
		btnClose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		btnClose.setBounds(15, 295, 160, 49);
		frmGithubIssueMiner.getContentPane().add(btnClose);
	}
}
