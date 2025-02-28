package io.github.eisop.opsc.qual;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.checkerframework.framework.qual.DefaultFor;
import org.checkerframework.framework.qual.InvisibleQualifier;
import org.checkerframework.framework.qual.SubtypeOf;
import org.checkerframework.framework.qual.TargetLocations;
import org.checkerframework.framework.qual.TypeUseLocation;

/** The bottom type for the type hierarchy. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
@SubtypeOf({SqlUnsupported.class})
@InvisibleQualifier
@TargetLocations({TypeUseLocation.LOWER_BOUND, TypeUseLocation.UPPER_BOUND})
@DefaultFor(value = {TypeUseLocation.LOWER_BOUND})
public @interface SqlBottom {}
