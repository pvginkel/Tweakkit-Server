package com.afterkraft.metadata;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.NBTBase;
import net.minecraft.server.NBTTagByte;
import net.minecraft.server.NBTTagByteArray;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagDouble;
import net.minecraft.server.NBTTagFloat;
import net.minecraft.server.NBTTagInt;
import net.minecraft.server.NBTTagIntArray;
import net.minecraft.server.NBTTagList;
import net.minecraft.server.NBTTagLong;
import net.minecraft.server.NBTTagShort;
import net.minecraft.server.NBTTagString;
import org.bukkit.Bukkit;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.NumberConversions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Encapsulates any custom data that may be stored in an NBTTagCompound.
 * <p>
 * Handles serialization of ConfigurationSerialization objects and
 * basic data types to and from a persistent data store.
 * <p>
 * This is used by CraftMetaItem to implement the Metadatable interface
 * by mapping Plugin ownership to a standard tag structure.
 */
public class NBTMetadataStore implements Cloneable {
    // Theses are key strings used to store and check for custom data
    // Tweakkit may store custom internal data under TWEAKKIT_DATA_KEY.
    // Custom Plugin data (from the Metadatable interface)
    // is stored under TWEAKKIT_DATA_KEY.PLUGIN_DATA_KEY.<plugin name>
    public final static String TWEAKKIT_DATA_KEY = "tweakkit";
    public final static String PLUGIN_DATA_KEY = "plugins";

    // This is copied from CraftMetaItem.SerializableMeta
    // It is needed for filtering, and I wasn't sure how to cleanly share
    // this without a lot of refactoring.
    static final String TYPE_FIELD = "meta-type";

    protected NBTTagCompound tag;

    /**
     * Returns a filtered store, such that it may not contain any of
     * the specified keys.
     * <p>
     * If there is no unfiltered data in the given tag, this method will
     * not create a new store, and will return null.
     *
     * @param tag The tag to scan for data
     * @param filterKeys A Set of keys to filter out
     * @return A new NBTMetadataStore containing a copy of the data in
     *   tag, minus what was filtered. If the end result is empty,
     *   null will be returned.
     */
    public static NBTMetadataStore getFilteredStore(NBTTagCompound tag, Set<String> filterKeys) {
        NBTTagCompound filteredTag = null;
        Set<String> keys = getAllKeys(tag);
        for (String key : keys) {
            if (filterKeys.contains(key)) {
                continue;
            }

            if (filteredTag == null) {
                filteredTag = new NBTTagCompound();
            }

            filteredTag.set(key, tag.get(key).clone());
        }
        return filteredTag == null ? null : new NBTMetadataStore(filteredTag);
    }

    /**
     * Returns a filtered store, such that it may not contain any of
     * the specified keys.
     * <p>
     * If there is no unfiltered data in the given Map, this method will
     * not create a new store, and will return null.
     *
     * @param dataMap The Ma[ to scan for data
     * @param filterKeys A Set of keys to filter out
     * @return A new NBTMetadataStore containing a copy of the data in
     *   dataMap, minus what was filtered. If the end result is empty,
     *   null will be returned.
     */
    public static NBTMetadataStore getFilteredStore(Map<String, Object> dataMap, Set<String> filterKeys) {
        NBTTagCompound filteredTag = null;
        Set<String> keys = dataMap.keySet();
        for (String key : keys) {
            if (filterKeys.contains(key)) {
                continue;
            }

            // Filter out special ConfigurationSerialization and
            // SerializableMeta tags.
            // These seem like they shouldn't make it this far down the pipeline,
            // but it seems like they will be in the root after deserialization.
            if (key.equals(ConfigurationSerialization.SERIALIZED_TYPE_KEY) || key.equals(TYPE_FIELD)) {
                continue;
            }

            if (filteredTag == null) {
                filteredTag = new NBTTagCompound();
            }
            filteredTag.set(key, convert(dataMap.get(key)));
        }
        return filteredTag == null ? null : new NBTMetadataStore(filteredTag);
    }

