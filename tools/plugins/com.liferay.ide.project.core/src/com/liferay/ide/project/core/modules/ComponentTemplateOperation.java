package com.liferay.ide.project.core.modules;


public class ComponentTemplateOperation
{

    private String name;
    private String source;
    private String type;
    private String location;
    private Boolean primary;
    private Boolean isInChildFolder;

    /**
     * @return the type
     */
    public String getType()
    {
        return type;
    }
    /**
     * @param type the type to set
     */
    public void setType( String type )
    {
        this.type = type;
    }
    /**
     * @return the location
     */
    public String getLocation()
    {
        return location;
    }
    /**
     * @param location the location to set
     */
    public void setLocation( String location )
    {
        this.location = location;
    }
    /**
     * @return the source
     */
    public String getSource()
    {
        return source;
    }
    /**
     * @param source the source to set
     */
    public void setSource( String source )
    {
        this.source = source;
    }
    /**
     * @return the primary
     */
    public Boolean isPrimary()
    {
        return primary;
    }
    /**
     * @param primary the primary to set
     */
    public void setPrimary( Boolean primary )
    {
        this.primary = primary;
    }
    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }
    /**
     * @param name the name to set
     */
    public void setName( String name )
    {
        this.name = name;
    }
    /**
     * @return the isInChildFolder
     */
    public Boolean getIsInChildFolder()
    {
        return isInChildFolder;
    }
    /**
     * @param isInChildFolder the isInChildFolder to set
     */
    public void setIsInChildFolder( Boolean isInChildFolder )
    {
        this.isInChildFolder = isInChildFolder;
    }
}
