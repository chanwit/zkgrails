/*
 * Copyright 2007-2009 the original author or authors.
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
 * Gant script that generate scaffolding for ZK from a Domain class
 *
 * @author Chanwit Kaewkasi
 *
 * @since 1.0-M5
 */

import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.commons.GrailsResourceUtils
import org.springframework.core.io.ByteArrayResource

import org.codehaus.groovy.grails.scaffolding.*

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")
includeTargets << grailsScript("_GrailsBootstrap")


target(generateForOne: "Generates controllers and views for only one domain class.") {
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

def generateForDomainClass(domainClass) {
    def templateGenerator = new DefaultGrailsTemplateGenerator(classLoader)
    if(generateViews) {
        event("StatusUpdate", ["Generating views for domain class ${domainClass.fullName}"])
        templateGenerator.generateViews(domainClass, basedir)
        event("GenerateViewsEnd", [domainClass.fullName])
    }
    if(generateController) {
        event("StatusUpdate", ["Generating controller for domain class ${domainClass.fullName}"])
        templateGenerator.generateController(domainClass, basedir)
        createUnitTest(name: domainClass.fullName, suffix: "Controller", superClass: "ControllerUnitTestCase")
        event("GenerateControllerEnd", [domainClass.fullName])
    }
}

target ('default': "Creates a new zul page") {
    depends(checkVersion, parseArguments)

    def gsp = ctx.getBean("groovyPagesTemplateEngine");

    byte[] buffer = new byte[(int)file.length()];
    BufferedInputStream bis = new BufferedInputStream(
        new FileInputStream("$zkPluginDir/src/templates/artifacts/scaffold_gsp.zul");
    )
    bis.read(buffer);
    
    String encoding = (String)ConfigurationHolder.getFlatConfig().get(CONFIG_OPTION_GSP_ENCODING);
    if(encoding == null) encoding = UTF_8_ENCODING;
    String bufferStr = new String(buffer, encoding);

    bufferStr = bufferStr.replaceAll('@\\{', '\\$\\{\'@\'\\}\\{');        

    def template = gsp.createTemplate(new ByteArrayResource(bufferStr.getBytes(encoding)));

    def w = template.make([test: "hello"])
    def sw = new StringWriter()
    w.writeTo(new PrintWriter(sw))
    println sw.toString()
}

