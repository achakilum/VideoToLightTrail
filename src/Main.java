import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Main 
{
	public static void main(String args[])
	{	
		if (args.length == 0)
		{
			runWithoutArguments();
		}
		else
		{
			runWithArguments(args);
		}
	}
	
	public static void runWithoutArguments()
	{	
		/* Create log file. */
		
		File log = new File("vtlt_log.txt");
		boolean logFirstWrite = false;
		PrintWriter logWriter = null;
		
		if (!log.exists())
		{
			try
			{
				log.createNewFile();
				log.setReadable(true);
				log.setWritable(true);
				log.setExecutable(true);
				
				logFirstWrite = true;
			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();
				System.err.println("Cannot proceed. Failed to create log file.");
				System.exit(1);
			}
		}
		
		try
		{
			logWriter = new PrintWriter(new FileWriter(log, true));
			if (logFirstWrite)
			{
				logWriter.println("Log successfully created!");
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			System.err.println("Cannot proceed. Failed to create log file output stream.");
			System.exit(1);
		}
		
		/* Log timestamp */
	
		Calendar curTime = Calendar.getInstance();
		String timestamp = "[" + curTime.get(Calendar.MONTH) + "/" 
							   + curTime.get(Calendar.DAY_OF_MONTH) + "/"
							   + curTime.get(Calendar.YEAR) + " "
							   + curTime.get(Calendar.HOUR_OF_DAY) + ":"
							   + curTime.get(Calendar.MINUTE) + ":"
							   + curTime.get(Calendar.SECOND)
							+ "]";
		logWriter.println(timestamp);
		logWriter.flush();
		
		/* Open file chooser to select MP4 video */
		
		File inDir = new File("in");
		File outDir = new File("out");
		
		if (!inDir.exists() || !inDir.isDirectory())
		{
			inDir.mkdir();
		}
		
		if (!outDir.exists() || !outDir.isDirectory())
		{
			outDir.mkdir();
		}
		
		String appTitle = "VTLT";
		
		try
		{
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e)
		{
			logWriter.println(e.getLocalizedMessage());
			logWriter.flush();
		}
		
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(appTitle + " - Select video to convert into light trail image.");
		chooser.setCurrentDirectory(inDir);
		chooser.setFileFilter(new FileNameExtensionFilter("MP4 Video (*.mp4)", "mp4"));
		
		int op = chooser.showOpenDialog(null);
		if (op != JFileChooser.APPROVE_OPTION)
		{
			System.exit(0);
		}
		
		File inputFile = chooser.getSelectedFile();
		String inputFilename = inputFile.getName();
		String inputFilenameFullPath = inputFile.getAbsolutePath();
		
		if (!new File(inputFilenameFullPath).exists())
		{
			JOptionPane.showMessageDialog(null, "Could not find input file.", appTitle, JOptionPane.ERROR_MESSAGE);
			System.exit(1);
		}
		
		chooser.setDialogTitle(appTitle + " - Save JPG light trail image as...");
		chooser.setCurrentDirectory(outDir);
		chooser.setSelectedFile(new File(inputFilename.substring(0, inputFilename.indexOf(".")) + ".jpg"));
		chooser.setFileFilter(new FileNameExtensionFilter("JPG Image (*.jpg)", "jpg"));
		
		op = chooser.showSaveDialog(null);
		if (op != JFileChooser.APPROVE_OPTION)
		{
			System.exit(0);
		}
		
		File outputFile = chooser.getSelectedFile();
		String outputFilename = outputFile.getName();
		String outputFilenameFullPath = outputFile.getAbsolutePath();
		
		/* Allow user to double-check selected files, then notify him/her of background progress. */
		
		int proceed = JOptionPane.showConfirmDialog(null, 
										"Input File: " + inputFilenameFullPath + "\n"
									  + "Output File: " + outputFilenameFullPath + "\n\n"
									  + "Proceed?", 
									  appTitle, JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		
		if (proceed != JOptionPane.YES_OPTION)
		{
			System.exit(0);
		}
		else
		{
			JOptionPane.showMessageDialog(null, 
											"The converter will now work in the background.\nA message will pop up to signify completion.", 
											appTitle, JOptionPane.INFORMATION_MESSAGE);
		}
		
		/* Begin light trail generation from input video file to output image file. */
		
		VideoToLightTrail vtlt = new VideoToLightTrail();
		vtlt.setLogger(logWriter);
		vtlt.setInputFilename(inputFilenameFullPath);
		vtlt.setOutputFilename(outputFilenameFullPath);
		
		logWriter.println("Generating light trail for \"" + vtlt.getInputFilename() + "\"... ");
		boolean ok = vtlt.generateLightTrail();
		
		if (!ok)
		{
			logWriter.println("Generation unsuccessful.");
			JOptionPane.showMessageDialog(null, "Conversion unsuccessful. Check \"vtlt_log.txt\".", appTitle, JOptionPane.WARNING_MESSAGE);
		}
		else
		{
			logWriter.print("Generation successful! ");
			logWriter.println("Output name is \"" + vtlt.getOutputFilename() + "\".");
			JOptionPane.showMessageDialog(null, "Conversion successful!", appTitle, JOptionPane.INFORMATION_MESSAGE);
		}
		
		logWriter.flush();
		logWriter.close();
	}
	
	public static void runWithArguments(String args[])
	{
		System.err.println("Command line arguments not supported yet. Running GUI...");
		runWithoutArguments();
	}

}
