<#include "/org/alfresco/include/alfresco-template.ftl" />

<@templateHeader>
    <meta http-equiv="Cache-Control" content="private" >

   <@link rel="stylesheet" type="text/css" href="${url.context}/res/citeck/components/card/card-details.css" />

   <@script type="text/javascript" src="${url.context}/res/citeck/modules/node-denied/node-denied.js"></@script>
   <@script type="text/javascript" src="${url.context}/res/modules/documentlibrary/doclib-actions.js"></@script>
   <@link rel="stylesheet" type="text/css" href="${url.context}/res/components/folder-details/folder-details-panel.css" />
   <@link rel="stylesheet" type="text/css" href="${url.context}/res/components/document-details/document-details-panel.css" />
   <@templateHtmlEditorAssets />
    <#include "/org/alfresco/components/form/form.css.ftl"/>
    <#include "/org/alfresco/components/form/form.js.ftl"/>
</@>

<#macro renderRegions regions>
    <#list regions as regionObj>
        <div class="cardlet"
            data-available-in-mobile="${regionObj.availableInMobile?string}"
            data-position-index-in-mobile="${regionObj.positionIndexInMobile?c}"
        ><@region id="${regionObj.regionId}" scope="page"/></div>
    </#list>
</#macro>

<@templateBody>
    <div id="alf-hd">
   <#include "/ru/citeck/include/header.ftl" />
    </div>
    <div id="bd">
        <@region id="actions-common" scope="template" />
        <div id="card-details-root"></div>
    </div>

    <@region id="html-upload" scope="template"/>
    <@region id="flash-upload" scope="template"/>
    <@region id="file-upload" scope="template"/>
    <@region id="dnd-upload" scope="template"/>
    <@region id="archive-and-download" scope="template"/>
    <@region id="doclib-custom" scope="template"/>
</@>

<@templateFooter>
    <div id="alf-ft">
        <@region id="footer" scope="global" />
    </div>

    <@relocateJavaScript/>

    <script type="text/javascript">
        require(['js/citeck/modules/card-details/card-details', 'react-dom', 'react'], function(comp, ReactDOM, React) {
            ReactDOM.render(
                React.createElement(comp.CardDetails, {
                    <#list page.url.args?keys as argKey>
                        "${argKey}":"${page.url.args[argKey]!}"<#if argKey_has_next>,</#if>
                    </#list>
                }),
                document.getElementById('card-details-root')
            );
        });
    </script>
</@>