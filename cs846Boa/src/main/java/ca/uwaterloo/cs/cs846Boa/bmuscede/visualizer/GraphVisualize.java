package ca.uwaterloo.cs.cs846Boa.bmuscede.visualizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import ca.uwaterloo.cs.cs846Boa.bmuscede.common.ContributionBuilder;
import ca.uwaterloo.cs.cs846Boa.bmuscede.mine.BoaProg;
import ca.uwaterloo.cs.cs846Boa.bmuscede.mine.LoginDlg;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.Actor;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.Commit;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.SocialNetworkBuilder;
import edu.uci.ics.jung.algorithms.layout.AbstractLayout;
import edu.uci.ics.jung.algorithms.layout.StaticLayout;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.UndirectedSparseGraph;
import edu.uci.ics.jung.visualization.VisualizationViewer;
import java.awt.Toolkit;

public class GraphVisualize {
	private JFrame fraVisualize;

	//GUI Variables
	private static final String BOA_SUCC = "Logged into Boa as:";
	private static final String BOA_NO_SUCC = "Not logged into Boa";
	private static final int SPACE = 5;
	private JLabel lblBoaStatus;
	private JButton btnAnalysis;
	private JButton btnPredict;
	
	//Graph Variables
	private String username;
	private String currentProject;
	private ContributionBuilder manager;
	private SocialNetworkBuilder builder;
	private Graph<Actor, Commit> network;
	private AbstractLayout<Actor, Commit> layout;
	private VisualizationViewer<Actor, Commit> graphViewer;
	private JPanel pnlGraph;
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					GraphVisualize window = new GraphVisualize();
					window.fraVisualize.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public GraphVisualize() {
		initialize();
		
		//Initializes the Boa Manager for use.
		while (true){
			LoginDlg login = new LoginDlg(fraVisualize);
			login.setVisible(true);
			
			//Checks if the user prompted to log in.
			if (login.getSuccess() == false) {
				JOptionPane.showMessageDialog(null, "You have chosen not "
						+ "to log in to Boa.\n"
			    		+ "Some features will not work until you log"
			    		+ "in.", "Boa Login",
			    		JOptionPane.INFORMATION_MESSAGE);
				manager = null;
				break;
			}
			
			//If so, we check if it went well.
			manager = new ContributionBuilder();
			if (manager.login(login.getUsername(), 
					login.getPassword())){
				username = login.getUsername();
				lblBoaStatus.setText(BOA_SUCC + " " + login.getUsername());
				break;
			} else {
				JOptionPane.showMessageDialog(null, "Login failed!\n"
			    		+ "Check your username and password.", "Boa Login",
			    		JOptionPane.ERROR_MESSAGE);
			}
		}		
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		//Initializes the social network builder.
		builder = new SocialNetworkBuilder();
		
		fraVisualize = new JFrame();
		fraVisualize.setIconImage(Toolkit.getDefaultToolkit().getImage(GraphVisualize.class.getResource("/transport.png")));
		fraVisualize.setMinimumSize(new Dimension(717, 500));
		fraVisualize.setTitle("NetworkMine");
		fraVisualize.setBounds(100, 100, 800, 500);
		fraVisualize.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		fraVisualize.setJMenuBar(menuBar);
		
		JButton btnLoad = new JButton("Load Project");
		btnLoad.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Loads the project.
				LoadDlg loadProject = new LoadDlg(fraVisualize);
				loadProject.setVisible(true);
				
				//Gets the results.
				if (loadProject.selectedProject == null) return;
				if (loadProject.selectedProject == LoadDlg.GET_MORE){
					//We first call on the BoaProj system.
					try {
						BoaProg window = (manager == null) ? 
								new BoaProg() : new BoaProg(username, manager);
								
						if (window.frmBoaMiner == null) return;		
						window.frmBoaMiner.setVisible(true);
					} catch (Exception e1) {
						e1.printStackTrace();
					}
					
					return;
				}
				
				//Otherwise, we load that project.
				currentProject = loadProject.selectedProject;
				fraVisualize.setTitle("NetworkMine - Project #"  + currentProject);
				
				//Builds the graph.
				layout = buildGraph(currentProject);
				graphViewer.setGraphLayout(layout);
				pnlGraph.repaint();
				
				//Enables the other buttons.
				btnAnalysis.setEnabled(true);
				btnPredict.setEnabled(true);
			}
		});
		btnLoad.setVerticalTextPosition(SwingConstants.BOTTOM);
	    btnLoad.setHorizontalTextPosition(SwingConstants.CENTER);
		btnLoad.setIcon(new ImageIcon(GraphVisualize.class.getResource("/images/technology.png")));
		menuBar.add(btnLoad);
		
		menuBar.add(Box.createHorizontalStrut(SPACE));
		
		btnAnalysis = new JButton("Perform Analysis");
		btnAnalysis.setEnabled(false);
		btnAnalysis.setVerticalTextPosition(SwingConstants.BOTTOM);
	    btnAnalysis.setHorizontalTextPosition(SwingConstants.CENTER);
		btnAnalysis.setIcon(new ImageIcon(GraphVisualize.class.getResource("/images/interface.png")));
		menuBar.add(btnAnalysis);
		
		btnPredict = new JButton("Predict Bugs");
		btnPredict.setEnabled(false);
		btnPredict.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Opens the prediction dialog.
				PredictDlg predict = new PredictDlg(currentProject, fraVisualize);
				predict.setVisible(true);
				
				//Next, we check for success.
				if (!predict.isSuccess()) return;
				
				//Now, if we're here, we have success.
				PredictRunDlg runDialog = new PredictRunDlg(
						currentProject, fraVisualize, true, predict);
				runDialog.setVisible(true);
			}
		});
		btnPredict.setVerticalTextPosition(SwingConstants.BOTTOM);
	    btnPredict.setHorizontalTextPosition(SwingConstants.CENTER);
		btnPredict.setIcon(new ImageIcon(GraphVisualize.class.getResource("/images/animals.png")));
		menuBar.add(btnPredict);
		
		menuBar.add(Box.createHorizontalStrut(SPACE));
		
		JButton btnNew = new JButton("Get New Projects");
		btnNew.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//We first call on the BoaProj system.
				try {
					BoaProg window = (manager == null) ? 
							new BoaProg() : new BoaProg(username, manager);
							
					if (window.frmBoaMiner == null) return;		
					window.frmBoaMiner.setVisible(true);
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}
		});
		btnNew.setVerticalTextPosition(SwingConstants.BOTTOM);
	    btnNew.setHorizontalTextPosition(SwingConstants.CENTER);
		btnNew.setIcon(new ImageIcon(GraphVisualize.class.getResource("/images/multimedia.png")));
		menuBar.add(btnNew);
		
		menuBar.add(Box.createHorizontalGlue());
		
		JButton btnPredictAll = new JButton("Predict Selected");
		btnPredictAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				//Opens the prediction dialog.
				PredictDlg predict = new PredictDlg(null, fraVisualize);
				predict.setVisible(true);
				
				//Next, we check for success.
				if (!predict.isSuccess()) return;
				
				//Now, if we're here, we have success.
				PredictRunDlg runDialog = new PredictRunDlg(
						null, fraVisualize, false, predict);
				runDialog.setVisible(true);
			}
		});
		btnPredictAll.setVerticalTextPosition(SwingConstants.BOTTOM);
		btnPredictAll.setHorizontalTextPosition(SwingConstants.CENTER);
		btnPredictAll.setIcon(new ImageIcon(GraphVisualize.class.getResource("/weather.png")));
		menuBar.add(btnPredictAll);
		
		menuBar.add(Box.createHorizontalStrut(SPACE));
		
		JButton btnAbout = new JButton("About");
		btnAbout.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				AboutDlg about = new AboutDlg(fraVisualize);
				about.setVisible(true);
			}
		});
		btnAbout.setVerticalTextPosition(SwingConstants.BOTTOM);
	    btnAbout.setHorizontalTextPosition(SwingConstants.CENTER);
		btnAbout.setIcon(new ImageIcon(GraphVisualize.class.getResource("/images/logo.png")));
		menuBar.add(btnAbout);
		fraVisualize.getContentPane().setLayout(new BorderLayout(0, 0));
		
		//Builds the graph.
		layout = buildGraph(null);
		
		graphViewer = new VisualizationViewer<Actor, Commit>(layout);
		pnlGraph = graphViewer;
		pnlGraph.setBackground(Color.WHITE);
		fraVisualize.getContentPane().add(pnlGraph, BorderLayout.CENTER);
		
		JPanel pnlStatus = new JPanel();
		pnlStatus.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		fraVisualize.getContentPane().add(pnlStatus, BorderLayout.SOUTH);
		pnlStatus.setLayout(new BorderLayout(0, 0));
		
		lblBoaStatus = new JLabel(BOA_NO_SUCC);
		lblBoaStatus.setFont(new Font("Tahoma", Font.BOLD, 16));
		pnlStatus.add(lblBoaStatus);
	}

	private AbstractLayout<Actor, Commit> buildGraph(String graphID) {
		//First, checks for null.
		if (graphID == null){
			network = new UndirectedSparseGraph<Actor, Commit>();
		} else {
			network = builder.buildSocialNetwork(graphID);
			
			//Check for error.
			if (network == null) {
				String message = "Invalid project ID was supplied.\n"
						+ "Maybe it was removed from the project?";
				JOptionPane.showMessageDialog(fraVisualize, message, ""
						+ "Error", JOptionPane.ERROR_MESSAGE);
				
				network = new UndirectedSparseGraph<Actor, Commit>();
			}
		}
		
		//TODO Find optimal layout
		return new StaticLayout<Actor, Commit>(network);
	}

}
