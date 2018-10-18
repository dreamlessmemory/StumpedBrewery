package com.dreamless.brewery;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class BreweryUtils {
	public final static List<HashMap<Map<String, Object>, Map<String, Object>>> serializeItemStackList(final ItemStack[] itemStackList) {
		final List<HashMap<Map<String, Object>, Map<String, Object>>> serializedItemStackList = new ArrayList<HashMap<Map<String, Object>, Map<String, Object>>>();
		
		for (ItemStack itemStack : itemStackList) {
			Map<String, Object> serializedItemStack, serializedItemMeta;
			HashMap<Map<String, Object>, Map<String, Object>> serializedMap = new HashMap<Map<String, Object>, Map<String, Object>>();
			
			if (itemStack == null) itemStack = new ItemStack(Material.AIR);
			serializedItemMeta = (itemStack.hasItemMeta())
				? itemStack.getItemMeta().serialize()
				: null;
			itemStack.setItemMeta(null);
			serializedItemStack = itemStack.serialize();
			
			serializedMap.put(serializedItemStack, serializedItemMeta);
			serializedItemStackList.add(serializedMap);
		}
		return serializedItemStackList;
	}
	
	public final static ItemStack[] deserializeItemStackList(final List<HashMap<Map<String, Object>, Map<String, Object>>> serializedItemStackList) {
		final ItemStack[] itemStackList = new ItemStack[serializedItemStackList.size()];
		
		int i = 0;
		for (HashMap<Map<String, Object>, Map<String, Object>> serializedItemStackMap : serializedItemStackList) {
			Entry<Map<String, Object>, Map<String, Object>> serializedItemStack = serializedItemStackMap.entrySet().iterator().next();
			
			ItemStack itemStack = ItemStack.deserialize(serializedItemStack.getKey());
			if (serializedItemStack.getValue() != null) {
				ItemMeta itemMeta = (ItemMeta)ConfigurationSerialization.deserializeObject(serializedItemStack.getValue(), ConfigurationSerialization.getClassByAlias("ItemMeta"));
				itemStack.setItemMeta(itemMeta);
			}
			
			itemStackList[i++] = itemStack;
		}
		return itemStackList;
	}
	

    /**
     * Gets an item back from the Map created by {@link serialize()}
     *
     * @param map The map to deserialize from.
     * @return The deserialized item.
     * @throws IllegalAccessException Things can go wrong.
     * @throws IllegalArgumentException Things can go wrong.
     * @throws InvocationTargetException Things can go wrong.
     */
    public static ItemStack deserialize(Map<String, Object> map) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        ItemStack i = ItemStack.deserialize(map);
        if (map.containsKey("meta")) {
            try {
                //  org.bukkit.craftbukkit.v1_8_R3.CraftMetaItem$SerializableMeta
                //  CraftMetaItem.SerializableMeta.deserialize(Map<String, Object>)
                if (ITEM_META_DESERIALIZATOR != null) {
                    ItemMeta im = (ItemMeta) DESERIALIZE.invoke(i, map.get("meta"));
                    i.setItemMeta(im);
                }
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw e;
            }
        }
        return i;
    }

    /**
     * Serializes an ItemStack and it's ItemMeta, use {@link deserialize()} to
     * get the item back.
     *
     * @param item Item to serialize
     * @return A HashMap with the serialized item
     */
    public static Map<String, Object> serialize(ItemStack item) {
        HashMap<String, Object> itemDocument = new HashMap(item.serialize());
        if (item.hasItemMeta()) {
            itemDocument.put("meta", new HashMap(item.getItemMeta().serialize()));
        }
        return itemDocument;
    }

    //Below here lays some crazy shit that make the above methods work :D yay!
    // <editor-fold desc="Some crazy shit" defaultstate="collapsed">
    /*
     * @return The string used in the CraftBukkit package for the version.
     */
    public static String getVersion() {
        String name = Bukkit.getServer().getClass().getPackage().getName();
        String version = name.substring(name.lastIndexOf('.') + 1) + ".";
        return version;
    }

    /**
     * Basic reflection.
     *
     * @param className
     * @return
     */
    public static Class<?> getOBCClass(String className) {
        String fullName = "org.bukkit.craftbukkit." + getVersion() + className;
        Class<?> clazz = null;
        try {
            clazz = Class.forName(fullName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clazz;
    }
    private static final Class ITEM_META_DESERIALIZATOR = getOBCClass("inventory.CraftMetaItem").getClasses()[0];
    private static final Method DESERIALIZE = getDeserialize();

    private static Method getDeserialize() {

        try {
            return ITEM_META_DESERIALIZATOR.getMethod("deserialize", new Class[]{Map.class});
        } catch (NoSuchMethodException | SecurityException ex) {
            return null;
        }
    }
    // </editor-fold>
}