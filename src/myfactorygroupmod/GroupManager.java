package myfactorygroupmod;

import arc.math.Mathf;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;

public class GroupManager {

    /** 群引用现在直接挂在建筑字段上（见 GroupBuild.fgmGroup()），
     *  这张表只用于“需要遍历所有已入群建筑”的场景（cleanup 兜底清理）。 */
    private static final ObjectMap<Building, FactoryGroup> groupByBuilding = new ObjectMap<>();
    private static final int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}};

    /** cleanup 的临时收集器，复用以避免每次分配。 */
    private static final ObjectSet<Building> deadScratch = new ObjectSet<>();

    public static FactoryGroup getGroup(Building b) {
        return b instanceof GroupBuild gb ? gb.fgmGroup() : null;
    }

    public static boolean isFactory(Building b) {
        return b instanceof GroupFactoryBuild;
    }

    public static boolean isTurret(Building b) {
        return b instanceof GroupTurretBuild;
    }

    public static boolean isWall(Building b) {
        return b instanceof GroupWallMember;
    }

    public static boolean isGroupable(Building b) {
        return b instanceof GroupBuild;
    }

    /**
     * 邻居是否与本建筑同类。墙/炮塔/工厂互相隔离，不合并。
     */
    private static boolean sameKind(Building a, Building b) {
        if (a instanceof GroupWallMember) return b instanceof GroupWallMember;
        if (a instanceof GroupTurretBuild) return b instanceof GroupTurretBuild;
        return b instanceof GroupFactoryBuild;
    }

    private static void setGroup(Building b, FactoryGroup g) {
        if (b instanceof GroupBuild gb) gb.fgmGroup(g);
        groupByBuilding.put(b, g);
    }

    private static void removeGroup(Building b) {
        if (b instanceof GroupBuild gb) gb.fgmGroup(null);
        groupByBuilding.remove(b);
    }

    /** 世界重载时清空所有引用。 */
    public static void reset() {
        for (ObjectMap.Entry<Building, FactoryGroup> e : groupByBuilding) {
            if (e.key instanceof GroupBuild gb) gb.fgmGroup(null);
        }
        groupByBuilding.clear();
        deadScratch.clear();
    }

    private static void applyShared(Building b, FactoryGroup group) {
        // 墙只共享血量，不共享物品/液体
        if (b instanceof GroupWallMember) return;
        if (b.items != group.sharedItems) b.items = group.sharedItems;
        if (b.block.hasLiquids && b.liquids != group.sharedLiquids) {
            b.liquids = group.sharedLiquids;
        }
    }

    private static void refreshPower(FactoryGroup group) {
        // 必须先快照：updatePowerGraph() 内部会走 getPowerConnections()，
        // 那里会再次遍历同一个 group.members。Arc 的 ObjectSet 不允许嵌套迭代。
        arc.struct.Seq<Building> snapshot = group.members.toSeq();
        for (Building b : snapshot) {
            if (b != null && b.power != null && b.isValid()) {
                b.updatePowerGraph();
            }
        }
    }

    /**
     * 清理已经不在世界里的建筑。
     * 不再复制整张表：遍历时只收集死键，遍历结束后再统一处理。
     */
    public static boolean cleanup() {
        deadScratch.clear();
        boolean changed = false;
        boolean nullKey = false;

        for (ObjectMap.Entry<Building, FactoryGroup> e : groupByBuilding) {
            Building b = e.key;
            if (b == null) nullKey = true;
            else if (!isAlive(b)) {
                deadScratch.add(b);
                changed = true;
            }
        }

        if (nullKey) {
            groupByBuilding.remove(null);
        }

        if (deadScratch.size > 0) {
            for (Building b : deadScratch) {
                onBuildingRemoved(b);
            }
            deadScratch.clear();
        }

        return changed;
    }

    private static boolean isAlive(Building b) {
        if (b.tile == null) return false;
        return b.tile.build == b;
    }

    private static void enqueueNeighbors(Building b, Queue<Building> queue, ObjectSet<Building> visited) {
        int size = b.block.size;
        int soff = b.block.sizeOffset;
        int worldW = Vars.world.width();
        int worldH = Vars.world.height();
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                int tx = b.tile.x + soff + dx;
                int ty = b.tile.y + soff + dy;
                for (int[] d : DIRS) {
                    int nx = tx + d[0];
                    int ny = ty + d[1];
                    if (nx < 0 || ny < 0 || nx >= worldW || ny >= worldH) continue;
                    Tile t = Vars.world.tile(nx, ny);
                    if (t == null || t.build == null) continue;
                    if (t.build == b) continue;
                    if (!sameKind(b, t.build)) continue;
                    if (visited.contains(t.build)) continue;
                    visited.add(t.build);
                    queue.addLast(t.build);
                }
            }
        }
    }

    public static boolean hasForeignNeighbor(Building b) {
        if (!isGroupable(b)) return false;
        FactoryGroup myGroup = getGroup(b);
        if (myGroup == null) return true;
        int size = b.block.size;
        int soff = b.block.sizeOffset;
        int worldW = Vars.world.width();
        int worldH = Vars.world.height();
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                int tx = b.tile.x + soff + dx;
                int ty = b.tile.y + soff + dy;
                for (int[] d : DIRS) {
                    int nx = tx + d[0];
                    int ny = ty + d[1];
                    if (nx < 0 || ny < 0 || nx >= worldW || ny >= worldH) continue;
                    Tile t = Vars.world.tile(nx, ny);
                    if (t == null || t.build == null) continue;
                    if (t.build == b) continue;
                    if (!sameKind(b, t.build)) continue;
                    FactoryGroup otherGroup = getGroup(t.build);
                    if (otherGroup != myGroup) return true;
                }
            }
        }
        return false;
    }

    public static void onBuildingPlaced(Building building) {
        if (!isGroupable(building)) return;

        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();
        ObjectSet<FactoryGroup> foundGroups = new ObjectSet<>();

        queue.addLast(building);
        visited.add(building);

        while (queue.size > 0) {
            Building current = queue.removeFirst();
            FactoryGroup existing = getGroup(current);
            if (existing != null) foundGroups.add(existing);
            enqueueNeighbors(current, queue, visited);
        }

        FactoryGroup targetGroup;
        if (foundGroups.size == 0) {
            targetGroup = new FactoryGroup();
        } else {
            targetGroup = foundGroups.first();
            for (FactoryGroup other : foundGroups) {
                if (other == targetGroup) continue;
                for (Building b : other.members) {
                    setGroup(b, targetGroup);
                    targetGroup.add(b);
                }
                targetGroup.absorbItems(other);
                targetGroup.absorbLiquids(other);
            }
        }

        for (Building b : visited) {
            targetGroup.add(b);
            setGroup(b, targetGroup);
            applyShared(b, targetGroup);
        }

        // 墙群：把当前所有成员的现有血量相加，重算比例，然后统一同步
        if (building instanceof GroupWallMember) {
            float totalMax = 0f;
            float totalHp = 0f;
            for (Building b : targetGroup.members) {
                totalMax += b.maxHealth;
                totalHp += b.health;
            }
            if (totalMax > 0f) {
                targetGroup.wallHealthFraction = Mathf.clamp(totalHp / totalMax, 0f, 1f);
            }
            for (Building b : targetGroup.members) {
                b.health = b.maxHealth * targetGroup.wallHealthFraction;
            }
        }

        refreshPower(targetGroup);
    }

    public static void onBuildingRemoved(Building building) {
        if (building == null) return;
        FactoryGroup group = getGroup(building);
        if (group == null) return;

        group.remove(building);
        removeGroup(building);

        if (group.isEmpty()) return;

        ObjectSet<Building> allMembers = new ObjectSet<>();
        allMembers.addAll(group.members);

        for (Building b : group.members) {
            removeGroup(b);
        }
        group.clear();

        ObjectSet<FactoryGroup> newGroups = new ObjectSet<>();
        for (Building b : allMembers) {
            if (getGroup(b) != null) continue;
            newGroups.add(bfsAssign(b, allMembers, group.wallHealthFraction));
        }

        for (FactoryGroup g : newGroups) {
            refreshPower(g);
        }
    }

    private static FactoryGroup bfsAssign(Building start, ObjectSet<Building> candidates, float inheritedWallFraction) {
        FactoryGroup newGroup = new FactoryGroup();
        newGroup.wallHealthFraction = inheritedWallFraction;

        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();

        queue.addLast(start);
        visited.add(start);

        while (queue.size > 0) {
            Building current = queue.removeFirst();
            newGroup.add(current);
            setGroup(current, newGroup);
            applyShared(current, newGroup);

            int size = current.block.size;
            int soff = current.block.sizeOffset;
            int worldW = Vars.world.width();
            int worldH = Vars.world.height();
            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    int tx = current.tile.x + soff + dx;
                    int ty = current.tile.y + soff + dy;
                    for (int[] d : DIRS) {
                        int nx = tx + d[0];
                        int ny = ty + d[1];
                        if (nx < 0 || ny < 0 || nx >= worldW || ny >= worldH) continue;
                        Tile t = Vars.world.tile(nx, ny);
                        if (t == null || t.build == null) continue;
                        if (t.build == current) continue;
                        if (!sameKind(current, t.build)) continue;
                        if (visited.contains(t.build)) continue;
                        if (!candidates.contains(t.build)) continue;
                        visited.add(t.build);
                        queue.addLast(t.build);
                    }
                }
            }
        }

        // 墙群拆分后，按继承的比例同步血量
        if (start instanceof GroupWallMember) {
            for (Building b : newGroup.members) {
                b.health = b.maxHealth * newGroup.wallHealthFraction;
            }
        }
        return newGroup;
    }
}