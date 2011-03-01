/*
 * Copyright (c) 2010. Department of Family Medicine, McMaster University. All Rights Reserved.
 *
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * This software was written for the
 * Department of Family Medicine
 * McMaster University
 * Hamilton
 * Ontario, Canada
 */
package org.mule.providers.tcp.protocols;

//import org.mule.providers.MllpConnector;
//import com.webreach.mirth.connectors.mllp.TcpProtocol;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.providers.tcp.TcpProtocol;
import java.util.Arrays;

public class LlpProtocol implements TcpProtocol {

    private static final Log logger = LogFactory.getLog(LlpProtocol.class);
    private static final int BUFFER_SIZE = 8192;
    private char END_MESSAGE;
    private char START_MESSAGE;
    private char END_OF_RECORD;
    private char END_OF_SEGMENT;
    private boolean useLLP;
    //private MllpConnector _mllpConnector;

    public LlpProtocol() {
        logger.debug("Instantiated");
        this.END_MESSAGE = '\034';
        this.START_MESSAGE = '\013';
        this.END_OF_RECORD = '\r';
        this.END_OF_SEGMENT = '\r';

        this.useLLP = true;
    }

    public void setTcpConnector(/*MllpConnector mllpConnector*/) {
        logger.debug("setTcpConnector has been called");
        /*try {
        this._mllpConnector = mllpConnector;
        if (this._mllpConnector.getCharEncoding().equals("hex")) {
        this.START_MESSAGE = (char)Integer.decode(this._mllpConnector.getMessageStart()).intValue();
        this.END_MESSAGE = (char)Integer.decode(this._mllpConnector.getMessageEnd()).intValue();
        this.END_OF_RECORD = (char)Integer.decode(this._mllpConnector.getRecordSeparator()).intValue();
        this.END_OF_SEGMENT = (char)Integer.decode(this._mllpConnector.getSegmentEnd()).intValue();
        }
        else {
        this.START_MESSAGE = this._mllpConnector.getMessageStart().charAt(0);
        this.END_MESSAGE = this._mllpConnector.getMessageEnd().charAt(0);
        this.END_OF_RECORD = this._mllpConnector.getRecordSeparator().charAt(0);
        this.END_OF_SEGMENT = this._mllpConnector.getSegmentEnd().charAt(0);
        }
        }
        catch (Exception e) {
        e.printStackTrace();
        }*/
    }

    private byte[] readTCP(InputStream is) throws IOException {
        logger.debug("readTCP has been called");
        String charset = "UTF-8";//this._mllpConnector.getCharsetEncoding();
        UtilReader myReader = new UtilReader(is, charset);

        int c = 0;
        try {
            c = myReader.read();
        } catch (SocketException e) {
            logger.info("SocketException on read() attempt.  Socket appears to have been closed: " + e.getMessage());
            return null;
        } catch (SocketTimeoutException ste) {
            logger.info("SocketTimeoutException on read() attempt.  Socket appears to have been closed: " + ste.getMessage());
            return null;
        }

        if (c == -1) {
            logger.info("End of input stream reached. --readTCP");
            return null;
        }

        while (c != -1) {
            myReader.append((char) c);
            try {
                c = myReader.read();
            } catch (Exception e) {
                c = -1;
            }
        }

        return myReader.getBytes();
    }

