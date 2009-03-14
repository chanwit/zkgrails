class ZkGrailsPlugin {
    // the plugin version
    def version = "0.7"
    // the version or versions of Grails the plugin is designed for
    def grailsVersion = "1.1 > *"
    // the other plugins this plugin depends on
    def dependsOn = [:]
    // resources that are excluded from plugin packaging
    def pluginExcludes = [
            "grails-app/views/error.gsp"
    ]

    // TODO Fill in these fields
    def author = "chanwit"
    def authorEmail = "chanwit@gmail.com"
    def title = "ZK 3.6.0 for Grails"
    def description = '''\\
Derived from Flyisland ZK Grails Plugin,
this plugin add ZK Ajax framework (www.zkoss.org)
support to Grails applications.
'''

    // URL to the plugin's documentation
    def documentation = "http://grails.org/Zk+Plugin"

    def doWithSpring = {
        // TODO Implement runtime spring config (optional)
    }

    def doWithApplicationContext = { applicationContext ->
        // TODO Implement post initialization spring config (optional)
    }

    def doWithWebDescriptor = { xml ->
        def urls = ["*.zul", "*.zhtml", "*.svg", "*.xml2html"]

        // adding GrailsOpenSessionInView
        def filterElements = xml.'filter'[0]
        filterElements + {
            'filter' {
                'filter-name' ("GOSIVFilter")
                'filter-class' ("org.codehaus.groovy.grails.orm.hibernate.support.GrailsOpenSessionInViewFilter")
            }
        }

        // filter for each ZK urls
        def filterMappingElements = xml.'filter-mapping'[0]
        ["*.zul", "/zkau/*"].each {p ->
            filterMappingElements + {
                'filter-mapping' {
                    'filter-name'("GOSIVFilter")
                    'url-pattern'("${p}")
                }
            }
        }

        // quick hack for page filtering
        def pageFilter = xml.filter.find { it.'filter-name' == 'sitemesh'}
        pageFilter.'filter-class'.replaceBody('org.zkoss.zkgrails.ZKGrailsPageFilter')

        def listenerElements = xml.'listener'[0]
        listenerElements + {
            'listener' {
                'display-name' ("ZK Session Cleaner")
                'listener-class' ("org.zkoss.zk.ui.http.HttpSessionListener")
            }
        }

        def servletElements = xml.'servlet'[0]
        def mappingElements = xml.'servlet-mapping'[0]

        servletElements + {
            'servlet' {
                'servlet-name' ("zkLoader")
                'servlet-class' ("org.zkoss.zk.ui.http.DHtmlLayoutServlet")
                'init-param' {
                    'param-name' ("update-uri")
                    'param-value' ("/zkau")
                }
                'load-on-startup' (0)
            }
        }

        urls.each {p ->
            mappingElements + {
                'servlet-mapping' {
                    'servlet-name'("zkLoader")
                    'url-pattern'("${p}")
                }
            }
        }

        servletElements + {
            'servlet' {
                'servlet-name' ("auEngine")
                'servlet-class' ("org.zkoss.zk.au.http.DHtmlUpdateServlet")
            }
        }
        mappingElements + {
            'servlet-mapping' {
                'servlet-name'("auEngine")
                'url-pattern'("/zkau/*")
            }
        }
    }

    def doWithDynamicMethods = { ctx ->        
        org.zkoss.zk.ui.AbstractComponent.metaClass.append = { closure ->
            closure.delegate = new ZkBuilder(parent: delegate)
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure.call()
        }
    }

    def onChange = { event ->
        // TODO Implement code that is executed when any artefact that this plugin is
        // watching is modified and reloaded. The event contains: event.source,
        // event.application, event.manager, event.ctx, and event.plugin.
    }

    def onConfigChange = { event ->
        // TODO Implement code that is executed when the project configuration changes.
        // The event is the same as for 'onChange'.
    }
}
