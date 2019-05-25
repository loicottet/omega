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

import com.dd.plist.NSData;
import com.dd.plist.NSObject;
import org.bouncycastle.cms.CMSSignedData;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author johan
 */
public class ProvisioningProfile implements Comparable<ProvisioningProfile> {


    public enum Type {
        Development, AppStore, AdHoc
    }

    private final Path file;
    private final NSDictionaryEx dict;
    private final String uuid;
    private final String name;
    private final String appIdName;
    private final String appIdPrefix;
    private final LocalDate creationDate;
    private final LocalDate expirationDate;
    private final NSDictionaryEx entitlements;
    private final String appId;
    private final List<String> certFingerprints = new LinkedList<>();
    private final Type type;

    ProvisioningProfile(Path file, NSDictionaryEx dict) {
        this.file = file;
        this.dict = dict;
        this.uuid = dict.getString("UUID");
        this.name = dict.getString("Name");
        this.appIdName = dict.getString("AppIDName");

        this.appIdPrefix = dict.getArray("ApplicationIdentifierPrefix")[0].toString();
        this.creationDate = dict.getDate("CreationDate");
        this.expirationDate = dict.getDate("ExpirationDate");
        this.entitlements = dict.getDictionary("Entitlements");
        this.appId = this.entitlements.getString("application-identifier");

        for (NSObject o : dict.getArray("DeveloperCertificates")) {
            NSData data = (NSData) o;
            certFingerprints.add(getCertFingerprint(data.bytes()));
        }
        boolean getTaskAllow = entitlements.getBoolean("get-task-allow");
        if (getTaskAllow) {
            type = Type.Development;
        } else {
            NSObject[] provisionedDevices = null;
            try {
                dict.getArray("ProvisionedDevices");
            } catch (Exception e) {
                System.err.println("Error getting ProvisioninedDevices, ignore in thread "+Thread.currentThread());
            }
            if (provisionedDevices != null) {
                type = Type.AdHoc;
            } else {
                type = Type.AppStore;
            }
        }
        System.out.println("created");
    }

    public Path getPath() {
        return file;
    }
    
    public NSDictionaryEx getEntitlements() {
        return entitlements;
    }
    
    public String getAppIdPrefix() {
        return appIdPrefix;
    }
    
    public static List<ProvisioningProfile> list() {

        Path dir = Paths.get(System.getProperty("user.home"),"Library/MobileDevice/Provisioning Profiles");
        if ( !Files.exists(dir) || !Files.isDirectory(dir)) {
            System.out.println("OUCH, can't find provisioning profiles!");
            return Collections.emptyList();
        }

        final LocalDate now = LocalDate.now();

        try {
            List<ProvisioningProfile> collect = Files.walk(dir)
                    .filter(f -> f.toFile().getName().endsWith(".mobileprovision"))
                    .map(ProvisioningProfile::create)
                    .filter(p -> ! p.expirationDate.isBefore(now))
                    .sorted()
                    .collect(Collectors.toList());
//            System.out.println("PROVISIONINGLIST = "+collect);
            return collect;
        } catch (IOException ex) {
            ex.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static ProvisioningProfile create(Path file) {
        try (InputStream in = new BufferedInputStream(Files.newInputStream(file))) {
            CMSSignedData data = new CMSSignedData(in);
            byte[] content = (byte[]) data.getSignedContent().getContent();
            NSDictionaryEx dict = new NSDictionaryEx(content);
            ProvisioningProfile provisioningProfile = new ProvisioningProfile(file, dict);
        //    System.out.println("Created provisioningprofile for "+file+" results in "+provisioningProfile);
            return provisioningProfile;
        } catch (Exception e) {
            System.out.println("Error creating provisioningprofile for "+file);
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String getCertFingerprint(byte[] certData) {
        try {
            CertificateFactory x509CertFact = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate) x509CertFact.generateCertificate(new ByteArrayInputStream(certData));
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            return bytesToHex(md.digest(cert.getEncoded())); /// this is much faster
        } catch (CertificateException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int compareTo(ProvisioningProfile o) {
        return this.name.compareToIgnoreCase(o.name);
    }

    public static ProvisioningProfile find(List<ProvisioningProfile> profiles, SigningIdentity signingIdentity, String bundleId) {
        return find(profiles, signingIdentity, bundleId, bundleId);
    }

    public static ProvisioningProfile find(SigningIdentity signingIdentity, String bundleId) {
        return find( list(), signingIdentity, bundleId );
    }

    private static ProvisioningProfile find(List<ProvisioningProfile> profiles, SigningIdentity signingIdentity, String bundleId, String origBundleId) {
        // Try a direct match first
        System.out.println("provprofile asked, bid = " + bundleId + " and origbid = " + origBundleId);
        ProvisioningProfile answer = null;
        for (ProvisioningProfile p : profiles) {
            System.err.println("CONSIDER provprofile " + p);
            if (p.appId.equals(p.appIdPrefix + "." + bundleId)) {
                for (String fp : p.certFingerprints) {
                    if (fp.equals(signingIdentity.fingerprint)) {
                        System.err.println("YES, we have a MATCH!! " + p + " matches " + signingIdentity);
                        answer = p;
                    } else {
                        //      System.err.println("APPIDS match, but fps not, fp = "+fp+" and sfp = "+signingIdentity.fingerprint);
                    }
                }
            }
        }
        if (answer == null) {
            if (!bundleId.equals("*")) {
                // Try with the last component replaced with a wildcard
                if (bundleId.endsWith(".*")) {
                    bundleId = bundleId.substring(0, bundleId.length() - 2);
                }
                int lastDot = bundleId.lastIndexOf('.');
                if (lastDot != -1) {
                    bundleId = bundleId.substring(0, lastDot) + ".*";
                } else {
                    bundleId = "*";
                }
                answer = find(profiles, signingIdentity, bundleId, origBundleId);
            }
        }
        if (answer == null) {
            System.out.println("No provisioning profile found "
                    + "matching signing identity '" + signingIdentity.name
                    + "' and app bundle ID '" + origBundleId + "'");
        }
        System.out.println("will return PP " + answer);
        return answer;
    }

    public String getName() {
        return this.name;
    }

    public boolean isDevelopment() {
        return Type.Development.equals(type);
    }
    
    @Override
    public String toString() {
        return "ProvisioningProfile [type=" + type + ", file=" + file
                + ", uuid=" + uuid + ", name=" + name + ", appIdName="
                + appIdName + ", appIdPrefix=" + appIdPrefix + ", appId="
                + appId + ", creationDate=" + creationDate
                + ", expirationDate=" + expirationDate + ", certFingerprints="
                + certFingerprints + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((uuid == null) ? 0 : uuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProvisioningProfile other = (ProvisioningProfile) obj;
        if (uuid == null) {
            if (other.uuid != null)
                return false;
        } else if (!uuid.equals(other.uuid))
            return false;
        return true;
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }


}
