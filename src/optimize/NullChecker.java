package optimize;

import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.*;
import joeq.Compiler.Quad.Operator.*;
import java.util.*;

class NullChecker implements ExFlow.Analysis
{
    public static class VarSet implements ExFlow.DataflowObject
    {
        private Set<String> set;
        public static Set<String> universalSet;
        public VarSet()
        {
            set = new TreeSet<String>(universalSet);
        }
        
        @Override
	    public void setToTop() {set = new TreeSet<String>(universalSet); }
        @Override
	    public void setToBottom() {set = new TreeSet<String>();}
        
        @Override
	    public void meetWith(ExFlow.DataflowObject o)
        {
            VarSet a = (VarSet)o;
            set.retainAll(a.set);
        }
        
        @Override
	    public void copy(ExFlow.DataflowObject o)
	{
	    VarSet a = (VarSet)o;
	    set = new TreeSet<String>(a.set);
	}
        
	@Override
            public boolean equals(Object o)
	{
	    if (o instanceof VarSet)
		{
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

    private Map<Quad, VarSet> in;
    private Map<Quad, Map<Quad, VarSet>> out;
    private VarSet entry;
    private VarSet exit;

    @Override
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

	Set<String> s = new TreeSet<String>();
	VarSet.universalSet = s;

	in = new HashMap<Quad, VarSet>();
	out = new HashMap<Quad, Map<Quad, VarSet>>();
	entry = new VarSet();
	exit = new VarSet();
	qit = new QuadIterator(cfg);
	    
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

        qit = new QuadIterator(cfg);
        while (qit.hasNext())
	    {
		Quad q = qit.next();
		VarSet tmp = newTempVar();
		tmp.setToBottom();
		in.put(q, tmp);
		out.put(q, new HashMap<Quad, VarSet>());
		for(Quad succ : qit.successors1())
		    {
			tmp = newTempVar();
			tmp.setToBottom();
			out.get(q).put(succ, tmp);
		    }
	    }
	entry.setToBottom();
	exit.setToBottom();
	    

	transferfn.val = new VarSet();
    }

    @Override
	public void postprocess(ControlFlowGraph cfg)
    {
	SortedSet<Integer> redundantChecks = new TreeSet<Integer>();
	for(QuadIterator qit = new QuadIterator(cfg); qit.hasNext();)
	    {
		Quad q = qit.next();
		if(q.getOperator() instanceof Operator.NullCheck)
		    {
			System.out.println("Candidate");
			System.out.println(getAllIn(q));
			System.out.println(getAllOut(q));
			if(getAllIn(q).equals(getAllOut(q)))
			    {
				System.out.println("Removed");
				qit.remove();
			    }
		    }
	    }
    }
	
    @Override
	public boolean isForward() { return true; }

    @Override
	public ExFlow.DataflowObject getEntry()
    {
	ExFlow.DataflowObject result = newTempVar();
        result.copy(entry);
	return result;
    }

    @Override
	public ExFlow.DataflowObject getExit()
    {
	ExFlow.DataflowObject result = newTempVar();
	result.copy(exit);
	return result;
    }

    @Override
	public ExFlow.DataflowObject getIn(Quad q, Quad pred)
    {
	throw new UnsupportedOperationException("In is always unified");
	/*
        ExFlow.DataflowObject result = newTempVar();
        if(in.get(q).containsKey(pred))
	    {
		result.copy(in.get(q).get(pred));
	    }
        else
	    {
		result.copy(in.get(q).get(null));
	    }
	return result;
	*/
    }

    @Override
	public ExFlow.DataflowObject getAllIn(Quad q)
    {
	ExFlow.DataflowObject result = newTempVar();
	result.copy(in.get(q));
	return result;
    }

    @Override
	public ExFlow.DataflowObject getOut(Quad q, Quad succ)
    {
	ExFlow.DataflowObject result = newTempVar();
	result.copy(out.get(q).get(succ));
        return result;
    }

    @Override
	public ExFlow.DataflowObject getAllOut(Quad q)
    {
	ExFlow.DataflowObject result = newTempVar();
	result.setToTop();
	for(Quad k : out.get(q).keySet())
	    result.meetWith(out.get(q).get(k));
	return result;
    }

    @Override
	public void setIn(Quad q, Quad pred, ExFlow.DataflowObject value)
    {
	throw new UnsupportedOperationException("In must be unified");
	/*
        if(!in.get(q).containsKey(pred))
	    {
		in.get(q).put(pred, newTempVar());
	    }
        System.out.println(in.get(q).get(pred));
        in.get(q).get(pred).copy(value);
        System.out.println(in.get(q).get(pred));
	*/
    }

    @Override
	public void setOut(Quad q, Quad succ, ExFlow.DataflowObject value)
    {
        if(!out.get(q).containsKey(succ))
	    {
		out.get(q).put(succ, newTempVar());
	    }
        out.get(q).get(succ).copy(value);
    }

    public void setAllIn(Quad q, ExFlow.DataflowObject value)
    {
	assert(in.containsKey(q));
	in.put(q, (VarSet)value);
    }

    public void setAllOut(Quad q, ExFlow.DataflowObject value)
    {
        for(Quad k : out.get(q).keySet())
            out.get(q).get(k).copy(value);
    }

    @Override
	public void setEntry(ExFlow.DataflowObject value)
    {
	entry.copy(value);
    }

    @Override
	public void setExit(ExFlow.DataflowObject value)
    {
	exit.copy(value);
    }

    @Override
	public VarSet newTempVar() { return new VarSet(); }

    private TransferFunction transferfn = new TransferFunction();

    @Override
	public void processQuad(Quad q, ControlFlowGraph cfg) {
        System.out.println("Processing: "+q);
        System.out.println(in.get(q));
	transferfn.val.copy(getAllIn(q));
        System.out.println(transferfn.val);
	transferfn.visitQuad(q);
        setAllOut(q, transferfn.val);
    }

    public static class TransferFunction extends QuadVisitor.EmptyVisitor
    {
	    
	VarSet val;
	@Override
	    public void visitQuad(Quad q)
	{
            System.out.println("preval:  " + val);
            for (RegisterOperand def : q.getDefinedRegisters())
                val.killVar(def.getRegister().toString());
            for (RegisterOperand use : q.getUsedRegisters())
                if(q.getOperator() instanceof Operator.NullCheck)
                    val.genVar(use.getRegister().toString());
            System.out.println("postval: " + val);
	}
    }
}
