<?xml version="1.0" encoding="utf-8"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:n1="urn:aia.com/Jet/Structure">

  <xsl:template match="response_1">
  <n1:MT_JdbcQuerySP_Resp_Trans>
   <xsl:for-each select="row">
     <row>
      <xsl:apply-templates/>
     </row>
   </xsl:for-each>   
   <update_count><xsl:value-of select="update_count"/></update_count>
  </n1:MT_JdbcQuerySP_Resp_Trans>
  </xsl:template>
<xsl:template match="row/*">
        <datacolumn><xsl:apply-templates/></datacolumn>
    </xsl:template>


        
  

</xsl:stylesheet>

