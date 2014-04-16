package rplayer.server.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import rplayer.server.base.rMediaScheduler;
import rplayer.server.base.rMediaPlayer;
import rplayer.server.base.rGuiInterface;

public class rServerBase implements rMediaPlayerEventHandlerInterface
{
    /// list of connected scheduler clients
    private LinkedList< rSchedulerClient > m_schedulerClients;
    
    /// an instance to a media player implementation
    private rMediaPlayer m_mediaplayer;
    
    /// the scheduler instance which will provide new media items to play 
    private rMediaScheduler m_scheduler;
    
    /// instance to a gui interface
    private rGuiInterface m_gui_interface = null;
    
    private Properties m_properties = new Properties();
    
    private boolean m_playing = true;
    
    private String m_currentlyPlayed;
    
    /// list of file names for config files (currently only one in use)
    private LinkedList< String > m_configurations = new LinkedList< String >();
    
    //public rServerBase(){}
    public rServerBase( rMediaPlayer a_player )
    {
        m_schedulerClients = new LinkedList< rSchedulerClient >();
        m_scheduler = new rMediaScheduler();
        m_mediaplayer = a_player;
        m_mediaplayer.setMediaPlayerEventHandlerInterface( this );
    }
    
    public String CurrentlyPlayed()
    {
        return m_currentlyPlayed;
    }
    
    public String CurrentTheme()
    {
        return "default";
    }
    
    public void StartUp()
    {
        loadConfiguration();

        findScheduleLists();

        reloadPlayList();

//        (new Thread(){ public void run(){ outputThreadFunc(); } }).start();
        (new Thread(){ public void run(){ listenerThreadFunc(); } }).start();
        (new Thread(){ public void run(){ UDPBroadcaster(); } }).start();
        (new Thread(){ public void run(){ UDPListener(); } }).start();
        
        m_mediaplayer.setVolume(Float.parseFloat(m_properties.getProperty("Volume", "0.3")));
        m_mediaplayer.setNextItemToPlay( m_properties.getProperty("BaseDirectory", "") + "/" + handleNextItem() );
        m_mediaplayer.startPlayback();
    }
    
    public void exitApplication()
    {
        for( rSchedulerClient item: m_schedulerClients )
        {
           item.Disconnect();
        }
        try{ Thread.sleep(1000); } catch(InterruptedException ie){ }
        System.exit( 0 );
    }
        
    /// rMediaPlayerEventHandlerInterface implementation
    ///
    
    /// makes the media player implementation start playing the next item 
    public void HandleItemCompletion()
    {
        m_mediaplayer.resetPlayer();
        m_mediaplayer.setNextItemToPlay( m_properties.getProperty("BaseDirectory", "") + "/" + handleNextItem() );
        if( m_playing ) m_mediaplayer.startPlayback();
    }

    public void SetGuiInterface( rGuiInterface a_interface )
    {
        m_gui_interface = a_interface;
        m_scheduler.setGuiInterface( a_interface );
    }
    
    public void SetBaseDirectory( String a_baseDirectory )
    {
        m_properties.put( "BaseDirectory", a_baseDirectory );
        saveConfiguration();
    }

    public void HandleClientData( String a_data )
    {
        
        if( a_data.equals("quit")) exitApplication();
        
        
        else if( a_data.startsWith("basedir")) SetBaseDirectory(a_data.substring(8));

        else if( a_data.startsWith("ban")) 
        {
            if( a_data.length() >= 4 )
            {
                m_scheduler.Ban( a_data.substring(4) );
                m_scheduler.WriteScheduleFile();
            }
        }

        else if( a_data.startsWith("skip"))
        {
            m_mediaplayer.flushCurrentTrack();
            /*
            m_mediaplayer.resetPlayer();
            m_mediaplayer.setNextItemToPlay( m_properties.getProperty("BaseDirectory", "") + "/" + handleNextItem() );
            if( m_playing ) m_mediaplayer.startPlayback();
            */
        }
        
        else if( a_data.equals( "volup" ) ) 
        {
            m_mediaplayer.addVolume( 0.1f );
            m_properties.put( "Volume", Float.toString( m_mediaplayer.getVolume()));
            saveConfiguration();
            sendMessageToAll( "volume " + Float.toString( m_mediaplayer.getVolume() ) );

        }
        else if( a_data.equals( "reload" ) )
        {
            reloadPlayList();
        }
        else if( a_data.equals( "voldown" ) )
        {
            m_mediaplayer.addVolume( -0.1f );
            m_properties.put( "Volume", Float.toString(m_mediaplayer.getVolume()));
            saveConfiguration();
            sendMessageToAll( "volume " + Float.toString( m_mediaplayer.getVolume() ) );
        }
        if( a_data.equals( "playpause" ) ) m_mediaplayer.togglePause();
    }
    
