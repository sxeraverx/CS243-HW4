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

        AnticipatedExpressions anticipatedExpressions = new AnticipatedExpressions();
        AvailableExpressions availableExpressions = new AvailableExpressions();
        PostponableExpressions postponableExpressions = new PostponableExpressions();
        NullChecker nullChecker = new NullChecker();

        for(int i = 0; i < classes.length; i++)
	    {
            /*
            solver.registerAnalysis(anticipatedExpressions);
            System.out.println("Running anticipation solver");
            Helper.runPass(classes[i], solver);

            solver.registerAnalysis(availableExpressions);
            availableExpressions.setAnticipatedExpressions(anticipatedExpressions.getAnticipatedExpressions());
	
            System.out.println("Running availability solver");	
            Helper.runPass(classes[i], solver);

            solver.registerAnalysis(postponableExpressions);
            postponableExpressions.setEarliestExpressions(availableExpressions.getEarliestExpressions());
	
            System.out.println("Running postponability solver");	
            Helper.runPass(classes[i], solver);
            */
            solver.registerAnalysis(nullChecker);
            Helper.runPass(classes[i], solver);
	    }
    }
}
