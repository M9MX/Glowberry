@echo off
set JAR=c:\Users\justi\Desktop\Minecraft Coding\GlowberryAddon (Cactus)\.gradle\loom-cache\remapped_mods\remapped\maven\modrinth\cactus-4abd26ae\0.12\cactus-4abd26ae-0.12.jar

echo ============================================================
echo 1. JAR contents related to "settings"
echo ============================================================
jar tf "%JAR%" | findstr /i "settings"

echo.
echo ============================================================
echo 2. IntegerSetting
echo ============================================================
javap -cp "%JAR%" com.dwarslooper.cactus.client.systems.config.settings.impl.IntegerSetting

echo.
echo ============================================================
echo 3. ColorSetting
echo ============================================================
javap -cp "%JAR%" com.dwarslooper.cactus.client.systems.config.settings.impl.ColorSetting

echo.
echo ============================================================
echo 4. BooleanSetting
echo ============================================================
javap -cp "%JAR%" com.dwarslooper.cactus.client.systems.config.settings.impl.BooleanSetting

echo.
echo ============================================================
echo 5. Module
echo ============================================================
javap -cp "%JAR%" com.dwarslooper.cactus.client.feature.module.Module

echo.
echo ============================================================
echo 6. SettingGroup
echo ============================================================
javap -cp "%JAR%" com.dwarslooper.cactus.client.systems.config.settings.group.SettingGroup

echo.
echo ============================================================
echo 7. Setting
echo ============================================================
javap -cp "%JAR%" com.dwarslooper.cactus.client.systems.config.settings.impl.Setting

pause
