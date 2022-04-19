
//
//import com.example.TestMqtt.config.ConfigProperties;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnectMessage;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnectReturnCode;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttFixedHeader;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttGrantedQoS;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttIdentifierRejectedException;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessage;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessageFactory;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessageType;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPacketIdVariableHeader;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPublishMessage;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttQoS;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttSubAckPayload;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttSubscribeMessage;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttTopicSubscription;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttUnacceptableProtocolVersionException;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
//import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttVersion;
//import com.example.TestMqtt.mqtt.api.auth.Authenticator;
//import com.example.TestMqtt.mqtt.api.auth.AuthorizeResult;
//import SessionRegistry;
//import com.example.TestMqtt.mqtt.broker.util.Validator;
//import com.example.TestMqtt.mqtt.paq.ClientInfoByVenueReq;
//import com.example.TestMqtt.mqtt.paq.RssiSubscribe;
//import com.example.TestMqtt.mqtt.util.Topics;
//import com.example.TestMqtt.mqtt.util.UUIDs;
//import com.example.TestMqtt.payload.DeviceMessageInMqtt;
//import io.netty.buffer.ByteBuf;
//import io.netty.buffer.ByteBufUtil;
//import io.netty.buffer.Unpooled;
//import io.netty.channel.ChannelHandlerContext;
//import io.netty.handler.codec.MessageToMessageDecoder;
//import io.netty.handler.timeout.IdleState;
//import io.netty.handler.timeout.IdleStateEvent;
//import io.netty.handler.timeout.IdleStateHandler;
//import org.apache.commons.lang3.ArrayUtils;
//import org.apache.commons.lang3.StringUtils;
//import org.apache.commons.lang3.exception.ExceptionUtils;
//import org.apache.commons.lang3.time.DateUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.TaskScheduler;
//
//import java.io.IOException;
//import java.util.Date;
//import java.util.List;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.regex.Matcher;
//import java.util.regex.Pattern;
//
//public class MqttProtocolHandler extends MessageToMessageDecoder<MqttMessage> {
//
//    private static Logger logger = LoggerFactory.getLogger(MqttProtocolHandler.class);
//
//    private static Pattern venueRegex = Pattern.compile("(3.0|2.1)/LOC/(.*)/L");
//
//    // session state
//    private MqttVersion version;
//    private String clientId;
//    private String userName;
//    private String brokerId;
//    private boolean connected;
//    private boolean cleanSession;
//    private int keepAlive = 120;
//    private int keepAliveMax = 65535;
//    private ConfigProperties properties;
//
//    private final Authenticator authenticator;
//    private final SessionRegistry registry;
//    private final Validator validator;
//
//    private MqttPublishMessage willMessage;
//
//    @Autowired
//    TaskScheduler scheduler;
//
//    AtomicInteger id = new AtomicInteger(100);
//    private int getId() {
//        return id.updateAndGet(x -> (x + 1) % 32768);
//    }
//
//    private class RSSISubscribeTask implements Runnable {
//
//        private String clientId, venue, topicVersion;
//        private int anon;
//        private int subtype;
//
//        public RSSISubscribeTask(
//                String clientId, String venue, String topicVersion, int subtype, int anon) {
//            this.clientId = clientId;
//            this.venue = venue;
//            this.anon = anon;
//            this.subtype = subtype;
//            this.topicVersion = topicVersion;
//        }
//
//        @Override
//        public void run() {
//            RssiSubscribe ps = new RssiSubscribe(subtype, anon);
//
//            int mid = getId();
//
//            MqttFixedHeader fh =
//                    new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0);
//            MqttPublishVariableHeader vh =
//                    MqttPublishVariableHeader.from(topicVersion + "/LOC/" + venue + "/LS/PAQ", mid);
//
//            ByteBuf payload = ps.toByteBuf();
//
//            MqttPublishMessage publish = new MqttPublishMessage(fh, vh, payload);
//            if (logger.isTraceEnabled()) {
//                logger.trace(
//                        "Sent to {}:\n{} ", vh.toString(), ByteBufUtil.prettyHexDump(publish.content()));
//            }
//            registry.sendMessage(publish, clientId, mid, true);
//            logger.trace("Sent subscription  {} to {}", publish, clientId);
//        }
//    }
//
//    private class ClientInfoTask implements Runnable {
//
//        private String clientId, venue, topicVersion;
//
//        public ClientInfoTask(String clientId, String venue, String topicVersion) {
//            this.clientId = clientId;
//            this.venue = venue;
//            this.topicVersion = topicVersion;
//        }
//
//        @Override
//        public void run() {
//
//            ClientInfoByVenueReq ps = new ClientInfoByVenueReq(venue);
//
//            int mid = getId();
//            MqttFixedHeader fh =
//                    new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0);
//            MqttPublishVariableHeader vh =
//                    MqttPublishVariableHeader.from(topicVersion + "/LOC/" + venue + "/LS/PAQ", mid);
//
//            ByteBuf payload = ps.toByteBuf();
//
//            MqttPublishMessage publish = new MqttPublishMessage(fh, vh, payload);
//            if (logger.isTraceEnabled()) {
//                logger.trace(
//                        "Sent to {}:\n{} ", vh.toString(), ByteBufUtil.prettyHexDump(publish.content()));
//            }
//            registry.sendMessage(publish, clientId, mid, true);
//            logger.trace("Sent client info request to {}", clientId);
//        }
//    }
//
//
//    public MqttProtocolHandler(Authenticator authenticator, SessionRegistry registry, Validator validator, ConfigProperties properties) {
//        this.authenticator = authenticator;
//        this.registry = registry;
//        this.validator = validator;
//        this.properties = properties;
//        this.brokerId = properties.getMqtt().getBroker_id();
//    }
//
//    @Override
//    protected void decode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out) throws Exception {
//        System.out.println("Message is : " + msg.decoderResult() + " type : "+ msg);
//        if (msg.decoderResult().isFailure()) {
//            Throwable cause = msg.decoderResult().cause();
//            logger.error(
//                    "Protocol violation: Invalid message {}",
//                    ExceptionUtils.getMessage(msg.decoderResult().cause()));
//            if (cause instanceof MqttUnacceptableProtocolVersionException) {
//                // Send back CONNACK if the protocol version is invalid
//                this.registry.sendMessage(
//                        ctx,
//                        MqttMessageFactory.newMessage(
//                                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                                new MqttConnAckVariableHeader(
//                                        MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false),
//                                null),
//                        "INVALID",
//                        null,
//                        true);
//            } else if (cause instanceof MqttIdentifierRejectedException) {
//                // Send back CONNACK if the client id is invalid
//                this.registry.sendMessage(
//                        ctx,
//                        MqttMessageFactory.newMessage(
//                                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                                new MqttConnAckVariableHeader(
//                                        MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false),
//                                null),
//                        "INVALID",
//                        null,
//                        true);
//            }
//            ctx.close();
//            return;
//        }
//
//        switch (msg.fixedHeader().messageType()) {
//            case CONNECT:
//                onConnect(ctx, (MqttConnectMessage) msg);
//                break;
//            case PUBLISH:
//                onPublish(ctx, (MqttPublishMessage) msg, out);
//                break;
//            case PUBACK:
//                onPubAck(ctx, msg);
//                break;
//            case PUBREC:
//                onPubRec(ctx, msg);
//                break;
//            case PUBREL:
//                onPubRel(ctx, msg);
//                break;
//            case PUBCOMP:
//                onPubComp(ctx, msg);
//                break;
//            case SUBSCRIBE:
//                onSubscribe(ctx, (MqttSubscribeMessage) msg);
//                break;
//            case UNSUBSCRIBE:
//                onUnsubscribe(ctx, (MqttUnsubscribeMessage) msg);
//                break;
//            case PINGREQ:
//                onPingReq(ctx);
//                break;
//            case DISCONNECT:
//                onDisconnect(ctx);
//                break;
//        }
//    }
//
//    private void onConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
//        logger.trace("Start handling CONNECT message");
//        System.out.println("Start handling CONNECT message");
//        this.version =
//                MqttVersion.fromProtocolNameAndLevel(
//                        msg.variableHeader().protocolName(), (byte) msg.variableHeader().protocolLevel());
//        this.clientId = msg.payload().clientId();
//        this.cleanSession = msg.variableHeader().cleanSession();
//        if (msg.variableHeader().keepAlive() > 0
//                && msg.variableHeader().keepAlive() <= this.keepAliveMax) {
//            this.keepAlive = msg.variableHeader().keepAlive();
//        }
//
//        if (StringUtils.isBlank(this.clientId)) {
//            if (!this.cleanSession) {
//                logger.debug(
//                        "Protocol violation: Empty client id with clean session 0, send CONNACK and disconnect the client");
//                this.registry.sendMessage(
//                        ctx,
//                        MqttMessageFactory.newMessage(
//                                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                                new MqttConnAckVariableHeader(
//                                        MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false),
//                                null),
//                        "INVALID",
//                        null,
//                        true);
//                ctx.close();
//                return;
//            } else {
//                this.clientId = UUIDs.shortUuid();
//            }
//        }
//
//        // Validate clientId based on configuration
//        else if (!this.validator.isClientIdValid(this.clientId)) {
//            logger.debug(
//                    "Protocol violation: Client id {} not valid based on configuration, send CONNACK and disconnect the client",
//                    this.clientId);
//            this.registry.sendMessage(
//                    ctx,
//                    MqttMessageFactory.newMessage(
//                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                            new MqttConnAckVariableHeader(
//                                    MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false),
//                            null),
//                    this.clientId,
//                    null,
//                    true);
//            ctx.close();
//            return;
//        }
//
//        // A Client can only send the CONNECT Packet once over a Network Connection. The Server MUST
//        // process a second CONNECT Packet sent from a Client as a protocol violation and disconnect the
//        // Client
//        if (this.connected) {
//            logger.debug(
//                    "Protocol violation: Second CONNECT packet sent from client {}, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        boolean userNameFlag = msg.variableHeader().userNameFlag();
//        boolean passwordFlag = msg.variableHeader().passwordFlag();
//        this.userName = msg.payload().userName();
//        String password = msg.payload().password();
//        boolean malformed = false;
//        // If the User Name Flag is set to 0, a user name MUST NOT be present in the payload
//        // If the User Name Flag is set to 1, a user name MUST be present in the payload
//        // If the Password Flag is set to 0, a password MUST NOT be present in the payload
//        // If the Password Flag is set to 1, a password MUST be present in the payload
//        // If the User Name Flag is set to 0, the Password Flag MUST be set to 0
//        // Validate User Name based on configuration
//        // Validate Password based on configuration
//        if (userNameFlag) {
//            if (StringUtils.isBlank(this.userName) || !this.validator.isUserNameValid(this.userName))
//                malformed = true;
//        } else {
//            if (StringUtils.isNotBlank(this.userName) || passwordFlag) malformed = true;
//        }
//        if (passwordFlag) {
//            if (StringUtils.isBlank(password) || !this.validator.isPasswordValid(password))
//                malformed = true;
//        } else {
//            if (StringUtils.isNotBlank(password)) malformed = true;
//        }
//        if (malformed) {
//            logger.debug(
//                    "Protocol violation: Bad user name or password from client {}, send CONNACK and disconnect the client",
//                    this.clientId);
//            this.registry.sendMessage(
//                    ctx,
//                    MqttMessageFactory.newMessage(
//                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                            new MqttConnAckVariableHeader(
//                                    MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, false),
//                            null),
//                    this.clientId,
//                    null,
//                    true);
//            ctx.close();
//            return;
//        }
//
//        logger.debug(
//                "Message received: Received CONNECT message from client {} user {}",
//                this.clientId,
//                this.userName);
//
//        AuthorizeResult result = this.authenticator.authConnect(this.clientId, this.userName, password);
//        // Authorize successful
//        if (result == AuthorizeResult.OK) {
//            logger.trace(
//                    "Authorization CONNECT succeeded for client {} user {}", this.clientId, this.userName);
//
//            boolean sessionPresent = !this.cleanSession;
//
//            // The first packet sent from the Server to the Client MUST be a CONNACK Packet
//            logger.trace("Send CONNACK back to client {}", this.clientId);
//            this.registry.sendMessage(
//                    ctx,
//                    MqttMessageFactory.newMessage(
//                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                            new MqttConnAckVariableHeader(
//                                    MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent),
//                            null),
//                    this.clientId,
//                    null,
//                    true);
//
//            // If CleanSession is set to 0, the Server MUST resume communications with the Client based on
//            // state from
//            // the current Session (as identified by the Client identifier). If there is no Session
//            // associated with the Client
//            // identifier the Server MUST create a new Session. The Client and Server MUST store the
//            // Session after
//            // the Client and Server are disconnected. After the disconnection of a Session that had
//            // CleanSession set to 0, the Server MUST store further QoS 1 and QoS 2 messages that match
//            // any
//            // subscriptions that the client had at the time of disconnection as part of the Session
//            // state.
//            // It MAY also store QoS 0 messages that meet the same criteria.
//            // The Session state in the Server consists of:
//            // The existence of a Session, even if the rest of the Session state is empty.
//            // The Client's subscriptions.
//            // QoS 1 and QoS 2 messages which have been sent to the Client, but have not been completely
//            // acknowledged.
//            // QoS 1 and QoS 2 messages pending transmission to the Client.
//            // QoS 2 messages which have been received from the Client, but have not been completely
//            // acknowledged.
//            // Optionally, QoS 0 messages pending transmission to the Client.
//            if (!this.cleanSession) {
//                logger.trace(
//                        "Clear session state for client {} because former connection is clean session",
//                        this.clientId);
//            }
//
//            // If CleanSession is set to 1, the Client and Server MUST discard any previous Session and
//            // start a new
//            // one. This Session lasts as long as the Network Connection. State data associated with this
//            // Session
//            // MUST NOT be reused in any subsequent Session.
//            // When CleanSession is set to 1 the Client and Server need not process the deletion of state
//            // atomically.
//
//            // If the ClientId represents a Client already connected to the Server then the Server MUST
//            // disconnect the existing Client
//            ChannelHandlerContext lastSession = this.registry.removeSession(this.clientId);
//            if (lastSession != null) {
//                logger.trace("Try to disconnect existed client {}", this.clientId);
//                lastSession.close();
//            }
//
//            // If the Will Flag is set to 1 this indicates that, if the Connect request is accepted, a
//            // Will Message MUST be
//            // stored on the Server and associated with the Network Connection. The Will Message MUST be
//            // published
//            // when the Network Connection is subsequently closed unless the Will Message has been deleted
//            // by the
//            // Server on receipt of a DISCONNECT Packet.
//            String willTopic = msg.payload().willTopic();
//            String willMessage = msg.payload().willMessage();
//            if (msg.variableHeader().willFlag()
//                    && StringUtils.isNotEmpty(willTopic)
//                    && this.validator.isTopicNameValid(willTopic)
//                    && StringUtils.isNotEmpty(willMessage)) {
//                logger.trace("Keep WILL message on topic {} for client {}", willTopic, this.clientId);
//
//                this.willMessage =
//                        (MqttPublishMessage)
//                                MqttMessageFactory.newMessage(
//                                        new MqttFixedHeader(
//                                                MqttMessageType.PUBLISH,
//                                                false,
//                                                msg.variableHeader().willQos(),
//                                                msg.variableHeader().willRetain(),
//                                                0),
//                                        MqttPublishVariableHeader.from(willTopic),
//                                        Unpooled.wrappedBuffer(willMessage.getBytes()));
//            }
//
//            // If the Keep Alive value is non-zero and the Server does not receive a Control Packet from
//            // the Client
//            // within one and a half times the Keep Alive time period, it MUST disconnect the Network
//            // Connection to the
//            // Client as if the network had failed
//            logger.trace("Update idleHandler for client {}", this.clientId);
//            if (ctx.pipeline().names().contains("idleHandler")) ctx.pipeline().remove("idleHandler");
//            ctx.pipeline()
//                    .addFirst("idleHandler", new IdleStateHandler(0, 0, Math.round(this.keepAlive * 1.5f)));
//
//            // Save connection state, add to local registry
//            logger.trace("Save client {} connection state in registry", this.clientId);
//            this.connected = true;
//            this.registry.saveSession(this.clientId, ctx);
//
//            // Authorize failed
//        } else {
//            logger.trace(
//                    "Authorization CONNECT failed {} for client {}, send CONNACK and disconnect the client",
//                    result,
//                    this.clientId);
//            this.registry.sendMessage(
//                    ctx,
//                    MqttMessageFactory.newMessage(
//                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                            new MqttConnAckVariableHeader(
//                                    MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, false),
//                            null),
//                    this.clientId,
//                    null,
//                    true);
//            ctx.close();
//        }
//
//        logger.trace("Finish handling CONNECT message for client {}", this.clientId);
//    }
//
//    public void onPublish(ChannelHandlerContext ctx, MqttPublishMessage msg, List<Object> out) {
//        logger.trace("Start handling PUBLISH message for client {}", this.clientId);
//        System.out.println("On publish ...........");
//
//        if (!this.connected) {
//            logger.debug(
//                    "Protocol violation: Client {} must first sent a CONNECT message, now received PUBLISH message, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        // boolean dup = msg.fixedHeader().dup();
//        MqttQoS qos = msg.fixedHeader().qos();
//        boolean retain = msg.fixedHeader().retain();
//        String topicName = msg.variableHeader().topicName();
//        int packetId = msg.variableHeader().packetId();
//
//        // The Topic Name in the PUBLISH Packet MUST NOT contain wildcard characters
//        // Validate Topic Name based on configuration
//        if (!this.validator.isTopicNameValid(topicName)) {
//            logger.debug(
//                    "Protocol violation: Client {} sent PUBLISH message contains invalid topic name {}, disconnect the client",
//                    this.clientId,
//                    topicName);
//            ctx.close();
//            return;
//        }
//
//        // The Packet Identifier field is only present in PUBLISH Packets where the QoS level is 1 or 2.
//        if (packetId <= 0 && (qos == MqttQoS.AT_LEAST_ONCE || qos == MqttQoS.EXACTLY_ONCE)) {
//            logger.debug(
//                    "Protocol violation: Client {} sent PUBLISH message does not contain packet id, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        List<String> topicLevels = Topics.sanitizeTopicName(topicName);
//
//        logger.debug(
//                "Message received: Received PUBLISH message from client {} user {} topic {}",
//                this.clientId,
//                this.userName,
//                topicName);
//        if (logger.isTraceEnabled()) {
//            logger.trace("Received:\n{} ", ByteBufUtil.prettyHexDump(msg.content()));
//        }
//        // try (FileOutputStream fos =
//        // new FileOutputStream("target/binTest-" + id.incrementAndGet() + ".bytes")) {
//        // msg.content().markReaderIndex();
//        // msg.content().readBytes(fos, msg.content().readableBytes());
//        // msg.content().resetReaderIndex();
//        // } catch (FileNotFoundException e) {
//        // // TODO Auto-generated catch block
//        // e.printStackTrace();
//        // } catch (IOException e) {
//        // // TODO Auto-generated catch block
//        // e.printStackTrace();
//        // }
//
//        if (topicName.startsWith("device/ble")) {
//            out.add(DeviceMessageInMqtt.of(msg.content(), topicName));
//        }
//
//        logger.trace("Send PUBACK back to client {}", this.clientId);
//        this.registry.sendMessage(
//                ctx,
//                MqttMessageFactory.newMessage(
//                        new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                        MqttPacketIdVariableHeader.from(packetId),
//                        null),
//                this.clientId,
//                packetId,
//                true);
//
//        logger.trace("Finish handling PUBLISH message for client {}", this.clientId);
//    }
//
//    private void onPubAck(ChannelHandlerContext ctx, MqttMessage msg) {
//        logger.trace("Start handling PUBACK message for client {}", this.clientId);
//        System.out.println("On pubAck ...........");
//
//        if (!this.connected) {
//            logger.debug(
//                    "Protocol violation: Client {} must first sent a CONNECT message, now received PUBACK message, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        logger.debug(
//                "Message received: Received PUBACK message from client {} user {}",
//                this.clientId,
//                this.userName);
//
//        MqttPacketIdVariableHeader variable = (MqttPacketIdVariableHeader) msg.variableHeader();
//        int packetId = variable.packetId();
//
//        logger.trace("Finish handling PUBACK message for client {}", this.clientId);
//    }
//
//    private void onPubRec(ChannelHandlerContext ctx, MqttMessage msg) {
//        logger.trace("Start handling PUBREC message for client {}", this.clientId);
//        System.out.println("On pubRec ...........");
//
//        if (!this.connected) {
//            logger.debug(
//                    "Protocol violation: Client {} must first sent a CONNECT message, now received PUBREC message, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        logger.debug(
//                "Message received: Received PUBREC message from client {} user {}",
//                this.clientId,
//                this.userName);
//
//        MqttPacketIdVariableHeader variable = (MqttPacketIdVariableHeader) msg.variableHeader();
//        int packetId = variable.packetId();
//
//        // In the QoS 2 delivery protocol, the Sender
//        // MUST treat the PUBLISH packet as “unacknowledged” until it has received the corresponding
//        // PUBREC packet from the receiver.
//        // MUST send a PUBREL packet when it receives a PUBREC packet from the receiver. This
//        // PUBREL packet MUST contain the same Packet Identifier as the original PUBLISH packet.
//        // MUST NOT re-send the PUBLISH once it has sent the corresponding PUBREL packet.
//        logger.trace("Remove in-flight PUBLISH message {} for client {}", packetId, this.clientId);
//
//        // Send back PUBREL
//        MqttMessage pubrel =
//                MqttMessageFactory.newMessage(
//                        new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0),
//                        MqttPacketIdVariableHeader.from(packetId),
//                        null);
//        logger.trace("Send PUBREL back to client {}", this.clientId);
//        this.registry.sendMessage(ctx, pubrel, this.clientId, packetId, true);
//
//        logger.trace("Finish handling PUBREC message for client {}", this.clientId);
//    }
//
//    private void onPubRel(ChannelHandlerContext ctx, MqttMessage msg) {
//        logger.trace("Start handling PUBREL message for client {}", this.clientId);
//        System.out.println("On pubRel ...........");
//
//        if (!this.connected) {
//            logger.debug(
//                    "Protocol violation: Client {} must first sent a CONNECT message, now received PUBREL message, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        logger.debug(
//                "Message received: Received PUBREL message from client {} user {}",
//                this.clientId,
//                this.userName);
//
//        MqttPacketIdVariableHeader variable = (MqttPacketIdVariableHeader) msg.variableHeader();
//        int packetId = variable.packetId();
//
//        logger.trace("Finish handling PUBREL message for client {}", this.clientId);
//    }
//
//    private void onPubComp(ChannelHandlerContext ctx, MqttMessage msg) {
//        logger.trace("Start handling PUBCOMP message for client {}", this.clientId);
//        System.out.println("On pub Comp ...........");
//
//        if (!this.connected) {
//            logger.debug(
//                    "Protocol violation: Client {} must first sent a CONNECT message, now received PUBCOMP message, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        logger.debug(
//                "Message received: Received PUBCOMP message from client {} user {}",
//                this.clientId,
//                this.userName);
//
//        MqttPacketIdVariableHeader variable = (MqttPacketIdVariableHeader) msg.variableHeader();
//        int packetId = variable.packetId();
//
//        logger.trace("Finish handling PUBCOMP message for client {}", this.clientId);
//    }
//
//    private void onSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage msg) {
//        logger.trace("Start handling SUBSCRIBE message for client {}", this.clientId);
//        System.out.println("On Subscribe ...........");
//
//        if (!this.connected) {
//            logger.debug(
//                    "Protocol violation: Client {} must first sent a CONNECT message, now received SUBSCRIBE message, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        int packetId = msg.variableHeader().packetId();
//        List<MqttTopicSubscription> requestSubscriptions = msg.payload().subscriptions();
//
//        // Authorize client subscribe using provided Authenticator
//        List<MqttGrantedQoS> grantedQosLevels =
//                this.authenticator.authSubscribe(this.clientId, this.userName, requestSubscriptions);
//        if (requestSubscriptions.size() != grantedQosLevels.size()) {
//            logger.warn(
//                    "Authorization error: SUBSCRIBE message's subscriptions count not equal to granted QoS count, disconnect the client");
//            ctx.close();
//            return;
//        }
//        logger.trace(
//                "Authorization granted on topic {} as {} for client {}",
//                ArrayUtils.toString(msg.payload().subscriptions()),
//                ArrayUtils.toString(grantedQosLevels),
//                this.clientId);
//
//        logger.trace("Send SUBACK back to client {}", this.clientId);
//        this.registry.sendMessage(
//                ctx,
//                MqttMessageFactory.newMessage(
//                        new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                        MqttPacketIdVariableHeader.from(packetId),
//                        new MqttSubAckPayload(grantedQosLevels)),
//                this.clientId,
//                packetId,
//                true);
//        logger.trace("Finish handling SUBSCRIBE message for client {}", this.clientId);
//
//        List<MqttTopicSubscription> subs = msg.payload().subscriptions();
//
//        subs.forEach(
//                s -> {
//                    String topic = s.topic();
//                    Matcher matched = venueRegex.matcher(topic);
//
//                    if (matched.find()) {
//                        logger.debug(
//                                "Subscribing to venue '{}' version '{}'", matched.group(2), matched.group(1));
//                        String venue = matched.group(2);
//                        String topicVersion = matched.group(1);
//                        Date targetTime = new Date();
//
//                        int scheduleSecs = properties.getMqtt().getScheduleFreq();
//                        int clientsFreq = properties.getMqtt().getClientsFreq();
//                        boolean getClientsInfos = properties.getMqtt().getInfo();
//                        boolean getProbing = properties.getMqtt().getProbing();
//                        boolean getAssoc = properties.getMqtt().getAssoc();
//                        logger.debug(
//                                "Config: getinfo: {} getAssoc: {} getProbing: {} freq: {}",
//                                getClientsInfos,
//                                getAssoc,
//                                getProbing,
//                                scheduleSecs);
//                        logger.info("Task scheduler is commented##############");
//                        // subscribe for client info
//            if (getClientsInfos) {
//              targetTime = DateUtils.addSeconds(targetTime, 1);
//              scheduler.scheduleAtFixedRate(
//                  new ClientInfoTask(this.clientId, venue, topicVersion),
//                  targetTime,
//                  clientsFreq * 1000);
//            }
//
//            targetTime = DateUtils.addSeconds(targetTime, 1);
//
//            // subscribe for probing clients
//            if (getProbing) {
//              scheduler.scheduleAtFixedRate(
//                  new RSSISubscribeTask(this.clientId, venue, topicVersion, 3, 1),
//                  targetTime,
//                  scheduleSecs * 2000);
//              targetTime = DateUtils.addSeconds(targetTime, scheduleSecs);
//            }
//            // subscribe for assoc clients
//            if (getAssoc) {
//              scheduler.scheduleAtFixedRate(
//                  new RSSISubscribeTask(this.clientId, venue, topicVersion, 3, 0),
//                  targetTime,
//                  scheduleSecs * 2000);
//            }
//                    }
//                });
//    }
//
//    private void onUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) {
//        logger.trace("Start handling UNSUBSCRIBE message for client {}", this.clientId);
//        System.out.println("On Unsubscribe ...........");
//
//        if (!this.connected) {
//            logger.debug(
//                    "Protocol violation: Client {} must first sent a CONNECT message, now received UNSUBSCRIBE message, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        logger.debug(
//                "Message received: Received UNSUBSCRIBE message from client {} user {} topics {}",
//                this.clientId,
//                this.userName,
//                ArrayUtils.toString(msg.payload().topics()));
//
//        int packetId = msg.variableHeader().packetId();
//
//        logger.debug("Send UNSUBACK back to client {}", this.clientId);
//        this.registry.sendMessage(
//                ctx,
//                MqttMessageFactory.newMessage(
//                        new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                        MqttPacketIdVariableHeader.from(packetId),
//                        null),
//                this.clientId,
//                packetId,
//                true);
//
//        logger.trace("Finish handling UNSUBSCRIBE message for client {}", this.clientId);
//    }
//
//    private void onPingReq(ChannelHandlerContext ctx) {
//        logger.trace("Start handling PINGREQ message for client {}", this.clientId);
//        System.out.println("On ping Request ...........");
//
//        if (!this.connected) {
//            logger.debug(
//                    "Protocol violation: Client {} must first sent a CONNECT message, now received PINGREQ message, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        logger.debug(
//                "Message received: Received PINGREQ message from client {} user {}",
//                this.clientId,
//                this.userName);
//
//        logger.debug("Response: Send PINGRESP back to client {}", this.clientId);
//        this.registry.sendMessage(
//                ctx,
//                MqttMessageFactory.newMessage(
//                        new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                        null,
//                        null),
//                this.clientId,
//                null,
//                true);
//
//        // Refresh client's connected broker node
//        logger.trace("Refresh client {} connected to broker {}", this.clientId, this.brokerId);
//
//        logger.trace("Finish handling PINGREQ message for client {}", this.clientId);
//    }
//
//    private void onDisconnect(ChannelHandlerContext ctx) {
//        logger.trace("Start handling DISCONNECT message for client {}", this.clientId);
//        System.out.println("Start handling DISCONNECT message for client "+ this.clientId);
//
//        if (!this.connected) {
//            logger.debug(
//                    "Protocol violation: Client {} must first sent a CONNECT message, now received DISCONNECT message, disconnect the client",
//                    this.clientId);
//            ctx.close();
//            return;
//        }
//
//        logger.debug(
//                "Message received: Received DISCONNECT message from client {} user {}",
//                this.clientId,
//                this.userName);
//
//        boolean redirect = handleConnectLost(ctx);
//
//        // // Pass message to 3rd party application
//        // if (redirect)
//        // logger.trace("Send a copy of DISCONNECT message from client {} to 3rd party
//        // application", this.clientId);
//        // this.cluster.sendToApplication(new Message<>(
//        // new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE,
//        // false, 0),
//        // new MqttAdditionalHeader(this.version, this.clientId, this.userName,
//        // this.brokerId), null, null));
//
//        // If the Will Flag is set to 1 this indicates that, if the Connect request is accepted, a Will
//        // Message MUST be
//        // stored on the Server and associated with the Network Connection. The Will Message MUST be
//        // published
//        // when the Network Connection is subsequently closed unless the Will Message has been deleted
//        // by the
//        // Server on receipt of a DISCONNECT Packet.
//        this.willMessage = null;
//
//        // On receipt of DISCONNECT the Server:
//        // MUST discard any Will Message associated with the current connection without publishing it.
//        // SHOULD close the Network Connection if the Client has not already done so.
//        this.connected = false;
//
//        // Make sure connection is closed
//        ctx.close();
//
//        logger.trace("Finish handling Disconnect for client {}", this.clientId);
//    }
//
//    private boolean handleConnectLost(ChannelHandlerContext ctx) {
//
//        // Remove connected node
//        logger.trace("Mark client {} disconnected from broker {}", this.clientId, this.brokerId);
//        registry.removeSession(this.clientId);
//
//        return false;
//    }
//
//    @Override
//    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
//        if (evt instanceof IdleStateEvent) {
//            IdleStateEvent e = (IdleStateEvent) evt;
//            if (e.state() == IdleState.ALL_IDLE) {
//                logger.debug(
//                        "Protocol violation: Client {} has been idle beyond keep alive time, disconnect the client",
//                        this.clientId);
//                ctx.close();
//            }
//        }
//    }
//
//    @Override
//    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
//        System.out.println("Exception is caught in Mqtt protocol handler");
//        if (this.connected) {
//            if (cause instanceof IOException) {
//                System.out.println("Exception caught: Exception caught from client");
//                logger.debug(
//                        "Exception caught: Exception caught from client {} user {}: ",
//                        this.clientId,
//                        this.userName,
//                        ExceptionUtils.getMessage(cause));
//            } else {
//                logger.debug(
//                        "Exception caught: Exception caught from client {} user {}: ",
//                        this.clientId,
//                        this.userName,
//                        cause);
//            }
//        }
//        ctx.close();
//    }
//}

