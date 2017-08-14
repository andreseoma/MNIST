import java.io.*;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import java.awt.image.WritableRaster;

class Network
{
	String trainData="C:/Users/Lenovo/Desktop/oop/ruhm/src/train-images.idx3-ubyte"; //60k pictures for training
	String trainLabels="C:/Users/Lenovo/Desktop/oop/ruhm/src/train-labels.idx1-ubyte";
	String testData="C:/Users/Lenovo/Desktop/oop/ruhm/src/t10k-images.idx3-ubyte"; //10k pictures for testing, these are written by different people than the training set
	String testLabels="C:/Users/Lenovo/Desktop/oop/ruhm/src/t10k-labels.idx1-ubyte";
	byte data0[]; //training data
	byte labels0[];
	byte data1[]; //testing data
	byte labels1[];
	byte data[]; //active data
	byte labels[];
	int curPic; //current picture data in the first layer
	int curLabel; //label of the current picture
	int curOffset;
	int l0=28*28; //neuron count on the first layer, since the picture is 28 by 28 pixels
	int l1=100; //neuron count of the second layer
	int l2=10; //neuron count of the third layer
	double[][] w1=new double[l1][l0+1];	//weights, w1[i][j] means the weight from j-th neuron of the first 
										//layer used by the i-th neuron of the second layer
	double[][] w2=new double[l2][l1+1]; //weights, same, except for the third layer
	double[] v0=new double[l0]; //output of the first layer of neurons, i.e. values of the pixels
	double[] v1=new double[l1]; //output of the second layer of neurons
	double[] v2=new double[l2]; //output of the third layer of neurons
	double[][] grad1=new double[l1][l0+1];
	double[][] grad2=new double[l2][l1+1];
	double[][] grads1=new double[l1][l0+1];
	double[][] grads2=new double[l2][l1+1];
	double learningRate=0.08;
	double decay=1/10;
	double w1scale=Math.sqrt(l0)*Math.sqrt(0.2);
	double w2scale=Math.sqrt(l1);
	
	Network() throws Exception
	{
		loadTestData();
		loadTrainingData();
	}
	
	void loadTestData() throws Exception
	{
		data=data1;
		labels=labels1;
		if(data1!=null&&labels1!=null)
			return;
		File f=new File(testData);
		FileInputStream in=new FileInputStream(testData);
		data1=new byte[(int)f.length()-16];
		in.skip(16);
		in.read(data1,0,(int)f.length()-16);
		in.close();
		f=new File(testLabels);
		in=new FileInputStream(testLabels);
		in.skip(8);
		labels1=new byte[(int)f.length()-8];
		in.read(labels1,0,(int)f.length()-8);
		in.close();
		data=data1;
		labels=labels1;
	}
	
	void loadTrainingData() throws Exception
	{
		data=data0;
		labels=labels0;
		if(data0!=null&&labels0!=null)
			return;
		File f=new File(trainData);
		FileInputStream in=new FileInputStream(trainData);
		data0=new byte[(int)f.length()-16];
		in.skip(16);
		in.read(data0,0,(int)f.length()-16);
		in.close();
		f=new File(trainLabels);
		in=new FileInputStream(trainLabels);
		in.skip(8);
		labels0=new byte[(int)f.length()-8];
		in.read(labels0,0,(int)f.length()-8);
		in.close();
		data=data0;
		labels=labels0;
	}
	
