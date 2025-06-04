package io.github.eisop.opsc;

import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

public class OpsStore extends CFAbstractStore<OpsValue, OpsStore> {

    //    private final List<ExecutableElement> setterAndGetterMethods = new ArrayList<>();

    protected OpsStore(
            CFAbstractAnalysis<OpsValue, OpsStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    protected OpsStore(CFAbstractStore<OpsValue, OpsStore> other) {
        super(other);
    }

    @Override
    protected OpsValue newFieldValueAfterMethodCall(
            FieldAccess fieldAccess,
            GenericAnnotatedTypeFactory<OpsValue, OpsStore, ?, ?> atypeFactory,
            OpsValue value) {
        // todo check if its a Statement setter/getter
        // or if it has Sql anno
        return value;
    }

    //    @Override
    //    public void updateForMethodCall(
    //            MethodInvocationNode methodInvocationNode,
    //            GenericAnnotatedTypeFactory<OpsValue, OpsStore, ?, ?> atypeFactory,
    //            OpsValue val) {
    //        // Skip information removal for setter and getter calls
    //        if (setterAndGetterMethods.isEmpty()) {
    //            initializeSetterAndGetterMethods((OpsAnnotatedTypeFactory) atypeFactory);
    //        }
    //        ExecutableElement method = methodInvocationNode.getTarget().getMethod();
    //        if (setterAndGetterMethods.contains(method)) {
    //            return;
    //        }
    //
    //        // Otherwise, call the super method to handle other method calls
    //        super.updateForMethodCall(methodInvocationNode, atypeFactory, val);
    //    }

    //    private void initializeSetterAndGetterMethods(OpsAnnotatedTypeFactory aTypeFactory) {
    //        TypeMapping typeMapping = aTypeFactory.getTypeMapping();
    //        ProcessingEnvironment processingEnv = aTypeFactory.getProcessingEnv();
    //
    //        setterAndGetterMethods.addAll(typeMapping.getSetterMethods(processingEnv));
    //        setterAndGetterMethods.addAll(typeMapping.getGetterByIndexMethods(processingEnv));
    //        setterAndGetterMethods.addAll(typeMapping.getGetterByNameMethods(processingEnv));
    //    }
}
