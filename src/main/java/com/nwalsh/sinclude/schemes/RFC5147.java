package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.exceptions.UnparseableXPointerSchemeException;
import com.nwalsh.sinclude.xpointer.DefaultSelectionResult;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import com.nwalsh.sinclude.xpointer.TextScheme;
import net.sf.saxon.s9api.XdmNode;

public class RFC5147 extends AbstractTextScheme implements TextScheme {
    // RFC 5147:
    // text-fragment   =  text-scheme 0*( ";" integrity-check )
    // text-scheme     =  ( char-scheme / line-scheme )
    // char-scheme     =  "char=" ( position / range )
    // line-scheme     =  "line=" ( position / range )
    // integrity-check =  ( length-scheme / md5-scheme )
    //                      [ "," mime-charset ]
    // position        =  number
    // range           =  ( position "," [ position ] ) / ( "," position )
    // number          =  1*( DIGIT )
    // length-scheme   =  "length=" number
    // md5-scheme      =  "md5=" md5-value
    // md5-value       =  32HEXDIG
    private boolean lineScheme = true;
    private int rangeStart = -1;
    private int rangeEnd = -1;
    private int length = -1;
    private String md5 = null;

    @Override
    public RFC5147 newInstance(String fdata) {
        boolean fLineScheme = false;
        if (fdata.startsWith("line=")) {
            fLineScheme = true;
        } else if (fdata.startsWith("char=")) {
            fLineScheme = false;
        } else {
            throw new UnparseableXPointerSchemeException("Unparseable text scheme: " + fdata);
        }

        String data = fdata.substring(5).trim();
        String integrity = null;
        if (data.contains(";")) {
            int pos = data.indexOf(";");
            integrity = data.substring(pos+1);
            data = data.substring(0,pos).trim();
        }

        int fRangeStart = -1;
        int fRangeEnd = Integer.MAX_VALUE;
        if (data.matches("^(\\d+)?,(\\d+)?$")) {
            int pos = data.indexOf(",");
            if (pos > 0) {
                fRangeStart = Integer.parseInt(data.substring(0, pos));
            }
            if (!"".equals(data.substring(pos+1))) {
                fRangeEnd = Integer.parseInt(data.substring(pos+1));
            }
        } else if (data.matches("^\\d+$")) {
            // This is pointless, but it's not syntactically invalid
            fRangeStart = Integer.parseInt(data);
            fRangeEnd = fRangeStart;
        } else {
            throw new UnparseableXPointerSchemeException("Unparseable text scheme: " + fdata);
        }

        // FIXME: deal with the integrity checks

        RFC5147 scheme = new RFC5147();
        scheme.rangeStart = fRangeStart;
        scheme.rangeEnd = fRangeEnd;
        scheme.lineScheme = fLineScheme;
        return scheme;
    }

    @Override
    public String schemeName() {
        return "text";
    }

    @Override
    public String parseType() {
        return "text";
    }

    @Override
    public SelectionResult select(SchemeData[] schemeData, XdmNode document) {
        String text = getText(document);

        int pos = 0;
        String[] lines = text.split("\n");
        // False unless vacuously true
        boolean found = ((rangeStart == rangeEnd) && rangeStart < lines.length);
        StringBuilder data = new StringBuilder();
        for (int lineno = 0; pos < rangeEnd && lineno < lines.length; lineno++) {
            String line = lines[lineno];

            found = found || (pos >= rangeStart);

            if (lineScheme) {
                if (pos >= rangeStart) {
                    data.append(line).append("\n");
                }
                pos += 1;
            } else {
                int endcp = pos + line.length() + 1;

                if (pos < rangeStart && endcp > rangeStart) {
                    found = true;
                    line = line.substring((int) (rangeStart - pos));
                    pos = rangeStart;
                }

                if (pos >= rangeStart && pos < rangeEnd) {
                    long rest = rangeEnd - pos;
                    if (rest > line.length()) {
                        data.append(line).append("\n");
                    } else {
                        data.append(line.substring(0,(int) rest));
                    }
                }

                pos = endcp;
            }
        }

        if (found) {
            return textResults(document, data.toString());
        } else {
            return new DefaultSelectionResult(false, null);
        }
    }
}
