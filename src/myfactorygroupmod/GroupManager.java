package myfactorygroupmod;

import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Queue;
import mindustry.Vars;
import mindustry.gen.Building;
import mindustry.world.Tile;

public class GroupManager {

    private static final ObjectMap<Building, FactoryGroup> buildingToGroup = new ObjectMap<>();

    public static FactoryGroup getGroup(Building building) {
        return buildingToGroup.get(building);
    }

    public static void onBuildingPlaced(Building building) {
        if (!building.block.hasItems) return;
        if (buildingToGroup.containsKey(building)) return;

        Queue<Building> queue = new Queue<>();
        ObjectSet<Building> visited = new ObjectSet<>();
        ObjectSet<FactoryGroup> foundGroups = new ObjectSet<>();

        queue.addLast(building);
        visited.add(building);

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (queue.size > 0) {
            Building current = queue.removeFirst();

            FactoryGroup existing = buildingToGroup.get(current);
            if (existing != null) {
                foundGroups.add(existing);
            }

            for (int i = 0; i < 4; i++) {
                int nx = current.tile.x + dx[i];
                int ny = current.tile.y + dy[i];

                if (nx < 0 || ny < 0 || nx >= Vars.world.width() || ny >= Vars.world.height()) continue;

                Tile neighbor = Vars.world.tile(nx, ny);
                if (neighbor == null || neighbor.build == null) continue;
                if (!neighbor.build.block.hasItems) continue;
                if (visited.contains(neighbor.build)) continue;

                visited.add(neighbor.build);
                queue.addLast(neighbor.build);
            }
        }

        FactoryGroup targetGroup;
        if (foundGroups.size == 0) {
            targetGroup = new FactoryGroup();
        } else {
            targetGroup = foundGroups.first();
            // 合并其他群
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

        int[] dx = {1, -1, 0, 0};
        int[] dy = {0, 0, 1, -1};

        while (queue.size > 0) {
            Building current = queue.removeFirst();
            newGroup.add(current);
            buildingToGroup.put(current, newGroup);

            for (int i = 0; i < 4; i++) {
                int nx = current.tile.x + dx[i];
                int ny = current.tile.y + dy[i];

                if (nx < 0 || ny < 0 || nx >= Vars.world.width() || ny >= Vars.world.height()) continue;

                Tile neighbor = Vars.world.tile(nx, ny);
                if (neighbor == null || neighbor.build == null) continue;
                if (!neighbor.build.block.hasItems) continue;
                if (visited.contains(neighbor.build)) continue;
                if (!candidates.contains(neighbor.build)) continue;

                visited.add(neighbor.build);
                queue.addLast(neighbor.build);
            }
        }
    }
}