	void randomizeWeights()
	{
		Random rand=new Random();
		for(int i=0;i<l1;++i)
		{
			for(int j=0;j<l0+1;++j)
			{
				w1[i][j]=rand.nextGaussian()/w1scale;
			}
		}
		for(int i=0;i<l2;++i)
		{
			for(int j=0;j<l1+1;++j)
			{
				w2[i][j]=rand.nextGaussian()/w2scale;
			}
		}
	}
	void trainStochastic(int iterations, int stochasticAverageCount) throws Exception
	{
		loadTrainingData();
		for(int o=0;o<iterations;++o)
		{
			loadRandomInput();
			calculateNeuronOutputs();
			calcGrad();
			for(int i=0;i<l1;++i)
				for(int j=0;j<l0+1;++j)
					grads1[i][j]=grad1[i][j];
			for(int i=0;i<l2;++i)
				for(int j=0;j<l1+1;++j)
					grads2[i][j]=grad2[i][j];
			for(int k=1;k<stochasticAverageCount;++k)
			{
				loadRandomInput();
				calculateNeuronOutputs();
				calcGrad();
				for(int i=0;i<l1;++i)
					for(int j=0;j<l0+1;++j)
						grads1[i][j]+=grad1[i][j];
				for(int i=0;i<l2;++i)
					for(int j=0;j<l1+1;++j)
						grads2[i][j]+=grad2[i][j];
			}
			for(int i=0;i<l1;++i)
				for(int j=0;j<l0+1;++j)
					w1[i][j]-=learningRate*(grads1[i][j]/stochasticAverageCount+decay*w1[i][j]);
//					w1[i][j]-=learningRate*grads1[i][j]/stochasticAverageCount;
			for(int i=0;i<l2;++i)
				for(int j=0;j<l1+1;++j)
					w2[i][j]-=learningRate*(grads2[i][j]/stochasticAverageCount+decay*w2[i][j]);
//					w2[i][j]-=learningRate*grads2[i][j]/stochasticAverageCount;
		}
	}
	void calculateNeuronOutputs()
	{
		for(int i=0;i<l1;++i)
		{
			v1[i]=0;
			for(int j=0;j<l0;++j)
			{
				v1[i]+=w1[i][j]*v0[j];
			}
			v1[i]+=w1[i][l0];
			if(v1[i]<0)
				v1[i]=0;
		}
		for(int i=0;i<l2;++i)
		{
			v2[i]=0;
			for(int j=0;j<l1;++j)
			{
				v2[i]+=w2[i][j]*v1[j];
			}
			v2[i]+=w2[i][l1];
			if(v2[i]<0)
				v2[i]=0;
		}
	}
	
	void calcGrad()	//calculates gradient using the backpropagation algorithm
	{
		for(int i=0;i<l2;++i)
		{
			if(v2[i]!=0)
			{
				if(i==curLabel)
					grad2[i][l1]=v2[i]-1;
				else
					grad2[i][l1]=v2[i];
				for(int j=0;j<l1;++j)
					grad2[i][j]=grad2[i][l1]*v1[j];
			}
			else
				for(int j=0;j<l1+1;++j)
					grad2[i][j]=0;
		}
		for(int i=0;i<l1;++i)
		{
			if(v1[i]!=0)
			{
				grad1[i][l0]=0;
				for(int m=0;m<l2;++m)
					grad1[i][l0]+=w2[m][i]*grad2[m][l1];
				for(int j=0;j<l0;++j)
					grad1[i][j]=grad1[i][l0]*v0[j];
			}
			else
				for(int j=0;j<l0+1;++j)
					grad1[i][j]=0;
		}
	}
	
	void loadRandomInput()
	{
		curPic=(int)(Math.random()*labels.length);
		curLabel=labels[curPic];
		for(int i=0;i<l0;++i)
		{
			v0[i]=(double)(data[curPic*28*28+i]&0xff)/255;
		}
	}
	
	void writeWeights(String filename) throws Exception
	{
		FileOutputStream out=new FileOutputStream(filename);
		DataOutputStream datout=new DataOutputStream(out);
		for(int i=0;i<l1;++i)
			for(int j=0;j<l0+1;++j)
				datout.writeDouble(w1[i][j]);
		for(int i=0;i<l2;++i)
			for(int j=0;j<l1+1;++j)
				datout.writeDouble(w2[i][j]);
		datout.close();
	}
	
	void readWeights(String filename) throws Exception
	{
		FileInputStream in=new FileInputStream(filename);
		DataInputStream datin=new DataInputStream(in);
		for(int i=0;i<l1;++i)
			for(int j=0;j<l0+1;++j)
				w1[i][j]=datin.readDouble();
		for(int i=0;i<l2;++i)
			for(int j=0;j<l1+1;++j)
				w2[i][j]=datin.readDouble();
		datin.close();
	}
	
	int bestGuess()
	{
		int result=0;
		double max=-1;
		for(int i=0;i<l2;++i)
		{
			if(v2[i]>max)
			{
				result=i;
				max=v2[i];
			}
		}
		return result;
	}
	void evaluateTest(int count) throws Exception
	{
		loadTestData();
		double correct=0;
		for(int i=0;i<count;++i)
		{
			loadRandomInput();
			calculateNeuronOutputs();
			if(bestGuess()==curLabel)
				++correct;
		}
		System.out.println("Test data correct "+correct/count*100+"%");
	}
	
