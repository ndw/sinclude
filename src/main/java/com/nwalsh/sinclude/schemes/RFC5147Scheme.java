package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.exceptions.UnparseableXPointerSchemeException;
import com.nwalsh.sinclude.exceptions.XIncludeIntegrityCheckException;
import com.nwalsh.sinclude.xpointer.DefaultSelectionResult;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import com.nwalsh.sinclude.xpointer.TextScheme;
import net.sf.saxon.s9api.XdmNode;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class RFC5147Scheme extends AbstractTextScheme implements TextScheme {
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
    public RFC5147Scheme newInstance(String fdata) {
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
            integrity = data.substring(pos+1).trim();
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

        RFC5147Scheme scheme = new RFC5147Scheme();
        scheme.rangeStart = fRangeStart;
        scheme.rangeEnd = fRangeEnd;
        scheme.lineScheme = fLineScheme;

        while (integrity != null) {
            int pos = integrity.indexOf(";");
            String ischeme = null;
            if (pos >= 0) {
                if (pos > 0) {
                    ischeme = integrity.substring(0, pos);
                }
                integrity = integrity.substring(pos+1).trim();
                if ("".equals(integrity)) {
                    integrity = null;
                }
            } else {
                ischeme = integrity;
                integrity = null;
            }

            if (ischeme != null) {
                pos = ischeme.indexOf(",");
                if (pos >= 0) {
                    String charset = ischeme.substring(pos+1).trim();
                    ischeme = ischeme.substring(0, pos).trim();

                    if (!"utf-8".equals(charset.toLowerCase())) {
                        // Ignore this length; the only characaters we'll have will be utf-8.
                        ischeme = "";
                    }
                }

                if (ischeme.matches("^length=\\d+$")) {
                    scheme.length = Integer.parseInt(ischeme.substring(7));
                } else if (ischeme.matches("^md5=[0-9A-Fa-f]+$")) {
                    scheme.md5 = ischeme.substring(4);
                } else {
                    // nop; ignore unknown schemes
                }
            }
        }

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

        if (length >= 0 && length != text.length()) {
            throw new XIncludeIntegrityCheckException("Document length is " + text.length() + "; expected " + length);
        }

        if (md5 != null) {
            byte[] bytesOfMessage = text.getBytes(StandardCharsets.UTF_8);
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                String digest = new BigInteger(1,md.digest(bytesOfMessage)).toString(16);
                if (!md5.equals(digest)) {
                    throw new XIncludeIntegrityCheckException("Document md5 is " + digest + "; expected " + md5);
                }
            } catch (NoSuchAlgorithmException nsae) {
                // This can't happen and ignore it if it does.
            }
        }

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
