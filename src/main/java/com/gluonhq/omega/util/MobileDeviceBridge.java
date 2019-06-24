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

import com.dd.plist.NSArray;
import com.dd.plist.NSDictionary;
import com.dd.plist.NSObject;
import com.dd.plist.PropertyListParser;
import com.gluonhq.omega.util.IDevice.InstproxyStatusCallback;
import jnr.ffi.*;
import jnr.ffi.Runtime;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 *
 * @author johan
 */
public class MobileDeviceBridge {

    public static final MobileDeviceBridge instance = new MobileDeviceBridge();

    private IDevice iDevice = null;
    private Runtime runtime;

    public final static String AFC_SERVICE_NAME = "com.apple.afc";
    public final static String INSTPROXY_SERVICE_NAME = "com.apple.mobile.installation_proxy";

    private MobileDeviceBridge() {
        Logger.logDebug("MobileDeviceBridge :: Create bridge");
    }

    public void init() {
        Logger.logDebug("MobileDeviceBridge Prepare device");

        Path f = FileOps.copyResourceToTmp("/native/ios/libimobiledevice.dylib");
        String libloc = f.toAbsolutePath().toString();
        // libloc = "/Users/johan/mobile/ios/github/libimobiledevice/src/.libs/libimobiledevice.dylib";
        iDevice = LibraryLoader.create(IDevice.class).load(libloc);
        iDevice.idevice_set_debug_level(10);
        runtime = jnr.ffi.Runtime.getRuntime(iDevice);
    }

    private boolean isReady() {
        return (iDevice != null);
    }

    public String[] getDeviceIds() {
        if (!isReady()) {
            Logger.logSevere("No iDevice found, fail");
            return null;
        }
        Logger.logDebug("MobileDeviceBridge :: get deviceIds");

        List<String> answer = new ArrayList<>();
        Pointer countPointer = Memory.allocate(runtime, 4);
        Pointer devicesPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);

