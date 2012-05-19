package field.bytecode.protect;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.Stack;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.EmptyVisitor;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;
import org.objectweb.asm.util.TraceMethodVisitor;

import sun.misc.Resource;
import sun.misc.URLClassPath;
import field.bytecode.protect.BasicInstrumentation2.CallOnEntryAndExit_exceptionAware;
import field.bytecode.protect.cache.DeferedCached;
import field.bytecode.protect.cache.DeferedDiskCached;
import field.bytecode.protect.cache.DeferedFixedDuringUpdate;
import field.bytecode.protect.cache.DeferredTrace;
import field.bytecode.protect.dispatch.DispatchSupport;
import field.bytecode.protect.dispatch.InsideSupport;
import field.bytecode.protect.yield.YieldSupport;
import field.namespace.context.CT.ContextTopology;
import field.namespace.context.CT.iStorage;
import field.namespace.generic.Generics.Pair;
import field.namespace.generic.ReflectionTools;

/**
 */
public class StandardTrampoline extends Trampoline2 {

	public class AnnotationMethodAdaptor extends MethodAdapter {

		private final int access;

		private final String class_name;

		private final ClassVisitor cv;

		private final String desc;

		private final String name;

		private final byte[] originalByteCode;

		private final String signature;

		private final String super_name;

		public AnnotationMethodAdaptor(int access, String name, String desc, String signature, ClassVisitor classDelegate, MethodVisitor arg0, String super_name, byte[] originalByteCode, String class_name) {
			super(arg0);
			this.access = access;
			this.name = name;
			this.desc = desc;
			this.signature = signature;
			this.cv = classDelegate;
			this.super_name = super_name;
			this.originalByteCode = originalByteCode;
			this.class_name = class_name;
		}

		@Override
		public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
			super.visitFrame(type, nLocal, local, nStack, stack);
		}

		public void setDelegate(MethodVisitor mv) {
			this.mv = mv;
		}