    /**
     * Check to see if a tag has a specific key
     * registered to any Plugin.
     *
     * @param tag The tag to scan for data
     * @param key The key to check for
     * @return True if the tag has a non-empty
     *   TWEAKKIT_DATA_KEY.PLUGIN_DATA_KEY compound.
     */
    public static boolean hasPluginMetadata(NBTTagCompound tag, String key) {
        NBTTagCompound bukkitRoot = tag.getCompound(TWEAKKIT_DATA_KEY);
        if (bukkitRoot == null) return false;
        NBTTagCompound pluginsRoot = bukkitRoot.getCompound(PLUGIN_DATA_KEY);
        if (pluginsRoot == null) return false;

        // We must scan for data here. This could be optimized with a
        // ref-counted Map of keys, but I think the (String, Plugin)
        // method should be the more common use case.
        Set<String> plugins = getAllKeys(pluginsRoot);
        for (String plugin : plugins) {
            NBTTagCompound pluginRoot = pluginsRoot.getCompound(plugin);
            if (pluginRoot.hasKey(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check to see if a tag has any plugin data on it.
     *
     * @param tag The tag to scan for data
     * @param key The key to check for
     * @param owningPlugin The plugin to check for data
     * @return True if the tag has a non-empty
     *   TWEAKKIT_DATA_KEY.PLUGIN_DATA_KEY.<owningPlugin.name> compound.
     */
    public static boolean hasPluginMetadata(NBTTagCompound tag, String key, Plugin owningPlugin) {
        NBTTagCompound bukkitRoot = tag.getCompound(TWEAKKIT_DATA_KEY);
        if (bukkitRoot == null) return false;
        NBTTagCompound pluginsRoot = bukkitRoot.getCompound(PLUGIN_DATA_KEY);
        String pluginName = owningPlugin.getName();
        if (pluginsRoot == null || !pluginsRoot.hasKey(pluginName)) return false;
        NBTTagCompound pluginRoot = pluginsRoot.getCompound(pluginName);
        return pluginRoot != null && pluginRoot.hasKey(key);
    }

    /**
     * Create an empty data store.
     */
    public NBTMetadataStore() {
        this.tag = new NBTTagCompound();
    }

    /**
     * Wrap a data store around an existing NBTTagCompound.
     *
     * @param tag The root of this datastore.
     */
    private NBTMetadataStore(NBTTagCompound tag) {
        this.tag = tag;
    }

    /**
     * Retrieve the NBTTagCompound that holds all of the Plugin metadata.
     *
     * @param create If True, the path to the root will be created if it does not exist.
     * @return The NBTTagCompound that holds this data
     */
    protected NBTTagCompound getPluginMetadataRoot(boolean create) {
        NBTTagCompound bukkitRoot = getTweakkitDataRoot(create);
        NBTTagCompound pluginsRoot = bukkitRoot.getCompound(PLUGIN_DATA_KEY);
        if (create) {
            bukkitRoot.set(PLUGIN_DATA_KEY, pluginsRoot);
        }
        return pluginsRoot;
    }

    /**
     * Store a MetadataValue.
     * <p>
     * Throws an IllegalArgumentException if setting anything
     * other than a PersistentMetadataValue value.
     *
     * @param metadataKey The metadata key to store
     * @param newMetadataValue The value to store, must be PersistentMetadataValue
     */
    public void setPluginMetadata(String metadataKey, MetadataValue newMetadataValue) {
        if (!(newMetadataValue instanceof PersistentMetadataValue)) {
            throw new IllegalArgumentException("This store can only hold PersistentMetadataValue");
        }

        NBTTagCompound metadataRoot = getPluginMetadataRoot(true);
        String pluginName = newMetadataValue.getOwningPlugin().getName();
        NBTTagCompound pluginTag = metadataRoot.getCompound(pluginName);
        pluginTag.set(metadataKey, convert(newMetadataValue.value()));
        metadataRoot.set(pluginName, pluginTag);
    }

    /**
     * Retrieve all stored metadata for all plugins.
     *
     * @param metadataKey The metadata to look up
     * @return A List of values found, or an empty List.
     */
    public List<MetadataValue> getPluginMetadata(String metadataKey) {
        NBTTagCompound metadataRoot = getPluginMetadataRoot(false);
        PluginManager pm = Bukkit.getPluginManager();
        List<MetadataValue> metadata = new ArrayList<MetadataValue>();

        Set<String> pluginKeys = getAllKeys(metadataRoot);
        for (String pluginKey : pluginKeys) {
            NBTTagCompound pluginData = metadataRoot.getCompound(pluginKey);
            if (pluginData.hasKey(metadataKey)) {
                MetadataValue value = convertToMetadata(pm.getPlugin(pluginKey), pluginData.get(metadataKey));
                if (value != null) {
                    metadata.add(value);
                }
            }
        }
        return Collections.unmodifiableList(metadata);
    }

    /**
     * Retrieve a single key of stored metadata for a specific Plugin
     *
     * @param metadataKey The metadata to look up
     * @param owningPlugin The Plugin to look for data
     * @return A List of values found, or an empty List.
     */
    public MetadataValue getPluginMetadata(String metadataKey, Plugin owningPlugin) {
        NBTTagCompound metadataRoot = getPluginMetadataRoot(false);
        String pluginName = owningPlugin.getName();
        if (!metadataRoot.hasKey(pluginName)) {
            return null;
        }
        NBTTagCompound pluginTag = metadataRoot.getCompound(pluginName);
        if (!pluginTag.hasKey(metadataKey)) {
            return null;
        }
        return convertToMetadata(owningPlugin, pluginTag.get(metadataKey));
    }

    /**
     * A helper method to convert a tag directly to a MetadataValue.
     *
     * @param plugin The owning plugin
     * @param tag The tag to convert
     * @return A new MetadataValue object, or null if the tag could not be converted
     */
    public static PersistentMetadataValue convertToMetadata(Plugin plugin, NBTBase tag) {
        if (plugin == null || tag == null) return null;

        Object converted = convert(tag);
        // Note that we don't currently export arrays as metadata
        if (converted instanceof String) {
            return new PersistentMetadataValue(plugin, (String)converted);
        } else if (converted instanceof Integer) {
            return new PersistentMetadataValue(plugin, (Integer)converted);
        } else if (converted instanceof Short) {
            return new PersistentMetadataValue(plugin, (Short)converted);
        } else if (converted instanceof Byte) {
            return new PersistentMetadataValue(plugin, (Byte)converted);
        } else if (converted instanceof Long) {
            return new PersistentMetadataValue(plugin, (Long)converted);
        } else if (converted instanceof Float) {
            return new PersistentMetadataValue(plugin, (Float)converted);
        } else if (converted instanceof Double) {
            return new PersistentMetadataValue(plugin, (Double)converted);
        } else if (converted instanceof Map) {
            return new PersistentMetadataValue(plugin, (Map<String, ?>)converted);
        } else if (converted instanceof List) {
            return new PersistentMetadataValue(plugin, (List<?>)converted);
        } else if (converted instanceof ConfigurationSerializable) {
            return new PersistentMetadataValue(plugin, (ConfigurationSerializable)converted);
        }

        return null;
    }

    /**
     * Check for existing metadata.
     *
     * @param metadataKey The key to check for
     * @return True if the key is present in this store
     */
    public boolean hasPluginMetadata(String metadataKey) {
        return hasPluginMetadata(tag, metadataKey);
    }

    /**
     * Check for existing metadata registered to a specific Plugin.
     *
     * @param metadataKey The key to remove
     * @param owningPlugin the plugin that owns the data
     */
    public boolean hasPluginMetadata(String metadataKey, Plugin owningPlugin) {
        return hasPluginMetadata(tag, metadataKey, owningPlugin);
    }

    /**
     * Remove data from this store.
     *
     * @param metadataKey The key to remove
     * @param owningPlugin The Plugin that owns this data.
     */
    public void removePluginMetadata(String metadataKey, Plugin owningPlugin) {
        NBTTagCompound metadataRoot = getPluginMetadataRoot(false);
        String pluginName = owningPlugin.getName();

        if (!metadataRoot.hasKey(pluginName)) return;

        NBTTagCompound pluginTag = metadataRoot.getCompound(pluginName);
        pluginTag.remove(metadataKey);
        if (pluginTag.isEmpty()) {
            metadataRoot.remove(pluginName);
            if (metadataRoot.isEmpty()) {
                NBTTagCompound bukkitRoot = tag.getCompound(TWEAKKIT_DATA_KEY);
                bukkitRoot.remove(PLUGIN_DATA_KEY);
                if (bukkitRoot.isEmpty()) {
                    tag.remove(TWEAKKIT_DATA_KEY);
                }
            }
        }
    }

    /**
     * Retrieve the NBTTagCompound that holds all of the custom Tweakkit data.
     *
     * @param create If True, the path to the root will be created if it does not exist.
     * @return The NBTTagCompound that holds this data
     */
    protected NBTTagCompound getTweakkitDataRoot(boolean create) {
        NBTTagCompound bukkitRoot = tag.getCompound(TWEAKKIT_DATA_KEY);
        if (create) {
            tag.set(TWEAKKIT_DATA_KEY, bukkitRoot);
        }
        return bukkitRoot;
    }

    /**
     * Check for a specific key of custom Tweakkit data in this store.
     *
     * @param key The key to look for
     * @return True if the key is in this store under the bukkit root.
     */
    public boolean hasTweakkitData(String key) {
        NBTTagCompound bukkitRoot = getTweakkitDataRoot(false);
        return bukkitRoot.hasKey(key);
    }

    /**
     * Store a raw Object in the custom Tweakkit data store
     *
     * @param key The key to store
     * @param value The data to store
     */
    public void setTweakkitData(String key, Object value) {
        NBTTagCompound bukkitRoot = getTweakkitDataRoot(true);
        bukkitRoot.set(key, convert(value));
    }

    /**
     * Retrieve a raw Object stored in the Tweakkit custom data.
     *
     * @param key The key to retrieve
     * @return The object, if it exists in the store, else null.
     */
    public Object getTweakkitData(String key) {
        NBTTagCompound bukkitRoot = getTweakkitDataRoot(false);
        return convert(bukkitRoot.get(key));
    }

    public int getTweakkitDataAsInt(String key) {
        return NumberConversions.toInt(getTweakkitData(key));
    }

    public float getTweakkitDataAsFloat(String key) {
        return NumberConversions.toFloat(getTweakkitData(key));
    }

    public double getTweakkitDataAsDouble(String key) {
        return NumberConversions.toDouble(getTweakkitData(key));
    }

    public long getTweakkitDataAsLong(String key) {
        return NumberConversions.toLong(getTweakkitData(key));
    }

    public short getTweakkitDataAsShort(String key) {
        return NumberConversions.toShort(getTweakkitData(key));
    }

    public byte getTweakkitDataAsByte(String key) {
        return NumberConversions.toByte(getTweakkitData(key));
    }

    public boolean getTweakkitDataAsBoolean(String key) {
        Object value = getTweakkitData(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }

        if (value instanceof Number) {
            return ((Number) value).intValue() != 0;
        }

        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }

        return value != null;
    }

    public String getTweakkitDataAsString(String key) {
        Object value = getTweakkitData(key);

        if (value == null) {
            return "";
        }
        return value.toString();
    }

    /**
     * Remove a specific key of Tweakkit data from this store.
     *
     * @param key The key to remove
     */
    public void removeTweakkitData(String key) {
        NBTTagCompound bukkitRoot = getTweakkitDataRoot(false);
        bukkitRoot.remove(key);
        if (bukkitRoot.isEmpty()) {
            tag.remove(TWEAKKIT_DATA_KEY);
        }
    }

    /**
     * Apply the contents of this data store on top of an item tag.
     * <p>
     * This will overwrite any keys in the tag that are also in this
     * store.
     *
     * @param other The tag to write this data store to
     */
    public void applyToTag(NBTTagCompound other) {
        if (other == null) return;

        Set<String> keys = getAllKeys(tag);
        for (String key : keys) {
            other.set(key, tag.get(key).clone());
        }
    }

    /**
     * This will serialize all data into an ImmutableMap.Builder.
     *
     * @param builder An ImmutableMap builder to serialize this data store.
     * @return The same Map Builder
     */
    public ImmutableMap.Builder<String, Object> serialize(ImmutableMap.Builder<String, Object> builder) {
        Set<String> pluginKeys = getAllKeys(tag);
        for (String pluginKey : pluginKeys) {
            builder.put(pluginKey, convert(tag.get(pluginKey)));
        }
        return builder;
    }

    /**
     * Convert an NBTBase object to an object of the appropriate type for
     * inclusion in our data map.
     * <p>
     * A compound tag may be returned as a Map or, if serialized type
     * information is found, a ConfigurationSerializable Object.
     *
     * @param tag The tag to convert
     * @return The converted object, or null if the data cannot be converted
     */
    private static Object convert(NBTBase tag) {
        if (tag == null) return null;

        Object value = null;
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound)tag;
            Collection<String> keys = getAllKeys(compound);

            // Check for Map or ConfigurationSerializable, both of which are stored in a compound tag
            boolean isSerializedObject = compound.hasKey(ConfigurationSerialization.SERIALIZED_TYPE_KEY);
            Map<String, Object> dataMap = new HashMap<String, Object>();
            for (String tagKey : keys) {
                dataMap.put(tagKey, convert(compound.get(tagKey)));
            }
            if (isSerializedObject) {
                try {
                    value = ConfigurationSerialization.deserializeObject(dataMap);
                    if (value == null) {
                        throw new IllegalArgumentException("Failed to deserialize object of class " + compound.get(ConfigurationSerialization.SERIALIZED_TYPE_KEY));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw new IllegalArgumentException("Failed to deserialize object of class " + compound.get(ConfigurationSerialization.SERIALIZED_TYPE_KEY) + ", " + ex.getMessage());
                }
            } else {
                value = dataMap;
            }
        } else if (tag instanceof NBTTagString) {
            value = ((NBTTagString) tag).a_();
        } else if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList)tag;
            int tagSize = list.size();
            List<Object> convertedList = new ArrayList<Object>(tagSize);
            NBTType listType = NBTType.getListType(list);
            for (int i = 0; i < tagSize; i++) {
                // Convert to appropriate NBT object type
                Object listValue = null;
                switch (listType) {
                    case COMPOUND:
                        listValue = convert(list.get(i));
                        break;
                    case BYTE:
                        listValue = list.getByte(i);
                        break;
                    case SHORT:
                        listValue = list.getShort(i);
                        break;
                    case INT:
                        listValue = list.getInt(i);
                        break;
                    case LONG:
                        listValue = list.getLong(i);
                        break;
                    case FLOAT:
                        listValue = list.e(i);
                        break;
                    case DOUBLE:
                        listValue = list.d(i);
                        break;
                    case BYTE_ARRAY:
                        listValue = list.getByteArray(i);
                        break;
                    case STRING:
                        listValue = list.getString(i);
                        break;
                    case LIST:
                        listValue = convert(list.getList(i));
                        break;
                    case INT_ARRAY:
                        listValue = list.c(i);
                        break;
                }
                if (listValue != null) {
                    convertedList.add(listValue);
                }
            }
            value = convertedList;
        } else if (tag instanceof NBTTagDouble) {
            value = ((NBTTagDouble)tag).g();
        } else if (tag instanceof NBTTagInt) {
            value = ((NBTTagInt)tag).d();
        } else if (tag instanceof NBTTagLong) {
            value = ((NBTTagLong)tag).c();
        } else if (tag instanceof NBTTagFloat) {
            value = ((NBTTagFloat)tag).h();
        } else if (tag instanceof NBTTagByte) {
            value = ((NBTTagByte)tag).f();
        } else if (tag instanceof NBTTagShort) {
            return ((NBTTagShort)tag).e();
        } else if (tag instanceof NBTTagByteArray) {
            value = ((NBTTagByteArray)tag).c();
        } else if (tag instanceof NBTTagIntArray) {
            value = ((NBTTagIntArray)tag).c();
        }

        return value;
    }