        iDevice.idevice_get_device_list(devicesPointer, countPointer);
        Pointer p0 = devicesPointer.getPointer(0);
        int size = countPointer.getInt(0);
        if (size > 0) {
            String[] nts = p0.getNullTerminatedStringArray(0);
            answer.addAll(Arrays.asList(nts));
        }
        Logger.logDebug("# connected devices = " + size);
        return answer.toArray( new String[0]);
    }

    public Pointer getDevice(String deviceId) {
        Pointer devicePointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);
        iDevice.idevice_new(devicePointer, deviceId);
        return devicePointer;
    }

    public Pointer connectDevice(Pointer devicePointer, int port) {
        Logger.logDebug("MobileDeviceBridge :: connect device");
        Pointer device = devicePointer.getPointer(0);
        Pointer connectionPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);
        iDevice.idevice_connect(device, (short) port, connectionPointer);
        return connectionPointer;
    }

    public int sendData(Pointer connectionPointer, byte[] b) {
        Pointer Connection = connectionPointer.getPointer(0);
        Pointer sentCountPointer = Memory.allocate(runtime, 4);

        iDevice.idevice_connection_send(Connection, b, b.length, sentCountPointer);
        int count = sentCountPointer.getInt(0);
        return count;
    }

    public int receiveData(Pointer connectionPointer, byte[] b, int offset, int len, int timeout) {
        Pointer Connection = connectionPointer.getPointer(0);
        Pointer receivedCountPointer = Memory.allocate(runtime, 4);
        iDevice.idevice_connection_receive(Connection, b, b.length, receivedCountPointer, timeout);
        int count = receivedCountPointer.getInt(0);
        return count;
    }

    public Pointer lockdownClient(Pointer devicePointer, String label) throws IllegalArgumentException {
        Logger.logDebug("MobileDeviceBridge :: lockdownClient ");
        Pointer lockdownClientPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);
        Pointer device = devicePointer.getPointer(0);
        int result = iDevice.lockdownd_client_new_with_handshake(device, lockdownClientPointer, label);
        Logger.logDebug("Result of lockdown for device "+device+" with label "+label+" = "+result+" returns pointer "+lockdownClientPointer);
        if (result != 0) {
            throw new IllegalArgumentException ("Result of lockdown is " + result);
        }
        return lockdownClientPointer;
    }

    public void unlockClient(Pointer p) {
        Logger.logDebug("MobileDeviceBridge :: unlockdownClient ");
        Pointer client = p.getPointer(0);
        int result = iDevice.lockdownd_client_free(client);
        Logger.logDebug("Result of unlock = " + result);
        if (result != 0) {
            throw new IllegalArgumentException ("result of lockdown is "+result);
        }
    }

    public Pointer getPlistPointer(byte[] b, int offset, long size) {
        Pointer plistPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);
        iDevice.plist_from_bin(b, size, plistPointer);
        return plistPointer;
    }

    public Object getValue (Pointer lockdownClientPointer, String domain, String key) throws IOException {
        Logger.logDebug("MobileDeviceBridge :: getValue for key " + key);
        Pointer lockdownClient = lockdownClientPointer.getPointer(0);
        Pointer plistPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);
        int result = iDevice.lockdownd_get_value(lockdownClient, domain, key, plistPointer);
        if (result != 0) {
            throw new IllegalArgumentException ("result of lockdownGetValue is "+result);
        }
        NSObject nsObject = getValueFromPlist(plistPointer.getPointer(0));
        return nsObject.toJavaObject();
    }

    public NSObject getValueFromPlist (Pointer plist) throws IOException {
        Pointer contentPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);
        Pointer lengthPointer = Memory.allocate(runtime, 4);
        iDevice.plist_to_bin(plist, contentPointer, lengthPointer);
        int count = lengthPointer.getInt(0);
        Logger.logDebug("getValueFromPlist, count = "+count);
        if (count > 0) {
            byte[] b = new byte[count];
            Pointer content = contentPointer.getPointer(0);
            content.get(0, b, 0, count);
            try {
                NSObject nsObject = PropertyListParser.parse(b);
                Object jo = nsObject.toJavaObject();
                return nsObject;
            } catch (Exception ex) {
                Logger.logSevere("Failed getting values from PList: " + ex);
            }
        }
        return null;
    }

    public Pointer startService(Pointer lockdownClientPointer, String identifier) {
        Logger.logDebug("MobileDeviceBridge :: startService for " + identifier);
        Pointer lockdownServiceDescriptorPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);

        Pointer lockdownClient = lockdownClientPointer.getPointer(0);
        int result = iDevice.lockdownd_start_service(lockdownClient, identifier, lockdownServiceDescriptorPointer);
        Logger.logDebug("MobileDeviceBridge :: result of startservice = "+result);
        if (result == -17) {
            Logger.logSevere("Device is locked");
            throw new IllegalArgumentException ("Device is locked!");
        } else if (result != 0) {
            throw new IllegalArgumentException ("result of lockdownd_start_service is "+result);
        }
        return lockdownServiceDescriptorPointer;
    }

    public Pointer newAfcClient(Pointer devicePointer, Pointer lockdownServiceDescriptorPointer) {
        Pointer afcClientPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);
        Pointer device = devicePointer.getPointer(0);
        Pointer lockdownServiceDescriptor = lockdownServiceDescriptorPointer.getPointer(0);
        int result = iDevice.afc_client_new(device, lockdownServiceDescriptor, afcClientPointer);
        Logger.logDebug("Result of newAfcClient = "+result);
        return afcClientPointer;
    }

    public void freeAfcClient(Pointer afcClientPointer) {
        Pointer afcClient = afcClientPointer.getPointer(0);
        int result = iDevice.afc_client_free(afcClient);
        Logger.logDebug("Result of freeAfcClient = "+result);
        if (result != 0) throw new RuntimeException ("FreeAfcClient failed");
    }

    public void makeDirectory(Pointer afcClientPointer, String name) {
        Pointer afcClient = afcClientPointer.getPointer(0);
        int result = iDevice.afc_make_directory(afcClient, name);
        if (result != 0) {
            throw new IllegalArgumentException ("Can't create directory named "+name+", result = "+result);
        }
    }

    public long fileOpen(Pointer afcClientPointer, String name, int mode) {
        Pointer afcClient = afcClientPointer.getPointer(0);
        Pointer handlePointer =  Memory.allocateDirect(runtime, 8);
        iDevice.afc_file_open(afcClient, name, mode, handlePointer);
        long answer = handlePointer.getLong(0);
        return answer;
    }

    public void fileClose(Pointer afcClientPointer, long handle) {
        Pointer afcClient = afcClientPointer.getPointer(0);
        int result = iDevice.afc_file_close(afcClient, handle);
        if (result != 0) {
            throw new RuntimeException ("Couldn't close handle!"+result);
        }
    }

    public int writeBytes (Pointer afcClientPointer, long handle, byte[] b,  long size) {
        Pointer afcClient = afcClientPointer.getPointer(0);
        Pointer bytesWrittenPointer =  Memory.allocateDirect(runtime, 4);
        iDevice.afc_file_write(afcClient, handle, b, (int)size, bytesWrittenPointer);
        return bytesWrittenPointer.getInt(0);
    }

    public void makeSymLink (Pointer afcClientPointer, String target, String source) {
        makeLink (afcClientPointer, 2, target, source);
    }

    public void makeHardLink (Pointer afcClientPointer, String target, String source) {
        makeLink (afcClientPointer, 1, target, source);
    }

    public void makeLink (Pointer afcClientPointer, int type, String target, String source) {
        Pointer afcClient = afcClientPointer.getPointer(0);
        int result = iDevice.afc_make_link(afcClient, type, target, source);
        if (result != 0) {
            throw new RuntimeException ("Couldn't make link from "+source+" to "+target+", result = "+result);
        }
    }

    public Pointer newInstProxyClient(Pointer devicePointer, Pointer lockdownServiceDescriptorPointer) {
        Pointer instProxyClientPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);
        Pointer device = devicePointer.getPointer(0);
        Pointer lockdownServiceDescriptor = lockdownServiceDescriptorPointer.getPointer(0);
        int result = iDevice.instproxy_client_new(device, lockdownServiceDescriptor, instProxyClientPointer);
        Logger.logDebug("Result of newInstProxyClient = "+result);
        return instProxyClientPointer;
    }

    public void freeInstProxyClient (Pointer instProxyClientPointer) {
        Pointer instproxyClient = instProxyClientPointer.getPointer(0);
        int result = iDevice.instproxy_client_free(instproxyClient);
        Logger.logDebug("Result of instproxy_client_free = "+result);
        if (result != 0) throw new RuntimeException ("error freeing instproxyClient!");
    }

    public ErrorCode instProxyUpgrade (Pointer instProxyClientPointer, String path, Pointer clientOptionsPointer, InstproxyStatusCallback statusCallback, Pointer userData) {
        Pointer instProxyClient = instProxyClientPointer.getPointer(0);
        Pointer clientOptions = clientOptionsPointer.getPointer(0);
        if (clientOptions == null) {
            //  clientOptions = Memory.allocate(runtime, NativeType.ADDRESS);
            Logger.logSevere("MobileDeviceBridge :: Pass NULL client options");
        }
        Logger.logDebug("About to call instproxyupgrade");
        int resultCode = iDevice.instproxy_upgrade(instProxyClient, path, clientOptions, statusCallback, userData);
        ErrorCode result = ErrorCode.valueOf(resultCode);
        Logger.logDebug("Result of instProxyUpgrad = "+result);
        return result;
    }


    public List<String> listApps(Pointer instProxyClientPointer) throws IOException {
        Pointer instProxyClient = instProxyClientPointer.getPointer(0);
        Pointer resultPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);
        iDevice.instproxy_browse(instProxyClient, null, resultPointer);
        Pointer plist = resultPointer.getPointer(0);
        NSArray nsArray = (NSArray) getValueFromPlist(plist);
        int count = nsArray.count();
        Logger.logDebug("List Apps returns: " + count + " apps");
        List<String> answer = new ArrayList<>();
        for ( NSObject obj: nsArray.getArray()) {
            NSDictionaryEx dict = new NSDictionaryEx( (NSDictionary) obj);
            String bundleId = dict.getString("CFBundleIdentifier");
            answer.add(bundleId);
            String nPath = dict.getString("Path");
            Logger.logDebug("BundleID: "+bundleId + ", nPath = "+nPath);
        }
        return answer;
    }

    public String getAppPath(Pointer instProxyClientPointer, String p) throws IOException {
        Logger.logDebug("Get App Path for: " + p);
        Pointer instProxyClient = instProxyClientPointer.getPointer(0);
        Pointer resultPointer = Memory.allocateDirect(runtime, NativeType.ADDRESS);
        iDevice.instproxy_browse(instProxyClient, null, resultPointer);
        Pointer plist = resultPointer.getPointer(0);
        NSArray nsArray = (NSArray) getValueFromPlist(plist);

        for (NSObject obj : nsArray.getArray()) {
            NSDictionaryEx dict = new NSDictionaryEx((NSDictionary) obj);
            String bts = dict.getString("CFBundleIdentifier");
            if (bts.equals(p)) {
                String nPath = dict.getString("Path");
                Logger.logDebug("We have a bundleIdentifier with the requested path: " + nPath);
                return nPath;
            }
        }
        Logger.logSevere("No bundleIdentifier found for " + p);
        return null;
    }

    enum ErrorCode {

        SUCCESS                                                   ( 0),
        INVALID_ARG                                               (-1),
        PLIST_ERROR                                               (-2),
        CONN_FAILED                                               (-3),
        OP_IN_PROGRESS                                            (-4),
        OP_FAILED                                                 (-5),
        RECEIVE_TIMEOUT                                           (-6),
        /* native */
        ALREADY_ARCHIVED                                          (-7),
        API_INTERNAL_ERROR                                        (-8),
        APPLICATION_ALREADY_INSTALLED                             (-9),
        APPLICATION_MOVE_FAILED                                   (-10),
        APPLICATION_SINF_CAPTURE_FAILED                           (-11),
        APPLICATION_SANDBOX_FAILED                                (-12),
        APPLICATION_VERIFICATION_FAILED                           (-13),
        ARCHIVE_DESTRUCTION_FAILED                                (-14),
        BUNDLE_VERIFICATION_FAILED                                (-15),
        CARRIER_BUNDLE_COPY_FAILED                                (-16),
        CARRIER_BUNDLE_DIRECTORY_CREATION_FAILED                  (-17),
        CARRIER_BUNDLE_MISSING_SUPPORTED_SIMS                     (-18),
        COMM_CENTER_NOTIFICATION_FAILED                           (-19),
        CONTAINER_CREATION_FAILED                                 (-20),
        CONTAINER_P0WN_FAILED                                     (-21),
        CONTAINER_REMOVAL_FAILED                                  (-22),
        EMBEDDED_PROFILE_INSTALL_FAILED                           (-23),
        EXECUTABLE_TWIDDLE_FAILED                                 (-24),
        EXISTENCE_CHECK_FAILED                                    (-25),
        INSTALL_MAP_UPDATE_FAILED                                 (-26),
        MANIFEST_CAPTURE_FAILED                                   (-27),
        MAP_GENERATION_FAILED                                     (-28),
        MISSING_BUNDLE_EXECUTABLE                                 (-29),
        MISSING_BUNDLE_IDENTIFIER                                 (-30),
        MISSING_BUNDLE_PATH                                       (-31),
        MISSING_CONTAINER                                         (-32),
        NOTIFICATION_FAILED                                       (-33),
        PACKAGE_EXTRACTION_FAILED                                 (-34),
        PACKAGE_INSPECTION_FAILED                                 (-35),
        PACKAGE_MOVE_FAILED                                       (-36),
        PATH_CONVERSION_FAILED                                    (-37),
        RESTORE_CONTAINER_FAILED                                  (-38),
        SEATBELT_PROFILE_REMOVAL_FAILED                           (-39),
        STAGE_CREATION_FAILED                                     (-40),
        SYMLINK_FAILED                                            (-41),
        UNKNOWN_COMMAND                                           (-42),
        ITUNES_ARTWORK_CAPTURE_FAILED                             (-43),
        ITUNES_METADATA_CAPTURE_FAILED                            (-44),
        DEVICE_OS_VERSION_TOO_LOW                                 (-45),
        DEVICE_FAMILY_NOT_SUPPORTED                               (-46),
        PACKAGE_PATCH_FAILED                                      (-47),
        INCORRECT_ARCHITECTURE                                    (-48),
        PLUGIN_COPY_FAILED                                        (-49),
        BREADCRUMB_FAILED                                         (-50),
        BREADCRUMB_UNLOCK_FAILED                                  (-51),
        GEOJSON_CAPTURE_FAILED                                    (-52),
        NEWSSTAND_ARTWORK_CAPTURE_FAILED                          (-53),
        MISSING_COMMAND                                           (-54),
        NOT_ENTITLED                                              (-55),
        MISSING_PACKAGE_PATH                                      (-56),
        MISSING_CONTAINER_PATH                                    (-57),
        MISSING_APPLICATION_IDENTIFIER                            (-58),
        MISSING_ATTRIBUTE_VALUE                                   (-59),
        LOOKUP_FAILED                                             (-60),
        DICT_CREATION_FAILED                                      (-61),
        INSTALL_PROHIBITED                                        (-62),
        UNINSTALL_PROHIBITED                                      (-63),
        MISSING_BUNDLE_VERSION                                    (-64),
        UNKNOWN_ERROR                                             (-256);

        private static final Map<Integer, ErrorCode> map = new HashMap<>();
        static {
            for( ErrorCode entry: values() ) {
                map.put( entry.code, entry );
            }
        }

        public int code;

        ErrorCode( int code ) {
            this.code = code;
        }

        public static ErrorCode valueOf( int code ) {
            return map.getOrDefault(code, UNKNOWN_ERROR );
        }

        @Override
        public String toString() {
            return name() + "(" + code + ')';
        }
    }


}