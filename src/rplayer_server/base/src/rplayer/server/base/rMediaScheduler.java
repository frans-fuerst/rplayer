package rplayer.server.base;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class rMediaScheduler
{
    
    //private rStackablePlayListItem m_actualPlaylist;
    private rScheduleItemBase m_abstractPlaylist = null;
    //private int m_index;
    private rGuiInterface m_gui_interface = null;
    
    private String m_scheduleFileName = null;

    public void setGuiInterface( rGuiInterface a_interface )
    {
        m_gui_interface = a_interface;
    }
    
    public void Ban( String a_pattern )
    {
        m_abstractPlaylist.Ban( a_pattern );
    }
    
    public void WriteScheduleFile() 
    {
        //We need a Document
        DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder;
        try { 
            docBuilder = dbfac.newDocumentBuilder();
        } 
        catch (ParserConfigurationException e) 
        {
            e.printStackTrace();
            return;
        }
        Document doc = docBuilder.newDocument();

        //create the root element and add it to the document
        Element root = doc.createElement("rplayer_meta_playlist");
        doc.appendChild( root );
        root.setAttribute("name", "chilling");

        //create a comment and put it in the root element
//        Comment comment = doc.createComment("Just a thought");
//        root.appendChild(comment);

        //create child element, add an attribute, and add to root
        m_abstractPlaylist.WriteContent( doc, root );
        //add a text element to the child
//        Text text = doc.createTextNode("Filler, ... I could have had a foo!");
//        child.appendChild(text);

        /////////////////
        //Output the XML

        //set up a transformer
        TransformerFactory transfac = TransformerFactory.newInstance();
        Transformer trans;
        try {
            trans = transfac.newTransformer();
        } catch (TransformerConfigurationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return;
        }
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        trans.setOutputProperty(OutputKeys.INDENT, "yes");

        //create string from xml tree
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        DOMSource source = new DOMSource(doc);
        try { trans.transform(source, result);} 
        catch (TransformerException e) { e.printStackTrace(); }
        String xmlString = sw.toString();

        //print xml
        System.out.println("Here's the xml:\n\n" + xmlString);
        Writer out;
        try {
            out = new OutputStreamWriter(new FileOutputStream( m_scheduleFileName ) );
            out.write( xmlString );
            out.close();
        } 
        catch (FileNotFoundException e) { e.printStackTrace(); }
        catch (IOException e) { e.printStackTrace(); }
        
    }
    
    public void loadPlaylist( String a_file, String a_baseDirectory )
    {
        m_scheduleFileName = a_file;
        try {
            System.out.println( "rMediaScheduler::Load: " + m_scheduleFileName );
            System.out.println( "rMediaScheduler::BaseDir: " + a_baseDirectory );
            Document l_playlistDocument = DocumentBuilderFactory.newInstance()
                                      .newDocumentBuilder()
                                      .parse(m_scheduleFileName);
            Element l_root = l_playlistDocument.getDocumentElement();
            System.out.println( l_root.getChildNodes() );
            m_abstractPlaylist = new rScheduleItemGroup( l_root, a_baseDirectory );
        } 
        catch (SAXException e) {
            e.printStackTrace();
        } 
        catch (IOException e) {
            e.printStackTrace();
        } 
        catch (ParserConfigurationException e) {
            e.printStackTrace();
        }
    }
    
    public rMediaScheduler() 
    {
//        m_index = 0;
    }
    
    /*
    public void addPlayListItem( String a_item )
    {
        m_actualPlaylist.add( new rStackablePlayListItem( a_item ) );
    }
    */
    
    public String getNextItem()
    {
        String l_return = m_abstractPlaylist.GetNextItem();
        m_gui_interface.sendTextMessage( l_return  );
        return l_return ;
    }   
};
