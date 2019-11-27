package org.codehaus.mojo.appassembler.daemon.script;

/*
 * The MIT License
 *
 * Copyright (c) 2006-2012, The Codehaus
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import org.apache.commons.io.IOUtils;
import org.codehaus.mojo.appassembler.daemon.DaemonGenerationRequest;
import org.codehaus.mojo.appassembler.daemon.DaemonGenerator;
import org.codehaus.mojo.appassembler.daemon.DaemonGeneratorException;
import org.codehaus.mojo.appassembler.model.Daemon;
import org.codehaus.mojo.appassembler.util.XmlPlexusConfigurationWriter;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;

import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * The abstract script daemon generator which contains all common parameters and methods for
 * AbstractBooterDaemonGenerator, UnixScriptDaemonGenerator and WindowsScriptDaemonGenerator
 *
 * @author <a href="mailto:khmarbaise@soebes.de">Karl-Heinz Marbaise</a>
 */
public abstract class AbstactScriptDaemonGenerator
        implements DaemonGenerator {
    /**
     * @plexus.requirement
     */
    protected ScriptGenerator scriptGenerator;

    private final String platformName;

    private static Pattern WINDOWS_PATTERN = Pattern.compile("\"%([^\"]+)%\"");

    public AbstactScriptDaemonGenerator(String platformName) {
        this.platformName = platformName;
    }

    public String getPlatformName() {
        return platformName;
    }

    @Override
    public void generate(DaemonGenerationRequest generationRequest) throws DaemonGeneratorException {
        if (generationRequest.getLaunch4jConfigFile() != null && Platform.WINDOWS_NAME.equals(platformName)) {
            Platform platform = Platform.getInstance(platformName);

            XmlPlexusConfiguration launch4jConfig = generationRequest.getLaunch4jConfig();
            if (launch4jConfig != null) {
                Daemon daemon = generationRequest.getDaemon();

                XmlPlexusConfiguration basedirVar = new XmlPlexusConfiguration("var");
                basedirVar.setValue("BASEDIR=%EXEDIR%" + platform.getSeparator() + "..");
                launch4jConfig.addChild(basedirVar);

                XmlPlexusConfiguration repoVar = new XmlPlexusConfiguration("var");
                repoVar.setValue("REPO=%BASEDIR%" + platform.getSeparator() + "lib");
                launch4jConfig.addChild(repoVar);

                // cmdLine
                XmlPlexusConfiguration cmdLine = (XmlPlexusConfiguration) launch4jConfig.getChild("cmdLine");
                cmdLine.setValue(platform.getAppArguments(daemon));

                // mainClass
                XmlPlexusConfiguration classPath = (XmlPlexusConfiguration) launch4jConfig.getChild("classPath");
                XmlPlexusConfiguration mainClass = (XmlPlexusConfiguration) classPath.getChild("mainClass");
                mainClass.setValue(daemon.getMainClass());

                // cp
                for (String s : platform.getClassPathList(daemon)) {
                    XmlPlexusConfiguration cp = new XmlPlexusConfiguration("cp");
                    cp.setValue(WINDOWS_PATTERN.matcher(s).replaceAll("%$1%"));
                    classPath.addChild(cp);
                }

                // jre
                XmlPlexusConfiguration jre = (XmlPlexusConfiguration) launch4jConfig.getChild("jre");
                XmlPlexusConfiguration appNameOpt = new XmlPlexusConfiguration("opt");
                appNameOpt.setValue("-Dapp.name=" + daemon.getId()+ "");
                jre.addChild(appNameOpt);
                XmlPlexusConfiguration appRepoOpt = new XmlPlexusConfiguration("opt");
                appRepoOpt.setValue("-Dapp.repo=%REPO%");
                jre.addChild(appRepoOpt);
                XmlPlexusConfiguration appHomeOpt = new XmlPlexusConfiguration("opt");
                appHomeOpt.setValue("-Dapp.home=%BASEDIR%");
                jre.addChild(appHomeOpt);
                XmlPlexusConfiguration basedirOpt = new XmlPlexusConfiguration("opt");
                basedirOpt.setValue("-Dbasedir=%BASEDIR%");
                jre.addChild(basedirOpt);

                FileWriter writer = null;
                try {
                    writer = new FileWriter(generationRequest.getLaunch4jConfigFile());
                    XmlPlexusConfigurationWriter xmlWriter = new XmlPlexusConfigurationWriter();
                    xmlWriter.write(launch4jConfig, writer);
                } catch (IOException e) {
                    throw new DaemonGeneratorException("Can't generate file: " + generationRequest.getLaunch4jConfigFile(), e);
                } finally {
                    if (writer != null) {
                        IOUtils.closeQuietly(writer);
                    }
                }
            }
        }
    }
}
