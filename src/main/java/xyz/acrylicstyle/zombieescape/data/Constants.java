package xyz.acrylicstyle.zombieescape.data;

import java.util.HashMap;

import org.bukkit.Material;

public class Constants {
	/**
	 * Represents material(block) durability
	 */
	public static HashMap<Material, Integer> materialDurability = new HashMap<Material, Integer>();

	static {
		materialDurability.put(Material.DIRT, 60);
		materialDurability.put(Material.GRASS, 60);
		materialDurability.put(Material.WOOD, 200);
		materialDurability.put(Material.COBBLESTONE, 3000);
		materialDurability.put(Material.BEDROCK, 50000000);
		materialDurability.put(Material.COMMAND, 50000000);
	}
}
