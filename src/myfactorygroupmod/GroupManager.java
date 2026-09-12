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

    /** 把建筑的四个方向的所有相邻格子上的建筑加入队列（正确支持多格建筑） */
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
                    if (!t.build.block.hasItems) continue;
                    if (visited.contains(t.build)) continue;
                    visited.add(t.build);
                    queue.addLast(t.build);
                }
            }
        }
    }

    /** 检查建筑是否有邻居不属于它的群（用于 Timer 决定是否重新分配） */
    public static boolean hasForeignNeighbor(Building b) {
        if (!b.block.hasItems) return false;
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
                    if (!t.build.block.hasItems) continue;
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
        if (!building.block.hasItems) return;

        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();
        ObjectSet<FactoryGroup> foundGroups = new ObjectSet<>();

        queue.addLast(building);
        visited.add(building);

        while (queue.size > 0) {
            Building current = queue.removeFirst();

            FactoryGroup existing = buildingToGroup.get(current);
            if (existing != null) {
                foundGroups.add(existing);
            }

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
            }
        }

        for (Building b : visited) {
            targetGroup.add(b);
            buildingToGroup.put(b, targetGroup);
        }
    }

    public static void onBuildingRemoved(Building building) {
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
        group.sharedItems.clear();

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
                        if (!t.build.block.hasItems) continue;
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