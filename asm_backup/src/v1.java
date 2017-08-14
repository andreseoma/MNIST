import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.awt.FlowLayout;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.Scanner;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

import java.awt.image.WritableRaster;

class Network
{
	String trainData="train-images.idx3-ubyte"; //60k pictures for training
	String trainLabels="train-labels.idx1-ubyte";
	String testData="t10k-images.idx3-ubyte"; //10k pictures for testing, these are written by different people than the training set
	String testLabels="t10k-labels.idx1-ubyte";
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
	int l1=104; //neuron count of the second layer
	int l2=10; //neuron count of the third layer
	float[][] w1=new float[l1][l0+1];	//weights, w1[i][j] means the weight from j-th neuron of the first 
										//layer used by the i-th neuron of the second layer
	float[][] w2=new float[l2][l1+1]; //weights, same, except for the third layer
	float[] v0=new float[l0]; //output of the first layer of neurons, i.e. values of the pixels
	float[] v1=new float[l1]; //output of the second layer of neurons
	float[] v2=new float[l2]; //output of the third layer of neurons
	float[][] grad1=new float[l1][l0+1];
	float[][] grad2=new float[l2][l1+1];
	float[][] grads1=new float[l1][l0+1];
	float[][] grads2=new float[l2][l1+1];
//	float learningRate=0.0001;
//	float decay=0;
	float w1scale=(float)Math.sqrt(l0*0.2);
	float w2scale=(float)Math.sqrt(l1);
	
	Network() throws Exception
	{
		loadTestData();
//		loadTrainingData();
	}
	
	void loadTestData() throws Exception //loads the test data pixels and labels into arrays, sets it active
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
	
	void loadTrainingData() throws Exception	//loads training images and labels to arrays, sets it active
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
	
	void randomizeWeights() //randomizes the weights with a gaussian distribution, scales it such that each layers max output is around 1
	{
		Random rand=new Random();
		for(int i=0;i<l1;++i)
		{
			for(int j=0;j<l0+1;++j)
			{
				w1[i][j]=(float)rand.nextGaussian()/w1scale;
			}
		}
		for(int i=0;i<l2;++i)
		{
			for(int j=0;j<l1+1;++j)
			{
				w2[i][j]=(float)rand.nextGaussian()/w2scale;
			}
		}
	}
	
	//trains using stochastic gradient descent, which means that the average gradient is calculated from
	//int stochasticAverageCount inputs, then weights are updated and the whole thing is repeated
	//int iterations times.
	void trainStochastic(int iterations, int stochasticAverageCount,
				float learningRate,float decay) throws Exception
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
	
	//calculates the outputs of the neurons, assumes the input layer is loaded with some data,
	//output of a neuron is max(0,w*x+b), where it is summed over all the neurons of the previous layer and a 
	//constant b is added. If the result is <0, it is 0, otherwise it is the sum. b is the last number in the array w[i]
	//i.e. for the first neuron of the second layer it is w1[0][l0]
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
	
	//calculates the gradient using the backpropagation algorithm
	//the gradient is taken of the cost function, which is 0.5*(y-v)^2 summed over all the neurons of the last layer, y is the vector
	//of the desired result, i.e. 1 for the correct label and 0 otherwise
	//first calculates the derivatives with respect to b-s of the third layer, then with respect to w2 using the chain rule,
	//then b-s of the first layer and w1 using the chain rule and previous results
	void calcGrad()	
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
	
	//loads random input into the first layer, sets its label to curLabel
	void loadRandomInput()
	{
		curPic=(int)(Math.random()*labels.length);
		curLabel=labels[curPic];
		for(int i=0;i<l0;++i)
		{
			v0[i]=(float)(data[curPic*28*28+i]&0xff)/255;
		}
	}
	
	//writes the weights to a file
	void writeWeights(String filename) throws Exception
	{
		FileOutputStream out=new FileOutputStream(filename);
		ByteBuffer buf=ByteBuffer.allocate(4);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		for(int i=0;i<l1;++i)
			for(int j=0;j<l0+1;++j)
				out.write(buf.putFloat(0,w1[i][j]).array());
		for(int i=0;i<l2;++i)
			for(int j=0;j<l1+1;++j)
				out.write(buf.putFloat(0,w2[i][j]).array());
		
		out.close();
		
//		FileOutputStream out=new FileOutputStream(filename); //little-endian
//		DataOutputStream datout=new DataOutputStream(out);
//		for(int i=0;i<l1;++i)
//			for(int j=0;j<l0+1;++j)
//				datout.writeFloat(w1[i][j]);
//		for(int i=0;i<l2;++i)
//			for(int j=0;j<l1+1;++j)
//				datout.writeFloat(w2[i][j]);
//		datout.close();
	}
	
	//reads the weights from a file, so that you don't have to retrain every time
	void readWeights(String filename) throws Exception
	{
		FileInputStream in=new FileInputStream(filename);
		DataInputStream datin=new DataInputStream(in);
		ByteBuffer buf=ByteBuffer.allocate(4);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		for(int i=0;i<l1;++i)
			for(int j=0;j<l0+1;++j){
				w1[i][j]=buf.order(ByteOrder.LITTLE_ENDIAN).putFloat(0,datin.readFloat()).order(ByteOrder.BIG_ENDIAN).getFloat(0);
			}
		for(int i=0;i<l2;++i)
			for(int j=0;j<l1+1;++j){
				w2[i][j]=buf.order(ByteOrder.LITTLE_ENDIAN).putFloat(0,datin.readFloat()).order(ByteOrder.BIG_ENDIAN).getFloat(0);
			}
		datin.close();
		
//		FileInputStream in=new FileInputStream(filename);
//		DataInputStream datin=new DataInputStream(in);
//		for(int i=0;i<l1;++i)
//			for(int j=0;j<l0+1;++j)
//				w1[i][j]=datin.readFloat();
//		for(int i=0;i<l2;++i)
//			for(int j=0;j<l1+1;++j)
//				w2[i][j]=datin.readFloat();
//		datin.close();
	}
	
