package org.jitsi.proxy;

//import org.jitsi.hammer.stats.*;
import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.framework.startlevel.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.provider.*;

import net.java.sip.communicator.impl.osgi.framework.launch.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

//import org.jitsi.hammer.FakeUser;
//import org.jitsi.hammer.Framework;
//import org.jitsi.hammer.Hammer;
//import org.jitsi.hammer.HostInfo;
//import org.jitsi.hammer.extension.*;
//import org.jitsi.hammer.utils.MediaDeviceChooser;
import org.jitsi.util.Logger;

import org.jitsi.proxy.utils.MediaDeviceChooser;

import java.util.*;

/**
*
* @author Bo Yan
*
* The <tt>Proxy</tt> class is the core class of the jitsi-proxy project.
* This class will try to create M virtual users to a XMPP server then to
* a MUC chatroom created by JitMeet (https://jitsi.org/Projects/JitMeet).
* And N virtual users to the Proxy XMPP server and MUC, respectively
*
* Each virtual user after succeeding in the connection to the JitMeet MUC
* receive an invitation to a audio/video conference. After receiving the
* invitation, each virtual user will positively reply to the invitation and
* start sending audio and video data to the jitsi-videobridge handling the
* conference.
*/

public class Proxy {
	
	private static final Logger logger
	= Logger.getLogger(Proxy.class);
	
	
	/**
     * nickname is used as the basename of the local hammer users
     * proxynickname is used as the basename of the proxy hammer users
     */
	private final String nickname;
	private final String proxynickname;
	
	private final HostInfo serverInfo;
	private final HostInfo proxyServerInfo;
	
    /**
     * The <tt>MediaDeviceChooser</tt> that will be used by all the
     * <tt>FakeUser</tt> to choose their <tt>MediaDevice</tt>
     */
    private final MediaDeviceChooser mediaDeviceChooser;


    /**
     * The <tt>org.osgi.framework.launch.Framework</tt> instance which
     * represents the OSGi instance launched by this <tt>ComponentImpl</tt>.
     */
    private static Framework framework;
    
    /**
     * The <tt>Object</tt> which synchronizes the access to {@link #framework}.
     */
    private static final Object frameworkSyncRoot = new Object();
    
    /**
     * The locations of the OSGi bundles (or rather of the path of the class
     * files of their <tt>BundleActivator</tt> implementations).
     * An element of the <tt>BUNDLES</tt> array is an array of <tt>String</tt>s
     * and represents an OSGi start level.
     */
    private static final String[][] BUNDLES =
    {

        {
            "net/java/sip/communicator/impl/libjitsi/LibJitsiActivator"
        }
    };
    
    /**
     * The array containing all the <tt>Luser</tt> and <tt>Ruser</tt> that this Hammer
     * handle, representing all the virtual user that will connect to the XMPP
     * server and start MediaStream with its jitsi-videobridge
     * 
     *                Local                          Proxy             |         Remote
     * 
     *  User1 \	   _____________	/ <-> LUser1 <-----------> RUser1 <-> \   _____________     
     *            |Cofo & Bridge|		 			   X                     |Cofo & Bridge|  
     *  User2 /						\ --> LUser2 ---->   ----> RUser2 --> /
     */
    private FakeUser Lusers[] = null;
    private FakeUser Rusers[] = null;
    
    // boolean used to know if the <tt>Proxy<tt> is started or not
    private boolean started = false;
    
    /**
     * 
     * @author Haoqin
     * 
     * Constructor for the Proxy class. In this part, we use part of ProxyInfo to initialize
     * HostInfo for Left Users and another part to initialize HostInfo for Right Users.
     * 
     * @param proxyInfo -- A class contains both the info of host of LUsers and host of RUsers.
     * @param mdc -- MediaDeviceChooser, TODO: check whether necessary or not.
     * @param nameLeft -- Nickname for left shadow users.
     * @param nameRight -- Nickname for right shadow users.
     * @param numberOfL -- Number of left shadow users.
     * @param numberOfR -- Number of right shadow users.
     */
    public Proxy(ProxyInfo proxyInfo, MediaDeviceChooser mdc, String nameLeft, String nameRight,
    		int numberOfL, int numberOfR) {
    	this.nickname = nameLeft;
    	this.proxynickname = nameRight;
    	
    	this.serverInfo = new HostInfo(
    			proxyInfo.getXMPPDomain(),
    			proxyInfo.getXMPPHostname(),
    			proxyInfo.getPort(),
    			proxyInfo.getMUCDomain(),
    			proxyInfo.getRoomName());
    	this.proxyServerInfo = new HostInfo(
    			proxyInfo.getXMPPProxy(),
    			proxyInfo.getProxyXMPPHostname(),
    			proxyInfo.getPort(),
    			proxyInfo.getProxyMUCDomain(),
    			proxyInfo.getRoomName());
    	
    	this.mediaDeviceChooser = mdc;
    	Lusers = new FakeUser[numberOfL];
    	Rusers = new FakeUser[numberOfR];
    	
    	/**
    	 * 
    	 * @author Haoqin
    	 * 
    	 * All users know their host where their conferences are and shadow users
    	 * which they should transit stream to. 
    	 * 
    	 * TODO: May need to add arguments to configure the transit direction
    	 * between left users and right users. For example, in the first demo, we
    	 * only want left users to transit stream to right users while we don't want
    	 * them to receive stream from right users. This problem can possibly solved
    	 * by not telling right users that they have left users.
    	 * 
    	 */
    	
    	for (int i = 0; i < Lusers.length; i++) {
    		Lusers[i] = new FakeUser(
    				this.serverInfo,
    				this.mediaDeviceChooser,
    				this.nickname + "_" + i);
    	}
    	logger.info(String.format("Left users created : %d fake users were created"
    			+ "with a base nickname %s", numberOfL, nickname));
    	
    	for (int i = 0; i < Rusers.length; i++) {
    		Rusers[i] = new FakeUser(
    				this.serverInfo,
    				this.mediaDeviceChooser,
    				this.proxynickname + "_" + i);
    	}
    	logger.info(String.format("Right users created : %d fake users were created"
    			+ "with a base nickname %s", numberOfR, proxynickname));
	}

	public static void init() {
		// TODO Auto-generated method stub
		
	}

	public void start(int interval) {
		// TODO Auto-generated method stub
		
	}

	public void stop() {
		// TODO Auto-generated method stub
		
	}
}
