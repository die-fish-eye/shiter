package myfactorygroupmod;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;

public class GroupManager {

    private static final ObjectMap<Building, FactoryGroup> buildingToGroup = new ObjectMap<>();
    private static final int[][] DIRS = {{1,0},{-1,0},{0,1},{0,-1}};

    public static FactoryGroup getGroup(Building building) {
        return buildingToGroup.get(building);
    }

    public static boolean isFactory(Building b) {
    return b instanceof GroupCrafterBuild
        || b instanceof GroupSeparatorBuild
        || b instanceof GroupDrillBuild
        || b instanceof GroupConsumeGeneratorBuild
        || b instanceof GroupHeaterGeneratorBuild
        || b instanceof GroupImpactReactorBuild
        || b instanceof GroupVariableReactorBuild
        || b instanceof GroupNuclearReactorBuild
        || b instanceof GroupPumpBuild
        || b instanceof GroupSolidPumpBuild
        || b instanceof GroupFrackerBuild
        || b instanceof GroupAttributeCrafterBuild;
}

    private static void applyShared(Building b, FactoryGroup group) {
        if (b.items != group.sharedItems) b.items = group.sharedItems;
        if (b.block.hasLiquids && b.liquids != group.sharedLiquids) {
            b.liquids = group.sharedLiquids;
        }
    }

    /** 重建群内所有成员的电力图 */
    private static void refreshPower(FactoryGroup group) {
    // 快照，避免嵌套迭代
    arc.struct.Seq<Building> snapshot = group.members.toSeq();
    for (Building b : snapshot) {
        if (b != null && b.power != null && b.isValid()) {
            b.updatePowerGraph();
        }
    }
}

    public static boolean cleanup() {
        boolean changed = false;
        ObjectMap<Building, FactoryGroup> copy = new ObjectMap<>(buildingToGroup);
        for (ObjectMap.Entry<Building, FactoryGroup> e : copy) {
            Building b = e.key;
            if (b == null || !isAlive(b)) {
                onBuildingRemoved(b);
                changed = true;
            }
        }
        return changed;
    }

    private static boolean isAlive(Building b) {
        if (b.tile == null) return false;
        return b.tile.build == b;
    }

    private static void enqueueNeighbors(Building b, Queue<Building> queue, ObjectSet<Building> visited) {
        int size = b.block.size;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                int tx = b.tile.x + dx;
                int ty = b.tile.y + dy;
                for (int[] d : DIRS) {
                    int nx = tx + d[0];
                    int ny = ty + d[1];
                    if (nx < 0 || ny < 0 || nx >= Vars.world.width() || ny >= Vars.world.height()) continue;
                    Tile t = Vars.world.tile(nx, ny);
                    if (t == null || t.build == null) continue;
                    if (t.build == b) continue;
                    if (!isFactory(t.build)) continue;
                    if (visited.contains(t.build)) continue;
                    visited.add(t.build);
                    queue.addLast(t.build);
                }
            }
        }
    }

    public static boolean hasForeignNeighbor(Building b) {
        if (!isFactory(b)) return false;
        FactoryGroup myGroup = buildingToGroup.get(b);
        if (myGroup == null) return true;
        int size = b.block.size;
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                int tx = b.tile.x + dx;
                int ty = b.tile.y + dy;
                for (int[] d : DIRS) {
                    int nx = tx + d[0];
                    int ny = ty + d[1];
                    if (nx < 0 || ny < 0 || nx >= Vars.world.width() || ny >= Vars.world.height()) continue;
                    Tile t = Vars.world.tile(nx, ny);
                    if (t == null || t.build == null) continue;
                    if (t.build == b) continue;
                    if (!isFactory(t.build)) continue;
                    FactoryGroup otherGroup = buildingToGroup.get(t.build);
                    if (otherGroup != myGroup) return true;
                }
            }
        }
        return false;
    }

    public static void onBuildingPlaced(Building building) {
        if (!isFactory(building)) return;

        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();
        ObjectSet<FactoryGroup> foundGroups = new ObjectSet<>();

        queue.addLast(building);
        visited.add(building);

        while (queue.size > 0) {
            Building current = queue.removeFirst();
            FactoryGroup existing = buildingToGroup.get(current);
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
                    buildingToGroup.put(b, targetGroup);
                    targetGroup.members.add(b);
                }
                targetGroup.absorbItems(other);
                targetGroup.absorbLiquids(other);
            }
        }

        for (Building b : visited) {
            targetGroup.add(b);
            buildingToGroup.put(b, targetGroup);
            applyShared(b, targetGroup);
        }

        // 群结构变化，刷新电力
        refreshPower(targetGroup);
    }

    public static void onBuildingRemoved(Building building) {
        if (building == null) return;
        FactoryGroup group = buildingToGroup.get(building);
        if (group == null) return;

        group.remove(building);
        buildingToGroup.remove(building);

        if (group.isEmpty()) return;

        ObjectSet<Building> allMembers = new ObjectSet<>();
        allMembers.addAll(group.members);

        for (Building b : group.members) {
            buildingToGroup.remove(b);
        }
        group.members.clear();

        ObjectSet<FactoryGroup> newGroups = new ObjectSet<>();
        for (Building b : allMembers) {
            if (buildingToGroup.containsKey(b)) continue;
            newGroups.add(bfsAssign(b, allMembers));
        }

        // 所有新分裂的群，刷新电力
        for (FactoryGroup g : newGroups) {
            refreshPower(g);
        }
    }

    private static FactoryGroup bfsAssign(Building start, ObjectSet<Building> candidates) {
        FactoryGroup newGroup = new FactoryGroup();
        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();

        queue.addLast(start);
        visited.add(start);

        while (queue.size > 0) {
            Building current = queue.removeFirst();
            newGroup.add(current);
            buildingToGroup.put(current, newGroup);
            applyShared(current, newGroup);

            int size = current.block.size;
            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    int tx = current.tile.x + dx;
                    int ty = current.tile.y + dy;
                    for (int[] d : DIRS) {
                        int nx = tx + d[0];
                        int ny = ty + d[1];
                        if (nx < 0 || ny < 0 || nx >= Vars.world.width() || ny >= Vars.world.height()) continue;
                        Tile t = Vars.world.tile(nx, ny);
                        if (t == null || t.build == null) continue;
                        if (t.build == current) continue;
                        if (!isFactory(t.build)) continue;
                        if (visited.contains(t.build)) continue;
                        if (!candidates.contains(t.build)) continue;
                        visited.add(t.build);
                        queue.addLast(t.build);
                    }
                }
            }
        }
        return newGroup;
    }
}