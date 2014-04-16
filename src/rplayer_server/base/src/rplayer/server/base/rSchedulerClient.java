package rplayer.server.base;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class rSchedulerClient
{
    Socket m_socket;
    PrintWriter m_out;
    BufferedReader m_in;
    boolean m_sending = true;
    boolean m_connected = false;
    rServerBase m_parent;
    
    public rSchedulerClient( Socket a_socket, rServerBase a_parent )
    {
        m_socket = a_socket;
        m_parent = a_parent;
    }
    
    public void Disconnect()
    {
        SendMessage( "quit" );
    }
    
    public void SendMessage( String a_message )
    {
        if( !m_connected ) return;

        m_out.println( a_message );
        //m_out.flush();
        
    }
    
    public rSchedulerClient start()
    {
        try 
        {
            m_out = new PrintWriter(m_socket.getOutputStream(), true);
            m_in = new BufferedReader( new InputStreamReader( m_socket.getInputStream()));
        } 
        catch (IOException e)
        {
            e.printStackTrace();
        }
        m_connected = true;
        
        SendMessage( "playing "+  m_parent.CurrentTheme() + " " + m_parent.CurrentlyPlayed() );
        
        ( new Thread(){ public void run()
        {
            String userInput;
            try
            {
                while ((userInput = m_in.readLine()) != null)
                {
                    System.out.println("rSchedulerClient: a client sent '" + userInput + "'");
                    
                    if( userInput.startsWith("hallo"))
                    {
//                        m_parent.SetBaseDirectory(userInput.substring(8));
                    }
                    
                    else
                    {
                        m_parent.HandleClientData( userInput );
                        //m_parent.setText( userInput );
                    }
                }
            }
            catch( Exception e )
            {
                SendMessage( "rSchedulerClient: exception while trying to read from socket: " + e.toString() );
            }
            
            System.out.println("rSchedulerClient: client disconnected");
            try 
            { 
                m_socket.close();    
            } 
            catch( Exception e )
            {
                e.printStackTrace();
            }
            
        }}).start();

        return this;
    }
}
