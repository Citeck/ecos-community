package ru.citeck.ecos.node;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import ru.citeck.ecos.model.ThanksModel;
import ru.citeck.ecos.records2.RecordRef;

import javax.annotation.PostConstruct;

@Configuration
public class ThanksTypesConfiguration {

    @Autowired
    private EcosTypeService ecosTypeService;

    @PostConstruct
    public void init() {
        ecosTypeService.register(ThanksModel.TYPE_THANKS_TYPE, (info) -> getType( "th-thanks"));
    }

    private RecordRef getType(String typeId) {
        return RecordRef.create("emodel", "type", typeId);
    }
}
