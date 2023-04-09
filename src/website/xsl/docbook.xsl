<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:f="http://docbook.org/ns/docbook/functions"
                xmlns:fp="http://docbook.org/ns/docbook/functions/private"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:m="http://docbook.org/ns/docbook/modes"
                xmlns:mp="http://docbook.org/ns/docbook/modes/private"
                xmlns:rddl="http://www.rddl.org/"
                xmlns:t="http://docbook.org/ns/docbook/templates"
                xmlns:tp="http://docbook.org/ns/docbook/templates/private"
                xmlns:v="http://docbook.org/ns/docbook/variables"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:import href="https://cdn.docbook.org/release/xsltng/current/xslt/docbook.xsl"/>
<!--
<xsl:import href="/Volumes/Projects/docbook/xslTNG/build/xslt/docbook.xsl"/>
-->

<xsl:param name="revhistory-style" select="'list'"/>

<xsl:param name="lists-of-figures"  select="'false'"/>
<xsl:param name="lists-of-tables"   select="'false'"/>
<xsl:param name="lists-of-examples" select="'false'"/>
<xsl:param name="lists-of-equations" select="'false'"/>
<xsl:param name="lists-of-procedures" select="'false'"/>

<xsl:param name="section-toc-depth" select="1"/>

<xsl:param name="sections-inherit-from" select="'component section'"/>
<xsl:param name="callout-default-column" select="50"/>

<xsl:param name="chunk-section-depth" select="0"/>
<xsl:param name="chunk-include" as="xs:string*"
           select="('parent::db:book')"/>

<xsl:param name="persistent-toc" select="'true'"/>
<xsl:variable name="v:toc-open" as="element()">
  <i class="far fa-book"/>
</xsl:variable>
<xsl:variable name="v:toc-close" as="element()">
  <i class="far fa-window-close"/>
</xsl:variable>
<xsl:variable name="v:annotation-close" as="element()">
  <i class="far fa-window-close"/>
</xsl:variable>

<!-- ============================================================ -->

<xsl:variable name="v:templates" as="document-node()"
              xmlns:tmp="http://docbook.org/ns/docbook/templates"
              xmlns:db="http://docbook.org/ns/docbook"
              xmlns="http://www.w3.org/1999/xhtml">
  <xsl:document>
    <db:book>
      <header>
        <tmp:apply-templates select="db:mediaobject[@role='cover']"/>
        <tmp:apply-templates select="db:title">
          <h1><tmp:content/></h1>
        </tmp:apply-templates>
        <tmp:apply-templates select="db:subtitle">
          <h2><tmp:content/></h2>
        </tmp:apply-templates>
        <tmp:apply-templates select="db:author">
          <div class="author">
            <h3><tmp:content/></h3>
          </div>
        </tmp:apply-templates>
        <tmp:apply-templates select="db:releaseinfo">
          <p class="releaseinfo">
            <tmp:content/>
          </p>
        </tmp:apply-templates>
        <tmp:apply-templates select="db:pubdate">
          <p class="pubdate"><tmp:content/></p>
        </tmp:apply-templates>
        <tmp:apply-templates select="db:legalnotice"/>
        <tmp:apply-templates select="db:abstract"/>
        <tmp:apply-templates select="db:revhistory"/>
        <tmp:apply-templates select="db:copyright"/>
        <tmp:apply-templates select="db:productname"/>
      </header>
    </db:book>
  </xsl:document>
</xsl:variable>

<!-- ============================================================ -->

<xsl:template match="*" mode="m:html-head-links">
  <xsl:next-match/>
  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <script type="text/javascript"
          src="https://kit.fontawesome.com/498b6706f0.js" crossorigin="anonymous"/>
  <link rel="stylesheet" href="css/sinclude.css"/>
</xsl:template>

<!-- ============================================================ -->

<xsl:template name="t:top-nav">
  <xsl:param name="docbook" as="node()" tunnel="yes"/>
  <xsl:param name="chunk" as="xs:boolean"/>
  <xsl:param name="node" as="element()"/>
  <xsl:param name="prev" as="element()?"/>
  <xsl:param name="next" as="element()?"/>
  <xsl:param name="up" as="element()?"/>
  <xsl:param name="top" as="element()?"/>

  <xsl:if test="$chunk">
    <span class="nav">
      <a title="{$docbook/db:book/db:info/db:title}" href="{$top/@db-chunk/string()}">
        <i class="fas fa-home"></i>
      </a>
      <xsl:text>&#160;</xsl:text>

      <xsl:choose>
        <xsl:when test="exists($prev)">
          <a href="{$prev/@db-chunk/string()}" title="{f:title-content($prev)}">
            <i class="fas fa-arrow-left"></i>
          </a>
        </xsl:when>
        <xsl:otherwise>
          <span class="inactive">
            <i class="fas fa-arrow-left"></i>
          </span>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>&#160;</xsl:text>

      <xsl:choose>
        <xsl:when test="exists($up)">
          <a title="{f:title-content($up)}" href="{$up/@db-chunk/string()}">
            <i class="fas fa-arrow-up"></i>
          </a>
        </xsl:when>
        <xsl:otherwise>
          <span class="inactive">
            <i class="fas fa-arrow-up"></i>
          </span>
        </xsl:otherwise>
      </xsl:choose>
      <xsl:text>&#160;</xsl:text>

      <xsl:choose>
        <xsl:when test="exists($next)">
          <a title="{f:title-content($next)}"
             href="{$next/@db-chunk/string()}">
            <i class="fas fa-arrow-right"></i>
          </a>
        </xsl:when>
        <xsl:otherwise>
          <span class="inactive">
            <i class="fas fa-arrow-right"></i>
          </span>
        </xsl:otherwise>
      </xsl:choose>
    </span>
    
    <span class="title">
      <i class="title"><xsl:value-of select="/h:html/h:head/h:title"/></i>
    </span>
  </xsl:if>
