/** Ben F Rayfield offers this software opensource MIT license */
package immutable.datasets.mnistOcr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import immutable.util.MathUtil;
import mutable.util.ByteStreams;
import mutable.util.Files;

/**
TRAINING SET LABEL FILE (train-labels-idx1-ubyte):
[offset] [type]          [value]          [description] 
0000     32 bit integer  0x00000801(2049) magic number (MSB first) 
0004     32 bit integer  60000            number of items 
0008     unsigned byte   ??               label 
0009     unsigned byte   ??               label 
........ 
xxxx     unsigned byte   ??               label 
The labels values are 0 to 9. 

TRAINING SET IMAGE FILE (train-images-idx3-ubyte):
[offset] [type]          [value]          [description] 
0000     32 bit integer  0x00000803(2051) magic number 
0004     32 bit integer  60000            number of images 
0008     32 bit integer  28               number of rows 
0012     32 bit integer  28               number of columns 
0016     unsigned byte   ??               pixel 
0017     unsigned byte   ??               pixel 
........ 
xxxx     unsigned byte   ??               pixel 
Pixels are organized row-wise. Pixel values are 0 to 255. 0 means background (white), 255 means foreground (black). 

TEST SET LABEL FILE (t10k-labels-idx1-ubyte):
[offset] [type]          [value]          [description] 
0000     32 bit integer  0x00000801(2049) magic number (MSB first) 
0004     32 bit integer  10000            number of items 
0008     unsigned byte   ??               label 
0009     unsigned byte   ??               label 
........ 
xxxx     unsigned byte   ??               label 
The labels values are 0 to 9. 

TEST SET IMAGE FILE (t10k-images-idx3-ubyte):
[offset] [type]          [value]          [description] 
0000     32 bit integer  0x00000803(2051) magic number 
0004     32 bit integer  10000            number of images 
0008     32 bit integer  28               number of rows 
0012     32 bit integer  28               number of columns 
0016     unsigned byte   ??               pixel 
0017     unsigned byte   ??               pixel 
........ 
xxxx     unsigned byte   ??               pixel 
*/
public class MnistOcrDataset{
	private MnistOcrDataset(){}
	
	public static MnistLabeledImage[] readTrainingLabeledImages(){
		byte images[][][] = readTrainingImages();
		byte labels[] = readTrainingLabels();
		//byte labels[] = new byte[images.length];
		if(images.length != labels.length) throw new RuntimeException(
			images.length+" == images.length != labels.length == "+labels.length);
		MnistLabeledImage m[] = new MnistLabeledImage[images.length];
		for(int i=0; i<m.length; i++){
			m[i] = new MnistLabeledImage(images[i], labels[i]);
		}
		return m;
	}
	
	public static MnistLabeledImage[] readTestLabeledImages(){
		byte images[][][] = readTestImages();
		byte labels[] = readTestLabels();
		//byte labels[] = new byte[images.length];
		if(images.length != labels.length) throw new RuntimeException(
			images.length+" == images.length != labels.length == "+labels.length);
		MnistLabeledImage m[] = new MnistLabeledImage[images.length];
		for(int i=0; i<m.length; i++){
			m[i] = new MnistLabeledImage(images[i], labels[i]);
		}
		return m;
	}
	
	public static byte[][][] readTrainingImages(){
		//byte gz[] = (byte[]) jsmGet("/files/data/mnistocrdataset/train-images-idx3-ubyte.gz");
		byte[] gz = Files.readFileRel("/data/tempDatasets/mnistOcr/train-images-idx3-ubyte.gz");
		return readImages(ByteStreams.bytes(ungzipping(gz)));
	}
	
	public static byte[][][] readTestImages(){
		byte[] gz = Files.readFileRel("/data/tempDatasets/mnistOcr/t10k-images-idx3-ubyte.gz");
		//byte unzipped[] = CoreUtil.bytes(ungzipping(gz));
		byte unzipped[] = ByteStreams.bytes(ungzipping(gz));
		//jsmPut("/files/data/mnistocrdataset/t10k-images-idx3-ubyte_unzippedByGZIPInputStream", unzipped);
		return readImages(unzipped);
	}
	
