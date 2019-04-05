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
package com.gluonhq.omega.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;


/**
 *
 * @author johan
 */
public class XcodeUtil {

    private String root;

    private String platformBuild;
    private String platformVersion;
    private String platformName;
    private String dtxcode;
    private String dtxcodeBuild;
    private String sdkName;

    public XcodeUtil(String root) throws IOException {
        this.root = root;
        resolve();
    }

    private void resolve() throws IOException {
        Path rootDir = Paths.get(root);
        Path systemVersionFile = rootDir.resolve("System/Library/CoreServices/SystemVersion.plist");
        Path sdkSettingsFile   = rootDir.resolve("SDKSettings.plist");
        Path platformInfoFile  = rootDir.getParent().getParent().getParent().resolve("Info.plist");
        Path xcodeInfoFile     = rootDir.getParent().getParent().getParent().getParent().getParent().getParent().resolve("Info.plist");
        Path xcodeVersionFile  = rootDir.getParent().getParent().getParent().getParent().getParent().getParent().resolve("version.plist");
        System.err.println("platformfile at " + platformInfoFile );
        try {
            NSDictionaryEx systemVersionDict = new NSDictionaryEx(systemVersionFile);
            NSDictionaryEx platformInfoDict  = new NSDictionaryEx(platformInfoFile);
            NSDictionaryEx xcodeInfoDict     = new NSDictionaryEx(xcodeInfoFile);
            NSDictionaryEx xcodeVersionDict  = new NSDictionaryEx(xcodeVersionFile);
            NSDictionaryEx sdkSettingsDict   = new NSDictionaryEx(sdkSettingsFile);

            platformBuild = systemVersionDict.getString("ProductBuildVersion");
            
            NSDictionaryEx additionalInfo =  platformInfoDict.getDictionary("AdditionalInfo");
            this.platformVersion = additionalInfo.getString("DTPlatformVersion");
            this.platformName    = additionalInfo.getString("DTPlatformName");
            // platformInfoDict.getEntrySet().forEach(e -> logDebug("MYENTRY: "+e));
            this.dtxcode         = xcodeInfoDict.getString("DTXcode");
            this.dtxcodeBuild    = xcodeInfoDict.getString("DTXcodeBuild");
            this.sdkName         = sdkSettingsDict.getString( "CanonicalName");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public String getPlatformBuild() {
        return platformBuild;
    }

    public String getPlatformVersion() {
        return platformVersion;
    }
    
    public String getDTXCode() {
        return dtxcode;
    }
    
    public String getDTXCodeBuild() {
        return dtxcodeBuild;
    }

    public String getPlatformName() {
        return platformName;
    }
    
    public String getSDKName() {
        return sdkName;
    }
    
    public static String getCommandForSdk(String command, String sdk) throws IOException {
        ProcessBuilder pb = new ProcessBuilder("xcrun", "-sdk", sdk, "-f", command);
        Process p = pb.start();
        try( BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            return br.readLine();
        }
    }
}
