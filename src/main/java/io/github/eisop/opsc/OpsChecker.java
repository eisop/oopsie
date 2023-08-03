package io.github.eisop.opsc;

import java.util.LinkedHashSet;
import javax.annotation.processing.SupportedOptions;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;
import org.checkerframework.common.value.ValueChecker;

/** The main checker class for the Optional Prepared Statement Checker (OPSC). */
@SupportedOptions({"dbUrl", "dbUser", "dbPassword"})
public class OpsChecker extends BaseTypeChecker {
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new OpsVisitor(this);
    }

    @Override
    @SuppressWarnings("NonApiType")
    protected LinkedHashSet<Class<? extends BaseTypeChecker>> getImmediateSubcheckerClasses() {
        LinkedHashSet<Class<? extends BaseTypeChecker>> checkers =
                super.getImmediateSubcheckerClasses();
        checkers.add(ValueChecker.class);

        return checkers;
    }
}
