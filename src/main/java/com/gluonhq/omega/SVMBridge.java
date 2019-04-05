/*
 * Copyright (c) 2019, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.omega;

import com.gluonhq.omega.target.AbstractTargetConfiguration;
import com.gluonhq.omega.target.LinuxTargetConfiguration;
import com.gluonhq.omega.target.MacosTargetConfiguration;
import com.gluonhq.omega.util.FileOps;
import com.oracle.svm.core.SubstrateUtil;
import com.oracle.svm.driver.NativeImage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class SVMBridge {

    public static String OMEGADEPSROOT;
    public static String JFXSDK;
    private static boolean USE_JAVAFX;
    private static boolean USE_LLVM;

    private static final List<String> CUSTOM_REFLECTION_LIST = new ArrayList<>();
    private static final List<String> CUSTOM_JNI_LIST = new ArrayList<>();
    private static final List<String> CUSTOM_DELAY_INIT_LIST = new ArrayList<>();

    static List<String> classPath;
    static List<String> modulePath;
    static List<String> upgradeModulePath;
    static List<String> runtimeArgs;

    private static Path workDir;
    private static List<Path> classDir;
    private static String mainClass;
    private static String appName;

    private static final List<String> delayClinitList = Arrays.asList(
            "javafx.scene.CssStyleHelper",
            "com.sun.prism.es2.ES2VramPool",
            "com.sun.prism.es2.ES2Pipeline",
            "com.sun.javafx.iio.jpeg.JPEGImageLoader",
            "com.sun.javafx.tk.quantum.ViewPainter",
            "com.sun.javafx.tk.quantum.PresentingPainter",
            "com.sun.javafx.tk.quantum.PrismImageLoader2",
            "com.sun.javafx.tk.quantum.PrismImageLoader2$AsyncImageLoader",
            "com.sun.javafx.tk.quantum.UploadingPainter",
            "com.sun.javafx.scene.control.LabeledText",
            "com.sun.prism.impl.ps.BaseShaderContext",
            "com.sun.prism.impl.ps.PaintHelper",
            "com.sun.prism.PresentableState",
            "javafx.scene.control.Labeled$StyleableProperties",
            "javafx.scene.control.TextInputControl$StyleableProperties",
            "javafx.scene.control.TextArea$StyleableProperties",
            "javafx.scene.control.TextField$StyleableProperties",
            "javafx.scene.control.PopupControl",
            "javafx.scene.control.skin.TextInputControlSkin",
            "javafx.scene.text.Text$StyleableProperties",
            "com.sun.javafx.scene.control.Properties",
            "com.sun.javafx.scene.control.behavior.TextFieldBehavior",
            "com.sun.javafx.scene.control.behavior.TextAreaBehavior",
            "com.sun.javafx.scene.control.behavior.TextInputControlBehavior",
            "com.sun.javafx.scene.control.skin.FXVK"
    );

    private static final List<String> bundlesList = new ArrayList<>(Arrays.asList(
            "com/sun/javafx/scene/control/skin/resources/controls"
    ));

    private static AbstractTargetConfiguration config;
    private static SvmConfiguration svmConfiguration;

    private static void init() {
        Config omegaConfig = Omega.getConfig();
        SVMBridge.OMEGADEPSROOT = omegaConfig.getDepsRoot();
        SVMBridge.JFXSDK = omegaConfig.getJavaFXRoot();
        SVMBridge.USE_JAVAFX = omegaConfig.isUseJavaFX();
        SVMBridge.USE_LLVM = "llvm".equals(Omega.getConfig().getBackend());
        SVMBridge.CUSTOM_REFLECTION_LIST.addAll(omegaConfig.getReflectionList());
        SVMBridge.CUSTOM_JNI_LIST.addAll(omegaConfig.getJniList());
        SVMBridge.CUSTOM_DELAY_INIT_LIST.addAll(omegaConfig.getDelayInitList());

    }

    public static void compile(Path workingDir, List<Path> gClassdir, String className, String appName,
                               AbstractTargetConfiguration configuration) throws Exception {
        init();
        config = configuration;
        deleteDirectory(workingDir.resolve("tmp").toFile());
        workingDir.toFile().mkdir();

        mainClass = className;
        // ModuleName not supported
        if (className.contains("/")) {
            mainClass = className.substring(className.indexOf("/") + 1);
        }
        System.err.println("mainClass: " + mainClass);

        workDir = workingDir;
        System.err.println("workDir: " + workDir);

        SVMBridge.appName = appName;
        System.err.println("appName: " + SVMBridge.appName);

        classDir = gClassdir;
        System.err.println("classDir: " + classDir);

        String suffix = config instanceof LinuxTargetConfiguration ? "linux" :
                config instanceof MacosTargetConfiguration ? "mac" : "ios";
        createReflectionConfig(suffix);
        createJNIConfig(suffix);

        svmConfiguration = new SVMBridge.SvmConfiguration(config.isCrossCompile());

        setClassPath();
        setModulePath();
        setUpgradeModulePath();
        setRuntimeArgs(suffix);
    }

    public static void linkSetup() {
        init();
        if (svmConfiguration == null) {
            svmConfiguration = new SVMBridge.SvmConfiguration(false);
        }
    }

    private static void setClassPath() {
        classPath = svmConfiguration.getBuilderClasspath().stream()
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private static void setModulePath() {
        modulePath = svmConfiguration.getBuilderModulePath().stream()
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private static void setUpgradeModulePath() {
        upgradeModulePath = svmConfiguration.getBuilderUpgradeModulePath().stream()
                .map(Path::toString)
                .collect(Collectors.toList());
    }

    private static void setRuntimeArgs(String suffix) {
        String cp = svmConfiguration.getImageClasspath().stream()
                .map(Path::toString)
                .filter(s -> !s.contains("javafx-"))
                .collect(Collectors.joining(File.pathSeparator));
        try {
            String javafxJars = USE_JAVAFX ? Files.walk(Paths.get(JFXSDK + "/lib"))
                    .map(Path::toString)
                    .filter(p -> p.endsWith(".jar"))
                    .collect(Collectors.joining(File.pathSeparator)) : "";
            cp = cp + File.pathSeparator + javafxJars;
        } catch (IOException e) {
            e.printStackTrace();
        }

        String hostedNative = Omega.macHost ?
                "darwin-amd64" :
                "linux-amd64";

        runtimeArgs = new ArrayList<>(Arrays.asList(
                "-imagecp", cp,
                "-H:Path=" + workDir,
                "-H:CLibraryPath=" + Paths.get(OMEGADEPSROOT).resolve(hostedNative).toFile().getAbsolutePath(),
                "-H:Class=" + mainClass,
                "-H:ReflectionConfigurationFiles=" + workDir + "/reflectionconfig-" + suffix + ".json",
                "-H:JNIConfigurationFiles=" + workDir + "/jniconfig-" + suffix + ".json",
                "-H:+ReportExceptionStackTraces"));

        if (config.isCrossCompile() || Omega.macHost) {
            runtimeArgs.add("-H:Kind=SHARED_LIBRARY");
        }
        runtimeArgs.add("-H:TempDirectory=" + workDir.resolve("tmp").toFile().getAbsolutePath());
        if (USE_JAVAFX) {
            delayClinitList.forEach(clinit -> runtimeArgs.add("-H:DelayClassInitialization=" + clinit));
        }
        CUSTOM_DELAY_INIT_LIST.forEach(clinit -> runtimeArgs.add("-H:DelayClassInitialization=" + clinit));
        if (USE_JAVAFX) {
            config.getRerunClinitList().forEach(clinit -> runtimeArgs.add("-H:RerunClassInitialization=" + clinit));
        }
        runtimeArgs.addAll(Arrays.asList(
                "-H:NumberOfThreads=1",
                "-H:Name=" + appName,
                "-H:IncludeResources=.*/.*frag$",
                "-H:IncludeResources=.*/.*fxml$",
                "-H:IncludeResources=.*/.*css$",
                "-H:IncludeResources=.*/.*gls$",
                "-H:IncludeResources=.*/.*ttf$",
                "-H:IncludeResources=.*/.*png$",
                "-H:IncludeResources=.*fxml$",
                "-H:IncludeResources=.*png$",
                "-H:IncludeResources=.*css$",
                "-H:+ReportUnsupportedElementsAtRuntime",
                "-H:+AllowIncompleteClasspath",
                "-H:EnableURLProtocols=http,https",
                "-H:+EnableAllSecurityServices"));
        if (USE_LLVM) {
            runtimeArgs.add("-H:CompilerBackend=llvm");
            runtimeArgs.add("-H:-MultiThreaded");
            runtimeArgs.add("-H:-SpawnIsolates");
        }
    }


    private static void createReflectionConfig(String suffix) throws Exception {
        Path reflectionPath = workDir.resolve("reflectionconfig-" + suffix + ".json");
        File f = reflectionPath.toFile();
        if (f.exists()) {
            f.delete();
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
            bw.write("[\n");
            writeSingleEntry(bw, mainClass);
            if (USE_JAVAFX) {
                for (String javafxClass : config.getReflectionClassList()) {
                    writeEntry(bw, javafxClass);
                }
            }
            for (String customClass : CUSTOM_REFLECTION_LIST) {
                writeEntry(bw, customClass);
            }
            bw.write("]");
        }
    }

    private static void createJNIConfig(String suffix) throws Exception {
        Path reflectionPath = workDir.resolve("jniconfig-" + suffix + ".json");
        File f = reflectionPath.toFile();
        if (f.exists()) {
            f.delete();
        }
        try (BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f)))) {
            bw.write("[\n");
            bw.write("  {\n    \"name\" : \"" + mainClass + "\"\n  }\n");
            for (String javaClass : config.getJavaJNIClassList()) {
                writeEntry(bw, javaClass);
            }
            if (USE_JAVAFX) {
                for (String javafxClass : config.getJavaFXJNIClassList()) {
                    writeEntry(bw, javafxClass);
                }
            }
            for (String javafxClass : CUSTOM_JNI_LIST) {
                writeEntry(bw, javafxClass);
            }

            bw.write("]");
        }
    }

    private static void writeEntry(BufferedWriter bw, String javafxClass) throws Exception {
        bw.write(",\n");
        writeSingleEntry(bw, javafxClass);
    }

    private static void writeSingleEntry (BufferedWriter bw, String javafxClass) throws Exception {
        bw.write("  {\n");
        bw.write("    \"name\" : \""+javafxClass+"\",\n");
        bw.write("    \"allDeclaredConstructors\" : true,\n");
        bw.write("    \"allPublicConstructors\" : true,\n");
        bw.write("    \"allDeclaredFields\" : true,\n");
        bw.write("    \"allPublicFields\" : true,\n");
        bw.write("    \"allDeclaredMethods\" : true,\n");
        bw.write("    \"allPublicMethods\" : true\n");
        bw.write("  }\n");
    }

    private static Path getJavaHome() {
        return Paths.get(System.getProperty("java.home"));
    }

    public static List<String> getBundlesList() {
        if (USE_JAVAFX) {
            return bundlesList;
        }
        return new ArrayList<>();
    }

    private final static class SvmConfiguration implements NativeImage.BuildConfiguration {

        boolean cross;

        public SvmConfiguration(boolean cross) {
            this.cross = cross;
            getBuilderCLibrariesPaths();
        }

        @Override
        public Path getWorkingDirectory() {
            System.err.println("WorkingDir asked, return " + workDir);
            return workDir;
        }

        @Override
        public Path getJavaExecutable() {
            return getJavaHome().resolve("bin/java");
        }

        @Override
        public List<Path> getBuilderClasspath() {
            List<Path> answer = new LinkedList<>();
            if (useJavaModules()) {
                answer.add(Paths.get(OMEGADEPSROOT, "graal-sdk.jar"));
                answer.add(Paths.get(OMEGADEPSROOT, "compiler.jar"));
            }
            answer.add(Paths.get(OMEGADEPSROOT, "svm.jar"));
            answer.add(Paths.get(OMEGADEPSROOT, "objectfile.jar"));
            answer.add(Paths.get(OMEGADEPSROOT, "pointsto.jar"));

            if (USE_LLVM) {
                answer.add(Paths.get(OMEGADEPSROOT, "svm-llvm.jar"));
                answer.add(Paths.get(OMEGADEPSROOT, "graal-llvm.jar"));
                answer.add(Paths.get(OMEGADEPSROOT, "llvm-platform-specific.jar"));
                answer.add(Paths.get(OMEGADEPSROOT, "llvm-wrapper.jar"));
                answer.add(Paths.get(OMEGADEPSROOT, "javacpp.jar"));
            }
            return answer;
        }

        @Override
        public List<Path> getBuilderCLibrariesPaths() {
            String hostedNative = Omega.macHost ?
                    "svm-hosted-native-darwin-amd64.jar" :
                    "svm-hosted-native-linux-amd64.jar";
            ProcessBuilder pb = new ProcessBuilder("tar", "xvf",
                    Paths.get(OMEGADEPSROOT).resolve(hostedNative).toFile().getAbsolutePath());
            pb.directory(Paths.get(OMEGADEPSROOT).toFile());
            File f = Paths.get(OMEGADEPSROOT).toFile();
            System.err.println("dir = "+f);
            try {
                pb.redirectErrorStream(true);
                Process process = pb.start();
                FileOps.mergeProcessOutput(process.getInputStream());
                int err = process.waitFor();
                System.err.println("Result of untar = "+err);
            } catch (Exception e) {
                e.printStackTrace();
            }
            List<Path> answer = new LinkedList<>();
            answer.add(Paths.get(OMEGADEPSROOT));
            return answer;
        }

        @Override
        public Path getBuilderInspectServerPath() {
            return null;
        }

        @Override
        public List<Path> getImageProvidedClasspath() {
            List<Path> answer = new LinkedList<>();
            answer.add(Paths.get(OMEGADEPSROOT, "library-support.jar"));
            return answer;
        }

        private Path getJVMCILibraryRoot() {
            return getJavaHome().resolve("lib/jvmci");
        }

        @Override
        public List<Path> getBuilderJVMCIClasspath() {
            List<Path> answer = new LinkedList<>();
            if (! useJavaModules()) {
                answer.add(getJVMCILibraryRoot().resolve("jvmci-api.jar"));
                answer.add(getJVMCILibraryRoot().resolve("jvmci-hotspot.jar"));
            }
            answer.add(Paths.get(OMEGADEPSROOT, "compiler.jar"));
            return answer;
        }

        @Override
        public List<Path> getBuilderJVMCIClasspathAppend() {
            List<Path> answer = new LinkedList<>();
            answer.add(Paths.get(OMEGADEPSROOT, "compiler.jar"));
            return answer;
        }

        @Override
        public List<Path> getBuilderBootClasspath() {
            List<Path> answer = new LinkedList<>();
            try {
                List<Path> javafxJars = USE_JAVAFX ? Files.walk(Paths.get(JFXSDK + "/lib"))
                        .filter(p -> p.endsWith(".jar"))
                        .collect(Collectors.toList()) : new ArrayList<>();
                answer.addAll(javafxJars);
            } catch (IOException e) {
                e.printStackTrace();
            }
            answer.add(Paths.get(OMEGADEPSROOT, "graal-sdk.jar"));
            return answer;
        }

        @Override
        public List<Path> getBuilderModulePath() {
            List<Path> paths = new ArrayList<>();
            paths.add(Paths.get(OMEGADEPSROOT, "graal-sdk.jar"));
            paths.add(Paths.get(OMEGADEPSROOT,"truffle-api.jar"));
            return paths;
        }

        @Override
        public List<Path> getBuilderUpgradeModulePath() {
            return Arrays.asList(Paths.get(OMEGADEPSROOT,"compiler.jar"));
        }

        @Override
        public List<Path> getImageClasspath() {
            return classDir;
        }

        @Override
        public List<String> getBuildArgs() {
            List<String> list = new LinkedList<>(config.getAdditionalBuildArgs());
            list.add("-H:Class=" + mainClass);
            list.add("-H:Name=" + appName);
            list.add("-H:JNIConfigurationFiles=jniconfig.json");
            list.add("-H:IncludeResources=.*/.*frag$");
            list.add("-H:ReflectionConfigurationFiles=reflectionconfig.json");
            if (this.cross || Omega.macHost) {
                list.add("-H:Kind=SHARED_LIBRARY");
            }
            if (USE_JAVAFX) {
                delayClinitList.stream().map(clinit -> "-H:DelayClassInitialization=" + clinit).forEach(list::add);
            }
            config.getRerunClinitList().stream().map(clinit -> "-H:RerunClassInitialization=" + clinit).forEach(list::add);
            list.add("-H:+ReportUnsupportedElementsAtRuntime");
            list.add("-H:TempDirectory="+workDir.toAbsolutePath()+"/tmp");
            return list;
        }

    }

    public static String uniqueShortNameForMain(String className) {
        StringBuilder fullName = new StringBuilder();
        fullName.append(className);
        fullName.append(".main([Ljava.lang.String;,)void");

        String qualifiedClassName = "main" + "_" +
                "main" + "_" +
                SubstrateUtil.digest(fullName.toString());
        return qualifiedClassName.substring(qualifiedClassName.lastIndexOf(".") + 1);
    }

    static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

}