</xsl:template>

<xsl:template name="t:bottom-nav">
  <xsl:param name="docbook" as="node()" tunnel="yes"/>
  <xsl:param name="chunk" as="xs:boolean"/>
  <xsl:param name="node" as="element()"/>
  <xsl:param name="prev" as="element()?"/>
  <xsl:param name="next" as="element()?"/>
  <xsl:param name="up" as="element()?"/>
  <xsl:param name="top" as="element()?"/>

  <xsl:if test="$chunk">
    <div class="navrow">
      <div class="navleft">
        <xsl:if test="count($prev)>0">
          <a title="{f:title-content($prev)}" href="{$prev/@db-chunk/string()}">
            <i class="fas fa-arrow-left"></i>
          </a>
        </xsl:if>
      </div>
      <div class="navmiddle">
        <xsl:if test="exists($top)">
          <a title="{f:title-content($top)}" href="{$top/@db-chunk/string()}">
            <i class="fas fa-home"></i>
          </a>
        </xsl:if>
      </div>
      <div class="navright">
        <xsl:if test="count($next)>0">
          <a title="{f:title-content($next)}" href="{$next/@db-chunk/string()}">
            <i class="fas fa-arrow-right"></i>
          </a>
        </xsl:if>
      </div>
    </div>

    <div class="navrow">
      <div class="navleft navtitle">
        <xsl:value-of select="f:title-content($prev)"/>
      </div>
      <div class="navmiddle">
        <xsl:if test="count($up) gt 0">
          <a title="{f:title-content($up)}" href="{$up/@db-chunk/string()}">
            <i class="fas fa-arrow-up"></i>
          </a>
        </xsl:if>
      </div>
      <div class="navright navtitle">
        <xsl:value-of select="f:title-content($next)"/>
      </div>
    </div>

    <xsl:variable name="db-node"
                  select="if ($node/@db-id)
                          then key('genid', $node/@db-id, $docbook)
                          else $docbook"/>

    <div class="infofooter">
      <xsl:variable name="years" select="root($db-node)/db:book/db:info/db:copyright/db:year"/>
      <span class="copyrightfooter">
        <xsl:text>Copyright</xsl:text>
        <xsl:text> &#xA9; </xsl:text>
        <xsl:value-of select="$years[1]"/>
        <xsl:if test="count($years) gt 1">
          <xsl:text>–</xsl:text>
          <xsl:value-of select="root($db-node)/db:book/db:info/db:copyright/db:year[last()]"/>
        </xsl:if>
        <xsl:text> Norman Walsh.</xsl:text>
      </span>

      <xsl:if test="$db-node/db:info/db:releaseinfo[@role='hash']">
        <xsl:variable name="hash"
                      select="$db-node/db:info/db:releaseinfo[@role='hash']"/>

        <span class="revision">
          <xsl:attribute name="title"
                         select="'git hash: '
                                 || substring($hash, 1, 6)
                                 || '…'"/>
          <xsl:text>Last revised by </xsl:text>
          <xsl:value-of
              select="substring-before($db-node/db:info/db:releaseinfo[@role='author'],
                      ' &lt;')"/>
          <xsl:text> on </xsl:text>
          <xsl:for-each select="$db-node/db:info/db:pubdate">
            <!-- hack: there should be only one -->
            <xsl:if test=". castable as xs:dateTime">
              <xsl:value-of select="format-dateTime(. cast as xs:dateTime,
                                    '[D1] [MNn,*-3] [Y0001]')"/>
            </xsl:if>
          </xsl:for-each>
        </span>
      </xsl:if>
    </div>
  </xsl:if>
</xsl:template>

<xsl:function name="f:title-content" as="node()*">
  <xsl:param name="node" as="element()?"/>

  <xsl:variable name="header" select="($node/h:header, $node/h:article/h:header)[1]"/>

  <xsl:variable name="title" as="element()?"
                select="($header/h:h1,
                         $header/h:h2,
                         $header/h:h3,
                         $header/h:h4,
                         $header/h:h5)[1]"/>

  <xsl:variable name="title" as="element()?"
                select="if (exists($title))
                        then $title
                        else ($node/h:div[@class='refnamediv']
                                 /h:p/h:span[@class='refname'])[1]"/>
 
  <xsl:sequence select="$title/node()"/>
</xsl:function>

<!-- ============================================================ -->

<xsl:template match="db:productname" mode="m:titlepage"
              expand-text="yes">
  <div class="versions">
    <p class="app">SInclude version {../db:productnumber/string()}</p>
  </div>
</xsl:template>

</xsl:stylesheet>
