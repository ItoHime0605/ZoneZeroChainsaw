#!/usr/bin/env python3
"""Build-time source guardrails for ChainsawUpgradeMenu transaction safety.
These complement pure Java recipe matrix tests and the real NeoForge build.
They are not a substitute for testing players clicking the game GUI.
"""
from pathlib import Path
base = Path("src/main/java/tw/zonezero/chainsaw")
menu = (base / "ChainsawUpgradeMenu.java").read_text("utf-8")
rules = (base / "ChainsawUpgradeRules.java").read_text("utf-8")
station = (base / "ChainsawUpgradeStationBlock.java").read_text("utf-8")
mod = (base / "ZoneZeroChainsaw.java").read_text("utf-8")
logic = (base / "logic/UpgradePlanLogic.java").read_text("utf-8")
lang = Path("src/main/resources/assets/zonezero_chainsaw/lang/zh_tw.json").read_text("utf-8")
checks = {
    "strict material kind check": "kind[i] == MATERIAL" in logic and
                                   "quantity[i] == MATERIAL" not in logic,
    "complete six-tier costs": "{3, 6, 27, 3, 3, 3}" in logic,
    "fuel and metal have distinct kinds": "cans == 1" in logic and
                                          "materials == requiredCount" in logic,
    "six-tier initial and max rules": "initialized ? previousTier + 1 : 0" in logic
                                      and "tier <= 5" in logic,
    "all colors supported": "colors[i] > 15" in logic and "selected != originalDye" in logic,
    "default orange distinct from white dye": "currentColor = " in rules and
                                               " : -1;" in rules,
    "reject unknown external items": "BuiltInRegistries.ITEM.containsKey(key)" in rules,
    "material ID checked on input": 'BuiltInRegistries.ITEM.getKey(stack.getItem())' in rules,
    "copy saw and retain custom data": "saw.copyWithCount(1)" in rules and
                                       "ZoneZeroChainsaw.clearRuntimeState(result)" in rules,
    "authoritative server output validation": "Slot(output, 0" in menu and
                                              "public boolean mayPickup(Player player)" in menu and
                                              "ChainsawUpgradeRules.preview" in menu,
    "output remove commits transaction": "public ItemStack remove(int amount)" in menu and
                                         "consumeInputs(result) ? result : ItemStack.EMPTY" in menu,
    "no client item consumption": "if (level.isClientSide) return super.remove(amount)" in menu,
    "block nonpickup result actions": "slotId == 4 && clickType != ClickType.PICKUP" in menu,
    "no result shift-click": "if (index < 5 || index >= slots.size())" in menu,
    "safe input return on close": "input.removeItemNoUpdate(i)" in menu and
                                   "player.getInventory().placeItemBackInInventory(stack)" in menu,
    "preview not returned on close": "output.setItem(0, ItemStack.EMPTY)" in menu,
    "server distance and block validity": "player.distanceToSqr" in menu and
                                          "UPGRADE_STATION.get()" in menu,
    "client actual position synchronized": "data.readBlockPos()" in mod and
                                            "buf -> buf.writeBlockPos(pos)" in station,
    "missing external registry warning": "upgrade.zonezero_chainsaw.missing_material" in lang
}
for label,passed in checks.items():
    assert passed, label
print("PASS:",len(checks),"upgrade-station safety and integration source guards")
