package org.processmining.behavioralspaces.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.processmining.framework.plugin.PluginContext;
import org.processmining.models.graphbased.directed.petrinet.Petrinet;
import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.impl.ToStochasticNet;
import org.processmining.models.semantics.petrinet.Marking;
import org.processmining.plugins.stochasticpetrinet.StochasticNetUtils;

public class IOHelper {

	
	public static Petrinet importPNML(PluginContext context, String filepath) throws Exception {
		Petrinet net = null;
		try {
//			System.out.println("trying to load as Petrinet");
			net = FileHelper.importPNML(filepath, context);
		} catch (Exception e) {
//			System.out.println("failed to load " + filepath + " as petrinet: " + e);
		}
		// net is stochastic
		if (net == null) {
			net = FileHelper.importStochasticPNML(filepath, getFileName(new File(filepath)));
			
			Marking marking = StochasticNetUtils.getInitialMarking(context, net); 
			net = (Petrinet) ToStochasticNet.asPetriNet(context, (StochasticNet) net, marking)[0]; 
		}
		return net;
	}
	
	public static void serializeObject(String folderpath, String filename, Object object) {
		if (!folderpath.endsWith("/")) {
			folderpath += "/";
		}
		File folder = new File(folderpath);
		if (!folder.exists()) {
			folder.mkdir();
		}
	   serializeObject(folderpath + filename, object);
	   }
	
	public static void serializeObject(String filepath, Object object) {
		try
	      {
	         FileOutputStream fileOut =
	         new FileOutputStream(filepath);
	         ObjectOutputStream out = new ObjectOutputStream(fileOut);
	         out.writeObject(object);
	         out.close();
	         fileOut.close();
	      }catch(IOException i)
	      {
	          i.printStackTrace();
	      }
	}
	
	 public static Object deserializeObject(String filepath)
	   {
	      Object o = null;
	      try
	      {
	         FileInputStream fileIn = new FileInputStream(filepath);
	         ObjectInputStream in = new ObjectInputStream(fileIn);
	         o = in.readObject();
	         in.close();
	         fileIn.close();
	      }catch(IOException i)
	      {
	         i.printStackTrace();
	      }catch(ClassNotFoundException c)
	      {
	         c.printStackTrace();
	      }
	      return o;
	   }


	public static List<File> getFilesWithExtension(File directory, String ext) {
		List<File> filtered = new ArrayList<File>();
		for (File file : directory.listFiles()) {
			if (file.getName().endsWith(ext)) {
				filtered.add(file);
			}
		}
		
		return filtered;
	}
	
	
	public static String readFile(File file) throws IOException {
		Charset encoding = Charset.defaultCharset();
		byte[] encoded = Files.readAllBytes(Paths.get(file.getAbsolutePath()));
		return new String(encoded, encoding);
	}
	
	
	public static String getFileName(File file) {
		return FilenameUtils.removeExtension(file.getName()).toLowerCase();
	}
	
	public static double round(double value, int places) {
	    BigDecimal bd = new BigDecimal(value);
	    bd = bd.setScale(places, RoundingMode.HALF_UP);
	    return bd.doubleValue();
	}
	
}