	public static byte[] readTrainingLabels(){
		//byte gz[] = (byte[]) jsmGet("/files/data/mnistocrdataset/train-labels-idx1-ubyte.gz");
		byte[] gz = Files.readFileRel("/data/tempDatasets/mnistOcr/train-labels-idx1-ubyte.gz");
		return readLabels(ByteStreams.bytes(ungzipping(gz)));
	}
	
	public static byte[] readTestLabels(){
		//byte gz[] = (byte[]) jsmGet("/files/data/mnistocrdataset/t10k-labels-idx1-ubyte.gz");
		byte[] gz = Files.readFileRel("/data/tempDatasets/mnistOcr/t10k-labels-idx1-ubyte.gz");
		return readLabels(ByteStreams.bytes(ungzipping(gz)));
	}
	
	public static int readIntBigendian(byte b[], int offset){
		return (b[offset]<<24)|(b[offset+1]<<16)|(b[offset+2]<<8)|b[offset+3]; 
	}
	
	/** returns byte[which image][xOrY][yOrX] */
	public static byte[][][] readImages(byte fromFile[]){
		int observedMagic = readIntBigendian(fromFile, 0);
		int correctMagic = 2051;
		if(observedMagic != correctMagic) throw new RuntimeException(
			observedMagic+" == observedMagic != correctMagic == "+correctMagic);
		int imageQuantity = readIntBigendian(fromFile, 4);
		int rows = readIntBigendian(fromFile, 8);
		int columns = readIntBigendian(fromFile, 12);
		int byteIndexOfFirstPixel = 16;
		int bytesPerImage = rows*columns;
		int imageBytesSize = imageQuantity*bytesPerImage;
		/*if(fromFile.length < byteIndexOfFirstPixel+imageBytesSize) throw new RuntimeException(
			fromFile.length+" == fromFile.length < byteIndexOfFirstPixel+imageBytesSize == "+(byteIndexOfFirstPixel+imageBytesSize));
		*/
		int maxImages = (fromFile.length-byteIndexOfFirstPixel)/bytesPerImage;
		if(maxImages < imageQuantity) throw new RuntimeException(
			"Not enough bytes for all the labels. fromFile.length="+fromFile
			+" maxImages="+maxImages+" imageQuantity="+imageQuantity);
		byte b[][][] = new byte[imageQuantity][columns][rows]; //TODO x and y need reversing?
		int offset = byteIndexOfFirstPixel;
		for(int image=0; image<imageQuantity; image++){
			for(int x=0; x<columns; x++){ //FIXME x and y need to be swapped andOr flipped in some combo, but readTestLabeledImages16x16AsListOfBooleanArray depends on this wrong way so fix that and the other views of the images.
				System.arraycopy(fromFile, offset, b[image][x], 0, rows);
				offset += rows;
			}
		}
		return b;
	}
	
	/** returns 1 byte per label, the baseTen digit in the image of the same index. */
	public static byte[] readLabels(byte fromFile[]){
		int observedMagic = readIntBigendian(fromFile, 0);
		int correctMagic = 2049;
		if(observedMagic != correctMagic) throw new RuntimeException(
			observedMagic+" == observedMagic != correctMagic == "+correctMagic);
		int labelQuantity = readIntBigendian(fromFile, 4);
		int byteIndexOfFirstLabel = 8;
		int bytesPerLabel = 1;
		int maxLabels = (fromFile.length-byteIndexOfFirstLabel)/bytesPerLabel;
		if(maxLabels < labelQuantity) throw new RuntimeException(
			"Not enough bytes for all the labels. fromFile.length="+fromFile
			+" maxLabels="+maxLabels+" labelQuantity="+labelQuantity);
		byte labels[] = new byte[labelQuantity*bytesPerLabel];
		System.arraycopy(fromFile, byteIndexOfFirstLabel, labels, 0, labels.length);
		return labels;
	}
	
	static GZIPInputStream ungzipping(byte bytes[]){
		try{
			return new GZIPInputStream(new ByteArrayInputStream(bytes));
		}catch(IOException e){
			throw new RuntimeException(e);
		}
	}
	
