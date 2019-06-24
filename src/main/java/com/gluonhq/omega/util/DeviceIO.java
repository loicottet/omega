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

import jnr.ffi.Pointer;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 *
 * @author johan
 */
public class DeviceIO {

    private Pointer connectionPointer;
    private String appPath;

    private Map<String, String> env = new HashMap<>();
    private List<String> args = new ArrayList<>();

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();


    private MobileDeviceBridge mobileDeviceBridge = MobileDeviceBridge.instance;

    private static final int RECEIVE_TIMEOUT = 5000;

    public DeviceIO(Pointer connectionPointer, String appPath) {
        this.connectionPointer = connectionPointer;
        this.appPath = appPath;
    }

    public int rerouteIO() throws IOException  {
        Logger.logDebug("DeviceIO::reroute stdio");

        // Talk to the debugserver using the GDB remote protocol.
        // See https://sourceware.org/gdb/onlinedocs/gdb/Remote-Protocol.html.
        // This process has been determined by observing how Xcode talks to
        // the debugserver. To enable GDB remote protocol logging in Xcode
        // write the following to ~/.lldbinit:
        //   log enable -v -f /tmp/gdb-remote.log gdb-remote all
        // Disable ack mode
        sendGdbPacket("+");
        sendReceivePacket(encode("QStartNoAckMode"), "OK", true);
        sendGdbPacket("+");

        // Disable buffered IO. Xcode does it so we do it too.
        sendReceivePacket( encode("QEnvironment:NSUnbufferedIO=YES"), "OK", false);
        // Set environment variables
        for (Map.Entry<String, String> entry : env.entrySet()) {
            String cmd = String.format("QEnvironment:%s=%s", entry.getKey(), entry.getValue());
            sendReceivePacket( encode(cmd), "OK", false);
        }
        // Tell the debuserver to send threads:xxx,yyy,... in stop replies
        sendReceivePacket(encode("QListThreadsInStopReply"), "OK", false);
        // Initialize argv with the app path and args
        sendReceivePacket(encode("A" + encodeArgs(appPath)), "OK", false);
        // Make sure the launch was successful
        sendReceivePacket( encode("qLaunchSuccess"), "OK", false);
        // Continue
        sendGdbPacket(encode("c"));

        boolean wasInterrupted = false;
        try {
            while (true) {
                try {
                    String response = receiveGdbPacket();
                    String payload = decode(response);
                    if (payload.charAt(0) == 'W') {
                        // The app exited. The number following W is the exit code.
                        int exitCode = Integer.parseInt(payload.substring(1), 16);
                        return exitCode;
                    } else if (payload.charAt(0) == 'O') {
                        // Console output encoded as hex.
                        byte[] data = fromHex(payload.substring(1));
//                        if (appLauncherCallback != null) {
//                            data = appLauncherCallback.filterOutput(data);
//                        }
                        System.out.write(data);
                        //   stdout.write(data);
                    } else if (payload.charAt(0) == 'T') {
                        // Signal received. Just continue.
                        // The Continue packet looks like this (thread 0x2403 was interrupted by signal 0x0b):
                        //   $vCont;c:2603;c:2703;c:2803;c:2903;c:2a03;c:2b03;c:2c03;c:2d03;C0b:2403#ed
                        String signal = payload.substring(1, 3);
                        String data = payload.substring(3);
                        String threadId = data.replaceAll(".*thread:([0-9a-fA-F]+).*", "$1");
                        String allThreadIds = data.replaceAll(".*threads:([0-9a-fA-F,]+).*", "$1");
                        Set<String> ids = new TreeSet<>(Arrays.asList(allThreadIds.split(",")));
                        ids.remove(threadId);
                        StringBuilder sb = new StringBuilder("vCont;");
                        for (String id : ids) {
                            sb.append("c:").append(id).append(';');
                        }
                        sb.append('C').append(signal).append(':').append(threadId);
                        sendGdbPacket(encode(sb.toString()));
                    } else if (payload.charAt(0) == 'X') {
                        int signal = Integer.parseInt(payload.substring(1, 3), 16);
                        String data = payload.substring(3);
                        String description = null;
                        if (data.contains("description:")) {
                            description = new String(fromHex(data.replaceAll(".*description:([0-9a-fA-F]+).*", "$1")), "UTF8").trim();
                            description = description.trim();
                            description = description.isEmpty() ? null : description;
                        }
                        String message = signal > 0 ? "The app crashed with signal " + signal : "The app crashed";
                        if (description != null) {
                            message += ": " + description;
                        }
                        message += ". Check the device logs in Xcode (Window->Devices) for more info.";
                        throw new RuntimeException(message);
                    } else {
                        throw new RuntimeException("Unexpected response "
                                + "from debugserver: " + response);
                    }
                } catch (InterruptedIOException e) {
                    // Remember whether we were interrupted. kill() clears
                    // the thread's interrupted state and we want to reset it
                    // when we exit.
                    wasInterrupted = Thread.currentThread().isInterrupted();
                    kill();
                }
            }
        } finally {
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sendGdbPacket( String packet) throws IOException {
        //   System.err.println("Sending packet: " + packet);
        byte[] data = packet.getBytes("ASCII");
        while (true) {
            int sentBytes = mobileDeviceBridge.sendData(connectionPointer,data);
            if (sentBytes == data.length) {
                break;
            }
            data = Arrays.copyOfRange(data, sentBytes, data.length);
        }
    }

    private boolean receiveGdbAck() throws IOException {
        if (bufferedResponses.length() > 0) {
            char c = bufferedResponses.charAt(0);
            bufferedResponses.delete(0, 1);
            return c == '+';
        }

        byte[] buffer = new byte[1];
        mobileDeviceBridge.receiveData(connectionPointer, buffer, 0, buffer.length, RECEIVE_TIMEOUT);
        Logger.logDebug("Received ack: " + (char) buffer[0]);
        return buffer[0] == '+';
    }

    private void sendReceivePacket(String packet,
                                   String expectedResponse, boolean ackMode) throws IOException {

        sendGdbPacket(packet);
        if (ackMode) {
            receiveGdbAck();
        }
        String response = decode(receiveGdbPacket(RECEIVE_TIMEOUT));
        if (!expectedResponse.equals(response)) {
            if (response.startsWith("E")) {
                if ((response.length() > 5) && (response.toLowerCase().startsWith("elocked"))) {
                    Logger.logSevere("Device locked.");
                    throw new DeviceLockedException();
                }
                throw new RuntimeException("Launch failed: " + response.substring(1));
            }
            throw new RuntimeException("Launch failed: Unexpected response '"
                    + response + "' to command '" + decode(packet) + "'");
        }
    }
    private String receiveGdbPacket() throws IOException {
        return receiveGdbPacket(Integer.MAX_VALUE);
    }

    private StringBuilder bufferedResponses = new StringBuilder(4096);
    private byte[] buffer = new byte[4096];
    private volatile boolean killed = false;

    private String receiveGdbPacket( long timeout) throws IOException {
        int packetEnd = bufferedResponses.indexOf("#");
        if (packetEnd != -1 && bufferedResponses.length() - packetEnd > 2) {
            String packet = bufferedResponses.substring(0, packetEnd + 3);
            bufferedResponses.delete(0, packetEnd + 3);
            // System.err.println("Received packet: " + packet);
            return packet;
        }

        long deadline = System.currentTimeMillis() + timeout;
        while (true) {
            if (killed || Thread.currentThread().isInterrupted()) {
                killed = true;
                throw new InterruptedIOException();
            }
            int receivedBytes = mobileDeviceBridge.receiveData(connectionPointer, buffer, 0, buffer.length, 10);
            if (receivedBytes > 0) {
                bufferedResponses.append(new String(buffer, 0, receivedBytes, "ASCII"));
                packetEnd = bufferedResponses.indexOf("#");
                if (packetEnd != -1 && bufferedResponses.length() - packetEnd > 2) {
                    String packet = bufferedResponses.substring(0, packetEnd + 3);
                    bufferedResponses.delete(0, packetEnd + 3);
                    //  System.err.println("Received packet: " + packet);
                    return packet;
                }
            }
            if (System.currentTimeMillis() > deadline) {
                throw new IOException("Timeout, deadline passed");
            }
        }
    }


    private String decode(String packet) {
        int start = 1;
        if (packet.charAt(0) == '+' || packet.charAt(0) == '-') {
            start = 2;
        }
        int end = packet.lastIndexOf('#');
        return packet.substring(start, end);
    }


    private String encode(String cmd) {
        int checksum = 0;
        for (int i = 0; i < cmd.length(); i++) {
            checksum += cmd.charAt(i);
        }
        return String.format("$%s#%02x", cmd, checksum & 0xff);
    }

    private String encodeArgs(String appPath) {
        StringBuilder sb = new StringBuilder();
        String hex = toHex(appPath);
        sb.append(String.format("%d,0,%s", hex.length(), hex));
        for (int i = 0; i < args.size(); i++) {
            hex = toHex(args.get(i));
            sb.append(String.format(",%d,%d,%s", hex.length(), i + 1, hex));
        }
        return sb.toString();
    }
    private static String toHex(String s) {
        StringBuilder sb = new StringBuilder(s.length() * 2);
        byte[] bytes;
        try {
            bytes = s.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            throw new Error(e);
        }
        for (int i = 0; i < bytes.length; i++) {
            int c = bytes[i] & 0xff;
            sb.append(HEX_CHARS[c >> 4]);
            sb.append(HEX_CHARS[c & 0xf]);
        }
        return sb.toString();
    }

    private static byte fromHex(char c1, char c2) {
        int d = 0;
        if (c1 <= '9') {
            d = c1 - '0';
        } else {
            d = c1 - 'a' + 10;
        }
        d <<= 4;
        if (c2 <= '9') {
            d |= c2 - '0';
        } else {
            d |= c2 - 'a' + 10;
        }
        return (byte) d;
    }

    private static byte[] fromHex(String s) {
        int length = s.length();
        byte[] data = new byte[length / 2];
        for (int i = 0; i < (length >> 1); i++) {
            data[i] = fromHex(s.charAt(i * 2), s.charAt(i * 2 + 1));
        }
        return data;
    }

    private static byte[] fromHex(byte[] buffer, int offset, int length) {
        byte[] data = new byte[length / 2];
        for (int i = 0; i < (length >> 1); i++) {
            data[i] = fromHex((char)buffer[offset + i * 2], (char)buffer[offset + i * 2 + 1]);
        }
        return data;
    }

    private void kill() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}