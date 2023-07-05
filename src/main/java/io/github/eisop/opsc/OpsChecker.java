package io.github.eisop.opsc;

import javax.annotation.processing.SupportedOptions;
import org.checkerframework.common.basetype.BaseTypeChecker;
import org.checkerframework.common.basetype.BaseTypeVisitor;

/** The main checker class for the Optional Prepared Statement Checker (OPSC). */
@SupportedOptions({"dbUrl", "dbUser", "dbPassword"})
public class OpsChecker extends BaseTypeChecker {
    @Override
    protected BaseTypeVisitor<?> createSourceVisitor() {
        return new OpsVisitor(this);
    }
}
