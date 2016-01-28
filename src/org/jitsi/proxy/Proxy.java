package org.jitsi.proxy;

//import org.jitsi.hammer.stats.*;
import org.osgi.framework.*;
import org.osgi.framework.launch.*;
import org.osgi.framework.startlevel.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.provider.*;

import net.java.sip.communicator.impl.osgi.framework.launch.*;
import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

import org.jitsi.hammer.FakeUser;
import org.jitsi.hammer.Framework;
import org.jitsi.hammer.Hammer;
import org.jitsi.hammer.HostInfo;
import org.jitsi.hammer.extension.*;
import org.jitsi.hammer.utils.MediaDeviceChooser;
import org.jitsi.util.Logger;

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
	= Logger.getLogger(Hammer.class);
	
	
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
    
}
