package field.bytecode.protect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.CONSTRUCTOR, ElementType.METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface ConstantContext {
	
	// these defautls must be kept in sync with the same defaults for ContextFor
	
	/*
	 * its set on the first complete exit (rather than first difference between entry and exit)
	 */
	public boolean immediate() default true;

	/*
	 * subsequent changes are ignored, and undone (rather than kept around for next entry)
	 */
	public boolean constant() default true;

	/*
	 * the context is always reset to be the same as where it entered
	 */
	public boolean resets() default true;

	/*
	 * this class has a class field called "context" that is of type ContextTopology
	 */
	public Class topology() default Object.class;

	
	public String group() default "--instance--";
}
