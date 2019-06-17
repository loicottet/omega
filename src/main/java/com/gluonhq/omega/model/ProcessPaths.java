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
package com.gluonhq.omega.model;

import com.gluonhq.omega.util.Constants;
import com.gluonhq.omega.util.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProcessPaths {

    private String buildRoot;
    private Path clientPath;
    private Path appPath;
    private Path gvmPath;
    private Path tmpPath;
    private Path sourcePath;

    /**
     * |-- build or target
     *     |-- client                   <-- buildRoot
     *         |-- $app                 <-- $OS-$ARCH
     *             |-- gvm
     *                 |-- tmp
     *                 |-- lib
     *             |-- appName
     * |-- src
     *     |-- mac or ios
     *     |-- main
     */

    public ProcessPaths(String buildRoot, String app) {
        this.buildRoot = buildRoot;
        try {
            clientPath = buildRoot != null && ! buildRoot.isEmpty() ?
                    Paths.get(buildRoot) : Paths.get(System.getProperty("user.dir"));

            appPath = Files.createDirectories(clientPath.resolve(app));
            gvmPath = Files.createDirectories(appPath.resolve(Constants.GVM_PATH));
            tmpPath = Files.createDirectories(gvmPath.resolve(Constants.TMP_PATH));
            sourcePath = clientPath.getParent().getParent().resolve(Constants.SOURCE_PATH);
            Logger.logDebug("gvmDir = " + gvmPath.toString());
        } catch (IOException e) {
            Logger.logSevere(e, "Error processingPaths for buildRoot: " + buildRoot);
        }
    }

    public String getBuildRoot() {
        return buildRoot;
    }

    public void setBuildRoot(String buildRoot) {
        this.buildRoot = buildRoot;
    }

    public Path getClientPath() {
        return clientPath;
    }

    public void setClientPath(Path clientPath) {
        this.clientPath = clientPath;
    }

    public Path getAppPath() {
        return appPath;
    }

    public void setAppPath(Path appPath) {
        this.appPath = appPath;
    }

    public Path getGvmPath() {
        return gvmPath;
    }

    public void setGvmPath(Path gvmPath) {
        this.gvmPath = gvmPath;
    }

    public Path getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(Path sourcePath) {
        this.sourcePath = sourcePath;
    }

    public Path getTmpPath() {
        return tmpPath;
    }

    public void setTmpPath(Path tmpPath) {
        this.tmpPath = tmpPath;
    }
}
