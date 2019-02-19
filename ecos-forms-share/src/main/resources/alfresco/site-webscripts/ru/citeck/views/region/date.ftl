<#assign params = viewScope.region.params!{} />
<#assign mode   = params.mode!"browser" />
<#assign min    = params.min!"" />
<#assign max    = params.max!"" />

<#assign labels     = { "month" :  msg("date-unit.single.month"), "year" : msg("date-unit.single.year"), "header": msg("date.select") }>
<#assign buttons    = { "submit" :  msg("button.ok"), "cancel" : msg("button.cancel") }>
<#assign placeholder>
    <#if config.scoped["DateFormatMask"]?? && config.scoped["DateFormatMask"]["placeholder"]?? && config.scoped["DateFormatMask"]["placeholder"].value>
        ${config.scoped["DateFormatMask"]["placeholder"].value}
    <#else>
        ${msg("date.formatIE")}
    </#if>
</#assign>
<#assign months     = msg("months.short")>
<#assign days       = msg("days.short")>

<div id="${fieldId}-dateControl" data-bind='dateControl: value,
    mode: "${mode}",
    min: "${min}",
    max: "${max}",
    localization: {
        labels: { month: "${labels.month}", year: "${labels.year}", header: "${labels.header}" },
        buttons: { submit: "${buttons.submit}", cancel: "${buttons.cancel}" },
        months: "${months}",
        days: "${days}"
    }'>

    <input id="${fieldId}" type="date" placeholder="${placeholder?trim}" data-bind="value: ko.computed({
        read: function() {
            var result = value();
            if (result instanceof Date) {
                result = Alfresco.util.formatDate(result, 'yyyy-mm-dd');
            }
            return result;
        },
        write: function(newValue) {
            var newDate = new Date(newValue);
            if (newDate !== 'Invalid Date' && !isNaN(newDate) && (newDate > new Date('1000-01-01'))) {
                newValue = Alfresco.util.fromISO8601(newValue).toISOString();
                value(newValue);
            }
        }
    }), disable: protected">

    <a data-bind="visible: !protected()" id="${fieldId}-calendarAccessor" class="calendar-link-button hidden">
        <img src="/share/res/components/form/images/calendar.png" class="datepicker-icon">
    </a>

    <img data-bind="visible: protected" src="/share/res/components/form/images/calendar.png" class="datepicker-icon">
</div>