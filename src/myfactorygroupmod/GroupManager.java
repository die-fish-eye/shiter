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

    /** 白名单：只有被替换成 GroupCrafterBuild 的方块才算工厂 */
    public static boolean isFactory(Building b) {
        return b instanceof GroupCrafterBuild;
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
                    if (myGroup == null || otherGroup == null || otherGroup != myGroup) {
                        return true;
                    }
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
        }
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

        for (Building b : allMembers) {
            if (buildingToGroup.containsKey(b)) continue;
            bfsAssign(b, allMembers);
        }
    }

    private static void bfsAssign(Building start, ObjectSet<Building> candidates) {
        FactoryGroup newGroup = new FactoryGroup();
        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();

        queue.addLast(start);
        visited.add(start);

        while (queue.size > 0) {
            Building current = queue.removeFirst();
            newGroup.add(current);
            buildingToGroup.put(current, newGroup);

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
    }
}