/*******************************************************************************
 * Copyright (c) 2020 Haonan Huang.
 *
 *     This file is part of QuantumVITAS (Quantum Visualization Interactive 
 *     Toolkit for Ab-initio Simulations).
 *
 *     QuantumVITAS is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or any 
 *     later version.
 *
 *     QuantumVITAS is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with QuantumVITAS.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 *******************************************************************************/
package app.centerwindow;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart.Data;
import javafx.scene.chart.XYChart.Series;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Callback;
import main.MainClass;
import com.consts.Constants.EnumAnalysis;
import com.consts.Constants.EnumCard;
import com.consts.Constants.EnumFileCategory;
import com.consts.Constants.EnumNameList;
import com.programconst.DefaultFileNames;
import com.programconst.ProgrammingConsts;
import app.input.InputGeoController;

public class OutputViewerController implements Initializable{

    @FXML private HBox rootHbox;

    @FXML private VBox vBoxCalcFolder,
    vboxFiles,
    vboxMainPlot;

    @FXML private ListView<String> listCalcFolders;

    @FXML private ListView<String> listFiles;
    
    @FXML private ScrollPane displayScroll;
    
    @FXML private Button deleteFolderButton,
    deleteFileButton,
    openAsButton,
    buttonRefreshFolder,
    buttonRefreshFiles,
    buttonRefresh,
    buttonSaveGeo;
    
    @FXML private Label labelFileCategory,
    labelPlot;
    
    @FXML private ComboBox<EnumAnalysis> comboAnalysis;
    
    @FXML private ComboBox<String> comboPlot;
    
    @FXML private HBox hboxPlotToolbar;
    
    @FXML private ToggleButton buttonShowMarker;
    
    private TextFlow textFlowDisplay;
    
    private MainClass mainClass;
    
    private File calcFolder;
    
    private File inoutFiles;
    
    private EnumFileCategory fileCategory;
    
    private FileDataClass fileData;
    
    private NumberAxis xAxis;

    private NumberAxis yAxis;

    private LineChart<Double, Double> lineChart;
    
    private final ArrayList<String> plotTypeDos;
    
    private ArrayList<String> plotTypeStdOut;
    
    private InputGeoController contGeo;
    
    private WorkScene3D geometry3d;
    		
    public OutputViewerController(MainClass mc, InputGeoController cg) {
    	mainClass = mc;
    	contGeo = cg;
    	textFlowDisplay = new TextFlow();
    	fileData = new FileDataClass();
    	xAxis = new NumberAxis();xAxis.setAutoRanging(true);xAxis.setForceZeroInRange(false);
    	yAxis = new NumberAxis();yAxis.setAutoRanging(true);yAxis.setForceZeroInRange(false);
    	
    	geometry3d = new WorkScene3D();
    	geometry3d.addAnimate3D(fileData);//add animation panel and link the data file
    	geometry3d.buildGeometry(fileData.getGeoAgent());//link the geoAgent in fileData to the workscene3d plot
    	
    	
    	lineChart = new LineChart(xAxis, yAxis);
    	
    	plotTypeDos = new ArrayList<String>() {
    		{
	    		add("DOS"); //0
	    		add("Integrated DOS"); //1 
    		}
		};
		plotTypeStdOut= new ArrayList<String>();
	}

