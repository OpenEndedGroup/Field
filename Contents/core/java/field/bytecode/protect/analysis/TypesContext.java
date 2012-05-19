/*
 * Copyright 2001-2005 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id: TypesContext.java 2098 2005-07-06 17:53:19Z gbevin $
 */
package field.bytecode.protect.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import org.objectweb.asm.Type;

public class TypesContext implements Cloneable {
	public final static String CAT1_BOOLEAN = "1Z";

	public final static String CAT1_CHAR = "1C";

	public final static String CAT1_FLOAT = "1F";

	public final static String CAT1_BYTE = "1B";

	public final static String CAT1_SHORT = "1S";

	public final static String CAT1_INT = "1I";

	public final static String CAT1_ADDRESS = "1A";

	public final static String CAT2_DOUBLE = "2D";

	public final static String CAT2_LONG = "2J";

	public final static String ARRAY_BOOLEAN = "[Z";

	public final static String ARRAY_CHAR = "[C";

	public final static String ARRAY_FLOAT = "[F";

	public final static String ARRAY_BYTE = "[B";

	public final static String ARRAY_SHORT = "[S";

	public final static String ARRAY_INT = "[I";

	public final static String ARRAY_DOUBLE = "[D";

	public final static String ARRAY_LONG = "[J";

	public final static String NULL = "NULL";

	private Map mVars = null;

	private Stack mStack = null;

	private int mSort = TypesNode.REGULAR;

	private String mDebugIndent = null;

	TypesContext() {
		mVars = new HashMap();
		mStack = new Stack();
	}

	TypesContext(Map vars, Stack stack) {
		mVars = vars;
		mStack = stack;
	}

	public Map getVars() {
//		return mVars;
		
		return new TreeMap<Integer, String>(mVars);
		
	}

	public Stack getStack() {
		return mStack;
	}

	public boolean hasVar(int var) {
		return mVars.containsKey(new Integer(var));
	}

	public String getVar(int var) {
		return (String) mVars.get(new Integer(var));
	}

	public void setVar(int var, String type) {
		mVars.put(new Integer(var), type);
	}

	public int getVarType(int var) {
		String type = getVar(var);
		if (CAT1_INT == type) {
			return Type.INT;
		} else if (CAT1_FLOAT == type) {
			return Type.FLOAT;
		} else if (CAT2_LONG == type) {
			return Type.LONG;
		} else if (CAT2_DOUBLE == type) {
			return Type.DOUBLE;
		} else {
			return Type.OBJECT;
		}
	}

	public String peek() {
		return (String) mStack.peek();
	}

	public String pop() {
		String result = null;
		if (mStack.size() > 0) {
			result = (String) mStack.pop();
		}
		printStack();
		return result;
	}

	public void push(String type) {
		mStack.push(type);
		printStack();
	}

	public Stack getStackClone() {
		return (Stack) mStack.clone();
	}

	public void cloneVars() {
		mVars = new HashMap(mVars);
	}

	public void setSort(int type) {
		mSort = type;
	}

	public int getSort() {
		return mSort;
	}

	void printStack() {
	}

	void setDebugIndent(String debugIndent) {
		mDebugIndent = debugIndent;
	}

	TypesContext clone(TypesNode node) {
		TypesContext new_context = new TypesContext(new HashMap(mVars), (Stack) mStack.clone());
		new_context.setSort(node.getSort());
		return new_context;
	}

	public Object clone() {
		TypesContext new_context = null;
		try {
			new_context = (TypesContext) super.clone();
		} catch (CloneNotSupportedException e) {
			// this should never happen
			e.printStackTrace();
		}

		new_context.mVars = new HashMap(mVars);
		new_context.mStack = (Stack) mStack.clone();

		return new_context;
	}
}
