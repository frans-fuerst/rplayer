package rplayer.server.base;

import java.util.Collections;
import java.util.LinkedList;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import java.io.File;

/// rScheduleItemSimpleFS is a simple file traverser 
/// schedule item: it loads everything from a given 
/// path except the items contained in a special 
/// exception list
class rScheduleItemSimpleFS extends rScheduleItemBase
{
    private class rFileSystemItem
    {
        /// we're a tree - these are our sub trees
        private LinkedList< rFileSystemItem > m_stackableItems = new LinkedList< rFileSystemItem >();
        
        /// these are the leafs - actual media files to play
        private LinkedList< String > m_playableItems = new LinkedList< String >();

        private String m_baseDir;
        private String m_url;
        private int m_index;
        private boolean m_expanded = false;
        private boolean m_stackableItemsComplete = false;
        private boolean m_playableItemsComplete = false;

        public rFileSystemItem( String a_baseDir, String a_newUrl )
        {
            m_baseDir = a_baseDir; 
            m_url = a_newUrl; 
        }
        
        /// this will go through a given folder and save a list for all folders
        /// and mp3 files contained
        public void expand()
        {
            System.out.println("rFileSystemItem:: expand()");
            m_index = 0;
            m_stackableItems.clear();
            m_playableItems.clear();
            m_stackableItemsComplete = true;
            m_playableItemsComplete = true;
            try
            {
                File l_dir = new File( m_baseDir + "/" + m_url );
                if( !l_dir.exists() )
                {
                    System.out.println( "rFileSystemItem: warning: folder '" + m_baseDir + "/" + m_url + "' does not exist!" );
                    return;
                }
                m_expanded = true;
                File[]  l_files = l_dir.listFiles();
                for( File f: l_files )
                {
                    System.out.println(f.toString());
                    if( f.isFile() )
                    {
                        if( f.toString().endsWith(".mp3") )
                        {
                            m_playableItems.add( stripBaseDir( f.toString(), m_baseDir ) );
                        }
                    }
                    if( f.isDirectory() )
                    {
                        m_stackableItems.add( new rFileSystemItem( m_baseDir, stripBaseDir( f.toString(), m_baseDir ) ) );
                    }
                }
            }
            catch(Exception e) { e.printStackTrace(); }
            
            Collections.shuffle( m_stackableItems );
            Collections.sort(m_playableItems);
            m_stackableItemsComplete = m_stackableItems.size() == 0;
            m_playableItemsComplete = m_playableItems.size() == 0;
        }

        public void collapse()
        {
            m_playableItems.clear();
            m_stackableItems.clear();
            m_expanded = false;
        }

        public boolean isComplete()
        {
            if( !m_expanded ) expand();
            return m_stackableItemsComplete && m_playableItemsComplete;
        }

        public String getNextPlayableItem()
        {
            if( !m_expanded ) expand();
            
            /// first try to handle folders
            if( m_stackableItems.size() > 0 )
            {
                /// clean up m_stackableItems which are depleated
                while( m_stackableItems.get(m_index).isComplete() )
                {
                    m_stackableItems.get(m_index).collapse();
                    m_index = (m_index + 1) % m_stackableItems.size();
                    if( m_index == 0 )
                    {
                        m_stackableItemsComplete = true;
                        break;
                    }
                }
                /*
                if( m_stackableItemsComplete )
                {
                    return null;
                }
                */
                return m_stackableItems.get(m_index).getNextPlayableItem();
            }
            
            
            if( m_playableItems.size() > 0 )
            {
                String l_return = m_playableItems.get(m_index);
                m_index = (m_index + 1) % m_playableItems.size();
                if( m_index == 0 ) m_playableItemsComplete = true;
                return l_return;
            }
            else
            {
                return null;
            }
        }
    }
        
    //String m_location = null;
    private LinkedList< String > m_exclusions = new LinkedList< String >();
    private String m_location;
    private rFileSystemItem m_playList;
    private String m_fileSeparator = System.getProperty("file.separator");

    /// this constructor will load 
    public rScheduleItemSimpleFS( Element a_element, String a_basedir )
    {
        System.out.println( "rScheduleItemSimpleFS( <" + a_element.getTagName() + "> )" );
        if( !a_element.getTagName().equals("item") ) return;
        /// we're inside an item so we can read type, url etc
        
        if( a_element.hasAttribute("location") )
        {
            m_location = a_element.getAttribute("location");
            System.out.println( "rScheduleItemSimpleFS: location = '" + m_location + "'" );
            m_playList = new rFileSystemItem( a_basedir, m_location );
        }
        else
        {
            /// error
            
        }
        
        for( Node l_entry = a_element.getFirstChild(); l_entry != null; l_entry = l_entry.getNextSibling() )
        {
            if( l_entry.getNodeName().equals("exclude")) 
            {
                Element l_childElement = (Element)l_entry;
                if( l_childElement.hasAttribute( "value" ) )
                {
                    System.out.println( "rScheduleItemSimpleFS: exclude: '" + l_childElement.getAttribute( "value" ) + "'");
                    m_exclusions.add( l_childElement.getAttribute( "value" ) );
                }
            }
        }
        System.out.println( "rScheduleItemSimpleFS() leave!");
    }
    
    public String GetNextItem()
    {
        String l_nextItem = "";
        
        /// go through the list until we have an appropriate item to play
        do
        {
            /// abort search if we are trough anyway
            if(m_playList.isComplete() ) break;
            
            
            l_nextItem = m_playList.getNextPlayableItem();
            System.out.println( "getNextItem try "+l_nextItem );
        }
        while( l_nextItem == null || stringMatchesExcludeList( l_nextItem ) );
        
        return l_nextItem;
    }
    
    public void Ban( String a_pattern )
    {
        m_exclusions.add( a_pattern );
    }
    
    public void WriteContent( Document xmlDocument, Element xmlElement )
    {
        Element l_item = xmlDocument.createElement("item");
        l_item.setAttribute("type", "simple_fs");
        l_item.setAttribute("location", m_location );
        xmlElement.appendChild( l_item );
        for(String l_exclude : m_exclusions )
        {
            Element l_excludeElement = xmlDocument.createElement("exclude");
            l_excludeElement.setAttribute("value", l_exclude);
            l_item.appendChild( l_excludeElement );
        }
    }
    
    public boolean Eoi()
    {
        return m_playList.isComplete();
    }
    
    private boolean stringMatchesExcludeList( String a_string )
    {
        for(String l_exclude : m_exclusions )
        {
            if( a_string.contains( l_exclude ))
            {
                System.out.println( "rScheduleItemSimpleFS: item '" + a_string + "' ommitted because it contains '" + l_exclude + "'" );
                return true;
            }
        }
        return false;
    }
    
    private String stripBaseDir( String a_filename, String a_basedir )
    {
        a_filename = a_filename.replace( m_fileSeparator, "/");
        a_basedir = a_basedir.replace( m_fileSeparator, "/");
        if( a_filename.startsWith( a_basedir ))
        {
            return a_filename.substring( a_basedir.length() );
        }
        else
        {
            System.out.println("stripBaseDir(): cannot strip '" + a_basedir + "' from '"+a_filename+"'");
            return a_filename;
        }
    }
};
