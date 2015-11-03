try {
	var task = workflow.getTask(args['taskId']);
	var prop = args['propName'].replace('prop_', '').replace('_', ':');
	relatedWorkflows = task.getProperties()[prop];
	if (relatedWorkflows == undefined)
		relatedWorkflows = "";
	model.workflows = [];
	for each(id in relatedWorkflows.split(','))
		if (id != ''){
			var inst = workflow.getInstance(id);
            if (!inst) {
                continue;
            }
			var workflowStatus = '';
			if (!inst.isActive())
				workflowStatus = 'complete';
			else
			{
				var paths = inst.getPaths();
				if (paths.length == 1 && paths[0].getNode().getType() == 'StartState')
					workflowStatus = 'awaiting';
				else
					workflowStatus = 'in-progress';
			}
			var desc = String(inst.getDescription());

			var start_date = inst.getStartDate();
			var start_date_string = null;
			if(start_date != null) {
				var s_year    = start_date.getUTCFullYear();
				var s_month   = start_date.getUTCMonth();
				var s_day     = start_date.getUTCDate();
				var s_hours   = start_date.getUTCHours();
				var s_minutes = start_date.getUTCMinutes();
				start_date_string = s_year + "-" + s_month + "-" + s_day + "-" + s_hours + "-" + s_minutes;
			}
			start_date_string = String(start_date_string);

			var end_date = inst.getEndDate();
			var end_date_string = null;
			if(end_date != null) {
				var e_year    = end_date.getUTCFullYear();
				var e_month   = end_date.getUTCMonth();
				var e_day     = end_date.getUTCDate();
				var e_hours   = end_date.getUTCHours();
				var e_minutes = end_date.getUTCMinutes();
				end_date_string = e_year + "-" + e_month + "-" + e_day + "-" + e_hours + "-" + e_minutes;
			}
			end_date_string = String(end_date_string);

			model.workflows.push({
				'id': id,
				'status': workflowStatus,
				'active': String(workflowStatus != 'complete'),
				'description': desc,
				'start_date': start_date_string, 
				'end_date': end_date_string 
			});
		}
	model.code = 200;			
} catch (ex) {
	model.code = 500;
	model.message = ex.message;
}
