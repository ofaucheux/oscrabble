package oscrabble.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter
{
	String label();
	String description() default "";
	String elementOf() default "";
}