package com.example.TestMqtt.handler;

import com.example.TestMqtt.config.ConfigProperties;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnAckVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnectMessage;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttConnectReturnCode;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttFixedHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttGrantedQoS;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttIdentifierRejectedException;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessage;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessageFactory;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttMessageType;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPacketIdVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPublishMessage;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttPublishVariableHeader;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttQoS;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttSubAckPayload;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttSubscribeMessage;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttTopicSubscription;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttUnacceptableProtocolVersionException;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttUnsubscribeMessage;
import com.example.TestMqtt.io.netty.handler.codec.mqtt.MqttVersion;
import com.example.TestMqtt.mqtt.broker.session.ClientSession;
import com.example.TestMqtt.mqtt.broker.subscribe.SubscribeService;
import com.example.TestMqtt.mqtt.broker.subscribe.Subscription;
import com.example.TestMqtt.mqtt.util.UUIDs;
import com.example.TestMqtt.mqtt.api.auth.Authenticator;
import com.example.TestMqtt.mqtt.api.auth.AuthorizeResult;
import com.example.TestMqtt.mqtt.broker.session.SessionRegistry;
import com.example.TestMqtt.mqtt.broker.util.Validator;
import com.example.TestMqtt.mqtt.paq.ClientInfoByVenueReq;
import com.example.TestMqtt.mqtt.paq.RssiSubscribe;
import com.example.TestMqtt.mqtt.util.Topics;
import com.example.TestMqtt.payload.DeviceMessageInMqtt;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import io.netty.util.CharsetUtil;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;

