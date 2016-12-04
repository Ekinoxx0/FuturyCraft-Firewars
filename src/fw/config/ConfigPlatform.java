package fw.config;

import api.config.ConfigLocation;
import api.utils.Region;

/**
 * Created by loucass003 on 26/11/16.
 */
public class ConfigPlatform
{
    public ConfigLocation firstPoint;
    public ConfigLocation lastPoint;

    public Region getRegion()
    {
        return new Region(firstPoint.getLocation(), lastPoint.getLocation());
    }
}
