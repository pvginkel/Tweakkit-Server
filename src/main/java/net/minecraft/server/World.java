package net.minecraft.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

// CraftBukkit start
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.block.BlockState;
// CraftBukkit end

public class World implements IBlockAccess {

    public final int a = 7;
    public final int b = 11;
    public final int c = 128;
    public final int d = 127;
    public final int e = 63;
    public boolean f = false;
    public List entityList = new ArrayList();
    private List M = new ArrayList();
    private TreeSet N = new TreeSet();
    private Set O = new HashSet();
    public List h = new ArrayList();
    private List P = new ArrayList();
    private List Q = new ArrayList();
    public List players = new ArrayList();
    public List j = new ArrayList();
    private long R = 16777215L;
    public int k = 0;
    protected int l = (new Random()).nextInt();
    protected final int m = 1013904223;
    protected float n;
    protected float o;
    protected float p;
    protected float q;
    protected int r = 0;
    public int s = 0;
    public boolean suppressPhysics = false;
    private long S = System.currentTimeMillis();
    protected int u = 40;
    public int spawnMonsters;
    public Random random = new Random();
    public boolean x = false;
    public WorldProvider worldProvider; // CraftBukkit - remove final
    protected List z = new ArrayList();
    public IChunkProvider chunkProvider; // CraftBukkit - protected -> public
    protected final IDataManager B;
    public WorldData worldData; // CraftBukkit - protected -> public
    public boolean isLoading;
    private boolean T;
    public WorldMapCollection worldMaps;
    private ArrayList U = new ArrayList();
    private boolean V;
    public boolean allowMonsters = true; // CraftBukkit - private -> public
    public boolean allowAnimals = true; // CraftBukkit - private -> public
    private Set W = new HashSet();
    private int X;
    int[] H;
    private List Y;
    public boolean isStatic;
    public double J;
    public double K;
    public double L;

    public WorldChunkManager getWorldChunkManager() {
        return this.worldProvider.b;
    }

    // CraftBukkit start
    private final CraftWorld world;
    public boolean pvpMode;
    public boolean keepSpawnInMemory = true;
    public ChunkGenerator generator;
    Chunk lastChunkAccessed;
    int lastXAccessed = Integer.MIN_VALUE;
    int lastZAccessed = Integer.MIN_VALUE;
    final Object chunkLock = new Object();

    private boolean canSpawn(int x, int z) {
        if (this.generator != null) {
            return this.generator.canSpawn(this.getWorld(), x, z);
        } else {
            return this.worldProvider.canSpawn(x, z);
        }
    }

    public CraftWorld getWorld() {
        return this.world;
    }

    public CraftServer getServer() {
        return (CraftServer) Bukkit.getServer();
    }

    // CraftBukkit - changed signature
    public World(IDataManager idatamanager, String s, WorldSettings worldsettings, WorldProvider worldprovider, ChunkGenerator gen, org.bukkit.World.Environment env) {
        this.generator = gen;
        this.world = new CraftWorld((WorldServer) this, gen, env);
        // CraftBukkit end

        this.X = this.random.nextInt(12000);
        this.H = new int['\u8000'];
        this.Y = new ArrayList();
        this.isStatic = false;
        this.B = idatamanager;
        this.worldMaps = new WorldMapCollection(idatamanager);
        this.worldData = idatamanager.c();
        this.x = this.worldData == null;
        if (worldprovider != null) {
            this.worldProvider = worldprovider;
        } else if (this.worldData != null && this.worldData.h() == -1) {
            this.worldProvider = WorldProvider.byDimension(-1);
        } else {
            this.worldProvider = WorldProvider.byDimension(0);
        }

        boolean flag = false;

        if (this.worldData == null) {
            this.worldData = new WorldData(worldsettings, s);
            flag = true;
        } else {
            this.worldData.a(s);
        }

        this.worldProvider.a(this);
        this.chunkProvider = this.b();
        if (flag) {
            this.c();
        }

        this.f();
        this.x();

        this.getServer().addWorld(this.world); // CraftBukkit
    }

    protected IChunkProvider b() {
        IChunkLoader ichunkloader = this.B.a(this.worldProvider);

        return new ChunkProviderLoadOrGenerate(this, ichunkloader, this.worldProvider.getChunkProvider());
    }

    protected void c() {
        this.isLoading = true;
        WorldChunkManager worldchunkmanager = this.getWorldChunkManager();
        List list = worldchunkmanager.a();
        Random random = new Random(this.getSeed());
        ChunkPosition chunkposition = worldchunkmanager.a(0, 0, 256, list, random);
        int i = 0;
        byte b0 = 64;
        int j = 0;

        // CraftBukkit start
        if (this.generator != null) {
            Random rand = new Random(this.getSeed());
            Location spawn = this.generator.getFixedSpawnLocation(((WorldServer) this).getWorld(), rand);

            if (spawn != null) {
                if (spawn.getWorld() != ((WorldServer) this).getWorld()) {
                    throw new IllegalStateException("Cannot set spawn point for " + this.worldData.name + " to be in another world (" + spawn.getWorld().getName() + ")");
                } else {
                    this.worldData.setSpawn(spawn.getBlockX(), spawn.getBlockY(), spawn.getBlockZ());
                    this.isLoading = false;
                    return;
                }
            }
        }
        // CraftBukkit end

        if (chunkposition != null) {
            i = chunkposition.x;
            j = chunkposition.z;
        } else {
            System.out.println("Unable to find spawn biome");
        }

        int k = 0;

        // CraftBukkit - use out own canSpawn
        while (!canSpawn(i, j)) {
            i += random.nextInt(64) - random.nextInt(64);
            j += random.nextInt(64) - random.nextInt(64);
            ++k;
            if (k == 1000) {
                break;
            }
        }

        this.worldData.setSpawn(i, b0, j);
        this.isLoading = false;
    }

    public int a(int i, int j) {
        int k;

        for (k = 63; !this.isEmpty(i, k + 1, j); ++k) {
            ;
        }

        return this.getTypeId(i, k, j);
    }

    public void save(boolean flag, IProgressUpdate iprogressupdate) {
        if (this.chunkProvider.canSave()) {
            if (iprogressupdate != null) {
                iprogressupdate.a("Saving level");
            }

            this.w();
            if (iprogressupdate != null) {
                iprogressupdate.b("Saving chunks");
            }

            this.chunkProvider.saveChunks(flag, iprogressupdate);
        }
    }

    private void w() {
        this.j();
        this.B.a(this.worldData, this.players);
        this.worldMaps.a();
    }

    public int getTypeId(int i, int j, int k) {
        return i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000 ? (j < 0 ? 0 : (j >= 128 ? 0 : this.getChunkAt(i >> 4, k >> 4).getTypeId(i & 15, j, k & 15))) : 0;
    }

    public boolean isEmpty(int i, int j, int k) {
        return this.getTypeId(i, j, k) == 0;
    }

    public boolean isLoaded(int i, int j, int k) {
        return j >= 0 && j < 128 ? this.isChunkLoaded(i >> 4, k >> 4) : false;
    }

    public boolean areChunksLoaded(int i, int j, int k, int l) {
        return this.a(i - l, j - l, k - l, i + l, j + l, k + l);
    }

    public boolean a(int i, int j, int k, int l, int i1, int j1) {
        if (i1 >= 0 && j < 128) {
            i >>= 4;
            j >>= 4;
            k >>= 4;
            l >>= 4;
            i1 >>= 4;
            j1 >>= 4;

            for (int k1 = i; k1 <= l; ++k1) {
                for (int l1 = k; l1 <= j1; ++l1) {
                    if (!this.isChunkLoaded(k1, l1)) {
                        return false;
                    }
                }
            }

            return true;
        } else {
            return false;
        }
    }

    private boolean isChunkLoaded(int i, int j) {
        return this.chunkProvider.isChunkLoaded(i, j);
    }

    public Chunk getChunkAtWorldCoords(int i, int j) {
        return this.getChunkAt(i >> 4, j >> 4);
    }

    // CraftBukkit start
    public Chunk getChunkAt(int i, int j) {
        Chunk result = null;
        synchronized (this.chunkLock) {
            if (this.lastChunkAccessed == null || this.lastXAccessed != i || this.lastZAccessed != j) {
                this.lastXAccessed = i;
                this.lastZAccessed = j;
                this.lastChunkAccessed = this.chunkProvider.getOrCreateChunk(i, j);
            }
            result = this.lastChunkAccessed;
        }
        return result;
    }
    // CraftBukkit end

    public boolean setRawTypeIdAndData(int i, int j, int k, int l, int i1) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (j < 0) {
                return false;
            } else if (j >= 128) {
                return false;
            } else {
                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);
                boolean flag = chunk.a(i & 15, j, k & 15, l, i1);

