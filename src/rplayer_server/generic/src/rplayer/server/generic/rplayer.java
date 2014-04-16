package rplayer.server.generic;

import rplayer.server.generic.rMediaPlayerImpl;
import rplayer.server.base.rGuiInterface;
import rplayer.server.base.rServerBase;

public class rplayer implements rGuiInterface 
{
    rServerBase m_server;

    public void onCreate() 
    { 
        rMediaPlayerImpl l_player = new rMediaPlayerImpl();
        l_player.setGuiInterface( this );
        m_server = new rServerBase( l_player );
        m_server.SetGuiInterface( this );
        m_server.StartUp();
    }

    // test client
    public static void main(String[] args)
    {
        new rplayer().onCreate();
    }
    
    public void sendTextMessage(String textMessage)
    {
        System.out.println(textMessage);
    }
}