	void evaluateTrain(int count) throws Exception
	{
		loadTrainingData();
		double correct=0;
		for(int i=0;i<count;++i)
		{
			loadRandomInput();
			calculateNeuronOutputs();
			if(bestGuess()==curLabel)
				++correct;
		}
		System.out.println("Training data correct "+correct/count*100+"%");
	}
	
	void testWholeSet() throws Exception
	{
		loadTestData();
		int correct=0;
		for(int i=0;i<labels.length;++i)
		{
			for(int j=0;j<l0;++j)
				v0[j]=(double)(data[i*28*28+j]&0xff)/255;
			curLabel=labels[i];
			calculateNeuronOutputs();
			if(bestGuess()==curLabel)
				++correct;
		}
		System.out.println("Whole test set correct "+correct+" / "+labels.length);
	}
	
	void printv0()
	{
		for(int i=0;i<28;++i)
		{
			for(int j=0;j<28;++j)
			{
				System.out.printf("%8.7f ",v0[i*28+j]);
			}
			System.out.println();
		}
	}
	
	void printv1()
	{
		{
			for(int i=0;i<10;++i)
			{
				for(int j=0;j<10;++j)
				{
					System.out.printf("%8.7f ",v1[i*10+j]);
				}
				System.out.println();
			}
		}
	}
	
	void printv2()
	{
		for(int j=0;j<10;++j)
		{
			System.out.printf("%8.7f ",v2[j]);
		}
		System.out.println();
	}
	
	void display()
	{
		int[] pixels=new int[28*28];
		for(int i=0;i<28*28;++i)
			pixels[i]=255-data[28*28*curPic+i]&0xff;
		BufferedImage image = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = image.getRaster();
        raster.setPixels(0,0,28,28,pixels);
        image.setData(raster);
        
        ImageIcon icon=new ImageIcon(image);
        JFrame frame=new JFrame();
        frame.setLayout(new FlowLayout());
        frame.setSize(50,100);
        JLabel lbl=new JLabel();
        lbl.setIcon(icon);
        frame.add(lbl);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}
	
	BufferedImage displayInfo()	{
		int[] pixels=new int[28*28];
		for(int i=0;i<28*28;++i)
			pixels[i]=255-data[28*28*curPic+i]&0xff;
		BufferedImage image = new BufferedImage(28, 28, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = image.getRaster();
        raster.setPixels(0,0,28,28,pixels);
        image.setData(raster);
        return image;
	}
}
class v1
{
	public static void main(String[] args) throws Exception{
		String weightsFile="Weights_regul.txt";
		Network net=new Network();
		long start=System.currentTimeMillis();
		/*
		net.loadRandomInput();
		net.display();
		net.calculateNeuronOutputs();
		System.out.println(net.bestGuess());
		*/
//		net.evaluateTest(1000);
//		net.evaluateTrain(1000);
		net.randomizeWeights();
		for(int i=0;i<30;++i)
		{
			net.trainStochastic(6000, 10);
//			net.evaluateTest(200);
			net.testWholeSet();
//			net.evaluateTrain(100);
		}
//		net.testWholeSet();
		net.writeWeights(weightsFile);
		
//		net.randomizeWeights();
//		net.trainStochastic(10000,10);
//		net.evaluateTest(1000);
//		net.evaluateTrain(1000);
//		net.writeWeights(weightsFile);
//		
//		net.readWeights(weightsFile);
//		net.testWholeSet();
//		net.evaluateTrain(10000);
//		net.evaluateTest(10000);
		
//		net.randomizeWeights();
//		net.loadRandomInput();
//		net.calculateNeuronOutputs();
//		for(int j=0;j<1;++j)
//		{
////			net.printv0();
//			net.printv1();
//			System.out.println();
//			net.printv2();
//			System.out.println();
//			net.trainStochastic(1,1);
//		}
		
//		net.loadRandomInput();
//		net.display();
////		net.printv0();
//		System.out.println(net.curLabel);
		/*
		net.readWeights(weightsFile);
		net.loadRandomInput();
		net.calculateNeuronOutputs();
		System.out.println(net.curLabel+" "+net.bestGuess());
		net.display();
		*/
		System.out.println("Finished "+(double)(System.currentTimeMillis()-start)/1000+"s");
	}
}