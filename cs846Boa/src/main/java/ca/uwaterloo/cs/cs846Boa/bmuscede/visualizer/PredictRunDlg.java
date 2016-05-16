package ca.uwaterloo.cs.cs846Boa.bmuscede.visualizer;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import ca.uwaterloo.cs.cs846Boa.bmuscede.network.NetworkMetrics.MachineLearning;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.PerformAnalysis;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.SocialNetworkBuilder;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.border.BevelBorder;
import javax.swing.JProgressBar;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.SwingConstants;
import javax.swing.ImageIcon;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class PredictRunDlg extends JDialog implements PerformAnalysis {

	private static final long serialVersionUID = 188694976776612013L;
	private static final String TITLE = "Performing Analysis on Project #";
	private JLabel lblTitle;
	private JLabel lblTitleDouble;
	private JProgressBar prgCurProj;
	private JLabel lblStepLabel;
	private JProgressBar prgTotalProj;
	private final JPanel contentPanel = new JPanel();
	private boolean single;

	
	/**
	 * Create the dialog.
	 * @param predict 
	 */
	public PredictRunDlg(String currentProject, 
			Frame parent, boolean singleMode, PredictDlg predict) {
		super(parent, "NetworkMine - Performing Bug Prediction", true);
		single = singleMode;
		
		setTitle("NetworkMine - Performing Bug Prediction");
		setResizable(false);
		setBounds(100, 100, 630, 352);
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		
		JPanel pnlDualMode = new JPanel();
		pnlDualMode.setBounds(0, 0, 624, 123);
		contentPanel.add(pnlDualMode);
		pnlDualMode.setLayout(null);
		
		JPanel pnlProj = new JPanel();
		pnlProj.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		pnlProj.setBounds(15, 65, 594, 42);
		pnlDualMode.add(pnlProj);
		pnlProj.setLayout(new BorderLayout(0, 0));
		
		prgTotalProj = new JProgressBar();
		pnlProj.add(prgTotalProj, BorderLayout.CENTER);
		
		lblTitleDouble = new JLabel("Starting Analysis...");
		lblTitleDouble.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitleDouble.setFont(new Font("Tahoma", Font.BOLD, 19));
		lblTitleDouble.setBounds(15, 16, 594, 33);
		pnlDualMode.add(lblTitleDouble);
		
		lblTitle = new JLabel("Starting Analysis...");
		lblTitle.setVerticalTextPosition(SwingConstants.BOTTOM);
	    lblTitle.setHorizontalTextPosition(SwingConstants.CENTER);
		lblTitle.setIcon(new ImageIcon(PredictRunDlg.class.getResource("/images/animals.png")));
		lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
		lblTitle.setFont(new Font("Tahoma", Font.PLAIN, 26));
		lblTitle.setBounds(15, 27, 594, 68);
		contentPanel.add(lblTitle);
		
		if (singleMode) {
			pnlDualMode.setVisible(false);
			pnlDualMode.setEnabled(false);
		}
		
		JPanel pnlProj2 = new JPanel();
		pnlProj2.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		pnlProj2.setBounds(15, 203, 594, 42);
		contentPanel.add(pnlProj2);
		pnlProj2.setLayout(new BorderLayout(0, 0));
		
		prgCurProj = new JProgressBar();
		pnlProj2.add(prgCurProj);
		
		JLabel lblTitle2 = new JLabel("Current Step:");
		lblTitle2.setFont(new Font("Tahoma", Font.BOLD, 19));
		lblTitle2.setBounds(15, 124, 594, 28);
		contentPanel.add(lblTitle2);
		
		lblStepLabel = new JLabel("Starting bug prediction process.");
		lblStepLabel.setHorizontalAlignment(SwingConstants.CENTER);
		lblStepLabel.setFont(new Font("Tahoma", Font.PLAIN, 22));
		lblStepLabel.setBounds(15, 153, 594, 42);
		contentPanel.add(lblStepLabel);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton cancelButton = new JButton("Cancel");
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						dispose();
					}
				});
				cancelButton.setActionCommand("Cancel");
				buttonPane.add(cancelButton);
			}
		}
		
		
		//Invokes the social network builder
		if (single){
			SocialNetworkBuilder.performAnalysis
				(this, currentProject, predict.getSaveLoc(),
						predict.getPrepassContrib(), predict.getPrepassFiles(), 
						predict.getMetrics(), 
						predict.getSpearman(), predict.getMLType(), 
						predict.getNumberSplits(), predict.getTrainingSize());
		} else {
			SocialNetworkBuilder.performAnalysisOnSome
				(this, predict.getSelected().split(","), predict.getSaveLoc(),
						predict.getPrepassContrib(), predict.getPrepassFiles(), 
						predict.getMetrics(), 
						predict.getSpearman(), predict.getMLType(), 
						predict.getNumberSplits(), predict.getTrainingSize());
		}
	}

	@Override
	public void informCurrentProj(String projID, int currentNum, int finalNum) {
		//Check if single mode.
		if (single){
			lblTitle.setText(TITLE + projID);
			return;
		}
		
		//Otherwise, we set both the title and progress bar.
		lblTitleDouble.setText(TITLE + projID);
		prgCurProj.setMaximum(finalNum);
		prgCurProj.setValue(currentNum);
	}

	@Override
	public void informCurrentStep(StepDescrip step) {
		//Simply print the step and update the progress bar.
		lblStepLabel.setText(step.toString());
		prgCurProj.setValue(step.getStepNum());
		prgCurProj.setMaximum(step.getTotal());
	}

	@Override
	public void informML(MachineLearning mlType, StepDescrip step) {
		//Now, we put the machine learning information.
		lblStepLabel.setText("Performing " + mlType.toString() + " on project.");
		prgTotalProj.setValue(step.getStepNum());
		prgTotalProj.setMaximum(step.getTotal());
	}
	
	@Override
	public void informComplete(){
		JOptionPane.showMessageDialog(null, "Success!\n"
	    		+ "Anaylsis complete for project(s).", "Success",
	    		JOptionPane.INFORMATION_MESSAGE);
		
		dispose();
	}
}
