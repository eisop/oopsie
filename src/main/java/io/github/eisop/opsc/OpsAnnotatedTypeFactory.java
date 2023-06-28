package io.github.eisop.opsc;

import io.github.eisop.opsc.qual.Sql;
import io.github.eisop.opsc.qual.SqlBottom;
import io.github.eisop.opsc.qual.SqlUnknown;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Set;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.util.Elements;
import org.checkerframework.common.basetype.BaseAnnotatedTypeFactory;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;
import org.checkerframework.framework.type.MostlyNoElementQualifierHierarchy;
import org.checkerframework.framework.type.QualifierHierarchy;
import org.checkerframework.framework.util.QualifierKind;
import org.checkerframework.javacutil.AnnotationBuilder;
import org.checkerframework.javacutil.TypeSystemError;

public class OpsAnnotatedTypeFactory extends BaseAnnotatedTypeFactory {

    protected final AnnotationMirror SQL = AnnotationBuilder.fromClass(elements, Sql.class);

    protected final AnnotationMirror SQLUNKNOWN =
            AnnotationBuilder.fromClass(elements, SqlUnknown.class);
    protected final AnnotationMirror SQLBOTTOM =
            AnnotationBuilder.fromClass(elements, SqlBottom.class);

    public OpsAnnotatedTypeFactory(BaseTypeChecker checker) {
        super(checker);

        this.postInit();
    }

    @Override
    protected Set<Class<? extends Annotation>> createSupportedTypeQualifiers() {
        return getBundledTypeQualifiers(SqlUnknown.class, Sql.class, SqlBottom.class);
    }

    @Override
    public CFTransfer createFlowTransferFunction(
            CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        return new OpsTransfer((CFAnalysis) analysis);
    }

    @Override
    protected QualifierHierarchy createQualifierHierarchy() {
        return new OpsQualifierHierarchy(this.getSupportedTypeQualifiers(), elements);
    }

    private final class OpsQualifierHierarchy extends MostlyNoElementQualifierHierarchy {
        private final QualifierKind SQL_KIND;

        private OpsQualifierHierarchy(
                Collection<Class<? extends Annotation>> qualifierClasses, Elements elements) {
            super(qualifierClasses, elements);
            SQL_KIND = getQualifierKind(SQL);
        }

        @Override
        protected boolean isSubtypeWithElements(
                AnnotationMirror subAnno,
                QualifierKind subKind,
                AnnotationMirror superAnno,
                QualifierKind superKind) {
            if (subKind == SQL_KIND && superKind == SQL_KIND) {
                // TODO: Compare subAnno and superAnno.
                return subAnno == superAnno;
            }
            throw new TypeSystemError("Unexpected qualifiers: %s %s", subAnno, superAnno);
        }

        @Override
        protected AnnotationMirror leastUpperBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind lubKind) {
            if (qualifierKind1 == SQL_KIND && qualifierKind2 == SQL_KIND) {
                // TODO: actually look at values.
                if (a1 == a2) {
                    return a1;
                } else {
                    return SQLUNKNOWN;
                }
            }
            throw new TypeSystemError("Unexpected qualifiers: %s %s", a1, a2);
        }

        @Override
        protected AnnotationMirror greatestLowerBoundWithElements(
                AnnotationMirror a1,
                QualifierKind qualifierKind1,
                AnnotationMirror a2,
                QualifierKind qualifierKind2,
                QualifierKind glbKind) {
            if (qualifierKind1 == SQL_KIND && qualifierKind2 == SQL_KIND) {
                // TODO: actually look at values.
                if (a1 == a2) {
                    return a1;
                } else {
                    return SQLBOTTOM;
                }
            }
            throw new TypeSystemError("Unexpected qualifiers: %s %s", a1, a2);
        }
    }
}
