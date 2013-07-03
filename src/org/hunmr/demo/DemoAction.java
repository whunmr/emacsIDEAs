package org.hunmr.demo;

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.hunmr.common.EmacsIdeasAction;

import java.io.IOException;

public class DemoAction extends EmacsIdeasAction {
    @Override
    public void actionPerformed(AnActionEvent e) {
        if (super.initAction(e)) {
            createDemoFile();
            cleanupSetupsInAndBackToNormalEditingMode();
        }
    }

    private void createDemoFile() {
        try {
            Runtime.getRuntime().exec("/Users/twer/idea/create_file_demo.sh");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
