package rrr;

import mindustry.mod.Mod;
import rrr.content.blocks.chaowall;   // 导入你的方块类

public class Main extends Mod {
    @Override
    public void loadContent() {
        new chaowall();           // 实例化即注册
    }
}