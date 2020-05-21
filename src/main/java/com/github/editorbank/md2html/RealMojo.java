package com.github.editorbank.md2html;

import jdk.nashorn.tools.Shell;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.LifecyclePhase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Properties;


/**
 * Goal which converting MarkDown files to HTML format.
 *
 * @version $Id$
 */

@Mojo(name = "md2html",
        requiresProject = false,
        threadSafe = true,
        defaultPhase = LifecyclePhase.GENERATE_SOURCES,
        executionStrategy = "always"
)
public class RealMojo extends AbstractMojo {
    private static final String PLUGIN_NAME = "mdProcessor";

    private static final String SCRIPT_EXTENSION_DEFAULT = ".js";
    private static final String PLUGIN_MAIN_SCRIPT = PLUGIN_NAME+SCRIPT_EXTENSION_DEFAULT;

    private static final String SOURCE_DEFAULT = "${basedir}/src/md";
    private static final String DESTINATION_DEFAULT = "${project.build.directory}";
    private static final String TEMPLATE_DEFAULT = "md2html.tmpl.j2";

    /**
     * This is where source location directory.
     */
    @Parameter(property="md2html.source", defaultValue= SOURCE_DEFAULT)
    private String source;

    /**
     * This is where destination location directory.
     */
    @Parameter(property="md2html.destination", defaultValue=DESTINATION_DEFAULT)
    private String destination;

    /**
     * This is where template HTML.
     */
    @Parameter(property="md2html.template")
    private String template;


    /**
     * Disables the plugin execution.
     */
    @Parameter( property = "md2html.skip", defaultValue = "false" )
    private boolean skip;

    private ClassLoader classLoader = null;
    private void initClassLoader() {
        if(classLoader!=null) return;
        classLoader = getClass().getClassLoader();
    }

    private Properties properties = null;
    private void initProperties() {
        if(properties!=null) return;
        try {
            initClassLoader();
            InputStream inputStream = classLoader.getResourceAsStream(PLUGIN_NAME+".properties");
            properties = new Properties();
            properties.load(inputStream);
        } catch (IOException e) {
            getLog().error(e);
        }
    }

    private String getTempDirWithCopiedResources() throws IOException {
        initProperties();
        String listSeparator = properties.getProperty("listSeparator",",");
        String[] resoucesList = properties.getProperty("resourcesList","").split(listSeparator);

        File tempDir = Files.createTempDirectory(PLUGIN_NAME).toFile();
        tempDir.deleteOnExit();

        for (String resouceName : resoucesList){
            File ofile = new File(tempDir, resouceName);
            ofile.deleteOnExit();
            getLog().info("Resource \"" + resouceName + "\" copy to file \"" + ofile.toString() + "\"...");
            try (InputStream is = classLoader.getResourceAsStream(resouceName)) {
                try (OutputStream os = new FileOutputStream(ofile)) {
                    int read;
                    byte[] bytes = new byte[1024];

                    while ((read = is.read(bytes)) != -1) {
                        os.write(bytes, 0, read);
                    }
                }
            }
        }
        return tempDir.toString();
    }

    public void execute() throws MojoExecutionException {

        if (skip) {
            return;
        }
        initProperties();
        String scriptFile = "UNKNOWN";
        try {
            String resoucesDir = getTempDirWithCopiedResources();
            if(template==null) template = resoucesDir + File.separator + TEMPLATE_DEFAULT;
            scriptFile = resoucesDir + File.separator + PLUGIN_MAIN_SCRIPT;
            String[] args = {
                    "-scripting",
                    "-classpath",
                    resoucesDir,
                    scriptFile,
                    "--",
                    source,
                    destination,
                    template
            };
            getLog().info("Run jdk.nashorn.tools.Shell.main(\"" + String.join("\",\n\"", args) + "\" ) ...");
            Shell.main(args);

        } catch (Exception e) {
            throw new MojoExecutionException("Runtime error in script \"" + scriptFile +"\": ", e);
        }
    }
}
