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

import java.util.List;

public class Config {

    private String graalLibsVersion;
    private String javaStaticSdkVersion;
    private String javafxStaticSdkVersion;
    private String graalLibsRoot;
    private String graalLibsUserPath;
    private String JavaFXRoot;
    private String StaticRoot;
    private boolean useJNI = false;
    private boolean useJavaFX = true;
    private boolean enableCheckHash = true;
    private boolean verbose = false;

    private String target;
    private String backend;
    private List<String> bundlesList;
    private List<String> resourcesList;
    private List<String> reflectionList;
    private List<String> jniList;
    private List<String> delayInitList;
    private List<String> runtimeArgsList;
    private List<String> releaseSymbolsList;

    private String appName;
    private String mainClassName;

    public Config() {}

    public String getGraalLibsVersion() {
        return graalLibsVersion;
    }

    /**
     * Sets the Graal libs version
     * @param graalLibsVersion the Graal libs version
     */
    public void setGraalLibsVersion(String graalLibsVersion) {
        this.graalLibsVersion = graalLibsVersion;
    }

    public String getJavaStaticSdkVersion() {
        return javaStaticSdkVersion;
    }

    /**
     * Sets the Java static SDK version
     * @param javaStaticSdkVersion the Java static SDK version
     */
    public void setJavaStaticSdkVersion(String javaStaticSdkVersion) {
        this.javaStaticSdkVersion = javaStaticSdkVersion;
    }

    public String getJavafxStaticSdkVersion() {
        return javafxStaticSdkVersion;
    }

    /**
     * Sets the JavaFX static SDK version
     * @param javafxStaticSdkVersion the JavaFX static SDK version
     */
    public void setJavafxStaticSdkVersion(String javafxStaticSdkVersion) {
        this.javafxStaticSdkVersion = javafxStaticSdkVersion;
    }

    public String getGraalLibsRoot() {
        return graalLibsRoot;
    }

    /**
     * Sets the omega dependencies directory
     * @param graalLibsRoot the omega dependencies directory
     *                      (e.g ~/.gluon/omega/graalLibs/20-ea/bundle/lib)
     */
    public void setGraalLibsRoot(String graalLibsRoot) {
        this.graalLibsRoot = graalLibsRoot;
    }

    public String getGraalLibsUserPath() {
        return graalLibsUserPath;
    }

    /**
     * Sets the omega dependencies directory set by the user
     * @param graalLibsUserPath the omega dependencies directory
     *                          (e.g $user/Downloads/graalLibs/lib)
     */
    public void setGraalLibsUserPath(String graalLibsUserPath) {
        this.graalLibsUserPath = graalLibsUserPath;
    }

    public String getJavaFXRoot() {
        return JavaFXRoot;
    }

    /**
     * Sets the JavaFX SDK directory
     * @param javaFXRoot the JavaFX SDK directory
     */
    public void setJavaFXRoot(String javaFXRoot) {
        JavaFXRoot = javaFXRoot;
    }

    public String getStaticRoot() {
        return StaticRoot;
    }

    /**
     * Sets the Static Libs directory
     * @param staticRoot the Static Libs directory
     */
    public void setStaticRoot(String staticRoot) {
        StaticRoot = staticRoot;
    }

    public boolean isUseJNI() {
        return useJNI;
    }

    public void setUseJNI(boolean useJNI) {
        this.useJNI = useJNI;
    }

    public boolean isUseJavaFX() {
        return useJavaFX;
    }

    public void setUseJavaFX(boolean useJavaFX) {
        this.useJavaFX = useJavaFX;
    }

    public boolean isEnableCheckHash() {
        return enableCheckHash;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isVerbose() {
        return verbose;
    }

    /**
     * Enables hash checking to verify integrity of Graal and Java/JavaFX files
     * @param enableCheckHash boolean to enable hash checking
     */
    public void setEnableCheckHash(boolean enableCheckHash) {
        this.enableCheckHash = enableCheckHash;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public List<String> getBundlesList() {
        return bundlesList;
    }

    /**
     * Sets additional bundles
     * @param bundlesList a list of classes that will be added to the default bundlesList list
     */
    public void setBundlesList(List<String> bundlesList) {
        this.bundlesList = bundlesList;
    }

    /**
     * Set additional resources to be included
     * @param resourcesList a list of resource patterns that will be included
     */
    public void setResourcesList(List<String> resourcesList) {
        this.resourcesList = resourcesList;
    }

    public List<String> getResourcesList() {
        return resourcesList;
    }

    public List<String> getReflectionList() {
        return reflectionList;
    }

    /**
     * Sets additional lists
     * @param reflectionList a list of classes that will be added to the default reflection list
     */
    public void setReflectionList(List<String> reflectionList) {
        this.reflectionList = reflectionList;
    }

    public List<String> getJniList() {
        return jniList;
    }

    /**
     * Sets additional lists
     * @param jniList a list of classes that will be added to the default jni list
     */
    public void setJniList(List<String> jniList) {
        this.jniList = jniList;
    }

    public List<String> getDelayInitList() {
        return delayInitList;
    }

    /**
     * Sets additional lists
     * @param delayInitList a list of classes that will be added to the default delayed list
     */
    public void setDelayInitList(List<String> delayInitList) {
        this.delayInitList = delayInitList;
    }

    public List<String> getRuntimeArgsList() {
        return runtimeArgsList;
    }

    /**
     * Sets additional lists of release symbols, like _Java_com_gluonhq*
     * @param releaseSymbolsList a list of classes that will be added to the default release symbols list
     */
    public void setReleaseSymbolsList(List<String> releaseSymbolsList) {
        this.releaseSymbolsList = releaseSymbolsList;
    }

    public List<String> getReleaseSymbolsList() {
        return releaseSymbolsList;
    }

    /**
     * Sets additional lists
     * @param runtimeArgsList a list of classes that will be added to the default runtime args list
     */
    public void setRuntimeArgsList(List<String> runtimeArgsList) {
        this.runtimeArgsList = runtimeArgsList;
    }

    public String getAppName() {
        return appName;
    }

    /**
     * Sets the app name
     * @param appName the name of the application (e.g. demo)
     */
    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getMainClassName() {
        return mainClassName;
    }

    /**
     * Sets the FQN of the mainclass (e.g. com.gluonhq.demo.Application)
     * @param mainClassName the FQN of the mainclass
     */
    public void setMainClassName(String mainClassName) {
        this.mainClassName = mainClassName;
    }
}
