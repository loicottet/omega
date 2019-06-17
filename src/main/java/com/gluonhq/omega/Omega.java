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

import com.gluonhq.omega.model.ProcessPaths;
import com.gluonhq.omega.target.TargetProcess;
import com.gluonhq.omega.target.TargetProcessFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Omega {

    private static Configuration configuration;
    private static ProcessPaths paths;

    public static Configuration getConfiguration() {
        return configuration;
    }

    public static ProcessPaths getPaths() {
        return paths;
    }

    /**
     * Runs LocalBuild on Mac, Linux or iOS
     *
     * @param buildRoot Client directory, (e.g. build/client)
     * @param configuration the required configuration
     * @param cp the list of directories and jars that make up the class path
     * @throws Exception
     */
    public static void nativeCompile(String buildRoot, Configuration configuration, String cp) throws Exception {
        configure(configuration);
        paths = new ProcessPaths(buildRoot, configuration.getTarget().getOs() + "-" + configuration.getTarget().getArch());

        List<Path> classPath = Stream.of(cp.split(File.pathSeparator))
                .map(Paths::get)
                .collect(Collectors.toList());

        TargetProcess targetProcess = TargetProcessFactory.getTargetProcess(configuration, paths.getSourcePath());
        targetProcess.compile(classPath, configuration.getMainClassName(), configuration.getAppName());
    }

    /**
     * Runs LocalLink on Mac, Linux or iOS
     * @param buildRoot Client directory, (e.g. build/client)
     * @param configuration the required configuration
     * @throws Exception
     */
    public static void nativeLink(String buildRoot, Configuration configuration) throws Exception {
        configure(configuration);
        paths = new ProcessPaths(buildRoot, configuration.getTarget().getOs() + "-" + configuration.getTarget().getArch());
        TargetProcess targetProcess = TargetProcessFactory.getTargetProcess(configuration, paths.getSourcePath());
        SVMBridge.createReleaseSymbols(paths.getGvmPath(), targetProcess);
        targetProcess.link(configuration.getAppName());
    }

    /**
     * Runs LocalLink on Mac, Linux or iOS
     * @param buildRoot Client directory, (e.g. build/client)
     * @param configuration the required configuration
     * @throws Exception
     */
    public static void nativeRun(String buildRoot, Configuration configuration) throws Exception {
        configure(configuration);
        paths = new ProcessPaths(buildRoot, configuration.getTarget().getOs() + "-" + configuration.getTarget().getArch());

        TargetProcess targetProcess = TargetProcessFactory.getTargetProcess(configuration, paths.getSourcePath());
        targetProcess.run(configuration.getAppName());
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
     * args[6] graal Libs Version directory (e.g. 13-ea+1)
     * args[7] java static Version directory (e.g. 13-ea+1)
     * args[8] JavaFX SDK Version directory (e.g. 13-ea+1)
     */
    public static void main(String[] args) {
        try {
            String buildRoot = args[0];
            String mainClassName = args[1];
            String appName = args[2];
            String cp = args[3];
            String target = args[4];
            String graalLibsVersion = args[6];
            String javaStaticVersion = args[7];
            String javafxStaticSDKVersion = args[8];

            Configuration configuration = new Configuration();
            configuration.setGraalLibsVersion(graalLibsVersion);
            configuration.setJavaStaticSdkVersion(javaStaticVersion);
            configuration.setJavafxStaticSdkVersion(javafxStaticSDKVersion);

            configuration.setAppName(appName);
            configuration.setMainClassName(mainClassName);

            Omega.nativeCompile(buildRoot, configuration, cp);

            Omega.nativeLink(buildRoot, configuration);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // private

    private static void configure(Configuration configuration) {
        if (configuration == null) {
            throw new RuntimeException("Error: configuration is null");
        }
        if (configuration.getMainClassName() == null || configuration.getMainClassName().isEmpty()) {
            throw new RuntimeException("Error: MainClassName is not set");
        }
        if (configuration.getAppName() == null || configuration.getAppName().isEmpty()) {
            throw new RuntimeException("Error: AppName is not set");
        }
        Omega.configuration = configuration;
        System.err.println("Omega :: host triplet = " + configuration.getHost());
        System.err.println("Omega :: target triplet = " + configuration.getTarget());
    }

}
