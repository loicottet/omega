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
package com.gluonhq.omega.target;

import java.io.File;
import java.io.FileFilter;
import java.nio.file.Files;

abstract class DarwinTargetConfiguration extends AbstractTargetConfiguration {

    private final static String SDK_BASE = "/Applications/Xcode.app/Contents/Developer/Platforms/";

    enum SdkDirType {
        MACOSX("MacOSX"),
        IPHONE_DEV("iPhoneOS"),
        IPHONE_SIM("iPhoneSimulator");

        private final String name;
        private String absolutePath;

        SdkDirType(String name) {
            this.name = name;
        }

        public String getSDKPath() {
            if (absolutePath == null) {
                absolutePath = getSdkDir(name);
            }
            return absolutePath;
        }

        private static String getSdkDir(String name) {
            File mdir = new File(SDK_BASE + name + ".platform/Developer/SDKs");
            FileFilter filter = f -> Files.isSymbolicLink(f.toPath());
            File[] listFiles = mdir.listFiles(filter);
            if (listFiles == null || listFiles.length == 0) {
                throw new RuntimeException("Couldn't find a relevant iOS SDK. Please mail support@gluonhq.com with the contents of " + mdir);
            }
            if (listFiles.length > 1) {
                System.err.println("Warning, more than 1 iOS SDK found. Please look at the contents of " + mdir);
            }

            String absolutePath = listFiles[0].getAbsolutePath();
            logDebug("Sdk path: " + absolutePath + ", for type " + name);
            return absolutePath;
        }
    }
}
