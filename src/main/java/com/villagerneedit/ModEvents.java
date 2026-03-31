package com.villagerneedit;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(modid = VillagerNeedIt.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ModEvents {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModEvents.class);
    
    // 食物消耗数量常量
    private static final int BREAD_AMOUNT = 4;
    private static final int POTATO_AMOUNT = 7;
    private static final int CARROT_AMOUNT = 7;

    private static long lastCheckedDay = -1;

    private static int timeLogCounter = 0;
    
    /**
     * 监听服务器 tick 事件
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        
        // 获取时间
        long dayTime = event.getServer().overworld().getDayTime() % 24000L;
        long gameTime = event.getServer().overworld().getDayTime();
        long currentDay = gameTime / 24000L;
        
        // 输出时间日志
        // timeLogCounter++;
        // if (timeLogCounter >= 60) {
        //     timeLogCounter = 0;
        //     LOGGER.info("[TimeLog] getDayTime()={}, currentDay={}", gameTime, currentDay);
        // }
        

        if (lastCheckedDay != currentDay) {
            lastCheckedDay = currentDay;
            event.getServer().execute(() -> {
                event.getServer().getAllLevels().forEach(level -> {
                    List<Villager> villagers = new java.util.ArrayList<>();
                    for (net.minecraft.world.entity.Entity entity : level.getAllEntities()) {
                        if (entity instanceof Villager) {
                            villagers.add((Villager) entity);
                        }
                    }

                    for (Villager villager : villagers) {
                        checkVillagerFood(villager);
                    }
                });
            });
        }
    }
    
    /**
     * 检查村民的食物修改 RestocksToday
     */
    private static void checkVillagerFood(Villager villager) {
        // LOGGER.info("Checking food for villager at ({}, {}, {})", villager.getX(), villager.getY(), villager.getZ());

        boolean hasFood = hasEnoughFood(villager);
        
        if (hasFood) {
            consumeFood(villager);
            villager.removeAllEffects();
            // LOGGER.info("Villager has enough food, food consumed");
        } else {
            villager.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 6000, 1));
            // LOGGER.info("Villager does not have enough food, given slowness effect");
        }
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
        villager.saveWithoutId(nbt);
        int currentRestocks = nbt.getInt("RestocksToday");
        // LOGGER.info("Current RestocksToday: {}", currentRestocks);
        if (hasFood) {
            int newRestocks = 0;
            nbt.putInt("RestocksToday", newRestocks);
        } else {
            nbt.putInt("RestocksToday", 2);
        }
        villager.load(nbt);
        
        if (hasFood) {
            // LOGGER.info("Villager has enough food, RestocksToday changed from {} to 0", currentRestocks);
        } else {
            // LOGGER.info("Villager does not have enough food, RestocksToday set to 2 (was {})", currentRestocks);
        }
    }
    
    /**
     * 检查村民是否有足够的食物
     */
    private static boolean hasEnoughFood(Villager villager) {
        int breadCount = 0;
        int potatoCount = 0;
        int carrotCount = 0;
        for (int i = 0; i < villager.getInventory().getContainerSize(); i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (stack.getItem() == Items.BREAD) {
                breadCount += stack.getCount();
            } else if (stack.getItem() == Items.POTATO) {
                potatoCount += stack.getCount();
            } else if (stack.getItem() == Items.CARROT) {
                carrotCount += stack.getCount();
            }
        }
        return breadCount >= BREAD_AMOUNT || potatoCount >= POTATO_AMOUNT || carrotCount >= CARROT_AMOUNT;
    }
    private static void consumeFood(Villager villager) {
        if (consumeItem(villager, Items.BREAD, BREAD_AMOUNT)) {
            return;
        }
        if (consumeItem(villager, Items.POTATO, POTATO_AMOUNT)) {
            return;
        }
        consumeItem(villager, Items.CARROT, CARROT_AMOUNT);
    }

    private static boolean consumeItem(Villager villager, Item item, int amount) {
        int remaining = amount;
        for (int i = 0; i < villager.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = villager.getInventory().getItem(i);
            if (stack.getItem() == item) {
                int take = Math.min(stack.getCount(), remaining);
                stack.shrink(take);
                remaining -= take;
            }
        }
        return remaining == 0;
    }
}