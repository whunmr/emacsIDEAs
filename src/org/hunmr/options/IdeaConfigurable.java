package org.hunmr.options;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.ColorPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;

public class IdeaConfigurable implements Configurable {
    private JPanel _optionsPanel;
    private ColorPanel _firstJumpBackground;
    private ColorPanel _firstJumpForeground;
    private ColorPanel _secondJumpBackground;
    private ColorPanel _secondJumpForeground;
    private JCheckBox _needSelectTextAfterJump;

    private final PluginConfig config = PluginConfig.getInstance();

    @Nls
    @Override
    public String getDisplayName() {
        return "emacsIDEAs";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    private void setFromConfig() {
        _firstJumpBackground.setSelectedColor(config.getFirstJumpBackground());
        _firstJumpForeground.setSelectedColor(config.getFirstJumpForeground());
        _secondJumpBackground.setSelectedColor(config.getSecondJumpBackground());
        _secondJumpForeground.setSelectedColor(config.getSecondJumpForeground());
        _needSelectTextAfterJump.setSelected(config._needSelectTextAfterJump);
    }

    private void ensureUiInitialized() {
        if (_optionsPanel != null) {
            return;
        }

        _optionsPanel = new JPanel(new GridBagLayout());
        _optionsPanel.setBorder(new EmptyBorder(12, 12, 12, 12));

        _firstJumpBackground = createColorPanel();
        _firstJumpForeground = createColorPanel();
        _secondJumpBackground = createColorPanel();
        _secondJumpForeground = createColorPanel();
        _needSelectTextAfterJump = new JCheckBox();
        _needSelectTextAfterJump.setToolTipText("Select moved or copied text after a jump command completes.");

        addRow(0, "First Jump Background:", _firstJumpBackground, null);
        addRow(1, "First Jump Foreground:", _firstJumpForeground, null);
        addRow(2, "Second Jump Background:", _secondJumpBackground, null);
        addRow(3, "Second Jump Foreground:", _secondJumpForeground, null);
        addRow(4, "Select Moved Text:", _needSelectTextAfterJump,
                "Select moved or copied text after commands like AceJump move/copy.");

        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridx = 0;
        spacer.gridy = 5;
        spacer.weighty = 1.0;
        spacer.fill = GridBagConstraints.VERTICAL;
        _optionsPanel.add(Box.createVerticalGlue(), spacer);
    }

    private ColorPanel createColorPanel() {
        ColorPanel panel = new ColorPanel();
        panel.setPreferredSize(new Dimension(100, panel.getPreferredSize().height));
        return panel;
    }

    private void addRow(int row, String labelText, JComponent component, @Nullable String toolTipText) {
        JLabel label = new JLabel(labelText);
        label.setLabelFor(component);
        if (toolTipText != null) {
            label.setToolTipText(toolTipText);
            component.setToolTipText(toolTipText);
        }

        GridBagConstraints labelConstraints = new GridBagConstraints();
        labelConstraints.gridx = 0;
        labelConstraints.gridy = row;
        labelConstraints.anchor = GridBagConstraints.WEST;
        labelConstraints.insets = new Insets(0, 0, 8, 12);
        _optionsPanel.add(label, labelConstraints);

        GridBagConstraints componentConstraints = new GridBagConstraints();
        componentConstraints.gridx = 1;
        componentConstraints.gridy = row;
        componentConstraints.anchor = GridBagConstraints.WEST;
        componentConstraints.insets = new Insets(0, 0, 8, 0);
        _optionsPanel.add(component, componentConstraints);
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        ensureUiInitialized();
        setFromConfig();
        return _optionsPanel;
    }

    @Override
    public boolean isModified() {
        return !Objects.equals(_firstJumpBackground.getSelectedColor(), config.getFirstJumpBackground())
                || !Objects.equals(_firstJumpForeground.getSelectedColor(), config.getFirstJumpForeground())
                || !Objects.equals(_secondJumpBackground.getSelectedColor(), config.getSecondJumpBackground())
                || !Objects.equals(_secondJumpForeground.getSelectedColor(), config.getSecondJumpForeground())
                || _needSelectTextAfterJump.isSelected() != config._needSelectTextAfterJump;
    }

    @Override
    public void apply() throws ConfigurationException {
        if (!isModified()) {
            return;
        }

        config._firstJumpBackground = _firstJumpBackground.getSelectedColor().getRGB();
        config._firstJumpForeground = _firstJumpForeground.getSelectedColor().getRGB();
        config._secondJumpBackground = _secondJumpBackground.getSelectedColor().getRGB();
        config._secondJumpForeground = _secondJumpForeground.getSelectedColor().getRGB();
        config._needSelectTextAfterJump = _needSelectTextAfterJump.isSelected();
    }

    @Override
    public void reset() {
        setFromConfig();
    }

    @Override
    public void disposeUIResources() {
    }
}
