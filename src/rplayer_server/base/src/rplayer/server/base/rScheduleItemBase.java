package rplayer.server.base;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

abstract class rScheduleItemBase
{
    public enum ItemTypes 
    {
        GROUP, SIMPLE_FS
    }
    
    protected ItemTypes m_type;
    
    public ItemTypes GetType()
    {
        return m_type;
    }
    
    abstract public boolean Eoi();
    
    public abstract String GetNextItem();
    
    public abstract void Ban( String pattern );
    
    public abstract void WriteContent( Document xmlDocument, Element xmlElement );
};
