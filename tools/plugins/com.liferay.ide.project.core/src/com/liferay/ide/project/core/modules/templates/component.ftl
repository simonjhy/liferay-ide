@Component(
<#if (attributes?size>0) >
	<#list attributes as attribute>
		${attribute}
	</#list>
</#if>
<#if (properties?size>0) >
	immediate = true,
	property = {
	<#list properties as property>
	<#if ( property_index != properties?size-1 )>
        "${property}",
	<#else>
        "${property}"
	</#if>
	</#list>
    },
<#else>
	immediate = true,
</#if>    
    service = ${extensionclass}
)