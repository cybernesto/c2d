/*

c2d, Code to Disk, Version 0.57

(c) 2012,2017 All Rights Reserved, Egan Ford (egan@sense.net)

THIS CODE AND INFORMATION ARE PROVIDED "AS IS" WITHOUT WARRANTY OF ANY 
KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND/OR FITNESS FOR A
PARTICULAR PURPOSE.

Based on work by:
    * Weishaar, Tom. "Running without filenames". Open-Apple Jan. 1985 Vol. 1, No. 0: p. 7
      (http://apple2online.com/web_documents/Open%20Apple%20Vol1No00.pdf)

Java Version by:
    * cybernesto, 2019

License:
	*  Do what you like, remember to credit all sources when using.

Description:
	This small utility will read Apple II binary and monitor text files and
	output a DOS ordered dsk image that will boot your code quickly.

Features:
	*  Apple II+, IIe support.
	*  Big and little-endian machine support.
		o  Little-endian tested.
	*  Platforms tested:
		o  32-bit/64-bit x86 OS/X.
		o  32-bit x86 Windows/MinGW.

Bugs:
	*  Yes (input checking)

*/

import java.io.*;
import java.util.regex.*;

class C2d {
	private static final String VERSION = "Version 0.57";
	private enum FileTypes { BINARY, MONITOR };
	
