package rplayer.server.generic;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

import javazoom.jl.player.Player;
import rplayer.server.base.rMediaPlayer;

public class rMediaPlayerImpl extends rMediaPlayer
{
    private Player m_player; 
    
    private float m_volume;
    
    private Thread m_playThread;
    
    boolean m_playing = false;
    
    public rMediaPlayerImpl()
    {
        
    }
    
    @Override
    public void resetPlayer()
    {
        if (m_player != null) m_player.close();
    }

    public void setVolume( float a_volume )
    {
        m_volume = a_volume;
        if (m_player != null) m_player.setVolume(m_volume);
    }
    
    public float getVolume( )
    {
        return m_volume;
    }

    @Override
    public void setNextItemToPlay(String aItem)
    {
        if( aItem == null ) return;
        if( aItem.equals( "" ) ) return;
        try 
        {
            FileInputStream fis     = new FileInputStream(aItem);
            BufferedInputStream bis = new BufferedInputStream(fis);
            m_player = new Player(bis);
            m_player.setVolume( m_volume );
        }
        catch (Exception e)
        {
            System.out.println("rMediaPlayerImpl: problem playing file " + aItem);
            System.out.println(e);
        }
    }
    
    public void onCompletion()
    {
        if( m_mediaPlayerEventHandler == null ) return; // exception
        
        m_mediaPlayerEventHandler.HandleItemCompletion();
    }

    @Override
    public synchronized void startPlayback()
    {
        if (m_player == null) return;
        m_playThread = new Thread()
        {
            public void run()
            {
                m_playing = true;
                try { m_player.play(); }
                catch (Exception e) { System.out.println(e); }
                onCompletion();
                m_playing = false;
            }
        };
        m_playThread.start();
    }

    public synchronized void flushCurrentTrack()
    {
        if( m_playThread == null )
        {
            System.out.println("rMediaPlayerImpl: stopPlayback: m_playThread was null!");
            return;
        }
        if(m_playing)
        {
            m_player.close();
        }
    }

    @Override
    public synchronized void togglePause()
    {
        if( m_playThread == null )
        {
            System.out.println("rMediaPlayerImpl: togglePlayback: m_playThread was null!");
            return;
        }
        if(m_playing)
        {
            m_playThread.suspend();
            m_playing = false;
        }
        else
        {
            m_playThread.resume();
            m_playing = true;
        }
    }
}
