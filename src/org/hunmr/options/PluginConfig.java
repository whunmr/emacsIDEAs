package org.hunmr.options;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

@State(
        name = "emacsIDEAsPluginConfig",
        storages = @Storage(value = "emacsIDEAs_plugin.xml", roamingType = RoamingType.DISABLED)
)
public class PluginConfig implements PersistentStateComponent<PluginConfig> {
    public int _firstJumpBackground = Color.BLUE.getRGB();
    public int _firstJumpForeground = Color.WHITE.getRGB();
    public int _secondJumpBackground = Color.RED.getRGB();
    public int _secondJumpForeground = Color.WHITE.getRGB();
    public boolean _needSelectTextAfterJump = true;

    public Color getFirstJumpBackground() {
        return new Color(_firstJumpBackground);
    }

    public Color getFirstJumpForeground() {
        return new Color(_firstJumpForeground);
    }

    public Color getSecondJumpBackground() {
        return new Color(_secondJumpBackground);
    }

    public Color getSecondJumpForeground() {
        return new Color(_secondJumpForeground);
    }

    @Nullable
    @Override
    public PluginConfig getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull PluginConfig config) {
        XmlSerializerUtil.copyBean(config, this);
    }

    public static PluginConfig getInstance() {
        return ApplicationManager.getApplication().getService(PluginConfig.class);
    }
}
