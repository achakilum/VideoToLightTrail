import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import javax.imageio.ImageIO;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FrameGrabber;

public class VideoToLightTrail 
{
	private PrintWriter logger = null;

	private String inputFilename;
	private String filenameNoExt;
	private String outputFilename;
	
	private int frameH;
	private int frameW;
	
	int[][] rgbMatrix;
	
	/**
	 * Constructs MP4 video to JPG light trail converter with no input or output filename specified.
	 */
	public VideoToLightTrail()
	{
		inputFilename = null;
		filenameNoExt = null;
		outputFilename = null;
		
		try
		{
			logger = new PrintWriter(new FileWriter("vtlt_log.txt", true));
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Constructs MP4 video to JPG light trail converter with specified input file.
	 * Output filename is automatically generated.
	 * 
	 * @param videoFilenameIn the filename of the input file
	 */
	public VideoToLightTrail(String videoFilenameIn)
	{
		inputFilename = videoFilenameIn;
		filenameNoExt = inputFilename.substring(0, inputFilename.indexOf('.'));
		outputFilename = "out/" + filenameNoExt + "_out.jpg";
		
		try
		{
			logger = new PrintWriter(new FileWriter("vtlt_log.txt", true));
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}

	}
	
	/**
	 * Constructs MP4 video to JPG light trail converter with specified input and output file.
	 * 
	 * @param videoFilenameIn the filename of the MP4 video input file.
	 * @param photoFilenameOut the filename of the JPG light trail output file.
	 */
	public VideoToLightTrail(String videoFilenameIn, String photoFilenameOut)
	{
		inputFilename = videoFilenameIn;
		filenameNoExt = inputFilename.substring(0, inputFilename.indexOf('.'));
		outputFilename = photoFilenameOut;
		
		try
		{
			logger = new PrintWriter(new FileWriter("vtlt_log.txt", true));
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	
	/**
	 * Converts the WHOLE given MP4 video to a JPG light trail.
	 * 
	 * @return true is conversion was successful, false if not.
	 */
	public boolean generateLightTrail()
	{
		return generateLightTrail(0, Integer.MAX_VALUE);
	}
	
	/**
	 * Converts the given MP4 video to a JPG light trail. 
	 * 
	 * May specify a starting frame # and ending frame #.
	 * 
	 * @return true is conversion was successful, false if not.
	 */
	public boolean generateLightTrail(int startFrame, int endFrame)
	{
		if (inputFilename == null || outputFilename == null)
		{
			return false;
		}
		
		return (collectFrameData(startFrame, endFrame) && writeRGBImage());
	}
	
	/**
	 * Searches through each frame of the given MP4 video for the neccesary pixel data
	 * to generate the final light trail image.
	 * 
	 * @return true if data collection was successful, false if not.
	 */
	/*private boolean collectFrameData()
	{
		return collectFrameData(0, Integer.MAX_VALUE);
	}*/
	
	/**
	 * Searches through each frame of the given MP4 video for the neccesary pixel data
	 * to generate the final light trail image.  
	 * 
	 * May specify a starting frame # and ending frame #.
	 * 
	 * @param startFrame the frame to start collection on.
	 * @param endFrame the frame to end collection on.
	 * @return true if data collection was successful, false if not.
	 */
	private boolean collectFrameData(int startFrame, int endFrame)
	{
		FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(new File(inputFilename));
		
		try
		{
			grabber.start();
			frameH = grabber.getImageHeight();
			frameW = grabber.getImageWidth();
		}
		catch (FrameGrabber.Exception fge)
		{
			logger.println("Failed to start FFmpegFrameGrabber instance.");
			logger.flush();
			return false;
		}
		
		/* Set up storage of RGB values. */
		
		rgbMatrix = new int[frameW][frameH];
		
		try
		{
			grabber.setFrameNumber(startFrame + 1);
		}
		catch (Exception e)
		{
			logger.println("Count not set frame # to " + startFrame);
			logger.flush();
			return false;
		}
		
		/* Loop through each video frame, gathering RGB values and comparing them to currently stored RGB values.*/
		
		for (int frameCount = startFrame + 1; frameCount <= endFrame; frameCount++)
		{
			try
			{
				IplImage frame = grabber.grab();
				
				if (frame == null)
				{
					break;
				}
				
				BufferedImage img = frame.getBufferedImage();
				
				for (int w = 0; w < frameW; w++)
				{
					for (int h = 0; h < frameH; h++)
					{
						int newRGB = img.getRGB(w, h);

						int newNorm = getRGBNormSquared(newRGB);
						int curNorm = getRGBNormSquared(rgbMatrix[w][h]);
						
						if (newNorm > curNorm)
						{
							rgbMatrix[w][h] = newRGB;
						}
					} 
				}
				
				frameCount++;
			}
			catch (FrameGrabber.Exception fge)
			{
				logger.println("Failed to grab frame #" + frameCount);
				logger.flush();
				return false;
			}
		}
		
		try
		{
			grabber.stop();
		}
		catch (FrameGrabber.Exception fge)
		{
			logger.println("Failed to stop FFmpegFrameGrabber instance.");
			logger.flush();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Writes the final JPG light trail, named as the specified output filename, to the file system.
	 * 
	 * @return true if writing was successful, false if not.
	 */
	private boolean writeRGBImage()
	{
		BufferedImage outImg = new BufferedImage(frameW, frameH, BufferedImage.TYPE_INT_RGB);
		
		for (int w = 0; w < frameW; w++)
		{
			for (int h = 0; h < frameH; h++)
			{
				outImg.setRGB(w, h, rgbMatrix[w][h]);
			} 
		}
		
		try 
		{
			File outFile = new File(outputFilename);
			ImageIO.write(outImg, "jpg", outFile);
		} 
		catch (IOException e) 
		{
			logger.println("Could not write output image.");
			logger.flush();
			return false;
		}
		
		return true;
	}
	
	/**
	 * Treats the given 32-bit RGB value as a vector in RGB space (ex. 0xFFFFFF is the vector <255, 255, 255>),
	 * and calculates the square 2-norm of said vector.
	 * 
	 * @param rgb the 32-bit RGB value
	 * @return the square 2-norm of the vector in RGB space, specified by the given RGB value
	 */
	private int getRGBNormSquared(int rgb)
	{
		int r = ((rgb >> 16) & 0xff);
		int g = ((rgb >> 8) & 0xff);
		int b = (rgb & 0xff);
		
		int intensity = (r*r) + (g*g) + (b*b);
		return intensity;
	}
	
	/**
	 * Retrieves the current MP4 video input filename.
	 * 
	 * @return the current input filename
	 */
	public String getInputFilename()
	{
		return inputFilename;
	}
	
	/**
	 * Sets a new MP4 video input filename.
	 * 
	 * @param the new input filename
	 */
	public void setInputFilename(String newInputFilename)
	{
		inputFilename = newInputFilename;
	}
	
	/**
	 * Retrieves the current JPG light trail output filename to be.
	 * 
	 * @return the current output filename
	 */
	public String getOutputFilename()
	{
		return outputFilename;
	}
	
	/**
	 * Sets a new JPG light trail output filename to be.
	 * 
	 * @param newOutputFilename the new output filename
	 */
	public void setOutputFilename(String newOutputFilename)
	{
		outputFilename = newOutputFilename;
	}
	
	/**
	 * Retrieves the current logging PrintWriter for the converter.
	 * 
	 * @return the current logger
	 */
	public PrintWriter getLogger() 
	{
		return logger;
	}

	/**
	 * Sets a new logging PrintWriter for the converter.
	 * 
	 * @param logger
	 */
	public void setLogger(PrintWriter logger) 
	{
		this.logger = logger;
	}
}
