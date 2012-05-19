// $Id: ClEditConstraint.java,v 1.10 1999/04/20 00:26:28 gjb Exp $
//
// Cassowary Incremental Constraint Solver
// Original Smalltalk Implementation by Alan Borning
// This Java Implementation by Greg J. Badros, <gjb@cs.washington.edu>
// http://www.cs.washington.edu/homes/gjb
// (C) 1998, 1999 Greg J. Badros and Alan Borning
// See ../LICENSE for legal details regarding this software
//
// ClEditConstraint
//
package field.core.plugins.constrain.cassowary;

public class ClEditConstraint extends ClEditOrStayConstraint {
	public ClEditConstraint(ClVariable clv, ClStrength strength, double weight) {
		super(clv, strength, weight);
	}

	public ClEditConstraint(ClVariable clv, ClStrength strength) {
		super(clv, strength);
	}

	public ClEditConstraint(ClVariable clv) {
		super(clv);
	}

	@Override
	public boolean isEditConstraint() {
		return true;
	}

	@Override
	public String toString() {
		return "edit" + super.toString();
	}
}