                this.p(i, j, k);
                return flag;
            }
        } else {
            return false;
        }
    }

    public boolean setRawTypeId(int i, int j, int k, int l) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (j < 0) {
                return false;
            } else if (j >= 128) {
                return false;
            } else {
                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);
                boolean flag = chunk.a(i & 15, j, k & 15, l);

                this.p(i, j, k);
                return flag;
            }
        } else {
            return false;
        }
    }

    public Material getMaterial(int i, int j, int k) {
        int l = this.getTypeId(i, j, k);

        return l == 0 ? Material.AIR : Block.byId[l].material;
    }

    public int getData(int i, int j, int k) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (j < 0) {
                return 0;
            } else if (j >= 128) {
                return 0;
            } else {
                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                i &= 15;
                k &= 15;
                return chunk.getData(i, j, k);
            }
        } else {
            return 0;
        }
    }

    public void setData(int i, int j, int k, int l) {
        if (this.setRawData(i, j, k, l)) {
            int i1 = this.getTypeId(i, j, k);

            if (Block.t[i1 & 255]) {
                this.update(i, j, k, i1);
            } else {
                this.applyPhysics(i, j, k, i1);
            }
        }
    }

    public boolean setRawData(int i, int j, int k, int l) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (j < 0) {
                return false;
            } else if (j >= 128) {
                return false;
            } else {
                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                i &= 15;
                k &= 15;
                chunk.b(i, j, k, l);
                return true;
            }
        } else {
            return false;
        }
    }

    public boolean setTypeId(int i, int j, int k, int l) {
        // CraftBukkit start
        int old = this.getTypeId(i, j, k);
        if (this.setRawTypeId(i, j, k, l)) {
            this.update(i, j, k, l == 0 ? old : l);
            return true;
        } else {
            return false;
        }
        // CraftBukkit end
    }

    public boolean setTypeIdAndData(int i, int j, int k, int l, int i1) {
        // CraftBukkit start
        int old = this.getTypeId(i, j, k);
        if (this.setRawTypeIdAndData(i, j, k, l, i1)) {
            this.update(i, j, k, l == 0 ? old : l);
            return true;
        } else {
            return false;
        }
        // CraftBukkit end
    }

    public void notify(int i, int j, int k) {
        for (int l = 0; l < this.z.size(); ++l) {
            ((IWorldAccess) this.z.get(l)).a(i, j, k);
        }
    }

    protected void update(int i, int j, int k, int l) {
        this.notify(i, j, k);
        this.applyPhysics(i, j, k, l);
    }

    public void g(int i, int j, int k, int l) {
        int i1;

        if (k > l) {
            i1 = l;
            l = k;
            k = i1;
        }

        for (i1 = k; i1 <= l; ++i1) {
            this.b(EnumSkyBlock.SKY, i, i1, j);
        }

        this.b(i, k, j, i, l, j);
    }

    public void i(int i, int j, int k) {
        for (int l = 0; l < this.z.size(); ++l) {
            ((IWorldAccess) this.z.get(l)).a(i, j, k, i, j, k);
        }
    }

    public void b(int i, int j, int k, int l, int i1, int j1) {
        for (int k1 = 0; k1 < this.z.size(); ++k1) {
            ((IWorldAccess) this.z.get(k1)).a(i, j, k, l, i1, j1);
        }
    }

    public void applyPhysics(int i, int j, int k, int l) {
        this.k(i - 1, j, k, l);
        this.k(i + 1, j, k, l);
        this.k(i, j - 1, k, l);
        this.k(i, j + 1, k, l);
        this.k(i, j, k - 1, l);
        this.k(i, j, k + 1, l);
    }

    private void k(int i, int j, int k, int l) {
        if (!this.suppressPhysics && !this.isStatic) {
            Block block = Block.byId[this.getTypeId(i, j, k)];

            if (block != null) {
                // CraftBukkit start
                CraftWorld world = ((WorldServer) this).getWorld();
                if (world != null) {
                    BlockPhysicsEvent event = new BlockPhysicsEvent(world.getBlockAt(i, j, k), l);
                    this.getServer().getPluginManager().callEvent(event);

                    if (event.isCancelled()) {
                        return;
                    }
                }
                // CraftBukkit end

                block.doPhysics(this, i, j, k, l);
            }
        }
    }

    public boolean isChunkLoaded(int i, int j, int k) {
        return this.getChunkAt(i >> 4, k >> 4).c(i & 15, j, k & 15);
    }

    public int k(int i, int j, int k) {
        if (j < 0) {
            return 0;
        } else {
            if (j >= 128) {
                j = 127;
            }

            return this.getChunkAt(i >> 4, k >> 4).c(i & 15, j, k & 15, 0);
        }
    }

    public int getLightLevel(int i, int j, int k) {
        return this.a(i, j, k, true);
    }

    public int a(int i, int j, int k, boolean flag) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (flag) {
                int l = this.getTypeId(i, j, k);

                if (l == Block.STEP.id || l == Block.SOIL.id || l == Block.COBBLESTONE_STAIRS.id || l == Block.WOOD_STAIRS.id) {
                    int i1 = this.a(i, j + 1, k, false);
                    int j1 = this.a(i + 1, j, k, false);
                    int k1 = this.a(i - 1, j, k, false);
                    int l1 = this.a(i, j, k + 1, false);
                    int i2 = this.a(i, j, k - 1, false);

                    if (j1 > i1) {
                        i1 = j1;
                    }

                    if (k1 > i1) {
                        i1 = k1;
                    }

                    if (l1 > i1) {
                        i1 = l1;
                    }

                    if (i2 > i1) {
                        i1 = i2;
                    }

                    return i1;
                }
            }

            if (j < 0) {
                return 0;
            } else {
                if (j >= 128) {
                    j = 127;
                }

                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                i &= 15;
                k &= 15;
                return chunk.c(i, j, k, this.k);
            }
        } else {
            return 15;
        }
    }

    public int getHighestBlockYAt(int i, int j) {
        if (i >= -30000000 && j >= -30000000 && i < 30000000 && j < 30000000) {
            if (!this.isChunkLoaded(i >> 4, j >> 4)) {
                return 0;
            } else {
                Chunk chunk = this.getChunkAt(i >> 4, j >> 4);

                return chunk.b(i & 15, j & 15);
            }
        } else {
            return 0;
        }
    }

    public int a(EnumSkyBlock enumskyblock, int i, int j, int k) {
        if (j < 0) {
            j = 0;
        }

        if (j >= 128) {
            j = 127;
        }

        if (j >= 0 && j < 128 && i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            int l = i >> 4;
            int i1 = k >> 4;

            if (!this.isChunkLoaded(l, i1)) {
                return 0;
            } else {
                Chunk chunk = this.getChunkAt(l, i1);

                return chunk.a(enumskyblock, i & 15, j, k & 15);
            }
        } else {
            return enumskyblock.c;
        }
    }

    public void a(EnumSkyBlock enumskyblock, int i, int j, int k, int l) {
        if (i >= -30000000 && k >= -30000000 && i < 30000000 && k < 30000000) {
            if (j >= 0) {
                if (j < 128) {
                    if (this.isChunkLoaded(i >> 4, k >> 4)) {
                        Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                        chunk.a(enumskyblock, i & 15, j, k & 15, l);

                        for (int i1 = 0; i1 < this.z.size(); ++i1) {
                            ((IWorldAccess) this.z.get(i1)).a(i, j, k);
                        }
                    }
                }
            }
        }
    }

    public float m(int i, int j, int k) {
        return this.worldProvider.f[this.getLightLevel(i, j, k)];
    }

    public boolean d() {
        return this.k < 4;
    }

    public MovingObjectPosition a(Vec3D vec3d, Vec3D vec3d1) {
        return this.rayTrace(vec3d, vec3d1, false, false);
    }

    public MovingObjectPosition rayTrace(Vec3D vec3d, Vec3D vec3d1, boolean flag) {
        return this.rayTrace(vec3d, vec3d1, flag, false);
    }

    public MovingObjectPosition rayTrace(Vec3D vec3d, Vec3D vec3d1, boolean flag, boolean flag1) {
        if (!Double.isNaN(vec3d.a) && !Double.isNaN(vec3d.b) && !Double.isNaN(vec3d.c)) {
            if (!Double.isNaN(vec3d1.a) && !Double.isNaN(vec3d1.b) && !Double.isNaN(vec3d1.c)) {
                int i = MathHelper.floor(vec3d1.a);
                int j = MathHelper.floor(vec3d1.b);
                int k = MathHelper.floor(vec3d1.c);
                int l = MathHelper.floor(vec3d.a);
                int i1 = MathHelper.floor(vec3d.b);
                int j1 = MathHelper.floor(vec3d.c);
                int k1 = this.getTypeId(l, i1, j1);
                int l1 = this.getData(l, i1, j1);
                Block block = Block.byId[k1];

                if ((!flag1 || block == null || block.e(this, l, i1, j1) != null) && k1 > 0 && block.a(l1, flag)) {
                    MovingObjectPosition movingobjectposition = block.a(this, l, i1, j1, vec3d, vec3d1);

                    if (movingobjectposition != null) {
                        return movingobjectposition;
                    }
                }

                k1 = 200;

                while (k1-- >= 0) {
                    if (Double.isNaN(vec3d.a) || Double.isNaN(vec3d.b) || Double.isNaN(vec3d.c)) {
                        return null;
                    }

                    if (l == i && i1 == j && j1 == k) {
                        return null;
                    }

                    boolean flag2 = true;
                    boolean flag3 = true;
                    boolean flag4 = true;
                    double d0 = 999.0D;
                    double d1 = 999.0D;
                    double d2 = 999.0D;

                    if (i > l) {
                        d0 = (double) l + 1.0D;
                    } else if (i < l) {
                        d0 = (double) l + 0.0D;
                    } else {
                        flag2 = false;
                    }

                    if (j > i1) {
                        d1 = (double) i1 + 1.0D;
                    } else if (j < i1) {
                        d1 = (double) i1 + 0.0D;
                    } else {
                        flag3 = false;
                    }

                    if (k > j1) {
                        d2 = (double) j1 + 1.0D;
                    } else if (k < j1) {
                        d2 = (double) j1 + 0.0D;
                    } else {
                        flag4 = false;
                    }

                    double d3 = 999.0D;
                    double d4 = 999.0D;
                    double d5 = 999.0D;
                    double d6 = vec3d1.a - vec3d.a;
                    double d7 = vec3d1.b - vec3d.b;
                    double d8 = vec3d1.c - vec3d.c;

                    if (flag2) {
                        d3 = (d0 - vec3d.a) / d6;
                    }

                    if (flag3) {
                        d4 = (d1 - vec3d.b) / d7;
                    }

                    if (flag4) {
                        d5 = (d2 - vec3d.c) / d8;
                    }

                    boolean flag5 = false;
                    byte b0;

                    if (d3 < d4 && d3 < d5) {
                        if (i > l) {
                            b0 = 4;
                        } else {
                            b0 = 5;
                        }

                        vec3d.a = d0;
                        vec3d.b += d7 * d3;
                        vec3d.c += d8 * d3;
                    } else if (d4 < d5) {
                        if (j > i1) {
                            b0 = 0;
                        } else {
                            b0 = 1;
                        }

                        vec3d.a += d6 * d4;
                        vec3d.b = d1;
                        vec3d.c += d8 * d4;
                    } else {
                        if (k > j1) {
                            b0 = 2;
                        } else {
                            b0 = 3;
                        }

                        vec3d.a += d6 * d5;
                        vec3d.b += d7 * d5;
                        vec3d.c = d2;
                    }

                    Vec3D vec3d2 = Vec3D.create(vec3d.a, vec3d.b, vec3d.c);

                    l = (int) (vec3d2.a = (double) MathHelper.floor(vec3d.a));
                    if (b0 == 5) {
                        --l;
                        ++vec3d2.a;
                    }

                    i1 = (int) (vec3d2.b = (double) MathHelper.floor(vec3d.b));
                    if (b0 == 1) {
                        --i1;
                        ++vec3d2.b;
                    }

                    j1 = (int) (vec3d2.c = (double) MathHelper.floor(vec3d.c));
                    if (b0 == 3) {
                        --j1;
                        ++vec3d2.c;
                    }

                    int i2 = this.getTypeId(l, i1, j1);
                    int j2 = this.getData(l, i1, j1);
                    Block block1 = Block.byId[i2];

                    if ((!flag1 || block1 == null || block1.e(this, l, i1, j1) != null) && i2 > 0 && block1.a(j2, flag)) {
                        MovingObjectPosition movingobjectposition1 = block1.a(this, l, i1, j1, vec3d, vec3d1);

                        if (movingobjectposition1 != null) {
                            return movingobjectposition1;
                        }
                    }
                }

                return null;
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    public void makeSound(Entity entity, String s, float f, float f1) {
        for (int i = 0; i < this.z.size(); ++i) {
            ((IWorldAccess) this.z.get(i)).a(s, entity.locX, entity.locY - (double) entity.height, entity.locZ, f, f1);
        }
    }

    public void makeSound(double d0, double d1, double d2, String s, float f, float f1) {
        for (int i = 0; i < this.z.size(); ++i) {
            ((IWorldAccess) this.z.get(i)).a(s, d0, d1, d2, f, f1);
        }
    }

    public void a(String s, int i, int j, int k) {
        for (int l = 0; l < this.z.size(); ++l) {
            ((IWorldAccess) this.z.get(l)).a(s, i, j, k);
        }
    }

    public void a(String s, double d0, double d1, double d2, double d3, double d4, double d5) {
        for (int i = 0; i < this.z.size(); ++i) {
            ((IWorldAccess) this.z.get(i)).a(s, d0, d1, d2, d3, d4, d5);
        }
    }

    public boolean strikeLightning(Entity entity) {
        this.j.add(entity);
        return true;
    }

    // CraftBukkit start - used for entities other than creatures
    public boolean addEntity(Entity entity) {
        return this.addEntity(entity, SpawnReason.CUSTOM); // Set reason as Custom by default
    }


    public boolean addEntity(Entity entity, SpawnReason spawnReason) { // Changed signature, added SpawnReason
    // CraftBukkit end
        int i = MathHelper.floor(entity.locX / 16.0D);
        int j = MathHelper.floor(entity.locZ / 16.0D);
        boolean flag = false;

        if (entity instanceof EntityHuman) {
            flag = true;
        }

        // CraftBukkit start
        if (entity instanceof EntityLiving && !(entity instanceof EntityPlayer)) {
            CreatureSpawnEvent event = CraftEventFactory.callCreatureSpawnEvent((EntityLiving) entity, spawnReason);

            if (event.isCancelled()) {
                return false;
            }
        } else if (entity instanceof EntityItem) {
            ItemSpawnEvent event = CraftEventFactory.callItemSpawnEvent((EntityItem) entity);
            if (event.isCancelled()) {
                return false;
            }
        }
        // CraftBukkit end

        if (!flag && !this.isChunkLoaded(i, j)) {
            return false;
        } else {
            if (entity instanceof EntityHuman) {
                EntityHuman entityhuman = (EntityHuman) entity;

                this.players.add(entityhuman);
                this.everyoneSleeping();
            }

            this.getChunkAt(i, j).a(entity);
            this.entityList.add(entity);
            this.c(entity);
            return true;
        }
    }

    protected void c(Entity entity) {
        for (int i = 0; i < this.z.size(); ++i) {
            ((IWorldAccess) this.z.get(i)).a(entity);
        }
    }

    protected void d(Entity entity) {
        for (int i = 0; i < this.z.size(); ++i) {
            ((IWorldAccess) this.z.get(i)).b(entity);
        }
    }

    public void kill(Entity entity) {
        if (entity.passenger != null) {
            entity.passenger.mount((Entity) null);
        }

        if (entity.vehicle != null) {
            entity.mount((Entity) null);
        }

        entity.die();
        if (entity instanceof EntityHuman) {
            this.players.remove((EntityHuman) entity);
            this.everyoneSleeping();
        }
    }

    public void removeEntity(Entity entity) {
        entity.die();
        if (entity instanceof EntityHuman) {
            this.players.remove((EntityHuman) entity);
            this.everyoneSleeping();
        }

        int i = entity.bW;
        int j = entity.bY;

        if (entity.bV && this.isChunkLoaded(i, j)) {
            this.getChunkAt(i, j).b(entity);
        }

        this.entityList.remove(entity);
        this.d(entity);
    }

    public void addIWorldAccess(IWorldAccess iworldaccess) {
        this.z.add(iworldaccess);
    }

    public List getEntities(Entity entity, AxisAlignedBB axisalignedbb) {
        this.U.clear();
        int i = MathHelper.floor(axisalignedbb.a);
        int j = MathHelper.floor(axisalignedbb.d + 1.0D);
        int k = MathHelper.floor(axisalignedbb.b);
        int l = MathHelper.floor(axisalignedbb.e + 1.0D);
        int i1 = MathHelper.floor(axisalignedbb.c);
        int j1 = MathHelper.floor(axisalignedbb.f + 1.0D);

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = i1; l1 < j1; ++l1) {
                if (this.isLoaded(k1, 64, l1)) {
                    for (int i2 = k - 1; i2 < l; ++i2) {
                        Block block = Block.byId[this.getTypeId(k1, i2, l1)];

                        if (block != null) {
                            block.a(this, k1, i2, l1, axisalignedbb, this.U);
                        }
                    }
                }
            }
        }

        double d0 = 0.25D;
        List list = this.b(entity, axisalignedbb.b(d0, d0, d0));

        for (int j2 = 0; j2 < list.size(); ++j2) {
            AxisAlignedBB axisalignedbb1 = ((Entity) list.get(j2)).f();

            if (axisalignedbb1 != null && axisalignedbb1.a(axisalignedbb)) {
                this.U.add(axisalignedbb1);
            }

            axisalignedbb1 = entity.b((Entity) list.get(j2));
            if (axisalignedbb1 != null && axisalignedbb1.a(axisalignedbb)) {
                this.U.add(axisalignedbb1);
            }
        }

        return this.U;
    }

    public int a(float f) {
        float f1 = this.b(f);
        float f2 = 1.0F - (MathHelper.cos(f1 * 3.1415927F * 2.0F) * 2.0F + 0.5F);

        if (f2 < 0.0F) {
            f2 = 0.0F;
        }

        if (f2 > 1.0F) {
            f2 = 1.0F;
        }

        f2 = 1.0F - f2;
        f2 = (float) ((double) f2 * (1.0D - (double) (this.d(f) * 5.0F) / 16.0D));
        f2 = (float) ((double) f2 * (1.0D - (double) (this.c(f) * 5.0F) / 16.0D));
        f2 = 1.0F - f2;
        return (int) (f2 * 11.0F);
    }

    public float b(float f) {
        return this.worldProvider.a(this.worldData.f(), f) + (float) (this.J + (this.K - this.J) * (double) f);
    }

    public int e(int i, int j) {
        return this.getChunkAtWorldCoords(i, j).c(i & 15, j & 15);
    }

    public int f(int i, int j) {
        Chunk chunk = this.getChunkAtWorldCoords(i, j);
        int k = 127;

        i &= 15;

        for (j &= 15; k > 0; --k) {
            int l = chunk.getTypeId(i, k, j);

            if (l != 0 && Block.byId[l].material.isSolid() && Block.byId[l].material != Material.LEAVES) {
                return k + 1;
            }
        }

        return -1;
    }

    public void c(int i, int j, int k, int l, int i1) {
        NextTickListEntry nextticklistentry = new NextTickListEntry(i, j, k, l);
        byte b0 = 8;

        if (this.f) {
            if (this.a(nextticklistentry.a - b0, nextticklistentry.b - b0, nextticklistentry.c - b0, nextticklistentry.a + b0, nextticklistentry.b + b0, nextticklistentry.c + b0)) {
                int j1 = this.getTypeId(nextticklistentry.a, nextticklistentry.b, nextticklistentry.c);

                if (j1 == nextticklistentry.d && j1 > 0) {
                    Block.byId[j1].a(this, nextticklistentry.a, nextticklistentry.b, nextticklistentry.c, this.random);
                }
            }
        } else {
            if (this.a(i - b0, j - b0, k - b0, i + b0, j + b0, k + b0)) {
                if (l > 0) {
                    nextticklistentry.a((long) i1 + this.worldData.f());
                }

                if (!this.O.contains(nextticklistentry)) {
                    this.O.add(nextticklistentry);
                    this.N.add(nextticklistentry);
                }
            }
        }
    }

    public void cleanUp() {
        int i;
        Entity entity;

        for (i = 0; i < this.j.size(); ++i) {
            entity = (Entity) this.j.get(i);
            // CraftBukkit start - fixed an NPE
            if (entity == null) {
                continue;
            }
            // CraftBukkit end
            entity.s_();
            if (entity.dead) {
                this.j.remove(i--);
            }
        }

        this.entityList.removeAll(this.M);

        int j;
        int k;

        for (i = 0; i < this.M.size(); ++i) {
            entity = (Entity) this.M.get(i);
            j = entity.bW;
            k = entity.bY;
            if (entity.bV && this.isChunkLoaded(j, k)) {
                this.getChunkAt(j, k).b(entity);
            }
        }

        for (i = 0; i < this.M.size(); ++i) {
            this.d((Entity) this.M.get(i));
        }

        this.M.clear();

        for (i = 0; i < this.entityList.size(); ++i) {
            entity = (Entity) this.entityList.get(i);
            if (entity.vehicle != null) {
                if (!entity.vehicle.dead && entity.vehicle.passenger == entity) {
                    continue;
                }

                entity.vehicle.passenger = null;
                entity.vehicle = null;
            }

            if (!entity.dead) {
                this.playerJoinedWorld(entity);
            }

            if (entity.dead) {
                j = entity.bW;
                k = entity.bY;
                if (entity.bV && this.isChunkLoaded(j, k)) {
                    this.getChunkAt(j, k).b(entity);
                }

                this.entityList.remove(i--);
                this.d(entity);
            }
        }

        this.V = true;
        Iterator iterator = this.h.iterator();

        while (iterator.hasNext()) {
            TileEntity tileentity = (TileEntity) iterator.next();

            if (!tileentity.m() && tileentity.world != null) {
                tileentity.h_();
            }

            if (tileentity.m()) {
                iterator.remove();
                if (this.isChunkLoaded(tileentity.x >> 4, tileentity.z >> 4)) {
                    Chunk chunk = this.getChunkAt(tileentity.x >> 4, tileentity.z >> 4);

                    if (chunk != null) {
                        chunk.e(tileentity.x & 15, tileentity.y, tileentity.z & 15);
                    }
                }
            }
        }

        this.V = false;
        if (!this.Q.isEmpty()) {
            this.h.removeAll(this.Q);
            this.Q.clear();
        }

        if (!this.P.isEmpty()) {
            Iterator iterator1 = this.P.iterator();

            while (iterator1.hasNext()) {
                TileEntity tileentity1 = (TileEntity) iterator1.next();

                if (!tileentity1.m()) {
                    // CraftBukkit - order matters, moved down
                    /* if (!this.h.contains(tileentity1)) {
                        this.h.add(tileentity1);
                    } */

                    if (this.isChunkLoaded(tileentity1.x >> 4, tileentity1.z >> 4)) {
                        Chunk chunk1 = this.getChunkAt(tileentity1.x >> 4, tileentity1.z >> 4);

                        if (chunk1 != null) {
                            chunk1.a(tileentity1.x & 15, tileentity1.y, tileentity1.z & 15, tileentity1);
                            // CraftBukkit start - moved in from above
                            if (!this.h.contains(tileentity1)) {
                                this.h.add(tileentity1);
                            }
                            // CraftBukkit end
                        }
                    }

                    this.notify(tileentity1.x, tileentity1.y, tileentity1.z);
                }
            }

            this.P.clear();
        }
    }

    public void a(Collection collection) {
        if (this.V) {
            this.P.addAll(collection);
        } else {
            this.h.addAll(collection);
        }
    }

    public void playerJoinedWorld(Entity entity) {
        this.entityJoinedWorld(entity, true);
    }

    public void entityJoinedWorld(Entity entity, boolean flag) {
        int i = MathHelper.floor(entity.locX);
        int j = MathHelper.floor(entity.locZ);
        byte b0 = 32;

        if (!flag || this.a(i - b0, 0, j - b0, i + b0, 128, j + b0)) {
            entity.bE = entity.locX;
            entity.bF = entity.locY;
            entity.bG = entity.locZ;
            entity.lastYaw = entity.yaw;
            entity.lastPitch = entity.pitch;
            if (flag && entity.bV) {
                if (entity.vehicle != null) {
                    entity.I();
                } else {
                    entity.s_();
                }
            }

            if (Double.isNaN(entity.locX) || Double.isInfinite(entity.locX)) {
                entity.locX = entity.bE;
            }

            if (Double.isNaN(entity.locY) || Double.isInfinite(entity.locY)) {
                entity.locY = entity.bF;
            }

            if (Double.isNaN(entity.locZ) || Double.isInfinite(entity.locZ)) {
                entity.locZ = entity.bG;
            }

            if (Double.isNaN((double) entity.pitch) || Double.isInfinite((double) entity.pitch)) {
                entity.pitch = entity.lastPitch;
            }

            if (Double.isNaN((double) entity.yaw) || Double.isInfinite((double) entity.yaw)) {
                entity.yaw = entity.lastYaw;
            }

            int k = MathHelper.floor(entity.locX / 16.0D);
            int l = MathHelper.floor(entity.locY / 16.0D);
            int i1 = MathHelper.floor(entity.locZ / 16.0D);

            if (!entity.bV || entity.bW != k || entity.bX != l || entity.bY != i1) {
                if (entity.bV && this.isChunkLoaded(entity.bW, entity.bY)) {
                    this.getChunkAt(entity.bW, entity.bY).a(entity, entity.bX);
                }

                if (this.isChunkLoaded(k, i1)) {
                    entity.bV = true;
                    this.getChunkAt(k, i1).a(entity);
                } else {
                    entity.bV = false;
                }
            }

            if (flag && entity.bV && entity.passenger != null) {
                if (!entity.passenger.dead && entity.passenger.vehicle == entity) {
                    this.playerJoinedWorld(entity.passenger);
                } else {
                    entity.passenger.vehicle = null;
                    entity.passenger = null;
                }
            }
        }
    }

    public boolean containsEntity(AxisAlignedBB axisalignedbb) {
        List list = this.b((Entity) null, axisalignedbb);

        for (int i = 0; i < list.size(); ++i) {
            Entity entity = (Entity) list.get(i);

            if (!entity.dead && entity.aY) {
                return false;
            }
        }

        return true;
    }

    public boolean b(AxisAlignedBB axisalignedbb) {
        int i = MathHelper.floor(axisalignedbb.a);
        int j = MathHelper.floor(axisalignedbb.d + 1.0D);
        int k = MathHelper.floor(axisalignedbb.b);
        int l = MathHelper.floor(axisalignedbb.e + 1.0D);
        int i1 = MathHelper.floor(axisalignedbb.c);
        int j1 = MathHelper.floor(axisalignedbb.f + 1.0D);

        if (axisalignedbb.a < 0.0D) {
            --i;
        }

        if (axisalignedbb.b < 0.0D) {
            --k;
        }

        if (axisalignedbb.c < 0.0D) {
            --i1;
        }

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    Block block = Block.byId[this.getTypeId(k1, l1, i2)];

                    if (block != null) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean c(AxisAlignedBB axisalignedbb) {
        int i = MathHelper.floor(axisalignedbb.a);
        int j = MathHelper.floor(axisalignedbb.d + 1.0D);
        int k = MathHelper.floor(axisalignedbb.b);
        int l = MathHelper.floor(axisalignedbb.e + 1.0D);
        int i1 = MathHelper.floor(axisalignedbb.c);
        int j1 = MathHelper.floor(axisalignedbb.f + 1.0D);

        if (axisalignedbb.a < 0.0D) {
            --i;
        }

        if (axisalignedbb.b < 0.0D) {
            --k;
        }

        if (axisalignedbb.c < 0.0D) {
            --i1;
        }

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    Block block = Block.byId[this.getTypeId(k1, l1, i2)];

                    if (block != null && block.material.isLiquid()) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean d(AxisAlignedBB axisalignedbb) {
        int i = MathHelper.floor(axisalignedbb.a);
        int j = MathHelper.floor(axisalignedbb.d + 1.0D);
        int k = MathHelper.floor(axisalignedbb.b);
        int l = MathHelper.floor(axisalignedbb.e + 1.0D);
        int i1 = MathHelper.floor(axisalignedbb.c);
        int j1 = MathHelper.floor(axisalignedbb.f + 1.0D);

        if (this.a(i, k, i1, j, l, j1)) {
            for (int k1 = i; k1 < j; ++k1) {
                for (int l1 = k; l1 < l; ++l1) {
                    for (int i2 = i1; i2 < j1; ++i2) {
                        int j2 = this.getTypeId(k1, l1, i2);

                        if (j2 == Block.FIRE.id || j2 == Block.LAVA.id || j2 == Block.STATIONARY_LAVA.id) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public boolean a(AxisAlignedBB axisalignedbb, Material material, Entity entity) {
        int i = MathHelper.floor(axisalignedbb.a);
        int j = MathHelper.floor(axisalignedbb.d + 1.0D);
        int k = MathHelper.floor(axisalignedbb.b);
        int l = MathHelper.floor(axisalignedbb.e + 1.0D);
        int i1 = MathHelper.floor(axisalignedbb.c);
        int j1 = MathHelper.floor(axisalignedbb.f + 1.0D);

        if (!this.a(i, k, i1, j, l, j1)) {
            return false;
        } else {
            boolean flag = false;
            Vec3D vec3d = Vec3D.create(0.0D, 0.0D, 0.0D);

            for (int k1 = i; k1 < j; ++k1) {
                for (int l1 = k; l1 < l; ++l1) {
                    for (int i2 = i1; i2 < j1; ++i2) {
                        Block block = Block.byId[this.getTypeId(k1, l1, i2)];

                        if (block != null && block.material == material) {
                            double d0 = (double) ((float) (l1 + 1) - BlockFluids.c(this.getData(k1, l1, i2)));

                            if ((double) l >= d0) {
                                flag = true;
                                block.a(this, k1, l1, i2, entity, vec3d);
                            }
                        }
                    }
                }
            }

            if (vec3d.c() > 0.0D) {
                vec3d = vec3d.b();
                double d1 = 0.014D;

                entity.motX += vec3d.a * d1;
                entity.motY += vec3d.b * d1;
                entity.motZ += vec3d.c * d1;
            }

            return flag;
        }
    }

    public boolean a(AxisAlignedBB axisalignedbb, Material material) {
        int i = MathHelper.floor(axisalignedbb.a);
        int j = MathHelper.floor(axisalignedbb.d + 1.0D);
        int k = MathHelper.floor(axisalignedbb.b);
        int l = MathHelper.floor(axisalignedbb.e + 1.0D);
        int i1 = MathHelper.floor(axisalignedbb.c);
        int j1 = MathHelper.floor(axisalignedbb.f + 1.0D);

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    Block block = Block.byId[this.getTypeId(k1, l1, i2)];

                    if (block != null && block.material == material) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public boolean b(AxisAlignedBB axisalignedbb, Material material) {
        int i = MathHelper.floor(axisalignedbb.a);
        int j = MathHelper.floor(axisalignedbb.d + 1.0D);
        int k = MathHelper.floor(axisalignedbb.b);
        int l = MathHelper.floor(axisalignedbb.e + 1.0D);
        int i1 = MathHelper.floor(axisalignedbb.c);
        int j1 = MathHelper.floor(axisalignedbb.f + 1.0D);

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    Block block = Block.byId[this.getTypeId(k1, l1, i2)];

                    if (block != null && block.material == material) {
                        int j2 = this.getData(k1, l1, i2);
                        double d0 = (double) (l1 + 1);

                        if (j2 < 8) {
                            d0 = (double) (l1 + 1) - (double) j2 / 8.0D;
                        }

                        if (d0 >= axisalignedbb.b) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    public Explosion a(Entity entity, double d0, double d1, double d2, float f) {
        return this.createExplosion(entity, d0, d1, d2, f, false);
    }

    public Explosion createExplosion(Entity entity, double d0, double d1, double d2, float f, boolean flag) {
        Explosion explosion = new Explosion(this, entity, d0, d1, d2, f);

        explosion.a = flag;
        explosion.a();
        explosion.a(true);
        return explosion;
    }

    public float a(Vec3D vec3d, AxisAlignedBB axisalignedbb) {
        double d0 = 1.0D / ((axisalignedbb.d - axisalignedbb.a) * 2.0D + 1.0D);
        double d1 = 1.0D / ((axisalignedbb.e - axisalignedbb.b) * 2.0D + 1.0D);
        double d2 = 1.0D / ((axisalignedbb.f - axisalignedbb.c) * 2.0D + 1.0D);
        int i = 0;
        int j = 0;

        for (float f = 0.0F; f <= 1.0F; f = (float) ((double) f + d0)) {
            for (float f1 = 0.0F; f1 <= 1.0F; f1 = (float) ((double) f1 + d1)) {
                for (float f2 = 0.0F; f2 <= 1.0F; f2 = (float) ((double) f2 + d2)) {
                    double d3 = axisalignedbb.a + (axisalignedbb.d - axisalignedbb.a) * (double) f;
                    double d4 = axisalignedbb.b + (axisalignedbb.e - axisalignedbb.b) * (double) f1;
                    double d5 = axisalignedbb.c + (axisalignedbb.f - axisalignedbb.c) * (double) f2;

                    if (this.a(Vec3D.create(d3, d4, d5), vec3d) == null) {
                        ++i;
                    }

                    ++j;
                }
            }
        }

        return (float) i / (float) j;
    }

    public void douseFire(EntityHuman entityhuman, int i, int j, int k, int l) {
        if (l == 0) {
            --j;
        }

        if (l == 1) {
            ++j;
        }

        if (l == 2) {
            --k;
        }

        if (l == 3) {
            ++k;
        }

        if (l == 4) {
            --i;
        }

        if (l == 5) {
            ++i;
        }

        if (this.getTypeId(i, j, k) == Block.FIRE.id) {
            this.a(entityhuman, 1004, i, j, k, 0);
            this.setTypeId(i, j, k, 0);
        }
    }

    public TileEntity getTileEntity(int i, int j, int k) {
        Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

        return chunk != null ? chunk.d(i & 15, j, k & 15) : null;
    }

    public void setTileEntity(int i, int j, int k, TileEntity tileentity) {
        if (tileentity != null && !tileentity.m()) {
            if (this.V) {
                tileentity.x = i;
                tileentity.y = j;
                tileentity.z = k;
                this.P.add(tileentity);
            } else {
                // CraftBukkit - order matters, moved down
                // this.h.add(tileentity);
                Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

                if (chunk != null) {
                    chunk.a(i & 15, j, k & 15, tileentity);
                    this.h.add(tileentity); // CraftBukkit - moved in from above
                }
            }
        }
    }

    public void n(int i, int j, int k) {
        TileEntity tileentity = this.getTileEntity(i, j, k);

        if (tileentity != null && this.V) {
            tileentity.i();
        } else {
            if (tileentity != null) {
                this.h.remove(tileentity);
            }

            Chunk chunk = this.getChunkAt(i >> 4, k >> 4);

            if (chunk != null) {
                chunk.e(i & 15, j, k & 15);
            }
        }
    }

    public void a(TileEntity tileentity) {
        this.Q.add(tileentity);
    }

    public boolean o(int i, int j, int k) {
        Block block = Block.byId[this.getTypeId(i, j, k)];

        return block == null ? false : block.a();
    }

    public boolean e(int i, int j, int k) {
        Block block = Block.byId[this.getTypeId(i, j, k)];

        return block == null ? false : block.material.j() && block.b();
    }

    public void f() {
        int i = this.a(1.0F);

        if (i != this.k) {
            this.k = i;
        }
    }

    public void setSpawnFlags(boolean flag, boolean flag1) {
        this.allowMonsters = flag;
        this.allowAnimals = flag1;
    }

    public void doTick() {
        this.J = this.K;
        this.K += this.L;
        this.L *= 0.98D;
        this.getWorldChunkManager().b();
        this.h();
        long i;

        if (this.everyoneDeeplySleeping()) {
            boolean flag = false;

            if (this.allowMonsters && this.spawnMonsters >= 1) {
                flag = SpawnerCreature.a(this, this.players);
            }

            if (!flag) {
                i = this.worldData.f() + 24000L;
                this.worldData.a(i - i % 24000L);
                this.r();
            }
        }

        // CraftBukkit start - Only call spawner if we have players online and the world allows for mobs or animals
        if ((this.allowMonsters || this.allowAnimals) && (this instanceof WorldServer && this.getServer().getHandle().players.size() > 0)) {
            SpawnerCreature.spawnEntities(this, this.allowMonsters, this.allowAnimals && this.worldData.f() % 400L == 0L);
        }
        // CraftBukkit end

        this.chunkProvider.unloadChunks();
        int j = this.a(1.0F);

        if (j != this.k) {
            this.k = j;
        }

        i = this.worldData.f() + 1L;
        if (i % (long) this.u == 0L) {
            this.save(false, (IProgressUpdate) null);
        }

        this.worldData.a(i);
        this.a(false);
        this.i();
    }

    private void x() {
        if (this.worldData.hasStorm()) {
            this.o = 1.0F;
            if (this.worldData.isThundering()) {
                this.q = 1.0F;
            }
        }
    }

    protected void h() {
        if (!this.worldProvider.e) {
            if (this.r > 0) {
                --this.r;
            }

            int i = this.worldData.getThunderDuration();

            if (i <= 0) {
                if (this.worldData.isThundering()) {
                    this.worldData.setThunderDuration(this.random.nextInt(12000) + 3600);
                } else {
                    this.worldData.setThunderDuration(this.random.nextInt(168000) + 12000);
                }
            } else {
                --i;
                this.worldData.setThunderDuration(i);
                if (i <= 0) {
                    // CraftBukkit start
                    ThunderChangeEvent thunder = new ThunderChangeEvent(this.getWorld(), !this.worldData.isThundering());
                    this.getServer().getPluginManager().callEvent(thunder);
                    if (!thunder.isCancelled()) {
                        this.worldData.setThundering(!this.worldData.isThundering());
                    }
                    // CraftBukkit end
                }
            }

            int j = this.worldData.getWeatherDuration();

            if (j <= 0) {
                if (this.worldData.hasStorm()) {
                    this.worldData.setWeatherDuration(this.random.nextInt(12000) + 12000);
                } else {
                    this.worldData.setWeatherDuration(this.random.nextInt(168000) + 12000);
                }
            } else {
                --j;
                this.worldData.setWeatherDuration(j);
                if (j <= 0) {
                    // CraftBukkit start
                    WeatherChangeEvent weather = new WeatherChangeEvent(this.getWorld(), !this.worldData.hasStorm());
                    this.getServer().getPluginManager().callEvent(weather);

                    if (!weather.isCancelled()) {
                        this.worldData.setStorm(!this.worldData.hasStorm());
                    }
                    // CraftBukkit end
                }
            }

            this.n = this.o;
            if (this.worldData.hasStorm()) {
                this.o = (float) ((double) this.o + 0.01D);
            } else {
                this.o = (float) ((double) this.o - 0.01D);
            }

            if (this.o < 0.0F) {
                this.o = 0.0F;
            }

            if (this.o > 1.0F) {
                this.o = 1.0F;
            }

            this.p = this.q;
            if (this.worldData.isThundering()) {
                this.q = (float) ((double) this.q + 0.01D);
            } else {
                this.q = (float) ((double) this.q - 0.01D);
            }

            if (this.q < 0.0F) {
                this.q = 0.0F;
            }

            if (this.q > 1.0F) {
                this.q = 1.0F;
            }
        }
    }

    private void y() {
        // CraftBukkit start
        WeatherChangeEvent weather = new WeatherChangeEvent(this.getWorld(), false);
        this.getServer().getPluginManager().callEvent(weather);

        ThunderChangeEvent thunder = new ThunderChangeEvent(this.getWorld(), false);
        this.getServer().getPluginManager().callEvent(thunder);
        if (!weather.isCancelled()) {
            this.worldData.setWeatherDuration(0);
            this.worldData.setStorm(false);
        }
        if (!thunder.isCancelled()) {
            this.worldData.setThunderDuration(0);
            this.worldData.setThundering(false);
        }
        // CraftBukkit end
    }

    protected void i() {
        this.W.clear();

        int i;
        int j;
        int k;
        int l;

        for (int i1 = 0; i1 < this.players.size(); ++i1) {
            EntityHuman entityhuman = (EntityHuman) this.players.get(i1);

            i = MathHelper.floor(entityhuman.locX / 16.0D);
            j = MathHelper.floor(entityhuman.locZ / 16.0D);
            byte b0 = 9;

            for (k = -b0; k <= b0; ++k) {
                for (l = -b0; l <= b0; ++l) {
                    this.W.add(new ChunkCoordIntPair(k + i, l + j));
                }
            }
        }

        if (this.X > 0) {
            --this.X;
        }

        Iterator iterator = this.W.iterator();

        while (iterator.hasNext()) {
            ChunkCoordIntPair chunkcoordintpair = (ChunkCoordIntPair) iterator.next();

            i = chunkcoordintpair.x * 16;
            j = chunkcoordintpair.z * 16;
            Chunk chunk = this.getChunkAt(chunkcoordintpair.x, chunkcoordintpair.z);

            chunk.h();
            int j1;
            int k1;
            int l1;

            if (this.X == 0) {
                this.l = this.l * 3 + 1013904223;
                k = this.l >> 2;
                l = k & 15;
                j1 = k >> 8 & 15;
                k1 = k >> 16 & 127;
                l1 = chunk.getTypeId(l, k1, j1);
                l += i;
                j1 += j;
                if (l1 == 0 && this.k(l, k1, j1) <= this.random.nextInt(8) && this.a(EnumSkyBlock.SKY, l, k1, j1) <= 0) {
                    EntityHuman entityhuman1 = this.a((double) l + 0.5D, (double) k1 + 0.5D, (double) j1 + 0.5D, 8.0D);

                    if (entityhuman1 != null && entityhuman1.e((double) l + 0.5D, (double) k1 + 0.5D, (double) j1 + 0.5D) > 4.0D) {
                        this.makeSound((double) l + 0.5D, (double) k1 + 0.5D, (double) j1 + 0.5D, "ambient.cave.cave", 0.7F, 0.8F + this.random.nextFloat() * 0.2F);
                        this.X = this.random.nextInt(12000) + 6000;
                    }
                }
            }

            if (this.random.nextInt(100000) == 0 && this.u() && this.t()) {
                this.l = this.l * 3 + 1013904223;
                k = this.l >> 2;
                l = i + (k & 15);
                j1 = j + (k >> 8 & 15);
                k1 = this.e(l, j1);
                if (this.s(l, k1, j1)) {
                    this.strikeLightning(new EntityWeatherStorm(this, (double) l, (double) k1, (double) j1));
                    this.r = 2;
                }
            }

            int i2;

            if (this.random.nextInt(16) == 0) {
                this.l = this.l * 3 + 1013904223;
                k = this.l >> 2;
                l = k & 15;
                j1 = k >> 8 & 15;
                k1 = this.e(l + i, j1 + j);
                if (this.getWorldChunkManager().getBiome(l + i, j1 + j).b() && k1 >= 0 && k1 < 128 && chunk.a(EnumSkyBlock.BLOCK, l, k1, j1) < 10) {
                    l1 = chunk.getTypeId(l, k1 - 1, j1);
                    i2 = chunk.getTypeId(l, k1, j1);
                    if (this.u() && i2 == 0 && Block.SNOW.canPlace(this, l + i, k1, j1 + j) && l1 != 0 && l1 != Block.ICE.id && Block.byId[l1].material.isSolid()) {
                        // CraftBukkit start
                        BlockState blockState = this.getWorld().getBlockAt(l + i, k1, j1 + j).getState();
                        blockState.setTypeId(Block.SNOW.id);

                        BlockFormEvent snow = new BlockFormEvent(blockState.getBlock(), blockState);
                        this.getServer().getPluginManager().callEvent(snow);
                        if (!snow.isCancelled()) {
                            blockState.update(true);
                        }
                        // CraftBukkit end
                    }

                    if (l1 == Block.STATIONARY_WATER.id && chunk.getData(l, k1 - 1, j1) == 0) {
                        boolean flag = true;

                        if (flag && this.getMaterial(l + i - 1, k1 - 1, j1 + j) != Material.WATER) {
                            flag = false;
                        }

                        if (flag && this.getMaterial(l + i + 1, k1 - 1, j1 + j) != Material.WATER) {
                            flag = false;
                        }

                        if (flag && this.getMaterial(l + i, k1 - 1, j1 + j - 1) != Material.WATER) {
                            flag = false;
                        }

                        if (flag && this.getMaterial(l + i, k1 - 1, j1 + j + 1) != Material.WATER) {
                            flag = false;
                        }

                        if (!flag) {
                            // CraftBukkit start
                            BlockState blockState = this.getWorld().getBlockAt(l + i, k1 - 1, j1 + j).getState();
                            blockState.setTypeId(Block.ICE.id);

                            BlockFormEvent iceBlockForm = new BlockFormEvent(blockState.getBlock(), blockState);
                            this.getServer().getPluginManager().callEvent(iceBlockForm);
                            if (!iceBlockForm.isCancelled()) {
                                blockState.update(true);
                            }
                            // CraftBukkit end
                        }
                    }
                }
            }

            this.p(i + this.random.nextInt(16), this.random.nextInt(128), j + this.random.nextInt(16));

            for (k = 0; k < 80; ++k) {
                this.l = this.l * 3 + 1013904223;
                l = this.l >> 2;
                j1 = l & 15;
                k1 = l >> 8 & 15;
                l1 = l >> 16 & 127;
                i2 = chunk.b[j1 << 11 | k1 << 7 | l1] & 255;
                if (Block.n[i2]) {
                    Block.byId[i2].a(this, j1 + i, l1, k1 + j, this.random);
                }
            }
        }
    }

    public void p(int i, int j, int k) {
        this.b(EnumSkyBlock.SKY, i, j, k);
        this.b(EnumSkyBlock.BLOCK, i, j, k);
    }

    private int d(int i, int j, int k, int l, int i1, int j1) {
        int k1 = 0;

        if (this.isChunkLoaded(j, k, l)) {
            k1 = 15;
        } else {
            if (j1 == 0) {
                j1 = 1;
            }

            for (int l1 = 0; l1 < 6; ++l1) {
                int i2 = l1 % 2 * 2 - 1;
                int j2 = j + l1 / 2 % 3 / 2 * i2;
                int k2 = k + (l1 / 2 + 1) % 3 / 2 * i2;
                int l2 = l + (l1 / 2 + 2) % 3 / 2 * i2;
                int i3 = this.a(EnumSkyBlock.SKY, j2, k2, l2) - j1;

                if (i3 > k1) {
                    k1 = i3;
                }
            }
        }

        return k1;
    }

    private int e(int i, int j, int k, int l, int i1, int j1) {
        int k1 = Block.s[i1];
        int l1 = this.a(EnumSkyBlock.BLOCK, j - 1, k, l) - j1;
        int i2 = this.a(EnumSkyBlock.BLOCK, j + 1, k, l) - j1;
        int j2 = this.a(EnumSkyBlock.BLOCK, j, k - 1, l) - j1;
        int k2 = this.a(EnumSkyBlock.BLOCK, j, k + 1, l) - j1;
        int l2 = this.a(EnumSkyBlock.BLOCK, j, k, l - 1) - j1;
        int i3 = this.a(EnumSkyBlock.BLOCK, j, k, l + 1) - j1;

        if (l1 > k1) {
            k1 = l1;
        }

        if (i2 > k1) {
            k1 = i2;
        }

        if (j2 > k1) {
            k1 = j2;
        }

        if (k2 > k1) {
            k1 = k2;
        }

        if (l2 > k1) {
            k1 = l2;
        }

        if (i3 > k1) {
            k1 = i3;
        }

        return k1;
    }

    public void b(EnumSkyBlock enumskyblock, int i, int j, int k) {
        if (this.areChunksLoaded(i, j, k, 17)) {
            int l = 0;
            int i1 = 0;
            int j1 = this.a(enumskyblock, i, j, k);
            boolean flag = false;
            int k1 = this.getTypeId(i, j, k);
            int l1 = Block.q[k1];

            if (l1 == 0) {
                l1 = 1;
            }

            boolean flag1 = false;
            int i2;

            if (enumskyblock == EnumSkyBlock.SKY) {
                i2 = this.d(j1, i, j, k, k1, l1);
            } else {
                i2 = this.e(j1, i, j, k, k1, l1);
            }

            int j2;
            int k2;
            int l2;
            int i3;
            int j3;
            int k3;

            if (i2 > j1) {
                this.H[i1++] = 133152;
            } else if (i2 < j1) {
                if (enumskyblock != EnumSkyBlock.BLOCK) {
                    ;
                }

                this.H[i1++] = 133152 + (j1 << 18);

                while (l < i1) {
                    j2 = this.H[l++];
                    k1 = (j2 & 63) - 32 + i;
                    l1 = (j2 >> 6 & 63) - 32 + j;
                    i2 = (j2 >> 12 & 63) - 32 + k;
                    k2 = j2 >> 18 & 15;
                    l2 = this.a(enumskyblock, k1, l1, i2);
                    if (l2 == k2) {
                        this.a(enumskyblock, k1, l1, i2, 0);
                        --k2;
                        if (k2 > 0) {
                            i3 = k1 - i;
                            k3 = l1 - j;
                            j3 = i2 - k;
                            if (i3 < 0) {
                                i3 = -i3;
                            }

                            if (k3 < 0) {
                                k3 = -k3;
                            }

                            if (j3 < 0) {
                                j3 = -j3;
                            }

                            if (i3 + k3 + j3 < 17) {
                                for (int l3 = 0; l3 < 6; ++l3) {
                                    int i4 = l3 % 2 * 2 - 1;
                                    int j4 = k1 + l3 / 2 % 3 / 2 * i4;
                                    int k4 = l1 + (l3 / 2 + 1) % 3 / 2 * i4;
                                    int l4 = i2 + (l3 / 2 + 2) % 3 / 2 * i4;

                                    l2 = this.a(enumskyblock, j4, k4, l4);
                                    if (l2 == k2) {
                                        this.H[i1++] = j4 - i + 32 + (k4 - j + 32 << 6) + (l4 - k + 32 << 12) + (k2 << 18);
                                    }
                                }
                            }
                        }
                    }
                }

                l = 0;
            }

            while (l < i1) {
                j1 = this.H[l++];
                int i5 = (j1 & 63) - 32 + i;

                j2 = (j1 >> 6 & 63) - 32 + j;
                k1 = (j1 >> 12 & 63) - 32 + k;
                l1 = this.a(enumskyblock, i5, j2, k1);
                i2 = this.getTypeId(i5, j2, k1);
                k2 = Block.q[i2];
                if (k2 == 0) {
                    k2 = 1;
                }

                boolean flag2 = false;

                if (enumskyblock == EnumSkyBlock.SKY) {
                    l2 = this.d(l1, i5, j2, k1, i2, k2);
                } else {
                    l2 = this.e(l1, i5, j2, k1, i2, k2);
                }

                if (l2 != l1) {
                    this.a(enumskyblock, i5, j2, k1, l2);
                    if (l2 > l1) {
                        i3 = i5 - i;
                        k3 = j2 - j;
                        j3 = k1 - k;
                        if (i3 < 0) {
                            i3 = -i3;
                        }

                        if (k3 < 0) {
                            k3 = -k3;
                        }

                        if (j3 < 0) {
                            j3 = -j3;
                        }

                        if (i3 + k3 + j3 < 17 && i1 < this.H.length - 6) {
                            if (this.a(enumskyblock, i5 - 1, j2, k1) < l2) {
                                this.H[i1++] = i5 - 1 - i + 32 + (j2 - j + 32 << 6) + (k1 - k + 32 << 12);
                            }

                            if (this.a(enumskyblock, i5 + 1, j2, k1) < l2) {
                                this.H[i1++] = i5 + 1 - i + 32 + (j2 - j + 32 << 6) + (k1 - k + 32 << 12);
                            }

                            if (this.a(enumskyblock, i5, j2 - 1, k1) < l2) {
                                this.H[i1++] = i5 - i + 32 + (j2 - 1 - j + 32 << 6) + (k1 - k + 32 << 12);
                            }

                            if (this.a(enumskyblock, i5, j2 + 1, k1) < l2) {
                                this.H[i1++] = i5 - i + 32 + (j2 + 1 - j + 32 << 6) + (k1 - k + 32 << 12);
                            }

                            if (this.a(enumskyblock, i5, j2, k1 - 1) < l2) {
                                this.H[i1++] = i5 - i + 32 + (j2 - j + 32 << 6) + (k1 - 1 - k + 32 << 12);
                            }

                            if (this.a(enumskyblock, i5, j2, k1 + 1) < l2) {
                                this.H[i1++] = i5 - i + 32 + (j2 - j + 32 << 6) + (k1 + 1 - k + 32 << 12);
                            }
                        }
                    }
                }
            }
        }
    }

    public boolean a(boolean flag) {
        int i = this.N.size();

        if (i != this.O.size()) {
            throw new IllegalStateException("TickNextTick list out of synch");
        } else {
            if (i > 1000) {
                i = 1000;
            }

            for (int j = 0; j < i; ++j) {
                NextTickListEntry nextticklistentry = (NextTickListEntry) this.N.first();

                if (!flag && nextticklistentry.e > this.worldData.f()) {
                    break;
                }

                this.N.remove(nextticklistentry);
                this.O.remove(nextticklistentry);
                byte b0 = 8;

                if (this.a(nextticklistentry.a - b0, nextticklistentry.b - b0, nextticklistentry.c - b0, nextticklistentry.a + b0, nextticklistentry.b + b0, nextticklistentry.c + b0)) {
                    int k = this.getTypeId(nextticklistentry.a, nextticklistentry.b, nextticklistentry.c);

                    if (k == nextticklistentry.d && k > 0) {
                        Block.byId[k].a(this, nextticklistentry.a, nextticklistentry.b, nextticklistentry.c, this.random);
                    }
                }
            }

            return this.N.size() != 0;
        }
    }

    public List b(Entity entity, AxisAlignedBB axisalignedbb) {
        this.Y.clear();
        int i = MathHelper.floor((axisalignedbb.a - 2.0D) / 16.0D);
        int j = MathHelper.floor((axisalignedbb.d + 2.0D) / 16.0D);
        int k = MathHelper.floor((axisalignedbb.c - 2.0D) / 16.0D);
        int l = MathHelper.floor((axisalignedbb.f + 2.0D) / 16.0D);

        for (int i1 = i; i1 <= j; ++i1) {
            for (int j1 = k; j1 <= l; ++j1) {
                if (this.isChunkLoaded(i1, j1)) {
                    this.getChunkAt(i1, j1).a(entity, axisalignedbb, this.Y);
                }
            }
        }

        return this.Y;
    }

    public List a(Class oclass, AxisAlignedBB axisalignedbb) {
        int i = MathHelper.floor((axisalignedbb.a - 2.0D) / 16.0D);
        int j = MathHelper.floor((axisalignedbb.d + 2.0D) / 16.0D);
        int k = MathHelper.floor((axisalignedbb.c - 2.0D) / 16.0D);
        int l = MathHelper.floor((axisalignedbb.f + 2.0D) / 16.0D);
        ArrayList arraylist = new ArrayList();

        for (int i1 = i; i1 <= j; ++i1) {
            for (int j1 = k; j1 <= l; ++j1) {
                if (this.isChunkLoaded(i1, j1)) {
                    this.getChunkAt(i1, j1).a(oclass, axisalignedbb, arraylist);
                }
            }
        }

        return arraylist;
    }

    public void b(int i, int j, int k, TileEntity tileentity) {
        if (this.isLoaded(i, j, k)) {
            this.getChunkAtWorldCoords(i, k).f();
        }

        for (int l = 0; l < this.z.size(); ++l) {
            ((IWorldAccess) this.z.get(l)).a(i, j, k, tileentity);
        }
    }

    public int a(Class oclass) {
        int i = 0;

        for (int j = 0; j < this.entityList.size(); ++j) {
            Entity entity = (Entity) this.entityList.get(j);

            if (oclass.isAssignableFrom(entity.getClass())) {
                ++i;
            }
        }

        return i;
    }

    public void a(List list) {
        // CraftBukkit start
        Entity entity = null;
        for (int i = 0; i < list.size(); ++i) {
            entity = (Entity) list.get(i);
            // CraftBukkit start - fixed an NPE
            if (entity == null) {
                continue;
            }
            // CraftBukkit end
            this.entityList.add(entity);
            // CraftBukkit end
            this.c((Entity) list.get(i));
        }
    }

    public void b(List list) {
        this.M.addAll(list);
    }

    public boolean a(int i, int j, int k, int l, boolean flag, int i1) {
        int j1 = this.getTypeId(j, k, l);
        Block block = Block.byId[j1];
        Block block1 = Block.byId[i];
        AxisAlignedBB axisalignedbb = block1.e(this, j, k, l);

        if (flag) {
            axisalignedbb = null;
        }

        boolean defaultReturn; // CraftBukkit - store the default action

        if (axisalignedbb != null && !this.containsEntity(axisalignedbb)) {
            defaultReturn = false; // CraftBukkit
        } else {
            if (block == Block.WATER || block == Block.STATIONARY_WATER || block == Block.LAVA || block == Block.STATIONARY_LAVA || block == Block.FIRE || block == Block.SNOW || block == Block.VINE) {
                block = null;
            }

            defaultReturn = i > 0 && block == null && block1.canPlace(this, j, k, l, i1); // CraftBukkit
        }

        // CraftBukkit start
        BlockCanBuildEvent event = new BlockCanBuildEvent(this.getWorld().getBlockAt(j, k, l), i, defaultReturn);
        this.getServer().getPluginManager().callEvent(event);

        return event.isBuildable();
        // CraftBukkit end
    }

    public PathEntity findPath(Entity entity, Entity entity1, float f) {
        int i = MathHelper.floor(entity.locX);
        int j = MathHelper.floor(entity.locY);
        int k = MathHelper.floor(entity.locZ);
        int l = (int) (f + 16.0F);
        int i1 = i - l;
        int j1 = j - l;
        int k1 = k - l;
        int l1 = i + l;
        int i2 = j + l;
        int j2 = k + l;
        ChunkCache chunkcache = new ChunkCache(this, i1, j1, k1, l1, i2, j2);

        return (new Pathfinder(chunkcache)).a(entity, entity1, f);
    }

    public PathEntity a(Entity entity, int i, int j, int k, float f) {
        int l = MathHelper.floor(entity.locX);
        int i1 = MathHelper.floor(entity.locY);
        int j1 = MathHelper.floor(entity.locZ);
        int k1 = (int) (f + 8.0F);
        int l1 = l - k1;
        int i2 = i1 - k1;
        int j2 = j1 - k1;
        int k2 = l + k1;
        int l2 = i1 + k1;
        int i3 = j1 + k1;
        ChunkCache chunkcache = new ChunkCache(this, l1, i2, j2, k2, l2, i3);

        return (new Pathfinder(chunkcache)).a(entity, i, j, k, f);
    }

    public boolean isBlockFacePowered(int i, int j, int k, int l) {
        int i1 = this.getTypeId(i, j, k);

        return i1 == 0 ? false : Block.byId[i1].d(this, i, j, k, l);
    }

    public boolean isBlockPowered(int i, int j, int k) {
        return this.isBlockFacePowered(i, j - 1, k, 0) ? true : (this.isBlockFacePowered(i, j + 1, k, 1) ? true : (this.isBlockFacePowered(i, j, k - 1, 2) ? true : (this.isBlockFacePowered(i, j, k + 1, 3) ? true : (this.isBlockFacePowered(i - 1, j, k, 4) ? true : this.isBlockFacePowered(i + 1, j, k, 5)))));
    }

    public boolean isBlockFaceIndirectlyPowered(int i, int j, int k, int l) {
        if (this.e(i, j, k)) {
            return this.isBlockPowered(i, j, k);
        } else {
            int i1 = this.getTypeId(i, j, k);

            return i1 == 0 ? false : Block.byId[i1].a(this, i, j, k, l);
        }
    }

    public boolean isBlockIndirectlyPowered(int i, int j, int k) {
        return this.isBlockFaceIndirectlyPowered(i, j - 1, k, 0) ? true : (this.isBlockFaceIndirectlyPowered(i, j + 1, k, 1) ? true : (this.isBlockFaceIndirectlyPowered(i, j, k - 1, 2) ? true : (this.isBlockFaceIndirectlyPowered(i, j, k + 1, 3) ? true : (this.isBlockFaceIndirectlyPowered(i - 1, j, k, 4) ? true : this.isBlockFaceIndirectlyPowered(i + 1, j, k, 5)))));
    }

    public EntityHuman findNearbyPlayer(Entity entity, double d0) {
        return this.a(entity.locX, entity.locY, entity.locZ, d0);
    }

    public EntityHuman a(double d0, double d1, double d2, double d3) {
        double d4 = -1.0D;
        EntityHuman entityhuman = null;

        for (int i = 0; i < this.players.size(); ++i) {
            EntityHuman entityhuman1 = (EntityHuman) this.players.get(i);
            // CraftBukkit start - fixed an NPE
            if (entityhuman1 == null || entityhuman1.dead) {
                continue;
            }
            // CraftBukkit end
            double d5 = entityhuman1.e(d0, d1, d2);

            if ((d3 < 0.0D || d5 < d3 * d3) && (d4 == -1.0D || d5 < d4)) {
                d4 = d5;
                entityhuman = entityhuman1;
            }
        }

        return entityhuman;
    }

    public EntityHuman a(String s) {
        for (int i = 0; i < this.players.size(); ++i) {
            if (s.equals(((EntityHuman) this.players.get(i)).name)) {
                return (EntityHuman) this.players.get(i);
            }
        }

        return null;
    }

    public byte[] getMultiChunkData(int i, int j, int k, int l, int i1, int j1) {
        byte[] abyte = new byte[l * i1 * j1 * 5 / 2];
        int k1 = i >> 4;
        int l1 = k >> 4;
        int i2 = i + l - 1 >> 4;
        int j2 = k + j1 - 1 >> 4;
        int k2 = 0;
        int l2 = j;
        int i3 = j + i1;

        if (j < 0) {
            l2 = 0;
        }

        if (i3 > 128) {
            i3 = 128;
        }

        for (int j3 = k1; j3 <= i2; ++j3) {
            int k3 = i - j3 * 16;
            int l3 = i + l - j3 * 16;

            if (k3 < 0) {
                k3 = 0;
            }

            if (l3 > 16) {
                l3 = 16;
            }

            for (int i4 = l1; i4 <= j2; ++i4) {
                int j4 = k - i4 * 16;
                int k4 = k + j1 - i4 * 16;

                if (j4 < 0) {
                    j4 = 0;
                }

                if (k4 > 16) {
                    k4 = 16;
                }

                k2 = this.getChunkAt(j3, i4).getData(abyte, k3, l2, j4, l3, i3, k4, k2);
            }
        }

        return abyte;
    }

    public void j() {
        this.B.b();
    }

    public void setTime(long i) {
        this.worldData.a(i);
    }

    public void setTimeAndFixTicklists(long i) {
        long j = i - this.worldData.f();

        NextTickListEntry nextticklistentry;

        for (Iterator iterator = this.O.iterator(); iterator.hasNext(); nextticklistentry.e += j) {
            nextticklistentry = (NextTickListEntry) iterator.next();
        }

        this.setTime(i);
    }

    public long getSeed() {
        return this.worldData.getSeed();
    }

    public long getTime() {
        return this.worldData.f();
    }

    public ChunkCoordinates getSpawn() {
        return new ChunkCoordinates(this.worldData.c(), this.worldData.d(), this.worldData.e());
    }

    public boolean a(EntityHuman entityhuman, int i, int j, int k) {
        return true;
    }

    public void a(Entity entity, byte b0) {}

    public IChunkProvider n() {
        return this.chunkProvider;
    }

    public void playNote(int i, int j, int k, int l, int i1) {
        int j1 = this.getTypeId(i, j, k);

        if (j1 > 0) {
            Block.byId[j1].a(this, i, j, k, l, i1);
        }
    }

    public IDataManager o() {
        return this.B;
    }

    public WorldData p() {
        return this.worldData;
    }

    public void everyoneSleeping() {
        this.T = !this.players.isEmpty();
        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityHuman entityhuman = (EntityHuman) iterator.next();

            // CraftBukkit
            if (!entityhuman.isSleeping() && !entityhuman.fauxSleeping) {
                this.T = false;
                break;
            }
        }
    }

    // CraftBukkit start
    // Calls the method that checks to see if players are sleeping
    // Called by CraftPlayer.setPermanentSleeping()
    public void checkSleepStatus() {
        if (!this.isStatic) {
            this.everyoneSleeping();
        }
    }
    // CraftBukkit end

    protected void r() {
        this.T = false;
        Iterator iterator = this.players.iterator();

        while (iterator.hasNext()) {
            EntityHuman entityhuman = (EntityHuman) iterator.next();

            if (entityhuman.isSleeping()) {
                entityhuman.a(false, false, true);
            }
        }

        this.y();
    }

    public boolean everyoneDeeplySleeping() {
        if (this.T && !this.isStatic) {
            Iterator iterator = this.players.iterator();

            // CraftBukkit - This allows us to assume that some people are in bed but not really, allowing time to pass in spite of AFKers
            boolean foundActualSleepers = false;

            EntityHuman entityhuman;

            do {
                if (!iterator.hasNext()) {
                    // CraftBukkit
                    return foundActualSleepers;
                }

                entityhuman = (EntityHuman) iterator.next();
                // CraftBukkit start
                if (entityhuman.isDeeplySleeping()) {
                    foundActualSleepers = true;
                }
            } while (entityhuman.isDeeplySleeping() || entityhuman.fauxSleeping);
            // CraftBukkit end

            return false;
        } else {
            return false;
        }
    }

    public float c(float f) {
        return (this.p + (this.q - this.p) * f) * this.d(f);
    }

    public float d(float f) {
        return this.n + (this.o - this.n) * f;
    }

    public boolean t() {
        return (double) this.c(1.0F) > 0.9D;
    }

    public boolean u() {
        return (double) this.d(1.0F) > 0.2D;
    }

    public boolean s(int i, int j, int k) {
        if (!this.u()) {
            return false;
        } else if (!this.isChunkLoaded(i, j, k)) {
            return false;
        } else if (this.e(i, k) > j) {
            return false;
        } else {
            BiomeBase biomebase = this.getWorldChunkManager().getBiome(i, k);

            return biomebase.b() ? false : biomebase.c();
        }
    }

    public void a(String s, WorldMapBase worldmapbase) {
        this.worldMaps.a(s, worldmapbase);
    }

    public WorldMapBase a(Class oclass, String s) {
        return this.worldMaps.a(oclass, s);
    }

    public int b(String s) {
        return this.worldMaps.a(s);
    }

    public void e(int i, int j, int k, int l, int i1) {
        this.a((EntityHuman) null, i, j, k, l, i1);
    }

    public void a(EntityHuman entityhuman, int i, int j, int k, int l, int i1) {
        for (int j1 = 0; j1 < this.z.size(); ++j1) {
            ((IWorldAccess) this.z.get(j1)).a(entityhuman, i, j, k, l, i1);
        }
    }

    public Random t(int i, int j, int k) {
        long l = (long) i * 341873128712L + (long) j * 132897987541L + this.p().getSeed() + (long) k;

        this.random.setSeed(l);
        return this.random;
    }

    public boolean v() {
        return false;
    }

    public void a(EnumSkyBlock enumskyblock, int i, int j, int k, int l, int i1, int j1) {}

    // CraftBukkit start
    public UUID getUUID() {
        return this.B.getUUID();
    }
    // CraftBukkit end
}
