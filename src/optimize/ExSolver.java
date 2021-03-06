package optimize;

import java.util.Iterator;
import joeq.Compiler.Quad.ControlFlowGraph;
import joeq.Compiler.Quad.Quad;
import joeq.Compiler.Quad.QuadIterator;
import java.util.*;

public class ExSolver implements ExFlow.Solver {

    /** the solver's quad analyzer */
    private ExFlow.Analysis analyzer;

    /**
     * Register a new quad analyzer
     * @param analyzer the analyzer to register
     */
    public void registerAnalysis(ExFlow.Analysis analyzer) {
        this.analyzer = analyzer;
    }

    /**
     * Visit a given control flow graph and runs the analyzer on all its
     * quads in the direction specified by the analyzer.
     * @param cfg the control flow graph to visit
     */
    public void visitCFG(ControlFlowGraph cfg) {
        // run preprocess
        analyzer.preprocess(cfg);

        // initialize the internal node values
        QuadIterator quadIterator = new QuadIterator(cfg);
        /*while (quadIterator.hasNext()) {
            Quad quad = quadIterator.next();
            // get a new value and set it to top
            ExFlow.DataflowObject topValue = analyzer.newTempVar();
            topValue.setToTop();
            // set the initial value according to analysis direction
            if (analyzer.isForward()) {
                for(Quad succ : quadIterator.successors1()) {
                    analyzer.setOut(quad, succ, topValue);
                }
            } else {
                for(Quad pred : quadIterator.predecessors1()) {
                    analyzer.setIn(quad, pred, topValue);
                }
            }
	    }*/

        // iterate over all quads repeatedly as long as there's change in
        // any node's value
        boolean changed = true;
        boolean nodeChanged;
        while (changed) {
            // assume there are no more changes in the node values
            changed = false;
            // get a new quad iterator
            quadIterator = new QuadIterator(cfg, analyzer.isForward());
            while (analyzer.isForward() && quadIterator.hasNext()
                   || !analyzer.isForward() && quadIterator.hasPrevious()) {
                // process each quad
                if (processQuad(quadIterator)) {
                    changed = true;
                }
            }
        }
        
        // depending on the analysis direction, calculate the final value
        // of the entry / exit node
        if (analyzer.isForward()) {
            calculateExit(cfg);
        } else {
            calculateEntry(cfg);
        }

        // run postprocess
        analyzer.postprocess(cfg);
    }

    /**
     * Process a single quad and returns true iff the node's value
     * changed.
     * @param quadIterator the quad iterator pointing at the node to
     * process
     * @return true if the node's value changed, false otherwise
     */
    private boolean processQuad(QuadIterator quadIterator) {
        // get the quad to process
        Quad quad =
            analyzer.isForward()
            ? quadIterator.next() : quadIterator.previous();
        // get the quad's In and Out
        ExFlow.DataflowObject preval = analyzer.newTempVar();
        Map<Quad, ExFlow.DataflowObject> postval = new HashMap<Quad, ExFlow.DataflowObject>();
        if(analyzer.isForward())
            for(Quad succ : quadIterator.successors1())
                postval.put(succ, analyzer.getOut(quad, succ));
        else
            for(Quad pred : quadIterator.predecessors1())
                postval.put(pred, analyzer.getIn(quad, pred));
        // reset In/Out value and meet with all predecessors/successors
	preval.setToTop();
        if (analyzer.isForward()) {
	    meetAllPredecessors(preval, quadIterator);
	    analyzer.setAllIn(quad, preval);
        } else {
	    meetAllSuccessors(preval, quadIterator);
	    analyzer.setAllOut(quad, preval);
        }
        
        /**///System.out.println("In: " + analyzer.getIn(quad).toString();)
        /**///System.out.println("Out: " + analyzer.getOut(quad).toString());

        // process the quad
        analyzer.processQuad(quad);
        /**///System.out.println("In: " + analyzer.getIn(quad).toString());
        /**///System.out.println("Out: " + analyzer.getOut(quad).toString());

        // check if the node's value changed and return the flag
        Map<Quad, ExFlow.DataflowObject> newValue = new HashMap<Quad, ExFlow.DataflowObject>();
        if (analyzer.isForward()) {
            for(Quad succ : quadIterator.successors1())
                newValue.put(succ, analyzer.getOut(quad,succ));
        } else {
            for(Quad pred : quadIterator.predecessors1())
                newValue.put(pred, analyzer.getIn(quad,pred));
        }
	return !newValue.equals(postval);
    }

