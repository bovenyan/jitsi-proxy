package org.jitsi.proxy;

/*
 * Copyright @ 2015 Atlassian Pty Ltd
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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


import org.jitsi.proxy.utils.*;
import org.kohsuke.args4j.*;

/**
 * @author Bo Yan
 *
 * This class is used with args4j to easily deal with jitsi-hammer arguments
 * and options
 *
 */
public class CmdLineArguments
{
    /**
     * @Option is used by args4j to know what options can be set as arguments
     * for this program.
     */

    @Option(name="-h", aliases= { "--help", "-help" }, usage="Get help and usage"
        + " to run the program")
    private boolean help = false;

    /**
     * The domain name of the XMPP server.
     */
    @Option(name="-XMPPdomain",usage="The XMPP domain name",required=true)
    private String XMPPdomain;
    
    @Option(name="-ProxyXMPPdomain",usage="The XMPP domain name to Proxy",required=true)
    private String ProxyXMPPdomain;

    /**
     * The hostname used to access the XMPP server.
     */
    @Option(name="-XMPPhost",usage="The XMPP server hostname",required=true)
    private String XMPPhost;
    
    @Option(name="-ProxyXMPPhost",usage="The ProxyXMPP server hostname",required=true)
    private String ProxyXMPPhost;

    /**
     * The hostname used by the XMPP server (used to access to the MUC).
     */
    @Option(name="-MUCdomain",usage="The MUC domain name",required=true)
    private String MUCdomain;
    
    @Option(name="-ProxyMUCdomain",usage="The MUC domain name",required=true)
    private String ProxyMUCdomain;

    /**
     * The name of the MUC room that we'll use.
     */
    @Option(name="-room",usage="The MUC room name")
    private String roomName = "TestHammer";

    /**
     * The port used by the XMPP server.
     */
    @Option(name="-port",usage="The port of the XMPP server")
    private int port = 5222;

    /**
     * The number of Lusers and Rusers jitsi-proxy will create.
     */
    @Option(name="-Lusers",usage="The number of fake users the hammer will create")
    private int numberOfLusers = 1;
    
    @Option(name="-Rusers",usage="The number of fake users the hammer will create")
    private int numberOfRusers = 2;

    /**
     * The length of the run (in seconds).
     */
    @Option(name="-length",usage="The length of the run in second "
        + "(If zero or negative, the run will never stop)")
    private int runLength = 0;

    /**
     * The path of an ivf file that will be read for vp8 frame
     */
    @Option(name="-ivf",usage="The path of an ivf file that will"
        + " be read for the video stream")
    private String ivffile = null;

    /**
     * The path of a rtpdump file containing recorded VP8 RTP packets
     * that will be read for the video stream
     */
    @Option(name="-videortpdump",usage="The path of a rtpdump file"
        + " containing recorded VP8 RTP packets"
        + " that will be read for the video stream")
    private String videoRtpdumpFile = null;

    /**
     * The path of a rtpdump file containing recorded Opus RTP packets
     * that will be read for the video stream
     */
    @Option(name="-audiortpdump",usage="The path of a rtpdump file"
        + " containing recorded Opus RTP packets"
        + " that will be read for the audio stream")
    private String audioRtpdumpFile = null;

    /**
     * The number of milliseconds to wait before adding a new user.
     */
    @Option(name="-interval", usage="The interval in milliseconds between "
        + "the start of new users.")
    private int interval = 2000;

    /**
     * Create a ProxyInfo from the CLI options
     * @return a HostInfo created from the CLI options
     */
    public HostInfo getHostInfoFromArguments(){
    	return new HostInfo(XMPPdomain, XMPPhost, 
        		port, MUCdomain, roomName);
    }
    
    public HostInfo getProxyHostInfoFromArguments()
    {
        return new HostInfo(ProxyXMPPdomain, ProxyXMPPdomain, 
        		port, ProxyXMPPdomain, roomName);
    }

    /**
     * Get the number of Lusers jitsi-proxy will create.
     * @return the number of fake users jitsi-proxy will create.
     */
    public int getNumberOfLusers()
    {
        return numberOfLusers;
    }
    public int getNumberOfRusers()
    {
        return numberOfRusers;
    }

    /**
     * Get the length of the run (in seconds).
     * @return the length of the run (in seconds).
     */
    public int getRunLength()
    {
        return runLength;
    }

    /**
     * Get the path of an ivf file that will be read for vp8 frame if it was
     * given as option to the program, or null if not.
     * @return the path of an ivf file that will be read for vp8 frame if it was
     * given as option to the program, or null if not.
     */
    public String getIVFFile()
    {
        return ivffile;
    }

    /**
     * Get The path of a rtpdump file containing recorded VP8 RTP packets
     * that will be read for the video stream if it was
     * given as option to the program, or null if not.
     * @return The path of a rtpdump file containing recorded VP8 RTP packets
     * that will be read for the video stream if it was
     * given as option to the program, or null if not.
     */
    public String getVideoRtpdumpFile()
    {
        return videoRtpdumpFile;
    }

    /**
     * Get The path of a rtpdump file containing recorded Opus RTP packets
     * that will be read for the audio stream if it was
     * given as option to the program, or null if not.
     * @return The path of a rtpdump file containing recorded Opus RTP packets
     * that will be read for the audio stream if it was
     * given as option to the program, or null if not.
     */
    public String getAudioRtpdumpFile()
    {
        return audioRtpdumpFile;
    }

    /**
     * Create an return a <tt>MediaDeviceChooser</tt> based on the options and
     * arguments this <tt>CmdLineArguments</tt> has collected and parsed.
     * @return a <tt>MediaDeviceChooser</tt> based on the options and
     * arguments this <tt>CmdLineArguments</tt> has collected and parsed.
     */
    public MediaDeviceChooser getMediaDeviceChooser()
    {
        return new MediaDeviceChooser(this);
    }

    /**
     * Get the boolean of the help option : if true, the help will be displayed
     * @return the boolean of the help option
     */
    public boolean getHelpOption()
    {
        return help;
    }

    /**
     * Gets the number of milliseconds to wait before adding a new user.
     * @return the number of milliseconds to wait before adding a new user.
     */
    public int getInterval()
    {
        return interval;
    }

    /**
     * Get the <tt>List</tt> of <tt>Credentials</tt> read from the file
     * given with the "-credentials" options
     * @return
     */
}
