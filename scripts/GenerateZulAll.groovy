/*
 * Copyright 2004-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Gant script that generates a CRUD controller and matching views for a given domain class
 * 
 * @author Graeme Rocher
 * @author Chanwit Kaewkasi
 *
 * @since 1.0-M5
 */

import org.zkoss.zkgrails.scaffolding.*
import grails.util.GrailsNameUtils

import grails.util.BuildSettingsHolder
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator
import org.codehaus.groovy.grails.scaffolding.GrailsTemplateGenerator
import org.springframework.context.ResourceLoaderAware
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.codehaus.groovy.grails.cli.CommandLineHelper
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

/**
TODO
====
$ grails generate-zul-all zk.User

 1. Change lstuser to lstUser. Now we're using ${dc.propertyName}, need change.
 2. Several kinds of editors.
 3. Many-to-one

**/

class DefaultZKGrailsTemplateGenerator implements ResourceLoaderAware {

    static final Log LOG = LogFactory.getLog(DefaultZKGrailsTemplateGenerator.class)

    String basedir = "."
    boolean overwrite = false
    def engine = new SimpleTemplateEngine()
    ResourceLoader resourceLoader
    Template renderEditorTemplate
    String domainSuffix = 'Instance'

    def componentNames = []


    //
    // Used by the scripts so that they can pass in their AntBuilder
    // instance.
    //
    DefaultZKGrailsTemplateGenerator(ClassLoader classLoader) {
        engine = new SimpleTemplateEngine(classLoader)        
	    def suffix = ConfigurationHolder.config?.grails?.scaffolding?.templates?.domainSuffix
	    if (suffix != [:]) {
	        domainSuffix = suffix
		}
    }

    //
    // Creates an instance
    //
    DefaultZKGrailsTemplateGenerator() { }

    void setResourceLoader(ResourceLoader rl) {
        LOG.info "Scaffolding template generator set to use resource loader ${rl}"
        this.resourceLoader = rl
    }

    // a closure that uses the type to render the appropriate editor
    def renderEditor = {property ->
        def domainClass = property.domainClass
        def cp = domainClass.constrainedProperties[property.name]

        if (!renderEditorTemplate) {
            // create template once for performance
            def templateText = getTemplateText("renderZulEditor.template")
            renderEditorTemplate = engine.createTemplate(templateText)
        }

        def propName = org.apache.commons.lang.StringUtils.capitalize(property.name)
        def binding = [property: property,
                       propName: propName,
                       domainClass: domainClass,
                       cp: cp,
                       //
                       // a list for accumulating component names
                       //
                       componentNames: componentNames,
                       domainInstance: getPropertyName(domainClass)
                      ]

        return renderEditorTemplate.make(binding).toString()
    }

    public void generateZul(GrailsDomainClass domainClass, String destdir) {
        if (!destdir)
            throw new IllegalArgumentException("Argument [destdir] not specified")

        def zulDir = new File("${destdir}/web-app/${domainClass.propertyName}")
        if (!zulDir.exists())
            zulDir.mkdirs()
        File destFile = new File("${zulDir.absolutePath}/index.zul")
        if (canWrite(destFile)) {
            destFile.withWriter {Writer writer ->
                generateZul domainClass, writer
            }
        }
    }

    void generateZul(GrailsDomainClass domainClass, Writer out) {
        def templateText = getTemplateText("index.zul")

        def t = engine.createTemplate(templateText)
        def multiPart = domainClass.properties.find {it.type == ([] as Byte[]).class || it.type == ([] as byte[]).class}

        def packageName = domainClass.packageName ? domainClass.packageName + "." : ""
        def binding = [packageName: packageName,
                domainClass: domainClass,
                multiPart: multiPart,
                className: domainClass.shortName,
                propertyName:  getPropertyName(domainClass),
                renderEditor: renderEditor,
                comparator: org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator.class]

        t.make(binding).writeTo(out)
    }    

    public void generateComposer(GrailsDomainClass domainClass, String destdir) {
        println "generate 1 composer"
    }
    
