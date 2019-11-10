package oscrabble.configuration;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Parameter
{
	String description();
//	Type type();

//	enum Type
//	{
//		INTEGER, STRING, BOOLEAN
//	}
}
