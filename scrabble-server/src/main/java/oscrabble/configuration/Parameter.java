package oscrabble.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter {
	String label();

	String description() default "";

	/**
	 * @return Lower bound for bound parameter.
	 */
	int lowerBound() default -1;

	/**
	 * @return Upper bound for bound parameter.
	 */
	int upperBound() default -1;

	/**
	 * @return if should be represented as JSlide.
	 */
	boolean isSlide() default false;
}




