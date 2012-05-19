package field.bytecode.protect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value=ElementType.METHOD)
@Retention(value=RetentionPolicy.RUNTIME)
public @interface NextUpdate{
	public int delay() default 1;
	public boolean saveAliasing() default true;
	public boolean saveContextLocation() default true;
}
