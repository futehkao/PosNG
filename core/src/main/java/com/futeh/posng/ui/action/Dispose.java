package com.futeh.posng.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.futeh.posng.ui.UI;
import com.futeh.posng.ui.UIAware;
import org.jdom2.Element;

public class Dispose implements ActionListener, UIAware {
    public UI ui;
    public Dispose () {
        super();
    }
    public void setUI (UI ui, Element e) {
        this.ui = ui;
    }
    public void actionPerformed (ActionEvent ev) {
        ui.dispose ();
    }
}