    private getTemplateText(String template) {
        def application = ApplicationHolder.getApplication()
        def pluginManager = PluginManagerHolder.getPluginManager()        
        // first check for presence of template in application
        if (resourceLoader && application?.warDeployed) {
            return resourceLoader.getResource("/WEB-INF/templates/scaffolding/${template}").inputStream.text
        }
        else {
            def templateFile = new FileSystemResource("${basedir}/src/templates/scaffolding/${template}")
            if (!templateFile.exists()) {
                if(pluginManager.hasGrailsPlugin("zk")) {
                    def zkPluginDir = pluginManager.getGrailsPlugin("zk").getPluginPath()
                    templateFile = new FileSystemResource("${zkPluginDir}/src/templates/scaffolding/${template}")
                } else {
                    templateFile = new ClassPathResource("src/grails/templates/scaffolding/${template}")
                }
                /*
                // template not found in application, use default template
                def grailsHome = BuildSettingsHolder.settings?.grailsHome

                if (grailsHome) {
                    templateFile = new FileSystemResource("${grailsHome}/src/grails/templates/scaffolding/${template}")
                }
                else {
                    templateFile = new ClassPathResource("src/grails/templates/scaffolding/${template}")
                }
                */
            }
            return templateFile.inputStream.getText()
        }
    }

    private String getPropertyName(GrailsDomainClass domainClass) {
        return "${domainClass.propertyName}${domainSuffix}"
    }

    private helper = new CommandLineHelper()
    private canWrite(testFile) {
        if (!overwrite && testFile.exists()) {
            try {
                def response = helper.userInput("File ${testFile} already exists. Overwrite?",['y','n','a'] as String[])
                overwrite = overwrite || response == "a"
                return overwrite || response == "y"
            }
            catch (Exception e) {
                // failure to read from standard in means we're probably running from an automation tool like a build server
                return true
            }
        }
        return true
    }
    
}

includeTargets << grailsScript("_GrailsBootstrap")
includeTargets << grailsScript("_GrailsCreateArtifacts")

generateForName = null
generateZul = true
generateComposer = true


target(generateForOne: "Generates composer and zul for only one domain class.") {
    depends(loadApp)

    def name = generateForName
    name = name.indexOf('.') > -1 ? name : GrailsNameUtils.getClassNameRepresentation(name)
    def domainClass = grailsApp.getDomainClass(name)

    if(!domainClass) {
        println "Domain class not found in grails-app/domain, trying hibernate mapped classes..."
        bootstrap()
        domainClass = grailsApp.getDomainClass(name)
    }

    if(domainClass) {
        generateForDomainClass(domainClass)
        event("StatusFinal", ["Finished generation for domain class ${domainClass.fullName}"])
    }
    else {
        event("StatusFinal", ["No domain class found for name ${name}. Please try again and enter a valid domain class name"])
    }
}

target(uberGenerate: "Generates controllers and views for all domain classes.") {
    depends(loadApp)

    def domainClasses = grailsApp.domainClasses

    if (!domainClasses) {
        println "No domain classes found in grails-app/domain, trying hibernate mapped classes..."
        bootstrap()
        domainClasses = grailsApp.domainClasses
    }

   if (domainClasses) {
        domainClasses.each { domainClass ->
            generateForDomainClass(domainClass)
        }
        event("StatusFinal", ["Finished generation for domain classes"])
    }
    else {
        event("StatusFinal", ["No domain classes found"])
    }
}


def generateForDomainClass(domainClass) {
    def templateGenerator = new DefaultZKGrailsTemplateGenerator(classLoader)
    if(generateZul) {
        event("StatusUpdate", ["Generating zul for domain class ${domainClass.fullName}"])
        templateGenerator.generateZul(domainClass, basedir)
        event("GenerateZulEnd", [domainClass.fullName])
    }
    if(generateComposer) {
        event("StatusUpdate", ["Generating composer for domain class ${domainClass.fullName}"])
        templateGenerator.generateComposer(domainClass, basedir)
        // TODO
        // createUnitTest(name: domainClass.fullName, suffix: "Composer", superClass: "ComposerUnitTestCase")
        event("GenerateComposerEnd", [domainClass.fullName])
    }
}


target ('default': "Generates a ZK CRUD interface (composer + zul) for a domain class") {
    depends( checkVersion, parseArguments, packageApp )
    promptForName(type: "Domain Class")

    try {
        def name = argsMap["params"][0]
        if (!name || name == "*") {
            uberGenerate()
        }
        else {
            generateForName = name
            generateForOne()
        }
    }
    catch(Exception e) {
        logError("Error running generate-all", e)
        exit(1)
    }
}