	public static void main(String[] args) {
		char c='h';
		int i, start = 0, loadAddress, fileSize = 0;
		boolean warm = false;
		FileTypes inputType;
		int loaderStart, loaderSize, loaderPos = 0, textPageSize = 0;
		int bar = 0, row = 19, gr = 0;				
		String ext, fileName, outFile, textPage = "";
		int optind = 0;
		Disk blank = new Disk();
		
		for (i = 0; i < args.length; i++) {
			if(args[i].charAt(0) == '-') {
				try {
					c = args[i].charAt(1);
				} catch (StringIndexOutOfBoundsException e) {
					usage();
					System.exit(1);
				}
				
				switch (c) {
					case 't':	// load a splash page while loading binary
						loaderPos = 1;
						if (i + 1 < args.length) {
							textPage = args[i+1];
							i++;
						} else {
							usage();
							System.exit(1);
						}
						break;
					case 'm':	// boot to monitor
						warm = true;
						break;
					case 'v':	// version
						System.out.println(VERSION);
						System.exit(1);
						break;
					case 's':	// start here instead of load address
						warm = false;
						start = parseIntArgument(args, i+1, 16);
						i++;
						break;
					case 'r':	// bar row
						row = parseIntArgument(args, i+1, 10);
						i++;
						if (row > 23)
							row = 23;
						break;
					case 'b':
						bar = 1;
						break;
					case 'g':
						gr = 1;
						break;
					case 'h':	// help
					case '?':
						usage();
						break;
				}
				optind = i;
			}			
		}		

		if (args.length - optind < 2) {
			usage();
			System.exit(1);
		}
		
		System.out.println();
		
		inputType = FileTypes.BINARY;
		
		String[] inputList = args[args.length-2].split(",");
		fileName = inputList[0];
		outFile = args[args.length-1];
		
		if (inputList.length > 1) {
			loadAddress = parseIntArgument(inputList, 1, 16);
		} else {
			loadAddress = -1;
		}

		inputList = fileName.split("\\.");
		ext = inputList[inputList.length - 1];
		if( ext.compareToIgnoreCase("mon") == 0)
			inputType = FileTypes.MONITOR;
		
		System.out.printf("Reading %s, type %s, load address: $", fileName, inputType);
		
		if (inputType == FileTypes.BINARY) {
			byte[] b = new byte[2];
			try {
				DataInputStream inStream = new DataInputStream(new FileInputStream(fileName));
				fileSize = (int) new File(fileName).length();
				if (loadAddress == -1) {
					inStream.read(b);
					loadAddress = (b[0]&0x00FF) | ((b[1]<<8)&0xFF00);

					inStream.read(b);
					fileSize = (b[0]&0x00FF) | ((b[1]<<8)&0xFF00);
				}

				byte[] buffer = new byte[fileSize];
				inStream.read(buffer);
				inStream.close();
				blank.writeByte(1 + loaderPos, 0, loadAddress & 0xFF, buffer);
			}
			catch (FileNotFoundException e) {
				System.out.println("Cannot read: " + fileName);
				System.exit(1);
			}
			catch ( IOException e) {
				System.out.println("Cannot read: " + fileName);
				System.exit(1);
			}
			catch ( IndexOutOfBoundsException e) {
				System.out.println();
				System.out.println("Binary too big!");
				System.exit(1);
			}			
		} 

		if (inputType == FileTypes.MONITOR) {
			loadAddress = -1;
			fileSize = 0;

			try {
				BufferedReader br = new BufferedReader(new FileReader(fileName));
				String st; 
				Pattern p = Pattern.compile("(\\p{XDigit}{4}):\\s((\\p{XDigit}{2}\\s?)+)");
				while ((st = br.readLine()) != null) {
					Matcher m = p.matcher(st);
					
					if(m.matches()){
						if (loadAddress == -1) {
							loadAddress = Integer.parseInt(m.group(1), 16);
							blank.setWritePos(1 + loaderPos, 0, loadAddress & 0xFF);
						}

						String[] byteList = m.group(2).split(" ");
						for(String value : byteList){
							blank.writeByte(Integer.parseInt(value, 16));
							fileSize++;
						}
					}
				}
				br.close();
			}
			catch (FileNotFoundException e) {
				System.out.println("Cannot read: " + fileName);
				System.exit(1);
			}
			catch ( IOException e) {
				System.out.println("Cannot read: " + fileName);
				System.exit(1);
			}
			catch ( IndexOutOfBoundsException e) {
				System.out.println();
				System.out.println("Binary too big!");
				System.exit(1);
			}			
		}

		System.out.printf("%04X, length: %d\n", loadAddress, fileSize);
		System.out.println();
		
		if (start == 0)
			start = loadAddress;
		if (warm)
			start = 0xFF69;
		
		if (loaderPos == 0) {
			blank.writeByte(0, 1, 0xE0, (int)Math.ceil((fileSize + (loadAddress&0xFF))/256.0));
			blank.writeByte(0, 1, 0xE7, ((loadAddress + fileSize - 1) >> 8) + 1);
			blank.writeByte(0, 1, 0x15, (int)Math.ceil((fileSize + (loadAddress & 0xFF)) / 4096.0));
			blank.writeByte(0, 1, 0x1A, (int)Math.ceil((fileSize + (loadAddress & 0xFF)) / 256.0) - 16 * ((int)Math.ceil((fileSize + (loadAddress & 0xFF)) / 4096.0) - 1) - 1);

			System.out.printf("Number of sectors:    %d\n", (int) Math.ceil((fileSize + (loadAddress & 0xFF)) / 256.0));
			System.out.printf("Memory page range:    $%02X - $%02X\n", loadAddress >> 8, (loadAddress + fileSize - 1) >> 8);
			
			blank.writeByte(0, 1, 0x3B, 0x4C);
			blank.writeByte(0, 1, 0x3C, start & 0xFF);
			blank.writeByte(0, 1, 0x3D, start >> 8);

			System.out.printf("After boot, jump to:  $%04X\n\n", start);

			System.out.printf("Writing %s to T:01/S:00 - T:%02d/S:%02d on %s\n\n", fileName, blank.readByte(0, 1, 0x15), blank.readByte(0, 1, 0x1A), outFile);
		} else {
			try {
				DataInputStream inStream = new DataInputStream(new FileInputStream(textPage));
				textPageSize = (int) new File(textPage).length();
				
				if (textPageSize != 1024) {
					System.out.printf("textpage %s size %d != 1024\n\n", textPage, textPageSize);
					System.exit(1);
				}

				byte[] buffer = new byte[textPageSize];
				inStream.read(buffer);
				inStream.close();
				blank.writeByte(1, 0, 0, buffer);
			}
			catch (FileNotFoundException e) {
				System.out.println("Cannot read: " + textPage);
				System.exit(1);
			}
			catch (IOException e) {
				System.out.println("Cannot read: " + textPage);
				System.exit(1);
			}
			
			Loader loader = new Loader(bar);				
			blank.writeByte(1, 4, 0, loader.code, loader.size);

			// loader args
			// lasttrack
			blank.writeByte(1, 4, loader.size, 1 + (int) Math.ceil(fileSize / 4096.0));
			// lastsector
			blank.writeByte(1, 4, loader.size + 1, (int) Math.ceil((fileSize % 4096) / 256.0) - 1);
			// loadpage
			blank.writeByte(1, 4, loader.size + 2, loadAddress >> 8);
			// program start LSB
			blank.writeByte(1, 4, loader.size + 3, start & 0xFF);
			// program start MSB
			blank.writeByte(1, 4, loader.size + 4, start >> 8);
			// gr mode
			blank.writeByte(1, 4, loader.size + 5, gr);

			//bar code, pre compute status bar table
			if(bar == 1) {
				int numSectors = (int) Math.ceil((fileSize + (loadAddress & 0xFF)) / 256.0);
				int barLength = 40;
				int rowAddr;

				// bar row
				blank.writeByte(1, 4, loader.size + 6, row);

				rowAddr = 0x400+(row/8)*0x28+((row%8)*0x80);

				// program start LSB
				blank.writeByte(1, 4, loader.size + 7, rowAddr & 0xFF);
				// program start MSB
				blank.writeByte(1, 4, loader.size + 8, rowAddr >> 8);

				for(i = 1; i <= barLength; i++)
					blank.writeByte(1, 4, loader.size + 8 + i, i * numSectors / barLength);
			}
			
			loaderStart = 0x800;

			// temp hack to effect the sound of the drive, i.e. to make consistent
			// longer term put binary payload at end of loader
			// loadersize += (1024 + 5);	// textpage + loader + loader args
			loaderSize = 4096;

			blank.writeByte(0, 1, 0xE0, (int) Math.ceil((loaderSize + (loaderStart & 0xFF)) / 256.0));
			blank.writeByte(0, 1, 0xE7, ((loaderStart + loaderSize - 1) >> 8) + 1);
			blank.writeByte(0, 1, 0x15, (int) Math.ceil((loaderSize + (loaderStart & 0xFF)) / 4096.0));
			blank.writeByte(0, 1, 0x1A, (int) Math.ceil((loaderSize + (loaderStart & 0xFF)) / 256.0) - 16 * ((int) Math.ceil((loaderSize + (loaderStart & 0xFF)) / 4096.0) - 1) - 1);

			System.out.printf("Loader number of sectors:    %d\n", (int) Math.ceil((loaderSize + (loaderStart & 0xFF)) / 256.0));
			System.out.printf("Loader memory page range:    $%02X - $%02X\n", loaderStart >> 8, (loaderStart + loaderSize - 1) >> 8);
			System.out.printf("After loader, jump to:       $%04X\n", start);
			System.out.printf("Binary Number of sectors:    %d\n", (int) Math.ceil((fileSize + (loadAddress & 0xFF)) / 256.0));
			System.out.printf("Binary Memory page range:    $%02X - $%02X\n", loadAddress >> 8, (loadAddress + fileSize - 1) >> 8);

			loaderStart = 0xC00;

			blank.writeByte(0, 1, 0x3B, 0x4C);
			blank.writeByte(0, 1, 0x3C, loaderStart & 0xFF);
			blank.writeByte(0, 1, 0x3D, loaderStart >> 8);

			System.out.printf("After boot, jump to:         $%04X\n", loaderStart);
			System.out.printf("\n");
			System.out.printf("Writing %s to T:02/S:00 - T:%02d/S:%02d on %s\n\n", fileName, blank.readByte(1, 4, loader.size), blank.readByte(1, 4, loader.size + 1), outFile);
		}
		
		blank.write(outFile);
	}
	
