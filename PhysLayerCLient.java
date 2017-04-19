import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import java.util.HashMap;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;
public class PhysLayerCLient{
	
	//HashMap to store the key and value to decode 5 bit to 4 bit data
	private static HashMap<LinkedList<Boolean>, Boolean[]> decoder;
	//main program
	public static void main(String[] args)throws Exception{
		//function setup 5 bit to 4 bit decoder hashmap.
		setDecoderMap();
		//connect to server
		try(Socket socket = new Socket("codebank.xyz", 38002)){
			System.out.println("Connected to server.");
			InputStream streamFromServer = socket.getInputStream();
			OutputStream streamToServer = socket.getOutputStream();
			//320 boolean array to hold 320 bits of message
			Boolean[] encodedMessage = new Boolean[320];
			//boolean array to hold the bit decoded from NRZI decoder
			Boolean[] decodedBits;
			//get baseline as float
			float baseline = getBaseline(streamFromServer);
			//get encoded message from server
			encodedMessage = getEncodedMessage(streamFromServer, baseline);
			//decode NRZI 
			decodedBits = decodeNRZI(encodedMessage);
			//get fully decoded message 
			Boolean[] messageBits = convert5To4Bits(decodedBits);
			//convert boolean array to byte arrays
			byte[] message = toBytes(messageBits);
			System.out.println("Baseline establised from preamble: "+baseline);
			System.out.print("Reveived 32 bytes: ");
			printBytesInHex(message);
			//send byte message to server
			streamToServer.write(message);
			//get response from server
			byte[] signal = new byte[1];
			streamFromServer.read(signal);
			if(signal[0] == 1){
				System.out.println("Response good.");
			}
			else{
				System.out.println("I didn’t receive the correct 32 bytes.");
			}
		}
	}
	//Method Convert 5 bits array to 4 bits array
	public static Boolean[] convert5To4Bits(Boolean[] fiveBitsMessage){
		//Initialize array to hold all groups of four bits 
		Boolean[] fourBitsMessage  = new Boolean[fiveBitsMessage.length - fiveBitsMessage.length/5];
		//index counter to loop through five bits array message
		int j = 0;
		//index counter to loop through four bits array message
		int i = 0;
		//loop through five bits array message
		for( i = 0; i < fiveBitsMessage.length;){
			int counter = 0;
			//group every five bits to a linkedlist
			LinkedList<Boolean> fiveBits = new LinkedList<>();
			while(counter < 5){
				fiveBits.add(fiveBitsMessage[i]);
				i++;
				counter++;
			}
			//use the five bit to four bit decoder to decode the five bits to four bits
			Boolean[] fourBits = decoder.get(fiveBits);
			//Store the four bits to four bits message array.
			counter = 0;
			while(counter < fourBits.length){
				fourBitsMessage[j] = fourBits[counter];
				counter++;
				j++;
			}	
		}
		return fourBitsMessage;
	}
	//Method to get the signals from server and store them as 1 or 0 into a boolean array
	public static Boolean[] getEncodedMessage(InputStream inputStream, float baseline)throws Exception{
		Boolean[] encodedMessage = new Boolean[320];
		byte[] a;
		int i = 0;
		while(i < 320){
			a = new byte[1];
			inputStream.read(a);
			encodedMessage[i] = (float)Byte.toUnsignedInt(a[0]) > baseline;
			i++;
		}
		return encodedMessage;
	}
	//Get the 64 preamble and find its average, which will be the baseline
	public static float getBaseline(InputStream inputStream)throws Exception{
		byte[] a;
		int i = 1;
		float sum = 0;
		while(i < 65){
			a = new byte[1];
			inputStream.read(a);
			int unsignInt = Byte.toUnsignedInt(a[0]);
			sum += (float)unsignInt;
			i++;
		}
		float avg = sum / (float)64;
		return avg;
	}
	//Decoder the NRZI bits
	public static Boolean[] decodeNRZI(Boolean[] nrzi){
		Boolean[] decodedBits = new Boolean[nrzi.length];
		Boolean preBit = false;
		for(int i = 0; i<nrzi.length; i++){
			if(i == 0){
				decodedBits[i] = nrzi[i];
			}
			else if(nrzi[i] != preBit){
				decodedBits[i] = true;
			}
			else{
				decodedBits[i] = false;
			}
			preBit = nrzi[i];
		}
		return decodedBits;
	}
	//Set up the four bit to five bits map
	public static void setDecoderMap(){
		decoder = new HashMap<>();
		ArrayList<LinkedList<Boolean>> fiveBitCodeLists = new ArrayList<>();
		Boolean[][] fourBitDatas = {{false, false, false, false},
									{false, false, false, true},
									{false, false, true, false},
									{false, false, true, true},
									{false, true, false, false},
									{false, true, false, true},
									{false, true, true, false},
									{false, true, true, true},
									{true, false, false, false},
									{true, false, false, true},
									{true, false, true, false},
									{true, false, true, true},
									{true, true, false, false},
									{true, true, false, true},
									{true, true, true, false},
									{true, true, true, true}};
		Boolean[][] fiveBitCode = {{true, true, true, true, false},
									{false, true, false, false, true},
									{true, false, true, false, false},
									{true, false, true, false, true},
									{false, true, false, true, false},
									{false, true, false, true, true},
									{false, true, true, true, false},
									{false, true, true, true, true},
									{true, false, false, true, false},
									{true, false, false, true, true},
									{true, false, true, true, false},
									{true, false, true, true, true},
									{true, true, false, true, false},
									{true, true, false, true, true},
									{true, true, true, false, false},
									{true, true, true, false, true}};
		for(int i = 0; i < fiveBitCode.length; i++){
			LinkedList<Boolean> fiveBitCodeList = new LinkedList<Boolean>();
			for(int j = 0; j < fiveBitCode[i].length; j++){
				fiveBitCodeList.add(fiveBitCode[i][j]);
			}
			fiveBitCodeLists.add(fiveBitCodeList);
		}
		int index = 0;
		ListIterator<LinkedList<Boolean>> fiveBitCodeIterator = fiveBitCodeLists.listIterator();
		while(fiveBitCodeIterator.hasNext()){
			decoder.put(fiveBitCodeIterator.next(),fourBitDatas[index]);
			index++;
		}
	}
	//convert group every five bits to a byte array
	public static byte[] toBytes(Boolean[] bits) {
	    byte[] bytes = new byte[bits.length / 8];
	    for (int i = 0; i < bytes.length; i++) {
	        for (int bit = 0; bit < 8; bit++) {
	            if (bits[i * 8 + bit]) {
	                bytes[i] |= (128 >> bit);
	            }
	        }
	    }
	    return bytes;
	} 
	//funtion that prints the byte array into hex format string. 
	public static void printBytesInHex(byte[] bytes){
		for(int i = 0; i<bytes.length; i++){
			System.out.print(String.format("%02X", bytes[i]));
		}
		System.out.println();
	}
	
}
