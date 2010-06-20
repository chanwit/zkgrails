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

includeTargets << grailsScript("_GrailsInit")
includeTargets << grailsScript("_GrailsCreateArtifacts")

target ('default': "Creates a new zul page") {
    depends(checkVersion, parseArguments)

    def gsp = ctx.getBean("groovyPagesTemplateEngine");

    byte[] buffer = new byte[(int)file.length()];
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file));
    bis.read(buffer);
    
    String encoding = (String)ConfigurationHolder.getFlatConfig().get(CONFIG_OPTION_GSP_ENCODING);
    if(encoding == null) encoding = UTF_8_ENCODING;        
    
    String bufferStr = new String(buffer, encoding);
    bufferStr = bufferStr.replaceAll("@\\{", "\\$\\{'@'\\}\\{");        

    def template = gsp.createTemplate(new ByteArrayResource(bufferStr.getBytes(encoding)));

    def w = template.make();
    def sw = new StringWriter();
    w.writeTo(new PrintWriter(sw));
}