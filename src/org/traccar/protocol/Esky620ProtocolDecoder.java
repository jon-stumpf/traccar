/*
 * Copyright 2014-2015 Jon S. Stumpf (http://github.com/jon-stumpf)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.protocol;

import java.net.SocketAddress;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;

import org.traccar.BaseProtocolDecoder;
import org.traccar.database.DataManager;
import org.traccar.helper.Log;
import org.traccar.model.ExtendedInfoFormatter;
import org.traccar.model.Position;

public class Esky620ProtocolDecoder extends BaseProtocolDecoder {

    public Esky620ProtocolDecoder(DataManager dataManager, String protocol, Properties properties) {
        super( dataManager, protocol, properties );
    }

    // GPS Report: "EO;1;864906020921972;RG;9+150408015802+44.57984+-74.71753+0.16+0+4301+1"

    private static final Pattern patternMessage = Pattern.compile(
            "E([LO])"        + ";" +     // eSky Header: Login or Observation
            "\\d+"           + ";" +     // Sequence Number (unused)
            "(\\d{15})"      + ";" +     // IMEI
            "(.+)"                       // Data
    );

    private static final Pattern patternLoginData = Pattern.compile (
            "(\\d{12})"                  // Time (YYMMDDhhmmss)
    );

    private static final Pattern patternReportDataGPS = Pattern.compile(
            "RG"             + ";"   +   // GPS Report Header
            "(\\d+)"         + "\\+" +   // Satellites
            "(\\d{12})"      + "\\+" +   // Timestamp (YYMMDDhhmmss)
            "(-?\\d+.\\d+)"  + "\\+" +   // Latitude
            "(-?\\d+.\\d+)"  + "\\+" +   // Longitude
            "(\\d+.\\d+)"    + "\\+" +   // Speed (m/s)
            "(\\d+)"         + "\\+" +   // Heading (degrees)
            "(\\d+)"         + "\\+" +   // Voltage
            "(\\d+)"                     // Message Type
    );
    
    private static final Pattern patternReportDataLBS = Pattern.compile(
            "RL"             + ";"   +   // LBS Report Header
            "(\\d+)"         + ","   +   // MNC
            "(\\d+)"         + ","   +   // Cell ID
            "(\\d+)"         + ","   +   // LAC
            "(\\d+)"         + ","   +   // MCC
            "(\\d{15})"      + ","   +   // IMEI
            "(.+)"           + "\\+" +   // LBS Mark (Character Encoding)
            "(\\d+)"         + "\\+" +   // Voltage
            "(\\d{12})"      + "\\+" +   // Timestamp (YYMMDDhhmmss)
            "(\\d+)"                     // Message Type
    );
    
    protected Object decodeGPSData( Matcher parser, String imei ) {
	Position position = new Position();
	ExtendedInfoFormatter extendedInfo = new ExtendedInfoFormatter(getProtocol());
	int index = 1;

        try {
            position.setDeviceId(getDataManager().getDeviceByImei(imei).getId());
        } catch(Exception error) {
            Log.warning("Unknown device - " + imei);
            return null;
        }

	// Satellites
	extendedInfo.set("satellites", parser.group(index++));

	// Date and Time
        try {
            DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
            position.setTime(df.parse(parser.group(index++)));
        } catch (ParseException error) {
            position.setTime(null);
        }
        
	// Latitude / Longitude / Validity
	position.setLatitude(Double.valueOf(parser.group(index++)));
	position.setLongitude(Double.valueOf(parser.group(index++)));

	position.setValid(! (position.getLatitude() == 0.0 && position.getLongitude() == 0.0));
            
	// Speed (m/s convert to knots because traccar assumes knots)
	position.setSpeed(Double.valueOf(parser.group(index++)) * 1.94384);
            
	// Heading
	position.setCourse(Double.valueOf(parser.group(index++)));
            
	// Voltage
	extendedInfo.set("voltage", parser.group(index++));
            
	// Message Type
	extendedInfo.set("messageType", parser.group(index++));
            
        // Extended info
	position.setExtendedInfo(extendedInfo.toString());

        // traccar requires the altitude to be set
        position.setAltitude(0.0);

	return position;
    }
    
    protected Object decodeMessage( Matcher parser ) {
        int index = 1;

        // Message Type
        String messageType = parser.group(index++);
        String imei = parser.group(index++);

        if ( ! messageType.equals("O") )
        {
            // We only handle Observation messages at this time

            return null;
        }
        
        String reportData = parser.group(index++);
        Matcher dataParser;

	if ( (dataParser = patternReportDataGPS.matcher(reportData)).matches() )
	{
            // GPS Report
            
	    return decodeGPSData(dataParser, imei);
        }

	// We only handle GPS Reports at this time

	return null;
    }
    
    @Override
    protected Object decode( ChannelHandlerContext ctx,
                             Channel channel,
                             Object msg ) throws Exception {

        String sentence = msg.toString();
        Matcher parser;
        
        if ( (parser = patternMessage.matcher(sentence)).matches() ) {
            return decodeMessage(parser);
        }
        
        // Unknown message type
        
        return null;
    }

}
