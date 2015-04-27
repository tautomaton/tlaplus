// Copyright (c) 2003 Compaq Corporation.  All rights reserved.
// Portions Copyright (c) 2003 Microsoft Corporation.  All rights reserved.
// Last modified on Mon 30 Apr 2007 at 13:26:33 PST by lamport
//      modified on Mon Dec 18 22:56:08 PST 2000 by yuanyu

package tlc2.util;

import java.io.File;
import java.io.IOException;
import java.util.NoSuchElementException;

import util.BufferedDataInputStream;
import util.BufferedDataOutputStream;
import util.FileUtil;

/**
 * 
 * @version $Id$
 */
public final class MemIntQueue {
  private final static int InitialSize = 4096;
  private final static int GrowthFactor = 2;

  private int len;
  private int[] elems;
  private int start;
  private String diskdir;
  private String filename;
    
  public MemIntQueue(String metadir, String filename) {
    this.len = 0;
    this.elems = new int[InitialSize];
    this.start = 0;
    this.diskdir = metadir;
    this.filename = filename;
  }

  public final int length() { return this.len; }
  
  public final boolean hasElements() { return this.len > 0; }
  
  public final void enqueueInt(int elem) {
    if (this.len == this.elems.length) {
      // grow the array
      int newLen = Math.max(1, this.len * GrowthFactor);
      int[] newElems = new int[newLen];
      int copyLen = this.elems.length - this.start;
      System.arraycopy(this.elems, this.start, newElems, 0, copyLen);
      System.arraycopy(this.elems, 0, newElems, copyLen, this.start);
      this.elems = newElems;
      this.start = 0;
    }
    int last = (this.start + this.len) % this.elems.length;
    this.elems[last] = elem;
    this.len++;
  }

  public final void enqueueLong(long elem) {
    this.enqueueInt((int)(elem >>> 32));
    this.enqueueInt((int)(elem & 0xFFFFFFFFL));
  }
    
  public final int dequeueInt() {
    // Assert.check(this.len > 0);
	if (this.len < 1) {
		throw new NoSuchElementException();
	}
    int res = this.elems[this.start];
    this.len--;
    this.start = (this.start + 1) % this.elems.length;
    return res;
  }

  public final long dequeueLong() {
    long high = this.dequeueInt();
    long low = this.dequeueInt();
    return (high << 32) | (low & 0xFFFFFFFFL);
  }
  
  // Checkpoint.
  public final void beginChkpt() throws IOException {
    String tmpName = this.diskdir + FileUtil.separator + this.filename + ".tmp";
    BufferedDataOutputStream bos = new BufferedDataOutputStream(tmpName);
    bos.writeInt(this.len);
    int index = this.start;
    for (int i = 0; i < this.len; i++) {
      bos.writeInt(this.elems[index++]);
      if (index == this.elems.length) index = 0;
    }
    bos.close();
  }

  public final void commitChkpt() throws IOException {
    String oldName = this.diskdir + FileUtil.separator + this.filename + ".chkpt";
    File oldChkpt = new File(oldName);
    String newName = this.diskdir + FileUtil.separator + this.filename + ".tmp";
    File newChkpt = new File(newName);
    if ((oldChkpt.exists() && !oldChkpt.delete()) ||
	!newChkpt.renameTo(oldChkpt)) {
      throw new IOException("MemStateQueue.commitChkpt: cannot delete " + oldChkpt);
    }
  }
  
  public final void recover() throws IOException {
    String chkptName = this.diskdir + FileUtil.separator + this.filename + ".chkpt";
    BufferedDataInputStream bis = new BufferedDataInputStream(chkptName);
    this.len = bis.readInt();
    for (int i = 0; i < this.len; i++) {
      this.elems[i] = bis.readInt();
    }
    bis.close();
  }

  	/*
	 * The detailed formatter below can be activated in Eclipse's variable view
	 * by choosing "New detailed formatter" from the MemIntQueue context menu.
	 * Insert "MemIntQueue.DetailedFormatter.fpAndtidxAndptr(this);".
	 */
  	public static class DetailedFormatter {
  		
  		// An Eclipse detailed formatter for when this Queue holds pairs of long
  		// (fp) and int (tableau idx)
  		public static String fpAndtidx(MemIntQueue aQueue) {
  			final StringBuffer buf = new StringBuffer(aQueue.len / 3);
  			for (int i = 0; i < aQueue.len; i+=3) {
  				final long fp = ((long) aQueue.elems[i] << 32) | ((long) (aQueue.elems[i + 1]) & 0xFFFFFFFFL);
  				buf.append("fp: " + fp);
  				buf.append(" tidx: " + aQueue.elems[i + 2]);
  				buf.append("\n");
  			}
  			return buf.toString();
  		}
  		
  		// An Eclipse detailed formatter for when this Queue holds pairs of long
  		// (fp), int (tableau idx) and long (disk graph pointer).
  		public static String fpAndtidxAndptr(MemIntQueue aQueue) {
  			final StringBuffer buf = new StringBuffer(aQueue.len / 5);
  			for (int i = 0; i < aQueue.len; i += 5) {
  				final long fp = ((long) aQueue.elems[i] << 32) | ((long) (aQueue.elems[i + 1]) & 0xFFFFFFFFL);
  				buf.append("fp: " + fp);
  				buf.append(" tidx: " + aQueue.elems[i + 2]);
  				final long ptr = ((long) aQueue.elems[i + 3] << 32) | ((long) (aQueue.elems[i+ 4] ) & 0xFFFFFFFFL);
  				buf.append(" ptr: " + ptr);
  				buf.append("\n");
  			}
  			return buf.toString();
  		}
  	}
}