public class MqttProtocolHandler extends MessageToMessageDecoder<MqttMessage> {

    //  private static final boolean GET_CLIENT_INFO = true;
    //  private static final boolean GET_PROBING = true;
    //  private static final boolean GET_ASSOC = true;
    //  private static final int SCHEDULE_FREQ_SECS = 60;

    private static final Logger logger = LoggerFactory.getLogger(MqttProtocolHandler.class);

    private static Pattern venueRegex = Pattern.compile("(3.0|2.1)/LOC/(.*)/L");

    private final Authenticator authenticator;
    private final SessionRegistry registry;
    private final Validator validator;

    private final  SubscribeService subscribeService;

    AtomicInteger id = new AtomicInteger(100);

    // session state
    private MqttVersion version;
    private String clientId;
    private String userName;
    private String brokerId;
    private boolean connected;
    private boolean cleanSession;
    private int keepAlive;
    private int keepAliveMax;
    private MqttPublishMessage willMessage;
    private TaskScheduler scheduler;
    private ConfigProperties properties;

    private int getId() {
        return id.updateAndGet(x -> (x + 1) % 32768);
    }

    private class RSSISubscribeTask implements Runnable {

        private String clientId, venue, topicVersion;
        private int anon;
        private int subtype;

        public RSSISubscribeTask(
                String clientId, String venue, String topicVersion, int subtype, int anon) {
            this.clientId = clientId;
            this.venue = venue;
            this.anon = anon;
            this.subtype = subtype;
            this.topicVersion = topicVersion;
        }

