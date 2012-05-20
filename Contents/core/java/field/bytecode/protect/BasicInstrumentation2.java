package field.bytecode.protect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.AnalyzerAdapter;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import field.bytecode.protect.analysis.TypesClassVisitor;
import field.bytecode.protect.analysis.TypesContext;
import field.namespace.generic.ReflectionTools;
import field.namespace.generic.Generics.Pair;
import field.util.ANSIColorUtils;

/**
 * Created on Mar 13, 2004
 * 
 * @author marc
 */
final public class BasicInstrumentation2 {

	static public abstract class CallOnEntry extends GeneratorAdapter implements EntryHandler {

		static int uniq = 0;

		private final String name;

		private final Method onMethod;

		private final String parameterName;

		private final HashMap<String, Object> parameters;

		int returnNumber = 0;

		public CallOnEntry(String name, int access, Method onMethod, MethodVisitor delegateTo, HashMap<String, Object> parameters) {
			super(access, onMethod, delegateTo);
			this.name = name;
			this.onMethod = onMethod;
			this.parameters = parameters;
			parameterName = "parameter:" + uniq_parameter++;
			BasicInstrumentation2.parameters.put(parameterName, parameters);
			returnNumber = 0;

			assert !entryHandlers.containsKey(name);
			entryHandlers.put(name, this);
		}

		abstract public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray);

