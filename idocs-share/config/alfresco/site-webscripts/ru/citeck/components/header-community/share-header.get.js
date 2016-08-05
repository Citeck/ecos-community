<import resource="classpath:/alfresco/site-webscripts/org/alfresco/share/imports/share-header.lib.js">
<import resource="classpath:/alfresco/site-webscripts/ru/citeck/components/header-community/share-header.lib.js">

// ---------------------
// HEADER MENU
// ---------------------

var appMenu = getWidget("HEADER_APP_MENU_BAR"),
    userMenu = getWidget("HEADER_USER_MENU_BAR"),
    currentSite = page.url.templateArgs.site || "";

// USER MENU
var userMenuItems = [
  {
     id: "HEADER_USER_MENU_STATUS",
     name: "alfresco/header/CurrentUserStatus"
  },

  {
     id: "HEADER_USER_MENU_MY_PROFILE",
     name: "alfresco/header/AlfMenuItem",
     config:
     {
        label: "header.my-profile.label",
        iconImage: "/share/res/components/images/header/my-profile.png",
        targetUrl: "user/" + encodeURIComponent(user.name) + "/profile"
     }
  }
];

if (user.properties.available === true || user.properties.available === null) {
  userMenuItems.push({
     id: "HEADER_USER_MENU_MAKE_NOTAVAILABLE",
     name: "alfresco/header/AlfMenuItem",
     config:
     {
        label: "header.make-notavailable.label",
        iconImage: "/share/res/components/images/header/make-notavailable.png",
        targetUrl: "/components/delegate/make-available?available=false"
     }
  });
} else if (user.properties.available === false) {
  userMenuItems.push({
     id: "HEADER_USER_MENU_MAKE_AVAILABLE",
     name: "alfresco/header/AlfMenuItem",
     config:
     {
        label: "header.make-available.label",
        iconImage: "/share/res/components/images/header/make-available.png",
        targetUrl: "/components/delegate/make-available?available=true"
     }
  });
}

if (user.capabilities.isMutable) {
   userMenuItems.push({
      id: "HEADER_USER_MENU_PASSWORD",
      name: "alfresco/header/AlfMenuItem",
      config:
      {
         label: "header.change-password.label",
         iconImage: "/share/res/components/images/header/change-password.png",
         targetUrl: "user/" + encodeURIComponent(user.name) + "/change-password"
      }
   });
}

if (!context.externalAuthentication) {
   userMenuItems.push({
      id: "HEADER_USER_MENU_LOGOUT",
      name: "alfresco/header/AlfMenuItem",
      config:
      {
         label: "header.logout.label",
         iconImage: "/share/res/components/images/header/logout.png",
         targetUrl: "dologout"
      }
   });
}

userMenu.config.widgets = [
  {
     id: "HEADER_USER_MENU_POPUP",
     name: "alfresco/header/AlfMenuBarPopup",
     config: {
        label: user.fullName,
        widgets: [
           {
              id: "HEADER_USER_MENU",
              name: "alfresco/menus/AlfMenuGroup",
              config: {
                 widgets: userMenuItems
              }
           }
        ]
     }
  }
];


// APP MENU
appMenu.config.widgets = [
  {
     id: "HEADER_HOME",
     name: "alfresco/menus/AlfMenuBarItem",
     config: {
        label: "header.menu.home.label",
        targetUrl: "user/" + encodeURIComponent(user.name) + "/dashboard",
        targetUrlType: "SHARE_PAGE_RELATIVE"
     }
  },

  {
     id: "HEADER_SITES_MENU",
     name: "alfresco/wrapped/HeaderJsWrapper",
     config: {
        id: "sites-global_x002e_share-header_x0023_default_sites",
        label: "header.menu.sites.label",
        itemId: "sites-global_x002e_share-header_x0023_default_sites",
        objectToInstantiate: "Alfresco.module.AllSites",
        currentSite: currentSite,
        currentUser: user.name
     }
  },

  {
     id: "HEADER_CREATE_VARIANTS",
     name: "alfresco/wrapped/HeaderJsWrapper",
     config: {
        id: "create-variants-global_x002e_share-header_x0023_default_create-variants",
        label: "header.create-variants.label",
        itemId: "create-variants-global_x002e_share-header_x0023_default_create-variants",
        objectToInstantiate: "Citeck.module.CreateVariants",
        currentSite: currentSite,
        currentUser: user.name
     }
  },

  {
     id: "HEADER_JOURNALS",
     name: "alfresco/menus/AlfMenuBarItem",
     config: {
        label: "header.journals.label",
        targetUrl: buildSiteUrl(currentSite) + "journals2/list/main",
        movable: { minWidth: 1089 }
     }
  },

  {
     id: "HEADER_DOCUMENTLIBRARY",
     name: "alfresco/menus/AlfMenuBarItem",
     config: {
        label: "header.documentlibrary.label",
        targetUrl: buildSiteUrl(currentSite) + "documentlibrary",
        movable: { minWidth: 1171 }
     }
  },

  {
     id: "HEADER_CREATE_WORKFLOW",
     name: "alfresco/header/AlfMenuBarPopup",
     config: {
        label: "header.create-workflow.label",
        widgets: [
          {
            id: "HEADER_CREATE_WORKFLOW_VARIANTS",
            name: "alfresco/menus/AlfMenuGroup",
            config: {
              widgets: [
                {
                   id: "HEADER_CREATE_WORKFLOW_ADHOC",
                   name: "alfresco/menus/AlfMenuItem",
                   config: {
                      label: "header.create-workflow-adhoc.label",
                      targetUrl: "start-specified-workflow?workflowId=activiti$perform"
                   }
                },
                {
                   id: "HEADER_CREATE_WORKFLOW_CONFIRM",
                   name: "alfresco/menus/AlfMenuItem",
                   config: {
                      label: "header.create-workflow-confirm.label",
                      targetUrl: "start-specified-workflow?workflowId=activiti$confirm"
                   }
                }
              ]
            }
          }
        ]
     }
  },

  buildMorePopup()
];

