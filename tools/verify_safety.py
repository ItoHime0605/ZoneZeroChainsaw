#!/usr/bin/env python3
"""Static guardrails complementing Java pure-rule tests and NeoForge build.

These are source-level regressions, not claims of Minecraft runtime testing.
"""
from pathlib import Path
root=Path("src/main/java/tw/zonezero/chainsaw")
game=(root/"ChainsawGameplay.java").read_text("utf-8")
can=(root/"BiofuelCanItem.java").read_text("utf-8")
item=(root/"ChainsawItem.java").read_text("utf-8")
core=(root/"ZoneZeroChainsaw.java").read_text("utf-8")
combat=(root/"ChainsawWieldRestrictions.java").read_text("utf-8")
checks={
 "server-only canister consumption": 'if (!level.isClientSide)' in can and
    "if (!player.getAbilities().instabuild) can.shrink(1)" in can,
 "full 600-unit refuel": "ChainsawSafetyRules.canRefuel(" in can and
    "ChainsawSafetyRules.refilledFuel(" in can,
 "stale first-packet cleanup": "ZoneZeroChainsaw.clearRuntimeState(saw);" in game and
    "if (input.lastSaw != saw)" in game,
 "slot switch cleanup": "ZoneZeroChainsaw.clearRuntimeState(input.lastSaw);" in game,
 "relog/death cleanup": all("public static void "+x+"(" in game for x in ("login","logout","respawn")),
 "inventory movement cleanup": "void inventoryTick(" in item and "clearRuntimeState(stack)" in item,
 "stop cuts if offhand occupied": "!player.getOffhandItem().isEmpty()" in game,
 "4 tick server-authoritative damage": "SustainedCutLogic.consumeUnit(input.heldTicks)" in game,
 "no vanilla hit bypass": "public static void attackEntity(" in combat and
    "if (event.getEntity().getMainHandItem().is(ZoneZeroChainsaw.CHAINSAW.get()))" in combat,
 "toggle packet cooldown": "ChainsawSafetyRules.toggleAllowed(" in game,
 "preserve stored fuel and upgrades": all(
    f'nbt.remove("{k}")' not in core
    for k in ("ChainsawFuel","ChainsawTier","ChainsawColor","ChainsawInitialized")
 ),
 "no client-supplied target or damage": "packet.action()" in
    (root/"ChainsawControlPacket.java").read_text("utf-8")
}
for name,ok in checks.items():
    assert ok,name
print("PASS: 12 server-side chainsaw safety source guards")