	static int parseIntArgument(String[] args, int i, int base) {
		int argument = 0;
		if (i < args.length) {
			try {
				argument = Integer.parseInt(args[i], base);
			} catch (NumberFormatException e) {
				usage();
				System.exit(1);
			}
		} else {
			usage();
			System.exit(1);
		}
		return argument;
	}

	static void usage() {
		final String usagetext ="\n"+
			"usage:  c2d [-vh?]\n"+
			"        c2d [-bgm] [-r row] [-t filename] [-s start address override] input[.mon],[load_address] output.dsk\n"+
			"\n"+
			"        -h|? this help\n"+
			"        -m jump to monitor after booting\n"+
			"        -s XXXX jump to XXXX after booting\n"+
			"        -t filename, where filename is a 1K $400-$7FF text page splash screen\n"+
			"           The splash screen will display while the binary is loading\n"+
			"        -b animated loading bar\n"+
			"        -g splash page is mixed mode GR\n"+
			"        -r override row default of 19 with 'row'\n"+
			"        -v print version number and exit\n"+
			"\n"+
			"Input without a .mon extension is assumed to be a binary with a 4 byte header.\n"+
			"If the header is missing then you must append ,load_address to the binary input\n"+
			"filename, e.g. filename,800.  The load address will be read as hex.\n"+
			"\n"+
			"input with a .mon extension expected input format:\n"+
			"\n"+
			"        0800: A2 FF 9A 20 8C 02 20 4F\n"+
			"        0808: 03 4C 00 FF 20 9E 02 A9\n"+
			"\n"+
			"Examples:\n"+
			"\n"+
			"        c2d hello hello.dsk\n"+
			"        c2d hello.mon hello.dsk \n"+
			"        c2d hello,800 hello.dsk \n"+
			"        c2d -m test,300 test.dsk\n"+
			"        c2d -s 7300 alpha4,400 alpha4.dsk\n"+
			"        c2d -t gameserverclient.textpage gameserverclient,800 gameserverclient.dsk\n"+
			"\n";		 
		System.out.println(usagetext);
	}
}