    /**
     * Perform the meet of a dataflow object with all the predecessor
     * dataflow objects.
     * @param quadIn the in object to meet with
     * @param quadIterator the quad iterator pointing to the quad
     */
    private void meetAllPredecessors(ExFlow.DataflowObject quadIn,
                                     QuadIterator quadIterator) {
        // get all predecessors
        Iterator<Quad> iterator = quadIterator.predecessors();
        // meet with all the predecessors' Out dataflow objects
        Quad quad;
        while (iterator.hasNext()) {
            quad = iterator.next();
            // use the entry value where appropriate
            if (quad == null) {
                quadIn.meetWith(analyzer.getEntry());
            } else {
                quadIn.meetWith(analyzer.getOut(quad, quadIterator.getCurrentQuad()));
            }
        }
    }

    /**
     * Perform the meet of a dataflow object with all the successor
     * dataflow objects.
     * @param quadOut the in object to meet with
     * @param quadIterator the quad iterator pointing to the quad
     */
    private void meetAllSuccessors(ExFlow.DataflowObject quadOut,
                                   QuadIterator quadIterator) {
        // get all successors
        Iterator<Quad> iterator = quadIterator.successors();
        // meet with all the successors' In dataflow objects
        Quad quad;
        while (iterator.hasNext()) {
            quad = iterator.next();
            // use the exit value where appropriate
            if (quad == null) {
                quadOut.meetWith(analyzer.getExit());
            } else {
                quadOut.meetWith(analyzer.getIn(quad, quadIterator.getCurrentQuad()));
            }
        }
    }

    /**
     * Calculate the final value of the exit node, based on the quads that
     * are its predecessors.
     * @param cfg the control flow graph of the exit node
     */
    private void calculateExit(ControlFlowGraph cfg) {
        // get a new exit value
        ExFlow.DataflowObject newExit = analyzer.newTempVar();
        // reset its value
        newExit.setToTop();
        // meet its value with all the Outs of the exit's predecessors
        QuadIterator quadIterator = new QuadIterator(cfg);
        while (quadIterator.hasNext()) {
            Quad quad = quadIterator.next();
            // check if the quad is the exit's predecessor
            if (isExitPredecessor(quadIterator)) // meet with its Out value
                newExit.meetWith(analyzer.getOut(quad, null));
        }
	// set the new exit value
	analyzer.setExit(newExit);
    }

    /**
     * Calculate the final value of the entry node, based on the quads that
     * are its successors.
     * @param cfg the control flow graph of the entry node
     */
    private void calculateEntry(ControlFlowGraph cfg) {
        // get a new entry value
        ExFlow.DataflowObject newEntry = analyzer.newTempVar();
        // reset its value
        newEntry.setToTop();
        // meet its value with all the Ins of the entry's successors
        QuadIterator quadIterator = new QuadIterator(cfg);
        while (quadIterator.hasNext()) {
            Quad quad = quadIterator.next();
            // check if the quad is the entry's successor
            if (isEntrySuccessor(quadIterator)) // meet with its In value
                newEntry.meetWith(analyzer.getIn(quad, null));
        }
	// set the new entry value
	analyzer.setEntry(newEntry);
    }

    /**
     * Determine whether a given quad (pointed by a quad iterator) is a
     * predecessor of the exit node.
     * @param quadIterator the quad iterator pointing to the quad
     * @return true if the quad is the exit's predecessor, false otherwise
     */
    private boolean isExitPredecessor(QuadIterator quadIterator) {
        // exit if a successor is null
        return quadIterator.successors1().contains(null);
    }

    /**
     * Determine whether a given quad (pointed by a quad iterator) is a
     * successor of the entry node.
     * @param quadIterator the quad iterator pointing to the quad
     * @return true if the quad is the entry's successor, false otherwise
     */
    private boolean isEntrySuccessor(QuadIterator quadIterator) {
        // entry if a predecessor is null
        return quadIterator.predecessors1().contains(null);
    }
}