// DEBUG MENU
if (config.global.flags.getChildValue("client-debug") == "true") {
  var loggingEnabled = false,
       allEnabled     = false,
       warnEnabled    = false,
       errorEnabled   = false;

  if (userPreferences &&
      userPreferences.org &&
      userPreferences.org.alfresco &&
      userPreferences.org.alfresco.share &&
      userPreferences.org.alfresco.share.logging) {

      var loggingPreferences = userPreferences.org.alfresco.share.logging;
      loggingEnabled = loggingPreferences.enabled && true;
      allEnabled = (loggingPreferences.all != null) ?  loggingPreferences.all : false;
      warnEnabled = (loggingPreferences.warn != null) ?  loggingPreferences.warn : false;
      errorEnabled = (loggingPreferences.error != null) ?  loggingPreferences.error : false;
   }

   var loggingWidget = {
      name: "alfresco/header/AlfMenuBarPopup",
      config: {
         label: "Debug Menu",
         widgets: [
            {
               name: "alfresco/menus/AlfMenuGroup",
               config: {
                  label: "Quick Settings",
                  widgets: [
                     {
                        name: "alfresco/menus/AlfCheckableMenuItem",
                        config: {
                           label: "Debug Logging",
                           value: "enabled",
                           publishTopic: "ALF_LOGGING_STATUS_CHANGE",
                           checked: loggingEnabled
                        }
                     },
                     {
                        name: "alfresco/menus/AlfCheckableMenuItem",
                        config: {
                           label: "Show All Logs",
                           value: "all",
                           publishTopic: "ALF_LOGGING_STATUS_CHANGE",
                           checked: allEnabled
                        }
                     },
                     {
                        name: "alfresco/menus/AlfCheckableMenuItem",
                        config: {
                           label: "Show Warning Messages",
                           value: "warn",
                           publishTopic: "ALF_LOGGING_STATUS_CHANGE",
                           checked: warnEnabled
                        }
                     },
                     {
                        name: "alfresco/menus/AlfCheckableMenuItem",
                        config: {
                           label: "Show Error Messages",
                           value: "error",
                           publishTopic: "ALF_LOGGING_STATUS_CHANGE",
                           checked: errorEnabled
                        }
                     },

                  ]
               }
            },
            {
               name: "alfresco/menus/AlfMenuGroup",
               config: {
                  label: "Logging Configuration",
                  widgets: [
                     {
                        name: "alfresco/menus/AlfMenuItem",
                        config: {
                           label: "Update Logging Preferences",
                           publishTopic: "ALF_UPDATE_LOGGING_PREFERENCES"
                        }
                     }
                  ]
               }
            }
         ]
      }
   };

   appMenu.config.widgets.push(loggingWidget);
}


// ---------------------
// TITLE MENU
// ---------------------

var siteConfig = getWidget("HEADER_SITE_CONFIGURATION_DROPDOWN"),
    siteData = getSiteData();

if (siteConfig && siteData.userIsSiteManager) {
  if (!page.titleId && !hasWidget("HEADER_CUSTOMIZE_SITE_DASHBOARD")) {
    siteConfig.config.widgets.splice(0, 0, {
      id: "HEADER_CUSTOMIZE_SITE_DASHBOARD",
      name: "alfresco/menus/AlfMenuItem",
      config: {
        label: "customize_dashboard.label",
        iconClass: "alf-cog-icon",
        targetUrl: "site/" + page.url.templateArgs.site + "/customise-site-dashboard"
      }
    });
  }
}


