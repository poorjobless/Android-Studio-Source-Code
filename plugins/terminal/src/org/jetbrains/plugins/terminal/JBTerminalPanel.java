/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* -*-mode:java; c-basic-offset:2; -*- */


package org.jetbrains.plugins.terminal;

import com.google.common.base.Predicate;
import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.keymap.Keymap;
import com.intellij.openapi.keymap.KeymapManager;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.util.JBHiDPIScaledImage;
import com.intellij.util.RetinaImage;
import com.intellij.util.ui.UIUtil;
import com.jediterm.terminal.display.BackBuffer;
import com.jediterm.terminal.display.StyleState;
import com.jediterm.terminal.ui.TerminalPanel;
import com.jediterm.terminal.ui.settings.SettingsProvider;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.IOException;

public class JBTerminalPanel extends TerminalPanel {
  public JBTerminalPanel(@NotNull SettingsProvider settingsProvider,
                         @NotNull BackBuffer backBuffer,
                         @NotNull StyleState styleState) {
    super(settingsProvider, backBuffer, styleState);

    JBTabbedTerminalWidget.convertActions(this, getActions(), new Predicate<KeyEvent>() {
      @Override
      public boolean apply(KeyEvent input) {
        JBTerminalPanel.this.handleKeyEvent(input);
        return true;
      }
    });

    registerKeymapActions(this);
  }

  private static void registerKeymapActions(final TerminalPanel terminalPanel) {
    Keymap keymap = KeymapManager.getInstance().getActiveKeymap();
    String[] actionIds = keymap.getActionIds();

    ActionManager actionManager = ActionManager.getInstance();
    for (String actionId : actionIds) {
      final AnAction action = actionManager.getAction(actionId);
      if (action != null) {
        AnAction a = new DumbAwareAction() {
          @Override
          public void actionPerformed(AnActionEvent e) {
            if (e.getInputEvent() instanceof KeyEvent) {
              action.update(e);
              if (e.getPresentation().isEnabled()) {
                action.actionPerformed(e);
              }
              else {
                terminalPanel.handleKeyEvent((KeyEvent)e.getInputEvent());
              }

              e.getInputEvent().consume();
            }
          }
        };
        for (Shortcut sc : action.getShortcutSet().getShortcuts()) {
          if (sc.isKeyboard() && sc instanceof KeyboardShortcut) {
            KeyboardShortcut ksc = (KeyboardShortcut)sc;
            a.registerCustomShortcutSet(ksc.getFirstKeyStroke().getKeyCode(), ksc.getFirstKeyStroke().getModifiers(), terminalPanel);
          }
        }
      }
    }
  }

  @Override
  protected void setupAntialiasing(Graphics graphics) {
    UISettings.setupAntialiasing(graphics);
  }

  @Override
  protected void setCopyContents(StringSelection selection) {
    CopyPasteManager.getInstance().setContents(selection);
  }

  @Override
  protected void drawImage(Graphics2D gfx, BufferedImage image, int x, int y, ImageObserver observer) {
    UIUtil.drawImage(gfx, image, x, y, observer);
  }

  @Override
  protected void drawImage(Graphics2D g, BufferedImage image, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2) {
    drawImage(g, image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, null);
  }

  public static void drawImage(Graphics g,
                               Image image,
                               int dx1,
                               int dy1,
                               int dx2,
                               int dy2,
                               int sx1,
                               int sy1,
                               int sx2,
                               int sy2,
                               ImageObserver observer) {
    if (image instanceof JBHiDPIScaledImage) {
      final Graphics2D newG = (Graphics2D)g.create(0, 0, image.getWidth(observer), image.getHeight(observer));
      newG.scale(0.5, 0.5);
      Image img = ((JBHiDPIScaledImage)image).getDelegate();
      if (img == null) {
        img = image;
      }
      newG.drawImage(img, 2 * dx1, 2 * dy1, 2 * dx2, 2 * dy2, sx1 * 2, sy1 * 2, sx2 * 2, sy2 * 2, observer);
      newG.scale(1, 1);
      newG.dispose();
    }
    else if (RetinaImage.isAppleHiDPIScaledImage(image)) {
      g.drawImage(image, dx1, dy1, dx2, dy2, sx1 * 2, sy1 * 2, sx2 * 2, sy2 * 2, observer);
    }
    else {
      g.drawImage(image, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer);
    }
  }

  @Override
  protected boolean isRetina() {
    return UIUtil.isRetina();
  }

  @Override
  protected String getClipboardContent() throws IOException, UnsupportedFlavorException {
    Transferable contents = CopyPasteManager.getInstance().getContents();
    if (contents == null) {
      return null;
    }
    return (String)contents.getTransferData(DataFlavor.stringFlavor);
  }

  @Override
  protected BufferedImage createBufferedImage(int width, int height) {
    return UIUtil.createImage(width, height, BufferedImage.TYPE_INT_ARGB);
  }
}