    private byte[] readLLP(InputStream is) throws IOException {
        logger.debug("readLLP has been called");
        String charset = "UTF-8";//this._mllpConnector.getCharsetEncoding();
        UtilReader myReader = new UtilReader(is, charset);

        boolean end_of_message = false;
	logger.debug("after myReader created");
        int c = 0;
        try {
            c = myReader.read();
		logger.debug("myreader first read "+c);
        } catch (SocketException e) {
            logger.info("SocketException on read() attempt.  Socket appears to have been closed: " + e.getMessage());
            return null;
        } catch (SocketTimeoutException ste) {
            logger.info("SocketTimeoutException on read() attempt.  Socket appears to have been closed: " + ste.getMessage());
            return null;
        }

        if (c == -1) {
            logger.info("End of input stream reached.-- readLLP");
            return null;
        }

        while ((c != this.START_MESSAGE) && (c != -1)) {
            try {
                c = myReader.read();
		logger.debug("not start message reading "+c);
            } catch (Exception e) {
                c = -1;
            }
        }

        if (c == -1) {
            String message = myReader.toString();
            logger.info("Bytes received violate the MLLP: no start of message indicator received.\r\n" + message);
            return null;
        }

        myReader.append((char) c);
        logger.debug("Char "+c+" --- "+ (char) c);

        while (!end_of_message) {
            c = myReader.read();

            if (c == -1) {
                logger.info("Message violates the minimal lower protocol: message terminated without a terminating character.");
                return null;
            }
            myReader.append((char) c);
            if (c != this.END_MESSAGE) {
                continue;
            }
            if (this.END_OF_RECORD != 0) {
                try {
                    c = myReader.read();
                    if ((this.END_OF_RECORD != 0) && (c != this.END_OF_RECORD)) {
                        logger.info("Message terminator was: " + c + "  Expected terminator: " + this.END_OF_RECORD);
                        return null;
                    }
                    myReader.append((char) c);
                } catch (SocketException e) {
                    logger.info("SocketException on read() attempt.  Socket appears to have been closed: " + e.getMessage());
                } catch (SocketTimeoutException ste) {
                    logger.info("SocketTimeoutException on read() attempt.  Socket appears to have been closed: " + ste.getMessage());
                }
            }
            end_of_message = true;
        }
        logger.debug(myReader.toString());
        
        byte[] strippedBytes = Arrays.copyOfRange(myReader.getBytes(), 1, myReader.getBytes().length-1);
        logger.debug(strippedBytes);

        //Should i remove the formatting characters here?  Or just the first character



        return strippedBytes; //myReader.getBytes();
    }

    public byte[] read(InputStream is) throws IOException {
        if (this.useLLP) {
            return readLLP(is);
        }
        return readTCP(is);
    }

    public void write(OutputStream os, byte[] data)
            throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        dos.writeByte(this.START_MESSAGE);
        dos.write(data);
        dos.writeByte(this.END_MESSAGE);
        if (this.END_OF_RECORD != 0) {
            dos.writeByte(this.END_OF_RECORD);
        }
        try {
            dos.flush();
        } catch (SocketException se) {
            logger.debug("Socket closed while trying to flush");
        }
    }

    public boolean isUseLLP() {
        return this.useLLP;
    }

    public void setUseLLP(boolean useLLP) {
        this.useLLP = useLLP;
    }

    protected class UtilReader {

        InputStream byteReader = null;
        ByteArrayOutputStream baos = null;
        String charset = "";

        public UtilReader(InputStream is, String charset) {
            this.charset = charset;
            this.byteReader = is;
            this.baos = new ByteArrayOutputStream();
        }

        public int read() throws IOException {
            if (this.byteReader != null) {
                return this.byteReader.read();
            }
            return -1;
        }

        public void close() throws IOException {
            if (this.byteReader != null) {
                this.byteReader.close();
            }
        }

        public void append(int c) throws IOException {
            this.baos.write(c);
        }

        public String toString() {
            try {
                this.baos.flush();
                this.baos.close();
            } catch (Throwable t) {
                LlpProtocol.logger.error("Error closing the auxiliar buffer " + t);
            }
            try {
                return new String(this.baos.toByteArray(), this.charset);
            } catch (UnsupportedEncodingException e) {
                LlpProtocol.logger.error("Error: " + this.charset + " is unsupported, changing to default encoding");
            }
            return new String(this.baos.toByteArray());
        }

        public byte[] getBytes() throws IOException {
            this.baos.flush();
            this.baos.close();
            return this.baos.toByteArray();
        }

        public void reset() {
            this.baos.reset();
        }
    }
}
