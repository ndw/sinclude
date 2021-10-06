package com.nwalsh.sinclude.xpointer;

import com.nwalsh.sinclude.XInclude;
import com.nwalsh.sinclude.exceptions.MalformedXPointerSchemeException;
import com.nwalsh.sinclude.exceptions.UnknownXPointerSchemeException;
import com.nwalsh.sinclude.exceptions.UnparseableXPointerSchemeException;
import com.nwalsh.sinclude.exceptions.XIncludeSyntaxException;
import net.sf.saxon.s9api.ItemType;
import net.sf.saxon.s9api.ItemTypeFactory;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.QName;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmAtomicValue;

import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultFragmentIdParser implements FragmentIdParser {
    private static final QName xs_NCName = new QName("http://www.w3.org/2001/XMLSchema", "NCName");
    private static final Pattern githubLines = Pattern.compile("^L\\s*(\\d+)\\s*(-\\s*L\\s*(\\d+))?\\s*$");
    private static final Processor processor = new Processor(false);
    private static final ItemTypeFactory typeFactory = new ItemTypeFactory(processor);
    private XInclude xinclude = null;

    public DefaultFragmentIdParser(XInclude xinclude) {
        this.xinclude = xinclude;
    }

    @Override
    public Scheme[] parseFragmentIdentifier(ParseType parseType, String fragid) {
        switch (parseType) {
            case XMLPARSE:
                return parseXmlFragid(fragid);
            case TEXTPARSE:
                return parseTextFragid(fragid);
            default:
                throw new XIncludeSyntaxException("Only 'text' or 'xml' fragment identifiers are supported");
        }
    }

    private Scheme[] parseXmlFragid(String fragid) {
        // name
        // name/1/2/3
        // scheme(data) {scheme(data) ...}
        if (!fragid.contains("(")) {
            // Assume it's the element scheme
            Scheme scheme = xinclude.getScheme("element");
            if (scheme != null) {
                if (scheme instanceof XmlScheme) {
                    return new Scheme[] { ((XmlScheme) scheme).newInstance(fragid, xinclude) };
                }
                // Programmer error, someone's extended the set of scheme types in an incomplete way
                throw new RuntimeException("Unexpected scheme type in parseXmlFragid");
            } else {
                throw new UnknownXPointerSchemeException("Unknown scheme: element");
            }
        }

        try {
            // FIXME: There's probably a less expensive expensive way to do this
            ItemType itype = typeFactory.getAtomicType(xs_NCName);
            XdmAtomicValue avalue = new XdmAtomicValue(fragid, itype);

            Scheme scheme = xinclude.getScheme("element");
            if (scheme != null) {
                if (scheme instanceof XmlScheme) {
                    return new Scheme[] { ((XmlScheme) scheme).newInstance(fragid, xinclude) };
                }
                // Programmer error, someone's extended the set of scheme types in an incomplete way
                throw new RuntimeException("Unexpected scheme type in parseXmlFragid");
            } else {
                throw new UnknownXPointerSchemeException("Unknown scheme: element");
            }
        } catch (SaxonApiException xe) {
            // this is not an error; it just isn't a bare identifier
        }

        // scheme(...) ...
        Vector<Scheme> schemes = new Vector<Scheme>();
        SchemeParser parser = new SchemeParser(fragid);
        while (parser.hasMore()) {
            schemes.add(parser.next());
        }
        Scheme[] array = new Scheme[schemes.size()];
        schemes.toArray(array);
        return array;
    }

    private Scheme getTextSchemeInstance(String name, String fragid) {
        Scheme scheme = xinclude.getScheme(name);
        if (scheme != null) {
            if (scheme instanceof TextScheme) {
                return ((TextScheme) scheme).newInstance(fragid);
            }
            // Programmer error, someone's extended the set of scheme types in an incomplete way
            throw new RuntimeException("Unexpected scheme type in parseTextFragid");
        } else {
            throw new UnknownXPointerSchemeException("Unknown scheme: " + name);
        }
    }

    private Scheme[] parseTextFragid(String fragid) {
        // char=
        // line=
        // search=
        // L#[-L#]
        if (fragid.matches("^\\s*char\\s*=.*")
            || fragid.matches("^\\s*line\\s*=.*")) {
            return new Scheme[] { getTextSchemeInstance("text", fragid) };
        } else if (fragid.matches("^L\\s*\\d+\\s*(-\\s*L\\s*\\d+)?\\s*$")) {
            // Fake it.
            Matcher lmatcher = githubLines.matcher(fragid);
            if (lmatcher.find()) {
                String fakefragid = "line=";
                int linenum = Integer.parseInt(lmatcher.group(1));
                fakefragid += (linenum - 1);
                if (lmatcher.group(3) != null) {
                    linenum = Integer.parseInt(lmatcher.group(3));
                    fakefragid += "," + linenum;
                } else {
                    fakefragid += "," + linenum;
                }
                return new Scheme[] { getTextSchemeInstance("text", fakefragid) };
            } else {
                throw new RuntimeException("Internal error in L# fragid parser");
            }
        } else if (fragid.matches("^\\s*search\\s*=.*")) {
            int pos = fragid.indexOf("=");
            return new Scheme[] { getTextSchemeInstance("search", fragid.substring(pos+1)) };
        }

        // scheme(...) ...
        Vector<Scheme> schemes = new Vector<Scheme>();
        SchemeParser parser = new SchemeParser(fragid);
        while (parser.hasMore()) {
            schemes.add(parser.next());
        }
        Scheme[] array = new Scheme[schemes.size()];
        schemes.toArray(array);
        return array;
    }

    private class SchemeParser {
        private final Pattern schemeRE = Pattern.compile("^([\\w+:]+)\\s*\\((.*)$");
        private String fragid = null;

        public SchemeParser(String fragid) {
            this.fragid = fragid.trim();
        }

        public boolean hasMore() {
            return !"".equals(fragid);
        }

        public Scheme next() {
            Matcher matcher = schemeRE.matcher(fragid);
            if (matcher.find()) {
                String name = matcher.group(1);
                String saveid = matcher.group(2);
                fragid = saveid;
                StringBuilder data = new StringBuilder();
                boolean done = false;
                int openCount = 0;
                while (!done) {
                    if ("".equals(fragid)) {
                        throw new MalformedXPointerSchemeException("End of string in data");
                    } else if (fragid.startsWith("(")) {
                        openCount++;
                        data.append(fragid.charAt(0));
                        fragid = fragid.substring(1);
                    } else if (fragid.startsWith(")")) {
                        if (openCount > 0) {
                            openCount--;
                            data.append(fragid.charAt(0));
                            fragid = fragid.substring(1);
                        } else {
                            fragid = fragid.substring(1).trim();
                            done = true;
                        }
                    } else if (fragid.startsWith("^(") || fragid.startsWith("^)") || fragid.startsWith("^^")) {
                        data.append(fragid.charAt(1));
                        fragid = fragid.substring(2);
                    } else {
                        data.append(fragid.charAt(0));
                        fragid = fragid.substring(1);
                    }
                }

                if (openCount != 0) {
                    throw new MalformedXPointerSchemeException("Unbalanced, unescaped parens in " + saveid);
                }

                Scheme scheme = xinclude.getScheme(name);
                if (scheme != null) {
                    if (scheme instanceof XmlScheme) {
                        return ((XmlScheme) scheme).newInstance(data.toString(), xinclude);
                    }
                    if (scheme instanceof TextScheme) {
                        return ((TextScheme) scheme).newInstance(data.toString());
                    }
                    // Programmer error, someone's extended the set of scheme types in an incomplete way
                    throw new RuntimeException("Unexpected scheme type");
                } else {
                    throw new UnknownXPointerSchemeException("Unknown scheme: " + name);
                }
            } else {
                throw new UnparseableXPointerSchemeException("Unparsable: " + fragid);
            }
        }
    }
}
