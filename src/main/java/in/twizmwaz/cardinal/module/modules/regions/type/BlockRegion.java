package in.twizmwaz.cardinal.module.modules.regions.type;

import in.twizmwaz.cardinal.module.modules.regions.Region;
import in.twizmwaz.cardinal.module.modules.regions.parsers.BlockParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;


/**
 * Created by kevin on 10/26/14.
 */
public class BlockRegion extends Region {

    private String name;
    private double x;
    private double y;
    private double z;

    public BlockRegion(String name, int x, int y, int z) {
        super(name);
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockRegion(BlockParser parser) {
        super(parser.getName());
        this.x = parser.getX();
        this.y = parser.getY();
        this.z = parser.getZ();

    }

    public String getName() {
        return super.getName();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    @Override
    public boolean contains(BlockRegion region) {
        return region.getX() == getX() && region.getY() == getY() && region.getZ() == getZ();
    }

    public Location getLocation() {
        return new Location(Bukkit.getWorlds().get(0), x, y, z);
    }
}
