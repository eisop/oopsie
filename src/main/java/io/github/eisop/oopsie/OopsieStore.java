package io.github.eisop.oopsie;

import org.checkerframework.dataflow.expression.FieldAccess;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAbstractStore;
import org.checkerframework.framework.type.GenericAnnotatedTypeFactory;

public class OopsieStore extends CFAbstractStore<OopsieValue, OopsieStore> {

    //    private final List<ExecutableElement> setterAndGetterMethods = new ArrayList<>();

    protected OopsieStore(
            CFAbstractAnalysis<OopsieValue, OopsieStore, ?> analysis, boolean sequentialSemantics) {
        super(analysis, sequentialSemantics);
    }

    protected OopsieStore(CFAbstractStore<OopsieValue, OopsieStore> other) {
        super(other);
    }

    @Override
    protected OopsieValue newFieldValueAfterMethodCall(
            FieldAccess fieldAccess,
            GenericAnnotatedTypeFactory<OopsieValue, OopsieStore, ?, ?> atypeFactory,
            OopsieValue value) {
        // todo check if its a Statement setter/getter
        // or if it has Sql anno
        return value;
    }

    //    @Override
    //    public void updateForMethodCall(
    //            MethodInvocationNode methodInvocationNode,
    //            GenericAnnotatedTypeFactory<OopsieValue, OopsieStore, ?, ?> atypeFactory,
    //            OopsieValue val) {
    //        // Skip information removal for setter and getter calls
    //        if (setterAndGetterMethods.isEmpty()) {
    //            initializeSetterAndGetterMethods((OopsieAnnotatedTypeFactory) atypeFactory);
    //        }
    //        ExecutableElement method = methodInvocationNode.getTarget().getMethod();
    //        if (setterAndGetterMethods.contains(method)) {
    //            return;
    //        }
    //
    //        // Otherwise, call the super method to handle other method calls
    //        super.updateForMethodCall(methodInvocationNode, atypeFactory, val);
    //    }

    //    private void initializeSetterAndGetterMethods(OopsieAnnotatedTypeFactory aTypeFactory) {
    //        TypeMapping typeMapping = aTypeFactory.getTypeMapping();
    //        ProcessingEnvironment processingEnv = aTypeFactory.getProcessingEnv();
    //
    //        setterAndGetterMethods.addAll(typeMapping.getSetterMethods(processingEnv));
    //        setterAndGetterMethods.addAll(typeMapping.getGetterByIndexMethods(processingEnv));
    //        setterAndGetterMethods.addAll(typeMapping.getGetterByNameMethods(processingEnv));
    //    }
}
