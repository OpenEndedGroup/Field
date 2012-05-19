/*
 * Copyright 2001-2005 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id: TypesOpcode.java 1175 2005-01-05 20:20:17Z gbevin $
 */
package field.bytecode.protect.analysis;

abstract class TypesOpcode {
	static final byte SET = 1;

	static final byte GET = 2;

	static final byte IINC = 3;

	static final byte POP = 4;

	static final byte POP2 = 5;

	static final byte PUSH = 6;

	static final byte AALOAD = 7;

	static final byte DUP = 8;

	static final byte DUPX1 = 9;

	static final byte DUPX2 = 10;

	static final byte DUP2 = 11;

	static final byte DUP2_X1 = 12;

	static final byte DUP2_X2 = 13;

	static final byte SWAP = 14;

	static final byte PAUSE = 15;

	static final byte LABEL = 16;

	public static String toString(byte opcode) {
		switch (opcode) {
		case SET:
			return "SET";
		case GET:
			return "GET";
		case IINC:
			return "IINC";
		case POP:
			return "POP";
		case POP2:
			return "POP2";
		case PUSH:
			return "PUSH";
		case AALOAD:
			return "AALOAD";
		case DUP:
			return "DUP";
		case DUPX1:
			return "DUPX1";
		case DUPX2:
			return "DUPX2";
		case DUP2:
			return "DUP2";
		case DUP2_X1:
			return "DUP2_X1";
		case DUP2_X2:
			return "DUP2_X2";
		case SWAP:
			return "SWAP";
		case PAUSE:
			return "PAUSE";
		case LABEL:
			return "LABEL";
		}

		return null;
	}
}
