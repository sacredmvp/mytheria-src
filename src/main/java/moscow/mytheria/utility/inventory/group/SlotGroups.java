package moscow.mytheria.utility.inventory.group;

import moscow.mytheria.utility.inventory.group.impl.ArmorSlotsGroup;
import moscow.mytheria.utility.inventory.group.impl.HotbarSlotsGroup;
import moscow.mytheria.utility.inventory.group.impl.InventorySlotsGroup;
import moscow.mytheria.utility.inventory.group.impl.OffhandSlotGroup;
import moscow.mytheria.utility.inventory.slots.ArmorSlot;
import moscow.mytheria.utility.inventory.slots.HotbarSlot;
import moscow.mytheria.utility.inventory.slots.InventorySlot;
import moscow.mytheria.utility.inventory.slots.OffhandSlot;

public class SlotGroups {
   private SlotGroups() {
   }

   public static SlotGroup<HotbarSlot> hotbar() {
      return new HotbarSlotsGroup();
   }

   public static SlotGroup<InventorySlot> inventory() {
      return new InventorySlotsGroup();
   }

   public static SlotGroup<ArmorSlot> armor() {
      return new ArmorSlotsGroup();
   }

   public static SlotGroup<OffhandSlot> offhand() {
      return new OffhandSlotGroup();
   }
}
