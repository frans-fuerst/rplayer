package rplayer.server.base;

import java.util.LinkedList;
import java.util.ListIterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/// rScheduleItemGroup is tree element node with capability
/// to load schedule content from an xml element
class rScheduleItemGroup extends rScheduleItemBase
{
    
    /// these are the elements making up the sub trees
    private LinkedList< rScheduleItemBase > m_subitems = new LinkedList< rScheduleItemBase >();
    private ListIterator< rScheduleItemBase > m_active_item_iterator;
    private rScheduleItemBase m_active_item;

    /// this will load item content from an xml element
    public rScheduleItemGroup( Element a_element, String a_baseDirectory )
    {
        System.out.println( "rScheduleItemGroup( <" + a_element.getTagName() + "> )" );
        
        super.m_type = ItemTypes.GROUP;

//        System.out.print( "rScheduleItemGroup: #children: " );
//        System.out.println( l_entries.getLength() );
        int l_counter = 0;
        /// traverse all children
        for( Node l_entry = a_element.getFirstChild(); l_entry != null; l_entry = l_entry.getNextSibling(), ++l_counter )
        {
            if( !l_entry.getNodeName().equals("item")) continue;
            
            Element l_element = (Element)l_entry;
            if( l_element.hasAttribute("type") )
            {
                String l_type = l_element.getAttribute("type");
                System.out.println( "rScheduleItemGroup:: item " + l_counter + ": type: '" + l_type + "'" );
                
                if( l_type.equals( "simple_fs" ) )
                {
                    m_subitems.add( new rScheduleItemSimpleFS( l_element, a_baseDirectory ) );
                }
            }
            else
            {
                /// no type given: error!
            }
        }
        m_active_item_iterator = m_subitems.listIterator();
        if( m_active_item_iterator.hasNext() )
        {
            m_active_item = m_active_item_iterator.next();
        }
        else
        {
            m_active_item = null;
        }
    }

    public void Ban( String pattern )
    {
        if( m_active_item == null ) return;
        m_active_item.Ban( pattern );
    }
    
    public void WriteContent( Document xmlDocument, Element xmlElement )
    {
        for( rScheduleItemBase i: m_subitems)
        {
            i.WriteContent( xmlDocument, xmlElement );
        }
    }

    
    /// return the url to the next media file or return empty
    /// if there is nothing left to play
    public String GetNextItem()
    {
        /// no item to play - return empty string
        if( m_active_item == null ) return "";
        
        /// if the current item is fully played through, take the next one
        if( m_active_item.Eoi() )
        {
            if( m_active_item_iterator.hasNext() )
            {
                m_active_item = m_active_item_iterator.next();
            }
            else
            {
                m_active_item = null;
            }
        }
        
        /// maybe the list was empty - also return empty
        if( m_active_item == null ) return "";
        
        return m_active_item.GetNextItem();
    }
    
    /// tell the world whether we have anything left to play.. 
    public boolean Eoi()
    {
        /// we're not through the list yet..
        if( m_active_item_iterator.hasNext() ) return false;
        
        /// we're through the list and we have no item to play.. die!
        if( m_active_item == null ) return true;
        
        /// we have one item left to play - every depends on this item now..
        return m_active_item.Eoi();
    }
}