        @Override
        public void run() {
            RssiSubscribe ps = new RssiSubscribe(subtype, anon);

            int mid = getId();

            MqttFixedHeader fh =
                    new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttPublishVariableHeader vh =
                    MqttPublishVariableHeader.from(topicVersion + "/LOC/" + venue + "/LS/PAQ", mid);

            ByteBuf payload = ps.toByteBuf();

            MqttPublishMessage publish = new MqttPublishMessage(fh, vh, payload);
            if (logger.isTraceEnabled()) {
                logger.trace(
                        "Sent to {}:\n{} ", vh.toString(), ByteBufUtil.prettyHexDump(publish.content()));
            }
            registry.sendMessage(publish, clientId, mid, true);
            logger.trace("Sent subscription  {} to {}", publish, clientId);
        }
    }

    private class ClientInfoTask implements Runnable {

        private String clientId, venue, topicVersion;

        public ClientInfoTask(String clientId, String venue, String topicVersion) {
            this.clientId = clientId;
            this.venue = venue;
            this.topicVersion = topicVersion;
        }

        @Override
        public void run() {

            ClientInfoByVenueReq ps = new ClientInfoByVenueReq(venue);

            int mid = getId();
            MqttFixedHeader fh =
                    new MqttFixedHeader(MqttMessageType.PUBLISH, false, MqttQoS.AT_MOST_ONCE, false, 0);
            MqttPublishVariableHeader vh =
                    MqttPublishVariableHeader.from(topicVersion + "/LOC/" + venue + "/LS/PAQ", mid);

            ByteBuf payload = ps.toByteBuf();

            MqttPublishMessage publish = new MqttPublishMessage(fh, vh, payload);
            if (logger.isTraceEnabled()) {
                logger.trace(
                        "Sent to {}:\n{} ", vh.toString(), ByteBufUtil.prettyHexDump(publish.content()));
            }
            registry.sendMessage(publish, clientId, mid, true);
            logger.trace("Sent client info request to {}", clientId);
        }
    }

