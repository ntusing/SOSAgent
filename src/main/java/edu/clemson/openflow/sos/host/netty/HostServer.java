package edu.clemson.openflow.sos.host.netty;

import edu.clemson.openflow.sos.agent.DataPipelineInitiator;
import edu.clemson.openflow.sos.agent.DataPipelineListener;
import edu.clemson.openflow.sos.agent.IncomingRequestListener;
import edu.clemson.openflow.sos.agent.netty.AgentClient;
import edu.clemson.openflow.sos.buf.SeqGen;
import edu.clemson.openflow.sos.manager.ISocketServer;
import edu.clemson.openflow.sos.manager.RequestManager;
import edu.clemson.openflow.sos.rest.ControllerRequestMapper;
import edu.clemson.openflow.sos.rest.IncomingRequestMapper;
import edu.clemson.openflow.sos.utils.EventListenersLists;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;


/**
 * @author Khayam Anjam kanjam@g.clemson.edu
 * this class will start a new thread for every incoming connection from clients
 */
public class HostServer extends ChannelInboundHandlerAdapter implements ISocketServer, IncomingRequestListener, DataPipelineListener {
    private static final Logger log = LoggerFactory.getLogger(HostServer.class);
    private static final int DATA_PORT = 9877;
    private IncomingRequestMapper request;
    private DataPipelineInitiator dataPipelineInitiator;
    //private AgentClient agentClient;
    private Channel myChannel;
    //PacketBuffer packetBuffer;
    private SeqGen seqGen;
    private AgentClient agentClient;
    private List<IncomingRequestMapper> incomingRequests = new ArrayList<>();

    public HostServer() {
        EventListenersLists.incomingRequestListeners.add(this);
    }

    public class HostServerHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
            log.info("New host-side connection from {} at Port {}",
                    socketAddress.getHostName(),
                    socketAddress.getPort());

            //   RequestManager requestManager = RequestManager.INSTANCE;
            //   this.request = requestManager.getRequest(socketAddress.getHostName(),
            //
            //       socketAddress.getPort(), true);
            request = getClientRequest(socketAddress.getHostName(), socketAddress.getPort()); // go through the list and find related request
            if (request == null) {
                log.error("No controller request found for this associated port ...all incoming packets will be dropped ");
                return;
            }
            myChannel = ctx.channel();


            if (request != null) {
                //       dataPipelineInitiator = new DataPipelineInitiator();
                //agentClient = new AgentClient();
                //dataPipelineInitiator.addListener(agentClient);
                //dataPipelineInitiator.hostConnected(request, this); //also pass the call back handler so It can respond back

                //packetBuffer  = new PacketBuffer(request);
                seqGen = new SeqGen();
                agentClient = new AgentClient(request);
                agentClient.setChannel(ctx.channel());

            }
            else log.error("Couldn't find the request {} in request pool. Not notifying agent",
                    request.toString());

        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            log.debug("Received new packet from host of size {}, will be forwarding to seqGen", ((byte[]) msg).length  ) ;
            //log.debug("content is {}", new String((byte[]) msg));

            if (request != null) {
                //dataPipelineInitiator.packetArrived(msg); //notify handlers
                if (seqGen != null) {
                    agentClient.incomingPacket(seqGen.incomingPacket((byte[]) msg)); // put packet on buffer
                }
            }
            else log.error("Couldn't find the request. Not forwarding packet");
            ReferenceCountUtil.release(msg);
        }
    }

    private boolean startSocket(int port) {
        NioEventLoopGroup group = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(group)
                    .channel(NioServerSocketChannel.class)
                    .localAddress(new InetSocketAddress(port))
                    .childHandler(new ChannelInitializer() {
                                      @Override
                                      protected void initChannel(Channel channel) throws Exception {
                                          channel.pipeline().addLast("bytesDecoder",
                                                  new ByteArrayDecoder());
                                          channel.pipeline().addLast("hostHandler", new HostServerHandler());
                                          channel.pipeline().addLast("bytesEncoder", new ByteArrayEncoder());
                                      }
                                  }
                    );
            ChannelFuture f = b.bind().sync();
            //myChannel = f.channel();
            log.info("Started host-side socket server at Port {}", port);
            return true;
            // Need to do socket closing handling. close all the remaining open sockets
            //System.out.println(EchoServer.class.getName() + " started and listen on " + f.channel().localAddress());
            //f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Error starting host-side socket");
            e.printStackTrace();
            return false;
        } finally {
            //group.shutdownGracefully().sync();
        }
    }
    private IncomingRequestMapper getClientRequest(String remoteIP, int remotePort) {
        for (IncomingRequestMapper incomingRequest : incomingRequests) {
            if (incomingRequest.getRequest().getClientIP().equals(remoteIP) &&
                    incomingRequest.getRequest().getClientPort() == remotePort) return incomingRequest;
        }
        return null;
    }

    @Override
    public boolean start() {
        return startSocket(DATA_PORT);
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }


    @Override
    public void hostConnected(ControllerRequestMapper request, Object o) {

    }

    @Override
    public void packetArrived(Object msg) {
        log.debug("Received new packet from agent sending to host");
        if (myChannel == null) log.error("Current context is null, wont be sending packet back to host");
        else myChannel.writeAndFlush(msg);
    }

    @Override
    public void hostDisconnected(String hostIP, int hostPort) {

    }

    @Override
    public void newIncomingRequest(IncomingRequestMapper request) {
        incomingRequests.add(request);

    }
}
