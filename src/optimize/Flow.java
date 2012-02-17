package optimize;

import joeq.Class.*;
import joeq.Compiler.Quad.*;
import joeq.Main.Helper;

public abstract class Flow 
{

    /**
     * Dataflow Object - Domain of the Dataflow analysis.<br />
     * Implementations should implement the <tt>equals</tt> and <tt>hashCode</tt>
     * methods with respect to the semantics of the dataflow object.
     * @see java.util.BitSet
     * @see java.util.SortedMap
     * @see java.util.SortedSet
     */
    public static interface DataflowObject {
        /** Set this object to top.
         */
        void setToTop();

        /** Set this object to bottom.
         */
        void setToBottom();

        /**
         * Meet with another dataflow object.
         * @param o the object to meet with
         */
        void meetWith(DataflowObject o);

        /**
         * Copy the dataflow object.
         * @param o the dataflow object to copy
         */
        void copy(DataflowObject o);
    }

    /** Dataflow analysis.
     */
    public static interface Analysis {
        /**
         * Analysis specific pre-processing.<br />
         * This pre-processing can be used to compute the <tt>GEN</tt> and
         * <tt>KILL</tt> sets for instance.
         * @param cfg the complete control flow graph
         */
        void preprocess(ControlFlowGraph cfg);

        /**
         * Analysis specific post-processing.
         * @param cfg the complete control flow graph
         */
        void postprocess(ControlFlowGraph cfg);

        /**
         * Wether this analysis is a forward analysis.
         * @return <tt>true</tt> if this analysis is a forward analysis
         */
        boolean isForward();

        /** Get the output value of the entry block.
         */
        DataflowObject getEntry();

        /** Get the input value of the exit block.
         */
        DataflowObject getExit();

        /**
         * Get the input value of the quad.
         * @param q the quad to get the input of
         */
        DataflowObject getIn(Quad q);

        /**
         * Get the output value of the quad.
         * @param q the quad to get the output of
         */
        DataflowObject getOut(Quad q);

        /**
         * Set the value of the input of a quad.
         * @param q the quad
         * @param value the value to set to the input of the quad
         */
        void setIn(Quad q, DataflowObject value);

        /**
         * Set the value of the output of a quad.
         * @param q the quad
         * @param value the value to set to the input of the quad
         */
        void setOut(Quad q, DataflowObject value);

        /**
         * Set the value of the output of the entry block of the CFG.
         * @param value the value to set to the output of the entry block
         */
        void setEntry(DataflowObject value);

        /**
         * Set the value of the input of the exit block of the CFG.
         * @param value the value to set to the input of the exit block
         */
        void setExit(DataflowObject value);

        /** Create a new dataflow object.
         */
        DataflowObject newTempVar();

        /**
         * Apply the transfer function to one quad.
         * @param q the quad on which to perform the transfer function
         */
        void processQuad(Quad q);
    }

    /** The dataflow analysis solver.
     */
    public static interface Solver extends ControlFlowGraphVisitor {
        /** Register an analysis.
         */
        void registerAnalysis(Analysis a);

        /** Visit a control flow graph and perform the last registered analysis.
         */
        void visitCFG(ControlFlowGraph cfg);
    }

    public static void main(String[] args) {
        Analysis analyzer = null;
        Solver solver = null;
        try {
            solver = (Solver)Class.forName(args[0]).newInstance();
        }
        catch (Exception e) {
            System.err.println("Error initializing solver");
            e.printStackTrace();
            System.exit(-1);
        }
        try {
            analyzer = (Analysis)Class.forName(args[1]).newInstance();
        }
        catch (Exception e) {
            System.err.println("Error initializing analyzer");
            e.printStackTrace();
            System.exit(-1);
        }
        solver.registerAnalysis(analyzer);

        jq_Class[] classes = new jq_Class[args.length-2];
        for (int i=0; i < classes.length; i++)
            classes[i] = (jq_Class)Helper.load(args[i+2]);

        for (int i=0; i < classes.length; i++) {
            System.out.println("Now analyzing "+classes[i].getName());
            Helper.runPass(classes[i], solver);
        }
    }
}
