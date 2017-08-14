import javax.swing.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;


public class gui extends JFrame{
	
	JLabel tutvustus, sisesta, vastusSs, vastusS, vastusAs, vastusA, vastusCs, vastusC, komm, pilt, arvutilC, sinulC, mangitud;
	JButton kontrolli, uus;
	JTextField tf;
	int av, cv, AC, SC, m;
	ImageIcon image1;
	
	public gui(){
		super("Numbri �raarvamise m�ng");
		
		setLayout (new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints(); 
		
		
		tutvustus = new JLabel ("Arva number �ra ja v�istle arvutiga!");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 6;
		c.ipadx = 3;
		c.ipady = 3;
		add (tutvustus, c);
		
		pilt = new JLabel();
		image1 = new ImageIcon("C:/Users/Lenovo/Desktop/oop/ruhm/src/Nr.jpg");
		pilt.setIcon(image1);;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		c.gridwidth = 3;
		c.gridheight = 3;
		add(pilt, c);
		
		uus = new JButton("Alusta!");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 4;
		c.gridy = 3;
		c.gridwidth = 1;
		c.gridheight = 1;
		add (uus, c);
		
		sisesta = new JLabel ("Sisesta number:");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 4;
		c.gridwidth = 2;
		add (sisesta, c);
		
		tf = new JTextField (5);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 4;
		add (tf, c);
		
		kontrolli = new JButton("Kontrolli!");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 4;
		c.gridy = 4;
		add (kontrolli, c);
		kontrolli.setEnabled(false);
		
		vastusSs = new JLabel ("Sinu vastus:");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 5;
		c.gridwidth = 1;
		add (vastusSs, c);
		
		vastusS = new JLabel ("... ");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 5;
		add (vastusS, c);
		
		vastusAs = new JLabel ("Arvuti vatus:");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 5;
		add (vastusAs, c);
		
		vastusA = new JLabel("... ");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 3;
		c.gridy = 5;
		add (vastusA, c);
		
		vastusCs = new JLabel("�ige vastus:");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 4;
		c.gridy = 5;
		add (vastusCs, c);
		
		vastusC = new JLabel("...  ");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 5;
		c.gridy = 5;
		add (vastusC, c);
		
		komm = new JLabel("   ");
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 6;
		c.gridwidth = 6;
		add (komm, c);
		
		sinulC = new JLabel("Sinul �igeid: " + SC);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 7;
		c.gridwidth = 2;
		add (sinulC, c);
		
		arvutilC = new JLabel ("Arvutil �igeid: " + AC);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 2;
		c.gridy = 7;
		add (arvutilC, c);
		
		mangitud = new JLabel ("M�ngitud: " + m);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 4;
		c.gridy = 7;
		add (mangitud, c);
		
		
		
		UusButtonClass ubc = new UusButtonClass();
		uus.addActionListener(ubc);
		
		KontrolliButtonClass kbc = new KontrolliButtonClass();
		kontrolli.addActionListener(kbc);
		
		SwingUtilities.getRootPane(kontrolli).setDefaultButton(kontrolli);
		
		
	}
	
	public class UusButtonClass implements ActionListener{
		public void actionPerformed(ActionEvent ubc){
			uus.setEnabled(false);
			kontrolli.setEnabled(true);
			tf.setText("");
			tf.setEnabled(true);
			tf.requestFocusInWindow();
			

			vastusS.setText("... ");
			vastusA.setText("... ");
			vastusC.setText("...  ");
			
			String weightsFile="Weights_regul.txt";
			Network net;
			try {
				net = new Network();
				net.readWeights(weightsFile);
			} catch (Exception e) {
				return;
			}
			
			net.loadRandomInput();
			net.calculateNeuronOutputs();
			av = net.bestGuess();
			cv = net.curLabel;
			BufferedImage image = net.displayInfo();
			ImageIcon resizedImg = new ImageIcon(image.getScaledInstance(112, 112, 0));
			//image1 = new ImageIcon (resizedImg);
			pilt.setIcon(resizedImg);
		}
	}
	
	
	public class KontrolliButtonClass implements ActionListener{
		public void actionPerformed(ActionEvent kbc){
			int number;
			
			try{
				number = (int)(Double.parseDouble(tf.getText()));
				
			} catch (NumberFormatException ex){
				komm.setText("Vigane sisend!");
				komm.setForeground(Color.RED);
				return;
			}
			vastusS.setText(" " + String.valueOf(number) + " ");
			vastusA.setText(" " + String.valueOf(av) + " ");
			vastusC.setText(" " + String.valueOf(cv) + " ");
			
			m++;
			
			if (number==av){
				if (number==cv){
					komm.setText("VIIK, m�lemal �igus!");
					AC++;
					SC++;
				}else{
					komm.setText("VIIK, m�lemal vale!");
				}
			}else{
				if (av==cv){
					komm.setText("KAOTASID, Arvuti vastas �igesti!");
					AC++;
				}else{
					komm.setText("V�ITSID, Arvuti vastas valesti!");
					SC++;
				}
			}
			sinulC.setText("Sinul �igeid: " + SC);
			arvutilC.setText("Arvutil �igeid: " + AC);
			mangitud.setText("M�ngitud: " + m);
			
			uus.setEnabled(true);
			kontrolli.setEnabled(false);
			tf.setEnabled(false);
		}
	}
	
	public static void main(String args[]) throws Exception{
		gui programm = new gui();
        programm.setSize(300,250);
        programm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        programm.setVisible(true);
        //programm.setTitle("Numbri �raarvamise m�ng");
		
	
	}

}
