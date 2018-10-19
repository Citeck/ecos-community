
<script type="text/javascript">//<![CDATA[
require(['citeck/components/form/controls/js-variable-value'], function () {

    new Citeck.JSVariableField("${fieldHtmlId}").setOptions({
    <#if field.control.params.variableName??>
        variableName: "${field.control.params.variableName!""}",
    </#if>
        formMode: "${form.mode}"
    });

});
//]]></script>


<div class="form-field">
    <input id="${fieldHtmlId}" type="hidden" name="${field.name}" />
</div>