	/** Returns a list of bit arrays size 256, to be interpreted as 16x16,
	where the lower right 14x14 is image data, and the upper 5x2 are digit bitvars
	which 1 of 10 are on at a time in trainingData. The indexs of the digit bitvars
	are 0-4 and 16-20 or TODO should it be that with x and y swapped?
	*/
	public static List<boolean[]> readTestLabeledImages16x16AsListOfBooleanArray(){
		List<boolean[]> list = new ArrayList<boolean[]>();
		for(MnistLabeledImage image : readTestLabeledImages()){
			list.add(to16x16(image));
		}
		return Collections.unmodifiableList(list);
	}
	
	public static float[] to28x28BitofispixelnonzeroAsFloatNoLabel(MnistLabeledImage image){
		float[] f = new float[28*28];
		for(int i=0; i<f.length; i++){
			f[i] = (image.pixels[i/28][i%28]!=0) ? 1 : 0;
		}
		return f;
	}
	
	/** ave node value is very near 1/4 */
	public static float[] to28x28ScalarsSortedPointersNormedByCubed(MnistLabeledImage image){
		float[] f = new float[28*28];
		for(int i=0; i<f.length; i++){
			f[i] = image.pixels[i/28][i%28]&0xff;
		}
		MathUtil.normBySortedPointers(d->d*d*d, f);
		return f;
	}
	
	public static boolean[] to16x16(MnistLabeledImage image){
		boolean b[] = new boolean[256];
		final byte p[][] = image.pixels;
		if(p.length != 28 || p[0].length != 28) throw new IllegalArgumentException(
			"Not 28x28 image: "+image);
		if(image.label < 0 || 9 < image.label) throw new IllegalArgumentException(
			"Label is not integer 0-9: "+image.label);
		for(int smallX=0; smallX<14; smallX++){
			for(int smallY=0; smallY<14; smallY++){
				int x = smallX*2; //x and y range 2-15
				int y = smallY*2;
				int countPixelsOn = 0;
				if(p[x+1][y+1] != 0) countPixelsOn++;
				if(p[x][y] != 0) countPixelsOn++;
				if(p[x+1][y+1] != 0) countPixelsOn++;
				if(p[x][y] != 0) countPixelsOn++;
				b[(2+smallY)*16+2+smallX] = 1 < countPixelsOn;
			}
		}
		//int labelIndex = image.label;
		//if(5 <= image.label) labelIndex += 16-5; //next row of 5x2 at topleft corner
		int labelIndex = image.label/5 + 16*(image.label%5);
		b[labelIndex] = true;
		return b;
	}
	
