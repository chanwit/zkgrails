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
 *
 * @since 0.4
 */

import org.zkoss.zkgrails.scaffolding.*
import grails.util.GrailsNameUtils

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