    /**
     * Convert an object to an NBTBase object. This creates a copy of the
     * input and wraps it in the appropriate NBT class.
     *
     * @param value The value to copy and wrap
     * @return An NBTBase representation of the input
     */
    @SuppressWarnings("unchecked")
    private static NBTBase convert(Object value) {
        if (value == null) return null;

        NBTBase copiedValue = null;
        if (value instanceof Map) {
            NBTTagCompound subtag = new NBTTagCompound();
            applyToTag(subtag, (Map<String, Object>)value);
            copiedValue = subtag;
        } else if (value instanceof String) {
            copiedValue = new NBTTagString((String)value);
        } else if (value instanceof Integer) {
            copiedValue = new NBTTagInt((Integer)value);
        } else if (value instanceof Long) {
            copiedValue = new NBTTagLong((Long)value);
        } else if (value instanceof Float) {
            copiedValue = new NBTTagFloat((Float)value);
        } else if (value instanceof Double) {
            copiedValue = new NBTTagDouble((Double)value);
        } else if (value instanceof Byte) {
            copiedValue = new NBTTagByte((Byte)value);
        } else if (value instanceof Boolean) {
            copiedValue = new NBTTagByte((Boolean)value ? (byte)1 : (byte)0);
        } else if (value instanceof Short) {
            copiedValue = new NBTTagShort((Short)value);
        } else if (value instanceof List) {
            NBTTagList tagList = new NBTTagList();
            List<Object> list = (List<Object>)value;
            for (Object listValue : list) {
                tagList.add(convert(listValue));
            }
            copiedValue = tagList;
        } else if (value.getClass().isArray()) {
            Class<?> arrayType = value.getClass().getComponentType();
            // I suppose you could convert Byte[], Integer[] here ... Long, Float, etc for that matter.
            if (arrayType == Byte.TYPE) {
                copiedValue = new NBTTagByteArray((byte[]) value);
            } else if (arrayType == Integer.TYPE) {
                copiedValue = new NBTTagIntArray((int[]) value);
            }
        } else if (value instanceof ConfigurationSerializable) {
            ConfigurationSerializable serializable = (ConfigurationSerializable)value;
            Map<String, Object> serializedMap = new HashMap<String, Object>();
            serializedMap.put(ConfigurationSerialization.SERIALIZED_TYPE_KEY, ConfigurationSerialization.getAlias(serializable.getClass()));
            serializedMap.putAll(serializable.serialize());
            NBTTagCompound subtag = new NBTTagCompound();
            applyToTag(subtag, serializedMap);
            copiedValue = subtag;
        } else {
            throw new IllegalArgumentException("Can't store objects of type " + value.getClass().getName());
        }

        return copiedValue;
    }

