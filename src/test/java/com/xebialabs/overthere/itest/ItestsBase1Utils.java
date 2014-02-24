/*
 * Copyright (c) 2008-2014, XebiaLabs B.V., All rights reserved.
 *
 *
 * Overthere is licensed under the terms of the GPLv2
 * <http://www.gnu.org/licenses/old-licenses/gpl-2.0.html>, like most XebiaLabs Libraries.
 * There are special exceptions to the terms and conditions of the GPLv2 as it is applied to
 * this software, see the FLOSS License Exception
 * <http://github.com/xebialabs/overthere/blob/master/LICENSE>.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms
 * of the GNU General Public License as published by the Free Software Foundation; version 2
 * of the License.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth
 * Floor, Boston, MA 02110-1301  USA
 */
package com.xebialabs.overthere.itest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import com.google.common.io.InputSupplier;
import com.google.common.io.OutputSupplier;

import com.xebialabs.overthere.ConnectionOptions;
import com.xebialabs.overthere.Overthere;
import com.xebialabs.overthere.OverthereConnection;
import com.xebialabs.overthere.OverthereFile;
import com.xebialabs.overthere.RuntimeIOException;
import com.xebialabs.overthere.TemporaryFolder;
import com.xebialabs.overthere.ssh.SshConnectionType;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.io.ByteStreams.write;
import static com.xebialabs.overthere.OperatingSystemFamily.UNIX;
import static com.xebialabs.overthere.OperatingSystemFamily.WINDOWS;
import static com.xebialabs.overthere.cifs.CifsConnectionBuilder.CIFS_PROTOCOL;
import static com.xebialabs.overthere.cifs.CifsConnectionType.TELNET;
import static com.xebialabs.overthere.cifs.CifsConnectionType.WINRM_INTERNAL;
import static com.xebialabs.overthere.local.LocalConnection.LOCAL_PROTOCOL;
import static com.xebialabs.overthere.ssh.SshConnectionBuilder.CONNECTION_TYPE;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;

public abstract class ItestsBase1Utils {

    protected TemporaryFolder temp;
    protected String protocol;
    protected ConnectionOptions options;
    protected String expectedConnectionClassName;
    protected OverthereConnection connection;

    @BeforeClass
    public void setupHost() throws Exception {
        temp = new TemporaryFolder();
        temp.create();

        protocol = getProtocol();
        options = getOptions();
        expectedConnectionClassName = getExpectedConnectionClassName();

        connection = Overthere.getConnection(protocol, options);
    }

    protected abstract String getProtocol();

    protected abstract ConnectionOptions getOptions();

    protected abstract String getExpectedConnectionClassName();

    @AfterClass(alwaysRun = true)
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
            } catch (Exception exc) {
                System.out.println("Exception while disconnecting at end of test case:");
                exc.printStackTrace(System.out);
            } catch (AssertionError e) {
                System.out.println("Ignoring " + e);
            }
        }
        temp.delete();
    }

    @BeforeMethod
    public void assertConnection() {
        assertThat("We're not connected!", connection != null);
    }

    protected static byte[] readFile(final OverthereFile f) {
        try {
            return toByteArray(new InputSupplier<InputStream>() {
                @Override
                public InputStream getInput() throws IOException {
                    return f.getInputStream();
                }
            });
        } catch (IOException exc) {
            throw new RuntimeIOException(format("Cannot read file [%s]", f), exc);
        }

    }

    protected static void writeData(final OverthereFile f, byte[] data) {
        try {
            write(data, new OutputSupplier<OutputStream>() {
                @Override
                public OutputStream getOutput() throws IOException {
                    return f.getOutputStream();
                }
            });
        } catch (IOException exc) {
            throw new RuntimeIOException(format("Cannot write data to file [%s]", f), exc);
        }
    }

    protected static byte[] writeRandomBytes(final File f, final int size) throws IOException {
        byte[] randomBytes = generateRandomBytes(size);
        write(randomBytes, new OutputSupplier<OutputStream>() {
            @Override
            public OutputStream getOutput() throws IOException {
                return new FileOutputStream(f);
            }
        });
        return randomBytes;
    }

    protected static byte[] generateRandomBytes(final int size) {
        byte[] randomBytes = new byte[size];
        new Random().nextBytes(randomBytes);
        return randomBytes;
    }


    public boolean notLocal() {
        return !protocol.equals(LOCAL_PROTOCOL);
    }

    public boolean notCifs() {
        return !protocol.equals(CIFS_PROTOCOL);
    }

    public boolean withPassword() {
        return options.containsKey("password");
    }

    public boolean onUnix() {
        return connection.getHostOperatingSystem().equals(UNIX);
    }

    public boolean onWindows() {
        return connection.getHostOperatingSystem().equals(WINDOWS);
    }

    public boolean onlyCifs() {
        return protocol.equals(CIFS_PROTOCOL);
    }

    public boolean onlyCifsWinrm() {
        return protocol.equals(CIFS_PROTOCOL) && options.get(CONNECTION_TYPE).equals(WINRM_INTERNAL);
    }

    public boolean onlyCifsTelnet() {
        return protocol.equals(CIFS_PROTOCOL) && options.get(CONNECTION_TYPE).equals(TELNET);
    }

    public boolean notSftpCygwin() {
        return !onlySftpCygwin();
    }

    public boolean onlySftpCygwin() {
        return SshConnectionType.SFTP_CYGWIN.equals(options.get(CONNECTION_TYPE, null));
    }

    public boolean notSftpWinsshd() {
        return !onlySftpWinsshd();
    }

    public boolean onlySftpWinsshd() {
        return SshConnectionType.SFTP_WINSSHD.equals(options.get(CONNECTION_TYPE, null));
    }

    public boolean supportsProcess() {
        return connection.canStartProcess();
    }

    public boolean notSupportsProcess() {
        return !supportsProcess();
    }

    protected static Logger logger = LoggerFactory.getLogger(OverthereConnectionItestBase.class);

}