	/*public static Polydim readTestLabeledImages16x16AsMultiDim(){
		//TODO use training data which is bigger, instead of the test data, for training
		String pathOfCache = "/files/data/humanaicore/datasetsForAI/mnistocrdataset/mnistTestData16x16.polydim";
		Bits b;
		long dimSizes[];
		//int dimSizes[];
		Nanotimer t = new Nanotimer();
		if(jsmExist(pathOfCache)){
			double askJsmExistDuration = t.secondsSinceLastCall();
			System.out.println("askJsmExistDuration took "+askJsmExistDuration+" seconds.");
			byte cacheBytes[] = (byte[]) jsmGet(pathOfCache);
			double getBytesDuration = t.secondsSinceLastCall();
			System.out.println("getBytesDuration took "+getBytesDuration+" seconds.");
			//b = new ByteArrayUntilSplit(cacheBytes); //Since images are 16x16, number of bits is always multiple of 8
			System.out.println("There are "+cacheBytes.length+" bytes.");
			b = BitsUtil.bytesToBits(cacheBytes); //Since images are 16x16, number of bits is always multiple of 8
			double bytesToBitsDuration = t.secondsSinceLastCall();
			System.out.println("bytesToBitsDuration took "+bytesToBitsDuration+" seconds.");
			//dimSizes = SimpleMultiDim.dimSizes(b);
			Polydim m = new SimplePolydim(b);
			double simpleMultidimConstructorDuration = t.secondsSinceLastCall();
			System.out.println("simpleMultidimConstructorDuration took "+simpleMultidimConstructorDuration+" seconds.");
			//return new SimpleMultiDim(b);
			return m;
			/* This part is much too slow for only 320016 bytes: bytesToBitsDuration took 0.7768798880000001 seconds.
			...
			askJsmExistDuration took 0.0011046950000000002 seconds.
			getBytesDuration took 0.001054044 seconds.
			There are 320016 bytes.
			FIXME: AvlBitstring is not always using Fast0To16Bits when split or concat things that result in at most 16 bits
			bytesToBitsDuration took 0.7768798880000001 seconds.
			TODO MultiDim.bits(int...) and SimpleMultiDim.dimSizes(Bits) and constructor with int[], decide on bigEndian (as it is now) or littleEndian (default for this so
			ftware) list of dims should be
			simpleMultidimConstructorDuration took 0.00162787 seconds.
			* /
		}else{
			List<boolean[]> list = readTestLabeledImages16x16AsListOfBooleanArray();
			//dimSizes = new int[]{list.size(), 16, 16};
			dimSizes = new long[]{list.size(), 16, 16};
			CountMemory.afterLongArrayAllocated(dimSizes);
			b = Fast0To16Bits.EMPTY;
			for(boolean image[] : list){
				for(boolean pixel : image){
					//TODO optimize this using ByteArrayUntilSplit instead of 1 bit at a time
					//b = b.cat(Fast0To16Bits.get(pixel)).balanceTree();
					b = b.cat(Fast0To16Bits.get(pixel));
				}
			}
			Polydim multidim = new SimplePolydim(b, dimSizes);
			
			//Save the multidim to file (TODO move this code to common.io.Out or some util class,
			//but there is still the issue of not every MultiDim is a multiple of 8 bits, but this one is.
			//Bits multidimData = multidim.data();
			Bits headerThenData = multidim.headerThenData();
			//Since images are 16x16, number of bits is always multiple of 8
			long howManyBytes = headerThenData.siz()>>3;
			if(howManyBytes > Integer.MAX_VALUE) throw new IndexOutOfBoundsException(
				"about to save multidim to file using byte array but howManyBytes="+howManyBytes+" not fit in int range");
			byte bytes[] = new byte[(int)howManyBytes];
			long bitIndex = 0;
			for(int i=0; i<bytes.length; i++){
				bytes[i] = headerThenData.byteAt(bitIndex);
				bitIndex += 8;
			}
			jsmPut(pathOfCache, bytes);
			
			return multidim;
		}
	}*/
	
	public static long labelIndexIn16x16(byte label){
		if(label < 0 || 10 <= label) throw new IndexOutOfBoundsException(
			"Not in range 0-9. label="+label);
		return label/5 + 16*(label%5);
	}
	
	/** Theres a 5x2 rectangle with 1 bitvar per baseTen digit, and 14x14 image data.
	Returns byte in range 0-9 for the first label it finds, and at least 1 of them
	must always be on, but if its not, returns -1.
	*
	public static byte getLabel(Bits image14x14in16x16){
		for(byte label=0; label<10; label++){
			long labelIndex = labelIndexIn16x16(label);
			if(image14x14in16x16.bitAt(labelIndex)) return label;
		}
		return -1; //error, no label of baseTen digit
	}*/
	
	/** Returns them in same order they occur in images16x16 *
	public static Polydim getAllOfDigitFrom16x16(Polydim images16x16, byte digit){
		long howManyImages = images16x16.dimSize(0);
		Bits imageList = Fast0To16Bits.EMPTY;
		long foundImagesOfThatDigit = 0;
		for(long i=0; i<howManyImages; i++){
			Polydim image16x16 = images16x16.bits(i);
			if(getLabel(image16x16.data()) == digit){
				imageList = imageList.cat(image16x16.data());
				foundImagesOfThatDigit++;
			}
		}
		return new SimplePolydim(imageList, foundImagesOfThatDigit, 16, 16);
	}*/

}
