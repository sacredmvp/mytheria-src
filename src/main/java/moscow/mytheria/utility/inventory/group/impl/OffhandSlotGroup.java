package moscow.mytheria.utility.inventory.group.impl;

import java.util.List;
import moscow.mytheria.utility.inventory.group.SlotGroup;
import moscow.mytheria.utility.inventory.slots.OffhandSlot;

public class OffhandSlotGroup extends SlotGroup<OffhandSlot> {
   public OffhandSlotGroup() {
      super(List.of(new OffhandSlot()));
   }
}
