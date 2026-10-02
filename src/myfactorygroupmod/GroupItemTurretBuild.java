package myfactorygroupmod;

import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Log;
import java.lang.reflect.Constructor;
import mindustry.entities.bullet.BulletType;
import mindustry.gen.Building;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.Turret;

public class GroupItemTurretBuild extends ItemTurret.ItemTurretBuild implements GroupTurretBuild {

    private FactoryGroup groupRef;

    @Override
    public FactoryGroup fgmGroup() {
        return groupRef;
    }

    @Override
    public void fgmGroup(FactoryGroup group) {
        groupRef = group;
    }

    /** 真正在共享群里（成员 > 1）才走群逻辑，单炮塔走原版 */
    private boolean inSharedGroup() {
        FactoryGroup g = GroupManager.getGroup(this);
        return g != null && g.members.size > 1;
    }

    /** 玩家选择的弹药；null = 自动从共享池挑 */
    public Item selectedAmmo;

    private Item lastSyncedItem;
    private int lastSyncedAmount = -1;

    /** 自维护的当前弹药类型，等价于 ammo.peek().item；由 syncAmmo 维护，替代 getEntryItem 反射读取 */
    private Item currentAmmo;

    // === 反射缓存（仅构造 ItemEntry 需要）===
    private static Constructor<?> itemEntryCtor;

    // === 弹药键缓存：每个 ItemTurret 的 Item[]，避免每 tick 重复遍历 ammoTypes.keys() ===
    private static final ObjectMap<ItemTurret, Item[]> ammoItemsCache = new ObjectMap<>();

    static {
        try {
            Class<?> cls = Class.forName(
                "mindustry.world.blocks.defense.turrets.ItemTurret$ItemEntry");
            itemEntryCtor = cls.getDeclaredConstructor(ItemTurret.class, Item.class, int.class);
            itemEntryCtor.setAccessible(true);
        } catch (Throwable t) {
            Log.warn("[fgm] 无法反射 ItemEntry: @", t.getMessage());
        }
    }

    public GroupItemTurretBuild(ItemTurret turret) {
        turret.super();
    }

    private Turret.AmmoEntry makeEntry(Item item, int amount) {
        if (itemEntryCtor == null) return null;
        try {
            return (Turret.AmmoEntry) itemEntryCtor.newInstance((ItemTurret) block, item, amount);
        } catch (Throwable t) {
            return null;
        }
    }

    /** 缓存的弹药键数组；首次访问时从 ammoTypes.keys() 拷贝一次，之后直接复用 */
    private static Item[] ammoItems(ItemTurret turret) {
        Item[] cached = ammoItemsCache.get(turret);
        if (cached == null) {
            Seq<Item> tmp = new Seq<>();
            for (Item it : turret.ammoTypes.keys()) {
                tmp.add(it);
            }
            cached = new Item[tmp.size];
            for (int i = 0; i < tmp.size; i++) {
                cached[i] = tmp.get(i);
            }
            ammoItemsCache.put(turret, cached);
        }
        return cached;
    }

    /** 决定当前该用哪种弹药 */
    private Item resolveAmmo(FactoryGroup g) {
        ItemTurret turret = (ItemTurret) block;

        // 1. 玩家选的
        if (selectedAmmo != null
                && turret.ammoTypes.get(selectedAmmo) != null
                && items.get(selectedAmmo) > 0) {
            return selectedAmmo;
        }

        // 2. 保留当前 ammo 队列的（currentAmmo 即 ammo.peek().item，由 syncAmmo 维护）
        if (ammo.size > 0 && currentAmmo != null
                && turret.ammoTypes.get(currentAmmo) != null
                && items.get(currentAmmo) > 0) {
            return currentAmmo;
        }

        // 3. 共享池里任选一个自己能用的
        for (Item it : ammoItems(turret)) {
            if (items.get(it) > 0) return it;
        }
        return null;
    }

    private void syncAmmo(FactoryGroup g) {
        items = g.sharedItems;
        Item target = resolveAmmo(g);
        int amount = target == null ? 0 : items.get(target);

        if (target == lastSyncedItem && amount == lastSyncedAmount) return;

        ammo.clear();
        currentAmmo = null;
        int maxA = ((ItemTurret) block).maxAmmo;
        if (target != null) {
            Turret.AmmoEntry e = makeEntry(target, Math.min(amount, maxA));
            if (e != null) {
                ammo.add(e);
                currentAmmo = target;
            }
            totalAmmo = Math.min(amount, maxA);
        } else {
            totalAmmo = 0;
        }

        lastSyncedItem = target;
        lastSyncedAmount = amount;
    }

    @Override
    public void updateTile() {
        if (!inSharedGroup()) {
            super.updateTile();
            return;
        }
        GroupSupport.redirectModules(this);
        FactoryGroup g = GroupManager.getGroup(this);
        syncAmmo(g);
        super.updateTile();
    }

    // ===== 弹药判定 =====

