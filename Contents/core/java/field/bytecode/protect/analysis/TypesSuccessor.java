/*
 * Copyright 2001-2005 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id: TypesSuccessor.java 1175 2005-01-05 20:20:17Z gbevin $
 */
package field.bytecode.protect.analysis;

import org.objectweb.asm.Label;

class TypesSuccessor {
	private Label mLabel = null;

	private TypesSuccessor mNextSuccessor = null;

	void setNextSuccessor(TypesSuccessor nextSuccessor) {
		mNextSuccessor = nextSuccessor;
	}

	TypesSuccessor getNextSuccessor() {
		return mNextSuccessor;
	}

	void setLabel(Label successor) {
		mLabel = successor;
	}

	Label getLabel() {
		return mLabel;
	}
}
