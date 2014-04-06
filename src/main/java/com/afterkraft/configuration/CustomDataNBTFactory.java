package com.afterkraft.configuration;

import java.util.Map;
import java.util.Set;

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

/**
 * Author: gabizou
 */
public class CustomDataNBTFactory {

    public static NBTTagCompound fromCustomToNBT(final CustomDataCompound compound) {
        if (compound == null || CustomDataCompound.getCustomDataCompoundMap(compound).isEmpty()) {
            return null;
        }
        NBTTagCompound data = new NBTTagCompound();
        final Map<String, CustomDataBase> customMap = CustomDataCompound.getCustomDataCompoundMap(compound);
        for (Map.Entry<String, CustomDataBase> entry : customMap.entrySet()) {
             data.set(entry.getKey(), fromCustomToNBT(entry.getKey(), entry.getValue()));
        }

        return data;
    }

    @SuppressWarnings("unchecked")
    public static CustomDataCompound fromNBTtoCustom(final NBTTagCompound compound) {
        if (compound == null || compound.c().isEmpty()) {
            return null;
        }
        CustomDataCompound data = new CustomDataCompound();
        final Set<String> keys = compound.c();
        for (String key : keys) {
            data.set(key, fromNBTtoCustom(key, compound.get(key)));
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private static CustomDataBase fromNBTtoCustom(final String key, final NBTBase base) {
        if (base instanceof NBTTagByte) {
            return new CustomDataByte(((NBTTagByte) base).f());
        } else if (base instanceof NBTTagShort) {
            return new CustomDataShort(((NBTTagShort) base).e());
        } else if (base instanceof NBTTagInt) {
            return new CustomDataInt(((NBTTagInt) base).d());
        } else if (base instanceof NBTTagLong) {
            return new CustomDataLong(((NBTTagLong) base).c());
        } else if (base instanceof NBTTagFloat) {
            return new CustomDataFloat(((NBTTagFloat) base).h());
        } else if (base instanceof NBTTagDouble) {
            return new CustomDataDouble(((NBTTagDouble) base).g());
        } else if (base instanceof NBTTagString) {
            return new CustomDataString(((NBTTagString) base).a_());
        } else if (base instanceof NBTTagByteArray) {
            return new CustomDataByteArray(((NBTTagByteArray) base).c());
        } else if (base instanceof NBTTagIntArray) {
            return new CustomDataIntArray(((NBTTagIntArray) base).c());
        } else if (base instanceof NBTTagList) {
            CustomDataList customList = new CustomDataList();
            for (int i = 0; i < ((NBTTagList) base).size(); i++) {
                customList.add(fromNBTtoCustom(((NBTTagList) base).get(i)));
            }
            return customList;
        } else if (base instanceof NBTTagCompound) {
            CustomDataCompound compound = new CustomDataCompound();
            final Set<String> keys = ((NBTTagCompound) base).c();
            for (String nbtKey : keys) {
                compound.set(key, fromNBTtoCustom(nbtKey, ((NBTTagCompound) base).get(nbtKey)));
            }
            return compound;
        } else {
            return null;
        }
    }

    private static NBTTagCompound fromCustomToNBT(final String key, final CustomDataBase customData) {
        NBTTagCompound data = new NBTTagCompound();
        switch (customData.getDataType()) {
            case END:
                return data;
            case BYTE:
                data.setByte(key, ((CustomDataNumber) customData).asByte());
                return data;
            case SHORT:
                data.setShort(key, ((CustomDataNumber) customData).asShort());
                return data;
            case INT:
                data.setInt(key, ((CustomDataNumber) customData).asInt());
                return data;
            case LONG:
                data.setLong(key, ((CustomDataNumber) customData).asLong());
                return data;
            case FLOAT:
                data.setFloat(key, ((CustomDataNumber) customData).asFloat());
                return data;
            case DOUBLE:
                data.setDouble(key, ((CustomDataNumber) customData).asDouble());
                return data;
            case BYTE_ARRAY:
                data.setByteArray(key, ((CustomDataByteArray) customData).getArray());
                return data;
            case STRING:
                data.setString(key, customData.asString());
                return data;
            case LIST:
                NBTTagList list = new NBTTagList();
                for (int i = 0; i < ((CustomDataList) customData).size(); i++) {
                    list.add(fromCustomToNBT(((CustomDataList) customData).get(i)));
                }
                data.set(key, list);
                return data;
            case COMPOUND:
                NBTTagCompound compound = new NBTTagCompound();
                final Map<String, CustomDataBase> customMap = CustomDataCompound.getCustomDataCompoundMap((CustomDataCompound) customData);
                for (Map.Entry<String, CustomDataBase> entry : customMap.entrySet()) {
                    compound.set(entry.getKey(), fromCustomToNBT(entry.getKey(), entry.getValue()));
                }
                data.set(key, compound);
                return data;
            case INT_ARRAY:
                data.setIntArray(key, ((CustomDataIntArray) customData).getArray());
                return data;
            default:
                return null;
        }
    }
}
