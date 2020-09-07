package org.processmining.behavioralspaces.models.behavioralspace;

import org.apache.commons.lang3.StringUtils;

public class DeviationMatrix {
	private  int M;             // number of rows
    private  int N;             // number of columns
    private  String[][] data;   // M-by-N array

    // create M-by-N matrix of 0's
    public DeviationMatrix(int M, int N) {
        this.M = M;
        this.N = N;
        data = new String[M][N];
    }

    // copy constructor
    private DeviationMatrix(DeviationMatrix A) { 
    	this(A.data); 
    }
    
    // create matrix based on 2d array
    public DeviationMatrix(String[][] data) {
        M = data.length;
        N = data[0].length;
        this.data = new String[M][N];
        for (int i = 0; i < M; i++)
            for (int j = 0; j < N; j++)
                this.data[i][j] = data[i][j];    
            	//this.data[i][j] = data[Integer.toString(i)][Integer.toString(j)];
    }

    public void showDeviationMatrix() {
    	System.out.println();
        for (int i = 0; i < M; i++) {
            for (int j = 0; j < N; j++) { 
                
            	//System.out.print(String.format("%1$4s", data[i][j])); used only for integer
            	if(j == 0) {
            		System.out.print(StringUtils.leftPad(data[i][j], 30));
            	}
            	else {
            		System.out.print(StringUtils.leftPad(data[i][j], 30));
            	}
            }
            
            System.out.println();
        }
    }
   

}
