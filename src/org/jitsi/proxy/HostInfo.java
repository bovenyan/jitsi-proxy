package org.jitsi.proxy;
/*
 * Copyright @ 2015 AT&T
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

/**
 * The class contains an number of information about the proxy server.
 * 
 * The XMPPdomain and XMPPhost attributes will be used by LUsers as HostInfo
 * while ProxyXMPPdomain and ProxyXMPPhost will be used by RUsers as their
 * own HostInfo.
 * The class contains an number of information about the host server.
 *
 * @author Boven
 */
public class HostInfo
{
    
    /**
     * The domain name of the XMPP server.
     */
    private String XMPPdomain;
    
    /**
     * The hostname used to access the XMPP server.
     */
    private String XMPPhost;
    
    /**
     * The hostname used by the XMPP server (used to access to the MUC).
     */
    private String MUCdomain;
    /**
     * The name of the MUC room that we'll use.
     */
    private String roomName;
    
    /**
     * The port used by the XMPP server.
     */
    private int port;

    
    /**
     * Instantiates a new <tt>HostInfo</tt> instance with default attribut. 
     */
    
    /**
     * @arg XMPPdomain the domain name of the XMPP server.
     * @arg XMPPhost the hostname of the XMPP server
     * @arg port the port number of the XMPP server
     * @arg MUCdomain the domain of the MUC server
     * @arg roomName the room name used for the MUC
     * Instantiates a new <tt>HostInfo</tt> instance
     * with all the informations needed.
     */
    public HostInfo(
            String XMPPdomain,
            String XMPPhost,
            int port,
            String MUCdomain,
            String roomName)
    {
        this.XMPPdomain = XMPPdomain;
        this.port = port;
        this.XMPPhost = XMPPhost;
        this.MUCdomain = MUCdomain;
        this.roomName = roomName;
    }
    
//    public HostInfo(
//            String XMPPdomain,
//            String ProxyXMPPdomain,
//            String XMPPhost,
//            String ProxyXMPPhost,
//            int port,
//            String MUCdomain,
//            String ProxyMUCdomain,
//            String roomName)
//    {
//    	this.XMPPdomain = XMPPdomain;
//    	this.ProxyXMPPdomain = ProxyXMPPdomain;
//        this.port = port;
//        this.XMPPhost = XMPPhost;
//        this.ProxyXMPPhost = ProxyXMPPhost;
//        this.MUCdomain = MUCdomain;
//        this.ProxyMUCdomain = ProxyMUCdomain;
//        this.roomName = roomName;
//    }
    
    /**
     * Get the domain of the XMPP server of this <tt>HostInfo</tt>
     * (in lower case).
     * @return the domain of the XMPP server (in lower case).
     */
    public String getXMPPDomain()
    {
        return this.XMPPdomain.toLowerCase();
    }
    
//    public String getXMPPProxy(){
//    	return this.ProxyXMPPdomain.toLowerCase();
//    }
    
    /**
     * Get the domain of the MUC server of this <tt>HostInfo</tt>
     * (in lower case).
     * @return the domain of the MUC server (in lower case).
     */
    public String getMUCDomain()
    {
        return this.MUCdomain.toLowerCase();
    }

//    public String getProxyMUCDomain()
//    {
//        return this.ProxyMUCdomain.toLowerCase();
//    }

    /**
     * Get the hostname of the XMPP server of this <tt>HostInfo</tt>
     * (in lower case).
     * @return the hostname of the XMPP server (in lower case).
     */
    public String getXMPPHostname()
    {
        return this.XMPPhost.toLowerCase();
    }

//    public String getProxyXMPPHostname()
//    {
//        return this.ProxyXMPPhost.toLowerCase();
//    }
    
    /**
     * Get the room name (to access a MUC) of this <tt>HostInfo</tt>
     * (in lower case).
     * @return the room name of a MUC (in lower case).
     */
    public String getRoomName()
    {
        return this.roomName.toLowerCase();
    }

    /**
     * Get the port number of the XMPP server of this <tt>HostInfo</tt>.
     * @return the port number of the XMPP server.
     */
    public int getPort()
    {
        return this.port;
    }
}
