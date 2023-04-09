<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns="http://docbook.org/ns/docbook"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:db="http://docbook.org/ns/docbook"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:output method="xml" encoding="utf-8" indent="no"/>

<xsl:param name="version" required="yes" as="xs:string"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="db:info/db:productnumber/text()">
  <xsl:sequence select="replace(., '@@VERSION@@', $version)"/>
</xsl:template>

</xsl:stylesheet>
