<@markup id="js">
    <@script type="text/javascript" src="${url.context}/res/citeck/components/barcode/barcode.js" group="barcode" />
</@>

<#assign id=args.htmlid?js_string />
<#assign viewScale = args.viewScale!'3.0' />
<#assign printScale = args.printScale!'1.5' />
<#assign viewMargins = args.viewMargins!'10,10,10,10' />
<#assign printMargins = args.printMargins!'10,10,10,10' />
<#assign viewFormat = args.viewFormat!'png' />
<#assign displayNonPdfVIew = args.displayNonPdfVIew!'false' />
<#assign barcodeType = args.barcodeType!'code-128' />
<#assign width = args.width!'' />
<#assign height = args.height!'' />
<#assign rnd = args.rnd!'0' />
<#assign barcodeURL = "${url.context}/proxy/alfresco/citeck/print/barcode?nodeRef=${args.nodeRef}&property=${args.property}&barcodeType=${barcodeType}" />

<script type="text/javascript">
    new Citeck.widget.Barcode("${id}-panel").setOptions({
        nodeRef: "${args.nodeRef}",
        property: "${args.property}",
        barcodeType: "${barcodeType}",
        header: "${args.header}",
        width: "${width}",
        height: "${height}",
        printScale: "${printScale}",
        printMargins: "${printMargins}",
        rnd: Math.random(),
        displayNonPdfVIew: "${displayNonPdfVIew}"
    });
</script>

<div id="${id}-panel" class="child-forms document-details-panel">
    <h2 id="${id}-heading" class="thin dark">
    ${msg("${args.header!}")}
        <span id="${id}-heading-actions" class="alfresco-twister-actions" style="position:relative;float:right;">
			<a class="print" href="${barcodeURL?html}&scale=${printScale}&margins=${printMargins}&print=true"
               target="_blank"><img src="${url.context}/res/components/images/printer-16.png"/></a>
		</span>
    </h2>
    <div class="panel-body">
		<#if viewFormat == 'pdf'>
            <embed src="${barcodeURL?html}&output=${viewFormat}&scale=${viewScale}&margins=${viewMargins}&rnd=${rnd}"></embed>
        <#elseif displayNonPdfVIew == 'true'>
			<img src="${barcodeURL?html}&output=${viewFormat}&scale=${viewScale}&margins=${viewMargins}&rnd=${rnd}"/>
        </#if>
    </div>
    <script type="text/javascript">//<![CDATA[
    Alfresco.util.createTwister("${id}-heading", "${args.twisterKey!'barcode'}");
    //]]></script>
</div>
