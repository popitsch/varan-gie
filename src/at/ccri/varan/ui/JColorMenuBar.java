package at.ccri.varan.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JMenuBar;

/**
 * @see https://stackoverflow.com/questions/15648030/change-background-and-text-color-of-jmenubar-and-jmenu-objects-inside-it
 * @author niko.popitsch
 *
 */
public class JColorMenuBar extends JMenuBar {
    Color bgColor = Color.WHITE;

    public void setColor(Color color) {
	bgColor = color;
    }

    @Override
    protected void paintComponent(Graphics g) {
	super.paintComponent(g);
	Graphics2D g2d = (Graphics2D) g;
	g2d.setColor(bgColor);
	g2d.fillRect(0, 0, getWidth() - 1, getHeight() - 1);

    }

}
