package optimize;

import joeq.Main.Helper;
import joeq.Class.*;
import java.util.List;

public class Optimize {
	
	/*
	 * optimizeFiles is a list of names of class that should be optimized
	 */
	public static void optimize(List<String> optimizeFiles)
	{
		ExFlow.Solver solver = new ExSolver();

		jq_Class[] classes = new jq_Class[optimizeFiles.size()];
		for(int i = 0; i < optimizeFiles.size(); i++)
			classes[i] = (jq_Class)Helper.load(optimizeFiles.get(i));

		ExFlow.Analysis nullChecker = new NullChecker();
		solver.registerAnalysis(nullChecker);

		for(int i = 0; i < classes.length; i++)
		{
			Helper.runPass(classes[i], solver);
		}
			//fill me in
    }
}
