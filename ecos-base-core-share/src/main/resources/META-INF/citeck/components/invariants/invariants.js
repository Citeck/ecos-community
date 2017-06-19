/*
 * Copyright (C) 2008-2017 Citeck LLC.
 *
 * This file is part of Citeck EcoS
 *
 * Citeck EcoS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Citeck EcoS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Citeck EcoS. If not, see <http://www.gnu.org/licenses/>.
 */
define(['lib/knockout', 'citeck/utils/knockout.utils', 'lib/moment'], function(ko, koutils, moment) {

    var logger = Alfresco.logger,
        koclass = koutils.koclass,
        $isNodeRef = Citeck.utils.isNodeRef,
        $isFilename = Citeck.utils.isFilename;

    var s = String,
        n = Number,
        b = Boolean,
        d = Date,
        o = Object,
        InvariantScope = koclass('invariants.InvariantScope'),
        Invariant = koclass('invariants.Invariant'),
        InvariantSet = koclass('invariants.InvariantSet'),
        ExplicitInvariantSet = koclass('invariants.ExplicitInvariantSet', InvariantSet),
        GroupedInvariantSet = koclass('invariants.GroupedInvariantSet', InvariantSet),
        ClassInvariantSet = koclass('invariants.ClassInvariantSet', InvariantSet),
        MultiClassInvariantSet = koclass('invariants.MultiClassInvariantSet', InvariantSet),
        DefaultModel = koclass('invariants.DefaultModel'),
        Message = koclass('invariants.Message'),
        Feature = koclass('invariants.Feature'),
        AttributeSet = koclass('invariants.AttributeSet'),
        AttributeInfo = koclass('invariants.AttributeInfo'),
        Attribute = koclass('invariants.Attribute'),
        Predicate = koclass('invariants.Predicate'),
        Node = koclass('invariants.Node'),
        NodeImpl = koclass('invariants.NodeImpl'),
        QName = koclass('invariants.QName'),
        Content = koclass('invariants.Content'),
        ContentFileImpl = koclass('invariants.ContentFileImpl'),
        ContentTextImpl = koclass('invariants.ContentTextImpl'),
        ContentFakeImpl = koclass('invariants.ContentFakeImpl'),
        Group = koclass('invariants.Group'),
        Runtime = koclass('invariants.Runtime');

    var EnumerationServiceImpl = {

        _templates: {},

        _numbers: {},

        getTemplate: function(id) {
            if($isNodeRef(id)) return new Node(id);
            if(this._templates[id]) return this._templates[id]();
            var template = this._templates[id] = new ko.observable();
            Alfresco.util.Ajax.jsonGet({
                url: Alfresco.constants.PROXY_URI + "citeck/enumeration/template?id=" + id,
                successCallback: function(response) {
                    template(new Node(response.json));
                }
            });
            return template();
        },

        isTemplate: function(id) {
            var template = this.getTemplate(id);
            if(template == null) return false;
            return template.isSubType('count:autonumberTemplate');
        },

        getNumber: function(template, model, count) {
            var templateId = template instanceof Node ? template.nodeRef :
                    _.isString(template) ? template : null;
            if(templateId == null) {
                throw "Can not get template id from specified argument: " + template;
            }

            var data = model instanceof Node ? { node: model.impl().allData.peek().attributes } :
                    _.isString(model) ? { node: model } :
                    _.clone(model);
            data.count = count || null;

            var templateNumbers = this._numbers[templateId];
            if(!templateNumbers) {
                templateNumbers = this._numbers[templateId] = [];
            }

            var numberRecord = _.find(templateNumbers, function(number) {
                return _.isEqual(data, number.model);
            });

            if(!numberRecord) {
                templateNumbers.push(numberRecord = {
                    model: data,
                    number: ko.observable()
                });

                Alfresco.util.Ajax.jsonPost({
                    url: Alfresco.constants.PROXY_URI + "citeck/enumeration/number?template=" + templateId,
                    dataObj: data,
                    successCallback: { fn: function(response) {
                        numberRecord.number(response.json.number);
                    } },
                    failureCallback: { fn: function(response) {
                        var index = templateNumbers.indexOf(numberRecord);
                        if(index != -1) templateNumbers.splice(index, 1);
                        Alfresco.util.PopupManager.displayPrompt({
                            text: response.json.message
                        });
                    } }
                });
            }

            return numberRecord.number();
        }
    };

    var DDClasses = koclass('dictionary.Classes'),
        DDClass = koclass('dictionary.Class'),
        DDProperty = koclass('dictionary.Property'),
        DDAssociation = koclass('dictionary.Association'),
        DDProperties = koclass('dictionary.Properties');

    DDProperties
        .key('parentDocType', s)
        .property('properties', [QName])
        .load('properties', function () {
            var load = function () {
                var properties = [];
                var attributes = [];
                Alfresco.util.Ajax.jsonGet({
                    url: Alfresco.constants.PROXY_URI + "/api/fullChildrenClasses",
                    dataObj: {'cf': 'all', 'ctbp': this.parentDocType(), 'wap': 'true'},
                    successCallback: {
                        scope: this,
                        fn: function (response) {
                            var classDefs = response.json;
                            for (var c in classDefs) {
                                properties = _.keys(classDefs[c].properties);
                                break;
                            }
                            for (var p in properties) {
                                attributes.push(new QName(properties[p]));
                            }
                            this.model({
                                properties: attributes
                            });
                            return attributes;
                        }
                    }
                });
            };
            load.call(this);
        })
    ;

    DDClass
        .key('name', s)
        .property('qname', QName)
        .property('title', s)
        .property('isAspect', b)
        .property('attributes', [QName])
        .load('attributes', koutils.bulkLoad(Citeck.utils.definedAttributesLoader, 'name', 'attributes'))
        ;

    DDClasses
        .key('filter', s)
        .property('classes', [DDClass])
        .property('ctbp', s)
        .init(function() {
            if (this.ctbp.loaded() == false) {
                this.ctbp("idocs:doc");
            }
        })
        .load('classes', koutils.simpleLoad({
            url: Alfresco.constants.PROXY_URI + "api/childrenClassesWithFullQname?cf={filter}&ctbp={ctbp}",
            resultsMap: function(response) {
                return {
                    // note: for the purposes of UI we sort this array
                    // though it is not mandatory
                    classes: _.sortBy(_.map(response, function(item) {
                        return {
                            name: item.prefixedName,
                            qname: {
                                shortQName: item.prefixedName,
                                fullQName: item.name
                            },
                            title: item.title,
                            isAspect: item.isAspect
                        }
                    }), 'name')
                }
            }
        }))
        ;

    var DictionaryServiceImpl = {

        getAllTypes: function() {
            return _.invoke(new DDClasses('type').classes(), 'name');
        },

        getAllAspects: function() {
            return _.invoke(new DDClasses('aspect').classes(), 'name');
        },

        getSubTypes: function(type) {
            var ddClasses = new DDClasses('type');
            ddClasses.ctbp(type);
            return _.invoke(ddClasses.classes(), 'name');
        },

        getTitle: function(name) {
            new DDClasses('all').ctbp("").classes();
            return new DDClass(name.key()).title();
        },

        getProperties: function (parentDocType) {
            return DDProperties(parentDocType).properties();
        }

    };

    var JournalService = koutils.koclass('journals.JournalsService')
        .property('journalTypes', [o])
        .load('journalTypes', koutils.simpleLoad({
            url: Alfresco.constants.PROXY_URI + "api/journals/maptypes",
            resultsMap: function(response) {
                return {
                    journalTypes: _.map(response, function(item) {
                        return {
                            journalType: item.journalType,
                            type: item.type
                        }
                    })
                }
            }
        }))
        .method('getAllJournalTypes', function() {
            var allJournalTypes = [];
            _.each(this.journalTypes(), function(value) {
                allJournalTypes.push(value.journalType)
            });
            return allJournalTypes;
            })
        .method('getJournalType', function(journalTypeId) {
            var journalType;
            _.each(this.journalTypes(), function(value) {
                if (value.type == QName(journalTypeId).key()) {
                    journalType = value.journalType;
                }
            })
            if(_.isUndefined(journalType)) {
                return this.getAllJournalTypes();
            } else {
                return journalType;
            }
        });

    var JournalServiceImpl = new JournalService();

    var UtilsImpl = {

        shortQName: function(name) {
            return new QName(name).shortQName();
        },

        longQName: function(name) {
            return new QName(name).fullQName();
        }

    }

    var rootObjects = {
        message: function(key) {
            var value = new Message(key).value();
            if(value == null) return key;
            return YAHOO.lang.substitute(value, _.rest(arguments));
        },
        ko: ko,
        koutils: koutils,
        utils: UtilsImpl,
        dictionary: DictionaryServiceImpl,
        enumeration: EnumerationServiceImpl,
        journals: JournalServiceImpl,
        moment: moment
    };

    function evalJavaScript(expression, model) {
        with(rootObjects) {
            with(model) {
                try {
                    return eval(expression);
                } catch(e) {
                    return undefined;
                }
            }
        }
    }

    function evalFreeMarker(expression, model) {
        try {
            var liveTemplate = _.template(expression, { interpolate: /\$\{(.+?)\}/g });
            return liveTemplate(model);
        } catch(e) {
            return undefined;
        }
    }

    function evalCriteriaQuery(criteria, model, pagination) {
        var query = {
            skipCount: 0
        };

        if (pagination) {
            if (pagination.maxItems) query.maxItems = pagination.maxItems;
            if (pagination.skipCount) query.skipCount = pagination.skipCount;
            if (pagination.sortBy) {
                if (_.isArray(pagination.sortBy)) query.sortBy = pagination.sortBy;
                if (_.isObject(pagination.sortBy)) query.sortBy = [pagination.sortBy];
            }
        }

        try {
            _.each(criteria, function(criterion, index) {
                query['field_' + index] = criterion.attribute;
                query['predicate_' + index] = criterion.predicate;
                var value = evalFreeMarker(criterion.value, model);
                if(value == null) {
                    throw {
                        message: "Expression evaluated with errors",
                        expression: criterion.value
                    };
                }
                query['value_' + index] = value;
            });
        } catch(e) {
            return null;
        }

        return query;
    }

    function evalCriteria(criteria, model, pagination) {
        var cache = model.cache;
        if(_.isUndefined(cache.result)) {
            cache.result = ko.observable(null);
        }

        var query = evalCriteriaQuery(criteria, model, pagination);
        if(query == null) return null;

        var previousResult = cache.result(); // always call this to create dependency
        if(cache.query) {
            if(_.isEqual(query, cache.query)) {
                return previousResult;
            }
        }

        // select search script by parameter from pagination
        // criteria-search by default
        var searchScripts = {
                "light-search": "citeck/light-search",
                "criteria-search": "search/criteria-search"
            },
            selectedSearchScript = pagination ? pagination.searchScript : undefined,
            searchScriptUrl = selectedSearchScript ? searchScripts[selectedSearchScript] : searchScripts["criteria-search"];

        cache.query = query;
        Alfresco.util.Ajax.jsonPost({
            url: Alfresco.constants.PROXY_URI + searchScriptUrl,
            dataObj: query,
            successCallback: {
                fn: function(response) {
                    var result = response.json.results;
                    result.pagination = response.json.paging;
                    result.query = response.json.query;
                    cache.result(result);
                }
            }
        });

        return undefined;
    }

    InvariantScope
        .property('class', s)
        .property('classKind', s)
        .property('attribute', s)
        .property('attributeKind', s)
        ;

    Invariant
        .property('scope', InvariantScope)
        .shortcut('classScope', 'scope.class')
        .shortcut('attributeScope', 'scope.attribute')
        .shortcut('classScopeKind', 'scope.classKind')
        .shortcut('attributeScopeKind', 'scope.attributeKind')
        .property('feature', s)
        .property('final', b)
        .shortcut('isFinal', 'final')
        .property('description', s)
        .property('language', s)
        .property('priority', s)
        .property('expression', o)
        .method('evaluate', function(model) {
            if(this.language() == 'javascript') {
                return evalJavaScript(this.expression(), model);
            }
            if(this.language() == 'freemarker') {
                return evalFreeMarker(this.expression(), model);
            }
            if(this.language() == 'explicit') {
                return this.expression();
            }
            if(this.language() == 'criteria') {
                return evalCriteria(this.expression(), model);
            }
            throw "Language is not supported: " + this.language();
        })
        ;

    Message
        .key('key', s)
        .property('value', s)
        .load('value', koutils.bulkLoad(Citeck.utils.messageLoader, 'key', 'value'))
        ;

    DefaultModel
        .key('key', s)
        .property('person', Node)
        .property('companyhome', Node)
        .property('userhome', Node)
        .property('view', o)
        ;

    var COMMON_DEFAULT_MODEL_KEY = "default",
        COMMON_INVARIANTS_KEY = "default";

    var invariantsLoader = new Citeck.utils.BulkLoader({
        url: Alfresco.constants.PROXY_URI + "citeck/invariants",
        method: "GET",
        emptyFn: function() { return { aspects: [] } },
        addFn: function(query, className) {
            if(className != COMMON_INVARIANTS_KEY) query.aspects.push(className);
        },
        getFn: function(response) {
            return _.groupBy(response.json.invariants, function(invariant) {
                return invariant.scope["class"] || COMMON_INVARIANTS_KEY;
            });
        }
    });

    InvariantSet
        /*.property('invariants', [Invariant])*/
        .computed('groupedInvariants', function() {
            return _.groupBy(this.invariants(), this.getInvariantKey, this);
        })
        .method('getInvariantKey', function(invariant) {
            return this.getKey(
                    invariant.scope().attributeKind(),
                    invariant.scope().attribute(),
                    invariant.feature(),
                    invariant.isFinal());
        })
        .method('getKey', function(kind, name, feature, isFinal) {
            var sep = "::";
            return kind + (name ? sep + name : '') + sep + feature + (isFinal ? sep + 'final' : '');
        })
        ;

    ExplicitInvariantSet
        .key('key', s)
        .property('invariants', [ Invariant ])
        ;

    GroupedInvariantSet
        .key('key', s)

        .computed('invariants', {
            read: function() {
                var invariants = [], createdInvariants = [],
                    processInvariant = function(invariant) {
                        if (createdInvariants.indexOf(invariant) == -1) {
                            invariants.push(new Invariant(invariant));
                            createdInvariants.push(invariant);
                        }
                    };

                _.each(this.forcedInvariants(), processInvariant);

                return invariants;
            },
            pure: true,
            deferEvaluation: false
        })

        .property('forcedInvariants', o)
        ;

    ClassInvariantSet
        .key('className', s)
        .property('invariants', [Invariant])
        .load('invariants', koutils.bulkLoad(invariantsLoader, "className", "invariants"))
        ;

    MultiClassInvariantSet
        .key('classNames', s)
        .computed('invariants', function() {
            var classNames = this.classNames().split(',');
            var invariants = _.flatten(_.map(classNames, function(className) {
                return new ClassInvariantSet(className).invariants();
            }));
            return invariants.sort(function(i1, i2) {
                // final first
                if(i1.isFinal() != i2.isFinal()) return i1.isFinal() ? -1 : 1;

                // highest attribute priority first
                var a1 = i1.attributeScopeKind().match(/_type$/) == null;
                var a2 = i2.attributeScopeKind().match(/_type$/) == null;
                if(a1 != a2) return a1 ? -1 : 1;

                // highest invariant priority first
                var priorities = { "common": 1, "module": 2, "extend": 3, "custom": 4, "view-scoped": 5 };
                var p1 = priorities[i1.priority()] || 0;
                var p2 = priorities[i2.priority()] || 0;
                if(p1 != p2) return p1 > p2 ? -1 : 1;

                // highest class priority first
                var c1 = i1.classScope();
                var c2 = i2.classScope();
                var cp1 = classNames.indexOf(c1);
                var cp2 = classNames.indexOf(c2);
                if(cp1 != cp2) return cp1 > cp2 ? -1 : 1;

                return 0;
            });
        })
        ;

    var featureInvariants = function(featureName) {
        return function() {
            var invariantSet = this.invariantSet();
            if(!invariantSet) return [];

            var groupedInvariants = invariantSet.groupedInvariants();

            var info = this.info(),
                name = info.name(),
                type = info.type(),
                kind = type == 'property' ? info.datatype() : info.nodetype();

            var keys = [
                invariantSet.getKey(type, name, featureName, true),
                invariantSet.getKey(type + '_type', kind, featureName, true),
                invariantSet.getKey(type + '_type', null, featureName, true),
                invariantSet.getKey(type, name, featureName, false),
                invariantSet.getKey(type + '_type', kind, featureName, false),
                invariantSet.getKey(type + '_type', null, featureName, false)
            ];

            return _.union.apply(_, _.map(keys, function(key) {
                return groupedInvariants[key] || [];
            }, this));
        };
    };

    var featureEvaluator = function(featureName, requiredClass, defaultValue, isTerminate) {
        return function(model) {
            var invariantSet = this.invariantSet(),
                invariant = null,
                invariantValue = null,
                invariants = featureInvariants(featureName).call(this);

            invariant = _.find(invariants, function(invariant) {
                invariantValue = invariant.evaluate(model);
                return isTerminate(invariantValue, invariant);
            });

            return {
                invariant: invariant,
                value: koutils.instantiate(invariant != null ? invariantValue : defaultValue, requiredClass)
            }
        };
    };

    var featuredProperty = function(featureName) {
        return function() {
            return this[featureName + 'Evaluator'](this.invariantsModel()).value;
        }
    };

    var notNull = function(value) { return value !== null; }
    var isFalse = function(value) { return value === false; }

    var classMapping = {
        'java.lang.Object': o,
        'java.lang.String': s,
        'java.util.Date': d,
        'java.lang.Long': n,
        'java.lang.Float': n,
        'java.lang.Double': n,
        'java.lang.Integer': n,
        'java.lang.Boolean': b,
        'java.util.Locale': s,
        'org.alfresco.service.cmr.repository.MLText': s,
        'org.alfresco.service.cmr.repository.NodeRef': Node,
        'org.alfresco.service.namespace.QName': QName,
        'org.alfresco.service.cmr.repository.ContentData': Content,
        'org.alfresco.util.VersionNumber': s,
        'org.alfresco.service.cmr.repository.Period': s
    };

    var datatypeClassMapping = {
        'd:any': 'java.lang.Object',
        'd:text': 'java.lang.String',
        'd:date': 'java.util.Date',
        'd:datetime': 'java.util.Date',
        'd:int': 'java.lang.Integer',
        'd:long': 'java.lang.Long',
        'd:float': 'java.lang.Float',
        'd:double': 'java.lang.Double',
        'd:boolean': 'java.lang.Boolean',
        'd:locale': 'java.util.Locale',
        'd:mltext': 'org.alfresco.service.cmr.repository.MLText',
        'd:noderef': 'org.alfresco.service.cmr.repository.NodeRef',
        'd:category': 'org.alfresco.service.cmr.repository.NodeRef',
        'd:qname': 'org.alfresco.service.namespace.QName',
        'd:content': 'org.alfresco.service.cmr.repository.ContentData',
        'd:version': 'org.alfresco.util.VersionNumber',
        'd:period': 'org.alfresco.service.cmr.repository.Period'
    };

    var datatypeNodetypeMapping = {
        'd:noderef': 'sys:base',
        'd:category': 'cm:category'
    };

    var attributeLoader = new Citeck.utils.BulkLoader({
        url: Alfresco.constants.PROXY_URI + "citeck/invariants/attributes",
        method: "POST",
        emptyFn: function() { return {names:[]} },
        addFn: function(query, name) {
            if(query.names.indexOf(name) == -1) {
                query.names.push(name);
                return true;
            } else {
                return false;
            }
        },
        getFn: function(response) {
            var attributes = response.json.attributes;
            return _.object(_.pluck(attributes, 'name'), attributes);
        }
    });

    Predicate
        .key('id', s)
        .property('label', s)
        .property('needsValue', b)
        ;

    AttributeSet
        .constructor([ Object, Node], function(data, node) {
            return new AttributeSet({
                id: data.id,
                _attributes: data.attributes,
                _sets: data.sets,
                node: node
            });
        }, true)
        .constructor([ Object, Node, AttributeSet], function(data, node, parentSet) {
            return new AttributeSet({
                id: data.id,
                _attributes: data.attributes,
                _sets: data.sets,
                node: node,
                parentSet: parentSet
            });
        }, true)

        .key('id', s)
        .property('node', Node)
        .property('parentSet', AttributeSet)

        .property('_attributes', o)
        .property('_sets', o)

        .computed('attributes', function() {
            var self = this, attributes = self.resolve('node.impl.attributes', []);
            return _.filter(attributes, function(attribute) {
                return !!_.find(self._attributes(), function(attr) { return attr.name == attribute.name(); });
            });
        })
        .computed('sets', function() {
            var self = this;
            return this._sets().map(function(as) {
                return new AttributeSet(as, self.node(), self);
            });
        })
        .computed('relevant', function() {
            return !this.irrelevant();
        })
        .computed('irrelevant', function() {
            var self = this;
            var a_irrelevant = _.every(this.attributes(), function(attr) { 
                    return attr.irrelevant() || self.getAttributeTemplate(attr.name()) == "none"; 
                }),
                s_irrelevant = _.every(this.sets(), function(set) { return set.irrelevant(); });
            return a_irrelevant && s_irrelevant;
        })

        .method('getAttributeTemplate', function(attributeName) {
            var attribute = _.find(this._attributes(), function(attr) { return attr.name == attributeName; });
            if (attribute) return attribute.template;
            return null;
        })
        ;

    AttributeInfo
        .key('name', s)
        .property('type', s) // one of: property, association, child-association, ...
        .property('nodetype', s)
        .property('datatype', s)
        .property('javaclass', s)
        .property('predicates', [ Predicate ])

        .load('*', koutils.bulkLoad(attributeLoader, 'name'))
        .load('predicates', function(attributeInfo) {
            var datatype = attributeInfo.datatype();
            if (datatype.indexOf(":") != -1) { datatype = datatype.split(":")[1]; }

            YAHOO.util.Connect.asyncRequest('GET',
                Alfresco.constants.URL_PAGECONTEXT + "search/search-predicates?datatype=" + datatype, {
                    success: function(response) {
                        var result = JSON.parse(response.responseText),
                            predicates = [];

                        for (var i in result.predicates) {
                            predicates.push(new Predicate(result.predicates[i]))
                        };

                        this.predicates(predicates);
                    },

                    failure: function(response) { /* error */ },

                    scope: attributeInfo
                }
            );
        })

        // auxiliary variables
        .property('_options', [ Node ])
        .property('_dependencies', o)
        ;

    Attribute
        .constructor([Node, String], function(node, name) {
            var attr = new Attribute({
                key: node.key() + ":" + name,
                info: name
            });
            attr.node(node);
            return attr;
        }, true)
        .constructor([Node, String, Boolean], function(node, name, persisted) {
            var attr = new Attribute(node, name);
            attr.persisted(persisted);
            return attr;
        }, true)
        .constructor([Node, String, Boolean, Object], function(node, name, persisted, value) {
            var attr = new Attribute(node, name, persisted);
            attr.persistedValue(value);
            return attr;
        }, true)

        .key('key', s)
        .property('info', AttributeInfo)
        .property('node', Node)
        .property('persisted', b)

        .property('inlineEditVisibility', b)

        .shortcut('name', 'info.name')
        .shortcut('type', 'info.type')
        .shortcut('nodetype', 'info.nodetype')
        .shortcut('datatype', 'info.datatype')
        .shortcut('predicates', 'info.predicates')
        .shortcut('javaclass', 'info.javaclass')
        .shortcut('default', 'defaultValue', null)

        .computed('valueClass', function() { return classMapping[this.javaclass()] || null; })
        .computed('invariantSet', function() { return this.node().impl().invariantSet(); })
        .computed('invariantsModel', function() { return this.getInvariantsModel(this.value, this.cache = this.cache || {}); })
        .computed('changed', function() { return this.newValue.loaded(); })
        .computed('textValue', {
            read: function() {
                return this.getValueText(this.value());
            },
            write: function(value) {
                if(value == null || value == "") {
                    return this.value(null);
                } else {
                    return this.value(value);
                }
            }
        })
        .computed('textValidationValue', {
            read: function() {
                return this.getValueText(this.value());
            },
            write: function(value) {
                if(value == null || value == "") {
                    return this.value(null);
                } else {
                    var newValue = (value.replace(/<(?:[^"'>]+|(["'])(?:\\[\s\S]|(?!\1)[\s\S])*\1)*>/g, ""));
                    return this.value(this.value() == newValue ? null : newValue);
                }
            }
        })
        .computed('title', featuredProperty('title'))
        .computed('description', featuredProperty('description'))
        .computed('multiple', featuredProperty('multiple'))
        .computed('mandatory', featuredProperty('mandatory'))
        .computed('invariantRelevant', featuredProperty('relevant'))
        .computed('relevant', function() {
            var forcedAttributes = this.node().impl().forcedAttributes();
            if(!_.isEmpty(forcedAttributes) // is a view attribute
            && !_.contains(forcedAttributes, this.name())) // is not forced
                return false; // non-exposed attributes are always irrelevant
            return this.invariantRelevant();
        })
        .computed('invariantProtected', featuredProperty('protected'))
        .computed('protected', function() {
            var invariantValue = this.invariantValue();
            if(invariantValue != null && (!_.isArray(invariantValue) || invariantValue.length > 0)) return true;
            return this.invariantProtected();
        })
        .computed('empty', function() {
            return this.value() == null
                || this.multiple() && this.value().length == 0
                || this.valueClass() == String && this.value().length == 0;
        })
        .computed('evaluatedValid', function() { return this.validEvaluator(this.invariantsModel()); })
        .computed('invariantValid', function() { return this.evaluatedValid().value; })
        .computed('valid', function() {
            // irrelevant is always valid
            if(this.irrelevant()) return true;

            // empty values: valid, if optional or protected
            if(this.empty()) {
                return this.optional() || this['protected']();
            }

            // non-empty values: valid invariants should all be valid
            return this.invariantValid();
        })
        .computed('validDraft', function() {
            // irrelevant or empty is always valid
            if(this.irrelevant() || this.empty()) return true;

            // non-empty values: valid invariants should all be valid
            return this.invariantValid();
        })
        .computed('validationMessage', function() {
            // mimic validation behaviour:
            if(this.irrelevant()) return "";

            if(this.empty()) {
                return this.optional() || this['protected']() ? "" : Alfresco.util.message("validation-hint.mandatory");
            }

            var invariant = this.evaluatedValid().invariant;
            return invariant != null ? Alfresco.util.message(invariant.description()) : "";
        })
        .computed('single', function() { return !this.multiple(); })
        .computed('optional', function() { return !this.mandatory(); })
        .computed('irrelevant', function() { return !this.relevant(); })
        .computed('invalid', function() { return !this.valid(); })
        .computed('unchanged', function() { return !this.changed(); })
        .computed('jsonValue', {
            read: function() {
                return this.getValueJSON(this.value());
            },
            write: function(value) {
                if(value == null || value == "") {
                    return this.value(null);
                } else {
                    return this.value(value);
                }
            }
        })

        .load('inlineEditVisibility', function() { this.inlineEditVisibility(false) })
        
        .method('showEditableField', function(data, event) {
            if (!this.inlineEditVisibility()) this.inlineEditVisibility(true);
        })
        .method('inlineEditChanger', function(data, event) {
            var isDraft = this.node().properties["invariants:isDraft"];

            // save node if it valid
            if (this.inlineEditVisibility()) {
                if (this.newValue() != null && this.newValue() != this.persistedValue()) {
                    if (isDraft || this.resolve("node.impl.valid")) {
                        this.node().thisclass.save(this.node(), { });

                        if (isDraft) {
                            data.inlineEditVisibility(false);
                        } else {
                            // hide inline edit form for all attributes after save node
                            _.each(this.node().impl().attributes(), function(attr) {
                                if (attr.inlineEditVisibility()) attr.inlineEditVisibility(false);
                            });
                        }
                    }
                    
                    return;
                }
            }
            

            // change visibility mode
            this.inlineEditVisibility(!this.inlineEditVisibility());
        })
        .method('convertValue', function(value, multiple) {
            var isArray = _.isArray(value),
                instantiate = _.partial(koutils.instantiate, _, this.valueClass());
            if(value == null) {
                return multiple
                        ? []
                        : null;
            } else if(isArray) {
                return multiple
                        ? _.map(value, instantiate)
                        : instantiate(value[0]);
            } else {
                return multiple
                        ? [ instantiate(value) ]
                        :   instantiate(value) ;
            }
        })
        .method('getInvariantsModel', function(value, cache) {
            var model = {};
            _.each(this.node().impl().defaultModel(), function(property, name) {
                Object.defineProperty(model, name, _.isFunction(property) ? { get: property } : { value: property });
            });
            Object.defineProperty(model, 'node', { get: this.node });
            Object.defineProperty(model, 'value', typeof value == "function" ? { get: value } : { value: value });
            Object.defineProperty(model, 'cache', { value: cache });
            return model;
        })
                .method('href', function(data) {
            if (data && data.toString().indexOf("invariants.Node") != -1) {
                if (data.typeShort == "cm:person") {
                    if (Alfresco.constants.URI_TEMPLATES["userprofilepage"]) {
                        return Alfresco.util.uriTemplate("userprofilepage", { userid: data.properties.userName });
                    }
                } 
                return Alfresco.util.siteURL('card-details?nodeRef=' + data.nodeRef);
            }
            return null;
        })
        .method('reset', function(full) {
            this.newValue(null);
            this.newValue.reload();
            if(full) {
                this.persisted.reload();
                this.persistedValue.reload();
            }
        })
        .method('getValueText', function(value) {
            if(value == null) return null;
            if(_.isArray(value)) return _.map(value, this.getValueText, this);

            var valueClass = this.valueClass();
            if(valueClass == null) return "" + value;
            if(valueClass == o) return value.toString();
            if(valueClass == s) return "" + value;
            if(valueClass == b) return value ? "true" : "false";
            if(valueClass == Node) return value.nodeRef;
            if(valueClass == QName) return value.shortQName();
            if(valueClass == Content) return value.content;

            var datatype = this.datatype();
            if(valueClass == n) {
                if(datatype == 'd:int' || datatype == 'd:long') {
                    return "" + Math.floor(value);
                }
                return "" + value;
            }
            if(valueClass == d) {
                if(datatype == 'd:date') {
                    var year = value.getFullYear(),
                        month = value.getMonth() + 1,
                        date = value.getDate();
                    return (year > 1000 ? "" : year > 100 ? "0" : year > 10 ? "00" : "000") + year
                            + (month < 10 ? "-0" : "-") + month
                            + (date  < 10 ? "-0" : "-") + date;
                }

                return Alfresco.util.toISO8601(value);
            }

            throw {
                message: "Value class is not supported",
                valueClass: valueClass,
                datatype: datatype
            };
        })
        .method('remove', function(index) {
            if(this.single()) {
                this.value(null);
            } else {
                var currentValues = this.value();
                if(index < currentValues.length) {
                    this.value(_.union(
                        _.first(currentValues, index),
                        _.rest(currentValues, index + 1)
                    ));
                }
            }
        })
        .method('destroy', function(index, nodeRef) {
            this.resolve('node.impl.runtime').deleteNode(nodeRef, {
                success: function(oResponse) {
                    var result = YAHOO.lang.JSON.parse(oResponse.responseText)
                    if (result.success) this.remove(index);
                },
                scope: this
            })
        })
        .method('push', function(data) {
            if (this.single()) { 
                this.value(data); 
            } else {
                var currentValues = this.value();
                currentValues.push(data);
                this.value(currentValues);
            }
        })
        .method('getValueJSON', function(value) {
            if(value == null) return null;
            if(_.isArray(value)) return _.map(value, this.getValueJSON, this);

            var valueClass = this.valueClass();
            if(valueClass == null) return null;
            if(valueClass == o) return value.toString();
            if(valueClass == s) return "" + value;
            if(valueClass == n) return value.toString();
            if(valueClass == b) return value ? true : false;
            if(valueClass == Node) return value.nodeRef;
            if(valueClass == QName) return value.shortQName();
            if(valueClass == Content) return value.impl().jsonValue();

            var datatype = this.datatype();
            if(valueClass == n) {
                if(datatype == 'd:int' || datatype == 'd:long') {
                    return Math.floor(value);
                }
                return value;
            }
            if(valueClass == d) {
                if(datatype == 'd:date') {
                    var year = value.getFullYear(),
                        month = value.getMonth() + 1,
                        date = value.getDate();
                    return (year > 1000 ? "" : year > 100 ? "0" : year > 10 ? "00" : "000") + year
                            + (month < 10 ? "-0" : "-") + month
                            + (date  < 10 ? "-0" : "-") + date;
                }
                return Alfresco.util.toISO8601(value);
            }

            throw {
                message: "Value class is not supported",
                valueClass: valueClass,
                datatype: datatype
            };
        })
        .method('getGroup', function() {
            var name = this.name(), groups = this.resolve("node.impl.groups", []);
            return _.find(groups, function(gp) { return gp.attributes().indexOf(name) != -1; }) || null;
        })

        // feature evaluators
        .method('valueEvaluator', featureEvaluator('value', o, null, notNull))
        .method('defaultEvaluator', featureEvaluator('default', o, null, notNull))
        .method('optionsEvaluator', featureEvaluator('options', o, null, notNull))
        .method('titleEvaluator', featureEvaluator('title', s, '', notNull))
        .method('descriptionEvaluator', featureEvaluator('description', s, '', notNull))
        .method('valueTitleEvaluator', featureEvaluator('value-title', s, '', notNull))
        .method('valueDescriptionEvaluator', featureEvaluator('value-description', s, '', notNull))
        .method('relevantEvaluator', featureEvaluator('relevant', b, true, notNull))
        .method('multipleEvaluator', featureEvaluator('multiple', b, false, notNull))
        .method('mandatoryEvaluator', featureEvaluator('mandatory', b, false, notNull))
        .method('protectedEvaluator', featureEvaluator('protected', b, false, notNull))
        .method('validEvaluator', featureEvaluator('valid', b, true, isFalse))

        // value properties:
        .property('newValue', o) // value, set by user
        .property('persistedValue', o) // value, persisted in repository
        .computed('invariantValue', featuredProperty('value'))
        .computed('invariantDefault', featuredProperty('default'))
        .computed('defaultValue', function() { return this.convertValue(this.invariantDefault(), this.multiple()); })
        .computed('rawValue', function() {
            var invariantValue = this.invariantValue(),
                isDraft = this.node().properties["invariants:isDraft"],
                isView = this.node().impl().inViewMode();

            if(invariantValue != null) return invariantValue;
            if(this.changed()) return this.newValue();

            if(this.persisted()) {
                if (!isView && isDraft &&
                    _.isEmpty(this.persistedValue()) &&
                    !_.isBoolean(this.persistedValue()) &&
                    !_.isNumber(this.persistedValue())) {
                    return this.invariantDefault();
                }
                return this.persistedValue();
            }

            return isView ? null : this.invariantDefault();
        })
        .computed('value', {
            read: function() { return this.convertValue(this.rawValue(), this.multiple()); },
            write: function(value) { this.newValue(this.convertValue(value, true)); }
        })
        .computed('singleValue', {
            read: function() { return this.convertValue(this.rawValue(), false); },
            write: function(value) { this.value(value); }
        })
        .computed('multipleValues', {
            read: function() { return this.convertValue(this.rawValue(), true); },
            write: function(value) { this.value(value); }
        })
        .computed('lastValue', {
            read: function() {
                return this.single() ? this.value() :
                    this.value().length == 0 ? null :
                    _.last(this.value());
            },
            write: function(value) {
                value = this.convertValue(value, false);
                if(this.single()) {
                    this.value(value);
                } else {
                    var currentValues = this.value();
                    if(!_.contains(currentValues, value)) {
                        this.value(_.union(currentValues, [value]))
                    }
                }
            }
        })

        // options properties
        .computed('invariantOptions', featuredProperty('options'))
        .computed('optionsInvariants', function() { return featureInvariants('options').call(this); })
        .computed('options', {
            read: function() {
                var options = this.invariantOptions();
                return options ? this.convertValue(options, true) : [];
            },
            pure: true
        })
        .method('filterOptions', function(criteria, pagination) {
            // find invariant with correct query
            var model = this.getInvariantsModel(this.value, criteria.cache = criteria.cache || {}),
                optionsInvariant = _.find(this.optionsInvariants(), function(invariant) {
                    if(invariant.language() == "criteria") {
                        var query = evalCriteriaQuery(_.union(invariant.expression(), criteria), model);
                        return query != null;
                    } else { return true }
                });

            if(optionsInvariant == null) return [];
            if(optionsInvariant.language() != "criteria") return this.options();

            var options = evalCriteria(_.union(optionsInvariant.expression(), criteria), model, pagination);
            if (options != null) {
                var optionsWithConvertedValues = this.convertValue(options, true);
                if (options.pagination) optionsWithConvertedValues.pagination = options.pagination;
                return optionsWithConvertedValues;
            }

            return [];
        })

        // value title
        .method('getValueTitle', function(value, postprocessing) {
            var value = this.valueTitleEvaluator(this.getInvariantsModel(value)).value;
            if (postprocessing) { return postprocessing(value);  }
            return value; 
        })
        .computed('valueTitle', function() { return this.getValueTitle(this.singleValue()); })
        .shortcut('value-title', 'valueTitle')

        // value description
        .method('getValueDescription', function(value) {
            var model = this.getInvariantsModel(value);
            return this.valueDescriptionEvaluator(model).value || this.getValueTitle(value);
        })
        .computed('valueDescription', function() { return this.getValueDescription(this.singleValue()); })
        .shortcut('value-description', 'valueDescription')

        // persisted value loading
        .load(['persisted', 'persistedValue'], function(attr) {
            if(!attr.node().impl().isPersisted()) {
                attr.persisted(false);
                return;
            }
            Citeck.utils.attributeValueLoader.loadValue(attr.node().nodeRef, attr.name(), function(key, response) {
                attr.persisted(response.persisted);
                if(response.persisted) {
                    attr.persistedValue(response.value);
                }
            });
        })
        ;

    Group
        .property('id', s)
        .property('index', n)
        .property('_visibility', b)
        .property('_activity', b)
        .property('attributes', [ s ])
        .property('invariants', [ Invariant ])

        .computed('hidden', {
            read: function() { return !this._visibility(); },
            write: function(newValue) { this._visibility(!newValue); }
        })
        .computed('visible', {
            read: function() { return this._visibility(); },
            write: function(newValue) { this._visibility(!!newValue); }
        })
        .computed('disable', {
            read: function() { return !this._activity(); },
            write: function(newValue) { this._activity(!newValue); }
        })
        .computed('enable', {
            read: function() { return this._activity(); },
            write: function(newValue) { this._activity(!!newValue); }
        })

        .load('_visibility', function() { this._visibility(true) })
        .load('_activity', function() { this._activity(true) })

        .method('open', function() {
            $(".tab-title[data-tab-id=" + this.id() + "]").addClass("selected");
            $(".tab-body[data-tab-id=" + this.id() + "]").show();
        })
        .method('close', function() {
            $(".tab-body[data-tab-id=" + this.id() + "]").hide();
            $(".tab-title[data-tab-id=" + this.id() + "]").removeClass("selected");
        })
        .method('getTitleEl', function() { return $(".tab-title[data-tab-id=" + this.id() + "]")[0]; })
        .method('getBodyEl', function() { return $(".tab-body[data-tab-id=" + this.id() + "]")[0]; })

        .init(function() {
            var self = this;

            // subscription
            this._visibility.subscribe(function(newValue) {
                var tab = $(self.getTitleEl()),
                    body = $(self.getBodyEl());

                if (!!newValue) { tab.show(); body.show(); }
                else { tab.hide(); body.hide(); }
            });

            this._activity.subscribe(function(newValue) {
                var tab = $(self.getTitleEl()),
                    body = $(self.getBodyEl());

                if (!!newValue) {
                    tab.attr("data-activity", true);
                    body.show();
                } else {
                    tab.attr("data-activity", false).removeClass("selected");
                    body.hide();
                }
            });

            // trigger subscription
            this._visibility(this._visibility());
            this._activity(this._activity());
        })
    ;

    NodeImpl
        .constructor([Node, Object], function(node, model) {
            var that = NodeImpl.call(this, node.key());
            that.node(node);
            that.updateModel(model);
            return that;
        })

        .key('key', s)
        .property('nodeRef', s)
        .property('isDraft', b)
        .property('node', Node)
        .property('type', s)
        .property('classNames', [ s ])
        .property('permissions', o)
        .property('inSubmitProcess', b)
        .property('groups', [ Group ])

        .shortcut('typeShort', 'type')

        .computed('isPersisted', function() { return this.nodeRef() != null; })
        .computed('typeFull', function() {
            if(this.type() == null) return null;
            var qnameType = new QName(this.type());
            return qnameType.fullQName();
        })
        .computed('invariantSet', function() {
            return this.resolve('runtime.invariantSet')
                || new MultiClassInvariantSet(this.classNames().concat(COMMON_INVARIANTS_KEY).join(','));
        })
        .computed('inViewMode', function() {
            return this.resolve('defaultModel.view.mode') == "view";
        })
        .computed('types', function() {
            var types = this.attribute('attr:types');
            return types ? types.multipleValues() : [];
        })
        .computed('aspects', function() {
            var aspects = this.attribute('attr:aspects');
            return aspects ? aspects.multipleValues() : [];
        })

        .computed('definedAttributeNames', function() {
            return _.uniq(_.flatten(_.map(
                    this.classNames(),
                    function(className) {
                        return _.invoke(new DDClass(className).attributes(), 'shortQName');
                    }
            )));
        })
        .computed('valid', function() {
            return _.all(this.attributes(), function(attr) {
                return attr.valid();
            });
        })
        .computed('validDraft', function() {
            return _.all(this.attributes(), function(attr) {
                return attr.validDraft();
            });
        })

        .computed('changed', function() {
            return _.any(this.attributes(), function(attr) {
                return attr.changed();
            });
        })
        .computed('invalid', function() {
            return !this.valid();
        })
        .computed('unchanged', function() {
            return !this.changed();
        })

        // Computed 'data' is deprecated. Will soon be deleted.
        // Use 'allData' or 'changedData'
        .computed('data', function() {
            var attributes = {};

            _.each(this.runtime() && this.runtime().inlineEdit() ? this.changedAttributes() : this.attributes(), function(attr) {
                if(attr.relevant()) { attributes[attr.name()] = attr.jsonValue(); }
            });

            return {
                nodeRef: this.nodeRef(),
                attributes: attributes
            };
        })

        .computed('changedData', function() {
            var attributes = {};

            _.each(this.changedAttributes(), function(attr) {
                if(attr.relevant()) { attributes[attr.name()] = attr.jsonValue(); }
            });

            return {
                nodeRef: this.nodeRef(),
                attributes: attributes
            };
        })
        .computed('allData', function() {
            var attributes = {};

            _.each(this.attributes(), function(attr) {
                if(attr.relevant()) {
                    attributes[attr.name()] = attr.jsonValue();
                }
            });

            return {
                nodeRef: this.nodeRef(),
                attributes: attributes
            };
        })

        .load('groups', function(impl) { impl.groups([]) })
        .load('*', function(impl) {
            // existing nodes:
            if(impl.isPersisted()) {
                Citeck.utils.nodeInfoLoader.load(impl.nodeRef(), function(nodeRef, model) {
                    if (model) impl.updateModel(model);
                });
            }
        })


        .method('_filterAttributes', function(filterBy) {
            return _.filter(this.attributes() || [], function(attr) {
                return attr[filterBy]();
            })
        })
        .method('attribute', function(name) {
            return _.find(this.attributes() || [], function(attr) {
                return attr.name() == name;
            });
        })
        .method('set', function(name) {
            var findSetRecursively = function(sets, setId) {
                for (var s = 0; s < sets.length; s++) {
                    if (sets[s].id() == setId) return sets[s];
                    if (sets[s].sets().length) {
                        var findedSet = findSetRecursively(sets[s].sets(), setId);
                        if (findedSet) return findedSet;
                    }
                }

                return null;
            };

            return findSetRecursively(this.sets(), name);
        })
        .method('group', function(id) {
            return _.find(this.groups() || [], function(group) {
                return group.id() == id;
            });
        })
        .method('reset', function(full) {
            _.invoke(this.attributes(), 'reset', full);
            if(full) this._attributes.reload();
        })
        .method('updateModel', function(model) {
            if (model) {
                this.model(_.omit(model, 'attributes'));

                if(model.attributes) {
                    this.model({ _attributes: model.attributes })
                }
            }
        })
        .method('changedAttributes', function() {
            return this._filterAttributes("changed");
        })
        .method('invalidAttributes', function() {
            return this._filterAttributes("invalid");
        })

        .property('_sets', o)
        .computed('sets', {
            read: function() {
                var node = this.node(), sets = [], createdNames = {};
                var processSetName = function(data) {
                    if(!createdNames[data.id]) {
                        createdNames[data.id] = true;
                        sets.push(new AttributeSet(data, node));
                    }
                };

                _.each(this._sets(), processSetName);
                return sets;
            },
            pure: true
        })

        .property('_attributes', o)
        .computed('attributes', {
            read: function() {
                var node = this.node(),
                    attributes = [],
                    createdNames = {};

                if(this.isPersisted()) {
                    _.each(this._attributes(), function(value, name) {
                        createdNames[name] = true;
                        attributes.push(new Attribute(node, name, true, value));
                    });
                    // little optimization trick:
                    // if node is persisted and its persisted attributes are not loaded,
                    // first wait for them and then add all other attributes
                    if (!this._attributes.loaded()) {
                        // preload defined attribute names:
                        this.definedAttributeNames();
                        return attributes;
                    }
                }

                var processAttributeName = function(name) {
                    if(!createdNames[name]) {
                        createdNames[name] = true;
                        // we can't be sure, whether this this attribute is persisted or not
                        // because not all persisted attributes are in the default attributes list
                        attributes.push(new Attribute(node, name));
                    }
                };

                // first load forced attributes
                _.each(this.forcedAttributes(), processAttributeName);

                // second load defined attributes
                if (!this.runtime() || this.runtime().loadAttributesMethod() == "default") {
                    _.each(this.definedAttributeNames(), processAttributeName);
                }

                // if (this.runtime().loadAttributesMethod() == "clickOnGroup") {
                //     if (_.every(_.flatten(this.groupedAttributes()), function(attribute) {
                //       return this.forcedAttributes().indexOf(attribute) != - 1;
                //     }, this)) {
                //         var remainingAttributes = _.difference(this.forcedAttributes(), this.definedAttributeNames());
                //         console.log(remainingAttributes);
                //         _.each(remainingAttributes, processAttributeName);
                //     }
                // }

                return attributes;
            },
            pure: true
        })

        .property('virtualParent', Node)
        .computed('parent', function() {
            var virtualParent = this.virtualParent();
            if(virtualParent) return virtualParent;
            var parent = this.attribute('attr:parent');
            return parent ? parent.value() : null;
        })

        .property('forcedAttributes', [s])
        .load('forcedAttributes', function(impl) { impl.forcedAttributes([]) })

        .property('defaultModel', DefaultModel)
        .load('defaultModel', function(impl) { impl.defaultModel(new DefaultModel(COMMON_DEFAULT_MODEL_KEY)) })

        .property('runtime', Runtime)
        .load('runtime', function(impl) { impl.runtime(null); })

        .init(function() {
            this.inSubmitProcess(false);
        })
        ;

    var assocsComputed = function(type) {
        return function() {
            var assocs = {};
            _.each(this.impl().attributes(), function(attr) {
                if(attr.type() != type) return;
                var config = {
                    configurable: false,
                    enumerable: true,
                    get: function() {
                        var value = attr.value();
                        if(value == null) return [];
                        if(_.isArray(value)) return value;
                        return [ value ];
                    }
                };
                Object.defineProperty(assocs, attr.name(), config);
            }, this);
            return assocs;
        }
    };

    Node
        .key('key', s)
        .property('impl', NodeImpl)

        .constructor([String], function(key) {
        }, true)
        .init(function() {
            if(this.impl.loaded() == false) {
                this.impl(new NodeImpl(this, {
                    nodeRef: this.key()
                }));
            }
        })
        .constructor([Object], function(model) {
            var that = Node.call(this, model.key || model.nodeRef);
            that.impl(new NodeImpl(that, model));
            return that;
        })

        .nativeProperty('nodeRef', function() {
            return this.impl().nodeRef() || '';
        })
        .nativeProperty('storeType', function() {
            return this.nodeRef.replace(/^(.+):\/\/(.+)\/(.+)$/, '$1');
        })
        .nativeProperty('storeId', function() {
            return this.nodeRef.replace(/^(.+):\/\/(.+)\/(.+)$/, '$2');
        })
        .nativeProperty('id', function() {
            return this.nodeRef.replace(/^(.+):\/\/(.+)\/(.+)$/, '$3');
        })

        .nativeProperty('type', function() {
            return this.impl().typeFull();
        })
        .nativeProperty('typeShort', function() {
            return this.impl().typeShort();
        })
        .method('isSubType', function(name) {
            if(Citeck.utils.isShortQName(name)) {
                return this.impl().typeShort() == name || _.contains(_.invoke(this.impl().types(), 'shortQName'), name);
            } else {
                return this.impl().typeFull() == name || _.contains(_.invoke(this.impl().types(), 'fullQName'), name);
            }
        })
        .nativeProperty('isCategory', function() {
            this.isSubType('cm:category');
        }, true)
        .nativeProperty('isContainer', function() {
            this.isSubType('cm:folder');
        }, true)
        .nativeProperty('isDocument', function() {
            this.isSubType('cm:content');
        }, true)

        .nativeProperty('aspects', function() {
            return _.invoke(this.impl().aspects(), 'shortQName');
        }, true)
        .method('hasAspect', function(name) {
            if(Citeck.utils.isShortQName(name)) {
                return _.contains(_.invoke(this.impl().aspects(), 'shortQName'), name);
            } else {
                return _.contains(_.invoke(this.impl().aspects(), 'fullQName'), name);
            }
        })

        .nativeProperty('parent', function() {
            return this.impl().parent();
        })
        .nativeProperty('properties', function() {
            logger.debug('properties recalculated for node ' + this.key() + ' (nodeRef ' + this.impl().nodeRef() + ')');
            var properties = {};
            _.each(this.impl().attributes(), function(attr) {
                if(attr.type() != 'property') return;
                var config = {
                    configurable: false,
                    enumerable: true,
                    get: attr.value,
                    set: attr.value,
                };
                Object.defineProperty(properties, attr.name(), config);
                if(attr.name().match(/^cm:/)) {
                    Object.defineProperty(properties, attr.name().replace(/^cm:/, ''), config);
                }
            });
            return properties;
        }, true)
        .nativeProperty('assocs', assocsComputed('association'), true)
        .nativeProperty('associations', assocsComputed('association'), true)
        .nativeProperty('childAssocs', assocsComputed('child-association'), true)
        .nativeProperty('childAssociations', assocsComputed('child-association'), true)
        .nativeProperty('name', {
            get: function() {
                return this.properties.name;
            },
            set: function(value) {
                this.properties.name = value;
            }
        })

        .method('hasPermission', function(permission) {
            return this.resolve('impl.permissions.' + permission, false) == true;
        })

        .save(koutils.simpleSave({
            url: function(node) {
                var baseUrl = Alfresco.constants.PROXY_URI + "citeck/invariants/view?";
                if($isNodeRef(node.nodeRef)) {
                    return baseUrl + "nodeRef=" + node.nodeRef;
                } else {
                    return baseUrl + "type=" + node.typeShort;
                }
            },
            toRequest: function(node) {
                node.impl().inSubmitProcess(true);

                var defaultView = node.resolve('impl.defaultModel.view'),
                    data = {
                        view: defaultView,
                        attributes: node.resolve('impl.' + ( defaultView.mode == "create" ? 'allData' : 'changedData' ) + '.attributes')
                    };
                    
                var isDraft = node.resolve('impl.isDraft');
                if (_.isBoolean(isDraft)) data['isDraft'] = isDraft;

                return data;
            },
            toResult: function(response) {
                return new Node(response.result);
            },
            toFailureMessage: function(response) {
                return response.message;
            },
            onSuccess: function(node, result) {
                if (node.impl().runtime() && node.impl().runtime().inlineEdit()) {
                    node.impl().changedAttributes().forEach(function(attribute) {
                        // newValue as persistedValue
                        // persisited by default as 'true' after save
                        attribute.persistedValue(attribute.newValue());
                        attribute.persisted(true);

                        // reset new value
                        attribute.reset();
                    });
                }

                node.impl().inSubmitProcess(false);
            },
            onFailure: function(node, message) {
                if (node.impl().runtime() && node.impl().runtime().inlineEdit()) {
                    node.impl().changedAttributes().forEach(function(attribute) {
                        // reset old and new value
                        attribute.reset(true);
                    });
                }

                node.impl().inSubmitProcess(false);
            }
        }))
        ;

    QName
        .key('key', s)

        .property('prefix', s)
        .property('uri', s)
        .property('localName', s)

        .computed('shortQName', function() {
            return this.prefix() ? this.prefix() + ":" + this.localName() : this.localName();
        })
        .computed('fullQName', function() {
            return this.uri() ? "{" + this.uri() + "}" + this.localName() : this.localName();
        })
        .computed('longQName', function() {
            return this.uri() ? "{" + this.uri() + "}" + this.localName() : this.localName();
        })

        .constructor([String], function(key) {
            var shortQNamePattern = /^(.+)[:](.+)$/;
            var fullQNamePattern = /^[{](.+)[}](.+)$/;
            if(key.match(shortQNamePattern)) {
                this.model({
                    prefix: key.replace(shortQNamePattern, '$1'),
                    localName: key.replace(shortQNamePattern, '$2')
                });
            } else if(key.match(fullQNamePattern)) {
                this.model({
                    uri: key.replace(fullQNamePattern, '$1'),
                    localName: key.replace(fullQNamePattern, '$2')
                });
            } else {
                this.model({
                    prefix: '',
                    uri: '',
                    localName: key
                });
            }
        }, true)

        .constructor([QName], function(qname) {
            return qname;
        })

        .constructor([Object], function(model) {
            return new QName(model.shortQName || model.fullQName);
        }, false)

        .load('prefix', koutils.bulkLoad(Citeck.utils.nsPrefixLoader, 'uri', 'prefix'))
        .load('uri', koutils.bulkLoad(Citeck.utils.nsURILoader, 'prefix', 'uri'))

        ;

    //  for file-upload control
    ContentFileImpl
        .key('nodeRef', s)
        .property('filename', s)
        .property('encoding', s)
        .property('content', s)
        .property('mimetype', s)
        .property('size', n)

        .computed('jsonValue', function() {
            return {
                url: this.nodeRef()
            }
        })

        // get content
        .load('content', function(content) {
            var nodeRef = content.nodeRef();
            YAHOO.util.Connect.asyncRequest(
                'GET',
                Alfresco.constants.PROXY_URI + "citeck/print/content?nodeRef=" + content.nodeRef(),
                {
                    success: function(response) {
                        var result = response.responseText;
                        this.content(result);
                    },

                    failure: function(response) {
                        // error
                    },

                    scope: this
                }
            );
        })

        // get properties
        .load(['filename', 'encoding', 'mimetype', 'size'], koutils.simpleLoad({
            url: Alfresco.constants.PROXY_URI + "citeck/node-content?nodeRef={nodeRef}",
            resultsMap: {
                filename: 'name',
                encoding: 'content.encoding',
                mimetype: 'content.mimetype',
                size: 'content.size'
            }
        }))
        ;

    // for textarea control
    ContentTextImpl
        .constructor([String], function(content) {
            var self = ContentTextImpl.call(this);
            self.model({
                content: content,
                mimetype: "text/plain",
                encoding: "UTF-8"
            })
            return self;
        })

        .property('nodeRef', s)
        .property('encoding', s)
        .property('content', s)
        .property('mimetype', s)
        .computed('size', function() {
            var content = this.content();
            return content ? content.length : 0;
        })

        .computed('jsonValue', function() {
            return {
                mimetype: this.mimetype(),
                encoding: this.encoding(),
                content: this.content(),
                size: this.size()
            }
        })

        ;

    ContentFakeImpl
        .property('filename', s)
        .property('encoding', s)
        .property('mimetype', s)
        .property('size', n)

        .constant('jsonValue', null)
        ;

    Content
        .property('impl', o)

        .constructor([String], function(string) {
            var self = Content.call(this);
            if ($isNodeRef(string)) {
                self.impl(new ContentFileImpl(string));
            } else {
                self.impl(new ContentTextImpl(string));
            }

            return self;
        })

        .constructor([Object], function(object) {
            var self = Content.call(this);

            if (object) {
                if (object.url) {
                    self.impl(new ContentFileImpl(object.url));
                } else  if (object.content) {
                    self.impl(new ContentTextImpl(string));
                } else  if (object.filename) {
                    self.impl(new ContentFakeImpl(object));
                }
            }

            return self;
        })

        .constructor([Node], function(node) {
            var self = Content.call(this);
            self.impl(new ContentFileImpl(node.nodeRef));
            return self;
        })

        .nativeProperty('filename', {
            get: function() {
                return this.impl().filename();
            }
        })
        .nativeProperty('nodeRef', {
            get: function() {
                return this.impl().nodeRef();
            }
        })


        //  content api
        .nativeProperty('content', {
            get: function() {
                return this.impl().content();
            },
            set: function(value) {
                this.impl().content(value);
            }
        })
        .nativeProperty('mimetype', {
            get: function() {
                return this.impl().mimetype();
            },
            set: function(value) {
                this.impl().mimetype(value);
            }
        })
        .nativeProperty('size', {
            get: function() {
                return this.impl().size();
            }
        })
        .nativeProperty('encoding', {
            get: function() {
                return this.impl().encoding();
            }
        })

        // download link
        .nativeProperty('downloadURL', {
            get: function() {
                return Alfresco.constants.PROXY_URI + "citeck/print/content?nodeRef=" + this.nodeRef;
            }
        })
        ;

    Runtime
        .key('key', s)
        .property('node', Node)
        .property('parent', Runtime)
        .property('invariantSet', GroupedInvariantSet)
        .property('inlineEdit', b)
        .property('loadAttributesMethod', s)
        .property('loadGroupIndicator', b)
        .property('_loading', b)

        .constant('rootObjects', rootObjects)

        .shortcut('inSubmitProcess', 'node.impl.inSubmitProcess')

        .computed('loaded', function() {
            if (this.node.loaded() && this.node().impl.loaded() && this.node().impl().attributes.loaded()) {
              var attributes = this.resolve("node.impl.attributes");
              for (var a = 0; a < attributes.length; a++) {
                for (var prop in attributes[a]) {
                  if (ko.isObservable(!attributes[a][prop])) {
                    if (!attributes[a][prop].loaded()) return false;
                  }
                }
              }

              // delay while controls are loading (2 seconds enough)
              if (this._loading()) { setTimeout(function(scope) { scope._loading(false); }, 2000, this); }
              return !this._loading();
            }

            return false;
        })

        .computed('invalid', function() {
            return !this.node().properties["invariants:isDraft"] && this.resolve('node.impl.invalid');
        })

        .load('loadAttributesMethod', function() { this.loadAttributesMethod("default"); })
        .load('loadGroupIndicator', function() { this.loadGroupIndicator(false); })

        .method('scrollToFormField', function(data, event) {
            var field = $(".form-field[data-attribute-name='" + data.name() + "']");
            if (!data.inlineEditVisibility()) data.inlineEditVisibility(true);
            if (field) $('html,body').animate({ scrollTop: field.offset().top - 10 }, 500 );
        })

        .method('deleteNode', function(nodeRef, callback) {
            YAHOO.util.Connect.asyncRequest('DELETE', Alfresco.constants.PROXY_URI + "citeck/node?nodeRef=" + nodeRef, callback);
        })

        .method('selectGroup', function(data, event) {
            var tabId = $(event.target).attr("data-tab-id"),
                tabIndex = $(event.target).attr("data-tab-index");

            var tab = $(".tab-title[data-tab-id=" + tabId + "]"),
                body = $(".tab-body[data-tab-id=" + tabId + "]");

            if (tab.attr("data-activity") == "false") return false;

            $(event.target)
                .parent()
                .children()
                .removeClass("selected")
                .end().end()
                .addClass("selected");

            $(".tabs-body .tab-body[data-tab-id=" + tabId + "]")
                .parent()
                .children()
                .addClass("hidden")
                .end().end()
                .removeClass("hidden");

            if (this.runtime().loadAttributesMethod() == "clickOnGroup") {
                var group = this.node().impl().group(tabId);

                if (this.runtime().loadGroupIndicator() && tabIndex > 0) {
                    var self = this,
                        indicatorId = tabId + "-loadGroupIndicator", bodyId = $(".tabs-body .tab-body[data-tab-id=" + tabId + "]").attr("id"),
                        buttons = $(".invariants-form .form-buttons");

                    if (!window[indicatorId]) {
                        buttons.hide();

                        window[indicatorId] = new Citeck.UI.waitIndicator(indicatorId, {
                            context: bodyId,
                            backgroundColor: "#f0f0f0"
                        });

                        window[indicatorId].handler = ko.computed(function() {
                            var impl = self.resolve("node.impl"), groupAttributes = group.attributes();
                            return _.every(groupAttributes, function(attribute) { return !!impl.attribute(attribute);  });
                        });

                        window[indicatorId].handler.subscribe(function(newValue) {
                            if (newValue) {
                                window[indicatorId].hide();
                                window[indicatorId].handler.dispose();

                                // indicator was finished
                                window[indicatorId] = true;

                                buttons.show();
                            }
                        });

                        window[indicatorId].show();
                    }
                }

                this.runtime().loadGroupInvariants(group);
                this.runtime().loadGroupAttributes(group);
            }
        })


        .method('loadGroupAttributes', function(group) {
            if (group && !this.checkGroupAttributes(group.attributes())) {
                this.node().impl().forcedAttributes(_.union(this.resolve("node.impl.forcedAttributes"), group.attributes()));
            }
        })
        .method('loadGroupInvariants', function(group) {
            if (group && !this.checkGroupInvariants(group.invariants())) {
                this.invariantSet().forcedInvariants(_.union(this.resolve("invariantSet.forcedInvariants"), group.invariants()));
            }
        })

        .method('checkGroupAttributes', function(attributes) {
            return _.every(attributes, function(attribute) {
                return this.resolve("node.impl.forcedAttributes").indexOf(attribute) != -1;
            }, this);
        })
        .method('checkGroupInvariants', function(invariants) {
            return _.every(invariants, function(invariant) {
                return this.resolve("invariantSet.forcedInvariants").indexOf(invariant) != -1;
            }, this);
        })


        .method('scrollGroups', function(data, event) {
            var scrollArrow = $(event.target),
                direction = (function() {
                    var matches = scrollArrow.attr("class").match(/scroll-(left|right)/);
                    return matches.length > 0 ? matches[1] : undefined;
                })();

            if (direction) {
                var list = $("ul", scrollArrow.parent());
                if (direction == "right") 
                    list.animate({ scrollLeft: list.scrollLeft() + 100 }, 300);
                if (direction == "left") 
                    list.animate({ scrollLeft: list.scrollLeft() - 100 }, 300);
            }

            return false;
        })


        .method('submit', function() {
            if(this.node().impl().valid()) {
                if (this.node().hasAspect("invariants:draftAspect")) {
                    this.node().impl().isDraft(false);
                }

                this.broadcast('node-view-submit');
            }
        })
        .method('submitDraft', function() {
            if (this.node().hasAspect("invariants:draftAspect")) {
                this.node().impl().isDraft(true);
                this.broadcast('node-view-submit');
            }
        })
        .method('cancel', function() {
            this.broadcast('node-view-cancel');
        })
        .method('broadcast', function(eventName) {
            YAHOO.Bubbling.fire(eventName, {
                key: this.key(),
                runtime: this,
                node: this.node()
            });
        })

        .init(function() {
            this._loading(true);


        })
        ;


    // performance tuning
    var rateLimit = { rateLimit: { timeout: 0, method: "notifyWhenChangesStop" } };

    Attribute.extend('*', rateLimit);
    AttributeInfo.extend('*', rateLimit);
    DDClass.extend('attributes', rateLimit);
    NodeImpl.extend('type', rateLimit);
    NodeImpl.extend('_attributes', rateLimit);

    GroupedInvariantSet.extend('invariants', rateLimit);
    NodeImpl.extend('attributes', rateLimit);

    MultiClassInvariantSet.extend('invariants', { rateLimit: { timeout: 250, method: "notifyWhenChangesStop" } })

    // create common attributes statically:
    _.each({
        "attr:aspects": "d:qname",
        "attr:noderef": "d:noderef",
        "attr:types": "d:qname",
        "attr:parent": "d:noderef",
        "attr:parentassoc": "d:qname",
        "cm:name": "d:text",
        "cm:created": "d:datetime",
        "cm:creator": "d:text",
        "cm:modified": "d:datetime",
        "cm:modifier": "d:text",
        "cm:accessed": "d:datetime",
        "cm:title": "d:mltext",
        "cm:description": "d:mltext",
        "cm:content": "d:content",
        "cm:owner": "d:text",
        "sys:store-protocol": "d:text",
        "sys:store-identifier": "d:text",
        "sys:node-uuid": "d:text",
        "sys:node-dbid": "d:long",
        "sys:locale": "d:locale",
    }, function(type, name) {
        new AttributeInfo({
                "name": name,
                "type": "property",
                "datatype": type,
                "nodetype": datatypeNodetypeMapping[type] || null,
                "javaclass": datatypeClassMapping[type]
            });
    });

    // create common aspects statically
    _.each({
        "cm:auditable": [ "cm:created", "cm:creator", "cm:modified", "cm:modifier", "cm:accessed" ],
        "sys:localized": [ "sys:locale" ],
        "sys:referenceable": [ "sys:store-protocol", "sys:store-identifier", "sys:node-uuid", "sys:node-dbid", "attr:noderef" ],
        "sys:incomplete": [],
        "cm:ownable": ["cm:owner"],
    }, function(attributes, name) {
        new DDClass({
            name: name,
            qname: name,
            isAspect: true,
            attributes: attributes
        })
    });

    // create common types statically
    _.each({
        "sys:base": [ "attr:types", "attr:aspects", "attr:parent", "attr:parentassoc" ],
        "cm:cmobject": ["cm:name"],
        "cm:content": ["cm:content"],
        "cm:folder": ["cm:contains"],
        "cm:authority": [],
        "cm:authorityContainer": ["cm:authorityName", "cm:authorityDisplayName", "cm:member"],
        "cm:category": ["cm:subcategories"],
        "dl:dataListItem": []
    }, function(attributes, name) {
        new DDClass({
            name: name,
            qname: name,
            isAspect: false,
            attributes: attributes
        })
    });

    var InvariantsRuntime = function(htmlid, runtimeKey) {
        InvariantsRuntime.superclass.constructor.call(this, "Citeck.invariants.InvariantsRuntime", htmlid);
        this.runtime = new Runtime(runtimeKey);
    };

    YAHOO.extend(InvariantsRuntime, Alfresco.component.Base, {

        onReady: function() {
            var self = this;

            if (!Citeck.mobile.isMobileDevice() && $(".template-tabs").length > 0) {
                $(window).resize(function() {
                   $.each($(".template-tabs .tabs-title", Dom.get(this.id)), function(it, tabTitle) {
                        var ulWidth = parseInt($("ul", tabTitle).innerWidth()),
                            lisWidth = 0;

                        $.each($("li", tabTitle), function(il, li) {
                            lisWidth += $(li).innerWidth() + (/left|right/.test($(li).css("float")) ? parseInt($(li).css("margin-right")) : 4);
                        });

                        if (lisWidth > ulWidth) {
                            $("span.scroll-tabs", tabTitle).removeClass("hidden");
                        } else { $("span.scroll-tabs", tabTitle).addClass("hidden"); }
                    }); 
                });

                $(window).resize();
            }

            // define attributes from first group as forced
            if (this.options.model.loadAttributesMethod == "clickOnGroup") {
                this.options.model.invariantSet.forcedInvariants = this.options.model.node.groups[0].invariants;
                this.options.model.node.forcedAttributes = this.options.model.node.groups[0].attributes;
            }

            // define invariants
            if ((!this.options.model.invariantSet.forcedInvariants || this.options.model.invariantSet.forcedInvariants.length < 1) && 
                (!this.options.model.node.classNames || this.options.model.node.classNames.length < 1)) {
                var invariantsURL = Alfresco.constants.PROXY_URI + "/citeck/invariants";
                if (this.options.model.node.nodeRef) { 
                    invariantsURL += "?nodeRef=" + this.options.model.node.nodeRef;
                    if (this.options.model.inlineEdit) invariantsURL += "&inlineEdit=true"
                } else if (this.options.model.node.type) { invariantsURL += "?type=" + this.options.model.node.type; }
                invariantsURL += "&attributes=" + this.options.model.node.forcedAttributes.join(",");

                Alfresco.util.Ajax.jsonGet({
                    url: invariantsURL,
                    successCallback: {
                        scope: this,
                        fn: function (response) {
                            this.options.model.invariantSet.forcedInvariants = response.json.invariants;
                            this.options.model.node.classNames = response.json.classNames;

                            for (var name in response.json.model) {
                                this.options.model.node.defaultModel[name] = response.json.model[name];
                            }

                            this.initRuntime();
                        }
                    }
                });
            } else {
                this.initRuntime();
            }

            if (this.options.model.inlineEdit) {
                $("body").click(function(e, a) {
                    var node = self.runtime.node(),
                        isDraft = node.properties["invariants:isDraft"],
                        form = $("#" + self.options.model.key),
                        inlineEditingAttributes = node.impl()._filterAttributes("inlineEditVisibility");

                    if (!form.is(e.target) && form.has(e.target).length == 0 && inlineEditingAttributes.length) {
                        if (isDraft || node.resolve("impl.valid")) {
                            // save node if it valid
                            if (_.any(inlineEditingAttributes, function(attr) {
                                return attr.newValue() != null && attr.newValue() != attr.persistedValue();
                            })) node.thisclass.save(node, { });
                            
                            // close all valid inline editing attributes
                            _.each(node.impl().attributes(), function(attr) {
                                if (attr.inlineEditVisibility() && (isDraft || attr.valid())) attr.inlineEditVisibility(false);
                            });
                        } else {
                            Alfresco.util.PopupManager.displayMessage({
                                text: Alfresco.util.message("message.invalid-node.form")
                            });   
                        }
                    }
                });
            }
        },

        initRuntime: function() {
            koutils.enableUserPrompts();
            this.runtime.model(this.options.model);
            ko.applyBindings(this.runtime, Dom.get(this.id));
        }

    });

    return InvariantsRuntime;
})