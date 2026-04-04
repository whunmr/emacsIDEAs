package org.hunmr.options;

import org.hunmr.location.CollectedOutputFileManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.ColorPanel;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class IdeaConfigurable implements Configurable {
    private JPanel _optionsPanel;
    private ColorPanel _firstJumpBackground;
    private ColorPanel _firstJumpForeground;
    private ColorPanel _secondJumpBackground;
    private ColorPanel _secondJumpForeground;
    private JCheckBox _needSelectTextAfterJump;
    private JTextArea _promptHeader;
    private JTextField _promptTmpFileLocation;
    private JButton _showPromptTmpFolderButton;
    private JCheckBox _collectTypesInSelectionProjectOnly;
    private JTextArea _collectTypesInSelectionInclude;
    private JTextArea _collectTypesInSelectionExclude;

    private final PluginConfig config = PluginConfig.getInstance();

    @Nls
    @Override
    public String getDisplayName() {
        return "emacsJump";
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
        _promptHeader.setText(config._promptHeader);
        _collectTypesInSelectionProjectOnly.setSelected(config._collectTypesInSelectionProjectOnly);
        _collectTypesInSelectionInclude.setText(config._collectTypesInSelectionInclude);
        _collectTypesInSelectionExclude.setText(config._collectTypesInSelectionExclude);
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
        _promptHeader = createTextArea();
        _promptTmpFileLocation = createReadOnlyTextField(CollectedOutputFileManager.getOutputDirectoryPath().toString());
        _showPromptTmpFolderButton = createShowFolderButton();
        _collectTypesInSelectionProjectOnly = new JCheckBox();
        _collectTypesInSelectionProjectOnly.setToolTipText("Only collect Go types defined under the current project path.");
        _collectTypesInSelectionInclude = createTextArea();
        _collectTypesInSelectionExclude = createTextArea();

        addRow(0, "First Jump Background:", _firstJumpBackground, null);
        addRow(1, "First Jump Foreground:", _firstJumpForeground, null);
        addRow(2, "Second Jump Background:", _secondJumpBackground, null);
        addRow(3, "Second Jump Foreground:", _secondJumpForeground, null);
        addRow(4, "Select Moved Text:", _needSelectTextAfterJump,
                "Select moved or copied text after commands like AceJump move/copy.");
        addRow(5, "Prompt Header:", wrapScroll(_promptHeader),
                "Text inserted at the beginning of each new collected prompt file.");
        addRow(6, "Prompt tmp file location:", createPromptTmpFileLocationPanel(),
                "The directory where collected prompt tmp files are stored.");
        addRow(7, "Collect Types Project Only:", _collectTypesInSelectionProjectOnly,
                "Only collect Go types defined under the current project path.");
        addRow(8, "Collect Types Include:", wrapScroll(_collectTypesInSelectionInclude),
                "Only collect Go types whose package contains one of these lines. Empty means include all.");
        addRow(9, "Collect Types Exclude:", wrapScroll(_collectTypesInSelectionExclude),
                "Skip Go types whose package contains one of these lines.");

        GridBagConstraints spacer = new GridBagConstraints();
        spacer.gridx = 0;
        spacer.gridy = 10;
        spacer.weighty = 1.0;
        spacer.fill = GridBagConstraints.VERTICAL;
        _optionsPanel.add(Box.createVerticalGlue(), spacer);
    }

    private ColorPanel createColorPanel() {
        ColorPanel panel = new ColorPanel();
        panel.setPreferredSize(new Dimension(100, panel.getPreferredSize().height));
        return panel;
    }

    private JTextArea createTextArea() {
        JTextArea textArea = new JTextArea(4, 36);
        textArea.setLineWrap(false);
        textArea.setWrapStyleWord(false);
        return textArea;
    }

    private JTextField createReadOnlyTextField(String text) {
        JTextField textField = new JTextField(text, 30);
        textField.setEditable(false);
        textField.setCaretPosition(0);
        return textField;
    }

    private JButton createShowFolderButton() {
        JButton button = new JButton("Show Folder");
        button.addActionListener(event -> showPromptTmpFolder());
        return button;
    }

    private JComponent createPromptTmpFileLocationPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.add(_promptTmpFileLocation, BorderLayout.CENTER);
        panel.add(_showPromptTmpFolderButton, BorderLayout.EAST);
        return panel;
    }

    private JScrollPane wrapScroll(JTextArea textArea) {
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(340, 72));
        return scrollPane;
    }

    private void showPromptTmpFolder() {
        Path outputDirectory = CollectedOutputFileManager.getOutputDirectoryPath();
        try {
            Files.createDirectories(outputDirectory);
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(outputDirectory.toFile());
                return;
            }
            Messages.showInfoMessage("Desktop folder opening is not supported on this platform.", "emacsJump");
        } catch (IOException exception) {
            Messages.showErrorDialog("Failed to open prompt tmp file location:\n" + outputDirectory + "\n\n" + exception.getMessage(), "emacsJump");
        }
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
        ensureUiInitialized();
        return !Objects.equals(_firstJumpBackground.getSelectedColor(), config.getFirstJumpBackground())
                || !Objects.equals(_firstJumpForeground.getSelectedColor(), config.getFirstJumpForeground())
                || !Objects.equals(_secondJumpBackground.getSelectedColor(), config.getSecondJumpBackground())
                || !Objects.equals(_secondJumpForeground.getSelectedColor(), config.getSecondJumpForeground())
                || _needSelectTextAfterJump.isSelected() != config._needSelectTextAfterJump
                || !Objects.equals(_promptHeader.getText(), config._promptHeader)
                || _collectTypesInSelectionProjectOnly.isSelected() != config._collectTypesInSelectionProjectOnly
                || !Objects.equals(_collectTypesInSelectionInclude.getText(), config._collectTypesInSelectionInclude)
                || !Objects.equals(_collectTypesInSelectionExclude.getText(), config._collectTypesInSelectionExclude);
    }

    @Override
    public void apply() throws ConfigurationException {
        ensureUiInitialized();
        if (!isModified()) {
            return;
        }

        config._firstJumpBackground = _firstJumpBackground.getSelectedColor().getRGB();
        config._firstJumpForeground = _firstJumpForeground.getSelectedColor().getRGB();
        config._secondJumpBackground = _secondJumpBackground.getSelectedColor().getRGB();
        config._secondJumpForeground = _secondJumpForeground.getSelectedColor().getRGB();
        config._needSelectTextAfterJump = _needSelectTextAfterJump.isSelected();
        config._promptHeader = _promptHeader.getText();
        config._collectTypesInSelectionProjectOnly = _collectTypesInSelectionProjectOnly.isSelected();
        config._collectTypesInSelectionInclude = _collectTypesInSelectionInclude.getText();
        config._collectTypesInSelectionExclude = _collectTypesInSelectionExclude.getText();
    }

    @Override
    public void reset() {
        ensureUiInitialized();
        setFromConfig();
    }

    @Override
    public void disposeUIResources() {
    }
}
