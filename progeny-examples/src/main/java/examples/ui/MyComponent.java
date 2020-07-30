package examples.ui;

import java.awt.Font;
import javax.swing.*;
import org.jdom2.Element;
import com.futeh.progeny.ui.UI;
import com.futeh.progeny.ui.UIFactory;

/**
 * @author Alejandro Revilla
 *
 * Demoes a user created component
 *
 * <pre>
 *  &lt;my-component"&gt;Custom Component&lt;/my-component&gt;
 * </pre>
 * @see UIFactory
 */
public class MyComponent implements UIFactory {
    public JComponent create (UI ui, Element e) {
        JLabel label = new JLabel (e.getText());
        String font = e.getAttributeValue ("font");
        if (font != null) 
            label.setFont (Font.decode (font));
        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    }
}

