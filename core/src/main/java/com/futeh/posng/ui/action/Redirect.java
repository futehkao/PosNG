package com.futeh.posng.ui.action;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import com.futeh.posng.ui.UIAware;
import com.futeh.posng.ui.UI;
import org.jdom2.Element;
import java.util.StringTokenizer;

public class Redirect implements ActionListener, UIAware {
    public UI ui;
    public Redirect () {
        super();
    }
    public void setUI (UI ui, Element e) {
        this.ui = ui;
    }
    public void actionPerformed (ActionEvent ev) {
        StringTokenizer st = new StringTokenizer (ev.getActionCommand ());
        ui.reconfigure (
            st.nextToken(), 
            st.hasMoreTokens () ?  st.nextToken () : null
        );
    }
}