    @Override
    public boolean hasAmmo() {
        if (!inSharedGroup()) return super.hasAmmo();
        FactoryGroup g = GroupManager.getGroup(this);
        if (!canConsume()) return false;
        if (cheating()) return true;
        items = g.sharedItems;
        Item target = resolveAmmo(g);
        if (target == null) return false;
        return items.get(target) >= ((ItemTurret) block).ammoPerShot;
    }

    @Override
    public BulletType useAmmo() {
        if (!inSharedGroup()) return super.useAmmo();
        FactoryGroup g = GroupManager.getGroup(this);
        if (cheating()) return peekAmmo();

        items = g.sharedItems;
        Item target = resolveAmmo(g);
        if (target == null) return null;

        int use = ((ItemTurret) block).ammoPerShot;
        if (items.get(target) < use) return null;

        items.remove(target, use);
        lastSyncedAmount = -1;
        return ((ItemTurret) block).ammoTypes.get(target);
    }

    @Override
    public BulletType peekAmmo() {
        if (!inSharedGroup()) return super.peekAmmo();
        FactoryGroup g = GroupManager.getGroup(this);
        items = g.sharedItems;
        Item target = resolveAmmo(g);
        return target == null ? null : ((ItemTurret) block).ammoTypes.get(target);
    }

    @Override
    public float getAmmoFraction() {
        if (!inSharedGroup()) return super.getAmmoFraction();
        FactoryGroup g = GroupManager.getGroup(this);
        items = g.sharedItems;
        Item target = resolveAmmo(g);
        if (target == null) return 0f;
        int maxA = ((ItemTurret) block).maxAmmo;
        return Math.min(1f, (float) items.get(target) / Math.max(1, maxA));
    }

    // ===== 物品接收：白名单改为群内所有炮塔的弹药（O(1) 掩码） ====

    @Override
    public boolean acceptItem(Building source, Item item) {
        if (!inSharedGroup()) return super.acceptItem(source, item);
        FactoryGroup g = GroupManager.getGroup(this);
        if (!g.acceptsTurretAmmo(item)) return false;
        items = g.sharedItems;
        return items.get(item) < getMaximumAccepted(item);
    }

    @Override
    public int getMaximumAccepted(Item item) {
        int maxA = ((ItemTurret) block).maxAmmo;
        FactoryGroup g = GroupManager.getGroup(this);
        if (g == null || g.members.size <= 1) return maxA;
        return maxA * g.members.size;
    }

    @Override
    public void handleItem(Building source, Item item) {
        if (!inSharedGroup()) {
            super.handleItem(source, item);
            return;
        }
        FactoryGroup g = GroupManager.getGroup(this);
        // 不再拒绝非自己弹药类型：进共享池就行
        items = g.sharedItems;
        items.add(item, 1);
        lastSyncedAmount = -1;
    }

    /** 炮塔不主动 dump 弹药 */
    @Override
    public boolean dump(Item item) {
        if (inSharedGroup()) return false;
        return super.dump(item);
    }

    // ===== 弹药选择 UI =====

    @Override
    public void buildConfiguration(Table table) {
        if (!inSharedGroup()) {
            super.buildConfiguration(table);
            return;
        }
        ItemTurret turret = (ItemTurret) block;

        table.table(t -> {
            t.defaults().size(48f).pad(4f);
            int i = 0;
            int cols = turret.selectionColumns > 0 ? turret.selectionColumns : 4;
            for (Item it : turret.ammoTypes.keys()) {
                if (i % cols == 0) t.row();
                final Item item = it;
                t.button(new TextureRegionDrawable(it.uiIcon), () -> {
                    selectedAmmo = (selectedAmmo == item) ? null : item;
                    lastSyncedAmount = -1;
                    deselect();
                }).update(b -> {
                    b.getImage().setColor(selectedAmmo == item ? Pal.accent : Color.white);
                }).tooltip(it.localizedName);
                i++;
            }
        });
    }

    @Override
    public boolean shouldShowConfigure(mindustry.gen.Player player) {
        return inSharedGroup();
    }

    @Override
    public boolean configTapped() {
        return inSharedGroup();
    }

    // ===== 液体 =====

    @Override
    public boolean acceptLiquid(Building source, Liquid liquid) {
        if (inSharedGroup()) {
            return GroupSupport.acceptLiquid(this, source, liquid);
        }
        return super.acceptLiquid(source, liquid);
    }

    @Override
    public void dumpLiquid(Liquid liquid, float scaling, int outputDir) {
        if (inSharedGroup()) {
            GroupSupport.dumpLiquidFiltered(this, liquid, scaling, outputDir);
        } else {
            super.dumpLiquid(liquid, scaling, outputDir);
        }
    }

    // ===== 电力 =====

    @Override
    public Seq<Building> getPowerConnections(Seq<Building> out) {
        super.getPowerConnections(out);
        return GroupSupport.getPowerConnections(this, out);
    }

    // ===== UI =====

    @Override
    public void display(Table table) {
        super.display(table);
        GroupSupport.displayGroup(this, table);
    }
}