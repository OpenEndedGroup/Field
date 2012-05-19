package field.bytecode.protect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.CONSTRUCTOR,ElementType.METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface DispatchOverTopology{
	public Class topology();
	public boolean forwards() default true;
	public String tag() default "";
	public String id() default "";
}
