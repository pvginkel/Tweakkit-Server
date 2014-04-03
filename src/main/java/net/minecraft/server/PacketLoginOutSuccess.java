package net.minecraft.server;

import net.minecraft.util.com.mojang.authlib.GameProfile;

import java.io.IOException;

public class PacketLoginOutSuccess extends Packet {

    private GameProfile a;

    public PacketLoginOutSuccess() {}

    public PacketLoginOutSuccess(GameProfile gameprofile) {
        this.a = gameprofile;
    }

    public void a(PacketDataSerializer packetdataserializer) throws IOException
    {
        String s = packetdataserializer.c(36);
        String s1 = packetdataserializer.c(16);

        this.a = new GameProfile(s, s1);
    }

    public void b(PacketDataSerializer packetdataserializer) throws IOException
    {
        packetdataserializer.a(this.a.getId());
        packetdataserializer.a(this.a.getName());
    }

    // Spigot start
    @Override
    public void writeSnapshot(PacketDataSerializer packetdataserializer) throws IOException
    {
        packetdataserializer.a( EntityHuman.a( this.a ).toString() );
        packetdataserializer.a( this.a.getName());
    }
    // Spigot end

    public void a(PacketLoginOutListener packetloginoutlistener) {
        packetloginoutlistener.a(this);
    }

    public boolean a() {
        return true;
    }

    public void handle(PacketListener packetlistener) {
        this.a((PacketLoginOutListener) packetlistener);
    }
}
