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
import org.jitsi.hammer.extension.*;
//import org.jitsi.hammer.utils.MediaDeviceChooser;
import org.jitsi.proxy.ProxyUser;
//import org.jitsi.hammer.extension.*;
import org.jitsi.proxy.utils.MediaDeviceChooser;
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
	/**
     * The <tt>Logger</tt> used by the <tt>Proxy</tt> class and its
     * instances for logging output.
     */
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
    private ProxyUser lUsers[] = null;
    private ProxyUser rUsers[] = null;
    
    
    /**
     * boolean used to know if the <tt>Proxy</tt> is started or not.
     */
    private boolean started = false;
    
    /**
     * Instantiate a <tt>Hammer</tt> object with <tt>numberOfUser</tt> virtual
     * users that will try to connect to the XMPP server and its videobridge
     * contained in <tt>host</tt>.
     *
     * @param host The information about the XMPP server to which all
     * virtual users will try to connect.
     * @param mdc
     * @param nickname The base of the nickname used by all the virtual users.
     * @param numberOfUser The number of virtual users this <tt>Hammer</tt>
     * will create and handle.
     */
    public Proxy(HostInfo host, HostInfo proxyHost, MediaDeviceChooser mdc, 
    		String nickname, int numberOfLuser, int numberOfRuser)
    {
        this.nickname = nickname+"_L";
        this.proxynickname = nickname+"_R";
        this.serverInfo = host;
        this.proxyServerInfo = proxyHost;
        this.mediaDeviceChooser = mdc;
        lUsers = new ProxyUser[numberOfLuser];
        rUsers = new ProxyUser[numberOfRuser];
        
        // construct lUsers
        for(int i = 0; i<lUsers.length; i++)
        {
            lUsers[i] = new ProxyUser(
                this.serverInfo,
                this.mediaDeviceChooser,
                this.nickname+"_"+i,
                false);
        }
        
        // construct rUsers
        for(int i = 0; i<rUsers.length; i++)
        {
            rUsers[i] = new ProxyUser(
                this.proxyServerInfo,
                this.mediaDeviceChooser,
                this.proxynickname+"_"+i,
                false);
        }
        
        logger.info(String.format("Luser and Ruser created : "
        		+ "%d lUsers were created, " + " %d rUsers were created "
        		+ " with a base nickname %s", numberOfLuser, numberOfRuser
        		, nickname));
    }
    
    // TODO: fix required -- Haoqin
    /**
     * Initialize the Proxy by launching the OSGi Framework and
     * installing/registering the needed bundle (LibJitis and more..).
     */
    public static void init()
    {
        /**
         * This code is a slightly modified copy of the one found in
         * startOSGi of the class ComponentImpl of jitsi-videobridge.
         *
         * This function run the activation of different bundle that are needed
         * These bundle are the one found in the <tt>BUNDLE</tt> array
         */
        synchronized (frameworkSyncRoot)
        {
            if (Proxy.framework != null)
                return;
        }

        logger.info("Start OSGi framework with the bundles : " + BUNDLES);
        FrameworkFactory frameworkFactory = new FrameworkFactoryImpl();
        Map<String, String> configuration = new HashMap<String, String>();
        BundleContext bundleContext = null;

        configuration.put(
            Constants.FRAMEWORK_BEGINNING_STARTLEVEL,
            Integer.toString(BUNDLES.length));

        Framework framework = frameworkFactory.newFramework(configuration);

        try
        {
            framework.init();

            bundleContext = framework.getBundleContext();

            for (int startLevelMinus1 = 0;
                startLevelMinus1 < BUNDLES.length;
                startLevelMinus1++)
            {
                int startLevel = startLevelMinus1 + 1;

                for (String location : BUNDLES[startLevelMinus1])
                {
                    Bundle bundle = bundleContext.installBundle(location);

                    if (bundle != null)
                    {
                        BundleStartLevel bundleStartLevel
                        = bundle.adapt(BundleStartLevel.class);

                        if (bundleStartLevel != null)
                            bundleStartLevel.setStartLevel(startLevel);
                    }
                }
            }

            framework.start();
        }
        catch (BundleException be)
        {
            throw new RuntimeException(be);
        }

        synchronized (frameworkSyncRoot)
        {
            Proxy.framework = framework;
        }

        logger.info("Add extension provider for :");
        ProviderManager manager = ProviderManager.getInstance();
        logger.info("Element name : " + MediaProvider.ELEMENT_NAME
            + ", Namespace : " + MediaProvider.NAMESPACE);
        manager.addExtensionProvider(
            MediaProvider.ELEMENT_NAME,
            MediaProvider.NAMESPACE,
            new MediaProvider());
        logger.info("Element name : " + SsrcProvider.ELEMENT_NAME
            + ", Namespace : " + SsrcProvider.NAMESPACE);
        manager.addExtensionProvider(
            SsrcProvider.ELEMENT_NAME,
            SsrcProvider.NAMESPACE,
            new SsrcProvider());
        logger.info("Element name : " + JingleIQ.ELEMENT_NAME
            + ", Namespace : " + JingleIQ.NAMESPACE);
        manager.addIQProvider(
            JingleIQ.ELEMENT_NAME,
            JingleIQ.NAMESPACE,
            new JingleIQProvider());
    }
    
    // TODO: fix required
    /**
     * Start the connection of all the virtual user that this <tt>Hammer</tt>
     * handles to the XMPP server(and then a MUC), using the <tt>Credential</tt>
     * given as arguments for the login.
     *
     * @param wait the number of milliseconds the Hammer will wait during the
     * start of two consecutive fake users.
     * @param disableStats whether statistics should be disabled.
     * @param credentials a list of <tt>Credentials</tt> used for the login
     * of the fake users.
     * @param overallStats enable or not the logging of the overall stats
     * computed at the end of the run.
     * @param allStats enable or not the logging of the all the stats collected
     * by the <tt>HammerStats</tt> during the run.
     * @param summaryStats enable or not the logging of the dummary stats
     * computed from all the streams' stats collected by the
     * <tt>HammerStats</tt> during the run.
     * @param statsPollingTime the number of seconds between two polling of stats
     * by the <tt>HammerStats</tt> run method.
     */
    public void start(int wait)
    {
        if(wait <= 0) wait = 1;
        if(started)
        {
            logger.warn("Proxy already started");
            return;
        }

        startUsersAnonymous(wait);
        this.started = true;
        logger.info("The Proxy has correctly been started");
    }
    
    private void startUsersAnonymous(int wait)
    {
        logger.info("Starting the Hammer : starting all "
                            + "FakeUsers with anonymous login");
        try
        {
            for(ProxyUser user : lUsers)
            {
                user.start();
                Thread.sleep(wait);
            }
            for(ProxyUser user : rUsers)
            {
            	user.start();
            	Thread.sleep(wait);
            }
        }
        catch (XMPPException e)
        {
            e.printStackTrace();
            System.exit(1);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }
    
    // TODO: fix required
    /**
     * Stop the streams of all the fake users created, and disconnect them
     * from the MUC and the XMPP server.
     * Also stop the <tt>HammerStats</tt> thread.
     */
    public void stop()
    {
        if (!this.started)
        {
            logger.warn("Proxy already stopped !");
            return;
        }

        logger.info("Stoppig the Proxy : stopping all ProxyUsers");
        for(ProxyUser lUser : lUsers)
        {
        	lUser.stop();
        }

        for(ProxyUser rUser : rUsers)
        {
        	rUser.stop();
        }

        /*
         * Stop the thread of the HammerStats, without using the Thread
         * instance hammerStatsThread, to allow it to cleanly stop.
         */
//        logger.info("Stopping the HammerStats and waiting for its thread to return");

        this.started = false;
        logger.info("The Proxy has been correctly stopped");
    }
}