	@Override
	public void initialize(URL arg0, ResourceBundle arg1) {
		geometry3d.centerSubScene(displayScroll);
		
		lineChart.prefWidthProperty().bind(displayScroll.widthProperty());
		lineChart.setCreateSymbols(false);
		//lineChart.prefHeightProperty().bind(workSpaceTabPane.heightProperty());
		
		//comboPlot.setVisible(false);labelPlot.setVisible(false);
		hboxPlotToolbar.setVisible(false);
		listCalcFolders.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
			fileData.clearAll();buttonSaveGeo.setDisable(true);
			
			if(newTab==null || newTab.isEmpty()) return;
			File pjFolder = getProjectFolder();
			if(pjFolder==null || !pjFolder.canRead()) return;
			calcFolder = new File(pjFolder,newTab);
			
			updateFilesInCalcFolder();
		});
		listFiles.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
			fileCategory = null;
			fileData.clearAll();buttonSaveGeo.setDisable(true);
			labelFileCategory.setText("");
			EnumAnalysis analTmp = comboAnalysis.getSelectionModel().getSelectedItem();//cache the selected item before clearing
			comboAnalysis.getItems().clear();
			
			if(newTab==null || newTab.isEmpty()) return;
			if(calcFolder==null || !calcFolder.canRead() || !calcFolder.isDirectory()) return;
			inoutFiles = new File(calcFolder,newTab);
			if(!inoutFiles.canRead()) return;
			
			if(newTab.contains(ProgrammingConsts.stdinExtension)) {fileCategory = EnumFileCategory.stdin;}
			else if(newTab.contains(ProgrammingConsts.stdoutExtension)) {fileCategory = EnumFileCategory.stdout;}
			else if(newTab.contains(ProgrammingConsts.stderrExtension)) {fileCategory = EnumFileCategory.stderr;}
			else if(newTab.contains(ProgrammingConsts.dosExtension)) {fileCategory = EnumFileCategory.dos;}
			else if(newTab.contains(DefaultFileNames.calcSaveFile)||newTab.contains(DefaultFileNames.projSaveFile)) 
			{fileCategory = EnumFileCategory.save;}
			else if(newTab.contains(".xml")) {fileCategory = EnumFileCategory.xmlout;}
			else if(newTab.toLowerCase().contains("crash")) {fileCategory = EnumFileCategory.crash;}
			else if(inoutFiles.isDirectory()) {fileCategory = EnumFileCategory.directory;}
			else {fileCategory = EnumFileCategory.unknown;}
			
			fileData.fileCategory = fileCategory;
			
			if(fileCategory!=null) {labelFileCategory.setText(fileCategory.toString());}//should not be null until this point!
			
			loadFile();
			
			if(fileCategory!=null) {
				comboAnalysis.getItems().addAll(EnumAnalysis.info);//always has the option of show summarized info
				comboAnalysis.getItems().addAll(EnumAnalysis.text);//always has the option of show in text
				comboAnalysis.getItems().addAll(EnumAnalysis.plot2D);//always has the option of show in plot (can be changed later)
				if(fileData.isMD || fileData.isOpt) {comboAnalysis.getItems().addAll(EnumAnalysis.plot3D);}
				
				if(analTmp!=null && comboAnalysis.getItems()!=null && comboAnalysis.getItems().contains(analTmp)) {
					//select back the choice before
					comboAnalysis.getSelectionModel().select(analTmp);
				}
				else {
					//select text first if there has been no selection
					comboAnalysis.getSelectionModel().select(EnumAnalysis.plot2D);
				}
			}
			
			updateIoDisplay();//*** not efficient because runs twice
		});
		
		buttonRefresh.setOnAction((event) -> {
			if((fileData.isMD || fileData.isOpt) && !comboAnalysis.getItems().contains(EnumAnalysis.plot3D)) {comboAnalysis.getItems().addAll(EnumAnalysis.plot3D);}
			loadFile();
			updateIoDisplay();
		});
		comboAnalysis.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
			if(newTab!=null) {updateIoDisplay();}
		});
		comboPlot.getSelectionModel().selectedItemProperty().addListener((ov, oldTab, newTab) -> {
			if(newTab!=null) {updateIoDisplay();}//very crucial to check null here!
		});
		openAsButton.setOnAction((event) -> {
			if(inoutFiles==null || !inoutFiles.canRead()) return;
			try {
				Desktop.getDesktop().open(inoutFiles);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		});
		buttonSaveGeo.setDisable(true);
		buttonSaveGeo.setOnAction((event) -> {
			if(fileData==null) {return;}
//			ArrayList<Atom> atomList = fileData.getFinalAtomicPositions();
//			CellParameter cellPara = fileData.getFinalCellParameter();
//			Double alat = fileData.getAlat();
//			if(atomList==null || atomList.isEmpty() || alat==null ) {return;}
			String calcFolderName = listCalcFolders.getSelectionModel().getSelectedItem();
			if(calcFolderName==null) {return;}
			//mainClass.projectManager.addGeoList(calcFolderName, atomList, cellPara, alat);
			mainClass.projectManager.addGeoList(calcFolderName, fileData.getGeoAgent());
			if(contGeo!=null) {contGeo.loadProjectParameters();}
		});
		buttonShowMarker.selectedProperty().addListener((observable, oldValue, newValue) ->{
			if(newValue==null) return;
			lineChart.setCreateSymbols(newValue);
		});
		buttonRefreshFolder.setOnAction((event) -> {
			int tmpInt = listCalcFolders.getSelectionModel().getSelectedIndex();
			updateProjectFolder();
			if(tmpInt>=0 && listCalcFolders.getItems()!=null && listCalcFolders.getItems().size()>tmpInt) {
				listCalcFolders.getSelectionModel().select(tmpInt);
			}
		});
		buttonRefreshFiles.setOnAction((event) -> {
			updateFilesInCalcFolder();
		});
		deleteFolderButton.setOnAction((event) -> {
			try {
				deleteDirectory(calcFolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
			updateProjectFolder();
		});
		deleteFileButton.setOnAction((event) -> {
			try {
				deleteDirectory(inoutFiles);
			} catch (IOException e) {
				e.printStackTrace();
			}
			updateFilesInCalcFolder();
		});
		
	}
	private boolean deleteDirectory(File directoryToBeDeleted) throws IOException {
	    File[] contentFiles = directoryToBeDeleted.listFiles();
	    if (contentFiles != null) {
	        for (File file : contentFiles) {
	            deleteDirectory(file);
	        }
	    }
	    return directoryToBeDeleted.delete();
	}
	private void updateFilesInCalcFolder() {
		int tmpInt = listFiles.getSelectionModel().getSelectedIndex();
		
		listFiles.getItems().clear();
		
		if(calcFolder==null || !calcFolder.canRead() || !calcFolder.isDirectory()) return;
		
		File[] fileList = calcFolder.listFiles();
		for (File f : fileList) {
			listFiles.getItems().add(f.getName());
		}
		if(listFiles.getItems()!=null) {
			if(tmpInt>=0 && listFiles.getItems().size()>tmpInt) {
				listFiles.getSelectionModel().select(tmpInt);//will invoke selection change listener and update "inoutFiles"
			}
			else if(!listFiles.getItems().isEmpty()){
				listFiles.getSelectionModel().select(0);//select first file
			}
		}
	}
	private void updateIoDisplay() {
		
		EnumAnalysis analTmp = comboAnalysis.getSelectionModel().getSelectedItem();
		
		displayScroll.setContent(null);
		displayScroll.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);//reset scrolling capability
		displayScroll.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		
		if(fileCategory==null || analTmp==null) return;
		
		boolean boolPlot = analTmp.equals(EnumAnalysis.plot2D);
		//comboPlot.setVisible(boolPlot);labelPlot.setVisible(boolPlot);
		hboxPlotToolbar.setVisible(boolPlot);
		
		if(analTmp.equals(EnumAnalysis.text)) {//show raw text
			displayScroll.setContent(textFlowDisplay);
			readTextFileToTextFlow();//only highlight input files
		}
		else {//show analysis
			displayScroll.setContent(textFlowDisplay);
			textFlowDisplay.getChildren().clear();
			if(fileCategory.equals(EnumFileCategory.stdout)) {
				if(analTmp.equals(EnumAnalysis.info)) {textFlowDisplay.getChildren().add(new Text(fileData.toString()));}
				else if(analTmp.equals(EnumAnalysis.plot2D)) {plot2dStdOut();}
				else if(analTmp.equals(EnumAnalysis.plot3D)) {plot3dStdOut();}
			}
			else if(fileCategory.equals(EnumFileCategory.dos)){
				if(analTmp.equals(EnumAnalysis.info)) {textFlowDisplay.getChildren().add(new Text(fileData.toString()));}
				else if(analTmp.equals(EnumAnalysis.plot2D)) {plot2dDos();}
				else if(analTmp.equals(EnumAnalysis.plot3D)) {textFlowDisplay.getChildren().add(new Text("No 3D view of this file type."));}
			}
			else {
				textFlowDisplay.getChildren().add(new Text("Analysis is not available for this file type."));
			}
		}
	}
	private boolean isSameTypeStdout(ArrayList<String> strTest) {
		ObservableList<String> comboTmp = comboPlot.getItems();
		if(comboTmp.size()!=strTest.size()) {return false;}
		for(int i=0;i<comboTmp.size();i++) {
			if(comboTmp.get(i)==null || !comboTmp.get(i).equals(strTest.get(i))) {return false;}
		}
		return true;
	}
	private void plot2dDos() {
		if(!isSameTypeStdout(plotTypeDos)) {
			comboPlot.getItems().clear();
			comboPlot.getItems().addAll(plotTypeDos);
			//ShowAlert.showAlert(AlertType.INFORMATION, "Info", "plot2dDos. Construct comboPlot.");
			comboPlot.getSelectionModel().select(0);
		}
		
		lineChart.getData().clear();
		
		String strSelect = comboPlot.getSelectionModel().getSelectedItem();
		boolean boolInteg;
		if(plotTypeDos.get(0).equals(strSelect)) {
			//add("DOS"); //0
			boolInteg = false;
		}
		else if(plotTypeDos.get(1).equals(strSelect)){
			//add("Integrated DOS"); //1 
			boolInteg = true;
		}
		else {
			lineChart.setTitle("Please select plotting type!");
			return;
		}
		
		Double fermiDos = fileData.fermiDos;
		ArrayList<ArrayList<Double>> dosArray = fileData.getDosArray();
		ArrayList<String> dosHeader = fileData.getDosHeader();
		
		if(dosArray.isEmpty()) {return;}
		
		xAxis.setLabel(dosHeader.size()>0 ? dosHeader.get(0):"Unknown (1st column)");
		yAxis.setLabel(strSelect);
		
		double minY = 1000.0;
		double maxY = -1000.0;
		
		for(int i=1;i<dosArray.get(0).size();i++) {//different columns (y axis)
			
			Series<Double,Double> dataSeries1 = new Series<Double, Double>();
			String headerTmp = (dosHeader.size()>i ? dosHeader.get(i):"Unknown ("+Integer.toString(i)+"th column)");
			dataSeries1.setName(headerTmp);
			if(boolInteg != headerTmp.toLowerCase().contains("int") ) {continue;}
			for(int j=0;j<dosArray.size();j++) {
				if(dosArray.get(j).size()<=i) {break;}//a certain row does not have enough numbers
				dataSeries1.getData().add(new Data<Double, Double>(dosArray.get(j).get(0), dosArray.get(j).get(i)));
				if(dosArray.get(j).get(i)>maxY) {maxY=dosArray.get(j).get(i);}
				if(dosArray.get(j).get(i)<minY) {minY=dosArray.get(j).get(i);}
			}
			lineChart.getData().add(dataSeries1);
        }
		
		//Fermi energy
		if(fermiDos!=null) {
			Series<Double,Double> dataSeries1 = new Series<Double, Double>();
			dataSeries1.getData().add(new Data<Double, Double>(fermiDos, minY));
			dataSeries1.getData().add(new Data<Double, Double>(fermiDos, maxY));
			dataSeries1.setName("Fermi Energy");
			lineChart.getData().add(dataSeries1);
		}
		
        displayScroll.setContent(lineChart);
        

	}
	private void plot3dStdOut() {
		//construct new plot type list
		plotTypeStdOut.clear();
		if(fileData.isMD || fileData.isOpt) {plotTypeStdOut.add("Geometry");}
		//plotTypeStdOut.add("With Force");
		//if(fileData.isMD) {plotTypeStdOut.add("With Velocity");}
		if(!isSameTypeStdout(plotTypeStdOut)) {
			comboPlot.getItems().clear();
			if(!plotTypeStdOut.isEmpty()) {
				comboPlot.getItems().addAll(plotTypeStdOut);
				comboPlot.getSelectionModel().select(0);//****run twice
			}
		}
		
		if(plotTypeStdOut.isEmpty()) {
			textFlowDisplay.getChildren().add(new Text("No available plot for this file type. SHOULD NOT REACH HERE!"));
			return;
		}
		
		String plotType = comboPlot.getSelectionModel().getSelectedItem();
		
		if(plotType==null) return;
		
		if(plotType.equals("Geometry")) {
			//ShowAlert.showAlert(AlertType.INFORMATION, "Info", "Plot Geometry");
			geometry3d.buildGeometry();
			geometry3d.updateAnimate3D();//update animate3D control panel
			AnchorPane acp = geometry3d.getRootPane();
			displayScroll.setContent(acp);

			//disable scrolling capability of the scrollpane so that it does not interfere with zoom-in/out of the workscene3d
			displayScroll.setHbarPolicy(ScrollBarPolicy.NEVER);
			displayScroll.setVbarPolicy(ScrollBarPolicy.NEVER);
		}
	}
	private void plot2dStdOut() {
		//construct new plot type list
		plotTypeStdOut.clear();
		if(fileData.hasScf) {plotTypeStdOut.add("SCF E conv");}
		if(fileData.isOpt) {plotTypeStdOut.add("OPT E conv");plotTypeStdOut.add("OPT F conv");}
		//check whether it is the same as in the combo. If yes, no update of the combo
		if(!isSameTypeStdout(plotTypeStdOut)) {
			comboPlot.getItems().clear();
			if(!plotTypeStdOut.isEmpty()) {
				comboPlot.getItems().addAll(plotTypeStdOut);
				//ShowAlert.showAlert(AlertType.INFORMATION, "Info", "plot2dStdOut. Construct comboPlot.");
				comboPlot.getSelectionModel().select(0);//****run twice
			}
		}
		
		if(plotTypeStdOut.isEmpty()) {textFlowDisplay.getChildren().add(new Text("No available plot for this file type."));return;}
		
		String plotType = comboPlot.getSelectionModel().getSelectedItem();
		
		if(plotType==null) return;
		
		lineChart.getData().clear();
		
		ArrayList<ArrayList<Double>> energyTmp = fileData.getEnergyArray();
		
		if(!energyTmp.isEmpty()){
			if(plotType.equals("SCF E conv")) {
				xAxis.setLabel("Iterations");
				yAxis.setLabel("Total Energy (Ry)");
				Series<Double,Double> dataSeries1 = new Series<Double, Double>();
		        dataSeries1.setName("SCF Energy Convergence");
				ArrayList<Double> scfTmp = energyTmp.get(energyTmp.size()-1);
				if(scfTmp.isEmpty() && energyTmp.size()>=2) {scfTmp = energyTmp.get(energyTmp.size()-2);}
				for(int i=0;i<scfTmp.size();i++) {
			        dataSeries1.getData().add(new Data<Double, Double>( (double) i+1, scfTmp.get(i)));
				}
				//dataSeries1.getData().add(new Data<Double, Double>( 0.0, -20.0));
				
				lineChart.getData().add(dataSeries1);
			}
			else if(plotType.equals("OPT E conv")) {
				xAxis.setLabel("Steps");
				yAxis.setLabel("Total Energy (Ry)");
				Series<Double,Double> dataSeries1 = new Series<Double, Double>();
		        dataSeries1.setName("Optimization Energy Convergence");
				
				for(int i=0;i<energyTmp.size();i++) {
					if(energyTmp.get(i).size()>0) {
						Double dbTmp = energyTmp.get(i).get(energyTmp.get(i).size()-1);
						if(dbTmp!=null) {dataSeries1.getData().add(new Data<Double, Double>( (double) i+1, dbTmp));}
					}
				}
				lineChart.getData().add(dataSeries1);
			}
			else if(plotType.equals("OPT F conv")) {
				
			}
		}
		
        
        displayScroll.setContent(lineChart);

	}
	private boolean loadFile() {
		//false is fail to load

		buttonSaveGeo.setDisable(true);
		String strTmp1 = checkErrors();
		if(strTmp1!=null && !strTmp1.isEmpty()) {showCannotLoad("Cannot load file. "+strTmp1);return false;}

		if(fileCategory.equals(EnumFileCategory.stdout)) {
			String msg = fileData.loadStdOut(inoutFiles);
			if(fileData.isOpt&&fileData.isOptFinished) {buttonSaveGeo.setDisable(false);}
			if(!msg.isEmpty()) {showCannotLoad(msg);return false;}
		}
		else if(fileCategory.equals(EnumFileCategory.dos)){
			return fileData.loadDOS(inoutFiles);
		}
		return true;
	}
	private void showCannotLoad(String msg) {
		displayScroll.setContent(textFlowDisplay);
		textFlowDisplay.getChildren().clear();
		textFlowDisplay.getChildren().add(new Text(msg));
	}
	private String checkErrors() {
		if(fileCategory==null) {return "Error: file category is null. Check code!";}
		if(fileCategory.equals(EnumFileCategory.directory)) {return "Target file is a directory.";}
		if(inoutFiles==null || !inoutFiles.canRead()) {return "Error: cannot read file.";}
		return "";
	}
	private void readTextFileToTextFlow() {
		textFlowDisplay.getChildren().clear();
		String strTmp1 = checkErrors();
		if(strTmp1!=null && !strTmp1.isEmpty()) {textFlowDisplay.getChildren().add(new Text(strTmp1));return;}
		
		boolean boolHighLight = false;
		if(fileCategory.equals(EnumFileCategory.stdin)) {boolHighLight=true;}
		
		try {
//			String data = new String(Files.readAllBytes(inoutFiles.toPath()));
//			textFlowDisplay.getChildren().add(new Text(data));
		    Scanner sc = new Scanner(inoutFiles); 
		  
		    String strTmp;
		    Text txtTmp;
		    int lineCount=0;
		    while (sc.hasNextLine()) {
		    	lineCount++;
		    	strTmp = sc.nextLine();
		    	txtTmp = new Text(strTmp+"\n");
		    	if(boolHighLight) {
			    	if(strTmp!=null && strTmp.contains("calculation")&& strTmp.contains("=")) {txtTmp.setFill(Color.BLUE);}
			    	if(strTmp!=null && containsSectionName(strTmp) && !strTmp.contains("=")) {txtTmp.setFill(Color.GREEN);}
		    	}
		    	textFlowDisplay.getChildren().add(txtTmp);
		    }
		    
		    sc.close();
		    
		    if(lineCount==0) {
		    	textFlowDisplay.getChildren().add(new Text("No lines detected. File empty or binary file."));
		    }
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	private boolean containsSectionName(String stTmp) {
		if(stTmp==null || stTmp.isEmpty()) return false;
		String newstTmp = stTmp.replaceAll("\\s+","").toLowerCase();//remove all whitespaces and to lower case
		List<String> enumValues1 = Stream.of(EnumNameList.values())
                .map(EnumNameList::toString)
                .collect(Collectors.toList());
		List<String> enumValues2 = Stream.of(EnumCard.values())
                .map(EnumCard::toString)
                .collect(Collectors.toList());
		for (String st : enumValues1) {
			if(newstTmp.contains(st.trim().toLowerCase())) {return true;}
		}
		for (String st : enumValues2) {
			if(newstTmp.contains(st.trim().toLowerCase())) {return true;}
		}
		return false;
		
	}
	private File getProjectFolder() {
		String wsp = mainClass.projectManager.workSpacePath;
		String pj = mainClass.projectManager.getActiveProjectName();
		if(wsp==null || pj==null || pj.isEmpty()) return null;
		return new File(new File(wsp),pj);
	}
	public void updateProjectFolder() {
		listCalcFolders.getItems().clear();listFiles.getItems().clear();
		
		File pjFolder = getProjectFolder();
		if(pjFolder==null || !pjFolder.canRead()) return;
		
		File[] fileList = pjFolder.listFiles();
		int count = 0;
		for (File f : fileList) {
			if(f.isDirectory()) {listCalcFolders.getItems().add(f.getName());count++;}
		}
		ArrayList<String> pureFiles = new ArrayList<String>();
		for (File f : fileList) {
			if(f.isFile()) {listCalcFolders.getItems().add(f.getName());pureFiles.add(f.getName());}
		}
		if(count>0) {listCalcFolders.getSelectionModel().select(0);}//will invoke selection change listener and update "calcFolder"
		listCalcFolders.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
	        @Override 
	        public ListCell<String> call(ListView<String> param) {
	            return new ListCell<String>() {
	                @Override 
	                protected void updateItem(String item, boolean empty) {
	                    super.updateItem(item, empty);
	                    if (pureFiles.contains(item)) {
	                        setDisable(true);setTextFill(Color.LIGHTGRAY);
	                    } else {
	                        setDisable(false);setTextFill(Color.BLACK);
	                    }
	                    setText(item);
	                }

	            };
	        }
	    });
		
	}


}
