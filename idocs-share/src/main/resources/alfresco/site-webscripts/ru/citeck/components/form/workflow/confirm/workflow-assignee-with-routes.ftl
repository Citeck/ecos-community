<div class="set">
	<div class="set-title">${msg("workflow.set.assignee")}</div>

	<@forms.renderField field = "assoc_wfcf_confirmers" extension = {
		"mandatory": true,
		"control" : {
			"template" : "/ru/citeck/components/form/controls/route.ftl",
			"params" : {
				"participantListFieldId": "assoc_wfcf_confirmers",
				"priorityFieldId": "prop_wfcf_precedence",

				"saveAndLoadTemplate": "true",
				
				"presetTemplate": "",
				"presetTemplateMandatory": "true",

				"allowedAuthorityType": "USER, GROUP",
				"allowedGroupType": "ROLE, BRANCH"
			}
		}
	 } />

	 <script type="text/javascript">
		YAHOO.Bubbling.on("formValueChanged", function(layer, args) {
			if (args[1] && args[1].selectedItems) {
				var selectedItems = args[1].selectedItems;

					citeckWidgetRoute.saveAndLoadTemplateVisibility("visible");
			}
      
      YAHOO.Bubbling.fire("mandatoryControlValueUpdated", this);
		});
	 </script>
</div>
