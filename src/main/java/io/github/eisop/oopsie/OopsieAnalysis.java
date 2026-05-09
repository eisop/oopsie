package io.github.eisop.oopsie;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

public class OopsieAnalysis extends CFAbstractAnalysis<OopsieValue, OopsieStore, OopsieTransfer> {

    protected OopsieAnalysis(BaseTypeChecker checker, OopsieAnnotatedTypeFactory factory) {
        super(checker, factory); // todo cast needed?
    }

    @Override
    public OopsieStore createEmptyStore(boolean sequentialSemantics) {
        return new OopsieStore(this, sequentialSemantics);
    }

    @Override
    public OopsieStore createCopiedStore(OopsieStore oopsieStore) {
        return new OopsieStore(oopsieStore);
    }

    @Override
    public @Nullable OopsieValue createAbstractValue(
            AnnotationMirrorSet annotations, TypeMirror underlyingType) {
        if (!CFAbstractValue.validateSet(annotations, underlyingType, atypeFactory)) {
            return null;
        }
        return new OopsieValue(this, annotations, underlyingType);
    }
}