		@Override
		public AnnotationVisitor visitAnnotation(final String annotationName, boolean vis) {

			if (anotatedMethodHandlers.containsKey(annotationName)) {
				final AnnotationVisitor av = super.visitAnnotation(annotationName, vis);
				final HashMap<String, Object> parameters = new HashMap<String, Object>();

				return new AnnotationVisitor() {

					public void visit(String arg0, Object arg1) {
						parameters.put(arg0, arg1);
						av.visit(arg0, arg1);
					}

					public AnnotationVisitor visitAnnotation(String arg0, String arg1) {
						return av.visitAnnotation(arg0, arg1);
					}

					public AnnotationVisitor visitArray(String arg0) {
						return av.visitArray(arg0);
					}

					public void visitEnd() {
						av.visitEnd();
						MethodVisitor m = anotatedMethodHandlers.get(annotationName).handleEnd(access, name, desc, signature, cv, mv, parameters, originalByteCode, class_name);
						if (m != null)
							mv = m;
					}

					public void visitEnum(String arg0, String arg1, String arg2) {
						av.visitEnum(arg0, arg1, arg2);
					}

				};
			} else {
				return super.visitAnnotation(annotationName, vis);
			}
		}

		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			return super.visitParameterAnnotation(parameter, desc, visible);
		}

		@Override
		public void visitLineNumber(int arg0, Label arg1) {
			// System.out.println(" line number <"+arg0+"> <"+arg1+">");
			super.visitLineNumber(arg0, arg1);
		}

	}

	public interface HandlesAnnontatedMethod {
		public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className);
	}

	public class InheritWovenMethodAdaptor extends MethodAdapter {

		private final int access;

		private final String desc;

		private final String[] interfaces;

		private final String name;

		private final String super_name;

		public InheritWovenMethodAdaptor(int access, String name, String desc, MethodVisitor arg0, String super_name, String[] interfaces) {
			super(arg0);
			this.access = access;
			this.name = name;
			this.desc = desc;
			this.super_name = super_name;
			this.interfaces = interfaces;
		}

		public void setDelegate(MethodVisitor mv) {
			this.mv = mv;
		}

		@Override
		public AnnotationVisitor visitAnnotation(final String annotationName, boolean vis) {
			if (annotationName.equals("Lfield/bytecode/protect/annotations/InheritWeave;")) {
				if (debug)
					System.out.println(" found inherit weave in <" + name + ">");

				try {

					Type[] at = new Method(name, desc).getArgumentTypes();
					Annotation[] annotations = getAllAnotationsForSuperMethodsOf(name, desc, at, super_name, interfaces);

					for (int i = 0; i < annotations.length; i++) {
						AnnotationVisitor va = mv.visitAnnotation("L" + annotations[i].annotationType().getName().replace('.', '/') + ";", true);

						java.lang.reflect.Method[] annotationMethods = annotations[i].annotationType().getDeclaredMethods();

						if (annotations[i].annotationType().getClassLoader() != loader)
							assert !shouldLoadLocal(annotations[i].annotationType().getName()) : "WARNING: leaked, " + annotations[i].annotationType().getClassLoader() + " " + annotations[i].annotationType();

						for (java.lang.reflect.Method mm : annotationMethods) {
							try {
								Object r;
								r = mm.invoke(annotations[i], new Object[] {});
								if (r instanceof Class)
									r = Type.getType((Class) r);
								va.visit(mm.getName(), r);
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (IllegalAccessException e) {
								e.printStackTrace();
							} catch (InvocationTargetException e) {
								e.printStackTrace();
							}
						}

						va.visitEnd();
					}

				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				} catch (SecurityException e) {
					e.printStackTrace();
				}
				return new EmptyVisitor();
			}
			return super.visitAnnotation(annotationName, vis);
		}

	}

	public static boolean debug = false;

	static public Comparator fieldComparator = new Comparator() {

		public int compare(Object arg0, Object arg1) {
			return ((Field) arg0).getName().compareTo(((Field) arg1).getName());
		}
	};

	static HashMap fieldsCache = new HashMap();

	static HashMap methodsCache = new HashMap();

	public static void checkClass(byte[] aa) throws Exception {
		if (true)
			return;
		// ClassReader cr = new ClassReader(aa);
		//
		// ClassNode cn = new ClassNode();
		// cr.accept(new CheckClassAdapter(cn), true);
		//
		// List methods = cn.methods;
		// for (int i = 0; i < methods.size(); ++i) {
		// MethodNode method = (MethodNode) methods.get(i);
		// if (method.instructions.size() > 0) {
		// Analyzer a = new Analyzer(new SimpleVerifier(Type.getType("L"
		// + cn.name + ";"), Type.getType("L" + cn.superName + ";"),
		// (cn.access & Opcodes.ACC_INTERFACE) != 0));
		// try {
		// a.analyze(cn.name, method);
		// // continue;
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
		// final Frame[] frames = a.getFrames();
		//
		// if (StandardTrampoline.debug)
		// System.out.println(method.name + method.desc);
		// TraceMethodVisitor mv = new TraceMethodVisitor() {
		// @Override
		// public void visitLocalVariable(String name, String desc,
		// String signature, Label start, Label end, int index) {
		// super.visitLocalVariable(name, desc, signature, start, end,
		// index);
		// }
		//
		// @Override
		// public void visitMaxs(final int maxStack, final int
		// maxLocals) {
		// for (int i = 0; i < text.size(); ++i) {
		// String s = frames[i] == null ? "null" : frames[i].toString();
		// while (s.length() < maxStack + maxLocals + 1) {
		// s += " ";
		// }
		// System.out.print(Integer.toString(i + 100000).substring(1));
		// System.out.print(" " + s + " : " + text.get(i));
		// }
		// if (StandardTrampoline.debug)
		// System.out.println();
		// }
		// };
		// for (int j = 0; j < method.instructions.size(); ++j) {
		// ((AbstractInsnNode) method.instructions.get(j)).accept(mv);
		// }
		// mv.visitMaxs(method.maxStack, method.maxLocals);
		// }
		// }
	}

	static public Field[] getAllFields(Class of) {
		Field[] ret = (Field[]) fieldsCache.get(of);
		if (ret == null) {
			List fieldsList = new ArrayList();
			_getAllFields(of, fieldsList);
			fieldsCache.put(of, ret = (Field[]) fieldsList.toArray(new Field[0]));
		}
		return ret;
	}

	static public java.lang.reflect.Method[] getAllMethods(Class of) {
		java.lang.reflect.Method[] ret = (java.lang.reflect.Method[]) methodsCache.get(of);
		if (ret == null) {
			ArrayList methodsList = new ArrayList();
			_getAllMethods(of, methodsList);
			methodsCache.put(of, ret = (java.lang.reflect.Method[]) methodsList.toArray(new java.lang.reflect.Method[0]));
		}
		return ret;
	}

	static public Field getFirstFIeldCalled(Class of, String name) {
		Field[] allFields = getAllFields(of);
		for (Field f : allFields) {
			if (f.getName().equals(name)) {
				f.setAccessible(true);
				return f;
			}
		}
		return null;
	}

	static protected void _getAllFields(Class of, List into) {
		if (of == null)
			return;
		Field[] m = of.getDeclaredFields();
		List list = Arrays.asList(m);
		Collections.sort(list, fieldComparator);
		into.addAll(list);
		_getAllFields(of.getSuperclass(), into);
		Class[] interfaces = of.getInterfaces();
		for (int i = 0; i < interfaces.length; i++)
			_getAllFields(interfaces[i], into);
	}

	static protected void _getAllMethods(Class of, List into) {
		if (of == null)
			return;
		java.lang.reflect.Method[] m = of.getDeclaredMethods();
		List list = Arrays.asList(m);
		into.addAll(list);
		_getAllMethods(of.getSuperclass(), into);
		Class[] interfaces = of.getInterfaces();
		for (int i = 0; i < interfaces.length; i++)
			_getAllMethods(interfaces[i], into);
	}

	HashSet<String> alreadyLoaded = new HashSet<String>();

	Map<String, HandlesAnnontatedMethod> anotatedMethodHandlers = new HashMap<String, HandlesAnnontatedMethod>();

	int inside = 0;

	int nn = 0;

	int uniq = 0;

	public StandardTrampoline() {

		BasicInstrumentation2.knownAliasingParameters.add("Lfield/bytecode/protect/annotations/AliasingParameter;");
		BasicInstrumentation2.knownAliasingParameters.add("Lfield/bytecode/protect/annotations/Value;");
		anotatedMethodHandlers.put("Lfield/bytecode/protect/Woven;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {
				return delegate;
			}
		});
		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/TestMethodAnnotation;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {
				return new BasicInstrumentation2.DeferCalling("testmethodannotation", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters) {

					@Override
					public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameters, Object[] argArray, Class[] argTypeArray) {

					}

				};
			}
		});
		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/TestMethodAnnotation2;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {
				return new BasicInstrumentation2.CallOnEntryAndExit_exceptionAware("testmethodannotation2", access, new Method(methodName, methodDesc), delegate, paramters) {
					@Override
					public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName) {
						if (StandardTrampoline.debug)
							System.out.println("out: handle return <" + fromThis + "> <" + methodName + "> <" + parameterName + "> <" + returningThis + ">");
						return returningThis;
					}

					@Override
					public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray) {
						if (StandardTrampoline.debug)
							System.out.println("in: handle entry <" + fromThis + "> <" + methodName + "> <" + parameterName + "> <" + Arrays.asList(argArray));
					}
				};

			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/DispatchOverTopology;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, final String className) {
				return new BasicInstrumentation2.CallOnEntryAndExit_exceptionAware("dispatchOverTopology+" + methodName + "+" + methodDesc + "+" + signature + "+" + (uniq++), access, new Method(methodName, methodDesc), delegate, paramters) {

					DispatchSupport support = new DispatchSupport();

					@Override
					public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName) {
						try {
							return support.exit(this.name, fromThis, returningThis, parameterName, className);
						} catch (Throwable t) {
							t.printStackTrace();
							throw new IllegalArgumentException(t);
						}
					}

					@Override
					public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray) {
						try {
							support.enter(this.name, fromName, fromThis, methodName, parameterName, argArray, className);
						} catch (Throwable t) {
							t.printStackTrace();
							throw new IllegalArgumentException(t);
						}
					}
				};
			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/Inside;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, final String className) {

				System.out.println(" cont, parameters are <" + methodName + ">");
				return new BasicInstrumentation2.CallOnEntryAndExit_exceptionAware("inside+" + methodName + "+" + methodDesc + "+" + signature + "+" + (uniq++), access, new Method(methodName, methodDesc), delegate, paramters) {

					@Override
					public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName) {
						try {
							InsideSupport.exit(fromThis, (String) parameterName.get("group"));
							return returningThis;
						} catch (Throwable t) {
							t.printStackTrace();
							throw new IllegalArgumentException(t);
						}
					}

					@Override
					public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray) {
						try {
							InsideSupport.enter(fromThis, (String) parameterName.get("group"));
						} catch (Throwable t) {
							t.printStackTrace();
							throw new IllegalArgumentException(t);
						}
					}
				};
			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/FastDispatch;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, final String className) {
				return new FastEntry("testmethodannotation2", access, new Method(methodName, methodDesc), delegate, paramters, className);
			}
		});

		

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/Yield;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {
				return new BasicInstrumentation2.Yield2("yield+" + methodName + "+" + methodDesc + (uniq++), access, new Method(methodName, methodDesc), delegate, paramters, originalByteCode, className) {

					YieldSupport support = new YieldSupport();

					@Override
					public int yieldIndexFor(String fromName, Object fromThis, String methodName) {
						if (StandardTrampoline.debug)
							System.out.println(" yield index for <" + fromName + "> <" + fromThis + "> <" + methodName + ">");
						return support.yieldIndexFor(this.name, fromThis, parameters);
					}

					@Override
					public Object[] yieldLoad(String fromName, Object fromThis, String methodName) {
						if (StandardTrampoline.debug)
							System.out.println(" yield load for <" + fromName + "> <" + fromThis + "> <" + methodName + ">");
						return support.yieldLoad(fromThis);
					}

					@Override
					public Object yieldStore(Object wasReturn, Object[] localStorage, String fromName, Object fromThis, String methodName, int resumeLabel) {
						if (StandardTrampoline.debug)
							System.out.println(" yield store for <" + wasReturn + "> <" + Arrays.asList(localStorage) + "> <" + fromName + "> <" + fromThis + "> <" + methodName + ">  resume at <" + resumeLabel + ">");
						return support.yieldStore(wasReturn, localStorage, this.name, fromThis, resumeLabel);
					}
				};
			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/Cancel;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new BasicInstrumentation2.DeferCalling("cancel", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters) {

					@Override
					public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameters, Object[] argArray, Class[] argTypeArray) {
					}
				};

			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/Cached;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new DeferedCached("cancel", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters);

			}
		});anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/Traced;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new DeferredTrace("cancel", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters);

			}
		});
		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/CachedPerUpdate;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {
				return new DeferedFixedDuringUpdate("cancelfix", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters);
			}
		});
		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/ModCountCached;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new DeferedModCountCached("cancel", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters);

			}
		});
		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/DiskCached;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new DeferedDiskCached("cancel", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters);

			}
		});
		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/NewThread;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new DeferedNewThread("nt", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters);

			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/InQueue;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new DeferedInQueue("nt", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters);

			}
		});
		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/InQueueThrough;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new DeferedInQueue("nt", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters, true);

			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/SimplyWrapped;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new SimplyWrappedInQueue("nt", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters);

			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/NextUpdate;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new DeferedNextUpdate("nu", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters);

			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/NonSwing;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {

				return new DeferedNonSwing("nu", access, new Method(methodName, methodDesc), classDelegate, delegate, signature, paramters);

			}
		});
		

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/ConstantContext;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {
				return new BasicInstrumentation2.CallOnEntryAndExit_exceptionAware("dispatchOverTopology+" + methodName + "+" + methodDesc + "+" + signature + "+" + (uniq++), access, new Method(methodName, methodDesc), delegate, paramters) {

					DispatchSupport support = new DispatchSupport();

					@Override
					public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName) {
						Cc.handle_exit(fromThis, name, parameterName);
						return returningThis;
					}

					@Override
					public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray) {
						Cc.handle_entry(fromThis, name, parameterName, aliasedParameterSet, argArray);
					}
				};

			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/Context_begin;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {
				return new BasicInstrumentation2.CallOnEntryAndExit_exceptionAware("dispatchOverTopology+" + methodName + "+" + methodDesc + "+" + signature + "+" + (uniq++), access, new Method(methodName, methodDesc), delegate, paramters) {
					Stack<Pair<ContextTopology, Object>> stack = new Stack<Pair<ContextTopology, Object>>();

					@Override
					public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName) {
						if (stack.size() > 0) {
							Pair<ContextTopology, Object> q = stack.pop();
							ContextAnnotationTools.end(q.left, q.right);
						}
						return returningThis;
					}

					@Override
					public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray) {
						System.out.println(" handle inside ");
						ContextTopology<?, ?> context;
						try {
							context = ContextAnnotationTools.contextFor(fromThis, parameterName, this.aliasedParameterSet, argArray);
						} catch (Exception e) {
							Error er = new Error(" exception thrown in finding context for Context_begin");
							er.initCause(e);
							throw er;
						}
						Object value = ContextAnnotationTools.valueFor(fromThis, parameterName, this.aliasedParameterSet, argArray);
						if (value == null || (value instanceof String && value.equals("")))
							value = fromThis;

						ContextAnnotationTools.begin(context, value);
						stack.push(new Pair<ContextTopology, Object>(context, value));

						ContextAnnotationTools.populateContexted(context, fromThis);
					}

				};

			}
		});
		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/Context_set;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {
				return new BasicInstrumentation2.CallOnEntryAndExit_exceptionAware("dispatchOverTopology+" + methodName + "+" + methodDesc + "+" + signature + "+" + (uniq++), access, new Method(methodName, methodDesc), delegate, paramters) {

					@Override
					public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName) {
						return returningThis;
					}

					@Override
					public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray) {
						System.out.println(" handle inside ");
						ContextTopology context;
						try {
							context = ContextAnnotationTools.contextFor(fromThis, parameterName, this.aliasedParameterSet, argArray);
						} catch (Exception e) {
							Error er = new Error(" exception thrown in finding context for Context_begin");
							er.initCause(e);
							System.err.println(" exception thrown <" + er + ">");
							throw er;
						}
						Object value = ContextAnnotationTools.valueFor(fromThis, parameterName, this.aliasedParameterSet, argArray);
						if (value == null || (value instanceof String && value.equals("")))
							value = fromThis;

						String name = (String) parameterName.get("name");
						iStorage storage = (iStorage) context.storage.get(context.getAt(), null);
						storage.set(name, new BaseRef<Object>(value));

						System.out.println(" set <" + name + "> to be <" + value + "> in <" + context.getAt() + ">");

						ContextAnnotationTools.populateContexted(context, fromThis);
					}

				};

			}
		});

		anotatedMethodHandlers.put("Lfield/bytecode/protect/annotations/TimingStatistics;", new HandlesAnnontatedMethod() {
			public MethodVisitor handleEnd(int access, String methodName, String methodDesc, String signature, ClassVisitor classDelegate, MethodVisitor delegate, HashMap<String, Object> paramters, byte[] originalByteCode, String className) {
				return new BasicInstrumentation2.CallOnEntryAndExit_exceptionAware("dispatchOverTopology+" + methodName + "+" + methodDesc + "+" + signature + "+" + (uniq++), access, new Method(methodName, methodDesc), delegate, paramters) {

					TimingSupport support = new TimingSupport();

					@Override
					public Object handle(Object returningThis, String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, String methodReturnName) {
						support.handle_exit(fromThis, name, parameterName);
						return returningThis;
					}

					@Override
					public void handle(String fromName, Object fromThis, String methodName, Map<String, Object> parameterName, Object[] argArray) {
						support.handle_entry(fromThis, name, parameterName);
					}
				};

			}
		});

	}

	public Annotation[] getAllAnotationsForSuperMethodsOf(String name, String desc, Type[] at, String super_name, String[] interfaces) throws ClassNotFoundException {

		Class[] parameterClasses = new Class[at.length];

		for (int i = 0; i < parameterClasses.length; i++) {
			parameterClasses[i] = getClassFor(at[i].getClassName());
		}

		java.lang.reflect.Method javamethod = ReflectionTools.findMethodWithParametersUpwards(name, parameterClasses, checkedLoadClass(super_name));

		if (javamethod == null) {
			for (int i = 0; i < interfaces.length; i++) {
				javamethod = ReflectionTools.findMethodWithParametersUpwards(name, parameterClasses, checkedLoadClass(interfaces[i]));
				if (javamethod != null)
					break;
			}
			assert javamethod != null : " couldn't find method to inherit from in <" + name + "> with parameters <" + Arrays.asList(parameterClasses) + ">";
		}

		try {
			Annotation[] annotations = javamethod.getAnnotations();
			return annotations;
		} catch (Throwable t) {
			t.printStackTrace();
			// System.exit(1);
		}
		return null;

	}

	public Class getClassFor(String className) throws ClassNotFoundException {
		if (className.equals("int")) {
			return Integer.TYPE;
		}
		if (className.equals("float")) {
			return Float.TYPE;
		}
		if (className.equals("double")) {
			return Double.TYPE;
		} else {

			return checkedLoadClass(className);
		}
	}

	/**
	 * this is going to get a little intense
	 * 
	 * @throws ClassNotFoundException
	 */
	private Class checkedLoadClass(String className) throws ClassNotFoundException {

		className = className.replace('/', '.');
		if (debug)
			System.out.println(" looking for <" + className + "> in <" + alreadyLoaded + ">");
		if (alreadyLoaded.contains(className) || !shouldLoadLocal(className)) {
			return loader.loadClass(className);
		}

		// got to make
		// sure that we
		// actually
		// instrument
		// it.
		URLClassPath path = (URLClassPath) ReflectionTools.illegalGetObject(deferTo, "ucp");
		Resource resource = path.getResource(className.replace('.', '/').concat(".class"), false);

		InputStream stream = loader.getResourceAsStream(className.replace('.', '/').concat(".class"));
		byte[] bb = new byte[100];
		int cursor = 0;
		try {
			while (stream.available() > 0) {
				int c = stream.read(bb, cursor, bb.length - cursor);
				if (c <= 0)
					break;
				cursor += c;
				if (cursor > bb.length - 2) {
					byte[] b2 = new byte[bb.length * 2];
					System.arraycopy(bb, 0, b2, 0, bb.length);
					bb = b2;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte[] bytes = new byte[cursor];
		System.arraycopy(bb, 0, bytes, 0, cursor);

		// if (resource == null)
		// System.err.println(" warning, couldn't find superclass ? :" +
		// className + " " + path + " " + stream);
		// byte[] bytes = null;
		// try {
		// bytes = resource.getBytes();
		// } catch (IOException e1) {
		// e1.printStackTrace();
		// }

		if (debug)
			System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>  about to renter code with <" + className + "> and <" + bytes.length + ">");
		byte[] o = bytes;

		check();
		bytes = this.instrumentBytecodes(bytes, className, loader);
		check();
		if (debug) {
			System.out.println("<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< and out again <" + className + "> and <" + bytes.length + "> from <" + o.length + ">");
			FileOutputStream os;
			try {
				os = new FileOutputStream(new File("/var/tmp/old_" + className.replace('.', 'X') + ".class"));
				os.write(o);
				os.close();

				FileOutputStream os2 = new FileOutputStream(new File("/var/tmp/new_" + className.replace('.', 'X') + ".class"));
				os2.write(bytes);
				os2.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		try {
			// we
			// need
			// to
			// cache
			// annotations
			// for
			// all
			// of
			// the
			// methods
			check();
			Class cc = loader._defineClass(className, bytes, 0, bytes.length);
			check();
			return cc;
			// return
			// cc;
			// return
			// loader.loadClass(className);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		// catch
		// (IllegalAccessException
		// e) {
		// e.printStackTrace();
		// } catch
		// (InvocationTargetException
		// e) {
		// e.printStackTrace();
		// }
		assert false : "failure of grand plan for <" + className + ">";
		return null;
	}

	@Override
	protected byte[] instrumentBytecodes(byte[] a, final String class_name, ClassLoader deferTo) {
		assert !alreadyLoaded.contains(class_name) : " class name is <" + class_name + "> aready";
		alreadyLoaded.add(class_name);
		check();

		long modAt = cache.modificationForURL(deferTo.getResource(resourceNameForClassName(class_name)));
		if (!cache.is(class_name, true, modAt))
			return a;

		// System.out.println(class_name);
		if (!(class_name.startsWith("java") || class_name.startsWith("com/sun") || class_name.startsWith("sun") || class_name.startsWith("apple") || class_name.contains("protect") || class_name.startsWith("org.python") || class_name.startsWith("ch.") || class_name.startsWith("com.")))
			try {

				// check
				// for
				// interesting
				// annotation

				ClassReader reader = new ClassReader(a);
				// ClassWriter
				// writer
				// =
				// new
				// ClassWriter(true);
				final boolean[] woven = { false };
				final String[] superName = { null };
				final String[][] interfaces = { null };

				final boolean isAnon = class_name.indexOf("$") != -1;

				if (StandardTrampoline.debug)
					System.out.println(" checking for <" + class_name + "> <" + woven[0] + ">");
				ClassAdapter adaptor = new ClassAdapter(new EmptyVisitor()) {
					@Override
					public void visit(int version, int access, String name, String signature, String supern, String[] interfac) {
						superName[0] = supern;
						interfaces[0] = interfac;
					}

					@Override
					public AnnotationVisitor visitAnnotation(String name, boolean visibleAtRuntime) {
						if (name.equals("Lfield/bytecode/protect/Woven;")) {
							woven[0] = true;
						}
						return cv.visitAnnotation(name, visibleAtRuntime);
					}

					@Override
					public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
						if (!isAnon)
							return super.visitMethod(access, name, desc, signature, exceptions);
						return new EmptyVisitor() {
							@Override
							public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
								if (desc.equals("Lfield/bytecode/protect/Woven;"))
									woven[0] = true;
								return super.visitAnnotation(desc, visible);
							}
						};
					}
				};
				check();
				reader.accept(adaptor, 0);
				check();

				if (StandardTrampoline.debug)
					System.out.println(" class name <" + class_name + "> is <" + woven[0] + "> <" + isAnon + ">");

				if (!woven[0]) {
					cache.state(class_name, false, modAt);
					return a;
				} else {
					cache.state(class_name, true, modAt);
				}

				check();
				a = weave(a, class_name, deferTo, superName[0], interfaces[0]);
				check();

				if (debug) {
					FileOutputStream os2 = new FileOutputStream(new File("/var/tmp/woven_" + class_name.replace('.', 'X') + ".class"));
					os2.write(a);
					os2.close();
				}

				return a;
			} catch (Throwable t) {
				t.printStackTrace();
				System.exit(1);
			}
		return a;
	}

	protected byte[] weave(byte[] oa, final String class_name, ClassLoader deferTo, final String superName, final String[] interfaces) {
		inside++;
		check();

		if (StandardTrampoline.debug)
			System.out.println(" -- weaving <" + class_name + "> <" + inside + ">");
		try {
			final boolean[] isInterface = { false };
			{
				check();
				// phase
				// one
				// \u2014
				// deal
				// with
				// the
				// inheritwoven
				// tag
				ClassReader reader = new ClassReader(oa);
				ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
					protected String getCommonSuperClass(final String type1, final String type2) {
						ClassInfo c, d;
						try {
							c = new ClassInfo(type1, Trampoline2.trampoline.getClassLoader());
							d = new ClassInfo(type2, Trampoline2.trampoline.getClassLoader());
						} catch (Throwable e) {
							throw new RuntimeException(e);
						}
						if (c.isAssignableFrom(d)) {
							return type1;
						}
						if (d.isAssignableFrom(c)) {
							return type2;
						}
						if (c.isInterface() || d.isInterface()) {
							return "java/lang/Object";
						} else {
							do {
								c = c.getSuperclass();
							} while (!c.isAssignableFrom(d));
							return c.getType().getInternalName();
						}
					}

					class ClassInfo {

						private Type type;

						private ClassLoader loader;

						int access;

						String superClass;

						String[] interfaces;

						public ClassInfo(final String type, final ClassLoader loader) {
							this.loader = loader;
							this.type = Type.getObjectType(type);
							String s = type.replace('.', '/') + ".class";
							InputStream is = null;
							ClassReader cr;
							try {
								is = loader.getResourceAsStream(s);
								cr = new ClassReader(is);
							} catch (IOException e) {
								throw new RuntimeException(e);
							} finally {
								if (is != null) {
									try {
										is.close();
									} catch (Exception e) {
									}
								}
							}

							// optimized version
							int h = cr.header;
							ClassInfo.this.access = cr.readUnsignedShort(h);
							char[] buf = new char[2048];
							// String name =
							// cr.readClass(
							// cr.header + 2, buf);

							int v = cr.getItem(cr.readUnsignedShort(h + 4));
							ClassInfo.this.superClass = v == 0 ? null : cr.readUTF8(v, buf);
							ClassInfo.this.interfaces = new String[cr.readUnsignedShort(h + 6)];
							h += 8;
							for (int i = 0; i < interfaces.length; ++i) {
								interfaces[i] = cr.readClass(h, buf);
								h += 2;
							}
						}

						String getName() {
							return type.getInternalName();
						}

						Type getType() {
							return type;
						}

						int getModifiers() {
							return access;
						}

						ClassInfo getSuperclass() {
							if (superClass == null) {
								return null;
							}
							return new ClassInfo(superClass, loader);
						}

						ClassInfo[] getInterfaces() {
							if (interfaces == null) {
								return new ClassInfo[0];
							}
							ClassInfo[] result = new ClassInfo[interfaces.length];
							for (int i = 0; i < result.length; ++i) {
								result[i] = new ClassInfo(interfaces[i], loader);
							}
							return result;
						}

						boolean isInterface() {
							return (getModifiers() & Opcodes.ACC_INTERFACE) > 0;
						}

						private boolean implementsInterface(final ClassInfo that) {
							for (ClassInfo c = this; c != null; c = c.getSuperclass()) {
								ClassInfo[] tis = c.getInterfaces();
								for (int i = 0; i < tis.length; ++i) {
									ClassInfo ti = tis[i];
									if (ti.type.equals(that.type) || ti.implementsInterface(that)) {
										return true;
									}
								}
							}
							return false;
						}

						private boolean isSubclassOf(final ClassInfo that) {
							for (ClassInfo c = this; c != null; c = c.getSuperclass()) {
								if (c.getSuperclass() != null && c.getSuperclass().type.equals(that.type)) {
									return true;
								}
							}
							return false;
						}

						public boolean isAssignableFrom(final ClassInfo that) {
							if (this == that) {
								return true;
							}

							if (that.isSubclassOf(this)) {
								return true;
							}

							if (that.implementsInterface(this)) {
								return true;
							}

							if (that.isInterface() && getType().getDescriptor().equals("Ljava/lang/Object;")) {
								return true;
							}

							return false;
						}
					}

				};
				final byte[] fa = oa;
				ClassAdapter adaptor = new ClassAdapter(writer) {
					@Override
					public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
						super.visit(version, access, name, signature, superName, interfaces);
						isInterface[0] = (access & Opcodes.ACC_INTERFACE) != 0;
					}

					@Override
					public MethodVisitor visitMethod(final int access, final String name, final String desc, String signature, String[] exceptions) {
						final MethodVisitor m = cv.visitMethod(access, name, desc, signature, exceptions);
						return new InheritWovenMethodAdaptor(access, name, desc, m, superName, interfaces);
					}

				};

				check();
				System.out.println(" A ");
				reader.accept(adaptor, 0);
				System.out.println(" B ");
				check();
				oa = writer.toByteArray();
				check();

			}
			if (!isInterface[0]) {
				// phase
				// two
				// \u2014
				// deal
				// with
				// other
				// tages
				check();
				ClassReader reader = new ClassReader(oa);

				if (StandardTrampoline.debug)
					System.err.println(" ------- before instrumentation -----------------------------------------------------------------------------------");
				try {
					if (StandardTrampoline.debug)
						checkClass(oa);
				} catch (Exception e) {
					e.printStackTrace();
				}

				final byte[] fa = oa;
				ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
				CheckClassAdapter check = new CheckClassAdapter(writer);
				final List<TraceMethodVisitor> traceMethods = new ArrayList<TraceMethodVisitor>();
				ClassAdapter adaptor = new ClassAdapter(debug ? check : writer) {

					@Override
					public MethodVisitor visitMethod(final int access, final String name, final String desc, String signature, String[] exceptions) {
						final MethodVisitor m = super.visitMethod(access, name, desc, signature, exceptions);
						return new AnnotationMethodAdaptor(access, name, desc, signature, cv, m, superName, fa, class_name);
					}
				};
				check();
				reader.accept(adaptor, reader.EXPAND_FRAMES);
				check();
				oa = writer.toByteArray();
				check();

				if (StandardTrampoline.debug)
					System.err.println(" ------- after instrumentation -----------------------------------------------------------------------------------");
				try {
					checkClass(oa);
				} catch (Exception e) {
					e.printStackTrace();
				}
				check();
			}
			if (debug && true) {
				check();
				ClassReader reader = new ClassReader(oa);
				reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), ClassReader.EXPAND_FRAMES);
				check();
			}
		} catch (Throwable r) {
			r.printStackTrace();
		}
		inside--;
		return oa;
	}

}
