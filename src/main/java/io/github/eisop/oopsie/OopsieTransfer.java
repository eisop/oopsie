package io.github.eisop.oopsie;

import com.sun.source.tree.MethodInvocationTree;
import io.github.eisop.oopsie.qual.CreatesSqlStatement;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import org.checkerframework.dataflow.analysis.TransferInput;
import org.checkerframework.dataflow.analysis.TransferResult;
import org.checkerframework.dataflow.cfg.node.MethodInvocationNode;
import org.checkerframework.dataflow.cfg.node.Node;
import org.checkerframework.dataflow.expression.JavaExpression;
import org.checkerframework.framework.flow.CFAbstractTransfer;
import org.checkerframework.javacutil.TreeUtils;
import org.checkerframework.javacutil.TypeSystemError;
import org.jspecify.annotations.Nullable;

/** The transfer function for oopsie. */
public class OopsieTransfer extends CFAbstractTransfer<OopsieValue, OopsieStore, OopsieTransfer> {

    private final OopsieAnnotatedTypeFactory aTypeFactory;

    /** Create the transfer function for the oopsie. */
    public OopsieTransfer(OopsieAnalysis analysis) {
        super(analysis);

        aTypeFactory = (OopsieAnnotatedTypeFactory) analysis.getTypeFactory();
    }

    private void insertAnnotation(
            AnnotationMirror annotation,
            TransferResult<OopsieValue, OopsieStore> result,
            Node receiver) {
        if (result.containsTwoStores()) {
            result.getThenStore().insertValue(JavaExpression.fromNode(receiver), annotation);
            result.getElseStore().insertValue(JavaExpression.fromNode(receiver), annotation);
        } else {
            result.getRegularStore().insertValue(JavaExpression.fromNode(receiver), annotation);
        }
    }

    @Override
    public TransferResult<OopsieValue, OopsieStore> visitMethodInvocation(
            MethodInvocationNode n, TransferInput<OopsieValue, OopsieStore> in) {
        TransferResult<OopsieValue, OopsieStore> result = super.visitMethodInvocation(n, in);

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
