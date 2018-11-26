<#assign controlId = fieldId + "-createObjectControl">
<#assign params = viewScope.region.params!{} />
<#assign source = params.source!"create-variants" />
<#assign customType = params.customType!"" />
<#assign buttonTitle = params.buttonTitle!"button.create" />

<#assign constraint = params.constraint!"" />
<#assign constraintMessage = params.constraintMessage!"" />

<div id="${controlId}" class="create-object-control" data-bind="component: {
	name: 'createObjectButton',
	params: {
		<#if constraint?has_content>constraint: ${constraint},</#if>
		<#if constraintMessage?has_content>constraintMessage: '${constraintMessage}',</#if>

		scope: $data,
		source: '${source}',
		customType: '${customType}',
		id: '${controlId}',
		buttonTitle: '${msg(buttonTitle)}',
		parentRuntime: $root.key(),
		virtualParent: ${((params.virtualParent!"false") == "true")?string},
		baseRef: $parents[1].resolve('node.impl.nodeRef') || '',
		rootAttributeName: <#if globalAttributeName??>'${globalAttributeName}'<#else>null</#if>
	}
}" ></div>