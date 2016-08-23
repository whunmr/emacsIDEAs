package org.hunmr.options;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.ColorPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class IdeaConfigurable implements Configurable {
    private JPanel optionsPanel;
    private ColorPanel color1;
    private ColorPanel color2;
    private ColorPanel color3;
    private ColorPanel color4;

    //bool,  select moved/coped text after jump

    //color  background color of first jump mark char
    //color  foreground color of first jump mark char

    //color  background color of second jump mark char
    //color  foreground color of second jump mark char

    @Nls
    @Override
    public String getDisplayName() {
        return "emacsIDEAs";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "preferences.topic";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return optionsPanel;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    @Override
    public void apply() throws ConfigurationException {
    }

    @Override
    public void reset() {

    }

    @Override
    public void disposeUIResources() {

    }
}
