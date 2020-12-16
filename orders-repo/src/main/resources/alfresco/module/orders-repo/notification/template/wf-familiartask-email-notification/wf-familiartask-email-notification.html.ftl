<html>
   <head>
      <style type="text/css"></style>
   </head>
   <body bgcolor="white">
        <div style="font-size: 14px; margin: 0px 0px 0px 0px; padding-top: 0px; border-top: 0px solid #aaaaaa;"> 
            <div style="font-size: 14px; margin: 0px 0px 0px 0px; padding-top: 0px; border-top: 0px solid #aaaaaa;"> 
            <#if documents?size != 0>
                <#list documents as doc>
                    Вам необходимо ознакомиться с документом ${doc.name!"б/н"}
                </#list>
            </#if>                   
            <#if (description)??>
                , сообщение: "${description}".
            </#if>
            </div>
            <div style="font-size: 14px; margin: 0px 0px 0px 0px; padding-top: 10px; border-top: 0px solid #aaaaaa;"> 
                Перейти к заданию можно по ссылке: 
                <#assign taskUrl = web_url + "/v2/dashboard?recordRef=wftask@" + taskId />
                <a href="${taskUrl}">
                    <u>${taskUrl}</u>
                </a>
            </div>
        </div>
   </body>
</html>