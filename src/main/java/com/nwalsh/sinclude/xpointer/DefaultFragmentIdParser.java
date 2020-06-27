package com.nwalsh.sinclude.xpointer;

import com.nwalsh.sinclude.XInclude;
import com.nwalsh.sinclude.exceptions.MalformedXPointerSchemeException;
import com.nwalsh.sinclude.exceptions.UnknownXPointerSchemeException;
import com.nwalsh.sinclude.exceptions.UnparseableXPointerSchemeException;
import com.nwalsh.sinclude.schemes.ElementScheme;
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
    private static final Processor processor = new Processor(false);
    private static final ItemTypeFactory typeFactory = new ItemTypeFactory(processor);
    private XInclude xinclude = null;

    public DefaultFragmentIdParser(XInclude xinclude) {
        this.xinclude = xinclude;
    }

    @Override
    public Scheme[] parseFragmentIdentifier(String parseType, String fragid) {
        switch (parseType) {
            case "xml":
                return parseXmlFragid(fragid);
            case "text":
                return parseTextFragid(fragid);
            default:
                throw new IllegalArgumentException("Only 'text' or 'xml' fragment identifiers are supported");
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
                return new Scheme[] { scheme.newInstance(fragid) };
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
                return new Scheme[] { scheme.newInstance(fragid) };
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

    private Scheme[] parseTextFragid(String fragid) {
        // char=
        // line=
        // search=
        if (fragid.matches("^\\s*char\\s*=.*")) {
            Scheme scheme = xinclude.getScheme("text");
            if (scheme != null) {
                return new Scheme[] { scheme.newInstance(fragid) };
            } else {
                throw new UnknownXPointerSchemeException("Unknown scheme: text");
            }
        } else if (fragid.matches("^\\s*line\\s*=.*")) {
            Scheme scheme = xinclude.getScheme("text");
            if (scheme != null) {
                return new Scheme[] { scheme.newInstance(fragid) };
            } else {
                throw new UnknownXPointerSchemeException("Unknown scheme: text");
            }
        } else if (fragid.matches("^\\s*search\\s*=.*")) {
            Scheme scheme = xinclude.getScheme("search");
            if (scheme != null) {
                int pos = fragid.indexOf("=");
                return new Scheme[] { scheme.newInstance(fragid.substring(pos+1).trim()) };
            } else {
                throw new UnknownXPointerSchemeException("Unknown scheme: text");
            }
        } else {
            throw new UnparseableXPointerSchemeException("Unparsable: " + fragid);
        }
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
                fragid = matcher.group(2);
                StringBuilder data = new StringBuilder();
                boolean done = false;
                while (!done) {
                    if ("".equals(fragid)) {
                        throw new MalformedXPointerSchemeException("End of string in data");
                    } else if (fragid.startsWith(")")) {
                        fragid = fragid.substring(1).trim();
                        done = true;
                    } else if (fragid.startsWith("^(") || fragid.startsWith("^)") || fragid.startsWith("^^")) {
                        data.append(fragid.substring(1,2));
                        fragid = fragid.substring(2);
                    } else if (fragid.startsWith("(")) {
                        throw new MalformedXPointerSchemeException("Unescaped open paren encountered");
                    } else {
                        data.append(fragid.substring(0,1));
                        fragid = fragid.substring(1);
                    }
                }

                Scheme scheme = xinclude.getScheme(name);
                if (scheme != null) {
                    return scheme.newInstance(data.toString());
                } else {
                    throw new UnknownXPointerSchemeException("Unknown scheme: " + name);
                }
            } else {
                throw new UnparseableXPointerSchemeException("Unparsable: " + fragid);
            }
        }
    }
}