	//outputs the best guess by finding the neuron in the last layer with the largest output
	int bestGuess()
	{
		int result=0;
		float max=-1;
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
	
	//randomly chooses int count images from the testing set and prints the percentage guessed correct
	void evaluateTest(int count) throws Exception
	{
		loadTestData();
		float correct=0;
		for(int i=0;i<count;++i)
		{
			loadRandomInput();
			calculateNeuronOutputs();
			if(bestGuess()==curLabel)
				++correct;
		}
		System.out.println("Test data correct "+correct/count*100+"%");
	}
	
	//choose int count random pictures from the training data and print success rate of guessing
	void evaluateTrain(int count) throws Exception
	{
		loadTrainingData();
		float correct=0;
		for(int i=0;i<count;++i)
		{
			loadRandomInput();
			calculateNeuronOutputs();
			if(bestGuess()==curLabel)
				++correct;
		}
		System.out.println("Training data correct "+correct/count*100+"%");
	}
	
	//tests every image in the test set once, prints results
	void testWholeSet() throws Exception
	{
		loadTestData();
		int correct=0;
		for(int i=0;i<labels.length;++i)
		{
			for(int j=0;j<l0;++j)
				v0[j]=(float)(data[i*28*28+j]&0xff)/255;
			curLabel=labels[i];
			calculateNeuronOutputs();
			if(bestGuess()==curLabel)
				++correct;
		}
		System.out.println("Whole test set correct "+correct+" / "+labels.length);
	}
	
	void loadPicNum(int n)
	{
		curLabel=labels[n];
		for(int i=0;i<l0;++i)
		{
			v0[i]=(float)(data[n*28*28+i]&0xff)/255;
		}
	}
	
	//prints outputs of the first layer, for debugging purposes
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

	void printgb1()
	{
		for(int i=0;i<10;++i)
		{
			System.out.printf("%8.7f ",grad1[i][l0]);
		}
		System.out.println();
	}
	
	void printgw2()
	{
		for(int j=0;j<10;++j){
		for(int i=0;i<10;++i)
		{
			System.out.printf("%8.7f ",grad2[i][j]);
		}
		System.out.println();
		}
	}
	
	//prints outputs of the second layer, for debugging purposes
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

	//prints outputs of the third layer, for debugging purposes
	void printv2()
	{
		for(int j=0;j<10;++j)
		{
			System.out.printf("%8.7f ",v2[j]);
		}
		System.out.println();
	}
	
	//displays the current loaded picture in a pop-up window
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
}


class v1
{
	public static void main(String[] args) throws Exception{
		String file1="Weightsasmfloatfast.txt"; //file for weights data
		String file2="Weights.txt";
		Network net=new Network();
		Scanner input=new Scanner(System.in);
//		long start=System.currentTimeMillis(); //for time measurement
		
//		net.randomizeWeights();
//		net.writeWeights(file1);
//		net.loadTrainingData();
//		net.readWeights(file1);
//		net.loadPicNum(0);
//		net.calculateNeuronOutputs();
//		net.calcGrad();
//		net.printgb1();
//		net.printv0();
//		net.printv1();
//		System.out.println(net.curLabel);
//		net.printv2();

//		net.trainStochastic(6000,10,0.05,0.00001);	//can get up to 97%
		
//		net.randomizeWeights();		//start training
//		net.readWeights(file1);
//		for(int i=0;i<10;++i)
//		{
//			net.trainStochastic(6000,10,0.001f,0.01f);
////			net.evaluateTest(200);
//			System.out.print((i+1)+": ");
//			net.testWholeSet();
//			System.out.println("Time "+(System.currentTimeMillis()-start)/1000+"s");
////			net.evaluateTrain(100);
//		}
//		System.out.println("Write to file?");
//		String ans=input.nextLine();
//		if(ans.trim().equals("y"))
//			net.writeWeights(file1);
//		else
//			System.out.println("Didn't write.");
//		
		net.readWeights(file1);	//test weights file
		long start=System.currentTimeMillis();
		
		for(int i=0;i<60000;++i)
			net.calculateNeuronOutputs();
//		net.testWholeSet();
		
//		net.readWeights(file1);
//		net.randomizeWeights();
//		for(int i=0;i<10;++i){
//			for(int j=0;j<10;++j)
//				System.out.printf("%8.5f ",net.w1[j][net.l0]);
//			System.out.println();
//		}
//		net.printv1();
		
//		net.randomizeWeights();		//neuron output testing, debugging purposes
//		net.loadRandomInput();
//		net.calculateNeuronOutputs();
//		for(int j=0;j<1;++j)
//		{
////			net.printv0();
//			net.printv1();
//			System.out.println();
//			net.printv2();
//			System.out.println(net.curLabel);
////			net.trainStochastic(1,1);
//		}
		
		//read weights file, display the picture, the correct guess and the best guess
//		net.readWeights(file1);	
//		net.loadRandomInput();
//		net.calculateNeuronOutputs();
//		System.out.println("Correct: "+net.curLabel+", My guess: "+net.bestGuess());
//		net.display();
//		
		System.out.println("Finished "+(System.currentTimeMillis()-start)/1000+"s");
	}
	
}