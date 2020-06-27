package com.nwalsh.sinclude.xpointer;

public interface FragmentIdParser {
    public Scheme[] parseFragmentIdentifier(String parseType, String fragid);
}
