package optimize;

import joeq.Compiler.Quad.*;
import joeq.Main.Helper;
import joeq.Class.*;


class PrintQuads {
    public static class QuadPrinter implements ControlFlowGraphVisitor {
        public void visitCFG(ControlFlowGraph cfg) {
            QuadIterator i = new QuadIterator(cfg);
            while(i.hasNext())
                System.out.println(i.next().toString());
        }
    }
    
    public static void main(String[] args) {
        jq_Class[] classes = new jq_Class[args.length];
        for (int i=0; i < classes.length; i++)
            classes[i] = (jq_Class)Helper.load(args[i]);
        
        for (int i=0; i < classes.length; i++) {
            System.out.println("Class: "+classes[i].getName());
            Helper.runPass(classes[i], new joeq.Compiler.Quad.PrintCFG());
        }
    }
}
