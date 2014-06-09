package org.hustsse.spider.util;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.hustsse.spider.model.CrawlURL;

public class CommonUtils {

    public static void prepareParentDirs(String absPath){
        File f = new File(absPath);
        if(!f.getParentFile().exists())
            f.getParentFile().mkdirs();
    }

	public static void writeToFile(ByteBuffer b, String file) {
        prepareParentDirs(file);
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(file);
			FileChannel localFile = out.getChannel();
			localFile.write(b);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(out != null)
				try {
					out.close();
				} catch (IOException e) {
				}
		}

	}

	public static void writeToFile(String s,String file) {
        prepareParentDirs(file);
        FileWriter w = null;
		try {
			w = new FileWriter(file);
			w.write(s);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(w!=null)
				try {
					w.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public static void appendToFile(String s,String file) {
		FileWriter w = null;
		try {
			w = new FileWriter(file);
			w.append(s);
			w.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally {
			if(w!=null)
				try {
					w.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}


}
