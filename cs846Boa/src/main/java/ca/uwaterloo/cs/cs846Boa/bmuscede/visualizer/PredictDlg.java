package ca.uwaterloo.cs.cs846Boa.bmuscede.visualizer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultFormatter;

import ca.uwaterloo.cs.cs846Boa.bmuscede.network.NetworkMetrics.MachineLearning;
import ca.uwaterloo.cs.cs846Boa.bmuscede.network.NetworkMetrics.SocialMetrics;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class PredictDlg extends JDialog {

	private static final long serialVersionUID = -8048453853148481309L;
	private final JPanel contentPanel = new JPanel();
	private JCheckBox chkPreContrib;
	private JCheckBox chkPreFiles;
	@SuppressWarnings("rawtypes")
	private JList lstMetrics;
	private JCheckBox chkSpearman;
	@SuppressWarnings("rawtypes")
	private JComboBox cmbML;
	private JSpinner spnSplits;
	private JSpinner spnTrain;
	
	private boolean success = false;
	private boolean preContrib = false;
	private boolean preFiles = false;
	private boolean spearman = false;
	private int numIter = 0;
	private int trainingSize = 0;
	private MachineLearning mlType;
	private ArrayList<SocialMetrics> metrics;
	private JTextField txtLoc;
	
	private final static String DEFAULT_NAME = "output.csv";
	private final static String DB_LOC = "data/boa.db";
	private final static int TIMEOUT = 30;
	
	private JTable tblProjects;
	
	public boolean getPrepassContrib(){
		return preContrib;
	}
	public boolean getPrepassFiles(){
		return preFiles;
	}
	public boolean getSpearman(){
		return spearman;
	}
	public int getNumberSplits(){
		return numIter;
	}
	public int getTrainingSize(){
		return trainingSize;
	}
	public MachineLearning getMLType(){
		return mlType;
	}
	public ArrayList<SocialMetrics> getMetrics(){
		return metrics;
	}
	public String getSaveLoc(){
		return txtLoc.getText();
	}
	public String getSelected(){
		String selectedIDs = "";
		for (int current : tblProjects.getSelectedRows()){
			selectedIDs += tblProjects.getModel().getValueAt(current, 0).toString() + ",";
		}

		return selectedIDs.substring(0, selectedIDs.length()-1);
	}
	public boolean isSuccess() {
		return success;
	}

	/**
	 * Create the dialog.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public PredictDlg(String currentProject, Frame parent) {
		super(parent, "NetworkMine - Predict Bugs", true);
		
		setResizable(false);
		setTitle("NetworkMine - Predict Bugs");
		setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		if (currentProject == null){
			setBounds(100, 100, 890, 640);
		} else {
			setBounds(100, 100, 530, 640);
		}
		getContentPane().setLayout(new BorderLayout());
		contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		getContentPane().add(contentPanel, BorderLayout.CENTER);
		contentPanel.setLayout(null);
		{
			JLabel lblTitle = new JLabel();
			if (currentProject == null){
				lblTitle.setText("Predict Bugs - Selected Projects");
				lblTitle.setBounds(15, 16, 854, 33);
			} else {
				lblTitle.setText("Predict Bugs - Project #" + currentProject);
				lblTitle.setBounds(15, 16, 492, 33);
			}
			lblTitle.setHorizontalAlignment(SwingConstants.CENTER);
			lblTitle.setFont(new Font("Tahoma", Font.PLAIN, 20));
			contentPanel.add(lblTitle);
		}
		{
			JPanel pnlStep1 = new JPanel();
			pnlStep1.setBorder(new TitledBorder(null, "Initalization Settings", TitledBorder.LEADING, TitledBorder.TOP, null, null));
			pnlStep1.setBounds(15, 63, 492, 111);
			contentPanel.add(pnlStep1);
			pnlStep1.setLayout(null);
			
			chkPreContrib = new JCheckBox("Remove Outlier Contributors");
			chkPreContrib.setEnabled(false);
			chkPreContrib.setBounds(11, 32, 235, 29);
			pnlStep1.add(chkPreContrib);
			
			chkPreFiles = new JCheckBox("Remove Unnecessary Files");
			chkPreFiles.setEnabled(false);
			chkPreFiles.setBounds(11, 67, 219, 29);
			pnlStep1.add(chkPreFiles);
		}
		
		JPanel pnlStep2 = new JPanel();
		pnlStep2.setBorder(new TitledBorder(null, "Social Network Metrics", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlStep2.setBounds(15, 190, 492, 111);
		contentPanel.add(pnlStep2);
		pnlStep2.setLayout(new BorderLayout(0, 0));
		
		JScrollPane scrollPane = new JScrollPane();
		pnlStep2.add(scrollPane, BorderLayout.CENTER);
		
		lstMetrics = new JList();
		lstMetrics.setModel(new DefaultComboBoxModel(SocialMetrics.values()));
		scrollPane.setViewportView(lstMetrics);
		
		JPanel pnlStep3 = new JPanel();
		pnlStep3.setBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"), "Test Settings", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		pnlStep3.setBounds(15, 317, 492, 189);
		contentPanel.add(pnlStep3);
		pnlStep3.setLayout(null);
		
		chkSpearman = new JCheckBox("Compute Spearman Correlation for Metrics");
		chkSpearman.setSelected(true);
		chkSpearman.setBounds(11, 32, 470, 29);
		pnlStep3.add(chkSpearman);
		
		JLabel lblML = new JLabel("Machine Learning Test:");
		lblML.setBounds(11, 73, 182, 20);
		pnlStep3.add(lblML);
		
		cmbML = new JComboBox<String>();
		cmbML.setModel(new DefaultComboBoxModel(MachineLearning.values()));
		cmbML.setBounds(190, 70, 287, 26);
		pnlStep3.add(cmbML);
		
		JLabel lblSplits = new JLabel("Number of Splits:");
		lblSplits.setBounds(11, 109, 182, 20);
		pnlStep3.add(lblSplits);
		
		spnSplits = new JSpinner();
		spnSplits.setModel(new SpinnerNumberModel(new Integer(100), new Integer(1), null, new Integer(1)));
		spnSplits.setBounds(190, 106, 287, 26);
		pnlStep3.add(spnSplits);
		
		JLabel lblTrain = new JLabel("Training Size (%):");
		lblTrain.setBounds(11, 145, 142, 20);
		pnlStep3.add(lblTrain);
		
		spnTrain = new JSpinner();
		spnTrain.setModel(new SpinnerNumberModel(60, 1, 99, 1));
		spnTrain.setBounds(151, 142, 67, 26);
		pnlStep3.add(spnTrain);
		
		JSpinner spnTest = new JSpinner();
		spnTest.setModel(new SpinnerNumberModel(40, 1, 99, 1));
		spnTest.setBounds(410, 142, 67, 26);
		pnlStep3.add(spnTest);
		
	    JComponent comp = spnTest.getEditor();
	    JFormattedTextField field = (JFormattedTextField) comp.getComponent(0);
	    DefaultFormatter formatter = (DefaultFormatter) field.getFormatter();
	    formatter.setCommitsOnValidEdit(true);
	    spnTest.addChangeListener(new ChangeListener() {
	        @Override
	        public void stateChanged(ChangeEvent e) {
	            spnTrain.setValue(100 - (int)spnTest.getValue());
	        }
	    });
	    comp = spnTrain.getEditor();
	    field = (JFormattedTextField) comp.getComponent(0);
	    formatter = (DefaultFormatter) field.getFormatter();
	    formatter.setCommitsOnValidEdit(true);
	    spnTrain.addChangeListener(new ChangeListener() {
	        @Override
	        public void stateChanged(ChangeEvent e) {
	            spnTest.setValue(100 - (int)spnTrain.getValue());
	        }
	    });
	    
		JLabel lblTestingSize = new JLabel("Testing Size (%):");
		lblTestingSize.setBounds(278, 145, 133, 20);
		pnlStep3.add(lblTestingSize);
		
		JLabel lblOutputSaveLocation = new JLabel("Output Save Location:");
		lblOutputSaveLocation.setBounds(15, 525, 174, 20);
		contentPanel.add(lblOutputSaveLocation);
		
		if (currentProject == null)
			txtLoc = new JTextField(System.getProperty("user.dir"));
		else
			txtLoc = new JTextField(System.getProperty("user.dir") + "\\" + DEFAULT_NAME);
		txtLoc.setEditable(false);
		txtLoc.setBounds(181, 522, 209, 26);
		contentPanel.add(txtLoc);
		txtLoc.setColumns(10);
		
		JButton btnSave = new JButton("Select...");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				//Sets up the dialog box.
				JFileChooser c = new JFileChooser();
			    c.setCurrentDirectory(new File(System.getProperty("user.dir")));
			    if (currentProject == null){
			    	c.setDialogTitle("Save directory for analysis...");
				    c.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				    c.setMultiSelectionEnabled(false);
			    } else {
				    c.setDialogTitle("Save output of analysis...");
				    c.setFileSelectionMode(JFileChooser.FILES_ONLY);
				    c.setMultiSelectionEnabled(false);
				    c.setSelectedFile(new File(DEFAULT_NAME));
				    FileNameExtensionFilter filter = 
				    		new FileNameExtensionFilter("Comma-Separated Values (CSV)", 
				    				"csv");
				    c.setFileFilter(filter);
				    c.setAcceptAllFileFilterUsed(false);
			    }

				//Opens the dialog.
			    int rVal = c.showSaveDialog(PredictDlg.this);
			    
			    //Checks what the status is.
			    if (rVal == JFileChooser.APPROVE_OPTION) {
			    	if (!c.getSelectedFile().getName().endsWith("csv") && currentProject != null){
			    		txtLoc.setText(c.getSelectedFile().getPath() + ".csv");
			    	} else {
				        txtLoc.setText(c.getSelectedFile().getPath());
			    	}
			    }
			}
		});
		btnSave.setBounds(392, 521, 115, 29);
		contentPanel.add(btnSave);
		
		JPanel pnlSelect = new JPanel();
		pnlSelect.setBorder(new TitledBorder(null, "Projects to Analyze", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		pnlSelect.setBounds(522, 65, 347, 485);
		contentPanel.add(pnlSelect);
		pnlSelect.setLayout(null);
		if (currentProject != null){
			pnlSelect.setEnabled(false);
			pnlSelect.setVisible(false);
		}
		
		JLabel lblNoProjects = new JLabel("No Projects Found!");
		lblNoProjects.setHorizontalAlignment(SwingConstants.CENTER);
		lblNoProjects.setBounds(15, 32, 317, 409);
		pnlSelect.add(lblNoProjects);
		
		JScrollPane srcSelect = new JScrollPane();
		srcSelect.setBounds(15, 32, 317, 400);
		pnlSelect.add(srcSelect);
		
		tblProjects = new JTable();
		tblProjects.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"ID", "Name", "Files", "Contributors"
			}
		) {
			private static final long serialVersionUID = 7504644534960542231L;
			boolean[] columnEditables = new boolean[] {
				false, false, false, false
			};
			public boolean isCellEditable(int row, int column) {
				return columnEditables[column];
			}
		});
		srcSelect.setViewportView(tblProjects);
		
		JButton btnAll = new JButton("Select All");
		btnAll.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tblProjects.selectAll();
			}
		});
		btnAll.setBounds(15, 440, 115, 29);
		pnlSelect.add(btnAll);
		
		JButton btnNone = new JButton("Select None");
		btnNone.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				tblProjects.clearSelection();
			}
		});
		btnNone.setBounds(217, 440, 115, 29);
		pnlSelect.add(btnNone);
		{
			JPanel buttonPane = new JPanel();
			buttonPane.setLayout(new FlowLayout(FlowLayout.RIGHT));
			getContentPane().add(buttonPane, BorderLayout.SOUTH);
			{
				JButton okButton = new JButton("Run!");
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						//Validates the input.
						if (lstMetrics.getSelectedIndices().length == 0) {
							String message = "You must select at least ONE network" +
									" centrality metric!";
							JOptionPane.showMessageDialog(contentPanel, message, ""
									+ "Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						if (currentProject == null && 
								tblProjects.getSelectedRowCount() == 0){
							String message = "You must select at least ONE project" +
									" to analyze!";
							JOptionPane.showMessageDialog(contentPanel, message, ""
									+ "Error", JOptionPane.ERROR_MESSAGE);
							return;
						}
						
						//Sets the success.
						success = true;
						
						//Updates the parameters.
						preContrib = chkPreContrib.isSelected();
						preFiles = chkPreFiles.isSelected();
						spearman = chkSpearman.isSelected();
						
						//Next, gets the spinners for the ML.
						numIter = (int) spnSplits.getValue();
						trainingSize = (int) spnTrain.getValue();
						
						//Finally, gets the ML type and metrics.
						mlType = (MachineLearning) cmbML.getSelectedItem();
						int[] sel = lstMetrics.getSelectedIndices();
						metrics = new ArrayList<SocialMetrics>();
						for (int i = 0; i < sel.length; i++){
							metrics.add((SocialMetrics) 
									lstMetrics.getModel().getElementAt(sel[i]));
						}
						
						dispose();
					}
				});
				okButton.setActionCommand("OK");
				buttonPane.add(okButton);
				getRootPane().setDefaultButton(okButton);
			}
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
		
		//Skip DB access if not necessary.
		if (currentProject != null) return;
		
		//Load in the projects.
		String sql = "SELECT Project.ProjectID AS ProjectID, " +
						"Project.Name AS Name, COUNT(*) AS Contributors, Files FROM " +
						"(Project INNER JOIN BelongsTo ON Project.ProjectID = BelongsTo.Project) " +
						"INNER JOIN (SELECT Project.ProjectID AS PID, COUNT(*) AS Files " +
						"FROM Project INNER JOIN File ON Project.ProjectID = File.ProjectID " +
						"GROUP BY Project.ProjectID) ON Project.ProjectID = PID GROUP BY " +
						"Project.ProjectID;";
						
		Connection conn = null;
		Statement state = null;
		ArrayList<String[]> results = new ArrayList<String[]>();
		try {
			conn = DriverManager.getConnection("jdbc:sqlite:" + DB_LOC);
		   	state = conn.createStatement();
			state.setQueryTimeout(TIMEOUT);
				
			//Runs the query to get all project IDs
			ResultSet rs = state.executeQuery(sql);
			while (rs.next()){
				String currProj[] = {
				rs.getString("ProjectID"),
				rs.getString("Name"),
				rs.getString("Contributors"),
				rs.getString("Files")};
				results.add(currProj);
			}
		    conn.close();
		} catch (SQLException e){
			e.printStackTrace();
	    }
			    
		if (results.size() != 0) {
			//Removes the help hint.
	    	lblNoProjects.setVisible(false);
			    	
		    //Next, we build the table.
			DefaultTableModel model = (DefaultTableModel) tblProjects.getModel();
			for (int i = 0; i < results.size(); i++){
				model.addRow(new Object[]{results.get(i)[0], 
						results.get(i)[1], results.get(i)[2], results.get(i)[3]});
			}
		}
	}
}