		@Override
		public void visitCode() {

			push(name);
			loadThis();
			push(onMethod.getName());
			push(parameterName);
			loadArgArray();
			invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handleFast", Type.VOID_TYPE, new Type[] { Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(Object[].class) }));
			super.visitCode();
		}
	}

	static public abstract class CallOnEntryAndExit extends GeneratorAdapter implements EntryHandler, ExitHandler {
		private final String name;

		private final Method onMethod;

		private final String parameterName;

		private final HashMap<String, Object> parameters;

		private int returnNumber;

		boolean isConstructor = false;

		public CallOnEntryAndExit(String name, int access, Method onMethod, MethodVisitor delegateTo, HashMap<String, Object> parameters) {
			super(access, onMethod, delegateTo);
			this.name = name;
			this.onMethod = onMethod;
			this.parameters = parameters;
			parameterName = "parameter:" + uniq_parameter++;
			returnNumber = 0;
			BasicInstrumentation2.parameters.put(parameterName, parameters);

			assert !entryHandlers.containsKey(name);
			entryHandlers.put(name, this);

			assert !exitHandlers.containsKey(name);
			exitHandlers.put(name, this);
		}

		abstract public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName);

		abstract public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray);

		@Override
		public void visitCode() {

			if (onMethod.getName().equals("<init>")) {

				// we have to leave this until after the first
				// invoke special
				isConstructor = true;

			} else {
				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				loadArgArray();
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.VOID_TYPE, new Type[] { Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(Object[].class) }));
			}
			super.visitCode();
		}

		@Override
		public void visitInsn(int op) {
			if (op == Opcodes.RETURN) {
				push((String) null);
				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.VOID_TYPE, new Type[] { Type.getType(Object.class), Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(String.class) }));
				pop();
			} else if (op == Opcodes.IRETURN) {
				box(Type.INT_TYPE);

				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.VOID_TYPE, new Type[] { Type.getType(Object.class), Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(String.class) }));
				unbox(Type.INT_TYPE);
			} else if (op == Opcodes.FRETURN) {
				box(Type.FLOAT_TYPE);

				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.VOID_TYPE, new Type[] { Type.getType(Object.class), Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(String.class) }));
				unbox(Type.FLOAT_TYPE);
			} else if (op == Opcodes.ARETURN) {
				dup();

				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.VOID_TYPE, new Type[] { Type.getType(Object.class), Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(String.class) }));
			}

			super.visitInsn(op);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			if (isConstructor) {
				if (opcode == Opcodes.INVOKESPECIAL) {
					super.visitMethodInsn(opcode, owner, name, desc);

					push(this.name);
					loadThis();
					push(onMethod.getName());
					push(parameterName);
					loadArgArray();

					invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.VOID_TYPE, new Type[] { Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(Object[].class) }));

					isConstructor = false;
				} else
					super.visitMethodInsn(opcode, owner, name, desc);

			} else
				super.visitMethodInsn(opcode, owner, name, desc);

		}
	}

	static public abstract class CallOnEntryAndExit_exceptionAware extends GeneratorAdapter implements EntryHandler, ExitHandler {
		private static final Type Type_Object = Type.getType(Object.class);

		private static final Type Type_String = Type.getType(String.class);

		private static final Type[] Type_handle_sig = new Type[] { Type_Object, Type_String, Type_Object, Type_String, Type_String, Type_String };

		private final int access2;

		private Label endTryCatchLabel;

		private int exceptionLocal;

		private final Method onMethod;

		private final String parameterName;

		private final HashMap<String, Object> parameters;

		private int returnNumber;

		protected String name;

		LinkedHashMap<Integer, Pair<String, String>> aliasedParameterSet = new LinkedHashMap<Integer, Pair<String, String>>();

		boolean isConstructor = false;

		Label startTryCatchLabel;

		public CallOnEntryAndExit_exceptionAware(String name, int access, Method onMethod, MethodVisitor delegateTo, HashMap<String, Object> parameters) {
			super(access, onMethod, delegateTo);
			this.name = name;
			access2 = access;
			this.onMethod = onMethod;
			this.parameters = parameters;
			parameterName = "parameter:" + uniq_parameter++;
			returnNumber = 0;
			BasicInstrumentation2.parameters.put(parameterName, parameters);

			assert !entryHandlers.containsKey(name);
			entryHandlers.put(name, this);

			assert !exitHandlers.containsKey(name);
			exitHandlers.put(name, this);

			parameters.put("method", onMethod);
		}

		abstract public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName);

		abstract public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray);

		@Override
		public void visitCode() {
			if (StandardTrampoline.debug)
				;//System.out.println(ANSIColorUtils.red(" entryAndExit begins"));
			super.visitCode();

			if (onMethod.getName().equals("<init>")) {

				// we have to leave this until after the first
				// invoke special
				isConstructor = true;

			} else {
				if (StandardTrampoline.debug)
					;//System.out.println(ANSIColorUtils.red(" entryAndExit :instrumented entrance"));

				startTryCatchLabel = this.mark();
				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				loadArgArray();
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.VOID_TYPE, new Type[] { Type_String, Type_Object, Type_String, Type_String, Type.getType(Object[].class) }));
			}
			exceptionLocal = this.newLocal(Type.getType(Throwable.class));

		}

		@Override
		public void visitEnd() {

			if ((access2 & Opcodes.ACC_ABSTRACT) == 0) {

				endTryCatchLabel = this.mark();
				if (StandardTrampoline.debug)
					;//System.out.println(" exception local is <" + exceptionLocal + ">");
				storeLocal(exceptionLocal);

				push((String) null);
				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type_Object, Type_handle_sig));

				loadLocal(exceptionLocal);
				throwException();

				super.visitTryCatchBlock(startTryCatchLabel, endTryCatchLabel, endTryCatchLabel, null);

			}
			super.visitMaxs(0, 0);
			super.visitEnd();
			if (StandardTrampoline.debug)
				;//System.out.println(ANSIColorUtils.red(" entryAndExit ends"));
		}

		@Override
		public void visitInsn(int op) {
			if (op == Opcodes.RETURN) {
				push((String) null);
				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type_Object, Type_handle_sig));
				pop();
				if (StandardTrampoline.debug)
					;//System.out.println(ANSIColorUtils.red(" entryAndExit :instrumented RETURN"));
			} else if (op == Opcodes.IRETURN) {
				// dup();
				box(Type.INT_TYPE);

				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type_Object, Type_handle_sig));
				unbox(Type.INT_TYPE);
				if (StandardTrampoline.debug)
					;//System.out.println(ANSIColorUtils.red(" entryAndExit :instrumented IRETURN"));
			} else if (op == Opcodes.FRETURN) {
				// dup();
				box(Type.FLOAT_TYPE);

				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type_Object, Type_handle_sig));
				unbox(Type.FLOAT_TYPE);
				if (StandardTrampoline.debug)
					;//System.out.println(ANSIColorUtils.red(" entryAndExit :instrumented FRETURN"));
			} else if (op == Opcodes.ARETURN) {
				// dup();

				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type_Object, Type_handle_sig));
				checkCast(onMethod.getReturnType());
				if (StandardTrampoline.debug)
					;//System.out.println(ANSIColorUtils.red(" entryAndExit :instrumented ARETURN"));
			}

			super.visitInsn(op);
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			if (isConstructor) {
				if (opcode == Opcodes.INVOKESPECIAL) {
					super.visitMethodInsn(opcode, owner, name, desc);
					if (StandardTrampoline.debug)
						;//System.out.println(ANSIColorUtils.red(" entryAndExit :instrumented entrance of constructor"));

					startTryCatchLabel = this.mark();
					push(this.name);
					loadThis();
					push(onMethod.getName());
					push(parameterName);
					loadArgArray();

					invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.VOID_TYPE, new Type[] { Type_String, Type_Object, Type_String, Type_String, Type.getType(Object[].class) }));

					isConstructor = false;
				} else
					super.visitMethodInsn(opcode, owner, name, desc);

			} else
				super.visitMethodInsn(opcode, owner, name, desc);

		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, boolean visible) {

			if (StandardTrampoline.debug)
				;//System.out.println(" entryAndExit, visit parameter annotation <" + parameter + "> <" + desc + "> <" + visible + ">");

			if (knownAliasingParameters.contains(desc)) {
				aliasedParameterSet.put(parameter, new Pair<String, String>(desc, null));

				// rip out the name and argument
				return new AnnotationVisitor() {

					public void visit(String name, Object value) {
						aliasedParameterSet.put(parameter, new Pair<String, String>(desc, (String) value));
					}

					public AnnotationVisitor visitAnnotation(String name, String desc) {
						return null;
					}

					public AnnotationVisitor visitArray(String name) {
						return null;
					}

					public void visitEnd() {
					}

					public void visitEnum(String name, String desc, String value) {
					}
				};
			} else
				return super.visitParameterAnnotation(parameter, desc, visible);
		}

	}

	static public abstract class CallOnEntryFast extends GeneratorAdapter implements FastEntryHandler {

		static int uniq = 0;

		private final String name;

		private final Method onMethod;

		protected final HashMap<String, Object> parameters;

		int returnNumber = 0;

		public CallOnEntryFast(String name, int access, Method onMethod, MethodVisitor delegateTo, HashMap<String, Object> parameters) {
			super(access, onMethod, delegateTo);
			this.name = name;
			this.onMethod = onMethod;
			this.parameters = parameters;
			returnNumber = 0;

			assert !entryHandlers.containsKey(name);
			FastEntryHandler[] ne = new FastEntryHandler[entryHandlerList.length + 1];
			System.arraycopy(entryHandlerList, 0, ne, 0, entryHandlerList.length);
			ne[ne.length - 1] = this;
			entryHandlerList = ne;
			uniq = entryHandlerList.length - 1;
		}

		abstract public void handle(int fromName, Object fromThis, Object[] argArray);

		@Override
		public void visitCode() {

			push(uniq);
			loadThis();
			loadArgArray();
			invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handleFast", Type.VOID_TYPE, new Type[] { Type.getType(Integer.TYPE), Type.getType(Object.class), Type.getType(Object[].class) }));
			super.visitCode();
		}
	}

	static public abstract class CallOnReturn extends GeneratorAdapter implements ExitHandler {

		private final String name;

		private final Method onMethod;

		private final String parameterName;

		private final HashMap<String, Object> parameters;

		int returnNumber = 0;

		public CallOnReturn(String name, int access, Method onMethod, MethodVisitor delegateTo, HashMap<String, Object> parameters) {
			super(access, onMethod, delegateTo);
			this.name = name;
			this.onMethod = onMethod;
			this.parameters = parameters;
			parameterName = "parameter:" + uniq_parameter++;
			BasicInstrumentation2.parameters.put(parameterName, parameters);
			returnNumber = 0;

			assert !exitHandlers.containsKey(name);
			exitHandlers.put(name, this);
		}

		abstract public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName);

		@Override
		public void visitInsn(int op) {
			if (StandardTrampoline.debug)
				;//System.out.println(" -- visit insn <" + op + "> <" + Opcodes.RETURN + ">");
			if (op == Opcodes.RETURN) {
				push((String) null);
				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.getType(Object.class), new Type[] { Type.getType(Object.class), Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(String.class) }));
				pop();
			} else if (op == Opcodes.IRETURN) {
				// dup();
				box(Type.INT_TYPE);

				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.getType(Object.class), new Type[] { Type.getType(Object.class), Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(String.class) }));
				unbox(Type.INT_TYPE);
			} else if (op == Opcodes.FRETURN) {
				// dup();
				box(Type.FLOAT_TYPE);

				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.getType(Object.class), new Type[] { Type.getType(Object.class), Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(String.class) }));
				unbox(Type.FLOAT_TYPE);
			} else if (op == Opcodes.ARETURN) {
				// dup();

				push(name);
				loadThis();
				push(onMethod.getName());
				push(parameterName);
				push("" + returnNumber++);
				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle", Type.getType(Object.class), new Type[] { Type.getType(Object.class), Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(String.class) }));
			}

			super.visitInsn(op);
		}
	}

	static public abstract class DeferCalling extends GeneratorAdapter implements DeferedHandler {
		private final int access;

		private final ClassVisitor classDelegate;

		private final String name;

		private final Method onMethod;

		private final String parameterName;

		private final HashMap<String, Object> parameters;

		private final int returnNumber;

		private final String signature;

		public DeferCalling(String name, int access, Method onMethod, ClassVisitor classDelegate, MethodVisitor delegateTo, String signature, HashMap<String, Object> parameters) {
			super(access, onMethod, delegateTo);
			this.name = name;
			this.access = access;
			this.onMethod = onMethod;
			this.classDelegate = classDelegate;
			this.signature = signature;
			this.parameters = parameters;
			parameterName = "parameter:" + uniq_parameter++;
			returnNumber = 0;
			BasicInstrumentation2.parameters.put(parameterName, parameters);

			assert onMethod.getReturnType() == Type.VOID_TYPE : onMethod.getReturnType();

			assert !entryHandlers.containsKey(name);
			deferedHandlers.put(name, this);

		}

		abstract public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameters, Object[] argArray, Class[] argTypeArray);

		@Override
		public void visitCode() {
			super.visitCode();
			push(name);
			loadThis();
			push(onMethod.getName() + "_original");
			push(parameterName);
			loadArgArray();

			invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handleCancelFast", Type.VOID_TYPE, new Type[] { Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.getType(String.class), Type.getType(Object[].class) }));

			visitInsn(Opcodes.RETURN);

			super.visitMaxs(0, 0);
			super.visitEnd();
			//
			this.mv = classDelegate.visitMethod(access, onMethod.getName() + "_original", onMethod.getDescriptor(), signature, new String[] {});
			this.mv.visitCode();
		}
	}

	static public abstract class DeferCallingFast extends GeneratorAdapter implements FastCancelHandler {
		static int uniq = 0;

		private final int access;

		private final ClassVisitor classDelegate;

		private final String name;

		private final Method onMethod;

		private final String parameterName;

		private final int returnNumber;

		private final String signature;

		protected HashMap<String, Object> parameters;

		public DeferCallingFast(String name, int access, Method onMethod, ClassVisitor classDelegate, MethodVisitor delegateTo, String signature, HashMap<String, Object> parameters) {
			super(access, onMethod, delegateTo);
			this.name = name;
			this.access = access;
			this.onMethod = onMethod;
			this.classDelegate = classDelegate;
			this.signature = signature;
			this.parameters = parameters;
			parameterName = "parameter:" + uniq_parameter++;
			returnNumber = 0;
			BasicInstrumentation2.parameters.put(parameterName, parameters);

			// assert onMethod.getReturnType() == Type.VOID_TYPE :
			// onMethod.getReturnType();

			assert !entryHandlers.containsKey(name);
			// deferedHandlers.put(name, this);

			assert !entryHandlers.containsKey(name);
			FastCancelHandler[] ne = new FastCancelHandler[entryCancelList.length + 1];
			System.arraycopy(entryCancelList, 0, ne, 0, entryCancelList.length);
			ne[ne.length - 1] = this;
			entryCancelList = ne;
			uniq = entryCancelList.length - 1;

		}

		abstract public Object handle(int fromName, Object fromThis, String originalMethodName, Object[] argArray);

		@Override
		public void visitCode() {
			super.visitCode();
			push(uniq);
			loadThis();
			push(onMethod.getName() + "_original");
			loadArgArray();

			invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handleCancelFast", Type.getType(Object.class), new Type[] { Type.getType(Integer.TYPE), Type.getType(Object.class), Type.getType(String.class), Type.getType(Object[].class) }));

			if (onMethod.getReturnType().getSort() == Type.OBJECT) {
				checkCast(onMethod.getReturnType());
				visitInsn(Opcodes.ARETURN);
			} else if (onMethod.getReturnType() == Type.INT_TYPE) {
				unbox(Type.INT_TYPE);
				super.visitInsn(Opcodes.IRETURN);
			} else if (onMethod.getReturnType() == Type.FLOAT_TYPE) {
				unbox(Type.FLOAT_TYPE);
				super.visitInsn(Opcodes.FRETURN);
			} else if (onMethod.getReturnType() == Type.VOID_TYPE) {
				super.visitInsn(Opcodes.RETURN);
			} else {
				assert false : onMethod.getReturnType();
			}

			super.visitMaxs(0, 0);
			super.visitEnd();
			//
			this.mv = classDelegate.visitMethod(access, onMethod.getName() + "_original", onMethod.getDescriptor(), signature, new String[] {});
			this.mv.visitCode();
		}
	}

	public interface DeferedHandler {
		public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameters, Object[] argArray, Class[] argTypeArray);
	}

	public interface EntryHandler {
		public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameters, Object[] argArray);
	}

	public interface ExitHandler {
		public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameters, String methodReturnName);
	}

	public interface FastCancelHandler {
		public Object handle(int fromName, Object fromThis, String originalMethodName, Object[] argArray);
	}

	public interface FastEntryHandler {
		public void handle(int fromName, Object fromThis, Object[] argArray);
	}

	static public abstract class Yield2 extends GeneratorAdapter implements YieldHandler {
		private final int access;

		private final String className;

		private Label initialJumpLabel;

		private final Method onMethod;

		private final byte[] originalByteCode;

		private final String parameterName;

		private final int returnNumber;

		private Label startLabel;

		protected final String name;

		protected final HashMap<String, Object> parameters;

		List<Label> jumpLabels = new ArrayList<Label>();

		List<Integer> validLocals = new ArrayList<Integer>();

		List<Type> validLocalTypes = new ArrayList<Type>();

		int yieldNumber = 0;

		AnalyzerAdapter analyzer;

		public Yield2(String name, int access, Method onMethod, MethodVisitor delegateTo, HashMap<String, Object> parameters, byte[] originalByteCode, String className) {
			// super(name, access, onMethod.getName(),
			// onMethod.getDescriptor(), delegateTo);
			super(new AnalyzerAdapter(className, access, onMethod.getName(), onMethod.getDescriptor(), delegateTo) {
			}, access, onMethod.getName(), onMethod.getDescriptor());

			this.analyzer = (AnalyzerAdapter) this.mv;
			this.name = name;
			this.access = access;
			this.onMethod = onMethod;
			this.parameters = parameters;
			this.originalByteCode = originalByteCode;
			this.className = className;

			parameterName = "parameter:" + uniq_parameter++;
			returnNumber = 0;
			BasicInstrumentation2.parameters.put(parameterName, parameters);

			yieldHandlers.put(name, this);

		}

		@Override
		public void visitCode() {
			if (StandardTrampoline.debug)
				;//System.out.println(ANSIColorUtils.red(" yield begins "));
			super.visitCode();

			initialJumpLabel = this.newLabel();
			this.goTo(initialJumpLabel);

			startLabel = this.mark();
			jumpLabels.add(startLabel);
			analyzer.stack = new ArrayList();
			analyzer.locals = new ArrayList();
		}

		@Override
		public void visitEnd() {
			// insert jump table
			this.visitLabel(initialJumpLabel);
			analyzer.stack = new ArrayList();
			analyzer.locals = new ArrayList();

			// insert code that works out where to jumpFrom
			push(name);
			loadThis();
			push(onMethod.getName());
			invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle_yieldIndex", Type.INT_TYPE, new Type[] { Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class) }));

			// if (StandardTrampoline.debug)
			;//System.out.println(" we have <" + jumpLabels.size() + "> <" + startLabel + ">");
			this.visitTableSwitchInsn(0, jumpLabels.size() - 1, startLabel, jumpLabels.toArray(new Label[jumpLabels.size()]));

			super.visitMaxs(0, 0);
			super.visitEnd();
			// if (StandardTrampoline.debug)
			// ;//System.out.println(ANSIColorUtils.red(" yield ends"));
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {

		}

		@Override
		public void visitInsn(int opcode) {
			;//System.out.println(" vi ->:" + opcode + " " + analyzer.locals);
			super.visitInsn(opcode);
			;//System.out.println(" vi <-:" + opcode + " " + analyzer.locals);
		}

		@Override
		public void visitIntInsn(int opcode, int operand) {
			;//System.out.println(" vii ->:" + opcode + " " + analyzer.locals);
			super.visitIntInsn(opcode, operand);
			;//System.out.println(" vii <-:" + opcode + " " + analyzer.locals);
		}

		@Override
		public void visitJumpInsn(int opcode, Label label) {
			;//System.out.println(" vji ->:" + opcode + " " + analyzer.locals);
			super.visitJumpInsn(opcode, label);
			;//System.out.println(" vji <-:" + opcode + " " + analyzer.locals);
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			// call it
			;//System.out.println(" vmi ->:" + opcode + " " + analyzer.locals);
			super.visitMethodInsn(opcode, owner, name, desc);
			;//System.out.println(" vmi <-:" + opcode + " " + analyzer.locals);

			if (owner.equals("field/bytecode/protect/yield/YieldUtilities") && name.equals("yield")) {

				;//System.out.println(" inside yield, stack is <" + analyzer.stack + " / " + analyzer.locals + ">");

				push(analyzer.locals.size());
				newArray(Type.getType(Object.class));

				ArrayList wasLocals = new ArrayList(analyzer.locals);
				ArrayList wasStack = new ArrayList(analyzer.stack);

				int n = 0;
				for (Object o : wasLocals) {
					n++;
					;//System.out.println(" o = " + o + " " + n);
					if (o == Opcodes.TOP || n==1)
						continue;

					dup();
					push(n - 1);

					Type t = null;
					if (o == Opcodes.INTEGER)
						this.loadLocal(n-1, t = Type.INT_TYPE);
					else if (o == Opcodes.FLOAT)
						this.loadLocal(n-1 , t = Type.FLOAT_TYPE);
					else if (o instanceof String)
						this.loadLocal(n-1, t = Type.getObjectType(((String) o).contains("/") ? ((String) o) : ((String) o).substring(1)));
					else if (o == Opcodes.DOUBLE)
						this.loadLocal(n-1, t = Type.DOUBLE_TYPE);
					else if (o == Opcodes.LONG)
						this.loadLocal(n-1 , t = Type.LONG_TYPE);
					else
						throw new IllegalStateException("unhandled <" + o + ">");

					box(t);

					//if (StandardTrampoline.debug)
						;//System.out.println(" type = <" + o + ">");

					// this.arrayStore(t);
					mv.visitInsn(Opcodes.AASTORE);
				}

				push(this.name);
				loadThis();
				push(onMethod.getName());
				push(jumpLabels.size());

				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle_yieldStore", Type.getType(Object.class), new Type[] { Type.getType(Object.class), Type.getType(Object[].class), Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.INT_TYPE }));

				// here it comes
				if (onMethod.getReturnType().getSort() == Type.OBJECT) {
					visitInsn(Opcodes.ARETURN);
				} else if (onMethod.getReturnType() == Type.INT_TYPE) {
					unbox(Type.INT_TYPE);
					super.visitInsn(Opcodes.IRETURN);
				} else if (onMethod.getReturnType() == Type.FLOAT_TYPE) {
					unbox(Type.FLOAT_TYPE);
					super.visitInsn(Opcodes.FRETURN);
				} else if (onMethod.getReturnType() == Type.VOID_TYPE) {
					super.visitInsn(Opcodes.RETURN);
				} else {
					assert false : onMethod.getReturnType();
				}

				analyzer.locals = new ArrayList(wasLocals);
				analyzer.stack = new ArrayList(wasStack);

				Label newLabel = mark();
				jumpLabels.add(newLabel);

				// now, we start in reverse

				push(this.name);
				loadThis();
				push(onMethod.getName());

				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle_yieldLoad", Type.getType(Object[].class), new Type[] { Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class) }));

				if (StandardTrampoline.debug)
					;//System.out.println(" --- load --- <" + newLabel + ">");

				n = 0;
				int off = 1;
				for (Object o : wasLocals) {
					n++;
					if (o == Opcodes.TOP || n==1)
						continue;

					dup();
					push(n - off);

					mv.visitInsn(Opcodes.AALOAD);

					Type t = null;
					if (o == Opcodes.INTEGER)
						unbox(t = Type.INT_TYPE);
					else if (o == Opcodes.FLOAT)
						unbox(t = Type.FLOAT_TYPE);
					else if (o instanceof String) {
						t = Type.getObjectType(((String) o).contains("/") ? ((String) o) : ((String) o).substring(1));
						checkCast(t);
					} else if (o == Opcodes.DOUBLE) {
						unbox(t = Type.DOUBLE_TYPE);
					} else if (o == Opcodes.LONG) {
						unbox(t = Type.LONG_TYPE);
					}

					else
						throw new IllegalStateException("unhandled <" + o + ">");

					// if (StandardTrampoline.debug)
					;//System.out.println(" loading back type = <" + o + ">");
					storeLocal(n-1, t);
				}

				analyzer.locals = new ArrayList(wasLocals);
				analyzer.stack = new ArrayList(wasStack);

			}

			yieldNumber++;
		}

		abstract public int yieldIndexFor(String fromName, Object fromThis, String methodName);

		abstract public Object[] yieldLoad(String fromName, Object fromThis, String methodName);

		abstract public Object yieldStore(Object wasReturn, Object[] localStorage, String fromName, Object fromThis, String methodName, int resumeLabel);
	}

	static public abstract class Yield extends GeneratorAdapter implements YieldHandler {
		private final int access;

		private final String className;

		private Label initialJumpLabel;

		private final Method onMethod;

		private final byte[] originalByteCode;

		private final String parameterName;

		private final int returnNumber;

		private Label startLabel;

		private final TypesClassVisitor typeAnalysis;

		protected final String name;

		protected final HashMap<String, Object> parameters;

		List<Label> jumpLabels = new ArrayList<Label>();

		List<Integer> validLocals = new ArrayList<Integer>();

		List<Type> validLocalTypes = new ArrayList<Type>();

		int yieldNumber = 0;

		public Yield(String name, int access, Method onMethod, MethodVisitor delegateTo, HashMap<String, Object> parameters, byte[] originalByteCode, String className) {
			super(access, onMethod, delegateTo);
			this.name = name;
			this.access = access;
			this.onMethod = onMethod;
			this.parameters = parameters;
			this.originalByteCode = originalByteCode;
			this.className = className;

			parameterName = "parameter:" + uniq_parameter++;
			returnNumber = 0;
			BasicInstrumentation2.parameters.put(parameterName, parameters);

			yieldHandlers.put(name, this);

			ClassReader reader = new ClassReader(originalByteCode);
			typeAnalysis = new TypesClassVisitor(className, onMethod.getName() + onMethod.getDescriptor()) {
				@Override
				protected boolean isYieldCall(String owner_classname, String name, String desc) {
					if (owner_classname.equalsIgnoreCase("field.bytecode.protect.yield.YieldUtilities") && name.equals("yield")) {
						return true;
					}
					return false;
				}
			};
			reader.accept(typeAnalysis, reader.EXPAND_FRAMES);

			// now we should have something

		}

		@Override
		public void visitCode() {
			if (StandardTrampoline.debug)
				;//System.out.println(ANSIColorUtils.red(" yield begins "));
			super.visitCode();

			initialJumpLabel = this.newLabel();
			this.goTo(initialJumpLabel);

			startLabel = this.mark();
			jumpLabels.add(startLabel);

		}

		@Override
		public void visitEnd() {
			// insert jump table
			this.visitLabel(initialJumpLabel);

			// insert code that works out where to jumpFrom
			push(name);
			loadThis();
			push(onMethod.getName());
			invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle_yieldIndex", Type.INT_TYPE, new Type[] { Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class) }));

			if (StandardTrampoline.debug)
				;//System.out.println(" we have <" + jumpLabels.size() + "> <" + startLabel + ">");
			this.visitTableSwitchInsn(0, jumpLabels.size() - 1, startLabel, jumpLabels.toArray(new Label[jumpLabels.size()]));

			super.visitMaxs(0, 0);
			super.visitEnd();
			if (StandardTrampoline.debug)
				;//System.out.println(ANSIColorUtils.red(" yield ends"));
		}

		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			if (StandardTrampoline.debug)
				;//System.out.println(" local variable <" + name + "> <" + desc + "> <" + signature + "> <" + start + "> <" + end + "> index = <" + index + ">");
			super.visitLocalVariable(name, desc, signature, start, end, index);
		}

		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
		}

		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc) {
			// call it
			super.visitMethodInsn(opcode, owner, name, desc);

			if (owner.equals("field/bytecode/protect/yield/YieldUtilities") && name.equals("yield")) {

				// duplicate _valid_ locals

				Iterator<Integer> i = validLocals.iterator();
				Iterator<Type> i2 = validLocalTypes.iterator();

				// push(validLocals.size());
				TypesContext context = typeAnalysis.nextPauseContext();
				if (StandardTrampoline.debug)
					;//System.out.println(" vars are <" + context.getVars() + ">");

				Map mm = new TreeMap<Integer, String>();
				Map m = (Map) ReflectionTools.illegalGetObject(this, "locals");

				int max = 0;
				for (Map.Entry<Integer, String> localsToSave : ((Map<Integer, String>) context.getVars()).entrySet()) {
					// assert
					// m.containsKey(localsToSave.getKey())
					// : localsToSave.getKey() + " " + m;
					if (m.containsKey(localsToSave.getKey())) {
						int mq = (Integer) m.get(localsToSave.getKey());
						if (mq > max)
							max = mq;
						mm.put(mq, localsToSave.getValue());
					}
				}

				push(max);
				newArray(Type.getType(Object.class));

				for (Map.Entry<Integer, String> localsToSave : ((Map<Integer, String>) mm).entrySet()) {
					dup();
					push(localsToSave.getKey().intValue() - 1);
					String typeName = localsToSave.getValue();

					Type t = Type.getType(typeName.contains("/") ? "L" + typeName + ";" : typeName.substring(1));

					if (StandardTrampoline.debug)
						;//System.out.println(" loading <" + localsToSave.getKey() + ">");
					this.loadLocal(localsToSave.getKey().intValue(), t);
					if (!typeName.contains("/"))
						box(t);

					if (StandardTrampoline.debug)
						;//System.out.println(" type = <" + typeName + ">");

					// this.arrayStore(t);
					mv.visitInsn(Opcodes.AASTORE);
				}

				push(this.name);
				loadThis();
				push(onMethod.getName());
				push(jumpLabels.size());

				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle_yieldStore", Type.getType(Object.class), new Type[] { Type.getType(Object.class), Type.getType(Object[].class), Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class), Type.INT_TYPE }));

				// here it comes
				if (onMethod.getReturnType().getSort() == Type.OBJECT) {
					visitInsn(Opcodes.ARETURN);
				} else if (onMethod.getReturnType() == Type.INT_TYPE) {
					unbox(Type.INT_TYPE);
					super.visitInsn(Opcodes.IRETURN);
				} else if (onMethod.getReturnType() == Type.FLOAT_TYPE) {
					unbox(Type.FLOAT_TYPE);
					super.visitInsn(Opcodes.FRETURN);
				} else if (onMethod.getReturnType() == Type.VOID_TYPE) {
					super.visitInsn(Opcodes.RETURN);
				} else {
					assert false : onMethod.getReturnType();
				}

				Label newLabel = mark();
				jumpLabels.add(newLabel);

				// now, we start in reverse

				push(this.name);
				loadThis();
				push(onMethod.getName());

				invokeStatic(Type.getType(BasicInstrumentation2.class), new Method("handle_yieldLoad", Type.getType(Object[].class), new Type[] { Type.getType(String.class), Type.getType(Object.class), Type.getType(String.class) }));

				if (StandardTrampoline.debug)
					;//System.out.println(" --- load --- <" + newLabel + ">");

				i = validLocals.iterator();
				i2 = validLocalTypes.iterator();

				for (Map.Entry<Integer, String> localsToSave : ((Map<Integer, String>) mm).entrySet()) {
					dup();
					push(localsToSave.getKey().intValue() - 1);
					String typeName = localsToSave.getValue();

					Type t = Type.getType(typeName.contains("/") ? "L" + typeName + ";" : typeName.substring(1));
					mv.visitInsn(Opcodes.AALOAD);
					// this.arrayLoad(t);

					if (typeName.contains("/")) {
						this.checkCast(t);
					}
					if (!typeName.contains("/"))
						unbox(t);
					this.storeLocal(localsToSave.getKey().intValue(), t);
				}

				if (StandardTrampoline.debug)
					;//System.out.println(ANSIColorUtils.red(" yield :instrumented yield"));
			}

			yieldNumber++;
		}

		@Override
		public void visitVarInsn(int opcode, int var) {
			super.visitVarInsn(opcode, var);

			Map m = (Map) ReflectionTools.illegalGetObject(this, "locals");

			if (StandardTrampoline.debug)
				;//System.out.println(" var instruction <" + opcode + "> <" + var + "> <" + m.get(var) + ">");

			if (var != 0) {
				if (!validLocals.contains(var)) {
					if (opcode == Opcodes.ASTORE || opcode == Opcodes.ALOAD) {
						validLocals.add(var);
						validLocalTypes.add(null);
					} else if (opcode == Opcodes.ISTORE || opcode == Opcodes.ILOAD) {
						validLocals.add(var);
						validLocalTypes.add(Type.INT_TYPE);
					} else if (opcode == Opcodes.FSTORE || opcode == Opcodes.FLOAD) {
						validLocals.add(var);
						validLocalTypes.add(Type.FLOAT_TYPE);
					} else {
						if (StandardTrampoline.debug)
							;//System.out.println(" opcode is <" + opcode + "> for <" + var + ">");
					}
				}
				// else
				// {
				// }
				// todo --- arrays?
			}
		}

		abstract public int yieldIndexFor(String fromName, Object fromThis, String methodName);

		abstract public Object[] yieldLoad(String fromName, Object fromThis, String methodName);

		abstract public Object yieldStore(Object wasReturn, Object[] localStorage, String fromName, Object fromThis, String methodName, int resumeLabel);
	}

	public interface YieldHandler {

		public int yieldIndexFor(String fromName, Object fromThis, String methodName);

		public Object[] yieldLoad(String fromName, Object fromThis, String methodName);

		public Object yieldStore(Object wasReturn, Object[] localStorage, String fromName, Object fromThis, String methodName, int resumeLabel);
	}

	static public HashSet<String> knownAliasingParameters = new HashSet<String>();

	static Map<String, DeferedHandler> deferedHandlers = new HashMap<String, DeferedHandler>();

	static FastCancelHandler[] entryCancelList = new FastCancelHandler[0];

	static FastEntryHandler[] entryHandlerList = new FastEntryHandler[0];

	static Map<String, EntryHandler> entryHandlers = new HashMap<String, EntryHandler>();

	static Map<String, ExitHandler> exitHandlers = new HashMap<String, ExitHandler>();

	static Map<String, Map<String, Object>> parameters = new HashMap<String, Map<String, Object>>();

	static int uniq_parameter = 0;

	static Map<String, YieldHandler> yieldHandlers = new HashMap<String, YieldHandler>();

	static public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, String parameterName, String methodReturnName) {
		return exitHandlers.get(fromName).handle(returningThis, fromName, fromThis, methodName, parameters.get(parameterName), methodReturnName);
	}

	static public void handle(String fromName, Object fromThis, String methodName, String parameterName, Object[] argArray) {
		assert entryHandlers.containsKey(fromName) : fromName + " " + entryHandlers;
		entryHandlers.get(fromName).handle(fromName, fromThis, methodName, parameters.get(parameterName), argArray);
	}

	static public void handle(String fromName, Object fromThis, String methodName, String parameterName, Object[] argArray, Class[] paramArray) {
		deferedHandlers.get(fromName).handle(fromName, fromThis, methodName, parameters.get(parameterName), argArray, paramArray);
		if (StandardTrampoline.debug)
			;//System.out.println(" parameter array <" + Arrays.asList(paramArray));
		java.lang.reflect.Method[] m = ReflectionTools.findAllMethodsCalled(fromThis.getClass(), methodName);
		java.lang.reflect.Method mFound = ReflectionTools.findMethodWithParameters(paramArray, m);
		if (StandardTrampoline.debug)
			;//System.out.println(" found method <" + mFound + ">");
	}

	static public int handle_yieldIndex(String fromName, Object fromThis, String methodName) {
		return yieldHandlers.get(fromName).yieldIndexFor(fromName, fromThis, methodName);
	}

	static public Object[] handle_yieldLoad(String fromName, Object fromThis, String methodName) {
		return yieldHandlers.get(fromName).yieldLoad(fromName, fromThis, methodName);
	}

	static public Object handle_yieldStore(Object wasReturn, Object[] localStorage, String fromName, Object fromThis, String methodName, int resumeLabel) {
		if (StandardTrampoline.debug)
			;//System.out.println(" fromname is <" + wasReturn + "> <" + localStorage + "> <" + fromName + "> <" + fromThis + "> <" + methodName + ">");
		return yieldHandlers.get(fromName).yieldStore(wasReturn, localStorage, fromName, fromThis, methodName, resumeLabel);
	}

	static public Object handleCancelFast(int name, Object from, String method, Object[] args) {
		return entryCancelList[name].handle(name, from, method, args);
	}

	static public void handleFast(int fromName, Object fromThis, Object[] argArray) {
		// assert entryHandlers.containsKey(fromName) : fromName + " " +
		// entryHandlers;
		entryHandlerList[fromName].handle(fromName, fromThis, argArray);
	}
}