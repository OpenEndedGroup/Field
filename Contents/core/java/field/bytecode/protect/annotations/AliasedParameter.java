package field.bytecode.protect.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value={ElementType.FIELD, ElementType.PARAMETER})
@Retention(value=RetentionPolicy.RUNTIME)
public @interface AliasedParameter {

	public String name() default "";
	public String expression() default "";
}
