package com.bossbank.sync;

import com.bossbank.sync.BossBankSyncPayloads.BankPayload;
import com.bossbank.sync.BossBankSyncPayloads.EquipmentPayload;
import com.bossbank.sync.BossBankSyncPayloads.ItemEntry;
import com.bossbank.sync.BossBankSyncPayloads.StatsPayload;
import com.google.inject.Provides;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.GameState;
import net.runelite.api.InventoryID;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Skill;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

@Slf4j
@PluginDescriptor(
    name = "BossBank Sync",
    description = "Syncs bank, inventory, equipment, and stats to BossBank",
    tags = {"bossbank", "bank", "sync", "gear", "inventory"}
)
public class BossBankSyncPlugin extends Plugin
{
    private static final String BANK_SYNC_PATH = "/api/sync/bank";
    private static final String EQUIPMENT_SYNC_PATH = "/api/sync/equipment";
    private static final String STATS_SYNC_PATH = "/api/sync/stats";

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private BossBankSyncConfig config;

    @Inject
    private BossBankSyncHttpClient httpClient;

    @Provides
    BossBankSyncConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(BossBankSyncConfig.class);
    }

    @Override
    protected void startUp()
    {
        log.info("BossBank Sync started");
    }

    @Override
    protected void shutDown()
    {
        log.info("BossBank Sync stopped");
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged event)
    {
        if (event.getGameState() == GameState.LOGGED_IN && config.autoSyncOnLogin())
        {
            clientThread.invokeLater(this::syncAll);
        }
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event)
    {
        if (!config.autoSyncOnBankOpen())
        {
            return;
        }

        ItemContainer itemContainer = event.getItemContainer();
        if (itemContainer == null)
        {
            return;
        }

        if (itemContainer.getId() == InventoryID.BANK.getId())
        {
            clientThread.invokeLater(this::syncAll);
        }
    }

    public void syncAll()
    {
        if (client.getGameState() != GameState.LOGGED_IN || client.getLocalPlayer() == null)
        {
            log.warn("BossBank Sync: not logged in");
            return;
        }

        syncBankAndInventory();
        syncEquipment();
        syncStats();
    }

    private void syncBankAndInventory()
    {
        String rsn = client.getLocalPlayer().getName();
        long syncedAt = System.currentTimeMillis();

        List<ItemEntry> bankItems = readContainer(InventoryID.BANK);
        List<ItemEntry> inventoryItems = config.includeInventory() ? readContainer(InventoryID.INVENTORY) : List.of();

        BankPayload payload = new BankPayload(rsn, syncedAt, bankItems, inventoryItems);
        httpClient.postJson(BANK_SYNC_PATH, payload);

        if (config.debugLogging())
        {
            log.info("BossBank Sync: synced bank={} inventory={}", bankItems.size(), inventoryItems.size());
        }
    }

    private void syncEquipment()
    {
        String rsn = client.getLocalPlayer().getName();
        long syncedAt = System.currentTimeMillis();

        ItemContainer worn = client.getItemContainer(InventoryID.EQUIPMENT);
        List<ItemEntry> equipmentItems = new ArrayList<>();
        Map<String, ItemEntry> equipmentBySlot = new HashMap<>();

        if (worn != null)
        {
            Item[] items = worn.getItems();
            for (EquipmentInventorySlot slot : EquipmentInventorySlot.values())
            {
                int idx = slot.getSlotIdx();
                if (idx < 0 || idx >= items.length)
                {
                    continue;
                }

                Item item = items[idx];
                if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
                {
                    continue;
                }

                String itemName = resolveItemName(item.getId());
                ItemEntry entry = new ItemEntry(item.getId(), itemName, item.getQuantity(), idx);

                equipmentItems.add(entry);
                equipmentBySlot.put(slot.name().toLowerCase(), entry);
            }
        }

        EquipmentPayload payload = new EquipmentPayload(rsn, syncedAt, equipmentItems, equipmentBySlot);
        httpClient.postJson(EQUIPMENT_SYNC_PATH, payload);

        if (config.debugLogging())
        {
            log.info("BossBank Sync: synced equipment={}", equipmentItems.size());
        }
    }

    private void syncStats()
    {
        String rsn = client.getLocalPlayer().getName();
        long syncedAt = System.currentTimeMillis();

        Map<String, Integer> out = new HashMap<>();

        addSkill(out, Skill.ATTACK);
        addSkill(out, Skill.STRENGTH);
        addSkill(out, Skill.DEFENCE);
        addSkill(out, Skill.HITPOINTS);
        addSkill(out, Skill.RANGED);
        addSkill(out, Skill.PRAYER);
        addSkill(out, Skill.MAGIC);
        addSkill(out, Skill.SLAYER);
        addSkill(out, Skill.HERBLORE);
        addSkill(out, Skill.AGILITY);

        StatsPayload payload = new StatsPayload(rsn, syncedAt, out);
        httpClient.postJson(STATS_SYNC_PATH, payload);

        if (config.debugLogging())
        {
            log.info("BossBank Sync: synced stats={}", out);
        }
    }

    private void addSkill(Map<String, Integer> out, Skill skill)
    {
        out.put(skill.name().toLowerCase(), client.getRealSkillLevel(skill));
    }

    private List<ItemEntry> readContainer(InventoryID inventoryID)
    {
        ItemContainer container = client.getItemContainer(inventoryID);
        List<ItemEntry> results = new ArrayList<>();

        if (container == null)
        {
            return results;
        }

        Item[] items = container.getItems();
        for (int slot = 0; slot < items.length; slot++)
        {
            Item item = items[slot];
            if (item == null || item.getId() <= 0 || item.getQuantity() <= 0)
            {
                continue;
            }

            results.add(new ItemEntry(
                item.getId(),
                resolveItemName(item.getId()),
                item.getQuantity(),
                slot
            ));
        }

        return results;
    }

    private String resolveItemName(int itemId)
    {
        return "item_" + itemId;
    }
}