    private void sendMessageToAll( String a_message )
    {
        for( rSchedulerClient item: m_schedulerClients )
        {
           item.SendMessage( a_message );
        }
    }
    
    private String handleNextItem()
    {
        String l_item = m_scheduler.getNextItem();
        m_currentlyPlayed = l_item;
        sendMessageToAll( "playing "+  CurrentTheme() + " " + CurrentlyPlayed() );
        return l_item;
    }
    
    private void findScheduleLists()
    {
        File l_dir = new File( "./" );
        String[]  l_files = l_dir.list();
        for( String f: l_files )
        {
            if( f.endsWith( ".xml" ) ) m_configurations.add( f );
        }
        boolean l_foundConfig = false;
        for( String s: m_configurations )
        {
            if( s.equals( m_properties.getProperty("LastPlaylist", "default.xml") ) )
            {
                l_foundConfig = true;
                break;
            }
        }
        
        if( !l_foundConfig && !m_configurations.isEmpty() )
        {
            m_properties.put( "LastPlaylist", m_configurations.get(0) );
            saveConfiguration();
        }
    }
    
    private void loadConfiguration()
    {
        try 
        {
            FileInputStream l_in;
            l_in = new FileInputStream("rplayer.cfg");
            try {
                m_properties.load( l_in );
                l_in.close();
            } 
            catch (IOException e)
            { e.printStackTrace(); }
        } 
        catch (FileNotFoundException e) 
        {}
    }
    
    private void saveConfiguration()
    {
        try 
        {
            FileOutputStream l_out = new FileOutputStream("rplayer.cfg");
            try {
                m_properties.store( l_out, "comments" );
                l_out.close();
            } 
            catch (IOException e) 
            { e.printStackTrace(); }
        } 
        catch (FileNotFoundException e) 
        {
            e.printStackTrace();
        }
    }

