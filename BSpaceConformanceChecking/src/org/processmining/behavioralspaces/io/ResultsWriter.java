package org.processmining.behavioralspaces.io;

import java.io.FileWriter;
import java.io.IOException;

import com.opencsv.CSVWriter;

public class ResultsWriter {

	CSVWriter writer;
	
	public ResultsWriter(String filepath, boolean append) {
		try {
			writer = new CSVWriter(new FileWriter(filepath, append), ';');
			if (!append) {
				// write header
				writer.writeNext(new String[]{
						"modelname", "noise", "error", "places", "transitions", "silent", "xorsplit", "andsplit", "skips", "duplicate", "loops", "NFC size", "log size (original)",
						"interpretations", "ambig event classes", "SESEs", "unambig. SESEs", "unambig size", "sese size",
						"compliant", "noncompl", "potentiallycompliant", "naive exec time (sec)", "eff exec time (sec)"
			});
		}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void close() {
		try {
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void addModelResult(String[] line) {
		writer.writeNext(line);
		try {
			writer.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
	}
	
	
}
