package io.github.eisop.opsc;

import com.sun.source.tree.MethodInvocationTree;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
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

/** The transfer function for OPSC. */
public class OpsTransfer extends CFTransfer {

    private final OpsAnnotatedTypeFactory aTypeFactory;
    private final ProcessingEnvironment processingEnv;

    private final List<ExecutableElement> statementExecuteMethods;

    /** Create the transfer function for the OPSC. */
    public OpsTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);

        aTypeFactory = (OpsAnnotatedTypeFactory) analysis.getTypeFactory();
        processingEnv = aTypeFactory.getProcessingEnv();

        statementExecuteMethods =
                TreeUtils.getMethods("java.sql.Statement", "execute", 1, processingEnv);
        statementExecuteMethods.addAll(
                TreeUtils.getMethods("java.sql.Statement", "execute", 2, processingEnv));
        statementExecuteMethods.addAll(
                TreeUtils.getMethods("java.sql.Statement", "executeLargeUpdate", 1, processingEnv));
        statementExecuteMethods.addAll(
                TreeUtils.getMethods("java.sql.Statement", "executeLargeUpdate", 2, processingEnv));
        statementExecuteMethods.addAll(
                TreeUtils.getMethods("java.sql.Statement", "executeUpdate", 1, processingEnv));
        statementExecuteMethods.addAll(
                TreeUtils.getMethods("java.sql.Statement", "executeUpdate", 2, processingEnv));
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

        if (!isStatementExecuteMethodInvocation(tree)) {
            return result;
        }

        // annotate the receiver with a Sql or SqlUnsupported annotation
        Node receiver = n.getTarget().getReceiver();
        AnnotationMirror sqlAnnotation = aTypeFactory.annotateStatement(tree, false);
        insertAnnotation(sqlAnnotation, result, receiver);
        return result;
    }

    private boolean isStatementExecuteMethodInvocation(MethodInvocationTree tree) {
        int argSize = tree.getArguments().size();
        if (argSize < 1 || argSize > 2) {
            return false;
        }
        return statementExecuteMethods.stream()
                .anyMatch(m -> TreeUtils.isMethodInvocation(tree, m, processingEnv));
    }
}
