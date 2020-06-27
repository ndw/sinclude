package com.nwalsh.sinclude.xpointer;

import net.sf.saxon.s9api.XdmNode;

import java.util.Collections;
import java.util.Vector;

public class DefaultSelectionResult implements SelectionResult {
    private final Vector<SchemeData> data = new Vector<>();
    private boolean finished = false;
    private XdmNode result = null;

    public DefaultSelectionResult(SchemeData[] data, boolean finished, XdmNode result) {
        Collections.addAll(this.data, data);
        this.finished = finished;
        this.result = result;
    }

    public DefaultSelectionResult(SchemeData[] data, boolean finished) {
        Collections.addAll(this.data, data);
        this.finished = finished;
    }

    public DefaultSelectionResult(boolean finished, XdmNode result) {
        this.finished = finished;
        this.result = result;
    }

    public DefaultSelectionResult(boolean finished) {
        this.finished = finished;
    }

    @Override
    public SchemeData[] getSchemeData() {
        SchemeData[] array = new SchemeData[data.size()];
        data.toArray(array);
        return array;
    }

    @Override
    public boolean finished() {
        return finished;
    }

    @Override
    public XdmNode getResult() {
        return result;
    }
}
