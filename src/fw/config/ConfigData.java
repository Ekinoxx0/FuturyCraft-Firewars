package fw.config;

import api.config.ConfigLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by loucass003 on 26/11/16.
 */
public class ConfigData
{
    public List<ConfigLocation> gameSpawns;
    public List<ConfigPlatform> platforms;
    public ConfigLocation spectatorLoc;
    public int deathLevel;

    public ConfigData()
    {
        gameSpawns = new ArrayList<>();
        platforms = new ArrayList<>();
        spectatorLoc = null;
        deathLevel = -1;
    }

    public boolean minimumConfigIsSet()
    {
        return deathLevel >= 0 && spectatorLoc != null && gameSpawns.size() > 0 && platforms.size() > 0;
    }
}
