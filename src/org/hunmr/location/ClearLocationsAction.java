package org.hunmr.location;

import com.intellij.codeInsight.hint.HintManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.DumbAwareAction;
import org.hunmr.util.ClipboardEditorUtil;

public class ClearLocationsAction extends DumbAwareAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        ClipboardEditorUtil.clearClipboard();

        if (e != null && e.getData(CommonDataKeys.EDITOR) != null) {
            HintManager.getInstance().showInformationHint(e.getData(CommonDataKeys.EDITOR), "Collected locations cleared");
        }
    }
}
