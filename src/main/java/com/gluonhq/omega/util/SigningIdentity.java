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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 *
 * @author johan
 */
public class SigningIdentity  implements Comparable<SigningIdentity> {
    
    final String name;
    public final String fingerprint;
    static final Pattern PATTERN = Pattern.compile("^\\d+\\)\\s+([0-9A-F]+)\\s+\"([^\"]*)\"\\s*(.*)");

    SigningIdentity(String name, String fp) {
        this.name = name;
        this.fingerprint = fp;
    }
    
    public static List<SigningIdentity> list() {
        List<SigningIdentity> answer = new LinkedList<>();
        ProcessBuilder pb = new ProcessBuilder("security", "find-identity", "-v", "-p", "codesigning");
        try {
            pb.redirectErrorStream(true);
            Process p = pb.start();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = br.readLine()) != null) {
                   // logDebug("process line " + line);
                    Matcher matcher = PATTERN.matcher(line.trim());
                    if (matcher.find()) {
                    //    logDebug("MATCH");
                        String flags = matcher.group(3);
                        if (flags == null || !flags.contains("CSSMERR_TP_CERT_")) {
                            String fingerprint = matcher.group(1);
                            String name = matcher.group(2);
                       //     logDebug("add " + name + ", fingerprint - " + fingerprint);
                            answer.add(new SigningIdentity(name, fingerprint));
                        }
                    } else {
                  //     logDebug("NO MATCH");
                    }
                }
            }
            Collections.sort(answer);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return answer;
    }
    
          
    @Override
    public int compareTo(SigningIdentity o) {
        return this.name.compareToIgnoreCase(o.name);
    }
    
    @Override
    public String toString() {
        return "SigningIdentity [name=" + name + ", fingerprint=" + fingerprint
                + "]";
    }

    public static List<SigningIdentity> find(List<SigningIdentity> ids, String search) {
        List<SigningIdentity> answer = new LinkedList<>();
        if (search.startsWith("/") && search.endsWith("/")) {
            Pattern pattern = Pattern.compile(search.substring(1, search.length() - 1));
            for (SigningIdentity id : ids) {
                if (pattern.matcher(id.name).find()) {
                    answer.add(id);
//                    return id;
                }
            }
        } else {
            for (SigningIdentity id : ids) {
                if (id.name.startsWith(search) || id.fingerprint.equals(search.toUpperCase())) {
                    answer.add(id);
//                    return id;
                }
            }
        }
        if (answer.size() == 0) {
            System.out.println("Warning, no signing identities found");
        }
        return answer;
    }

    public static List<SigningIdentity> find(String search) {
        return find( list(), search);
    }
    
    
}
