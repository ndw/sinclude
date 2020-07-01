package com.nwalsh.sinclude.schemes;

import com.nwalsh.sinclude.exceptions.MalformedXPointerSchemeException;
import com.nwalsh.sinclude.exceptions.XIncludeIntegrityCheckException;
import com.nwalsh.sinclude.xpointer.DefaultSelectionResult;
import com.nwalsh.sinclude.xpointer.Scheme;
import com.nwalsh.sinclude.xpointer.SchemeData;
import com.nwalsh.sinclude.xpointer.SelectionResult;
import com.nwalsh.sinclude.xpointer.TextScheme;
import net.sf.saxon.s9api.XdmNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchScheme extends AbstractTextScheme implements TextScheme {
    // https://norman.walsh.name/2016/09/29/search

    private static final Pattern rangeRE = Pattern.compile("^.*?=(\\d*)?(,(\\d*)?)?$");
    private static final Pattern lengthRE = Pattern.compile("^length=(\\d+)(,[^;]*)?(.*)$");
    private static final Pattern leadingWhitespaceRE = Pattern.compile("^(\\s*)(\\S.*)$");
    private static final int INCLUDE_MATCH = 0;
    private static final int EXCLUDE_MATCH = 1;
    private static final int TRIM = 2;

    private String startSearch = null;
    private int startOpt = INCLUDE_MATCH;
    private int startCount = 1;
    private String endSearch = null;
    private int endOpt = INCLUDE_MATCH;
    private int endCount = 1;
    private boolean strip = false;
    private int checkLen = -1;

    @Override
    public SearchScheme newInstance(String fdata) {
        // Yes, this is probably all horribly inefficient...
        String select = fdata.trim();
        Matcher matcher = null;
        String rStartSearch = null;
        int rStartOpt = INCLUDE_MATCH;
        int rStartCount = 1;
        String rEndSearch = null;
        int rEndOpt = INCLUDE_MATCH;
        int rEndCount = 1;

        if ("".equals(select)) {
            malformedSearch("at least one of start/end required", fdata);
        }

        StringBuilder skip = new StringBuilder();
        char ch = select.charAt(0);
        if (ch == ',') {
            select = select.substring(1);
        } else {
            while (Character.isDigit(ch)) {
                skip.append(ch);
                select = select.substring(1);
                if ("".equals(select)) {
                    malformedSearch("start must specify a search string", fdata);
                }
                ch = select.charAt(0);
            }

            if (skip.length() != 0) {
                rStartCount = Integer.parseInt(skip.toString());
            }

            select = select.substring(1);
            int pos = select.indexOf(ch);
            if (pos < 0) {
                malformedSearch("unterminated start string", fdata);
            }
            rStartSearch = select.substring(0, pos);
            select = select.substring(pos+1).trim();

            if (select.startsWith(";trim")) {
                rStartOpt = TRIM;
                select = select.substring(5).trim();
            } else if (select.startsWith(";from")) {
                rStartOpt = INCLUDE_MATCH;
                select = select.substring(5).trim();
            } else if (select.startsWith(";after")) {
                rStartOpt = EXCLUDE_MATCH;
                select = select.substring(6).trim();
            } else if ("".equals(select) || select.startsWith(",")) {
                // ok
            } else {
                malformedSearch("invalid start option", fdata);
            }
        }

        if (select.startsWith(",")) {
            select = select.substring(1);
        }

        if (!"".equals(select)) {
            skip = new StringBuilder();
            ch = select.charAt(0);
            while (Character.isDigit(ch)) {
                skip.append(ch);
                select = select.substring(1);
                if ("".equals(select)) {
                    malformedSearch("end must specify a search string", fdata);
                }
                ch = select.charAt(0);
            }

            if (skip.length() != 0) {
                rEndCount = Integer.parseInt(skip.toString());
            }

            select = select.substring(1);
            int pos = select.indexOf(ch);
            if (pos < 0) {
                malformedSearch("unterminated end string", fdata);
            }
            rEndSearch = select.substring(0, pos);
            select = select.substring(pos+1).trim();

            if (select.startsWith(";trim")) {
                rEndOpt = TRIM;
                select = select.substring(5).trim();
            } else if (select.startsWith(";to")) {
                rEndOpt = INCLUDE_MATCH;
                select = select.substring(3).trim();
            } else if (select.startsWith(";before")) {
                rEndOpt = EXCLUDE_MATCH;
                select = select.substring(7).trim();
            }
        }

        if (select.startsWith(";")) {
            select = select.substring(1).trim();
        }

        boolean rstrip = false;
        if (select.startsWith("strip")) {
            rstrip = true;
            select = select.substring(5).trim();
            if (select.startsWith(";")) {
                select = select.substring(1).trim();
            }
        }

        int rCheckLen = -1;
        matcher = lengthRE.matcher(select);
        if (matcher.matches()) {
            rCheckLen = Integer.parseInt(matcher.group(1));
            String charset = matcher.group(2);
            select = matcher.group(3);

            if (select.startsWith(";")) {
                select = select.substring(1).trim();
            }

            if (select.startsWith("strip")) {
                rstrip = true;
                select = select.substring(5).trim();
            }
        }

        if (!"".equals(select)) {
            malformedSearch("unexpected characters at end", fdata);
        }

        SearchScheme scheme = new SearchScheme();
        scheme.startSearch = rStartSearch;
        scheme.startOpt = rStartOpt;
        scheme.startCount = rStartCount;
        scheme.endSearch = rEndSearch;
        scheme.endOpt = rEndOpt;
        scheme.endCount = rEndCount;
        scheme.strip = rstrip;
        scheme.checkLen = rCheckLen;
        return scheme;
    }

    @Override
    public String schemeName() {
        return "search";
    }

    @Override
    public String parseType() {
        return "text";
    }

    @Override
    public SelectionResult select(SchemeData[] schemeData, XdmNode document) {
        String text = getText(document);

        if (checkLen >= 0 && checkLen != text.length()) {
            throw new XIncludeIntegrityCheckException("Integrity check failed: " + checkLen + " != " + text.length());
        }

        String[] lines = text.split("\n");
        int startLine = -1;
        int endLine = -1;
        if (startSearch == null) {
            startLine = 0;
        } else {
            for (int lnum = 0; lnum < lines.length && startCount > 0; lnum++) {
                if (lines[lnum].contains(startSearch)) {
                    startCount--;
                    if (startCount == 0) {
                        if (startOpt == INCLUDE_MATCH) {
                            startLine = lnum;
                        } else {
                            startLine = lnum + 1;
                        }
                    }
                }
            }
        }

        if (endSearch == null) {
            endLine = lines.length;
        } else {
            for (int lnum = startLine; lnum < lines.length && endCount > 0; lnum++) {
                if (lines[lnum].contains(endSearch)) {
                    endCount--;
                    if (endCount == 0) {
                        if (endOpt == INCLUDE_MATCH) {
                            endLine = lnum;
                        } else {
                            endLine = lnum - 1;
                        }
                    }
                }
            }
        }

        if (startLine >= 0 && endLine >= 0 && startLine <= endLine){
            while (startOpt == TRIM && startLine <= endLine && "".equals(lines[startLine].trim())) {
                startLine++;
            }

            while (endOpt == TRIM && endLine >= startLine && "".equals(lines[endLine].trim())) {
                endLine--;
            }

            if (startLine > endLine) {
                return textResults(document, "");
            }

            int stripWS = Integer.MAX_VALUE;
            if (strip) {
                for (int pos = startLine; pos <= endLine && pos < lines.length; pos++) {
                    String line = lines[pos];
                    Matcher matcher = leadingWhitespaceRE.matcher(line);
                    if (matcher.matches()) {
                        int wslen = matcher.group(1).length();
                        if (wslen < stripWS) {
                            stripWS = wslen;
                        }
                    }
                }
            }

            StringBuilder data = new StringBuilder();
            for (int pos = startLine; pos <= endLine && pos < lines.length; pos++) {
                String line = lines[pos];
                if (strip && line.length() >= stripWS) {
                    line = line.substring(stripWS);
                }
                data.append(line).append("\n");
            }

            return textResults(document, data.toString());
        } else {
            return new DefaultSelectionResult(false, null);
        }
    }

    private void malformedSearch(String select, String msg) {
        throw new MalformedXPointerSchemeException("Malformed search: " + msg + ": " + select);
    }
}