    public MqttProtocolHandler(
            ConfigProperties properties,
            Authenticator authenticator,
            SessionRegistry registry,
            Validator validator,
            SubscribeService subscribeService, String brokerId,
            int keepAlive,
            int keepAliveMax,
            TaskScheduler scheduler) {
        this.authenticator = authenticator;
        this.registry = registry;
        this.validator = validator;
        this.subscribeService = subscribeService;

        this.brokerId = brokerId;
        this.keepAlive = keepAlive;
        this.keepAliveMax = keepAliveMax;
        this.scheduler = scheduler;
        this.properties = properties;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, MqttMessage msg, List<Object> out)
            throws Exception {

        logger.info("Message is : " + msg);
        // Disconnect if The MQTT message is invalid
        if (msg.decoderResult().isFailure()) {
            Throwable cause = msg.decoderResult().cause();
            logger.error(
                    "Protocol violation: Invalid message {}",
                    ExceptionUtils.getMessage(msg.decoderResult().cause()));
            if (cause instanceof MqttUnacceptableProtocolVersionException) {
                // Send back CONNACK if the protocol version is invalid
                this.registry.sendMessage(
                        ctx,
                        MqttMessageFactory.newMessage(
                                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                                new MqttConnAckVariableHeader(
                                        MqttConnectReturnCode.CONNECTION_REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, false),
                                null),
                        "INVALID",
                        null,
                        true);
            } else if (cause instanceof MqttIdentifierRejectedException) {
                // Send back CONNACK if the client id is invalid
                this.registry.sendMessage(
                        ctx,
                        MqttMessageFactory.newMessage(
                                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                                new MqttConnAckVariableHeader(
                                        MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false),
                                null),
                        "INVALID",
                        null,
                        true);
            }
            ctx.close();
            return;
        }

        switch (msg.fixedHeader().messageType()) {
            case CONNECT:
                onConnect(ctx, (MqttConnectMessage) msg);
                break;
            case PUBLISH:
                onPublish(ctx, (MqttPublishMessage) msg, out);
                break;
            case PUBACK:
                onPubAck(ctx, msg);
                break;
            case PUBREC:
                onPubRec(ctx, msg);
                break;
            case PUBREL:
                onPubRel(ctx, msg);
                break;
            case PUBCOMP:
                onPubComp(ctx, msg);
                break;
            case SUBSCRIBE:
                onSubscribe(ctx, (MqttSubscribeMessage) msg);
                break;
            case UNSUBSCRIBE:
                onUnsubscribe(ctx, (MqttUnsubscribeMessage) msg);
                break;
            case PINGREQ:
                onPingReq(ctx);
                break;
            case DISCONNECT:
                onDisconnect(ctx);
                break;
        }
    }

    private void onConnect(ChannelHandlerContext ctx, MqttConnectMessage msg) {
        logger.trace("Start handling CONNECT message");
        this.version =
                MqttVersion.fromProtocolNameAndLevel(
                        msg.variableHeader().protocolName(), (byte) msg.variableHeader().protocolLevel());
        this.clientId = msg.payload().clientId();
        this.cleanSession = msg.variableHeader().cleanSession();
        if (msg.variableHeader().keepAlive() > 0
                && msg.variableHeader().keepAlive() <= this.keepAliveMax) {
            this.keepAlive = msg.variableHeader().keepAlive();
        }

        if (StringUtils.isBlank(this.clientId)) {
            if (!this.cleanSession) {
                logger.debug(
                        "Protocol violation: Empty client id with clean session 0, send CONNACK and disconnect the client");
                this.registry.sendMessage(
                        ctx,
                        MqttMessageFactory.newMessage(
                                new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                                new MqttConnAckVariableHeader(
                                        MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false),
                                null),
                        "INVALID",
                        null,
                        true);
                ctx.close();
                return;
            } else {
                this.clientId = UUIDs.shortUuid();
            }
        }

        // Validate clientId based on configuration
        else if (!this.validator.isClientIdValid(this.clientId)) {
            logger.debug(
                    "Protocol violation: Client id {} not valid based on configuration, send CONNACK and disconnect the client",
                    this.clientId);
            this.registry.sendMessage(
                    ctx,
                    MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            new MqttConnAckVariableHeader(
                                    MqttConnectReturnCode.CONNECTION_REFUSED_IDENTIFIER_REJECTED, false),
                            null),
                    this.clientId,
                    null,
                    true);
            ctx.close();
            return;
        }

