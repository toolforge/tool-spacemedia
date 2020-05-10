package org.wikimedia.commons.donvip.spacemedia.data.commons.api;

import java.util.Map;

public class Revision {

    private Map<String, Slot> slots;

    public Map<String, Slot> getSlots() {
        return slots;
    }

    public void setSlots(Map<String, Slot> slots) {
        this.slots = slots;
    }

    @Override
    public String toString() {
        return "Revision [slots=" + slots + "]";
    }
}
