package ru.citeck.ecos.action.group;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.extensions.surf.util.I18NUtil;
import ru.citeck.ecos.action.group.impl.CustomTxnGroupAction;
import ru.citeck.ecos.action.group.impl.GroupActionExecutor;
import ru.citeck.ecos.action.group.impl.GroupActionExecutorFactory;
import ru.citeck.ecos.repo.RemoteRef;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Pavel Simonov
 */
public class GroupActionServiceImpl implements GroupActionService {

    private static final String ALREADY_RUNNING_MSG = "The action is already running. " +
                                                      "You can not start several identical actions!";

    private TransactionService transactionService;

    private Map<String, GroupActionFactory> processorFactories = new HashMap<>();

    private Set<ActionExecution> activeActions;

    @Autowired
    public GroupActionServiceImpl(TransactionService transactionService) {
        this.transactionService = transactionService;
        activeActions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    }

    private List<ActionResult> executeImpl(ActionExecution execution) {
        if (activeActions.add(execution)) {
            try {
                return execution.run();
            } finally {
                activeActions.remove(execution);
            }
        } else {
            throw new IllegalStateException(ALREADY_RUNNING_MSG);
        }
    }

    @Override
    public List<ActionResult> execute(Iterable<RemoteRef> nodes, GroupAction action) {

        String author = AuthenticationUtil.getFullyAuthenticatedUser();
        ActionExecution execution = new ActionExecution(nodes, action, author);

        if (activeActions.contains(execution)) {
            throw new IllegalStateException(ALREADY_RUNNING_MSG);
        }

        if (action.isAsync()) {

            final String currentUser = AuthenticationUtil.getFullyAuthenticatedUser();
            final Locale locale = I18NUtil.getLocale();

            RetryingTransactionHelper txnHelper = transactionService.getRetryingTransactionHelper();

            AlfrescoTransactionSupport.bindListener(new TransactionListenerAdapter() {
                @Override
                public void afterCommit() {
                    new Thread(() -> txnHelper.doInTransaction(() -> {

                        AuthenticationUtil.setRunAsUser(currentUser);
                        I18NUtil.setLocale(locale);

                        return executeImpl(execution);

                    }, false)).start();
                }
            });

            return Collections.emptyList();

        } else {

            return executeImpl(execution);
        }
    }

    @Override
    public List<ActionResult> execute(Iterable<RemoteRef> nodes,
                                      Consumer<RemoteRef> action,
                                      GroupActionConfig config) {

        return execute(nodes, new CustomTxnGroupAction(transactionService, action, config));
    }

    @Override
    public List<ActionResult> execute(Iterable<RemoteRef> nodes,
                                      Function<RemoteRef, ActionStatus> action,
                                      GroupActionConfig config) {

        return execute(nodes, new CustomTxnGroupAction(transactionService, action, config));
    }

    @Override
    public List<ActionResult> execute(Iterable<RemoteRef> nodes,
                                      String actionId,
                                      GroupActionConfig config) {

        return execute(nodes, getProcessor(actionId, config));
    }

    private GroupAction getProcessor(String actionId, GroupActionConfig config) {

        GroupActionFactory factory = processorFactories.get(actionId);
        if (factory == null) {
            throw new IllegalArgumentException("Action not found: '" + actionId + "'");
        }

        checkParams(config.getParams(), factory.getMandatoryParams());

        return factory.createAction(config);
    }

    private void checkParams(Map<String, String> params, String[] mandatoryParams) {
        List<String> missing = new ArrayList<>(mandatoryParams.length);
        for (String param : mandatoryParams) {
            if (!params.containsKey(param) || StringUtils.isBlank(params.get(param))) {
                missing.add(param);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Mandatory parameters are missing: " + String.join(", ", missing));
        }
    }

    @Override
    public void cancelActions() {
        for (ActionExecution execution : activeActions) {
            execution.cancel();
        }
        activeActions.clear();
    }

    @Override
    public List<ActionExecution> getActiveActions() {
        return new ArrayList<>(activeActions);
    }

    @Override
    public void register(GroupActionFactory factory) {
        processorFactories.put(factory.getActionId(), factory);
    }

    @Override
    public void register(GroupActionExecutor executor) {
        register(new GroupActionExecutorFactory(executor, transactionService));
    }
}