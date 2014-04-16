package rplayer.server.android;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
//import android.os.Message;

import rplayer.server.base.rGuiInterface;
import rplayer.server.base.rServerBase;
import rplayer.server.android.rMediaPlayerImpl;

public class rplayer extends Activity implements rGuiInterface
{
	TextView tv;
	TextView m_tv_address;
	String m_str_address = "";
	String s;
	rServerBase m_server;
	
	// Called when the activity is first created. 
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
	    super.onCreate(savedInstanceState);
	    tv = new TextView(this);
	    setContentView(tv);
	    
	    rMediaPlayerImpl l_player = new rMediaPlayerImpl();
	    l_player.setGuiInterface( this );
	    
	    m_server = new rServerBase( l_player );
	    m_server.SetGuiInterface( this );
	    
	    m_server.StartUp();
	}
	

	private Handler m_messageHandler = new Handler() 
   	{
	      @Override
	      public void  handleMessage(Message msg) 
	      {  
	    	   tv.setText( s );
//		          switch(msg.what) {
//		    	   tv.setText("Blu!");
//		          }
	      }
   	};

	public void sendTextMessage(String textMessage)
	{
		s = textMessage;
		m_messageHandler.sendMessage(Message.obtain( m_messageHandler ) );
		
	}
}
