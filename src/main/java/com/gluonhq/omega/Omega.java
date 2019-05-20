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

import com.gluonhq.omega.target.IosTargetConfiguration;
import com.gluonhq.omega.target.LinuxTargetConfiguration;
import com.gluonhq.omega.target.MacosTargetConfiguration;
import com.gluonhq.omega.target.TargetConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Omega {

    private static Path omegaPath;
    private static Path gvmPath;

    static boolean linux = false;
    static boolean macHost = false;
    private static Config config;

    static {
        String osname = System.getProperty("os.name");
        System.err.println("SVMBridge, osname = "+osname);
        if (osname.toLowerCase(Locale.ROOT).contains("linux")) {
            linux = true;
        } else if (osname.toLowerCase(Locale.ROOT).contains("mac")) {
            macHost = true;
        }
    }

    private static void setConfig(Config config) {
        Omega.config = config;
    }

    public static Config getConfig() {
        return config;
    }

    /**
     * Runs LocalBuild on Mac, Linux or iOS
     *
     * @param buildRoot Omega directory, (e.g. build/omega)
     * @param config the required configuration
     * @param cp the list of directories and jars that make up the class path
     * @param target  the host machine, iOS sim or device
     * @throws Exception
     */
    public static void nativeCompile(String buildRoot, Config config, String cp, String target) throws Exception {
        prepareConfig(config);
        prepareDirs(buildRoot);

        List<Path> classPath = Stream.of(cp.split(File.pathSeparator))
                .map(Paths::get)
                .collect(Collectors.toList());

        TargetConfiguration targetConfig = getTargetConfiguration(target);
        targetConfig.compile(gvmPath, classPath, config.getMainClassName(), config.getAppName(), target);
    }

    /**
     * Runs LocalLink on Mac, Linux or iOS
     * @param buildRoot Omega directory, (e.g. build/omega)
     * @param workDir the directory of the application (e.g. build/omega/gvm/tmp)
     * @param config the required configuration
     * @param target  the host machine, iOS sim or device
     * @throws Exception
     */
    public static void nativeLink(String buildRoot, Path workDir, Config config, String target) throws Exception {
        prepareConfig(config);
        prepareDirs(buildRoot);

        TargetConfiguration targetConfig = getTargetConfiguration(target);
        targetConfig.link(workDir, config.getAppName(), target);
    }

    /**
     * Runs LocalLink on Mac, Linux or iOS
     * @param workDir Omega directory, (e.g. build/omega)
     * @param config the required configuration
     * @param target  the host machine, iOS sim or device
     * @throws Exception
     */
    public static void nativeRun(Path workDir, Config config, String target) throws Exception {
        prepareConfig(config);
        prepareDirs(workDir.toString());

        TargetConfiguration targetConfig = getTargetConfiguration(target);
        targetConfig.run(workDir, config.getAppName(), target);
    }

    /**
     * Returns the target name based on the configuration.
     * @param config the required configuration
     * @return
     */
    public static String getTarget(Config config) {
        String target = "";
        if ("host".equals(config.getTarget())) {
            String osname = System.getProperty("os.name");
            if (osname.toLowerCase(Locale.ROOT).contains("linux")) {
                target = "linux";
            } else if (osname.toLowerCase(Locale.ROOT).contains("mac")) {
                target = "macosx";
            }
        } else if ("ios".equals(Omega.config.getTarget()) || "ios-sim".equals(config.getTarget())) {
            target = "ios";
        } else {
            throw new RuntimeException("No valid target: " + config.getTarget());
        }
        return target;
    }

    /**
     * Returns the list of runtime arguments
     * @return a list of runtime arguments
     */
    public static List<String> getRuntimeArgs() {
        return SVMBridge.runtimeArgs;
    }

    /**
     * Returns the list with the classpath
     * @return a list with the classpath
     */
    public static List<String> getClassPath() {
        return SVMBridge.classPath;
    }

    /**
     * Returns the list with the module path
     * @return a list with the module path
     */
    public static List<String> getModulePath() {
        return SVMBridge.modulePath;
    }

    /**
     * Returns the list with the upgrade module path
     * @return a list with the upgrade module path
     */
    public static List<String> getUpgradeModulePath() {
        return SVMBridge.upgradeModulePath;
    }

    /**
     * Returns the list with the default bundles
     * @return a list with the default bundles
     */
    public static List<String> getBundlesList() {
        return SVMBridge.getBundlesList();
    }

    /**
     * List of expected parameters:
     * args[0] buildRoot Omega directory, (e.g. build/omega)
     * args[1] mainClassName the FQN of the mainclass (e.g. com.gluonhq.demo.Application)
     * args[2] appName the name of the application (e.g. demo)
     * args[3] cp the list of directories and jars that make up the class path
     * args[4] true if target is not the host machine, iOS for now
     * args[5] workDir the directory of the application (e.g. build/omega/gvm/tmp)
     * args[6] deps directory (e.g. build/omega/deps)
     * args[7] JavaFX SDK directory (if it is null or empty, JavaFX won't be used)
     * args[8] Java static directory
     */
    public static void main(String[] args) {
        try {
            String buildRoot = args[0];
            String mainClassName = args[1];
            String appName = args[2];
            String cp = args[3];
            String target = args[4];
            String deps = args[6];
            String sdk = args[7];
            String staticDir = args[8];

            Config config = new Config();
            config.setDepsRoot(deps);
            config.setJavaFXRoot(sdk);
            config.setUseJavaFX(sdk != null && ! sdk.isEmpty());
            config.setStaticRoot(staticDir);
            config.setAppName(appName);
            config.setMainClassName(mainClassName);
            Omega.nativeCompile(buildRoot, config, cp, target);

            Path workDir = Path.of(args[5]);
            Omega.nativeLink(buildRoot, workDir, config, target);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private

    private static String prepareDirs(String buildRoot) {
        String gvmDir = null;
        try {
            omegaPath = buildRoot != null && ! buildRoot.isEmpty() ?
                    Paths.get(buildRoot) : Paths.get(System.getProperty("user.dir"));
            String rootDir = omegaPath.toAbsolutePath().toString();

            gvmPath = Paths.get(rootDir, "gvm");
            gvmPath = Files.createDirectories(gvmPath);
            gvmDir = gvmPath.toAbsolutePath().toString();
            System.err.println("gvmDir = " + gvmDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return gvmDir;
    }

    private static void prepareConfig(Config config) {
        if (config.getMainClassName() == null || config.getMainClassName().isEmpty()) {
            throw new RuntimeException("MainClassName is not set");
        }
        if (config.getAppName() == null || config.getAppName().isEmpty()) {
            throw new RuntimeException("AppName is not set");
        }
        setConfig(config);
    }

    private static TargetConfiguration getTargetConfiguration(String target) {
        TargetConfiguration config;
        if (target.startsWith("ios")) {
            config = new IosTargetConfiguration(omegaPath.getParent().getParent().resolve("src").resolve("ios"));
        } else if (macHost) {
            config = new MacosTargetConfiguration();
        } else if (linux) {
            config = new LinuxTargetConfiguration();
        } else {
            throw new RuntimeException("target not found: "+target);
        }
        return config;
    }

}
