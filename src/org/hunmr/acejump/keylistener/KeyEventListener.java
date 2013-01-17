package org.hunmr.acejump.keylistener;

import org.hunmr.acejump.keylistener.KeyEventDispatcher;

import java.awt.event.KeyEvent;

public abstract class KeyEventListener<T> {
    public void keyTyped(KeyEvent keyEvent, KeyEventDispatcher<T> tKeyEventDispatcher, T paramObject) {
    }

    public void keyPressed(KeyEvent keyEvent, KeyEventDispatcher<T> tKeyEventDispatcher, T paramObject) {
    }

    public void keyReleased(KeyEvent keyEvent, KeyEventDispatcher<T> tKeyEventDispatcher, T paramObject) {
    }
}
