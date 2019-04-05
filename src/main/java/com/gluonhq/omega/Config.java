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

    private String depsRoot;
    private String JavaFXRoot;
    private boolean useJavaFX;
    private String graalVersion;
    private String target;
    private String backend;
    private List<String> bundles;
    private List<String> reflectionList;
    private List<String> jniList;
    private List<String> delayInitList;
    private List<String> runtimeArgsList;

    private String appName;
    private String mainClassName;

    public Config() {}

    public String getDepsRoot() {
        return depsRoot;
    }

    /**
     * Sets the omega dependencies directory
     * @param depsRoot the omega dependencies directory (e.g build/omega/deps/1.0.0)
     */
    public void setDepsRoot(String depsRoot) {
        this.depsRoot = depsRoot;
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

    public boolean isUseJavaFX() {
        return useJavaFX;
    }

    public void setUseJavaFX(boolean useJavaFX) {
        this.useJavaFX = useJavaFX;
    }

    public String getGraalVersion() {
        return graalVersion;
    }

    public void setGraalVersion(String graalVersion) {
        this.graalVersion = graalVersion;
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

    public List<String> getBundles() {
        return bundles;
    }

    /**
     * Sets additional lists
     * @param bundles a list of classes that will be added to the default bundles list
     */
    public void setBundles(List<String> bundles) {
        this.bundles = bundles;
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
