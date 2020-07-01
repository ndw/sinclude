package com.nwalsh.sinclude.xpointer;

public interface FragmentIdParser {
    public Scheme[] parseFragmentIdentifier(ParseType parseType, String fragid);
}
