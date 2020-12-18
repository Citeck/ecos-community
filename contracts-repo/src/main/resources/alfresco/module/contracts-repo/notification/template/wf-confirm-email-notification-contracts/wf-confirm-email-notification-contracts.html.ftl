<html>
<head>
    <style type="text/css"></style>
</head>
<body bgcolor="white">
    <div style="font-size: 14px; margin: 0px 0px 0px 0px; padding-top: 0px; border-top: 0px solid #aaaaaa;">
        <#if documentType??>
            <div style="font-size: 14px; margin: 0px 0px 0px 0px; padding-top: 0px; border-top: 0px solid #aaaaaa;">
                <#if documentType == "contracts:agreement">
                    <p> Договор №: <u>${documentAgreementNumber!"б-н"}</u> был рассмотрен и согласован. <br></p>
                <#elseif documentType == "contracts:supplementaryAgreement">
                    <p> Доп.соглашение №: <u>${documentAgreementNumber!"б-н"}</u> было рассмотрено и согласовано. <br></p>
                <#else>
                    <p>Завершено согласование документа.</p>
                </#if>
             </div>
        </#if>


        <#if documentId??>
            <#assign documentUrl = web_url + "/v2/dashboard?recordRef=" + documentId />
            <div style="font-size: 14px; margin: 0px 0px 0px 0px; padding-top: 10px; border-top: 0px solid #aaaaaa;">
                Перейти к документу можно по ссылке: <a href="${documentUrl}"><u>${documentUrl}</u></a>
            </div>
        </#if>

        <#if taskId??>
            <#assign taskUrl = web_url + "/v2/dashboard?recordRef=wftask@" + taskId />
            <div style="font-size: 14px; margin: 0px 0px 0px 0px; padding-top: 10px; border-top: 0px solid #aaaaaa;">
                Сведения о процессе: <a href="${taskUrl}"><u>${taskUrl}</u></a>
            </div>
        </#if>

    </div>
</body>
</html>