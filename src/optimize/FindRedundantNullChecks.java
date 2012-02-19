package optimize;

import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.*;
import joeq.Compiler.Quad.Operator.*;
import joeq.Main.Helper;
import joeq.Class.*;
import java.util.*;


class FindRedundantNullChecks
{
    public static class NullChecker implements Flow.Analysis
    {
	public static class VarSet implements Flow.DataflowObject
	{
	    private Set<String> set;
	    public static Set<String> universalSet;
	    public VarSet()
	    {
		set = new TreeSet<String>(universalSet);
	    }

	    public void setToTop() {set = new TreeSet<String>(universalSet); }
	    public void setToBottom() {set = new TreeSet<String>();}

	    public void meetWith(Flow.DataflowObject o)
	    {
		VarSet a = (VarSet)o;
		set.retainAll(a.set);
	    }

	    public void copy(Flow.DataflowObject o)
	    {
		VarSet a = (VarSet)o;
		set = new TreeSet<String>(a.set);
	    }

	    @Override
	    public boolean equals(Object o)
	    {
		if (o instanceof VarSet) {
		    VarSet a = (VarSet) o;
		    return set.equals(a.set);
		}
		return false;
	    }
	    
	    @Override
	    public int hashCode() {
		return set.hashCode();
	    }

	    @Override
	    public String toString() {
		return set.toString();
	    }

	    public void genVar(String v) {set.add(v);}
	    public void killVar(String v) {set.remove(v);}
	}

	private VarSet[] in, out;
	private VarSet entry, exit;

	public void preprocess(ControlFlowGraph cfg)
	{
	    QuadIterator qit = new QuadIterator(cfg);
	    int max = 0;
	    while (qit.hasNext())
	    {
		int x = qit.next().getID();
		if (x > max) max = x;
	    }
	    max += 1;
	    in = new VarSet[max];
	    out = new VarSet[max];
	    qit = new QuadIterator(cfg);
	    
	    Set<String> s = new TreeSet<String>();
	    VarSet.universalSet = s;
	    
	    int numargs = cfg.getMethod().getParamTypes().length;
	    for (int i =0; i < numargs; i++)
	    {
		s.add("R"+i);
	    }
	    
	    while (qit.hasNext())
	    {
		Quad q = qit.next();
		for (RegisterOperand def : q.getDefinedRegisters()) {
		    s.add(def.getRegister().toString());
		}
		for (RegisterOperand use : q.getUsedRegisters()) {
		    s.add(use.getRegister().toString());
		}
	    }
	    
	    entry = new VarSet();
	    entry.setToBottom();
	    exit = new VarSet();
	    exit.setToBottom();
	    
	    transferfn.val = new VarSet();
	    for (int i = 0; i < in.length; i++)
	    {
		in[i] = new VarSet();
		in[i].setToBottom();
		out[i] = new VarSet();
		out[i].setToBottom();
	    }
	}

	public void postprocess(ControlFlowGraph cfg)
	{
	    System.out.print(cfg.getMethod().getName());
	    SortedSet<Integer> redundantChecks = new TreeSet<Integer>();
	    for(QuadIterator qit = new QuadIterator(cfg); qit.hasNext();)
	    {
		if(((Quad)qit.next()).getOperator() instanceof Operator.NullCheck)
		{
		    if(in[qit.getCurrentQuad().getID()].equals(out[qit.getCurrentQuad().getID()]))
			redundantChecks.add(qit.getCurrentQuad().getID());
		}
	    }
	    for(int i : redundantChecks)
		System.out.print(" " + i);
	    System.out.println();
	}
	
	public boolean isForward() { return true; }

	public Flow.DataflowObject getEntry()
	{
	    Flow.DataflowObject result = newTempVar();
	    result.copy(entry);
	    return result;
	}

	public Flow.DataflowObject getExit()
	{
	    Flow.DataflowObject result = newTempVar();
	    result.copy(exit);
	    return result;
	}

	public Flow.DataflowObject getIn(Quad q)
	{
	    Flow.DataflowObject result = newTempVar();
	    result.copy(in[q.getID()]);
	    return result;
	}

	public Flow.DataflowObject getOut(Quad q)
	{
	    Flow.DataflowObject result = newTempVar();
	    result.copy(out[q.getID()]);
	    return result;
	}

	public void setIn(Quad q, Flow.DataflowObject value)
	{
	    in[q.getID()].copy(value);
	}

	public void setOut(Quad q, Flow.DataflowObject value)
	{
	    out[q.getID()].copy(value);
	}

	public void setEntry(Flow.DataflowObject value)
	{
	    entry.copy(value);
	}

	public void setExit(Flow.DataflowObject value)
	{
	    exit.copy(value);
	}

	public Flow.DataflowObject newTempVar() { return new VarSet(); }

	private TransferFunction transferfn = new TransferFunction();

	public void processQuad(Quad q) {
	    transferfn.val.copy(in[q.getID()]);
	    transferfn.visitQuad(q);
	    out[q.getID()].copy(transferfn.val);
	}

	public static class TransferFunction extends QuadVisitor.EmptyVisitor
	{
	    
	    VarSet val;
	    @Override
	    public void visitQuad(Quad q)
	    {
		//System.out.println(q);
		for (RegisterOperand def : q.getDefinedRegisters())
		    val.killVar(def.getRegister().toString());
		for (RegisterOperand use : q.getUsedRegisters())
		    if(q.getOperator() instanceof Operator.NullCheck)
			val.genVar(use.getRegister().toString());
	    }
	}
    }

    
    
    public static void main(String[] args)
    {
	Flow.Analysis analyzer = new NullChecker();
	Flow.Solver solver = new ReferenceSolver();
	solver.registerAnalysis(analyzer);

        jq_Class[] classes = new jq_Class[args.length];
	for (int i=0; i < classes.length; i++)
            classes[i] = (jq_Class)Helper.load(args[i]);
        
        for (int i=0; i < classes.length; i++)
	{
            Helper.runPass(classes[i], solver);
        }
    }
}
