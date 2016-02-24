<#import "journals.lib.ftl" as journals />
<#escape x as jsonUtils.encodeJSONString(x)>
{
    "id": "${args.journalsList}",
    "title": <#if journalList??>"${journalList.properties["cm:title"]!}"<#else>""</#if>,

    <#if journalByDefault??>
        "default": <@journals.renderJournal journal=journalByDefault full=false />,
    </#if>

    "journals": [
        <#if args.nodeRef?? && journalsWithNode??>
            <#list journalsWithNode as jwn>
                {
                    <#if jwn.criteria??>
                        "criteria": [
                            <#list jwn.criteria as criterion>
                                <@journals.renderCriterion criterion=criterion /><#if criterion_has_next>,</#if>
                            </#list>
                        ],
                    </#if>
                    <#if jwn.journal??>
                        "nodeRef": "${jwn.journal.nodeRef}",
                        "title": "${jwn.journal.properties["cm:title"]!}",
                        "type": "${jwn.journal.properties["journal:journalType"]}"
                    </#if>
                }
                <#if jwn_has_next>,</#if>
            </#list>
        <#else>
            <#if journalsInList??>
                <#list journalsInList as journal>
                    <@journals.renderJournal journal=journal full=false/>
                    <#if journal_has_next>,</#if>
                </#list>
            </#if>
        </#if>
    ]
}
</#escape>