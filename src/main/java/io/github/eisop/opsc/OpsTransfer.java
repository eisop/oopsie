package io.github.eisop.opsc;

import org.checkerframework.framework.flow.CFAbstractAnalysis;
import org.checkerframework.framework.flow.CFStore;
import org.checkerframework.framework.flow.CFTransfer;
import org.checkerframework.framework.flow.CFValue;

/** The transfer function for OPSC. */
public class OpsTransfer extends CFTransfer {
    /** Create the transfer function for the OPSC. */
    public OpsTransfer(CFAbstractAnalysis<CFValue, CFStore, CFTransfer> analysis) {
        super(analysis);
    }
}
