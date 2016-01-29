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

import java.io.*;
import java.lang.ref.*;
import java.util.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.jingle.*;

// import org.jitsi.eventadmin.*;
import org.jitsi.impl.neomedia.rtp.translator.*;
import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.service.neomedia.recording.*;
import org.jitsi.util.*;
import org.jitsi.util.event.*;
import org.osgi.framework.*;

/**
 * Represents a content in the terms of Jitsi Videobridge.
 *
 * @author Lyubomir Marinov
 * @author Boris Grozev
 */
public class Content
    extends PropertyChangeNotifier
    implements RTPTranslator.WriteFilter
{
    /**
     * The <tt>Logger</tt> used by the <tt>Content</tt> class and its instances
     * to print debug information.
     */
    private static final Logger logger = Logger.getLogger(Content.class);

    /**
     * The name of the property which specifies an event that a
     * <tt>VideoChannel</tt> of this <tt>Content</tt> has changed.
     */
    public static final String CHANNEL_MODIFIED_PROPERTY_NAME
        = "org.jitsi.videobridge.VideoChannel.mod";

    /**
     * The <tt>Channel</tt>s of this <tt>Content</tt> mapped by their IDs.
     */
    private final Map<String,ProxyUser> channels = new HashMap<>();

    /**
     * The <tt>Conference</tt> which has initialized this <tt>Content</tt>.
     */
    //private final Conference conference;
    private final Proxy proxy;

    /**
     * The indicator which determines whether {@link #expire()} has been called
     * on this <tt>Content</tt>.
     */
    // private boolean expired = false;

    /**
     * The local synchronization source identifier (SSRC) associated with this
     * <tt>Content</tt>, which is to to be pre-announced by the
     * <tt>Channel</tt>s of this <tt>Content</tt>.
     *
     * Currently, the value is taken into account in the case of RTP translation.
     */
    private long initialLocalSSRC = -1;

    /**
     * The <tt>MediaType</tt> of this <tt>Content</tt>. The implementation
     * detects the <tt>MediaType</tt> by looking at the {@link #name} of this
     * instance.
     */
    private final MediaType mediaType;

    /**
     * The name of this <tt>Content</tt>.
     */
    private final String name;

    private RTCPFeedbackMessageSender rtcpFeedbackMessageSender;

    /**
     * The <tt>Object</tt> which synchronizes the access to the RTP-level relays
     * (i.e. {@link #mixer} and {@link #rtpTranslator}) provided by this
     * <tt>Content</tt>.
     */
    private final Object rtpLevelRelaySyncRoot = new Object();

    /**
     * The <tt>RTPTranslator</tt> which forwards the RTP and RTCP traffic
     * between those {@link #channels} which use a translator as their RTP-level
     * relay.
     */
    private RTPTranslator rtpTranslator;

    /**
     * Initializes a new <tt>Content</tt> instance which is to be a part of a
     * specific <tt>Conference</tt> and which is to have a specific name.
     *
     * @param conference the <tt>Conference</tt> which is initializing the new
     * instance
     * @param name the name of the new instance
     */
    public Content(Proxy proxy, String name)
    {
        if (proxy == null)
            throw new NullPointerException("proxy");
        if (name == null)
            throw new NullPointerException("name");

        this.proxy = proxy;
        this.name = name;

        this.mediaType = MediaType.parseString(this.name);

        /*EventAdmin eventAdmin
            = this.conference.getVideobridge().getEventAdmin();

        if (eventAdmin != null)
        {
            eventAdmin.sendEvent(EventFactory.contentCreated(this));
        }
        
        touch();
        */
    }

    @Override
    public boolean accept(
            MediaStream source,
            byte[] buffer, int offset, int length,
            MediaStream destination,
            boolean data)
    {
        boolean accept = true;

        if (destination != null)
        {
            RtpChannel dst = RtpChannel.getChannel(destination);

            if (dst != null)
            {
                RtpChannel src
                    = (source == null) ? null : RtpChannel.getChannel(source);

                accept
                    = dst.rtpTranslatorWillWrite(
                            data,
                            buffer, offset, length,
                            src);
            }
        }
        return accept;
    }

    void askForKeyframes(Collection<Endpoint> endpoints)
    {
        for (Endpoint endpoint : endpoints)
        {
            for (RtpChannel channel : endpoint.getChannels(MediaType.VIDEO))
                channel.askForKeyframes();
        }
    }

    /**
     * Initializes a new <tt>RtpChannel</tt> instance and adds it to the list of
     * <tt>RtpChannel</tt>s of this <tt>Content</tt>. The new
     * <tt>RtpChannel</tt> instance has an ID which is unique within the list of
     * <tt>RtpChannel</tt>s of this <tt>Content</tt>.
     *
     * @param channelBundleId the ID of the channel-bundle that the created
     * <tt>RtpChannel</tt> is to be a part of (or <tt>null</tt> if it is not to
     * be a part of a channel-bundle).
     * @param transportNamespace transport namespace that will used by new
     * channel. Can be either {@link IceUdpTransportPacketExtension#NAMESPACE}
     * or {@link RawUdpTransportPacketExtension#NAMESPACE}.
     * @param initiator the value to use for the initiator field, or
     * <tt>null</tt> to use the default value.
     * @return the created <tt>RtpChannel</tt> instance.
     * @throws Exception
     */
    public RtpChannel createRtpChannel(String channelBundleId,
                                       String transportNamespace,
                                       Boolean initiator)
        throws Exception
    {
        RtpChannel channel = null;

        do
        {
            String id = generateChannelID();

            synchronized (channels)
            {
                if (!channels.containsKey(id))
                {
                    switch (getMediaType())
                    {
                    case AUDIO:
                        channel = new AudioChannel(
                                this, id, channelBundleId,
                                transportNamespace, initiator);
                        break;
                    case DATA:
                        /*
                         * MediaType.DATA signals an SctpConnection, not an
                         * RtpChannel.
                         */
                        throw new IllegalStateException("mediaType");
                    case VIDEO:
                        channel = new VideoChannel(
                                this, id, channelBundleId,
                                transportNamespace, initiator);
                        break;
                    default:
                        channel = new RtpChannel(
                            this, id, channelBundleId,
                            transportNamespace, initiator);
                        break;
                    }
                    channels.put(id, channel);
                }
            }
        }
        while (channel == null);

        // Initialize channel
        channel.initialize();

        Videobridge videobridge = getConference().getVideobridge();

        if (logger.isInfoEnabled())
        {
            /*
             * The method Videobridge.getChannelCount() should better be
             * executed outside synchronized blocks in order to reduce the risks
             * of causing deadlocks.
             */

            logger.info(
                    "Created channel " + channel.getID() + " of content "
                        + getName() + " of conference " + conference.getID()
                        + ". " + videobridge.getConferenceCountString());
        }

        return channel;
    }

    /**
     * XXX REMOVE
     * Returns a <tt>Channel</tt> of this <tt>Content</tt>, which has
     * <tt>ssrc</tt> in its list of received SSRCs, or <tt>null</tt> in case no
     * such <tt>Channel</tt> exists.
     * @param ssrc the ssrc to search for.
     * @return a <tt>Channel</tt> of this <tt>Content</tt>, which has
     * <tt>ssrc</tt> in its list of received SSRCs, or <tt>null</tt> in case no
     * such <tt>Channel</tt> exists.
     */
    public Channel findChannel(long ssrc)
    {
        // TODO we need to optimize the performance of this. We could use a
        // simple Map as a Cache for this loop.
        for (Channel channel : getChannels())
        {
            if (channel instanceof RtpChannel)
            {
                RtpChannel rtpChannel = (RtpChannel) channel;
                for (int channelSsrc : rtpChannel.getReceiveSSRCs())
                {
                    if (ssrc == (0xffffffffL & channelSsrc))
                        return channel;
                }
            }
        }

        return null;
    }

    /**
     * Returns a <tt>Channel</tt> of this <tt>Content</tt>, which has
     * <tt>receiveSSRC</tt> in its list of received SSRCs, or <tt>null</tt> in
     * case no such <tt>Channel</tt> exists.
     *
     * @param receiveSSRC the SSRC to search for.
     * @return a <tt>Channel</tt> of this <tt>Content</tt>, which has
     * <tt>receiveSSRC</tt> in its list of received SSRCs, or <tt>null</tt> in
     * case no such <tt>Channel</tt> exists.
     */
    Channel findChannelByReceiveSSRC(long receiveSSRC)
    {
        for (Channel channel : getChannels())
        {
            //FIXME: fix instanceof
            if(!(channel instanceof RtpChannel))
                continue;

            RtpChannel rtpChannel = (RtpChannel) channel;

            for (int channelReceiveSSRC : rtpChannel.getReceiveSSRCs())
            {
                if (receiveSSRC == (0xFFFFFFFFL & channelReceiveSSRC))
                    return channel;
            }
        }
        return null;
    }

    /**
     * Generates a new <tt>Channel</tt> ID which is not guaranteed to be unique.
     *
     * @return a new <tt>Channel</tt> ID which is not guaranteed to be unique
     */
    private String generateChannelID()
    {
        return
            Long.toHexString(
                    System.currentTimeMillis() + Proxy.RANDOM.nextLong());
    }

    /**
     * Returns a <tt>Channel</tt> from the list of <tt>Channel</tt>s of this
     * <tt>Content</tt> which has a specific ID.
     *
     * @param id the ID of the <tt>Channel</tt> to be returned
     * @return a <tt>Channel</tt> from the list of <tt>Channel</tt>s of this
     * <tt>Content</tt> which has the specified <tt>id</tt> if such a
     * <tt>Channel</tt> exists; otherwise, <tt>null</tt>
     */
    public Channel getChannel(String id)
    {
        Channel channel;

        synchronized (channels)
        {
            channel = channels.get(id);
        }

        // It seems the channel is still active.
        if (channel != null)
            channel.touch();

        return channel;
    }

    /**
     * Gets the number of <tt>Channel</tt>s of this <tt>Content</tt> that are
     * not expired.
     *
     * @return the number of <tt>Channel</tt>s of this <tt>Content</tt> that are
     * not expired.
     */
    public int getChannelCount()
    {
        int sz = 0;
        Channel[] cs = getChannels();
        if (cs != null && cs.length != 0)
        {
            for (int i = 0; i < cs.length; i++)
            {
                Channel c = cs[i];
                if (c != null && !c.isExpired())
                {
                    sz++;
                }
            }
        }
        return sz;
    }

    /**
     * Gets the <tt>Channel</tt>s of this <tt>Content</tt>.
     *
     * @return the <tt>Channel</tt>s of this <tt>Content</tt>
     */
    public Channel[] getChannels()
    {
        synchronized (channels)
        {
            Collection<Channel> values = channels.values();

            return values.toArray(new Channel[values.size()]);
        }
    }

    /**
     * Gets the <tt>Conference</tt> which has initialized this <tt>Content</tt>.
     *
     * @return the <tt>Conference</tt> which has initialized this
     * <tt>Content</tt>
     */
    public final Proxy getProxy()
    {
        return proxy;
    }

    /**
     * Returns the local synchronization source identifier (SSRC) associated
     * with this <tt>Content</tt>,
     *
     * @return the local synchronization source identifier (SSRC) associated
     * with this <tt>Content</tt>,
     */
    public long getInitialLocalSSRC()
    {
        return initialLocalSSRC;
    }

    /**
     * Returns a <tt>MediaService</tt> implementation (if any).
     *
     * @return a <tt>MediaService</tt> implementation (if any).
     */
    MediaService getMediaService()
    {
        return proxy.getMediaService();
    }

    /**
     * Gets the <tt>MediaType</tt> of this <tt>Content</tt>. The implementation
     * detects the <tt>MediaType</tt> by looking at the <tt>name</tt> of this
     * instance.
     *
     * @return the <tt>MediaType</tt> of this <tt>Content</tt>
     */
    public MediaType getMediaType()
    {
        return mediaType;
    }

    /**
     * Gets the name of this <tt>Content</tt>.
     *
     * @return the name of this <tt>Content</tt>
     */
    public final String getName()
    {
        return name;
    }


    RTCPFeedbackMessageSender getRTCPFeedbackMessageSender()
    {
        return rtcpFeedbackMessageSender;
    }

    /**
     * Gets the <tt>RTPTranslator</tt> which forwards the RTP and RTCP traffic
     * between the <tt>Channel</tt>s of this <tt>Content</tt> which use a
     * translator as their RTP-level relay.
     *
     * @return the <tt>RTPTranslator</tt> which forwards the RTP and RTCP
     * traffic between the <tt>Channel</tt>s of this <tt>Content</tt> which use
     * a translator as their RTP-level relay
     */
    public RTPTranslator getRTPTranslator()
    {
        synchronized (rtpLevelRelaySyncRoot)
        {
            /*
             * The expired field of Content is initially assigned true and the
             * only possible change of the value is from true to false, never
             * from false to true. Moreover, an existing rtpTranslator will be
             * disposed after the change of expired from true to false.
             * Consequently, no synchronization with respect to the access of
             * expired is required.
             */
            if (rtpTranslator == null)
            {
                rtpTranslator = getMediaService().createRTPTranslator();
                if (rtpTranslator != null)
                {
                    new RTPTranslatorWriteFilter(rtpTranslator, this);
                    if (rtpTranslator instanceof RTPTranslatorImpl)
                    {
                        RTPTranslatorImpl rtpTranslatorImpl
                            = (RTPTranslatorImpl) rtpTranslator;

                        /**
                         * XXX(gp) some thoughts on the use of initialLocalSSRC:
                         *
                         * 1. By using the initialLocalSSRC as the SSRC of the
                         * translator aren't we breaking the mixing
                         * functionality? because FMJ is going to use its "own"
                         * SSRC to for mixed stream, which remains unannounced.
                         *
                         * 2. By using an initialLocalSSRC we're losing the FMJ
                         * collision detection mechanism.
                         *
                         * The places that are involved in this have been tagged
                         * with TAG(cat4-local-ssrc-hurricane).
                         */
                        initialLocalSSRC = Videobridge.RANDOM.nextInt();

                        rtpTranslatorImpl.setLocalSSRC(initialLocalSSRC);

                        rtcpFeedbackMessageSender
                            = rtpTranslatorImpl.getRtcpFeedbackMessageSender();
                    }
                }
            }
            return rtpTranslator;
        }
    }
    
    /**
     *
     * @param channel
     */
    public void fireChannelChanged(RtpChannel channel)
    {
        firePropertyChange(CHANNEL_MODIFIED_PROPERTY_NAME, channel, channel);
    }

    /**
     * Tries to find an <tt>RtpChannel</tt> from this <tt>Content</tt> which
     * has <tt>ssrc</tt> in one of its FID source groups.
     * @param ssrc the SSRC to search for.
     * @return an <tt>RtpChannel</tt> with <tt>ssrc</tt> in one of its FID
     * source groups, or <tt>null</tt> if there is no such <tt>RtpChannel</tt>
     * in this <tt>Content</tt>.
     */
    public RtpChannel findChannelByFidSsrc(long ssrc)
    {
        if (expired)
            return null;

        for (Channel channel : getChannels())
        {
            if (! (channel instanceof RtpChannel))
            {
                continue;
            }
            RtpChannel rtpChannel = (RtpChannel) channel;

            if (rtpChannel.getFidPairedSsrc(ssrc) != -1)
            {
                return rtpChannel;
            }
        }

        return null;
    }

    private static class RTPTranslatorWriteFilter
        implements RTPTranslator.WriteFilter
    {
        private final WeakReference<RTPTranslator> rtpTranslator;

        private final WeakReference<RTPTranslator.WriteFilter> writeFilter;

        public RTPTranslatorWriteFilter(
                RTPTranslator rtpTranslator,
                RTPTranslator.WriteFilter writeFilter)
        {
            this.rtpTranslator = new WeakReference<>(rtpTranslator);
            this.writeFilter = new WeakReference<>(writeFilter);

            rtpTranslator.addWriteFilter(this);
        }

        @Override
        public boolean accept(
                MediaStream source,
                byte[] buffer, int offset, int length,
                MediaStream destination,
                boolean data)
        {
            RTPTranslator.WriteFilter writeFilter = this.writeFilter.get();
            boolean accept = true;

            if (writeFilter == null)
            {
                RTPTranslator rtpTranslator = this.rtpTranslator.get();

                if (rtpTranslator != null)
                    rtpTranslator.removeWriteFilter(this);
            }
            else
            {
                accept
                    = writeFilter.accept(
                            source,
                            buffer, offset, length,
                            destination,
                            data);
            }
            return accept;
        }
    }
}