        // A Client can only send the CONNECT Packet once over a Network Connection. The Server MUST
        // process a second CONNECT Packet sent from a Client as a protocol violation and disconnect the
        // Client
        if (this.connected) {
            logger.debug(
                    "Protocol violation: Second CONNECT packet sent from client {}, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        boolean userNameFlag = msg.variableHeader().userNameFlag();
        boolean passwordFlag = msg.variableHeader().passwordFlag();
        this.userName = msg.payload().userName();
        String password = msg.payload().password();
        boolean malformed = false;
        // If the User Name Flag is set to 0, a user name MUST NOT be present in the payload
        // If the User Name Flag is set to 1, a user name MUST be present in the payload
        // If the Password Flag is set to 0, a password MUST NOT be present in the payload
        // If the Password Flag is set to 1, a password MUST be present in the payload
        // If the User Name Flag is set to 0, the Password Flag MUST be set to 0
        // Validate User Name based on configuration
        // Validate Password based on configuration
        if (userNameFlag) {
            if (StringUtils.isBlank(this.userName) || !this.validator.isUserNameValid(this.userName))
                malformed = true;
        } else {
            if (StringUtils.isNotBlank(this.userName) || passwordFlag) malformed = true;
        }
        if (passwordFlag) {
            if (StringUtils.isBlank(password) || !this.validator.isPasswordValid(password))
                malformed = true;
        } else {
            if (StringUtils.isNotBlank(password)) malformed = true;
        }
        if (malformed) {
            logger.debug(
                    "Protocol violation: Bad user name or password from client {}, send CONNACK and disconnect the client",
                    this.clientId);
            this.registry.sendMessage(
                    ctx,
                    MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            new MqttConnAckVariableHeader(
                                    MqttConnectReturnCode.CONNECTION_REFUSED_BAD_USER_NAME_OR_PASSWORD, false),
                            null),
                    this.clientId,
                    null,
                    true);
            ctx.close();
            return;
        }

        logger.debug(
                "Message received: Received CONNECT message from client {} user {}",
                this.clientId,
                this.userName);

        AuthorizeResult result = this.authenticator.authConnect(this.clientId, this.userName, password);
        // Authorize successful
        if (result == AuthorizeResult.OK) {
            logger.trace(
                    "Authorization CONNECT succeeded for client {} user {}", this.clientId, this.userName);

            boolean sessionPresent = !this.cleanSession;

            // The first packet sent from the Server to the Client MUST be a CONNACK Packet


            // If CleanSession is set to 0, the Server MUST resume communications with the Client based on
            // state from
            // the current Session (as identified by the Client identifier). If there is no Session
            // associated with the Client
            // identifier the Server MUST create a new Session. The Client and Server MUST store the
            // Session after
            // the Client and Server are disconnected. After the disconnection of a Session that had
            // CleanSession set to 0, the Server MUST store further QoS 1 and QoS 2 messages that match
            // any
            // subscriptions that the client had at the time of disconnection as part of the Session
            // state.
            // It MAY also store QoS 0 messages that meet the same criteria.
            // The Session state in the Server consists of:
            // The existence of a Session, even if the rest of the Session state is empty.
            // The Client's subscriptions.
            // QoS 1 and QoS 2 messages which have been sent to the Client, but have not been completely
            // acknowledged.
            // QoS 1 and QoS 2 messages pending transmission to the Client.
            // QoS 2 messages which have been received from the Client, but have not been completely
            // acknowledged.
            // Optionally, QoS 0 messages pending transmission to the Client.
            if (!this.cleanSession) {
                logger.trace(
                        "Clear session state for client {} because former connection is clean session",
                        this.clientId);
            }

            // If CleanSession is set to 1, the Client and Server MUST discard any previous Session and
            // start a new
            // one. This Session lasts as long as the Network Connection. State data associated with this
            // Session
            // MUST NOT be reused in any subsequent Session.
            // When CleanSession is set to 1 the Client and Server need not process the deletion of state
            // atomically.

            // If the ClientId represents a Client already connected to the Server then the Server MUST
            // disconnect the existing Client
            ChannelHandlerContext lastSession = this.registry.removeSession(this.clientId);
            if (lastSession != null) {
                logger.trace("Try to disconnect existed client {}", this.clientId);
                lastSession.close();
            }

            // If the Will Flag is set to 1 this indicates that, if the Connect request is accepted, a
            // Will Message MUST be
            // stored on the Server and associated with the Network Connection. The Will Message MUST be
            // published
            // when the Network Connection is subsequently closed unless the Will Message has been deleted
            // by the
            // Server on receipt of a DISCONNECT Packet.
            String willTopic = msg.payload().willTopic();
            String willMessage = msg.payload().willMessage();
            if (msg.variableHeader().willFlag()
                    && StringUtils.isNotEmpty(willTopic)
                    && this.validator.isTopicNameValid(willTopic)
                    && StringUtils.isNotEmpty(willMessage)) {
                logger.trace("Keep WILL message on topic {} for client {}", willTopic, this.clientId);

                this.willMessage =
                        (MqttPublishMessage)
                                MqttMessageFactory.newMessage(
                                        new MqttFixedHeader(
                                                MqttMessageType.PUBLISH,
                                                false,
                                                msg.variableHeader().willQos(),
                                                msg.variableHeader().willRetain(),
                                                0),
                                        MqttPublishVariableHeader.from(willTopic),
                                        Unpooled.wrappedBuffer(willMessage.getBytes()));
            }

            // If the Keep Alive value is non-zero and the Server does not receive a Control Packet from
            // the Client
            // within one and a half times the Keep Alive time period, it MUST disconnect the Network
            // Connection to the
            // Client as if the network had failed
            logger.trace("Update idleHandler for client {}", this.clientId);
            if (ctx.pipeline().names().contains("idleHandler")) ctx.pipeline().remove("idleHandler");
            ctx.pipeline()
                    .addFirst("idleHandler", new IdleStateHandler(0, 0, Math.round(this.keepAlive * 1.5f)));

            // Save connection state, add to local registry
            logger.trace("Save client {} connection state in registry", this.clientId);
            this.connected = true;

            this.registry.put(this.clientId,new ClientSession(this.clientId, ctx.channel(),false));
            this.registry.saveSession(this.clientId, ctx);

            logger.trace("Send CONNACK back to client {}", this.clientId);
            this.registry.sendMessage(
                    ctx,
                    MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            new MqttConnAckVariableHeader(
                                    MqttConnectReturnCode.CONNECTION_ACCEPTED, sessionPresent),
                            null),
                    this.clientId,
                    null,
                    true);

            // Authorize failed
        } else {
            logger.trace(
                    "Authorization CONNECT failed {} for client {}, send CONNACK and disconnect the client",
                    result,
                    this.clientId);
            this.registry.sendMessage(
                    ctx,
                    MqttMessageFactory.newMessage(
                            new MqttFixedHeader(MqttMessageType.CONNACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                            new MqttConnAckVariableHeader(
                                    MqttConnectReturnCode.CONNECTION_REFUSED_NOT_AUTHORIZED, false),
                            null),
                    this.clientId,
                    null,
                    true);
            ctx.close();
        }

        logger.trace("Finish handling CONNECT message for client {}", this.clientId);
    }

