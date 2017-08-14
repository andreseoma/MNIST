
import java.awt.*;

import javax.imageio.ImageIO;
import javax.swing.*;

import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class NumbriEraldus {
	public BufferedImage kuvaTõmmis(JPanel raam) {
	    Rectangle rec = raam.getBounds();
	    BufferedImage pilt = new BufferedImage(rec.width, rec.height, BufferedImage.TYPE_INT_ARGB);
	    raam.printAll(pilt.getGraphics());
	    //ImageIcon resizedImg = new ImageIcon(pilt.getScaledInstance(112, 112, 0));
	    return pilt;
	}
	
	public static void main (String args[]){
		
	}
}
