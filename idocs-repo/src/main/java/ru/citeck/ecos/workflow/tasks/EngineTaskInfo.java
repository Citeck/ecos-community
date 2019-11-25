package ru.citeck.ecos.workflow.tasks;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import ru.citeck.ecos.records2.RecordRef;

import java.util.List;
import java.util.Map;

public class EngineTaskInfo implements TaskInfo {

    private final String engineId;
    private final TaskInfo info;

    EngineTaskInfo(String engineId, TaskInfo localInfo) {
        this.engineId = engineId;
        this.info = localInfo;
    }

    @Override
    public String getTitle() {
        return AuthenticationUtil.runAsSystem(info::getTitle);
    }

    @Override
    public String getId() {
        return engineId + "$" + info.getId();
    }

    @Override
    public String getAssignee() {
        return AuthenticationUtil.runAsSystem(info::getAssignee);
    }

    @Override
    public String getCandidate() {
        return AuthenticationUtil.runAsSystem(info::getCandidate);
    }

    @Override
    public List<String> getActors() {
        return AuthenticationUtil.runAsSystem(info::getActors);
    }

    @Override
    public String getFormKey() {
        return AuthenticationUtil.runAsSystem(info::getFormKey);
    }

    @Override
    public Map<String, Object> getAttributes() {
        return AuthenticationUtil.runAsSystem(info::getAttributes);
    }

    @Override
    public Map<String, Object> getLocalAttributes() {
        return AuthenticationUtil.runAsSystem(info::getLocalAttributes);
    }

    @Override
    public RecordRef getDocument() {
        return AuthenticationUtil.runAsSystem(info::getDocument);
    }

    @Override
    public Object getAttribute(String name) {
        return AuthenticationUtil.runAsSystem(() -> info.getAttribute(name));
    }
}