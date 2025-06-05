package io.github.eisop.opsc;

import javax.lang.model.type.TypeMirror;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractValue;
import org.checkerframework.javacutil.AnnotationMirrorSet;

public class OpsAnalysis extends CFAbstractAnalysis<OpsValue, OpsStore, OpsTransfer> {

    protected OpsAnalysis(BaseTypeChecker checker, OpsAnnotatedTypeFactory factory) {
        super(checker, factory); // todo cast needed?
    }

    @Override
    public OpsStore createEmptyStore(boolean sequentialSemantics) {
        return new OpsStore(this, sequentialSemantics);
    }

    @Override
    public OpsStore createCopiedStore(OpsStore opsStore) {
        return new OpsStore(opsStore);
    }

    @Override
    public @Nullable OpsValue createAbstractValue(
            AnnotationMirrorSet annotations, TypeMirror underlyingType) {
        if (!CFAbstractValue.validateSet(annotations, underlyingType, atypeFactory)) {
            return null;
        }
        return new OpsValue(this, annotations, underlyingType);
    }
}
