/*
 * Copyright 2010 the original author or authors.
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

import org.zkoss.zkgrails.scaffolding.*
import grails.util.GrailsNameUtils


/**
 * Gant script that generates a CRUD composer and zul for a given domain class
 *
 * @author Graeme Rocher
 * @author Chanwit Kaewkasi
 *
 * @since 1.0-M5
 */

includeTargets << grailsScript("_GrailsBootstrap")

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