    /**
     * Return a raw copy of this store's data.
     *
     * @return A copy of this store's data.
     */
    public NBTTagCompound getTag()
    {
        return (NBTTagCompound)tag.clone();
    }

    /**
     * Apply a Map of data to an NBTTagCompound
     *
     * @param tag The tag for which to apply data.
     * @param data The data to apply
     */
    private static void applyToTag(NBTTagCompound tag, Map<String, Object> data) {
        if (tag == null || data == null) return;

        for (Map.Entry<String, Object> entry : data.entrySet()) {
            NBTBase copiedValue = convert(entry.getValue());
            if (copiedValue != null) {
                tag.set(entry.getKey(), copiedValue);
            } else {
                tag.remove(entry.getKey());
            }
        }
    }

    /**
     * Retrieve all keys for a tag.
     * <p>
     * This is a simple wrapper for the obfuscated c() method
     *
     * @param tag The NBTTagCompound to list keys
     * @return A Set of keys from the tag, or null on null input.
     */
    @SuppressWarnings("unchecked")
    protected static Set<String> getAllKeys(NBTTagCompound tag) {
        if (tag == null) return null;
        return tag.c();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof NBTMetadataStore && ((NBTMetadataStore) other).tag.equals(this.tag);
    }

    @Override
    public int hashCode() {
        return tag.hashCode();
    }

    @Override
    public Object clone() {
        return new NBTMetadataStore((NBTTagCompound)tag.clone());
    }

    public boolean isEmpty() {
        return tag.isEmpty();
    }
}