// ---------------------
// FUNCTIONS
// ---------------------

function buildMorePopup() {
  var groups = [ buildMovableGroup(), buildMyGroup() ];
  if (user.isAdmin) groups.push(buildAdminGroup());

  return {
     id: "HEADER_MORE",
     name: "alfresco/header/AlfMenuBarPopup",
     config: {
        label: "header.more.label",
        widgets: groups
     }
  }
};

function buildMyGroup() {
  return {
    id: "HEADER_MORE_MY_GROUP",
    name: "alfresco/menus/AlfMenuGroup",
    config: {
      label: "header.my.label",
      widgets: buildItems([
        { id: "task-journals", url: "journals2/list/tasks", iconImage: "/share/res/components/images/header/my-tasks.png" },
        { id: "my-workflows", url: "my-workflows", iconImage: "/share/res/components/images/header/my-workflows.png" },
        { id: "completed-workflows", url: "completed-workflows#paging=%7C&filter=workflows%7Call", iconImage: "/share/res/components/images/header/completed-workflows.png" },
        { id: "my-content", url: "user/user-content", iconImage: "/share/res/components/images/header/my-content.png" },
        { id: "my-sites", url: "user/user-sites", iconImage: "/share/res/components/images/header/my-sites.png" },
        { id: "my-profile", url: "user/" + encodeURIComponent(user.name) + "/profile", iconImage: "/share/res/components/images/header/my-profile.png" },
        { id: "my-files", url: "context/mine/myfiles", iconImage: "/share/res/components/images/header/my-content.png" },
        { id: "global_journals2", url: "journals2/list/main", iconImage: "/share/res/components/images/header/journals.png" }
      ], "my")
    }
  };
};

function buildMovableGroup() {
  return {
    id: "HEADER_MORE_MOVABLE_GROUP",
    name: "alfresco/menus/AlfMenuGroup",
    config: {
      label: "",
      movable: { maxWidth: 1170 },
      widgets: buildItems([
        {
          id: "journals",
          url: buildSiteUrl(currentSite) + "journals2/list/main",
          movable: { maxWidth: 1080 }
        },
        {
          id: "documentlibrary", url: buildSiteUrl(currentSite) + "documentlibrary",
          movable: { maxWidth: 1170 }
        }
      ], "movable")
    }
  };
};

function buildAdminGroup() {
  return {
    id: "HEADER_MORE_TOOLS_GROUP",
    name: "alfresco/menus/AlfMenuGroup",
    config: {
      label: "header.tools.label",
      widgets: buildItems([
        { id: "repository", url: "repository", iconImage: "/share/res/components/images/header/repository.png" },
        { id: "application", url: "console/admin-console/application", iconImage: "/share/res/components/images/header/application.png" },
        { id: "groups", url: "console/admin-console/groups", iconImage: "/share/res/components/images/header/groups.png" },
        { id: "users", url: "console/admin-console/users", iconImage: "/share/res/components/images/header/users.png" },
        { id: "categories", url: "console/admin-console/type-manager", iconImage: "/share/res/components/images/header/category-manager.png" },
        { id: "system", url: "journals2/list/system", iconImage: "/share/res/components/images/header/journals.png" },
        { id: "meta_journals", url: "journals2/list/meta", iconImage: "/share/res/components/images/header/journals.png" },
        { id: "templates", url: "journals2/list/templates", iconImage: "/share/res/components/images/header/templates.png" },
        { id: "orgstruct", url: "orgstruct", iconImage: "/share/res/components/images/header/orgstruct.png" },
        { id: "more", url: "console/admin-console/", iconImage: "/share/res/components/images/header/more.png" }
      ], "tools")
    }
  };
};

function buildSiteUrl(sitename) {
  return sitename ? "site/" + encodeURIComponent(sitename) + "/" : "";
};

function buildItems(items, groupName) {
  var result = [];
  for (var i = 0; i < items.length; i++) {
    var configuration = {
      label: "header." + items[i].id + ".label",
      targetUrl: items[i].url
    };

    if (items[i].iconImage) configuration["iconImage"] = items[i].iconImage;
    if (items[i].movable) configuration["movable"] = items[i].movable;

    result.push({
      id: ("HEADER_MORE_" + groupName + "_" + items[i].id).toUpperCase(),
      name: "alfresco/menus/AlfMenuItem",
      config: configuration
    });
  }

  return result;
};
