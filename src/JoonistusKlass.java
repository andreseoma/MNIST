//https://docs.oracle.com/javase/tutorial/uiswing/events/mousemotionlistener.html



import java.awt.*;

import javax.swing.*;

import java.awt.event.*;
import java.awt.image.BufferedImage;


public class JoonistusKlass extends JFrame {

  JoonistusPaneel joonistus;
  JButton okNupp, cancelNupp;
  JLabel pilt;

  public JoonistusKlass() {
    super("Joonista!");
    
    setLayout (new GridBagLayout());
	GridBagConstraints c = new GridBagConstraints(); 
	
	
	joonistus = new JoonistusPaneel();
    joonistus.setBackground(Color.white);
	c.fill = GridBagConstraints.HORIZONTAL;
	c.gridwidth = 2;
	c.gridx = 0;
	c.gridy = 0;    
    add(joonistus, c);
    
    okNupp = new JButton("OK");   
	c.fill = GridBagConstraints.HORIZONTAL;
	c.gridwidth = 1;
	c.gridx = 0;
	c.gridy = 1;    
    add(okNupp, c);
    
    cancelNupp = new JButton("Tühista");    
	c.fill = GridBagConstraints.HORIZONTAL;
	c.gridx = 1;
	c.gridy = 1;    
    add(cancelNupp, c);
    
    pilt = new JLabel();
    c.fill = GridBagConstraints.HORIZONTAL;
    c.gridwidth = 2;
	c.gridx = 0;
	c.gridy = 2;    
    add(pilt, c);
    pilt.setSize(pilt.getPreferredSize());
    
    OkButtonClass obc = new OkButtonClass();
	okNupp.addActionListener(obc);

  }
  
  public class OkButtonClass implements ActionListener{
		public void actionPerformed(ActionEvent obc){
			
		
			abimeetod();
		}
	}
	
	void abimeetod(){
		//();
		joonistus.update(getGraphics());
		NumbriEraldus ne;
	    ne = new NumbriEraldus();
	    
		BufferedImage image = ne.kuvaTõmmis(this.joonistus);
		ImageIcon resizedImg = new ImageIcon(image);
		//image1 = new ImageIcon (resizedImg);
		pilt.setIcon(resizedImg);
	}
  
  public static void main(String[] args) {
	    JoonistusKlass joonis = new JoonistusKlass();
	    joonis.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    joonis.setSize(300, 300);
	    joonis.setVisible(true);
	    //NumbriEraldus ne;
	    //ne = new NumbriEraldus();
	    //ne.kuvaTõmmis(joonis);
	  }

}



class JoonistusPaneel extends JPanel {
  public JoonistusPaneel() {
	  
	  Dimension dim = new Dimension(112, 112);
	  setSize(dim);
      setMinimumSize(dim);
      setMaximumSize(dim);
	  setPreferredSize(dim);
	  
	addMouseMotionListener(new MouseMotionAdapter() {
      public void mouseDragged(MouseEvent hiirl) {
        joonista(hiirl.getX(), hiirl.getY()); 
      }// Joonistab joone eelmise koha ja uue koha vahele
    });
    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent hiirv) {
        vana(hiirv.getX(), hiirv.getY()); //jätab meelde asukoha
        requestFocusInWindow(); 
      }
    });		
    }

  int vana_x;
  int vana_y;

  public void vana(int x, int y) {
    vana_x = x;
    vana_y = y;
  }

  public void joonista(int x, int y) {
    Graphics g = getGraphics();
    
    //Järgneb oskamatu joonepaksuse suurendamine
    
    g.drawLine(vana_x, vana_y, x, y); // joonistab joone koordinaatide vahele
    g.drawLine(vana_x+1, vana_y+1, x+1, y+1);
    g.drawLine(vana_x+1, vana_y+1, x+1, y-1);
    g.drawLine(vana_x+1, vana_y+1, x-1, y+1);
    g.drawLine(vana_x+1, vana_y-1, x-1, y-1);
    g.drawLine(vana_x-1, vana_y-1, x-1, y-1);
    g.drawLine(vana_x-1, vana_y-1, x-1, y+1);
    g.drawLine(vana_x-1, vana_y-1, x+1, y+1);
    g.drawLine(vana_x-1, vana_y+1, x+1, y+1);
    
    g.drawLine(vana_x+1, vana_y-1, x+1, y+1);
    g.drawLine(vana_x+1, vana_y-1, x+1, y-1);
    g.drawLine(vana_x+1, vana_y-1, x-1, y+1);
    g.drawLine(vana_x+1, vana_y+1, x-1, y-1);
    g.drawLine(vana_x-1, vana_y+1, x-1, y-1);
    g.drawLine(vana_x-1, vana_y+1, x-1, y+1);
    g.drawLine(vana_x-1, vana_y+1, x+1, y+1);
    g.drawLine(vana_x-1, vana_y-1, x+1, y+1);
    
    g.drawLine(vana_x-1, vana_y+1, x+1, y+1);
    g.drawLine(vana_x-1, vana_y+1, x+1, y-1);
    g.drawLine(vana_x-1, vana_y+1, x-1, y+1);
    g.drawLine(vana_x-1, vana_y-1, x-1, y-1);
    g.drawLine(vana_x+1, vana_y-1, x-1, y-1);
    g.drawLine(vana_x+1, vana_y-1, x-1, y+1);
    g.drawLine(vana_x+1, vana_y-1, x+1, y+1);
    g.drawLine(vana_x+1, vana_y+1, x+1, y+1);
    
    g.drawLine(vana_x+1, vana_y+1, x-1, y+1);
    g.drawLine(vana_x+1, vana_y+1, x-1, y-1);
    g.drawLine(vana_x+1, vana_y+1, x+1, y+1);
    g.drawLine(vana_x+1, vana_y-1, x+1, y-1);
    g.drawLine(vana_x-1, vana_y-1, x+1, y-1);
    g.drawLine(vana_x-1, vana_y-1, x+1, y+1);
    g.drawLine(vana_x-1, vana_y-1, x-1, y+1);
    g.drawLine(vana_x-1, vana_y+1, x-1, y+1);
    
    g.drawLine(vana_x+1, vana_y+1, x+1, y-1);
    g.drawLine(vana_x+1, vana_y+1, x+1, y+1);
    g.drawLine(vana_x+1, vana_y+1, x-1, y-1);
    g.drawLine(vana_x+1, vana_y-1, x-1, y+1);
    g.drawLine(vana_x-1, vana_y-1, x-1, y+1);
    g.drawLine(vana_x-1, vana_y-1, x-1, y-1);
    g.drawLine(vana_x-1, vana_y-1, x+1, y-1);
    g.drawLine(vana_x-1, vana_y+1, x+1, y-1);
    
    //g.fillOval(Math.abs((vana_x+x)/2), Math.abs((vana_y+y)/2), 3, 3); //Lootus ovaalidega luua ühtlasemat joont
    vana(x, y);
  }


}
