<import resource="classpath:alfresco/module/idocs-repo/scripts/import-nodes.lib.js">

var type = '{http://www.citeck.ru/model/icase/1.0}caseStatus';
var destination = 'app:company_home/app:dictionary/cm:dataLists/cm:case-status';

var data = [
    {type: type, props: {
        'cm:name': 'draft',
        'cm:title': {'en': 'Draft', 'ru': '\u0427\u0435\u0440\u043D\u043E\u0432\u0438\u043A'}
    }},
    {type: type, props: {
        'cm:name': 'new',
        'cm:title': {'en': 'New', 'ru': '\u041D\u043E\u0432\u044B\u0439'}
    }},
    {type: type, props: {
        'cm:name': 'ecos-process-start-error',
        'cm:title': {'en': 'Error while starting the process', 'ru': '\u041E\u0448\u0438\u0431\u043A\u0430 \u043F\u0440\u0438 \u0441\u0442\u0430\u0440\u0442\u0435 \u043F\u0440\u043E\u0446\u0435\u0441\u0441\u0430'}
    }}
];

importNodes(destination, data);