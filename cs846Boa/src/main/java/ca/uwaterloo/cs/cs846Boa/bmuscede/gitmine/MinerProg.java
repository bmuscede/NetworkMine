package ca.uwaterloo.cs.cs846Boa.bmuscede.gitmine;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextField;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.TitledBorder;

public class MinerProg {
	private JFrame frmGithubIssueMiner;
	private JTextField txtRepo;
	private JTextField txtOrg;
	private JLabel lblOrg;
	private GitMiner gitHubMiner;
	private final int BOX_LEN = 10;
	
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
		frmGithubIssueMiner.setResizable(false);
		frmGithubIssueMiner.setTitle("GitHub Issue Miner");
		frmGithubIssueMiner.setBounds(100, 100, 636, 400);
		frmGithubIssueMiner.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmGithubIssueMiner.getContentPane().setLayout(null);
		
		JLabel lblAuthenticate = new JLabel("Authenticate with GitHub:");
		lblAuthenticate.setBounds(15, 28, 215, 20);
		frmGithubIssueMiner.getContentPane().add(lblAuthenticate);
		
		JButton btnConnectWithGithub = new JButton("GitHub Login (Optional)");
		btnConnectWithGithub.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Gets user information.
				String[] userInfo = promptGitHubLogin();
				if (userInfo == null) return;
				
				//Attempts to log in.
				try {
					gitHubMiner = new GitMiner(userInfo[0], userInfo[1]);
				} catch (Exception e1) {
					JOptionPane.showMessageDialog
						(frmGithubIssueMiner, 
						"Login for " + userInfo[0] + " failed!", "GitHub Login Failed!",
						JOptionPane.ERROR_MESSAGE);
				}
				
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
			}
		});
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
		
		JPanel pnlIssues = new JPanel();
		pnlIssues.setBorder(new TitledBorder(null, "Issues Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlIssues.setBounds(15, 146, 600, 133);
		frmGithubIssueMiner.getContentPane().add(pnlIssues);
	}
	
	private String[] promptGitHubLogin() {
		//Sets up the password field.
        JPanel connectionPanel = new JPanel(false);
        connectionPanel.setBounds(100, 100, 528, 200);
        connectionPanel.setLayout(null);
        
	 	// Create the labels and text fields.
        JLabel lblUser = new JLabel("GitHub Username:");
		lblUser.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblUser.setBounds(15, 16, 162, 20);
		connectionPanel.add(lblUser);
		
		JTextField txtUser = new JTextField();
		txtUser.setFont(new Font("Tahoma", Font.PLAIN, 18));
		txtUser.setBounds(182, 14, 309, 26);
		connectionPanel.add(txtUser);
		txtUser.setColumns(10);
		
		JLabel lblPassword = new JLabel("GitHub Password");
		lblPassword.setFont(new Font("Tahoma", Font.PLAIN, 18));
		lblPassword.setBounds(15, 51, 162, 20);
		connectionPanel.add(lblPassword);
		
		JPasswordField txtPassword = new JPasswordField();
		txtPassword.setFont(new Font("Tahoma", Font.PLAIN, 18));
		txtPassword.setColumns(10);
		txtPassword.setBounds(182, 49, 309, 26);
		connectionPanel.add(txtPassword);
		
		String[] options = new String[]{"Login", "Cancel"};
		
		//Runs the dialog box.
		int option = JOptionPane.showOptionDialog(null, connectionPanel, "GitHub Login",
		                         JOptionPane.OK_CANCEL_OPTION,
		                         JOptionPane.PLAIN_MESSAGE,
		                         null, options, options[1]);
		
		//Checks if dialog is processed.
		if(option == 0){
			String[] values = {txtUser.getText(), 
					new String(txtPassword.getPassword())};
			return values;
		}
		
		return null;
	}
}
