/*
 * Copyright 2001-2005 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id: TypesNode.java 1175 2005-01-05 20:20:17Z gbevin $
 */
package field.bytecode.protect.analysis;

import java.util.ArrayList;
import java.util.Collection;

import org.objectweb.asm.Label;

class TypesNode {
	final static int REGULAR = 0;

	final static int EXCEPTION = 1;

	private ArrayList mInstructions = new ArrayList();

	private TypesNode mFollowingNode = null;

	private boolean mIsSuccessor = false;

	private TypesSuccessor mSuccessors = null;

	private int mLevel = 0;

	private TypesContext mContext = null;

	private boolean mProcessed = false;

	private TypesNode mNextToProcess = null;

	private TypesNode mPreceeder = null;

	private int mSort = REGULAR;

	void addInstruction(TypesInstruction instruction) {
		mInstructions.add(instruction);
	}

	Collection getInstructions() {
		return mInstructions;
	}

	void setSort(int type) {
		mSort = type;
	}

	int getSort() {
		return mSort;
	}

	void setFollowingNode(TypesNode followingNode) {
		mFollowingNode = followingNode;
	}

	TypesNode getFollowingNode() {
		return mFollowingNode;
	}

	void addSuccessor(Label label) {
		TypesSuccessor successor = new TypesSuccessor();

		successor.setLabel(label);
		successor.setNextSuccessor(getSuccessors());

		setSuccessors(successor);
	}

	void setSuccessors(TypesSuccessor successors) {
		mSuccessors = successors;
	}

	TypesSuccessor getSuccessors() {
		return mSuccessors;
	}

	void setNextToProcess(TypesNode nextNode) {
		mNextToProcess = nextNode;
	}

	TypesNode getNextToProcess() {
		return mNextToProcess;
	}

	void setPreceeder(boolean isSuccessor, TypesNode preceeder) {
		mIsSuccessor = isSuccessor;
		mPreceeder = preceeder;

		if (mIsSuccessor) {
			mLevel = mPreceeder.getLevel() + 1;
		} else {
			mLevel = mPreceeder.getLevel();
		}
	}

	TypesNode getPreceeder() {
		return mPreceeder;
	}

	boolean getIsSuccessor() {
		return mIsSuccessor;
	}

	void setProcessed(boolean processed) {
		mProcessed = processed;
	}

	boolean isProcessed() {
		return mProcessed;
	}

	void setContext(TypesContext previousContext) {
		mContext = previousContext;
	}

	TypesContext getContext() {
		return mContext;
	}

	int getLevel() {
		return mLevel;
	}
}
