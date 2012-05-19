/*
 * Copyright 2001-2005 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id: TypesMethodVisitor.java 1911 2005-05-19 14:06:07Z gbevin $
 */
package field.bytecode.protect.analysis;

import java.util.HashMap;
import java.util.Iterator;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;

public abstract class TypesMethodVisitor implements MethodVisitor, Opcodes {
	private TypesClassVisitor mClassVisitor = null;

	private String mClassName = null;

	private String mClassNameInternal = null;

	private TypesNode mCurrentNode = null;

	private TypesNode mRootNode = null;

	private HashMap mLabelMapping = null;

	private TypesContext[] mPauseContexts = null;

	private TypesContext[] mLabelContexts = null;

	private int mPausecount = -1;

	private int mLabelcount = -1;

	TypesMethodVisitor(TypesClassVisitor classVisitor, String className) {
		mClassVisitor = classVisitor;
		mClassName = className;
		mClassNameInternal = mClassName.replace('.', '/');

		mLabelMapping = new HashMap();

		// pushes the first block onto the stack of blocks to be visited
		mCurrentNode = new TypesNode();
		mRootNode = mCurrentNode;
	}

	/**
	 * Visits a local variable instruction. A local variable instruction is an instruction that loads or stores the value of a local variable.
	 * 
	 * @param opcode
	 *                        the opcode of the local variable instruction to be visited. This opcode is either ILOAD, LLOAD, FLOAD, DLOAD, ALOAD, ISTORE, LSTORE, FSTORE, DSTORE, ASTORE or RET.
	 * @param var
	 *                        the operand of the instruction to be visited. This operand is the index of a local variable.
	 */
	public void visitVarInsn(int opcode, int var) {

		if (mCurrentNode != null) {
			switch (opcode) {
			// store primitive var
			case ISTORE:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.SET, var, TypesContext.CAT1_INT));
				break;
			case FSTORE:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.SET, var, TypesContext.CAT1_FLOAT));
				break;
			case LSTORE:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.SET, var, TypesContext.CAT2_LONG));
				break;
			case DSTORE:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.SET, var, TypesContext.CAT2_DOUBLE));
				break;
			// store reference var
			case ASTORE:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.SET, var, null));
				break;
			// load primitive var
			case ILOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.GET, var, TypesContext.CAT1_INT));
				break;
			case FLOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.GET, var, TypesContext.CAT1_FLOAT));
				break;
			case LLOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.GET, var, TypesContext.CAT2_LONG));
				break;
			case DLOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.GET, var, TypesContext.CAT2_DOUBLE));
				break;
			// load reference var
			case ALOAD:
				if (0 == var) {
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.GET, var, mClassNameInternal));
				} else {
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.GET, var, null));
				}
				break;
			// no stack change, but end of current block (no successor)
			case RET:
				mCurrentNode = null;
				break;
			}
		}
	}

	/**
	 * Visits a method instruction. A method instruction is an instruction that invokes a method.
	 * 
	 * @param opcode
	 *                        the opcode of the type instruction to be visited. This opcode is either INVOKEVIRTUAL, INVOKESPECIAL, INVOKESTATIC or INVOKEINTERFACE.
	 * @param owner
	 *                        the internal name of the method's owner class (see {@link Type#getInternalName getInternalName}).
	 * @param name
	 *                        the method's name.
	 * @param desc
	 *                        the method's descriptor (see {@link Type Type}).
	 */
	public void visitMethodInsn(int opcode, String owner, String name, String desc) {

		if (mCurrentNode != null) {
			String owner_classname = owner.replace('/', '.');

//			if ((ContinuationConfig.getInstance().getContinuableClassOrInterfaceName().equals(owner_classname) || mClassName.equals(owner_classname))
//					&& (("pause".equals(name)) || ("call".equals(name) && "(Ljava/lang/String;)Ljava/lang/Object;".equals(desc)))) 
			if (isYieldCall(owner_classname, name, desc))
			{
				// todo: support parameters dynamically
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));

				// pop the element instance reference from the stack
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));

				// remember the node in which the pause invocation is called
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PAUSE, ++mPausecount));

				return;
			}
			// not the pause invocation
			else {
				// pop off the argument types of the method
				Type[] arguments = Type.getArgumentTypes(desc);
				for (int i = 0; i < arguments.length; i++) {
					Type argument = arguments[i];
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				}

				// pop the objectref from the stack if it'd not a static
				// method invocation
				if (INVOKESTATIC != opcode) {
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				}

				// store the return type of the method
				if (!("<init>".equals(name) && INVOKESPECIAL == opcode)) {
					Type type = Type.getReturnType(desc);
					switch (type.getSort()) {
					case Type.OBJECT:
						mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH,
								type.getInternalName()));
						break;
					case Type.ARRAY:
						mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH,
								type.getDescriptor()));
						break;
					case Type.BOOLEAN:
						mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH,
								TypesContext.CAT1_BOOLEAN));
						break;
					case Type.BYTE:
						mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH,
								TypesContext.CAT1_BYTE));
						break;
					case Type.CHAR:
						mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH,
								TypesContext.CAT1_CHAR));
						break;
					case Type.FLOAT:
						mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH,
								TypesContext.CAT1_FLOAT));
						break;
					case Type.INT:
						mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH,
								TypesContext.CAT1_INT));
						break;
					case Type.SHORT:
						mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH,
								TypesContext.CAT1_SHORT));
						break;
					case Type.DOUBLE:
						mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH,
								TypesContext.CAT2_DOUBLE));
						break;
					case Type.LONG:
						mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH,
								TypesContext.CAT2_LONG));
						break;
					}
				}
			}
		}
	}

	abstract protected boolean isYieldCall(String owner_classname, String name, String desc);

	/**
	 * Visits a type instruction. A type instruction is an instruction that takes a type descriptor as parameter.
	 * 
	 * @param opcode
	 *                        the opcode of the type instruction to be visited. This opcode is either NEW, ANEWARRAY, CHECKCAST or INSTANCEOF.
	 * @param desc
	 *                        the operand of the instruction to be visited. This operand is must be a fully qualified class name in internal form, or the type descriptor of an array type (see {@link Type Type}).
	 */
	public void visitTypeInsn(int opcode, String desc) {

		if (mCurrentNode != null) {
			switch (opcode) {
			case NEW:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, desc));
				break;
			case ANEWARRAY:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				// multi dims new arrays without final dimension specification
				// end up here as an array of arrays
				if (desc.startsWith("[")) {
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, "[" + desc));
				} else {
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, "[L" + desc + ";"));
				}
				break;
			case CHECKCAST:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, desc));
				break;
			case INSTANCEOF:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, desc));
				break;
			}
		}
	}

	/**
	 * Visits a LDC instruction.
	 * 
	 * @param cst
	 *                        the constant to be loaded on the stack. This parameter must be a non null {@link java.lang.Integer Integer}, a {@link java.lang.Float Float}, a {@link java.lang.Long Long}, a {@link java.lang.Double Double} or a {@link String String}.
	 */
	public void visitLdcInsn(Object cst) {

		if (mCurrentNode != null) {
			mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, Type.getInternalName(cst.getClass())));
		}
	}

	/**
	 * Visits a MULTIANEWARRAY instruction.
	 * 
	 * @param desc
	 *                        an array type descriptor (see {@link Type Type}).
	 * @param dims
	 *                        number of dimensions of the array to allocate.
	 */
	public void visitMultiANewArrayInsn(String desc, int dims) {
		if (mCurrentNode != null) {
			for (int i = 1; i <= dims; i++) {
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
			}
			mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, desc));
		}
	}

	/**
	 * Visits a zero operand instruction.
	 * 
	 * @param opcode
	 *                        the opcode of the instruction to be visited. This opcode is either NOP, ACONST_NULL, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, LCONST_0, LCONST_1, FCONST_0, FCONST_1, FCONST_2, DCONST_0, DCONST_1,
	 * 
	 * IALOAD, LALOAD, FALOAD, DALOAD, AALOAD, BALOAD, CALOAD, SALOAD, IASTORE, LASTORE, FASTORE, DASTORE, AASTORE, BASTORE, CASTORE, SASTORE,
	 * 
	 * POP, POP2, DUP, DUP_X1, DUP_X2, DUP2, DUP2_X1, DUP2_X2, SWAP,
	 * 
	 * IADD, LADD, FADD, DADD, ISUB, LSUB, FSUB, DSUB, IMUL, LMUL, FMUL, DMUL, IDIV, LDIV, FDIV, DDIV, IREM, LREM, FREM, DREM, INEG, LNEG, FNEG, DNEG, ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR, IAND, LAND, IOR, LOR, IXOR, LXOR,
	 * 
	 * I2L, I2F, I2D, L2I, L2F, L2D, F2I, F2L, F2D, D2I, D2L, D2F, I2B, I2C, I2S,
	 * 
	 * LCMP, FCMPL, FCMPG, DCMPL, DCMPG,
	 * 
	 * IRETURN, LRETURN, FRETURN, DRETURN, ARETURN, RETURN,
	 * 
	 * ARRAYLENGTH,
	 * 
	 * ATHROW,
	 * 
	 * MONITORENTER, or MONITOREXIT.
	 */
	public void visitInsn(int opcode) {
		if (mCurrentNode != null) {
			switch (opcode) {
			case RETURN:
				mCurrentNode = null;
				break;
			case ATHROW:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				break;
			case ACONST_NULL:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.NULL));
				break;
			case ICONST_0:
			case ICONST_1:
			case ICONST_2:
			case ICONST_3:
			case ICONST_4:
			case ICONST_5:
			case ICONST_M1:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_INT));
				break;
			case FCONST_0:
			case FCONST_1:
			case FCONST_2:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_FLOAT));
				break;
			case LCONST_0:
			case LCONST_1:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT2_LONG));
				break;
			case DCONST_0:
			case DCONST_1:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT2_DOUBLE));
				break;
			case AALOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.AALOAD));
				break;
			case IALOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_INT));
				break;
			case FALOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_FLOAT));
				break;
			case BALOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_BYTE));
				break;
			case CALOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_CHAR));
				break;
			case SALOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_SHORT));
				break;
			case LALOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT2_LONG));
				break;
			case DALOAD:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT2_DOUBLE));
				break;
			case IASTORE:
			case LASTORE:
			case FASTORE:
			case DASTORE:
			case AASTORE:
			case BASTORE:
			case CASTORE:
			case SASTORE:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				break;
			case POP:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				break;
			case POP2:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP2));
				break;
			case DUP:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.DUP));
				break;
			case DUP_X1:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.DUPX1));
				break;
			case DUP_X2:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.DUPX2));
				break;
			case DUP2:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.DUP2));
				break;
			case DUP2_X1:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.DUP2_X1));
				break;
			case DUP2_X2:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.DUP2_X2));
				break;
			case SWAP:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.SWAP));
				break;
			case IADD:
			case LADD:
			case FADD:
			case DADD:
			case ISUB:
			case LSUB:
			case FSUB:
			case DSUB:
			case IMUL:
			case LMUL:
			case FMUL:
			case DMUL:
			case IDIV:
			case LDIV:
			case FDIV:
			case DDIV:
			case IREM:
			case LREM:
			case FREM:
			case DREM:
			case ISHL:
			case LSHL:
			case ISHR:
			case LSHR:
			case IUSHR:
			case LUSHR:
			case IAND:
			case LAND:
			case IOR:
			case LOR:
			case IXOR:
			case LXOR:
				// just pop one type since the result is the same type as both operands
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				break;
			case INEG:
			case LNEG:
			case FNEG:
			case DNEG:
				// do nothing, the type stack remains the same
				break;
			case I2F:
			case I2B:
			case I2C:
			case I2S:
			case L2D:
			case F2I:
			case D2L:
				// do nothing, the type stack remains the same
				break;
			case I2L:
			case F2L:
				// the type widens
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT2_LONG));
				break;
			case I2D:
			case F2D:
				// the type widens
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT2_DOUBLE));
				break;
			case L2I:
			case D2I:
				// the type shrinks
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_INT));
				break;
			case L2F:
			case D2F:
				// the type shrinks
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_FLOAT));
				break;
			case LCMP:
			case FCMPL:
			case FCMPG:
			case DCMPL:
			case DCMPG:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_INT));
				break;
			case ARRAYLENGTH:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_INT));
				break;
			case MONITORENTER:
			case MONITOREXIT:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				break;
			}
		}
	}

	/**
	 * Visits an IINC instruction.
	 * 
	 * @param var
	 *                        index of the local variable to be incremented.
	 * @param increment
	 *                        amount to increment the local variable by.
	 */
	public void visitIincInsn(int var, int increment) {
		if (mCurrentNode != null) {
			mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.IINC, var));
		}
	}

	/**
	 * Visits a field instruction. A field instruction is an instruction that loads or stores the value of a field of an object.
	 * 
	 * @param opcode
	 *                        the opcode of the type instruction to be visited. This opcode is either GETSTATIC, PUTSTATIC, GETFIELD or PUTFIELD.
	 * @param owner
	 *                        the internal name of the field's owner class (see {@link Type#getInternalName getInternalName}).
	 * @param name
	 *                        the field's name.
	 * @param desc
	 *                        the field's descriptor (see {@link Type Type}).
	 */
	public void visitFieldInsn(int opcode, String owner, String name, String desc) {
		if (mCurrentNode != null) {
			switch (opcode) {
			case GETFIELD:
			case GETSTATIC: {
				// pop the objectref from the stack if it'd not a static
				// field access
				if (GETSTATIC != opcode) {
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				}

				Type type = Type.getType(desc);
				switch (type.getSort()) {
				case Type.OBJECT:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, type.getInternalName()));
					break;
				case Type.ARRAY:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, type.getDescriptor()));
					break;
				case Type.BOOLEAN:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_BOOLEAN));
					break;
				case Type.BYTE:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_BYTE));
					break;
				case Type.CHAR:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_CHAR));
					break;
				case Type.FLOAT:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_FLOAT));
					break;
				case Type.INT:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_INT));
					break;
				case Type.SHORT:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_SHORT));
					break;
				case Type.DOUBLE:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT2_DOUBLE));
					break;
				case Type.LONG:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT2_LONG));
					break;
				}
			}

				break;
			case PUTFIELD:
			case PUTSTATIC:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));

				// pop the objectref from the stack if it'd not a static
				// field access
				if (GETSTATIC != opcode) {
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				}

				break;
			}
		}
	}

	/**
	 * Visits an instruction with a single int operand.
	 * 
	 * @param opcode
	 *                        the opcode of the instruction to be visited. This opcode is either BIPUSH, SIPUSH or NEWARRAY.
	 * @param operand
	 *                        the operand of the instruction to be visited.
	 */
	public void visitIntInsn(int opcode, int operand) {

		if (mCurrentNode != null) {
			switch (opcode) {
			case BIPUSH:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_BYTE));
				break;
			case SIPUSH:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_SHORT));
				break;
			case NEWARRAY:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				switch (operand) {
				case T_BOOLEAN:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.ARRAY_BOOLEAN));
					break;
				case T_CHAR:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.ARRAY_CHAR));
					break;
				case T_FLOAT:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.ARRAY_FLOAT));
					break;
				case T_DOUBLE:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.ARRAY_DOUBLE));
					break;
				case T_BYTE:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.ARRAY_BYTE));
					break;
				case T_SHORT:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.ARRAY_SHORT));
					break;
				case T_INT:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.ARRAY_INT));
					break;
				case T_LONG:
					mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.ARRAY_LONG));
					break;
				}
				break;
			}
		}
	}

	/**
	 * Visits a try catch block.
	 * 
	 * @param start
	 *                        beginning of the exception handler's scope (inclusive).
	 * @param end
	 *                        end of the exception handler's scope (exclusive).
	 * @param handler
	 *                        beginning of the exception handler's code.
	 * @param type
	 *                        internal name of the type of exceptions handled by the handler, or <tt>null</tt> to catch any exceptions (for "finally" blocks).
	 * @throws IllegalArgumentException
	 *                         if one of the labels has not already been visited by this visitor (by the {@link #visitLabel visitLabel} method).
	 */
	public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {

		TypesNode try_node = (TypesNode) mLabelMapping.get(start);
		if (try_node != null) {
			try_node.addSuccessor(handler);
		}
	}

	/**
	 * Visits a LOOKUPSWITCH instruction.
	 * 
	 * @param dflt
	 *                        beginning of the default handler block.
	 * @param keys
	 *                        the values of the keys.
	 * @param labels
	 *                        beginnings of the handler blocks. <tt>labels[i]</tt> is the beginning of the handler block for the <tt>keys[i]</tt> key.
	 */
	public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {

		if (mCurrentNode != null) {
			mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));

			// add all the switch's successors
			mCurrentNode.addSuccessor(dflt);
			for (int i = 0; i < labels.length; ++i) {
				mCurrentNode.addSuccessor(labels[i]);
			}

			// end the current node
			mCurrentNode = null;
		}
	}

	/**
	 * Visits a jump instruction. A jump instruction is an instruction that may jump to another instruction.
	 * 
	 * @param opcode
	 *                        the opcode of the type instruction to be visited. This opcode is either IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE, IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE, IF_ACMPEQ, IF_ACMPNE, GOTO, JSR, IFNULL or IFNONNULL.
	 * @param label
	 *                        the operand of the instruction to be visited. This operand is a label that designates the instruction to which the jump instruction may jump.
	 */
	public void visitJumpInsn(int opcode, Label label) {

		if (mCurrentNode != null) {
			switch (opcode) {
			case IFEQ:
			case IFNE:
			case IFLT:
			case IFGE:
			case IFGT:
			case IFLE:
			case IFNULL:
			case IFNONNULL:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addSuccessor(label);
				break;
			case IF_ICMPEQ:
			case IF_ICMPNE:
			case IF_ICMPLT:
			case IF_ICMPGE:
			case IF_ICMPGT:
			case IF_ICMPLE:
			case IF_ACMPEQ:
			case IF_ACMPNE:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));
				mCurrentNode.addSuccessor(label);
				break;
			case JSR:
				mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, TypesContext.CAT1_ADDRESS));
				mCurrentNode.addSuccessor(label);
				break;
			case GOTO:
				mCurrentNode.addSuccessor(label);

				// end the current node
				mCurrentNode = null;
				break;
			}
		}
	}

	/**
	 * Visits a label. A label designates the instruction that will be visited just after it.
	 * 
	 * @param label
	 *                        a {@link Label Label} object.
	 */
	public void visitLabel(Label label) {

		// begins a new current block
		TypesNode new_node = new TypesNode();
		mLabelMapping.put(label, new_node);

		if (mCurrentNode != null) {
			mCurrentNode.setFollowingNode(new_node);
		}
		mCurrentNode = new_node;

		mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.LABEL, ++mLabelcount));

		// // if the label starts with an exception type, change the sort of
		// // the created node and add the exception type as the the first type
		// // on the stack
		// String exception_type = mClassVisitor.getMetrics().nextExceptionType();
		// if (exception_type != null) {
		// mCurrentNode.setSort(TypesNode.EXCEPTION);
		// mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.PUSH, exception_type));
		// }
	}

	/**
	 * Visits a TABLESWITCH instruction.
	 * 
	 * @param min
	 *                        the minimum key value.
	 * @param max
	 *                        the maximum key value.
	 * @param dflt
	 *                        beginning of the default handler block.
	 * @param labels
	 *                        beginnings of the handler blocks. <tt>labels[i]</tt> is the beginning of the handler block for the <tt>min + i</tt> key.
	 */
	public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {

		if (mCurrentNode != null) {
			mCurrentNode.addInstruction(new TypesInstruction(TypesOpcode.POP));

			// add all the switch's successors
			mCurrentNode.addSuccessor(dflt);
			for (int i = 0; i < labels.length; ++i) {
				mCurrentNode.addSuccessor(labels[i]);
			}

			// end the current node
			mCurrentNode = null;
		}
	}

	/**
	 * Visits the maximum stack size and the maximum number of local variables of the method.
	 * 
	 * @param maxStack
	 *                        maximum stack size of the method.
	 * @param maxLocals
	 *                        maximum number of local variables for the method.
	 */
	public void visitMaxs(int maxStack, int maxLocals) {

		mPauseContexts = new TypesContext[mPausecount + 1];
		mLabelContexts = new TypesContext[mLabelcount + 1];

		TypesNode following_node = null;
		TypesNode successor_node = null;

		// control flow analysis algorithm
		TypesNode stack = mRootNode;
		while (stack != null) {
			// pops a block from the stack
			TypesNode node = stack;
			stack = stack.getNextToProcess();

			// process the node's instructions
			processInstructions(node);

			// analyses the node's successors
			TypesSuccessor successor = node.getSuccessors();
			while (successor != null) {
				successor_node = (TypesNode) mLabelMapping.get(successor.getLabel());

				if (!successor_node.isProcessed()) {
					successor_node.setProcessed(true);
					successor_node.setPreceeder(true, node);

					// push the previous node on the stack
					successor_node.setNextToProcess(stack);
					stack = successor_node;
				}

				// iterate through the successors
				successor = successor.getNextSuccessor();
			}
			// handle a possible following node
			if (node.getFollowingNode() != null && !node.getFollowingNode().isProcessed()) {
				following_node = node.getFollowingNode();

				following_node.setProcessed(true);
				following_node.setPreceeder(false, node);

				// push the previous node on the stack
				following_node.setNextToProcess(stack);
				stack = following_node;
			}
		}

		mClassVisitor.setPauseContexts(mPauseContexts);
		mClassVisitor.setLabelContexts(mLabelContexts);
	}

	private void processInstructions(TypesNode node) {
		// setup the context for the node
		TypesContext context = null;
		// if it's the first node, create a new context
		if (null == node.getPreceeder()) {
			context = new TypesContext();
		}
		// otherwise retrieve the previous context
		else {
			TypesContext preceeder_context = node.getPreceeder().getContext();
			// always isolate the context for a successor
			if (node.getIsSuccessor()) {
				context = (TypesContext) preceeder_context.clone();
			} else {
				context = new TypesContext(preceeder_context.getVars(), preceeder_context.getStackClone());
			}
		}
		node.setContext(context);

		TypesContext exception_context = null;
		for (Iterator iterator = node.getInstructions().iterator(); iterator.hasNext();) {
			TypesInstruction instruction = (TypesInstruction) iterator.next();
			switch (instruction.getOpcode()) {
			case TypesOpcode.SET: {
				String type = context.peek();
				if (instruction.getType() != null) {
					type = instruction.getType();
				}

				// if the variables types in the scope change or if a new var is added,
				// ensure that the context vars are isolated
				String current_var_type = context.getVar(instruction.getArgument());
				if (node.getPreceeder() != null
						&& context.getVars() == node.getPreceeder().getContext().getVars()
						&& (null == current_var_type || (current_var_type != TypesContext.NULL && !current_var_type.equals(type)))) {
					context.cloneVars();
				}

				context.pop();
				context.setVar(instruction.getArgument(), type);
				if (exception_context != null) {
					exception_context.setVar(instruction.getArgument(), type);
					exception_context = null;
				}
			}
				break;
			case TypesOpcode.GET: {
				String type = instruction.getType();
				if (null == type) {
					type = context.getVar(instruction.getArgument());
				}

				context.push(type);
			}
				break;
			case TypesOpcode.IINC:
				// do nothing
				break;
			case TypesOpcode.POP: {

				context.pop();
			}
				break;
			case TypesOpcode.POP2: {

				String type = context.pop();
				if (!type.startsWith("2")) {
					context.pop();
				}
			}
				break;
			case TypesOpcode.PUSH:

				context.push(instruction.getType());
				break;
			case TypesOpcode.AALOAD: {
				context.pop();
				String array_desc = context.pop();
				String element_desc = null;
				if (array_desc.startsWith("[[")) {
					element_desc = array_desc.substring(1);
				} else {
					Type array_type = Type.getType(array_desc);
					element_desc = array_type.getElementType().getInternalName();
				}

				context.push(element_desc);
			}
				break;
			case TypesOpcode.DUP:
				context.push(context.peek());
				break;
			case TypesOpcode.DUPX1: {
				String type1 = context.pop();
				String type2 = context.pop();
				context.push(type1);
				context.push(type2);
				context.push(type1);
			}
				break;
			case TypesOpcode.DUPX2: {
				String type1 = context.pop();
				String type2 = context.pop();
				if (type2.startsWith("2")) {
					context.push(type1);
					context.push(type2);
					context.push(type1);
				} else {
					String type3 = context.pop();
					if (type3 != null) {
						context.push(type1);
						context.push(type3);
						context.push(type2);
						context.push(type1);
					}
				}
			}
				break;
			case TypesOpcode.DUP2: {
				String type1 = context.pop();
				if (type1.startsWith("2")) {
					context.push(type1);
					context.push(type1);
				} else {
					String type2 = context.pop();
					context.push(type2);
					context.push(type1);
					context.push(type2);
					context.push(type1);
				}
			}
				break;
			case TypesOpcode.DUP2_X1: {
				String type1 = context.pop();
				String type2 = context.pop();
				if (type1.startsWith("2")) {
					context.push(type1);
					context.push(type2);
					context.push(type1);
				} else {
					String type3 = context.pop();
					context.push(type2);
					context.push(type1);
					context.push(type3);
					context.push(type2);
					context.push(type1);
				}
			}
				break;
			case TypesOpcode.DUP2_X2: {
				String type1 = context.pop();
				String type2 = context.pop();
				if (type1.startsWith("2")) {
					if (type2.startsWith("2")) // form 4
					{
						context.push(type1);
						context.push(type2);
						context.push(type1);
					} else // form 2
					{
						String type3 = context.pop();
						context.push(type1);
						context.push(type3);
						context.push(type2);
						context.push(type1);
					}
				} else if (!type2.startsWith("2")) {
					String type3 = context.pop();
					if (type3.startsWith("2")) // form 3
					{
						context.push(type2);
						context.push(type1);
						context.push(type3);
						context.push(type2);
						context.push(type1);
					} else // form 1
					{
						String type4 = context.pop();
						context.push(type2);
						context.push(type1);
						context.push(type4);
						context.push(type3);
						context.push(type2);
						context.push(type1);
					}
				}
			}
				break;
			case TypesOpcode.SWAP: {
				String type1 = context.pop();
				String type2 = context.pop();
				context.push(type1);
				context.push(type2);
			}
				break;
			case TypesOpcode.PAUSE:

				mPauseContexts[instruction.getArgument()] = context.clone(node);
				break;
			case TypesOpcode.LABEL: {

				TypesContext label_context = context.clone(node);
				mLabelContexts[instruction.getArgument()] = label_context;
				if (TypesNode.EXCEPTION == node.getSort()) {
					exception_context = label_context;
				}
			}
				break;
			}
		}
	}

	/**
	 * Visits a line number declaration.
	 * 
	 * @param line
	 *                        a line number. This number refers to the source file from which the class was compiled.
	 * @param start
	 *                        the first instruction corresponding to this line number.
	 * @throws IllegalArgumentException
	 *                         if <tt>start</tt> has not already been visited by this visitor (by the {@link #visitLabel visitLabel} method).
	 */
	public void visitLineNumber(int line, Label start) {
	}

	/**
	 * Visits a local variable declaration.
	 * 
	 * @param name
	 *                        the name of a local variable.
	 * @param desc
	 *                        the type descriptor of this local variable.
	 * @param signature
	 *                        the type signature of this local variable. May be <tt>null</tt> if the local variable type does not use generic types.
	 * @param start
	 *                        the first instruction corresponding to the scope of this local variable (inclusive).
	 * @param end
	 *                        the last instruction corresponding to the scope of this local variable (exclusive).
	 * @param index
	 *                        the local variable's index.
	 * @throws IllegalArgumentException
	 *                         if one of the labels has not already been visited by this visitor (by the {@link #visitLabel visitLabel} method).
	 */
	public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
	}

	/**
	 * Visits a non standard attribute of the code. This method must visit only the first attribute in the given attribute list.
	 * 
	 * @param attr
	 *                        a non standard code attribute. Must not be <tt>null</tt>.
	 */
	public void visitAttribute(Attribute attr) {
	}

	public void visitCode() {
	}

	/**
	 * Visits the default value of this annotation interface method.
	 * 
	 * @return a visitor to the visit the actual default value of this annotation interface method. The 'name' parameters passed to the methods of this annotation visitor are ignored. Moreover, exacly one visit method must be called on this annotation visitor, followed by visitEnd.
	 */
	public AnnotationVisitor visitAnnotationDefault() {
		return new EmptyVisitor();
	}

	/**
	 * Visits an annotation of this method.
	 * 
	 * @param desc
	 *                        the class descriptor of the annotation class.
	 * @param visible
	 *                        <tt>true</tt> if the annotation is visible at runtime.
	 * @return a visitor to visit the annotation values.
	 */
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return new EmptyVisitor();
	}

	/**
	 * Visits an annotation of a parameter this method.
	 * 
	 * @param parameter
	 *                        the parameter index.
	 * @param desc
	 *                        the class descriptor of the annotation class.
	 * @param visible
	 *                        <tt>true</tt> if the annotation is visible at runtime.
	 * @return a visitor to visit the annotation values.
	 */
	public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
		return new EmptyVisitor();
	}

	/**
	 * Visits the end of the method. This method, which is the last one to be called, is used to inform the visitor that all the annotations and attributes of the method have been visited.
	 */
	public void visitEnd() {
	}
	
	public void visitFrame(int arg0, int arg1, Object[] arg2, int arg3, Object[] arg4) {
	}
}
