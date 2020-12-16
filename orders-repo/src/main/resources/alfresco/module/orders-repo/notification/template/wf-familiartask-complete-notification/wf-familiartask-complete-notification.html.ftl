<html>
   <head>
      <style type="text/css"></style>
   </head>
   <body bgcolor="white">
        <div style="font-size: 14px; margin: 0px 0px 0px 0px; padding-top: 0px; border-top: 0px solid #aaaaaa;"> 
            Сотрудник <u>${editorName}</u> ознакомился с:
            <#if documentName?? >
                <div style="font-size: 14px; margin: 0px 0px 0px 0px; padding-top: 0px; border-top: 0px solid #aaaaaa;">
                    ${documentName}
                </div>
            </#if>
            <#if comment??>
                Замечания: ${comment}.
            </#if>
            <div style="font-size: 14px; margin: 0px 0px 0px 0px; padding-top: 10px; border-top: 0px solid #aaaaaa;">
                Ссылка на задачу ознакомления: 
                <#if documentId??>
                  <#assign taskUrl = web_url + "/v2/dashboard?recordRef=" + documentId />
                <#else>
                  <#assign taskUrl = web_url + "/v2/dashboard?recordRef=wftask@" + taskId />
                </#if>
                <a href="${taskUrl}">
                    <u>${taskUrl}</u>
                </a>
            </div>
        </div>
   </body>
</html>