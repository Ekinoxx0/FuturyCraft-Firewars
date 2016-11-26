package fw.config;

import api.config.ConfigLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by loucass003 on 26/11/16.
 */
public class ConfigData {

    public List<ConfigLocation> gameSpawns;
    public List<ConfigPlatform> platforms;

    public ConfigData()
    {
        gameSpawns = new ArrayList<>();
        platforms = new ArrayList<>();
    }

    public boolean minimumConfigIsSet()
    {
        return gameSpawns.size() > 0 && platforms.size() > 0;
    }
}