    private void UDPListener()
    {
        
        try {
            DatagramSocket socket = new DatagramSocket(Integer.parseInt(m_properties.getProperty("UDPPort", "2010")));
            byte[] buffer = new byte[128];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length );
            while(true)
            {
                  //Receive request from client
                  socket.receive(packet);
                  InetAddress client = packet.getAddress();
                  String s = new String( packet.getData(), 0, packet.getLength() );
//              int client_port = packet.getPort();
                  System.out.println("UDPListener: received '" + s + "' from " + client );
                  HandleClientData( s );
            }
        }
        catch (SocketException e) {e.printStackTrace();} 
        catch (IOException e) {e.printStackTrace();}
    }

    private void UDPBroadcaster()
    {
        /// multiple interfaces: 
        /// http://stackoverflow.com/questions/835960/java-on-linux-listening-to-broadcast-messages-on-a-bound-local-address
        try 
        {
            List< InetAddress > l_broadcastReceiver = new LinkedList< InetAddress >();
        	if( Boolean.parseBoolean(m_properties.getProperty("LocalSocketOnly", "true")))
        	{
                try { l_broadcastReceiver.add( InetAddress.getByName("127.0.0.1") ); } 
                catch (UnknownHostException e) { e.printStackTrace(); }
        	}
        	else
        	{
	            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
	            while (interfaces.hasMoreElements())
	            {
	                NetworkInterface ni = (NetworkInterface) interfaces.nextElement();
	                System.out.println( "UDPBroadcaster:   crawling interface:" + ni.getName());
	                if( ni.isVirtual() ) continue;
	                for( InterfaceAddress address:  ni.getInterfaceAddresses() ) 
	                {
	                    if (address.getAddress().isLoopbackAddress() || address.getAddress() instanceof Inet6Address) continue;
	                    if( address.getBroadcast() == null ) continue;
	                    System.out.println( "UDPBroadcaster:   found broadcast address:" + address.getBroadcast() );
	                    l_broadcastReceiver.add( address.getBroadcast() );
	                }
	            }
        	}
            
            DatagramSocket l_udpSocket = new DatagramSocket();
            l_udpSocket.setBroadcast(true);

            
            int l_n = 0;
            int l_session = new Random().nextInt();
            while( true )
            {
                l_n = (l_n +1) % 10;
                byte[] l_hello =  String.format("rplayer%1d %010d", l_n, l_session ).getBytes();
                // System.out.println( "UDPBroadcaster: send '" + new String(l_hello) + "'"  );

                for( InetAddress a : l_broadcastReceiver )
                {
//                    System.out.println( "UDPBroadcaster: send '" + new String(l_hello) + "' to " + a.toString() );
                    DatagramPacket dp = new DatagramPacket( l_hello, l_hello.length, a, Integer.parseInt(m_properties.getProperty("BroadcastPort", "2011")) );
                    try { l_udpSocket.send(dp); } 
                    catch (IOException e) { e.printStackTrace(); }
                }
                
                try { Thread.sleep( 2000 ); } 
                catch (InterruptedException e) { e.printStackTrace(); }

            }
        } 
        catch (SocketException e) { e.printStackTrace(); } 
    }
    
    private void listenerThreadFunc()
    {
        ServerSocket serverSocket = null;
        boolean l_listening = true;
        try
        {
            serverSocket = new ServerSocket( Integer.parseInt( m_properties.getProperty("TCPPort", "2012") ) );
        }
        catch( IOException e )
        {
            System.err.println("rServerBase: IOException when trying to create socket");
            return;
        }
        
        if( serverSocket == null )
        {
            System.err.println("rServerBase: could not create server socket");
            return;
        }
        
        try
        {
            while( l_listening )
            {
//                s = "address: " + serverSocket.getInetAddress().toString() + " C#:" + ((Integer)m_schedulers.size()).toString();
//                messageHandler.sendMessage(Message.obtain(messageHandler)); 
//                new SchedulerClientThread(serverSocket.accept()).start();
                Socket l_client = serverSocket.accept();
                m_schedulerClients.add( new rSchedulerClient( l_client, this).start() );
                m_gui_interface.sendTextMessage( "listenerThreadFunc: new client: '" + l_client.getInetAddress().toString() + "', total number: " + ((Integer)m_schedulerClients.size()).toString() );
            }
        } 
        catch (IOException e) 
        {
            System.err.println("rServerBase: IOException: Could not listen on port: 4444.");
            return;
        }
    }

    /*
    private void listFolder( String folder )
    {
        try
        {
//            setText( "list folder: " + folder );
               File dir = new File( folder );
               String[] list = dir.list();
               for( String f: list )
               {
                   sendMessageToAll( f );
               }
        }
        catch(Exception e)
        {
               sendMessageToAll( "rServerBase: Exception when trying to send data to clients" );
        }
    }
     */
    private void outputThreadFunc()
    {
        /*
        try {Socket s = new Socket("10.0.2.2", 2000);} 
        catch (UnknownHostException e1) {    e1.printStackTrace();} 
        catch (IOException e1) {e1.printStackTrace();}
        */
        
        while( true ) 
        {
            try{ Thread.sleep(1000); } catch(InterruptedException ie){ }
            sendMessageToAll( "alife" );
        }
    }
       
    public String getLocalIpAddress()
    {
        try 
        {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) 
            {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();)
                {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!( inetAddress.isLoopbackAddress() ||  inetAddress instanceof Inet6Address ))
                    {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } 
        catch (SocketException ex) 
        {
            //Log.e(LOG_TAG, ex.toString());
        }
        return "unknown";
    }
    
    private void reloadPlayList()
    {
        m_scheduler.loadPlaylist( m_properties.getProperty("LastPlaylist", "default.xml"), m_properties.getProperty("BaseDirectory", "") );
    }

}
