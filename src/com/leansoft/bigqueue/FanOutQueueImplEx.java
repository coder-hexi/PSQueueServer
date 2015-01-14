package com.leansoft.bigqueue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import wjw.psqueue.msg.ResQueueStatus;
import wjw.psqueue.msg.ResSubStatus;
import wjw.psqueue.msg.ResultCode;

import com.leansoft.bigqueue.utils.FileUtil;

public class FanOutQueueImplEx extends FanOutQueueImpl {
	public final static int MINI_TRUNCATE_SIZE = 100000;
	public final static long DEFAULT_CAPACITY = 10000000L; //���е�ȱʡ����,1ǧ��.
	public final static String FILENAME_CAPACITY = "capacity.dat";

	String _queueName;

	public String getQueueName() {
		return _queueName;
	}

	long _capacity;

	public long getCapacity() {
		return this._capacity;
	}

	private long getCapacityFromFile() throws Exception {
		String name = super.innerArray.arrayDirectory;
		this._capacity = Long.parseLong(readFileToString(new File(name, FILENAME_CAPACITY), "UTF-8"));
		return this._capacity;
	}

	public void setCapacity(long capacity) throws Exception {
		this._capacity = capacity;

		String name = super.innerArray.arrayDirectory;

		writeStringToFile(new File(name, FILENAME_CAPACITY), String.valueOf(this._capacity), "UTF-8");
	}

	public FanOutQueueImplEx(String queueDir, String queueName) throws IOException {
		super(queueDir, queueName);
		this._queueName = queueName;

		try {
			this._capacity = getCapacityFromFile();
		} catch (Throwable thex) {
			this._capacity = DEFAULT_CAPACITY;
			try {
				setCapacity(this._capacity);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
	}

	public FanOutQueueImplEx(String queueDir, String queueName, int pageSize) throws IOException {
		super(queueDir, queueName, pageSize);
		this._queueName = queueName;

		try {
			this._capacity = getCapacityFromFile();
		} catch (Throwable thex) {
			this._capacity = DEFAULT_CAPACITY;
			try {
				setCapacity(this._capacity);
			} catch (Exception e) {
				throw new IOException(e);
			}
		}
	}

/*	@wjw_note: addʱ,�Ƿ�������?
	@Override
	public long enqueue(byte[] data) throws IOException {
		if (this.innerArray.size() >= this._capacity) {
			return -1L; //queue is full!!!
		} else {
			return super.enqueue(data);
		}
	}
*/
	
	//����Queueռ�õĴ��̿ռ�
	public long getDiskSize() { //��BigArrayImpl.getBackFileSize()��������:������index�ļ�!
		return super.innerArray.dataPageFactory.getBackPageFileSet().size();
	}

	//ɾ��Queue�µ����ж���(����Ŀ¼)
	public void erase() throws IOException {
		String name = super.innerArray.arrayDirectory;

		super.removeAll();
		super.close();

		FileUtil.deleteDirectory(new File(name));
	}

	//���Sub
	public void addFanout(String fanoutId) throws IOException {
		super.getQueueFront(fanoutId);
	}

	//ɾ��Sub
	public void removeFanout(String fanoutId) throws IOException {
		this.queueFrontMap.remove(fanoutId).indexPageFactory.deleteAllPages();

		String dirName = innerArray.arrayDirectory + QUEUE_FRONT_INDEX_PAGE_FOLDER_PREFIX + fanoutId;
		FileUtil.deleteDirectory(new File(dirName));
	}

	//�Ƿ����ָ����Sub
	public boolean containFanout(String fanoutId) {
		return this.queueFrontMap.containsKey(fanoutId);
	}

	//�õ����е�Sub
	public List<String> getAllFanoutNames() {
		List<String> result = new ArrayList<String>(this.queueFrontMap.size());
		for (QueueFront qf : this.queueFrontMap.values()) {
			result.add(qf.fanoutId);
		}

		return result;
	}

	//����Queue��Ϣ
	public ResQueueStatus getQueueInfo() throws IOException {
		return new ResQueueStatus(ResultCode.SUCCESS, _queueName, this._capacity, super.size(), this.getRearIndex(), this.getFrontIndex());
	}

	//����Sub��Ϣ
	public ResSubStatus getFanoutInfo(String fanoutId) throws IOException {
		return new ResSubStatus(ResultCode.SUCCESS, _queueName, this._capacity, fanoutId, super.size(fanoutId), this.getRearIndex(), this.getFrontIndex(fanoutId));
	}

	//����Sub��Ŀ¼ǰ׺
	public static String getFanoutFolderPrefix() {
		return QUEUE_FRONT_INDEX_PAGE_FOLDER_PREFIX;
	}

	//��������
	public void limitCapacity() throws IOException {
		long toTruncateSize = this.size() - this._capacity;
		if (toTruncateSize < MINI_TRUNCATE_SIZE) {
			return;
		}

		try {
			this.innerArray.arrayWriteLock.lock();

			long tailIndex = this.innerArray.getTailIndex();
			if ((tailIndex + toTruncateSize) < 0) {
				tailIndex = toTruncateSize + tailIndex - Long.MAX_VALUE;
			} else {
				tailIndex = toTruncateSize + tailIndex;
			}
			this.innerArray.removeBeforeIndex(tailIndex);

			for (QueueFront qf : this.queueFrontMap.values()) {
				try {
					qf.writeLock.lock();
					qf.validateAndAdjustIndex();
				} finally {
					qf.writeLock.unlock();
				}
			}

		} finally {
			this.innerArray.arrayWriteLock.unlock();
		}

	}

	public static class QueueFilenameFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			try {
				File file = new File(dir, name);
				return file.isDirectory();
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public static class SubFilenameFilter implements FilenameFilter {
		@Override
		public boolean accept(File dir, String name) {
			try {
				File file = new File(dir, name);
				return (file.isDirectory() && file.getName().startsWith(QUEUE_FRONT_INDEX_PAGE_FOLDER_PREFIX));
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}

	public static String readFileToString(File file, String encoding) throws IOException {
		FileChannel channel = new FileInputStream(file).getChannel();
		Charset charset = Charset.forName(encoding);
		try {
			ByteBuffer buf = ByteBuffer.allocate((int) file.length());
			if (channel.read(buf) != -1) {
				buf.flip();
				CharBuffer charBuffer = charset.decode(buf);
				return charBuffer.toString();
			} else {
				return "";
			}
		} finally {
			channel.close();
		}
	}

	public static void writeStringToFile(File file, String data, String encoding) throws IOException {
		FileChannel channel = new FileOutputStream(file).getChannel();
		Charset charset = Charset.forName(encoding);
		try {
			ByteBuffer buf = charset.encode(data);
			channel.write(buf);
		} finally {
			channel.close();
		}
	}

}
