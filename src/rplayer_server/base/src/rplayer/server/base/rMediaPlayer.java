package rplayer.server.base;

public abstract class rMediaPlayer
{
    public rGuiInterface m_gui_interface = null;
    
    public rMediaPlayerEventHandlerInterface m_mediaPlayerEventHandler = null;
    
    public void setMediaPlayerEventHandlerInterface( rMediaPlayerEventHandlerInterface mediaPlayerEventHandler )
    {
        m_mediaPlayerEventHandler = mediaPlayerEventHandler;
    }

    public void setGuiInterface( rGuiInterface guiInterface )
    {
        m_gui_interface = guiInterface;
    }
    
    abstract public void setVolume( float volume );

    abstract public float getVolume();

    public void addVolume( float step )
    {
        setVolume( Math.min(Math.max(getVolume() + step, 0.0f), 1.0f) );
    }
    
    abstract public void resetPlayer();
    
    abstract public void setNextItemToPlay( String item );
    
    abstract public void startPlayback();

    abstract public void flushCurrentTrack();
    
    abstract public void togglePause();
};
