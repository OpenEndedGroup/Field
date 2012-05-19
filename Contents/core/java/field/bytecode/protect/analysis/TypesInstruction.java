/*
 * Copyright 2001-2005 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id: TypesInstruction.java 1175 2005-01-05 20:20:17Z gbevin $
 */
package field.bytecode.protect.analysis;

class TypesInstruction {
	private byte mOpcode = -1;

	private int mArgument = -1;

	private String mType = null;

	public TypesInstruction(byte opcode) {
		mOpcode = opcode;
	}

	public TypesInstruction(byte opcode, String type) {
		mOpcode = opcode;
		mType = type;
	}

	public TypesInstruction(byte opcode, int argument) {
		mOpcode = opcode;
		mArgument = argument;
	}

	public TypesInstruction(byte opcode, int argument, String type) {
		mOpcode = opcode;
		mArgument = argument;
		mType = type;
	}

	public byte getOpcode() {
		return mOpcode;
	}

	public int getArgument() {
		return mArgument;
	}

	public String getType() {
		return mType;
	}

	public String toString() {
		StringBuffer result = new StringBuffer(TypesOpcode.toString(mOpcode));
		if (mArgument != -1) {
			result.append(", ");
			result.append(mArgument);
		}
		if (mType != null) {
			result.append(", ");
			result.append(mType);
		}

		return result.toString();
	}
}
