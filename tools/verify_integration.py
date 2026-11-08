#!/usr/bin/env python3
"""Check compiled integration boundaries; not a Minecraft gameplay test."""
from pathlib import Path
from zipfile import ZipFile
import json, re

root=Path("src/main/java/tw/zonezero/chainsaw")
read=lambda name:(root/(name+".java")).read_text("utf-8")
toml=Path("src/main/resources/META-INF/neoforge.mods.toml").read_text("utf-8")
gradle=Path("build.gradle").read_text("utf-8")
client=("ChainsawClientInput","ChainsawClientKeys","ChainsawHud","ChainsawPlayerAnimator",
        "ChainsawTwoHandedClient","ChainsawUpgradeClient","ClientItemModels")
common=("ZoneZeroChainsaw","ChainsawGameplay","ChainsawControlPacket",
        "ChainsawUpgradeMenu","ChainsawUpgradeRules","ChainsawItem")
for name in client:
    code=read(name)
    assert "Dist.CLIENT" in code, "Missing client-only registration: "+name
    for forbidden in ("RenderSystem.setShaderColor","glColor4","PlayerRendererMixin",
                      "HumanoidModelMixin","RenderPlayerEvent","com.tacz."):
        assert forbidden not in code,(name,forbidden)
for name in common:
    code=read(name)
    assert "import net.minecraft.client." not in code,name
    assert "import dev.kosmx.playerAnim." not in code,name
assert 'modId="playeranimator"' in toml
assert 'side="CLIENT"' in toml.split('modId="playeranimator"')[1]
assert "player-animation-lib-forge:2.0.4+1.21.1" in gradle
assert "compileOnly" in gradle
assert 'modId="tacz"' not in toml
assert "context.enqueueWork(" in read("ChainsawControlPacket")
assert "packet.action()" in read("ChainsawControlPacket")
assert "GLFW.GLFW_KEY_R" in read("ChainsawClientKeys")
assert "if (active) PacketDistributor.sendToServer" in read("ChainsawClientInput")
assert "mc.player.isAlive() && !mc.player.isSpectator()" in read("ChainsawClientInput")
anim=read("ChainsawPlayerAnimator")
assert "return mayAnimateSaw() &&" in anim
assert "oldHold = hold = 0.0f" in anim
assert "FirstPersonMode.NONE" in anim
assert "getMainHandItem().is(ZoneZeroChainsaw.CHAINSAW.get())" in anim
assert "player.getOffhandItem().isEmpty()" in anim
assert "type != TransformType.ROTATION" in anim
assert "rightArm" in anim and "leftArm" in anim
assert "isMainHandSaw(stack, entity)" in read("ClientItemModels")
assert "ItemStack.isSameItemSameComponents" in read("ChainsawTwoHandedClient")
hud=read("ChainsawHud")
assert "running ? BAR_RUNNING : BAR_IDLE" in hud and "ChainsawHudStyle.TEXT_COLOR" in hud
assert "g.fill(" not in hud
game=read("ChainsawGameplay")
for s in ("changedDimension(","login(","logout(","respawn(","clearInventoryRuntime(player)",
          "SustainedCutLogic.consumeUnit(input.heldTicks)","findTarget(player)"):
    assert s in game,s
jarfiles=[p for p in Path("build/libs").glob("zonezero-chainsaw-*.jar") if "-sources" not in p.name]
assert len(jarfiles)==1,jarfiles
jarfile=jarfiles[0]
version=re.search(r'version="([^"]+)"',toml).group(1)
assert version in jarfile.name and version in gradle
with ZipFile(jarfile) as jar:
    assert jar.testzip() is None
    names=set(jar.namelist())
    for name in (*client,*common):
        cls="tw/zonezero/chainsaw/"+name+".class"
        assert cls in names,cls
    for name in names:
        assert not name.startswith(("dev/kosmx/","com/tacz/")),name
    for name in common:
        binary=jar.read("tw/zonezero/chainsaw/"+name+".class")
        assert b"net/minecraft/client/" not in binary, name
        assert b"dev/kosmx/playerAnim/" not in binary, name
    for color in "white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black".split():
        assert "assets/zonezero_chainsaw/textures/item/housing_"+color+".png" in names
        for state in ("running","cutting","coast_fast","coast_medium","coast_slow"):
            assert "assets/zonezero_chainsaw/models/item/chainsaw_"+color+"_"+state+".json" in names
    for state in ("idle","running"):
        assert "assets/zonezero_chainsaw/textures/gui/guide_"+state+".png" in names
    for name in ("start","engine","cut","contact","stop","stall"):
        assert jar.read("assets/zonezero_chainsaw/sounds/"+name+".ogg").startswith(b"OggS")
    for lang in ("zh_tw","en_us"):
        values=json.loads(jar.read("assets/zonezero_chainsaw/lang/"+lang+".json"))
        assert "upgrade.zonezero_chainsaw.missing_material" in values
        assert "tooltip.zonezero_chainsaw.description" in values
print("PASS: server-class isolation, client-only animator, optional TaCZ,")
print("      scope-safe key/packet/render, 16 dyes, HUD, six sounds, JAR:",jarfile.name)
