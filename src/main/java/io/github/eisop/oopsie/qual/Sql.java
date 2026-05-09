package io.github.eisop.oopsie.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.SubtypeOf;

/** The Sql annotation to express input and output types. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({SqlUnknown.class})
public @interface Sql {
    /** The input types for the SQL query. */
    String[] in() default {};

    /** The output types for the SQL query. */
    String[] out() default {};

    /** The file name of the source code that contains the query */
    String file() default "";

    /** The location of the query within the source code file */
    String line() default "";

    /** The column of the query within the source code file */
    String column() default "";
}
