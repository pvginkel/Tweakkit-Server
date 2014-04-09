package org.spigotmc;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.EntityHuman;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.NBTBase;
import net.minecraft.server.NBTTagCompound;
import net.minecraft.server.NBTTagList;
import net.minecraft.server.TileEntitySkull;
import net.minecraft.util.com.mojang.authlib.GameProfile;
import org.bukkit.Bukkit;
import org.spigotmc.authlib.properties.Property;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class HeadConverter
{
    private static final Executor executor = Executors.newFixedThreadPool( 3,
            new ThreadFactoryBuilder()
                    .setNameFormat( "Head Conversion Thread - %1$d" )
                    .build()
    );
    private static boolean hasWarned = false;

    public static void convertHead(final TileEntitySkull head)
    {
        if ( head.getSkullType() != 3 )
        {
            return;
        }
        final int x = head.x;
        final int y = head.y;
        final int z = head.z;
        final String name = head.getExtraType();
        final NBTTagCompound tag = new NBTTagCompound();
        head.b( tag );
        if ( tag.hasKey( "Owner" ) && tag.getCompound( "Owner" ).hasKey( "Properties" ) )
        {
            // Validate the head
            org.spigotmc.authlib.GameProfile profile = getProfile( tag.getCompound( "Owner" ) );
            if ( MinecraftServer.getServer().newSessionService.getTextures( profile, false ).size() == 0 ) {
                tag.remove( "Owner" );
                head.a( tag );
            } else
            {
                return;
            }
        }

        executor.execute( new Runnable()
        {
            @Override
            public void run()
            {
                HttpURLConnection connection = null;
                try
                {
                    // Firstly convert name -> uuid
                    URL accountsAPI = new URL( "https://api.mojang.com/profiles/page/1" );

                    connection = (HttpURLConnection) accountsAPI.openConnection();
                    connection.setRequestProperty( "Content-Type", "application/json" );
                    connection.setRequestMethod( "POST" );
                    connection.setDoInput( true );
                    connection.setDoOutput( true );

                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write( ( "[{\"name\":\"" + name +
                            "\", \"agent\":\"minecraft\"}]" ).getBytes( Charsets.UTF_8 ) );
                    outputStream.flush();
                    outputStream.close();

                    InputStreamReader inputStreamReader = new InputStreamReader( connection.getInputStream() );
                    JsonObject response;
                    try
                    {
                        response = new JsonParser().parse( inputStreamReader )
                                .getAsJsonObject();
                    } finally
                    {
                        inputStreamReader.close();
                    }
                    if ( response.get( "size" ).getAsInt() != 1 ||
                            response.getAsJsonArray( "profiles" ).size() != 1 )
                    {
                        return;
                    }
                    String uuid = response.getAsJsonArray( "profiles" )
                            .get( 0 ).getAsJsonObject()
                            .get( "id" ).getAsString();
                    String correctedName = response.getAsJsonArray( "profiles" )
                            .get( 0 ).getAsJsonObject()
                            .get( "name" ).getAsString();

                    NBTTagCompound owner = new NBTTagCompound();
                    GameProfile gameProfile = new GameProfile( uuid, correctedName );
                    owner.setString( "Name", correctedName );
                    owner.setString( "Id", EntityHuman.a( gameProfile ).toString() );

                    NBTTagCompound properties = new NBTTagCompound();

                    // Now to lookup the textures
                    org.spigotmc.authlib.GameProfile newStyleProfile = new org.spigotmc.authlib.GameProfile(
                            EntityHuman.a( gameProfile ),
                            gameProfile.getName() );
                    MinecraftServer.getServer().newSessionService.fillProfileProperties( newStyleProfile );

                    if ( newStyleProfile.getProperties().size() < 1)
                    {
                        return;
                    }


                    for ( String key : newStyleProfile.getProperties().keys() )
                    {
                        NBTTagList propList = new NBTTagList();
                        for ( Property prop : newStyleProfile.getProperties().get( key ) )
                        {
                            NBTTagCompound nprop = new NBTTagCompound();
                            nprop.setString( "Signature", prop.getSignature() );
                            nprop.setString( "Value", prop.getValue()  );
                            propList.add( nprop );
                        }
                        properties.set( key, propList );
                    }

                    owner.set( "Properties", properties );
                    tag.set( "Owner", owner );

                    // Update the tile entity
                    MinecraftServer.getServer().processQueue.add( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            head.a( tag );
                            // Send the updated version
                            MinecraftServer.getServer().getPlayerList().sendPacketNearby(
                                    x, y, z, head.getWorld().spigotConfig.viewDistance * 16, head.getWorld().worldData.j(),
                                    head.getUpdatePacket() );
                        }
                    } );

                } catch ( MalformedURLException e )
                {
                    e.printStackTrace();
                } catch ( IOException e )
                {
                    if (!hasWarned)
                    {
                        hasWarned = true;
                        Bukkit.getLogger().warning( "Error connecting to Mojang servers, cannot convert player heads" );
                    }
                } finally
                {
                    if ( connection != null )
                    {
                        connection.disconnect();
                    }
                }
            }
        } );
    }

    private static org.spigotmc.authlib.GameProfile getProfile(NBTTagCompound owner)
    {
        org.spigotmc.authlib.GameProfile profile = new org.spigotmc.authlib.GameProfile(
                UUID.fromString( owner.getString( "Id" ) ), owner.getString( "Name" ) );

        NBTTagCompound properties = owner.getCompound( "Properties" );
        for (String key : (Set<String>) properties.c())
        {
            NBTTagList props = properties.getList( key, 10 );
            for (int i = 0; i < props.size(); i++) {
                NBTTagCompound prop = props.get( i );
                profile.getProperties().put( key, new Property( key, prop.getString( "Value" ), prop.getString( "Signature" ) ) );
            }
        }
        return profile;
    }
}
