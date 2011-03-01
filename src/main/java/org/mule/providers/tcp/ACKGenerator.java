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
package org.mule.providers.tcp;

//Will remove after commit
//import com.webreach.mirth.model.MessageObject.Protocol;

//import com.webreach.mirth.model.converters.ER7Serializer;
//import com.webreach.mirth.model.converters.SerializerFactory;
//import com.webreach.mirth.server.util.DateUtil;

import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



/**
 *
 * @author jaygallagher
 */
public class ACKGenerator {

    private final String DEFAULTDATEFORMAT = "yyyyMMddHHmmss";
    private static final Log logger = LogFactory.getLog(ACKGenerator.class);

    public String generateAckResponse(String message, String acknowledgementCode, String textMessage) throws Exception {
        return generateAckResponse(message, acknowledgementCode, textMessage, "yyyyMMddHHmmss", new String());
    }

    public String generateAckResponse(String message, String acknowledgementCode, String textMessage, String dateFormat, String errorMessage) throws Exception {
        if ((message == null) || (message.length() < 9)) {
            this.logger.error("Unable to parse, message is null or too short: " + message);
            throw new Exception("Unable to parse, message is null or too short: " + message);
        }

        boolean ackIsXML = false;
        char segmentDelim = '\r';

        char fieldDelim = '|';
        char componentDelim = '^';
        char repetitionSeparator = '~';
        char escapeCharacter = '\\';
        char subcomponentDelim = '&';

        String sendingApplication = "";
        String sendingFacility = "";
        String receivingApplication = "";
        String receivingFacility = "";
        String originalid = "";
        String procid = "";
        String procidmode = "";
        String version = "";

//Will Remove after committing.  XML encoding is not needed now.
//        if (protocol.equals(MessageObject.Protocol.XML)) {
//            ackIsXML = true;
//            message = SerializerFactory.getHL7Serializer(true, false, false).fromXML(message);
//        }

        fieldDelim = message.charAt(3);
        componentDelim = message.charAt(4);
        if (message.charAt(5) != fieldDelim) {
            repetitionSeparator = message.charAt(5);
            if (message.charAt(6) != fieldDelim) {
                escapeCharacter = message.charAt(6);
                if (message.charAt(7) != fieldDelim) {
                    subcomponentDelim = message.charAt(7);
                }
            }

        }

        int firstSegmentDelim = message.indexOf(String.valueOf(segmentDelim));
        String mshString;
        if (firstSegmentDelim != -1) {
            mshString = message.substring(0, firstSegmentDelim);
        } else {
            mshString = message;
        }

        Pattern fieldPattern = Pattern.compile(Pattern.quote(String.valueOf(fieldDelim)));
        Pattern componentPattern = Pattern.compile(Pattern.quote(String.valueOf(componentDelim)));

        String[] mshFields = fieldPattern.split(mshString);
        int mshFieldsLength = mshFields.length;

        if (mshFieldsLength > 2) {
            sendingApplication = componentPattern.split(mshFields[2])[0];
            if (mshFieldsLength > 3) {
                sendingFacility = componentPattern.split(mshFields[3])[0];
                if (mshFieldsLength > 4) {
                    receivingApplication = componentPattern.split(mshFields[4])[0];
                    if (mshFieldsLength > 5) {
                        receivingFacility = componentPattern.split(mshFields[5])[0];
                        if (mshFieldsLength > 9) {
                            originalid = componentPattern.split(mshFields[9])[0];
                            if (mshFieldsLength > 10) {
                                String[] msh11 = componentPattern.split(mshFields[10]);
                                procid = msh11[0];

                                if (msh11.length > 1) {
                                    procidmode = msh11[1];
                                }

                                if (mshFieldsLength > 11) {
                                    version = componentPattern.split(mshFields[11])[0];
                                }
                            }
                        }
                    }
                }
            }
        }

        if ((textMessage != null) && (textMessage.length() > 0)) {
            textMessage = fieldDelim + textMessage;
        } else {
            textMessage = new String();
        }
        if ((errorMessage != null) && (errorMessage.length() > 0)) {
            errorMessage = segmentDelim + "ERR" + fieldDelim + errorMessage;
        } else {
            errorMessage = new String();
        }

        if (version.length() == 0) {
            version = "2.4";
        }
        if (procid.length() == 0) {
            procid = "P";
        }
        if (originalid.length() == 0) {
            originalid = "1";
        }
        if (receivingApplication.length() == 0) {
            receivingApplication = "MIRTH";
        }


        Format formatter = new SimpleDateFormat(dateFormat);

        String timestamp = formatter.format(new Date());

        StringBuilder ackBuilder = new StringBuilder();

        ackBuilder.append("MSH" + fieldDelim + componentDelim + repetitionSeparator + escapeCharacter + subcomponentDelim + fieldDelim);

        ackBuilder.append(receivingApplication);
        ackBuilder.append(fieldDelim);
        ackBuilder.append(receivingFacility);
        ackBuilder.append(fieldDelim);
        ackBuilder.append(sendingApplication);
        ackBuilder.append(fieldDelim);
        ackBuilder.append(sendingFacility);
        ackBuilder.append(fieldDelim);
        ackBuilder.append(timestamp);
        ackBuilder.append(fieldDelim);
        ackBuilder.append(fieldDelim);
        ackBuilder.append("ACK");
        ackBuilder.append(fieldDelim);
        ackBuilder.append(timestamp);
        ackBuilder.append(fieldDelim);
        ackBuilder.append(procid);

        if ((procidmode != null) && (procidmode.length() > 0)) {
            ackBuilder.append(componentDelim);
            ackBuilder.append(procidmode);
        }

        ackBuilder.append(fieldDelim);
        ackBuilder.append(version);
        ackBuilder.append(segmentDelim);
        ackBuilder.append("MSA");
        ackBuilder.append(fieldDelim);
        ackBuilder.append(acknowledgementCode);
        ackBuilder.append(fieldDelim);
        ackBuilder.append(originalid);
        ackBuilder.append(textMessage);
        ackBuilder.append(errorMessage);
        ackBuilder.append(segmentDelim);

//Will Remove after committing.  XML encoding is not needed now.
//        if (ackIsXML) {
//            try {
//                return SerializerFactory.getHL7Serializer(true, false, false).toXML(ackBuilder.toString());
//            } catch (Throwable t) {
//                this.logger.warn("Cannot create the accept ACK for the message (" + message + ") from [" + ackBuilder.toString() + "] as an HL7 message");
//                return ackBuilder.toString();
//            }
//        }

        return ackBuilder.toString();
    }
}
