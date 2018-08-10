package ru.citeck.ecos.webscripts.cardlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.webscripts.*;
import ru.citeck.ecos.cardlet.CardletService;
import ru.citeck.ecos.cardlet.xml.*;
import ru.citeck.ecos.model.CardletModel;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CardletsGet extends AbstractWebScript {

    //========PARAMS========
    private static final String PARAM_MODE_ID = "mode";
    private static final String PARAM_NODEREF = "nodeRef";
    //=======/PARAMS========

    private static final String DEFAULT_MODE = "default";

    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private CardletService cardletService;
    @Autowired
    private NodeService nodeService;

    @Override
    public void execute(WebScriptRequest req, WebScriptResponse res) throws IOException {

        NodeRef nodeRef = new NodeRef(req.getParameter(PARAM_NODEREF));
        String mode = req.getParameter(PARAM_MODE_ID);

        List<Cardlet> cardlets = cardletService.queryCardlets(nodeRef, mode);
        List<NodeRef> cardModes = cardletService.queryCardModes(nodeRef);

        res.setContentType(Format.JSON.mimetype() + ";charset=UTF-8");
        objectMapper.writeValue(res.getOutputStream(), new Response(cardlets, cardModes));

        res.setStatus(Status.STATUS_OK);
    }

    public class Response {

        public List<RespCardMode> cardModes;
        public List<RespCardlet> cardlets;

        public Response(List<Cardlet> cardlets, List<NodeRef> cardModes) {
            this.cardModes = cardModes.stream().map(RespCardMode::new).collect(Collectors.toList());
            this.cardlets = cardlets.stream().map(RespCardlet::new).collect(Collectors.toList());
        }
    }

    public class RespCardlet {

        public final String id;
        public final int mobileOrder;
        public final String column;
        public final String order;
        public final RespControl control;
        public final String cardMode;

        public RespCardlet(Cardlet cardlet) {
            id = cardlet.getId();
            String mode = cardlet.getPosition().getCardMode();
            cardMode = StringUtils.isNotBlank(mode) ? mode : DEFAULT_MODE;
            order = cardlet.getPosition().getOrder();
            mobileOrder = cardlet.getPosition().getMobileOrder().intValue();
            column = cardlet.getPosition().getColumn().value();
            control = new RespControl(cardlet.getControl());
        }
    }

    public class RespCardMode {

        public final String id;
        public final String title;
        public final String description;
        public final String order;

        RespCardMode(NodeRef mode) {

            Map<QName, Serializable> props = nodeService.getProperties(mode);

            id = (String) props.get(CardletModel.PROP_CARD_MODE_ID);
            order = (String) props.get(CardletModel.PROP_CARD_MODE_ORDER);
            title = (String) props.get(ContentModel.PROP_TITLE);
            description = (String) props.get(ContentModel.PROP_DESCRIPTION);
         }
    }

    public class RespControl {

        public final String url;
        public final Map<String, String> props = new HashMap<>();

        RespControl(Control control) {
            url = control.getUrl();
            for (Property prop : control.getProp()) {
                props.put(prop.getName(), prop.getValue());
            }
        }
    }
}

