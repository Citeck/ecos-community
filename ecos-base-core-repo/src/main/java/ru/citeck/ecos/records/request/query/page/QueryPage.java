package ru.citeck.ecos.records.request.query.page;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.JsonNode;
import ru.citeck.ecos.records.RecordRef;

public abstract class QueryPage {

    private static final String SKIP_COUNT_FIELD = "skipCount";
    private static final String MAX_ITEMS_FIELD = "maxItems";
    private static final String AFTER_ID_FIELD = "afterId";

    private final int maxItems;

    QueryPage() {
        maxItems = -1;
    }

    QueryPage(Integer maxItems) {
        this.maxItems = maxItems != null ? maxItems : -1;
    }

    public int getMaxItems() {
        return maxItems;
    }

    public abstract QueryPage withMaxItems(Integer maxItems);

    @JsonCreator
    public static QueryPage createPage(JsonNode pageData) {

        Integer maxItems = getInt(pageData, MAX_ITEMS_FIELD);

        if (pageData.has(AFTER_ID_FIELD)) {
            JsonNode afterIdNode = pageData.path(AFTER_ID_FIELD);
            return new AfterPage(RecordRef.valueOf(afterIdNode.textValue()), maxItems);
        } else {
            return new SkipPage(getInt(pageData, SKIP_COUNT_FIELD), maxItems);
        }
    }

    private static Integer getInt(JsonNode node, String field) {
        JsonNode intNode = node.path(field);
        return intNode.canConvertToInt() ? intNode.intValue() : null;
    }
}