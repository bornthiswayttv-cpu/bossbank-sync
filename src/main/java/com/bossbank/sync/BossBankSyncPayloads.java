package com.bossbank.sync;

import java.util.List;
import java.util.Map;

public final class BossBankSyncPayloads
{
    private BossBankSyncPayloads() {}

    public static final class ItemEntry
    {
        public int id;
        public String name;
        public int quantity;
        public int slot;

        public ItemEntry(int id, String name, int quantity, int slot)
        {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.slot = slot;
        }
    }

    public static final class BankPayload
    {
        public String rsn;
        public long syncedAt;
        public List<ItemEntry> bank;
        public List<ItemEntry> inventory;

        public BankPayload(String rsn, long syncedAt, List<ItemEntry> bank, List<ItemEntry> inventory)
        {
            this.rsn = rsn;
            this.syncedAt = syncedAt;
            this.bank = bank;
            this.inventory = inventory;
        }
    }

    public static final class EquipmentPayload
    {
        public String rsn;
        public long syncedAt;
        public List<ItemEntry> equipment;
        public Map<String, ItemEntry> equipmentBySlot;

        public EquipmentPayload(String rsn, long syncedAt, List<ItemEntry> equipment, Map<String, ItemEntry> equipmentBySlot)
        {
            this.rsn = rsn;
            this.syncedAt = syncedAt;
            this.equipment = equipment;
            this.equipmentBySlot = equipmentBySlot;
        }
    }

    public static final class StatsPayload
    {
        public String rsn;
        public long syncedAt;
        public Map<String, Integer> stats;

        public StatsPayload(String rsn, long syncedAt, Map<String, Integer> stats)
        {
            this.rsn = rsn;
            this.syncedAt = syncedAt;
            this.stats = stats;
        }
    }
}
