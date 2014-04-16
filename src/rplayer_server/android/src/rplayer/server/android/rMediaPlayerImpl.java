package rplayer.server.android;

import java.io.IOException;

import rplayer.server.base.rMediaPlayer;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;

class rMediaPlayerImpl extends rMediaPlayer implements OnCompletionListener
{
	private MediaPlayer m_android_media_player = null;
	
	public rMediaPlayerImpl()
	{
		m_android_media_player = new MediaPlayer();
		m_android_media_player.setOnCompletionListener( this );
	}
	
	public void onCompletion(MediaPlayer  mp)
	{
		if( m_mediaPlayerEventHandler == null ) return; // exception
		
		m_mediaPlayerEventHandler.HandleItemCompletion();
	}

	@Override
	public void resetPlayer()
	{
		m_android_media_player.reset(); 
	}

	@Override
	public void setNextItemToPlay( String a_Item ) 
	{
	    try 
	    {
	    	m_android_media_player.setDataSource( a_Item ); 
	    } 
	    catch (IllegalArgumentException e ){ e.printStackTrace(); } 
	    catch (IllegalStateException e)    { e.printStackTrace(); } 
	    catch (IOException e)              { e.printStackTrace(); }
	    
	    try { m_android_media_player.prepare(); } 
	    catch (IllegalStateException e) { e.printStackTrace(); } 
	    catch (IOException e)           { e.printStackTrace(); }
	}

	@Override
	public void startPlayback()
	{
		m_android_media_player.start();
	}

	@Override
	public float getVolume() {
		// TODO Auto-generated method stub
		return 0;
	} 

	@Override
	public void setVolume(float volume) {
		// TODO Auto-generated method stub
		
	}
};
