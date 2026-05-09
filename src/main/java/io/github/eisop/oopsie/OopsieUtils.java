package io.github.eisop.oopsie;

import java.util.Collections;
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.javacutil.AnnotationUtils;

public class OopsieUtils {

    protected static List<String> retrieveStringValues(
            @Nullable AnnotationMirror stringValAnnoMirror,
            ExecutableElement stringValValueElement) {
        if (stringValAnnoMirror == null) return Collections.emptyList();
        return AnnotationUtils.getElementValueArray(
                stringValAnnoMirror, stringValValueElement, String.class, Collections.emptyList());
    }
}
