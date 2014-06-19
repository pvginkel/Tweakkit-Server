package com.afterkraft.metadata;

import net.minecraft.server.NBTBase;
import net.minecraft.server.NBTTagList;

/**
 * Enumerates the possible types of NBTBase variant data.
 */
public enum NBTType {
    END(0),
    BYTE(1),
    SHORT(2),
    INT(3),
    LONG(4),
    FLOAT(5),
    DOUBLE(6),
    BYTE_ARRAY(7),
    STRING(8),
    LIST(9),
    COMPOUND(10),
    INT_ARRAY(11);

    private final byte id;
    private final static NBTType[] byId = new NBTType[12];

    private NBTType(int id) {
        this.id = (byte)id;
    }

    static {
        for (NBTType nbtType : values()) {
            byId[nbtType.id] = nbtType;
        }
    }

    /**
     * Retrieve the type of a given NBTBase.
     *
     * @param tag The base tag to type check
     * @return The NBT type of "tag"
     */
    public static NBTType getType(NBTBase tag) {
        byte typeId = tag.getTypeId();
        if (typeId < 0 || typeId >= byId.length) return null;
        return byId[typeId];
    }

    /**
     * Retrieve the type of data contained in a given NBTTagList.
     *
     * @param list The list tag to check
     * @return The type of data contained in "list"
     */
    public static NBTType getListType(NBTTagList list) {
        byte typeId = (byte)list.d();
        if (typeId < 0 || typeId >= byId.length) return null;
        return byId[typeId];
    }

    /**
     * Check to see if a given tag is of a specific type
     *
     * @param tag The tag to check
     * @return True if the tag matches the given type
     */
    public boolean is(NBTBase tag) {
        return id == tag.getTypeId();
    }
}