    private void onPublish(ChannelHandlerContext ctx, MqttPublishMessage msg, List<Object> out) {
        logger.trace("Start handling PUBLISH message for client {}", this.clientId);

        if (!this.connected) {
            logger.debug(
                    "Protocol violation: Client {} must first sent a CONNECT message, now received PUBLISH message, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        // boolean dup = msg.fixedHeader().dup();
        MqttQoS qos = msg.fixedHeader().qos();
        boolean retain = msg.fixedHeader().retain();
        String topicName = msg.variableHeader().topicName();
        int packetId = msg.variableHeader().packetId();

        // The Topic Name in the PUBLISH Packet MUST NOT contain wildcard characters
        // Validate Topic Name based on configuration
        if (!this.validator.isTopicNameValid(topicName)) {
            logger.debug(
                    "Protocol violation: Client {} sent PUBLISH message contains invalid topic name {}, disconnect the client",
                    this.clientId,
                    topicName);
            ctx.close();
            return;
        }

        // The Packet Identifier field is only present in PUBLISH Packets where the QoS level is 1 or 2.
        if (packetId <= 0 && (qos == MqttQoS.AT_LEAST_ONCE || qos == MqttQoS.EXACTLY_ONCE)) {
            logger.debug(
                    "Protocol violation: Client {} sent PUBLISH message does not contain packet id, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        List<String> topicLevels = Topics.sanitizeTopicName(topicName);

        logger.debug(
                "Message received: Received PUBLISH message from client {} user {} topic {}",
                this.clientId,
                this.userName,
                topicName);
        if (logger.isTraceEnabled()) {
            logger.trace("Received:\n{} ", ByteBufUtil.prettyHexDump(msg.content()));
        }
        // try (FileOutputStream fos =
        // new FileOutputStream("target/binTest-" + id.incrementAndGet() + ".bytes")) {
        // msg.content().markReaderIndex();
        // msg.content().readBytes(fos, msg.content().readableBytes());
        // msg.content().resetReaderIndex();
        // } catch (FileNotFoundException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }

        if (topicName.startsWith("gliot")) {
            System.out.println(msg.content().toString(CharsetUtil.UTF_8));
            out.add(DeviceMessageInMqtt.of(msg.content(), topicName));
        }else{
            System.out.println(msg.content().toString(CharsetUtil.UTF_8));
        }

        List<Subscription> subscriptionClients = subscribeService.getSubscriptions(topicName);
        if(subscriptionClients != null){
            subscriptionClients.forEach(client -> {
                System.out.println("Publishing message to "+ client.toString());
                this.registry.sendMessage(
                        MqttMessageFactory.newMessage(
                                new MqttFixedHeader(MqttMessageType.PUBLISH, false,MqttQoS.AT_MOST_ONCE, false,0),
                                MqttPublishVariableHeader.from(topicName, 1),
                                msg.payload()
                        ),
                        client.getClientId(),
                        1,
                        true);

//                this.registry.sendMessage(
//                        this.registry.getSession(client.getClientId()),
//                        MqttMessageFactory.newMessage(
//                                new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
//                                MqttPacketIdVariableHeader.from(packetId),
//                                msg.payload()),
//                        client.getClientId(),
//                        packetId,
//                        false);
            });
        }else{
            System.out.println("No Subscription for topic "+ topicName);
        }


        System.out.println("Send PUBACK back to client : "+ this.clientId);

        logger.trace("Send PUBACK back to client {}", this.clientId);
        this.registry.sendMessage(
                ctx,
                MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        MqttPacketIdVariableHeader.from(packetId),
                        null),
                this.clientId,
                packetId,
                true);

        //logger.trace("Finish handling PUBLISH message for client {}", this.clientId);
        System.out.println("Finish handling PUBLISH message for client "+ this.clientId);
    }

    private void onPubAck(ChannelHandlerContext ctx, MqttMessage msg) {
        logger.trace("Start handling PUBACK message for client {}", this.clientId);

        if (!this.connected) {
            logger.debug(
                    "Protocol violation: Client {} must first sent a CONNECT message, now received PUBACK message, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        logger.debug(
                "Message received: Received PUBACK message from client {} user {}",
                this.clientId,
                this.userName);

        MqttPacketIdVariableHeader variable = (MqttPacketIdVariableHeader) msg.variableHeader();
        int packetId = variable.packetId();

        logger.trace("Finish handling PUBACK message for client {}", this.clientId);
    }

    private void onPubRec(ChannelHandlerContext ctx, MqttMessage msg) {
        logger.trace("Start handling PUBREC message for client {}", this.clientId);

        if (!this.connected) {
            logger.debug(
                    "Protocol violation: Client {} must first sent a CONNECT message, now received PUBREC message, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        logger.debug(
                "Message received: Received PUBREC message from client {} user {}",
                this.clientId,
                this.userName);

        MqttPacketIdVariableHeader variable = (MqttPacketIdVariableHeader) msg.variableHeader();
        int packetId = variable.packetId();

        // In the QoS 2 delivery protocol, the Sender
        // MUST treat the PUBLISH packet as “unacknowledged” until it has received the corresponding
        // PUBREC packet from the receiver.
        // MUST send a PUBREL packet when it receives a PUBREC packet from the receiver. This
        // PUBREL packet MUST contain the same Packet Identifier as the original PUBLISH packet.
        // MUST NOT re-send the PUBLISH once it has sent the corresponding PUBREL packet.
        logger.trace("Remove in-flight PUBLISH message {} for client {}", packetId, this.clientId);

        // Send back PUBREL
        MqttMessage pubrel =
                MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PUBREL, false, MqttQoS.AT_LEAST_ONCE, false, 0),
                        MqttPacketIdVariableHeader.from(packetId),
                        null);
        logger.trace("Send PUBREL back to client {}", this.clientId);
        this.registry.sendMessage(ctx, pubrel, this.clientId, packetId, true);

        logger.trace("Finish handling PUBREC message for client {}", this.clientId);
    }

    private void onPubRel(ChannelHandlerContext ctx, MqttMessage msg) {
        logger.trace("Start handling PUBREL message for client {}", this.clientId);

        if (!this.connected) {
            logger.debug(
                    "Protocol violation: Client {} must first sent a CONNECT message, now received PUBREL message, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        logger.debug(
                "Message received: Received PUBREL message from client {} user {}",
                this.clientId,
                this.userName);

        MqttPacketIdVariableHeader variable = (MqttPacketIdVariableHeader) msg.variableHeader();
        int packetId = variable.packetId();

        logger.trace("Finish handling PUBREL message for client {}", this.clientId);
    }

    private void onPubComp(ChannelHandlerContext ctx, MqttMessage msg) {
        logger.trace("Start handling PUBCOMP message for client {}", this.clientId);

        if (!this.connected) {
            logger.debug(
                    "Protocol violation: Client {} must first sent a CONNECT message, now received PUBCOMP message, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        logger.debug(
                "Message received: Received PUBCOMP message from client {} user {}",
                this.clientId,
                this.userName);

        MqttPacketIdVariableHeader variable = (MqttPacketIdVariableHeader) msg.variableHeader();
        int packetId = variable.packetId();

        logger.trace("Finish handling PUBCOMP message for client {}", this.clientId);
    }

    private void onSubscribe(ChannelHandlerContext ctx, MqttSubscribeMessage msg) {
        logger.trace("Start handling SUBSCRIBE message for client {}", this.clientId);

        if (!this.connected) {
            logger.debug(
                    "Protocol violation: Client {} must first sent a CONNECT message, now received SUBSCRIBE message, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        int packetId = msg.variableHeader().packetId();
        List<MqttTopicSubscription> requestSubscriptions = msg.payload().subscriptions();

        // Authorize client subscribe using provided Authenticator
        List<MqttGrantedQoS> grantedQosLevels =
                this.authenticator.authSubscribe(this.clientId, this.userName, requestSubscriptions);
        if (requestSubscriptions.size() != grantedQosLevels.size()) {
            logger.warn(
                    "Authorization error: SUBSCRIBE message's subscriptions count not equal to granted QoS count, disconnect the client");
            ctx.close();
            return;
        }
        logger.trace(
                "Authorization granted on topic {} as {} for client {}",
                ArrayUtils.toString(msg.payload().subscriptions()),
                ArrayUtils.toString(grantedQosLevels),
                this.clientId);

        Boolean isSubscribed;
        for (MqttTopicSubscription topicSubscriptionsub :
                requestSubscriptions) {
            String topic = topicSubscriptionsub.topic();
            isSubscribed = false;
            List<Subscription> subscriptions = subscribeService.getSubscriptions(topic);
            if(subscriptions != null){
                for (Subscription sub :
                        subscriptions ) {
                    if(sub.getClientId().equals(this.clientId)){
                        isSubscribed = true;
                    }
                }
            }
            if(!isSubscribed){
                Subscription subscription = new Subscription(this.clientId, topic);
                subscribeService.put(topic, subscription);
                System.out.println("Subscription "+topic +" subscription "+ subscription.toString());
            }
        }

//        requestSubscriptions.forEach(
//                s -> {
//                    String topic = s.topic();
//                    Matcher matched = venueRegex.matcher(topic);
//
//                    Subscription subscription = new Subscription(this.clientId, topic);
//                    subscribeService.put(topic, subscription);
//
//                    System.out.println("Subscription "+topic +" subscription "+ subscription.toString());

//                    if (matched.find()) {
//                        logger.debug(
//                                "Subscribing to venue '{}' version '{}'", matched.group(2), matched.group(1));
//                        String venue = matched.group(2);
//                        String topicVersion = matched.group(1);
//                        Date targetTime = new Date();
//
//                        int scheduleSecs = properties.getMqtt().getScheduleFreq();
//                        int clientsFreq = properties.getMqtt().getClientsFreq();
//                        boolean getClientsInfos = properties.getMqtt().getInfo();
//                        boolean getProbing = properties.getMqtt().getProbing();
//                        boolean getAssoc = properties.getMqtt().getAssoc();
//                        logger.debug(
//                                "Config: getinfo: {} getAssoc: {} getProbing: {} freq: {}",
//                                getClientsInfos,
//                                getAssoc,
//                                getProbing,
//                                scheduleSecs);
//                        // subscribe for client info
//                        if (getClientsInfos) {
//                            targetTime = DateUtils.addSeconds(targetTime, 1);
//                            scheduler.scheduleAtFixedRate(
//                                    new ClientInfoTask(this.clientId, venue, topicVersion),
//                                    targetTime,
//                                    clientsFreq * 1000);
//                        }
//
//                        targetTime = DateUtils.addSeconds(targetTime, 1);
//
//                        // subscribe for probing clients
//                        if (getProbing) {
//                            scheduler.scheduleAtFixedRate(
//                                    new RSSISubscribeTask(this.clientId, venue, topicVersion, 3, 1),
//                                    targetTime,
//                                    scheduleSecs * 2000);
//                            targetTime = DateUtils.addSeconds(targetTime, scheduleSecs);
//                        }
//                        // subscribe for assoc clients
//                        if (getAssoc) {
//                            scheduler.scheduleAtFixedRate(
//                                    new RSSISubscribeTask(this.clientId, venue, topicVersion, 3, 0),
//                                    targetTime,
//                                    scheduleSecs * 2000);
//                        }
//                    }
//                });

//        logger.trace("Send SUBACK back to client {}", this.clientId);
        System.out.println("Sending SUBBAK to client " + this.clientId);
        this.registry.sendMessage(
                ctx,
                MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.SUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        MqttPacketIdVariableHeader.from(1),
                        new MqttSubAckPayload(grantedQosLevels)),
                this.clientId,
                packetId,
                true);
        logger.trace("Finish handling SUBSCRIBE message for client {}", this.clientId);
    }

    private void onUnsubscribe(ChannelHandlerContext ctx, MqttUnsubscribeMessage msg) {
        logger.trace("Start handling UNSUBSCRIBE message for client {}", this.clientId);

        if (!this.connected) {
            logger.debug(
                    "Protocol violation: Client {} must first sent a CONNECT message, now received UNSUBSCRIBE message, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        logger.debug(
                "Message received: Received UNSUBSCRIBE message from client {} user {} topics {}",
                this.clientId,
                this.userName,
                ArrayUtils.toString(msg.payload().topics()));

        int packetId = msg.variableHeader().packetId();

        logger.debug("Send UNSUBACK back to client {}", this.clientId);
        this.registry.sendMessage(
                ctx,
                MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.UNSUBACK, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        MqttPacketIdVariableHeader.from(packetId),
                        null),
                this.clientId,
                packetId,
                true);

        logger.trace("Finish handling UNSUBSCRIBE message for client {}", this.clientId);
    }

    private void onPingReq(ChannelHandlerContext ctx) {
        logger.trace("Start handling PINGREQ message for client {}", this.clientId);

        if (!this.connected) {
            logger.debug(
                    "Protocol violation: Client {} must first sent a CONNECT message, now received PINGREQ message, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        logger.debug(
                "Message received: Received PINGREQ message from client {} user {}",
                this.clientId,
                this.userName);

        logger.debug("Response: Send PINGRESP back to client {}", this.clientId);
        this.registry.sendMessage(
                ctx,
                MqttMessageFactory.newMessage(
                        new MqttFixedHeader(MqttMessageType.PINGRESP, false, MqttQoS.AT_MOST_ONCE, false, 0),
                        null,
                        null),
                this.clientId,
                null,
                true);

        // Refresh client's connected broker node
        logger.trace("Refresh client {} connected to broker {}", this.clientId, this.brokerId);

        logger.trace("Finish handling PINGREQ message for client {}", this.clientId);
    }

    private void onDisconnect(ChannelHandlerContext ctx) {
        logger.trace("Start handling DISCONNECT message for client {}", this.clientId);
        System.out.println("Start handling DISCONNECT message for client : " + this.clientId);

        if (!this.connected) {
            logger.debug(
                    "Protocol violation: Client {} must first sent a CONNECT message, now received DISCONNECT message, disconnect the client",
                    this.clientId);
            ctx.close();
            return;
        }

        logger.debug(
                "Message received: Received DISCONNECT message from client {} user {}",
                this.clientId,
                this.userName);

        boolean redirect = handleConnectLost(ctx);

        // // Pass message to 3rd party application
        // if (redirect)
        // logger.trace("Send a copy of DISCONNECT message from client {} to 3rd party
        // application", this.clientId);
        // this.cluster.sendToApplication(new Message<>(
        // new MqttFixedHeader(MqttMessageType.DISCONNECT, false, MqttQoS.AT_MOST_ONCE,
        // false, 0),
        // new MqttAdditionalHeader(this.version, this.clientId, this.userName,
        // this.brokerId), null, null));

        // If the Will Flag is set to 1 this indicates that, if the Connect request is accepted, a Will
        // Message MUST be
        // stored on the Server and associated with the Network Connection. The Will Message MUST be
        // published
        // when the Network Connection is subsequently closed unless the Will Message has been deleted
        // by the
        // Server on receipt of a DISCONNECT Packet.
        this.willMessage = null;

        // On receipt of DISCONNECT the Server:
        // MUST discard any Will Message associated with the current connection without publishing it.
        // SHOULD close the Network Connection if the Client has not already done so.
        this.connected = false;

        // Make sure connection is closed
        ctx.close();

        logger.trace("Finish handling Disconnect for client {}", this.clientId);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.trace("Start handling inactive event for client {}", this.clientId);
        handleConnectLost(ctx);

        // if (this.connected) {
        //
        // logger.debug(
        // "Connection closed: Connection lost from client {} user {}",
        // this.clientId,
        // this.userName);
        //
        // boolean redirect = handleConnectLost(ctx);
        //
        // // // Pass message to 3rd party application
        // // if (redirect)
        // // logger.trace("Send a copy of DISCONNECT message from client {} to 3rd
        // party
        // // application", this.clientId);
        // // this.cluster.sendToApplication(new Message<>(
        // // new MqttFixedHeader(MqttMessageType.DISCONNECT, false,
        // // MqttQoS.AT_MOST_ONCE, false, 0),
        // // new MqttAdditionalHeader(this.version, this.clientId,
        // this.userName,
        // // this.brokerId), null, null));
        //
        // // If the Will Flag is set to 1 this indicates that, if the Connect request is accepted,
        // a
        // // Will Message MUST be
        // // stored on the Server and associated with the Network Connection. The Will Message
        // MUST be
        // // published
        // // when the Network Connection is subsequently closed unless the Will Message has been
        // deleted
        // // by the
        // // Server on receipt of a DISCONNECT Packet.
        // // Situations in which the Will Message is published include, but are not limited to:
        // // An I/O error or network failure detected by the Server.
        // // The Client fails to communicate within the Keep Alive time.
        // // The Client closes the Network Connection without first sending a DISCONNECT Packet.
        // // The Server closes the Network Connection because of a protocol error.
        // if (this.willMessage != null) {
        //
        // MqttQoS willQos = this.willMessage.fixedHeader().qos();
        // String willTopic = this.willMessage.variableHeader().topicName();
        // boolean willRetain = this.willMessage.fixedHeader().retain();
        //
        // AuthorizeResult result =
        // this.authenticator.authPublish(
        // this.clientId, this.userName, willTopic, willQos.value(), willRetain);
        // // Authorize successful
        // if (result == AuthorizeResult.OK) {
        // logger.trace(
        // "Authorization WILL message succeeded on topic {} for client {}",
        // willTopic,
        // this.clientId);
        //
        // // Onward to recipients
        // onwardRecipients(
        // this.willMessage,
        // Message.fromMqttMessage(
        // this.willMessage, this.version, this.clientId, this.userName,
        // this.brokerId)
        // .payload());
        // }
        // // Authorize failed
        // else {
        // logger.trace(
        // "Authorization WILL message failed on topic {} for client {}",
        // willTopic,
        // this.clientId);
        // }
        // }
        // }

        logger.trace("Finish handling inactive event for client {}", this.clientId);
    }

    /**
     * Handle connection lost condition Both when received DISCONNECT message or not
     *
     * @param ctx Session
     * @return True client is marked as disconnected, False client already re-connected
     */
    private boolean handleConnectLost(ChannelHandlerContext ctx) {

        // Remove connected node
        logger.trace("Mark client {} disconnected from broker {}", this.clientId, this.brokerId);
        registry.removeSession(this.clientId);

        return false;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.ALL_IDLE) {
                logger.debug(
                        "Protocol violation: Client {} has been idle beyond keep alive time, disconnect the client",
                        this.clientId);
                ctx.close();
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (this.connected) {
            if (cause instanceof IOException) {
                logger.debug(
                        "Exception caught: Exception caught from client {} user {}: ",
                        this.clientId,
                        this.userName,
                        ExceptionUtils.getMessage(cause));
            } else {
                logger.debug(
                        "Exception caught: Exception caught from client {} user {}: ",
                        this.clientId,
                        this.userName,
                        cause);
            }
        }
        ctx.close();
    }
}

