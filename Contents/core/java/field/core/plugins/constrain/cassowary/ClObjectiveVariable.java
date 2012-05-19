// $Id: ClObjectiveVariable.java,v 1.9 1999/04/20 00:26:34 gjb Exp $
//
// Cassowary Incremental Constraint Solver
// Original Smalltalk Implementation by Alan Borning
// This Java Implementation by Greg J. Badros, <gjb@cs.washington.edu>
// http://www.cs.washington.edu/homes/gjb
// (C) 1998, 1999 Greg J. Badros and Alan Borning
// See ../LICENSE for legal details regarding this software
//
// ClObjectiveVariable
//
package field.core.plugins.constrain.cassowary;

class ClObjectiveVariable extends ClAbstractVariable {
	public ClObjectiveVariable(String name) {
		super(name);
	}

	public ClObjectiveVariable(long number, String prefix) {
		super(number, prefix);
	}

	@Override
	public String toString()
	//    { return "[" + name() + ":obj:" + hashCode() + "]"; }
	{
		return "[" + name() + ":obj]";
	}

	@Override
	public boolean isExternal() {
		return false;
	}

	@Override
	public boolean isPivotable() {
		return false;
	}

	@Override
	public boolean isRestricted() {
		return false;
	}
}
