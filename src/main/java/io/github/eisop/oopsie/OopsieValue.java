package io.github.eisop.oopsie;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

public class OopsieValue extends CFAbstractValue<OopsieValue> {

    protected OopsieValue(
            CFAbstractAnalysis<OopsieValue, ?, ?> analysis,
            AnnotationMirrorSet annotations,
            TypeMirror underlyingType) {
        super(analysis, annotations, underlyingType);
    }
}
