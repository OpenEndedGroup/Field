/*
 * Copyright 2001-2005 Geert Bevin <gbevin[remove] at uwyn dot com>
 * Distributed under the terms of either:
 * - the common development and distribution license (CDDL), v1.0; or
 * - the GNU Lesser General Public License, v2.1 or later
 * $Id: TypesClassVisitor.java 1175 2005-01-05 20:20:17Z gbevin $
 */
package field.bytecode.protect.analysis;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Attribute;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.commons.EmptyVisitor;

abstract public class TypesClassVisitor implements ClassVisitor {
	// private MetricsClassVisitor mMetrics = null;

	private String mClassName = null;

	private String mEntryMethod = null;

	private TypesContext[] mPauseContexts = null;

	private TypesContext[] mLabelContexts = null;

	private int mPauseContextCounter = 0;

	private int mLabelContextCounter = 0;

	public TypesClassVisitor(String className, String entryMethod) {
		// mMetrics = metrics;
		mClassName = className;
		mEntryMethod = entryMethod;
	}

	//
	// MetricsClassVisitor getMetrics() {
	// return mMetrics;
	// }

	void setPauseContexts(TypesContext[] pauseContexts) {
		mPauseContexts = pauseContexts;
	}

	public TypesContext nextPauseContext() {
		return mPauseContexts[mPauseContextCounter++];
	}

	void setLabelContexts(TypesContext[] labelContexts) {
		mLabelContexts = labelContexts;
	}

	TypesContext nextLabelTypes() {
		return mLabelContexts[mLabelContextCounter++];
	}

	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		if (mEntryMethod.equals(name + desc)) { return new TypesMethodVisitor(this, mClassName){
			protected boolean isYieldCall(String owner_classname, String name, String desc) {
				return TypesClassVisitor.this.isYieldCall(owner_classname, name, desc);
			};
		};

		}

		return null;
	}

	abstract protected boolean isYieldCall(String owner_classname, String name, String desc);

	public void visitInnerClass(String name, String outerName, String innerName, int access) {
	}

	public void visitOuterClass(String owner, String name, String desc) {
	}

	public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
	}

	public void visitSource(String source, String debug) {
	}

	public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
		return new EmptyVisitor();
	}

	public void visitAttribute(Attribute attr) {
	}

	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
		return new EmptyVisitor();
	}

	public void visitEnd() {
	}
}