class Disk {
	private byte[] buffer;
	private int writePos;
	
	final int TRACKS = 35;
	final int SECTORS = 16;
	final int BYTES = 256;
		
	public Disk() {
		buffer = new byte[TRACKS*SECTORS*BYTES];
		
		try {			
			//DataInputStream inStream = new DataInputStream(new FileInputStream("res/c2d.dsk"));
			InputStream inStream = getClass().getResourceAsStream("java/res/c2d.dsk");
			inStream.read(buffer);
			inStream.close();
		}
		catch (FileNotFoundException e) {
			System.out.println("Cannot read c2d.dsk!");
			System.exit(1);
		}
		catch ( IOException e) {
			System.out.println("Cannot read c2d.dsk!");
			System.exit(1);
		}
	}

	public byte readByte(int track, int sector, int pos) {
		byte b = buffer[(track * SECTORS  + sector) * BYTES + pos];

		return b;
	}

	public void writeByte(int track, int sector, int pos, byte[] buf, int size)  throws IndexOutOfBoundsException {
		int start = (track * SECTORS  + sector) * BYTES + pos;

		if ( (start + size) > buffer.length) {
			throw new IndexOutOfBoundsException();
		} else {
			for(int i = 0; i < size; i++) {
				buffer[start + i] = buf[i];			
			}
			writePos = start + size;
		}	
	}
	
	public void writeByte(int track, int sector, int pos, byte[] buf) throws IndexOutOfBoundsException {
		writeByte(track, sector, pos, buf, buf.length);
	}
	
	public void writeByte(int track, int sector, int pos, int value) throws IndexOutOfBoundsException {
		byte[] buf = new byte[1];
		buf[0] = (byte)value;
		writeByte(track, sector, pos, buf);
	}
	
	public void writeByte(int value) {
		buffer[writePos++] = (byte)value;			
	}
	
	public void setWritePos(int track, int sector, int pos) throws IndexOutOfBoundsException {
		int start = (track * SECTORS  + sector) * BYTES + pos;

		if ( start > buffer.length) {
			throw new IndexOutOfBoundsException();
		} else {
			writePos = start;
		}
	}
	
	public void write(String fileName){
		try {
			DataOutputStream outStream = new DataOutputStream(new FileOutputStream(fileName));
			outStream.write(buffer);
			outStream.close();
		}
		catch (FileNotFoundException e) {
			System.out.println("Cannot write " + fileName);
			System.exit(1);
		}
		catch (IOException e) {
			System.out.println("Cannot write " + fileName);
			System.exit(1);
		}
	}
}

class Loader {
	byte[] code;
	int size;
		
	public Loader(int bar) {
		code = new byte[256];
		try {
			InputStream inStream;
			
			if (bar == 0){
				inStream = getClass().getResourceAsStream("asm/loader");
			} else {
				inStream = getClass().getResourceAsStream("asm/bar");
			}
			
			size = inStream.read(code);
			inStream.close();
		}
		catch (FileNotFoundException e) {
			System.out.println("Cannot read loader!");
			System.exit(1);
		}
		catch (IOException e) {
			System.out.println("Cannot read loader!");
			System.exit(1);
		}
		catch (IndexOutOfBoundsException e) {
			System.out.println();
			System.out.printf("Loader code size > 256\n\n");
			System.exit(1);
		}
	}
}
