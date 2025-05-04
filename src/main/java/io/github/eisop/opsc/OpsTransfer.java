package io.github.eisop.opsc;

import com.sun.source.tree.MethodInvocationTree;
import io.github.eisop.opsc.qual.CreatesSqlStatement;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.jspecify.annotations.Nullable;

/** The transfer function for OPSC. */
public class OpsTransfer extends CFTransfer {

    private final OpsAnnotatedTypeFactory aTypeFactory;

    /** Create the transfer function for the OPSC. */
    public OpsTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);

        aTypeFactory = (OpsAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    private void insertAnnotation(
            AnnotationMirror annotation, TransferResult<CFValue, CFStore> result, Node receiver) {
        if (result.containsTwoStores()) {
            result.getThenStore().insertValue(JavaExpression.fromNode(receiver), annotation);
            result.getElseStore().insertValue(JavaExpression.fromNode(receiver), annotation);
        } else {
            result.getRegularStore().insertValue(JavaExpression.fromNode(receiver), annotation);
        }
    }

    @Override
    public TransferResult<CFValue, CFStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<CFValue, CFStore> in) {
        TransferResult<CFValue, CFStore> result = super.visitMethodInvocation(n, in);

        MethodInvocationTree tree = n.getTree();
        if (tree == null) {
            throw new TypeSystemError("MethodInvocationNode has null tree: " + n);
        }

        AnnotationMirror createsSqlStatementAnno = getCreatesSqlStatementAnno(tree);
        if (createsSqlStatementAnno == null) {
            return result;
        }

        // We are looking for methods where the receiver should be annotated
        // and assume this is the case for methods with a primitive or void return type
        if (!(n.getType().getKind().isPrimitive()
                || n.getType().getKind() == javax.lang.model.type.TypeKind.VOID)) {
            return result;
        }

        // annotate the receiver with a Sql or SqlUnsupported annotation
        Node receiver = n.getTarget().getReceiver();
        AnnotationMirror sqlAnnotation =
                aTypeFactory.annotateStatement(tree, createsSqlStatementAnno);
        insertAnnotation(sqlAnnotation, result, receiver);
        return result;
    }

    private @Nullable AnnotationMirror getCreatesSqlStatementAnno(MethodInvocationTree invocation) {
        ExecutableElement method = TreeUtils.elementFromUse(invocation);
        return aTypeFactory.getDeclAnnotation(method, CreatesSqlStatement.class);
    }
}
